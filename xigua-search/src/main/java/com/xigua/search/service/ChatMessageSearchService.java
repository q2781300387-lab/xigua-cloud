package com.xigua.search.service;

import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.WildcardQuery;
import com.alibaba.fastjson2.JSONObject;
import com.xigua.common.core.exception.BusinessException;
import com.xigua.common.core.util.UserContext;
import com.xigua.api.service.GroupMemberService;
import com.xigua.common.es.client.EsClient;
import com.xigua.common.es.client.EsHit;
import com.xigua.common.es.client.EsPage;
import com.xigua.common.redis.client.RedisClient;
import com.xigua.domain.dto.GlobalSearchChatMessageDTO;
import com.xigua.domain.dto.SearchChatMessageDTO;
import com.xigua.domain.enums.ChatType;
import com.xigua.domain.enums.RedisEnum;
import com.xigua.domain.vo.ChatMessageSearchResultVO;
import com.xigua.domain.vo.ChatMessageSyncVO;
import com.xigua.domain.vo.SearchPageVO;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @ClassName ChatMessageSearchService
 * @Description 历史聊天记录搜索（基于 ES）
 * @Author wangjinfei
 * @Date 2026/9/29
 */
@Service
public class ChatMessageSearchService {

    private static final String CHAT_MESSAGE_INDEX = "chat_message";

    @Autowired
    private EsClient esClient;

    @Autowired
    private RedisClient redisClient;

    /**
     * 群聊搜索前校验成员身份：search 服务只读 ES，不能仅凭前端传入的群 id 放开全群消息
     */
    @DubboReference(check = false, timeout = 3000)
    private GroupMemberService groupMemberService;

    /**
     * 搜索历史聊天记录
     * @author wangjinfei
     * @date 2026/9/29
     * @param dto
     * @return SearchPageVO<ChatMessageSearchResultVO>
     */
    public SearchPageVO<ChatMessageSearchResultVO> search(SearchChatMessageDTO dto) {
        String keyword = dto.getKeyword();
        if (StringUtils.isBlank(keyword)) {
            throw new BusinessException("请输入搜索关键字");
        }
        Integer chatType = dto.getChatType();
        String receiverId = dto.getReceiverId();
        if (chatType == null || StringUtils.isBlank(receiverId)) {
            throw new BusinessException("聊天类型或对方id不能为空");
        }

        String userId = UserContext.get().getUserId();
        // 群聊只允许搜索自己已加入的群，避免任意登录用户按 groupId 拉取他群消息
        if (ChatType.GROUP.getType().equals(chatType)) {
            Integer role = groupMemberService.getGroupRole(receiverId, userId);
            if (role == null) {
                throw new BusinessException("你不在该群聊中，无法搜索群消息");
            }
        }
        int pageNum = (dto.getPageNum() == null || dto.getPageNum() < 1) ? 1 : dto.getPageNum();
        int pageSize = (dto.getPageSize() == null || dto.getPageSize() < 1) ? 20 : dto.getPageSize();

        // 三条路并集：
        // 1) message 走分词匹配（smartcn），负责中文自然语言检索；
        // 2) messageRaw 走整串 term，负责「整条消息就是一个编号」的场景；
        // 3) messageRaw 走 wildcard 子串，负责「编号夹在一句话里」的场景
        //    （smartcn 会把 C38INCR 切成 c/38/incr，搜 38INC 这类跨界子串分词救不回来）。
        // wildcard 只对纯 ASCII 关键词启用，避免中文短词带来无谓的全表扫描。
        Query keywordQuery = buildKeywordQuery(keyword);
        Query chatTypeFilter = TermQuery.of(t -> t.field("chatType").value(chatType.longValue()))._toQuery();
        Query scopeFilter = buildScopeFilter(chatType, userId, receiverId);

        Query finalQuery = BoolQuery.of(b -> b.must(keywordQuery, chatTypeFilter, scopeFilter))._toQuery();

        int from = (pageNum - 1) * pageSize;
        EsPage<EsHit<ChatMessageSyncVO>> page = esClient.searchWithHighlight(CHAT_MESSAGE_INDEX, finalQuery, from, pageSize,
                "message", "<em>", "</em>", "createTime", SortOrder.Desc, ChatMessageSyncVO.class);

        // 发送人/接收人昵称、头像按 id 缓存，避免同一页内重复查询 redis
        Map<String, ProfileInfo> profileCache = new HashMap<>();
        List<ChatMessageSearchResultVO> rows = new ArrayList<>();
        for (EsHit<ChatMessageSyncVO> hit : page.getRows()) {
            ChatMessageSyncVO vo = hit.getSource();

            ChatMessageSearchResultVO result = new ChatMessageSearchResultVO();
            result.setId(vo.getId());
            result.setSenderId(vo.getSenderId());
            result.setReceiverId(vo.getReceiverId());
            result.setMessage(vo.getMessage());
            result.setHighlightMessage(extractHighlight(hit, vo.getMessage()));
            result.setChatType(vo.getChatType());
            result.setMessageType(vo.getMessageType());
            result.setCreateTime(vo.getCreateTime());

            ProfileInfo senderProfile = resolveProfile(vo.getSenderId(), profileCache);
            result.setSenderName(senderProfile.name());
            result.setSenderAvatar(senderProfile.avatar());

            ProfileInfo receiverProfile = resolveProfile(vo.getReceiverId(), profileCache);
            result.setReceiverName(receiverProfile.name());
            result.setReceiverAvatar(receiverProfile.avatar());

            rows.add(result);
        }

        SearchPageVO<ChatMessageSearchResultVO> result = new SearchPageVO<>();
        result.setPageNum(pageNum);
        result.setPageSize(pageSize);
        result.setTotalCount(page.getTotal());
        result.setTotalPage((page.getTotal() + pageSize - 1) / pageSize);
        result.setRows(rows);
        return result;
    }

    /**
     * 全局搜索：跨当前用户的全部单聊、群聊、机器人会话检索消息。
     * 群聊范围只取当前用户已加入的群，避免按关键字扫到别人的群。
     */
    public SearchPageVO<ChatMessageSearchResultVO> searchGlobal(GlobalSearchChatMessageDTO dto) {
        String keyword = StringUtils.trimToEmpty(dto.getKeyword());
        if (StringUtils.isBlank(keyword)) {
            throw new BusinessException("请输入搜索关键字");
        }
        int pageNum = (dto.getPageNum() == null || dto.getPageNum() < 1) ? 1 : dto.getPageNum();
        int pageSize = (dto.getPageSize() == null || dto.getPageSize() < 1) ? 20 : Math.min(dto.getPageSize(), 50);

        String userId = UserContext.get().getUserId();
        Set<String> groupIds = groupMemberService.getGroupIdsByUserId(userId);

        Query keywordQuery = buildKeywordQuery(keyword);

        // 单聊 / 机器人：只要一方是当前用户即可，不区分方向（机器人回复时双方 id 会互换）
        Query privateScope = BoolQuery.of(b -> b
                .should(BoolQuery.of(x -> x
                        .must(TermQuery.of(t -> t.field("senderId").value(userId))._toQuery())
                        .must(TermQuery.of(t -> t.field("chatType").value(ChatType.PRIVATE.getType().longValue()))._toQuery())
                )._toQuery())
                .should(BoolQuery.of(x -> x
                        .must(TermQuery.of(t -> t.field("receiverId").value(userId))._toQuery())
                        .must(TermQuery.of(t -> t.field("chatType").value(ChatType.PRIVATE.getType().longValue()))._toQuery())
                )._toQuery())
                .should(BoolQuery.of(x -> x
                        .must(TermQuery.of(t -> t.field("senderId").value(userId))._toQuery())
                        .must(TermQuery.of(t -> t.field("chatType").value(ChatType.BOT.getType().longValue()))._toQuery())
                )._toQuery())
                .should(BoolQuery.of(x -> x
                        .must(TermQuery.of(t -> t.field("receiverId").value(userId))._toQuery())
                        .must(TermQuery.of(t -> t.field("chatType").value(ChatType.BOT.getType().longValue()))._toQuery())
                )._toQuery())
                .minimumShouldMatch("1")
        )._toQuery();

        Query groupScope = BoolQuery.of(b -> {
            b.must(TermQuery.of(t -> t.field("chatType").value(ChatType.GROUP.getType().longValue()))._toQuery());
            if (groupIds != null && !groupIds.isEmpty()) {
                BoolQuery.Builder groupIdsQuery = new BoolQuery.Builder();
                groupIds.forEach(groupId -> groupIdsQuery.should(
                        TermQuery.of(t -> t.field("receiverId").value(groupId))._toQuery()));
                groupIdsQuery.minimumShouldMatch("1");
                b.must(groupIdsQuery.build()._toQuery());
            }
            return b;
        })._toQuery();

        Query scope = (groupIds == null || groupIds.isEmpty())
                ? privateScope
                : BoolQuery.of(b -> b.should(privateScope).should(groupScope).minimumShouldMatch("1"))._toQuery();
        Query withdrawnFilter = TermQuery.of(t -> t.field("withdrawn").value(0L))._toQuery();

        Query finalQuery = BoolQuery.of(b -> {
            b.must(keywordQuery, scope, withdrawnFilter);
            if (dto.getMessageType() != null) {
                b.must(TermQuery.of(t -> t.field("messageType").value(dto.getMessageType().longValue()))._toQuery());
            }
            return b;
        })._toQuery();

        int from = (pageNum - 1) * pageSize;
        EsPage<EsHit<ChatMessageSyncVO>> page = esClient.searchWithHighlight(CHAT_MESSAGE_INDEX, finalQuery, from, pageSize,
                "message", "<em>", "</em>", "createTime", SortOrder.Desc, ChatMessageSyncVO.class);

        Map<String, ProfileInfo> profileCache = new HashMap<>();
        List<ChatMessageSearchResultVO> rows = new ArrayList<>();
        for (EsHit<ChatMessageSyncVO> hit : page.getRows()) {
            ChatMessageSyncVO vo = hit.getSource();
            ChatMessageSearchResultVO result = new ChatMessageSearchResultVO();
            result.setId(vo.getId());
            result.setSenderId(vo.getSenderId());
            result.setReceiverId(vo.getReceiverId());
            result.setMessage(vo.getMessage());
            result.setHighlightMessage(extractHighlight(hit, vo.getMessage()));
            result.setChatType(vo.getChatType());
            result.setMessageType(vo.getMessageType());
            result.setCreateTime(vo.getCreateTime());

            ProfileInfo senderProfile = resolveProfile(vo.getSenderId(), profileCache);
            result.setSenderName(senderProfile.name());
            result.setSenderAvatar(senderProfile.avatar());

            ProfileInfo receiverProfile = resolveProfile(vo.getReceiverId(), profileCache);
            result.setReceiverName(receiverProfile.name());
            result.setReceiverAvatar(receiverProfile.avatar());

            // 归属会话：群聊就是群本身；单聊/机器人取对话对端，自己在左侧搜索时也能显示对方名字
            if (ChatType.GROUP.getType().equals(vo.getChatType())) {
                result.setConversationId(vo.getReceiverId());
                result.setConversationName(receiverProfile.name());
                result.setConversationAvatar(receiverProfile.avatar());
                result.setConversationChatType(ChatType.GROUP.getType());
            } else {
                String conversationId = userId.equals(vo.getSenderId()) ? vo.getReceiverId() : vo.getSenderId();
                ProfileInfo conversationProfile = resolveProfile(conversationId, profileCache);
                result.setConversationId(conversationId);
                result.setConversationName(conversationProfile.name());
                result.setConversationAvatar(conversationProfile.avatar());
                result.setConversationChatType(vo.getChatType());
            }

            rows.add(result);
        }

        SearchPageVO<ChatMessageSearchResultVO> result = new SearchPageVO<>();
        result.setPageNum(pageNum);
        result.setPageSize(pageSize);
        result.setTotalCount(page.getTotal());
        result.setTotalPage((page.getTotal() + pageSize - 1) / pageSize);
        result.setRows(rows);
        return result;
    }

    /**
     * 关键字查询：分词匹配 + 整串精确匹配 + ASCII 子串通配
     */
    private Query buildKeywordQuery(String keyword) {
        boolean asciiKeyword = keyword.codePoints().allMatch(c -> c < 128);
        String wildcardValue = "*" + keyword.replace("\\", "\\\\")
                .replace("*", "\\*").replace("?", "\\?") + "*";
        return BoolQuery.of(b -> {
            b.should(MatchQuery.of(m -> m.field("message").query(keyword).operator(Operator.And))._toQuery());
            b.should(TermQuery.of(t -> t.field("messageRaw").value(keyword))._toQuery());
            if (asciiKeyword) {
                b.should(WildcardQuery.of(w -> w.field("messageRaw").value(wildcardValue))._toQuery());
            }
            b.minimumShouldMatch("1");
            return b;
        })._toQuery();
    }

    /**
     * 提取 message 字段的高亮片段，取不到时回退为原始消息内容
     */
    private String extractHighlight(EsHit<ChatMessageSyncVO> hit, String fallbackMessage) {
        Map<String, List<String>> highlights = hit.getHighlights();
        if (highlights == null) {
            return fallbackMessage;
        }
        List<String> fragments = highlights.get("message");
        if (fragments == null || fragments.isEmpty()) {
            return fallbackMessage;
        }
        return String.join("", fragments);
    }

    /**
     * 构建会话范围过滤条件：群聊按接收人（群id）过滤；单聊/机器人对话按双方id（不限方向）过滤，
     * 因为机器人回复时 senderId/receiverId 会互换（与 getBotChatHistoryMes 的查询逻辑保持一致）
     */
    private Query buildScopeFilter(Integer chatType, String userId, String receiverId) {
        if (chatType.equals(ChatType.GROUP.getType())) {
            return TermQuery.of(t -> t.field("receiverId").value(receiverId))._toQuery();
        }

        Query forward = BoolQuery.of(b -> b
                .must(TermQuery.of(t -> t.field("senderId").value(userId))._toQuery())
                .must(TermQuery.of(t -> t.field("receiverId").value(receiverId))._toQuery())
        )._toQuery();

        Query backward = BoolQuery.of(b -> b
                .must(TermQuery.of(t -> t.field("senderId").value(receiverId))._toQuery())
                .must(TermQuery.of(t -> t.field("receiverId").value(userId))._toQuery())
        )._toQuery();

        return BoolQuery.of(b -> b.should(forward, backward).minimumShouldMatch("1"))._toQuery();
    }

    /**
     * 根据id解析昵称/头像：依次尝试用户、群组、机器人缓存（与 ChatMessageServiceImpl#setReceiverDefaultInfo 逻辑一致），
     * 不直接依赖 User/Group/Bot 实体（避免 xigua-search 引入 mybatis-plus 相关依赖），改为解析原始 json
     */
    private ProfileInfo resolveProfile(String id, Map<String, ProfileInfo> cache) {
        if (StringUtils.isBlank(id)) {
            return new ProfileInfo(null, null);
        }

        ProfileInfo cached = cache.get(id);
        if (cached != null) {
            return cached;
        }

        ProfileInfo profile = new ProfileInfo(null, null);

        String userCache = redisClient.get(RedisEnum.USER.getKey() + id);
        if (StringUtils.isNotBlank(userCache)) {
            JSONObject json = JSONObject.parseObject(userCache);
            profile = new ProfileInfo(json.getString("username"), json.getString("avatar"));
        } else {
            String groupCache = redisClient.get(RedisEnum.GROUP.getKey() + id);
            if (StringUtils.isNotBlank(groupCache)) {
                JSONObject json = JSONObject.parseObject(groupCache);
                profile = new ProfileInfo(json.getString("groupName"), json.getString("groupAvatar"));
            } else {
                String botCache = redisClient.get(RedisEnum.BOT.getKey() + id);
                if (StringUtils.isNotBlank(botCache)) {
                    JSONObject json = JSONObject.parseObject(botCache);
                    profile = new ProfileInfo(json.getString("name"), json.getString("avatar"));
                }
            }
        }

        cache.put(id, profile);
        return profile;
    }

    private record ProfileInfo(String name, String avatar) {
    }
}
