package com.xigua.client.service;

import com.alibaba.fastjson2.JSONObject;
import com.xigua.api.service.*;
import com.xigua.common.core.util.DateUtil;
import com.xigua.common.core.util.FirstLetterUtil;
import com.xigua.common.core.util.UserContext;
import com.xigua.common.redis.client.RedisClient;
import com.xigua.domain.entity.Bot;
import com.xigua.domain.entity.FriendRequest;
import com.xigua.domain.entity.Group;
import com.xigua.domain.entity.User;
import com.xigua.domain.enums.RedisEnum;
import com.xigua.domain.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @ClassName ContactServiceImpl
 * @Description TODO
 * @Author wangjinfei
 * @Date 2025/7/27 9:38
 */
@Slf4j
@Service
@DubboService
public class ContactServiceImpl implements ContactService {
    @Autowired
    private FriendRelationService friendRelationService;
    @Autowired
    private FriendRequestService friendRequestService;
    @Autowired
    private GroupMemberService groupMemberService;
    @Autowired
    private GroupService groupService;
    @Autowired
    private RedisClient redisClient;
    @Autowired
    private UserService userService;
    @DubboReference
    private BotService botService;
    @DubboReference
    private CenterService centerService;


    /**
     * 获取联系人数量
     * @author wangjinfei
     * @date 2025/7/27 9:42
     * @return ContactCountVO
     */
    @Override
    public ContactCountVO getContactCount() {
        String userId = UserContext.get().getUserId();
        ContactCountVO contactCountVO = new ContactCountVO();

        // 好友数量
        contactCountVO.setFriendCount(friendRelationService.getCountByUserId(userId));
        // 群数量
        contactCountVO.setGroupCount(groupMemberService.getCountByUserId(userId));
        // 机器人数量
        contactCountVO.setBotCount(botService.getCountByUserId(userId));
        // 发送好友申请数量
        contactCountVO.setSendCount(friendRequestService.getSendCountByUserId(userId));
        // 接收好友申请数量
        contactCountVO.setReceiveCount(friendRequestService.getReceiveCountByUserId(userId));

        return contactCountVO;
    }

    /**
     * 获取好友列表
     * @author wangjinfei
     * @date 2025/5/14 20:56
     * @return List<FriendVO>
     */
    @Override
    public List<FriendVO> getFriendList() {
        // todo 优化成分页获取好友

        String userId = UserContext.get().getUserId();
        List<FriendVO> voList = new ArrayList<>();

        // 获取好友id列表
        List<String> friendIds = friendRelationService.getFriendIdsByUserId(userId);
        if(CollectionUtils.isEmpty(friendIds)){
            return voList;
        }

        // 映射vo字段
        for (String friendId : friendIds) {
            // todo 可以批量获取
            // 从redis获取用户信息
            String friendCache = redisClient.get(RedisEnum.USER.getKey() + friendId);
            User user;
            if(StringUtils.isEmpty(friendCache)){
                // 缓存未命中时回查数据库并回填：直接写库导入的用户没有 user: 缓存，
                // 这里直接 continue 会让“好友 (N)”的数字和展开后的列表对不上
                user = userService.getById(friendId);
                if (user == null) {
                    continue;
                }
                redisClient.set(RedisEnum.USER.getKey() + friendId, JSONObject.toJSONString(user));
            } else {
                user = JSONObject.parseObject(friendCache, User.class);
            }

            FriendVO friendVO = new FriendVO();
            friendVO.setUserId(friendId);
            friendVO.setUsername(user.getUsername());
            friendVO.setAvatar(user.getAvatar());
            friendVO.setSignature(user.getSignature());

            // 判断是否在线
            friendVO.setIsOnline(centerService.isOnline(friendId));
            voList.add(friendVO);
        }
        return voList;
    }

    /**
     * 获取按字母分组的好友列表
     * @author wangjinfei
     * @date 2026/4/15
     * @return List<FriendGroupVO>
     */
    @Override
    public List<FriendGroupVO> firstLetterFriends() {
        // 获取所有好友列表
        List<FriendVO> friendList = getFriendList();
        if (CollectionUtils.isEmpty(friendList)) {
            return new ArrayList<>();
        }

        // 按首字母分组
        Map<String, List<FriendVO>> groupedMap = friendList.stream()
                .collect(Collectors.groupingBy(friend ->
                    FirstLetterUtil.getFirstLetter(friend.getUsername()),
                    LinkedHashMap::new,
                    Collectors.toList()
                ));

        // 转换为 FriendGroupVO 列表并按字母排序
        List<FriendGroupVO> result = groupedMap.entrySet().stream()
                .map(entry -> {
                    FriendGroupVO groupVO = new FriendGroupVO();
                    groupVO.setLetter(entry.getKey());
                    groupVO.setList(entry.getValue());
                    return groupVO;
                })
                .sorted(Comparator.comparing(FriendGroupVO::getLetter, (letter1, letter2) -> {
                    // # 排在最后
                    if ("#".equals(letter1)) return 1;
                    if ("#".equals(letter2)) return -1;
                    return letter1.compareTo(letter2);
                }))
                .collect(Collectors.toList());

        return result;
    }

    /**
     * 获取群列表
     * @author wangjinfei
     * @date 2025/7/27 11:44
     * @return List<GroupVO>
     */
    @Override
    public List<GroupVO> getGroupList() {
        // todo 优化成分页获取群组

        String userId = UserContext.get().getUserId();
        List<GroupVO> voList = new ArrayList<>();

        // 获取加入的群组
        Set<String> groupIds = groupMemberService.getGroupIdsByUserId(userId);
        if(CollectionUtils.isEmpty(groupIds)){
            return voList;
        }
        for (String groupId : groupIds) {
            String groupCache = redisClient.get(RedisEnum.GROUP.getKey() + groupId);
            if(StringUtils.isEmpty(groupCache)){
                // 缓存未命中时回查数据库并回填：直接写库导入的群没有 group: 缓存，
                // 这里直接 continue 会让群成员接口能看到群、群列表却显示不出来
                Group groupFromDb = groupService.getById(groupId);
                if (groupFromDb == null) {
                    continue;
                }
                groupCache = JSONObject.toJSONString(groupFromDb);
                redisClient.set(RedisEnum.GROUP.getKey() + groupId, groupCache);
            }
            Group group = JSONObject.parseObject(groupCache, Group.class);
            GroupVO groupVO = new GroupVO();
            groupVO.setGroupId(group.getId());
            groupVO.setGroupName(group.getGroupName());
            groupVO.setGroupAvatar(group.getGroupAvatar());
            groupVO.setCurrentMember(group.getCurrentMember());
            voList.add(groupVO);
        }

        return voList;
    }

    /**
     * 获取机器人列表
     * @author wangjinfei
     * @date 2025/10/1 16:49
     * @return List<BotVO>
     */
    @Override
    public List<BotVO> getBotList() {
        String userId = UserContext.get().getUserId();
        List<BotVO> voList = new ArrayList<>();

        // 获取加入的群组
        Set<String> botIds = botService.getBotIdsByUserId(userId);
        if(CollectionUtils.isEmpty(botIds)){
            return voList;
        }
        for (String botId : botIds) {
            String botCache = redisClient.get(RedisEnum.BOT.getKey() + botId);
            if(StringUtils.isEmpty(botCache)){
                // 缓存未命中时回查数据库并回填：直接写库导入的机器人没有缓存，
                // 这里直接 continue 会让“机器人 (N)”的数字和展开后的列表对不上
                Bot bot = botService.getById(botId);
                if (bot == null) {
                    continue;
                }
                String botJson = JSONObject.toJSONString(bot);
                redisClient.set(RedisEnum.BOT.getKey() + botId, botJson);
                voList.add(JSONObject.parseObject(botJson, BotVO.class));
                continue;
            }
            BotVO bot = JSONObject.parseObject(botCache, BotVO.class);
            voList.add(bot);
        }

        return voList;
    }

    /**
     * 获取发送好友申请列表
     * @author wangjinfei
     * @date 2025/8/2 12:35
     * @return List<FriendRequestVO>
     */
    @Override
    public List<FriendRequestVO> getSendFriendRequestList() {
        String userId = UserContext.get().getUserId();
        List<FriendRequestVO> voList = new ArrayList<>();

        // 查询发送的好友请求
        List<FriendRequest> senderFriendReqList = friendRequestService.getListBySenderId(userId);
        if(CollectionUtils.isEmpty(senderFriendReqList)){
            return voList;
        }

        // 映射发送好友请求vo字段
        for (FriendRequest friendRequest : senderFriendReqList) {
            String receiverId = friendRequest.getReceiverId();

            // 从redis获取用户信息
            String friendCache = redisClient.get(RedisEnum.USER.getKey() + receiverId);
            if(StringUtils.isEmpty(friendCache)){
                continue;
            }
            User receiver = JSONObject.parseObject(friendCache, User.class);
            if(receiver == null){
                continue;
            }

            // 映射vo字段
            FriendRequestVO friendRequestVO = new FriendRequestVO();
            friendRequestVO.setRequestId(friendRequest.getId());
            friendRequestVO.setUserId(receiver.getId());
            friendRequestVO.setUsername(receiver.getUsername());
            friendRequestVO.setAvatar(receiver.getAvatar());
            friendRequestVO.setApplyMsg(friendRequest.getApplyMsg());
            friendRequestVO.setSource("send");

            friendRequestVO.setFlowStatus(friendRequest.getFlowStatus());
            friendRequestVO.setCreateTime(DateUtil.formatDateTime(friendRequest.getCreateTime(),
                    DateUtil.DATE_TIME_FORMATTER));
            voList.add(friendRequestVO);
        }

        return voList;
    }

    /**
     * 获取接收好友申请列表
     * @author wangjinfei
     * @date 2025/8/2 12:35
     * @return List<FriendRequestVO>
     */
    @Override
    public List<FriendRequestVO> getReceiveFriendRequestList() {
        String userId = UserContext.get().getUserId();
        List<FriendRequestVO> voList = new ArrayList<>();

        // 查询接收的好友请求
        List<FriendRequest> receiverFriendReqList = friendRequestService.getListByReceiverId(userId);
        if(CollectionUtils.isEmpty(receiverFriendReqList)){
            return voList;
        }

        // 映射接收好友请求vo字段
        for (FriendRequest friendRequest : receiverFriendReqList) {
            String senderId = friendRequest.getSenderId();
            // 从redis获取用户信息
            String friendCache = redisClient.get(RedisEnum.USER.getKey() + senderId);
            if(StringUtils.isEmpty(friendCache)){
                continue;
            }
            User sender = JSONObject.parseObject(friendCache, User.class);
            if(sender == null){
                continue;
            }
            FriendRequestVO friendRequestVO = new FriendRequestVO();
            friendRequestVO.setRequestId(friendRequest.getId());
            friendRequestVO.setUserId(sender.getId());
            friendRequestVO.setUsername(sender.getUsername());
            friendRequestVO.setAvatar(sender.getAvatar());
            friendRequestVO.setApplyMsg(friendRequest.getApplyMsg());
            friendRequestVO.setSource("receive");

            friendRequestVO.setFlowStatus(friendRequest.getFlowStatus());
            friendRequestVO.setCreateTime(DateUtil.formatDateTime(friendRequest.getCreateTime(),
                    DateUtil.DATE_TIME_FORMATTER));
            voList.add(friendRequestVO);
        }
        return voList;
    }

    /**
     * 撤回我发送且仍待处理的好友申请
     * 只有发送者本人、且申请仍处于待处理状态时才允许撤回
     * @param requestId 申请ID
     * @return Boolean
     */
    @Override
    public Boolean withdrawFriendRequest(String requestId) {
        String userId = UserContext.get().getUserId();
        FriendRequest request = friendRequestService.getById(requestId);
        if (request == null || !userId.equals(request.getSenderId())) {
            return false;
        }
        if (request.getFlowStatus() == null || request.getFlowStatus() != 0) {
            return false;
        }
        // FriendRequest.delFlag 是逻辑删除字段，removeById 会走逻辑删除
        return friendRequestService.removeById(requestId);
    }
}
