package com.xigua.client.service;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xigua.api.service.ChatMessageService;
import com.xigua.api.service.UserService;
import com.xigua.client.helper.WsSessionHelper;
import com.xigua.client.mapper.GroupMapper;
import com.xigua.client.mapper.GroupJoinRequestMapper;
import com.xigua.domain.entity.GroupJoinRequest;
import com.xigua.domain.vo.GroupJoinRequestVO;
import com.xigua.client.utils.NineCellImageUtil;
import com.xigua.common.core.exception.BusinessException;
import com.xigua.common.core.util.DateUtil;
import com.xigua.common.core.util.UserContext;
import com.xigua.common.redis.client.RedisClient;
import com.xigua.common.sequence.sequence.Sequence;
import com.xigua.domain.bo.LastMessageContentBO;
import com.xigua.domain.dto.GroupDTO;
import com.xigua.domain.dto.GroupCallSignalDTO;
import com.xigua.domain.dto.GroupUpdateDTO;
import com.xigua.domain.entity.Group;
import com.xigua.domain.entity.GroupMember;
import com.xigua.domain.entity.User;
import com.xigua.domain.enums.ChatType;
import com.xigua.domain.enums.GroupRole;
import com.xigua.domain.enums.RedisEnum;
import com.xigua.api.service.GroupMemberService;
import com.xigua.api.service.GroupService;
import com.xigua.domain.vo.GroupDetailVO;
import com.xigua.domain.vo.GroupCallRoomVO;
import com.xigua.domain.vo.GroupCallParticipantVO;
import com.xigua.domain.vo.GroupCallSignalVO;
import com.xigua.domain.vo.GroupMemberVO;
import com.xigua.domain.vo.LastMessageVO;
import com.xigua.domain.vo.UserSearchVO;
import com.xigua.domain.ws.MessageResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.Serializable;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @ClassName GroupServiceImpl
 * @Description TODO
 * @Author wangjinfei
 * @Date 2025/7/6 11:07
 */
@Slf4j
@Service
@DubboService
public class GroupServiceImpl extends ServiceImpl<GroupMapper, Group> implements GroupService {
    @Autowired
    private Sequence sequence;
    @Autowired
    private GroupJoinRequestMapper groupJoinRequestMapper;
    @Autowired
    private RedisClient redisClient;
    @Autowired
    private NineCellImageUtil nineCellImageUtil;
    @Autowired
    private UserService userService;

    @DubboReference
    private GroupMemberService groupMemberService;
    @DubboReference
    private ChatMessageService chatMessageService;

    @Override
    public Group getById(Serializable id) {
        return super.getById(id);
    }

    /**
     * 创建群组
     * @author wangjinfei
     * @date 2025/7/6 11:07
     * @param dto
     * @return Boolean
     */
    @Override
    public Boolean createGroup(GroupDTO dto) throws Exception {
        String groupId = sequence.nextNo();
        String groupAvatar = null;
        List<String> memberIds = dto.getMemberIds();

        // 1.校验数据
        checkData(dto);

        // 2.异步生成群组头像
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            try {
                return generateGroupAvatar(memberIds);
            } catch (Exception e) {
                log.error("--------->>>>>> 生成群组头像失败，群成员:{}", memberIds, e);
            }
            return null;
        });

        // 3.添加群组
        dto.setGroupId(groupId);

        // 3.1 如果生成群组头像完成，随着群组添加头像
        if(future.isDone()){
            groupAvatar = future.get();
            dto.setGroupAvatar(groupAvatar);
        }
        Group group = addGroup(dto);

        // 4.添加群成员
        Boolean addGroupMember = groupMemberService.addGroupMember(groupId, memberIds);

        // 5.如果群组头像没有随着添加保存（1.get阻塞获取群组头像  2.并根据群组id修改群组头像）
        if(StringUtils.isEmpty(groupAvatar)){
            groupAvatar = future.get();
            if (StringUtils.isNotEmpty(groupAvatar)){
                updateGroupAvatar(groupId, groupAvatar);
            }
        }

        // 设置群组缓存 (后续可以用mq代替)
        CompletableFuture.runAsync(() -> {
            // 调用方法 缓存群组
            addGroup2Redis(groupId);
            // 调用方法 每个群成员最后一条消息为空串 （这样可以在消息列表显示该群聊）
            addLastEmptyMes2Redis(groupId);
        });

        return true;
    }

    /**
     * 生成群组头像
     * @author wangjinfei
     * @date 2025/7/12 19:31
     * @param memberIds
    */
    private String generateGroupAvatar(List<String> memberIds) throws Exception {
        List<BufferedImage> userAvatars = new ArrayList<>();

        // 获取群成员头像
        for (String memberId : memberIds) {
            String userCache = redisClient.get(RedisEnum.USER.getKey() + memberId);
            if(StringUtils.isEmpty(userCache)){
                // todo 缓存中如果没有，从数据库中获取
                continue;
            }
            User user = JSONObject.parseObject(userCache, User.class);
            if(user != null && StringUtils.isNotEmpty(user.getAvatar())){
                BufferedImage image = ImageIO.read(new URL(user.getAvatar()));
                userAvatars.add(image);
            }
        }

        // 生成群头像
        String groupAvatar = nineCellImageUtil.generateNineCellImage(userAvatars);

        return groupAvatar;
    }

    /**
     * 校验数据
     * @author wangjinfei
     * @date 2025/7/6 14:50
     * @param dto
    */
    private void checkData(GroupDTO dto){
        String userId = UserContext.get().getUserId();
        List<String> memberIds = dto.getMemberIds();

        if(CollectionUtils.isEmpty(memberIds)){
            throw new BusinessException("群组成员不能为空");
        }
        if(memberIds.size() < 3 ){
            throw new BusinessException("群组成员不能少于3人");
        }
        if(memberIds.size() > 300 ){
            throw new BusinessException("群组成员不能多于300人");
        }
        if(!memberIds.contains(userId)){
            throw new BusinessException("群组成员必须包含自己");
        }
    }

    /**
     * 添加群组
     * @author wangjinfei
     * @date 2025/7/6 11:21
     * @param dto
     * @return Boolean
    */
    private Group addGroup(GroupDTO dto){
        String userId = UserContext.get().getUserId();

        Group group = new Group();
        group.setId(dto.getGroupId());
        group.setOwnerId(userId);
        group.setGroupName(setGroupName(dto.getMemberIds()));
        if(StringUtils.isNotEmpty(dto.getGroupAvatar())){
            group.setGroupAvatar(dto.getGroupAvatar());
        }
        group.setCurrentMember(dto.getMemberIds().size());
        int insert = baseMapper.insert(group);

        return group;
    }

    /**
     * 设置群名称
     * @author wangjinfei
     * @date 2025/7/6 15:11
     * @param memberIds
     * @return String
    */
    private String setGroupName(List<String> memberIds){
        // 获取前三的成员id
        List<String> top3 = memberIds.subList(0, 3);
        StringBuilder groupName = new StringBuilder("");

        // 用前三个成员的用户名拼接
        for (int i = 0; i < top3.size(); i++) {
            String memberId = top3.get(i);
            String userCache = redisClient.get(RedisEnum.USER.getKey() + memberId);
            if(StringUtils.isEmpty(userCache)){
                // todo 缓存中如果没有，从数据库中获取
                continue;
            }

            User user = JSONObject.parseObject(userCache, User.class);
            if(user != null && StringUtils.isNotEmpty(user.getUsername())){
                groupName.append(user.getUsername());
            }

            // 拼接分隔符
            if(i != top3.size() - 1){
                groupName.append("、");
            }
        }

        return groupName.toString();
    }

    /**
     * 修改群头像
     * @author wangjinfei
     * @date 2025/7/12 20:10
     * @param groupId
     * @param groupAvatar
     * @return Boolean
    */
    private Boolean updateGroupAvatar(String groupId, String groupAvatar){
        int update = baseMapper.update(new LambdaUpdateWrapper<Group>()
                .eq(Group::getId, groupId)
                .set(Group::getGroupAvatar, groupAvatar));

        return update > 0;
    }

    /**
     * 群组添加到缓存
     * @author wangjinfei
     * @date 2025/7/28 15:28
     * @param groupId
     * @return Boolean
     */
    @Override
    public Boolean addGroup2Redis(String groupId) {
        if(StringUtils.isEmpty(groupId)){
            return false;
        }

        Group group = baseMapper.selectById(groupId);
        if(group == null){
            return false;
        }

        // 群聊信息添加缓存
        redisClient.set(RedisEnum.GROUP.getKey() + groupId, JSONObject.toJSONString(group));

        // 群成员添加缓存
        List<GroupMember> groupMembers = groupMemberService.getListByGroupId(groupId);
        if(CollectionUtils.isNotEmpty(groupMembers)){
            for (GroupMember groupMember : groupMembers) {
                String userId = groupMember.getUserId();
                LocalDateTime createTime = groupMember.getCreateTime();

                // set 缓存群成员信息
                redisClient.set(RedisEnum.GROUP_MEMBER.getKey() + groupId + "_" + userId, JSONObject.toJSONString(groupMember));

                // zset 缓存群成员id
                redisClient.zsadd(RedisEnum.GROUP_MEMBER_ID.getKey() + groupId, userId, DateUtil.toEpochMilli(createTime));
            }
        }

        return true;
    }

    /**
     * 添加最后一条空消息到缓存
     * @author wangjinfei
     * @date 2025/8/23 13:15
     * @param groupId
     * @return Boolean
     */
    @Override
    public Boolean addLastEmptyMes2Redis(String groupId) {
        String groupCache = redisClient.get(RedisEnum.GROUP.getKey() + groupId);
        if(StringUtils.isEmpty(groupCache)){
            return false;
        }

        Group group = JSONObject.parseObject(groupCache, Group.class);
        if(group == null){
            return false;
        }

        List<GroupMember> groupMembers = groupMemberService.getListByGroupId(groupId);
        if(CollectionUtils.isEmpty(groupMembers)){
            return false;
        }

        for (GroupMember groupMember : groupMembers) {
            String userId = groupMember.getUserId();

            // 映射vo
            LastMessageVO lastMessageVO = new LastMessageVO();
            lastMessageVO.setChatId(groupId);
            lastMessageVO.setChatType(ChatType.GROUP.getType());
            lastMessageVO.setChatName(group.getGroupName());
            lastMessageVO.setAvatar(group.getGroupAvatar());
            LastMessageContentBO lastMessageContentBO = new LastMessageContentBO();
            lastMessageContentBO.setContent("");
            lastMessageVO.setLastMessageContent(lastMessageContentBO);
            lastMessageVO.setUpdateTime(DateUtil.toEpochMilli(groupMember.getCreateTime()));

            // redis 最后消息
            redisClient.zsadd(RedisEnum.LAST_MES.getKey() + userId, groupId, DateUtil.toEpochMilli(groupMember.getCreateTime()));
            // redis 最后消息内容
            redisClient.hashPut(RedisEnum.LAST_MES_CONTENT.getKey() + userId, groupId, JSONObject.toJSONString(lastMessageVO));
        }
        return true;
    }

    /**
     * 获取所有群组id
     * @author wangjinfei
     * @date 2025/7/29 17:50
     * @return List<String>
     */
    @Override
    public List<String> getAllGroupId() {
        return baseMapper.getAllGroupId();
    }

    /**
     * 获取群组详情
     * @author wangjinfei
     * @date 2025/7/30 19:35
     * @param groupId
     * @return GroupDetailVO
     */
    @Override
    public GroupDetailVO getGroupDetail(String groupId) {
        String userId = UserContext.get().getUserId();

        if(StringUtils.isEmpty(groupId)){
            throw new BusinessException("群组id不能为空");
        }

        // 群详情以数据库为准：公告、描述、全员禁言是后续新增字段，
        // 旧缓存可能缺少这些字段，命中缓存会返回 null 并让公告横幅/禁言状态消失。
        // 读取后顺手回写缓存，旧缓存会在第一次打开详情时自愈。
        Group group = baseMapper.selectById(groupId);
        if (group == null) {
            throw new BusinessException("群组不存在");
        }
        redisClient.set(RedisEnum.GROUP.getKey() + groupId, JSONObject.toJSONString(group));

        GroupDetailVO groupDetailVO = new GroupDetailVO();
        groupDetailVO.setGroupId(group.getId());
        groupDetailVO.setGroupName(group.getGroupName());
        groupDetailVO.setGroupAvatar(group.getGroupAvatar());
        groupDetailVO.setDescription(group.getDescription());
        groupDetailVO.setCurrentMember(group.getCurrentMember());
        groupDetailVO.setAnnouncement(group.getAnnouncement());
        groupDetailVO.setMuteAll(group.getMuteAll() == null ? 0 : group.getMuteAll());
        groupDetailVO.setOwnerId(group.getOwnerId());
        if(group.getAnnouncementTime() != null){
            groupDetailVO.setAnnouncementTime(DateUtil.formatDateTime(group.getAnnouncementTime(), DateUtil.DATE_TIME_FORMATTER));
        }
        if(StringUtils.isNotEmpty(group.getAnnouncementEditor())){
            groupDetailVO.setAnnouncementEditor(resolveUsername(group.getAnnouncementEditor()));
        }

        // 格式化时间
        if(group.getCreateTime() != null){
            groupDetailVO.setCreateTime(DateUtil.formatDateTime(group.getCreateTime(), DateUtil.DATE_FORMATTER));
        }

        // 获取群主名称
        if(StringUtils.isNotEmpty(group.getOwnerId())){
            User user = userService.getById(group.getOwnerId());
            if(user != null){
                groupDetailVO.setGroupOwner(user.getUsername());
            }
        }

        // 获取加入时间
        LocalDateTime joinTime = groupMemberService.getJoinTime(groupId, userId);
        if(joinTime != null){
            groupDetailVO.setJoinTime(DateUtil.formatDateTime(joinTime, DateUtil.DATE_FORMATTER));
        }

        // 获取我的角色
        Integer role = groupMemberService.getGroupRole(groupId, userId);
        if(role != null){
            // 数值角色与文案一起返回，前端权限判断用 role，展示用 roleName
            groupDetailVO.setRole(role);
            GroupRole groupRoleE = GroupRole.getByType(role);
            if(groupRoleE != null){
                groupDetailVO.setRoleName(groupRoleE.getDesc());
            }
        }

        // 成员列表（带角色）：群详情页的成员网格与「设管理员 / 移出群聊」操作都基于它
        groupDetailVO.setMembers(buildMemberVOs(groupId));

        return groupDetailVO;
    }

    /**
     * 组装群成员列表（头像、角色、入群时间）
     * @author doubao
     * @date 2026/11/12
     * @param groupId 群组id
     * @return List<GroupMemberVO>
     */
    private List<GroupMemberVO> buildMemberVOs(String groupId) {
        List<GroupMember> members = groupMemberService.getListByGroupId(groupId);
        List<GroupMemberVO> result = new ArrayList<>();
        if (CollectionUtils.isEmpty(members)) {
            return result;
        }
        // 群主排在最前，管理员次之，普通成员按入群时间
        members.sort((a, b) -> {
            int ra = a.getRole() == null ? 3 : a.getRole();
            int rb = b.getRole() == null ? 3 : b.getRole();
            if (ra != rb) {
                return ra - rb;
            }
            if (a.getCreateTime() == null || b.getCreateTime() == null) {
                return 0;
            }
            return a.getCreateTime().compareTo(b.getCreateTime());
        });
        for (GroupMember member : members) {
            // 逻辑删除或已退出的成员不展示
            if (member.getDelFlag() != null && member.getDelFlag() != 0) {
                continue;
            }
            if (member.getStatus() != null && member.getStatus() == 0) {
                continue;
            }
            GroupMemberVO vo = new GroupMemberVO();
            vo.setUserId(member.getUserId());
            User user = userService.getById(member.getUserId());
            vo.setUsername(user != null && StringUtils.isNotEmpty(user.getUsername())
                    ? user.getUsername() : member.getNickname());
            vo.setAvatar(user != null ? user.getAvatar() : null);
            vo.setRole(member.getRole());
            GroupRole roleE = GroupRole.getByType(member.getRole());
            vo.setRoleName(roleE != null ? roleE.getDesc() : "");
            vo.setOwner(GroupRole.OWNER.getType().equals(member.getRole()));
            if (member.getCreateTime() != null) {
                vo.setJoinTime(DateUtil.formatDateTime(member.getCreateTime(), DateUtil.DATE_FORMATTER));
            }
            LocalDateTime muteUntil = member.getMuteUntil();
            boolean muted = muteUntil != null && muteUntil.isAfter(LocalDateTime.now());
            vo.setMuted(muted);
            if (muted) {
                vo.setMuteUntil(DateUtil.formatDateTime(muteUntil, DateUtil.DATE_FORMATTER));
            }
            result.add(vo);
        }
        return result;
    }

    /**
     * 获取群成员用户信息列表（@提及选人用）
     * @author doubao
     * @date 2026/11/4
     * @param groupId 群组id
     * @return List<UserSearchVO>
     */
    @Override
    public List<UserSearchVO> getGroupMemberUsers(String groupId) {
        if(StringUtils.isEmpty(groupId)){
            throw new BusinessException("群组id不能为空");
        }
        // 群成员名单只对群内成员开放，避免拿到任意 groupId 就能枚举别人的群成员
        String currentUserId = UserContext.get().getUserId();
        groupMemberService.getGroupRole(groupId, currentUserId);
        Set<String> memberIds = groupMemberService.getGroupMembers(groupId);
        if(CollectionUtils.isEmpty(memberIds)){
            return new ArrayList<>();
        }
        List<UserSearchVO> userVOS = new ArrayList<>();
        for (String memberId : memberIds) {
            User user = userService.getById(memberId);
            if(user == null){
                continue;
            }
            UserSearchVO userVO = new UserSearchVO();
            userVO.setId(user.getId());
            userVO.setUsername(user.getUsername());
            userVO.setAvatar(user.getAvatar());
            userVOS.add(userVO);
        }
        return userVOS;
    }

    /**
     * 根据群组id获取群组
     * @author wangjinfei
     * @date 2025/8/17 9:50
     * @return List<Group>
     */
    @Override
    public List<Group> getListByIds(List<String> groupIds) {
        List<Group> groups = baseMapper.selectList(new LambdaQueryWrapper<Group>()
                .in(Group::getId, groupIds));
        return groups;
    }

    /**
     * 退出群聊
     * 群主不能直接退出，必须走解散流程，避免出现"群没有主人"的状态。
     * @author doubao
     * @date 2026/11/6
     * @param groupId 群组id
     * @return Boolean 是否退出成功
     */
    @Override
    public Boolean quitGroup(String groupId) {
        String userId = UserContext.get().getUserId();
        if (StringUtils.isEmpty(groupId)) {
            throw new BusinessException("群组id不能为空");
        }

        Group group = getGroupOrThrow(groupId);

        // 校验是否在群内
        Integer role = groupMemberService.getGroupRole(groupId, userId);
        if (role == null) {
            throw new BusinessException("你不在该群聊中");
        }
        if (GroupRole.OWNER.getType().equals(role)) {
            throw new BusinessException("群主不能退出群聊，请选择解散群聊");
        }

        Boolean removed = groupMemberService.removeGroupMember(groupId, userId);
        if (!Boolean.TRUE.equals(removed)) {
            throw new BusinessException("退出群聊失败，请稍后重试");
        }

        // 成员数 -1（DB），再刷新群缓存
        baseMapper.update(new LambdaUpdateWrapper<Group>()
                .eq(Group::getId, groupId)
                .gt(Group::getCurrentMember, 0)
                .setSql("current_member = current_member - 1"));
        addGroup2Redis(groupId);

        // 清掉该用户会话列表里的这个群，退群后不再出现在消息列表
        redisClient.zsrem(RedisEnum.LAST_MES.getKey() + userId, groupId);
        redisClient.hashDel(RedisEnum.LAST_MES_CONTENT.getKey() + userId, groupId);

        log.info("--------->>>>>> 用户 {} 退出群聊 {}", userId, groupId);
        return true;
    }

    /**
     * 解散群聊
     * @author doubao
     * @date 2026/11/6
     * @param groupId 群组id
     * @return Boolean 是否解散成功
     */
    @Override
    public Boolean disbandGroup(String groupId) {
        String userId = UserContext.get().getUserId();
        if (StringUtils.isEmpty(groupId)) {
            throw new BusinessException("群组id不能为空");
        }

        Group group = getGroupOrThrow(groupId);
        if (!userId.equals(group.getOwnerId())) {
            throw new BusinessException("只有群主才能解散群聊");
        }

        // 先取成员列表，后面要用它清理每个人的会话缓存
        List<GroupMember> members = groupMemberService.getListByGroupId(groupId);

        // 删除全部成员 + 逻辑删除群
        groupMemberService.removeAllGroupMember(groupId);
        baseMapper.deleteById(groupId);

        // 清群缓存与成员 zset
        redisClient.del(RedisEnum.GROUP.getKey() + groupId);
        redisClient.del(RedisEnum.GROUP_MEMBER_ID.getKey() + groupId);

        // 清每个成员的会话列表缓存
        if (CollectionUtils.isNotEmpty(members)) {
            for (GroupMember member : members) {
                String memberId = member.getUserId();
                redisClient.del(RedisEnum.GROUP_MEMBER.getKey() + groupId + "_" + memberId);
                redisClient.zsrem(RedisEnum.LAST_MES.getKey() + memberId, groupId);
                redisClient.hashDel(RedisEnum.LAST_MES_CONTENT.getKey() + memberId, groupId);
            }
        }

        log.info("--------->>>>>> 群主 {} 解散群聊 {}，成员数 {}", userId, groupId, members == null ? 0 : members.size());
        return true;
    }

    /**
     * 邀请成员入群（群主或管理员可操作）
     * 除了写库写缓存，还会：刷新成员数、给新成员补会话列表、落库一条系统消息并实时推送
     * @author doubao
     * @date 2026/11/12
     * @param groupId 群组id
     * @param userIds 被邀请用户id列表
     * @return Integer 实际入群人数
     */
    @Override
    public Integer inviteMembers(String groupId, List<String> userIds) {
        String userId = UserContext.get().getUserId();
        if (StringUtils.isEmpty(groupId) || CollectionUtils.isEmpty(userIds)) {
            throw new BusinessException("群组id和邀请成员不能为空");
        }

        Group group = getGroupOrThrow(groupId);

        // 权限：群主或管理员
        Integer myRole = groupMemberService.getGroupRole(groupId, userId);
        if (myRole == null) {
            throw new BusinessException("你不在该群聊中");
        }
        if (!GroupRole.OWNER.getType().equals(myRole) && !GroupRole.GROUP_ADMIN.getType().equals(myRole)) {
            throw new BusinessException("只有群主或管理员才能邀请成员");
        }

        // 过滤已在群内 / 空 id / 自己
        Set<String> existing = new HashSet<>(groupMemberService.getGroupMembers(groupId));
        List<String> valid = new ArrayList<>();
        for (String id : userIds) {
            if (StringUtils.isEmpty(id) || id.equals(userId) || existing.contains(id) || valid.contains(id)) {
                continue;
            }
            valid.add(id);
        }
        if (valid.isEmpty()) {
            throw new BusinessException("所选用户都已在群内");
        }
        if (group.getCurrentMember() != null && group.getCurrentMember() + valid.size() > 500) {
            throw new BusinessException("群成员已达上限（500 人）");
        }

        Boolean added = groupMemberService.addGroupMember(groupId, valid);
        if (!Boolean.TRUE.equals(added)) {
            throw new BusinessException("邀请成员失败，请稍后重试");
        }

        // 成员数 + n，并刷新群缓存
        baseMapper.update(new LambdaUpdateWrapper<Group>()
                .eq(Group::getId, groupId)
                .setSql("current_member = current_member + " + valid.size()));
        addGroup2Redis(groupId);

        // 新成员的会话列表里补上这个群，刷新后能在消息列表看到
        for (String newMemberId : valid) {
            writeGroupSessionToMember(groupId, newMemberId);
        }

        // 系统消息：落库 + 实时推送，群内所有人都能看到「谁邀请了谁」
        String operatorName = resolveUsername(userId);
        List<String> names = new ArrayList<>();
        for (String id : valid) {
            names.add(resolveUsername(id));
        }
        String content = operatorName + " 邀请 " + String.join("、", names) + " 加入群聊";
        String messageId = chatMessageService.saveGroupSystemMessage(groupId, content, userId);
        pushSystemMessage(groupId, content, messageId, userId);

        log.info("--------->>>>>> 用户 {} 邀请 {} 人加入群 {}，系统消息 {}", userId, valid.size(), groupId, messageId);
        return valid.size();
    }

    /**
     * 将成员移出群聊
     * 群主可移出任何人（自己除外），管理员只能移出普通成员
     * @author doubao
     * @date 2026/11/12
     * @param groupId 群组id
     * @param targetUserId 被移出用户id
     * @return Boolean 是否移出成功
     */
    @Override
    public Boolean kickMember(String groupId, String targetUserId) {
        String userId = UserContext.get().getUserId();
        if (StringUtils.isEmpty(groupId) || StringUtils.isEmpty(targetUserId)) {
            throw new BusinessException("群组id和被移出成员不能为空");
        }
        if (userId.equals(targetUserId)) {
            throw new BusinessException("不能移出自己，请使用退出群聊");
        }

        getGroupOrThrow(groupId);

        Integer myRole = groupMemberService.getGroupRole(groupId, userId);
        if (myRole == null) {
            throw new BusinessException("你不在该群聊中");
        }
        Integer targetRole = groupMemberService.getGroupRole(groupId, targetUserId);
        if (targetRole == null) {
            throw new BusinessException("该成员不在群内");
        }

        // 权限：群主可移出管理员与普通成员；管理员只能移出普通成员
        if (GroupRole.GROUP_ADMIN.getType().equals(myRole)) {
            if (!GroupRole.MEMBER.getType().equals(targetRole)) {
                throw new BusinessException("管理员只能移出普通成员");
            }
        } else if (!GroupRole.OWNER.getType().equals(myRole)) {
            throw new BusinessException("只有群主或管理员才能移出成员");
        }

        Boolean removed = groupMemberService.removeGroupMember(groupId, targetUserId);
        if (!Boolean.TRUE.equals(removed)) {
            throw new BusinessException("移出成员失败，请稍后重试");
        }

        // 成员数 -1，刷新群缓存
        baseMapper.update(new LambdaUpdateWrapper<Group>()
                .eq(Group::getId, groupId)
                .gt(Group::getCurrentMember, 0)
                .setSql("current_member = current_member - 1"));
        addGroup2Redis(groupId);

        // 被移出的人会话列表里去掉这个群
        redisClient.zsrem(RedisEnum.LAST_MES.getKey() + targetUserId, groupId);
        redisClient.hashDel(RedisEnum.LAST_MES_CONTENT.getKey() + targetUserId, groupId);

        String content = resolveUsername(userId) + " 将 " + resolveUsername(targetUserId) + " 移出群聊";
        String messageId = chatMessageService.saveGroupSystemMessage(groupId, content, userId);
        pushSystemMessage(groupId, content, messageId, userId);

        log.info("--------->>>>>> 用户 {} 将 {} 移出群 {}，系统消息 {}", userId, targetUserId, groupId, messageId);
        return true;
    }

    /**
     * 设置成员角色（仅群主；2=管理员，3=普通成员）
     * @author doubao
     * @date 2026/11/12
     * @param groupId 群组id
     * @param targetUserId 目标用户id
     * @param role 目标角色
     * @return Boolean 是否设置成功
     */
    @Override
    public Boolean setMemberRole(String groupId, String targetUserId, Integer role) {
        String userId = UserContext.get().getUserId();
        if (StringUtils.isEmpty(groupId) || StringUtils.isEmpty(targetUserId) || role == null) {
            throw new BusinessException("群组id、成员与角色不能为空");
        }
        if (!GroupRole.GROUP_ADMIN.getType().equals(role) && !GroupRole.MEMBER.getType().equals(role)) {
            throw new BusinessException("只能设置为管理员或普通成员");
        }
        if (userId.equals(targetUserId)) {
            throw new BusinessException("不能修改自己的角色");
        }

        getGroupOrThrow(groupId);

        Integer myRole = groupMemberService.getGroupRole(groupId, userId);
        if (myRole == null) {
            throw new BusinessException("你不在该群聊中");
        }
        if (!GroupRole.OWNER.getType().equals(myRole)) {
            throw new BusinessException("只有群主才能设置管理员");
        }
        Integer targetRole = groupMemberService.getGroupRole(groupId, targetUserId);
        if (targetRole == null) {
            throw new BusinessException("该成员不在群内");
        }

        Boolean updated = groupMemberService.updateRole(groupId, targetUserId, role);
        if (!Boolean.TRUE.equals(updated)) {
            throw new BusinessException("设置角色失败，请稍后重试");
        }

        String content = resolveUsername(userId) + " 将 " + resolveUsername(targetUserId)
                + (GroupRole.GROUP_ADMIN.getType().equals(role) ? " 设为管理员" : " 取消管理员");
        String messageId = chatMessageService.saveGroupSystemMessage(groupId, content, userId);
        pushSystemMessage(groupId, content, messageId, userId);

        log.info("--------->>>>>> 群主 {} 将 {} 角色改为 {}（群 {}），系统消息 {}", userId, targetUserId, role, groupId, messageId);
        return true;
    }

    /**
     * 修改群资料（群名称 / 群头像 / 群简介）
     * 群主与管理员可改；改完刷新群缓存，并重写每个成员会话列表里的群名与头像
     * @param dto 群资料
     * @return Boolean 是否修改成功
     */
    @Override
    public Boolean updateGroupInfo(GroupUpdateDTO dto) {
        String userId = UserContext.get().getUserId();
        if (dto == null || StringUtils.isEmpty(dto.getGroupId())) {
            throw new BusinessException("群组id不能为空");
        }
        String groupId = dto.getGroupId();
        assertCanManage(groupId, userId);

        String name = StringUtils.trimToNull(dto.getGroupName());
        if (name == null) {
            throw new BusinessException("群名称不能为空");
        }
        if (name.length() > 30) {
            throw new BusinessException("群名称最多 30 个字符");
        }
        String description = dto.getDescription() == null ? "" : dto.getDescription().trim();
        if (description.length() > 200) {
            throw new BusinessException("群简介最多 200 个字符");
        }
        String avatar = StringUtils.trimToNull(dto.getGroupAvatar());
        if (avatar != null && !avatar.startsWith("http")) {
            throw new BusinessException("群头像地址不合法");
        }

        int updateRows = baseMapper.update(new LambdaUpdateWrapper<Group>()
                .eq(Group::getId, groupId)
                .set(Group::getGroupName, name)
                .set(Group::getDescription, description)
                .set(avatar != null, Group::getGroupAvatar, avatar));
        if (updateRows <= 0) {
            throw new BusinessException("群资料保存失败，请稍后重试");
        }

        // 群详情、成员缓存与会话列表都读缓存，必须刷新
        addGroup2Redis(groupId);
        refreshGroupSessions(groupId);

        String content = resolveUsername(userId) + " 更新了群资料";
        String messageId = chatMessageService.saveGroupSystemMessage(groupId, content, userId);
        pushSystemMessage(groupId, content, messageId, userId);

        log.info("--------->>>>>> 用户 {} 更新群资料 {}，名称={}，系统消息 {}", userId, groupId, name, messageId);
        return true;
    }

    /**
     * 发布 / 编辑 / 清空群公告
     * 群主与管理员可操作；保存后刷新群缓存并向群内广播系统消息
     * @param groupId 群组id
     * @param announcement 公告内容
     * @return Boolean 是否保存成功
     */
    @Override
    public Boolean updateAnnouncement(String groupId, String announcement) {
        String userId = UserContext.get().getUserId();
        if (StringUtils.isEmpty(groupId)) {
            throw new BusinessException("群组id不能为空");
        }
        assertCanManage(groupId, userId);

        String content = announcement == null ? "" : announcement.trim();
        if (content.length() > 500) {
            throw new BusinessException("群公告最多 500 个字符");
        }

        int updateRows = baseMapper.update(new LambdaUpdateWrapper<Group>()
                .eq(Group::getId, groupId)
                .set(Group::getAnnouncement, content)
                .set(Group::getAnnouncementTime, LocalDateTime.now())
                .set(Group::getAnnouncementEditor, userId));
        if (updateRows <= 0) {
            throw new BusinessException("群公告保存失败，请稍后重试");
        }

        addGroup2Redis(groupId);

        String sysContent = content.isEmpty()
                ? resolveUsername(userId) + " 删除了群公告"
                : resolveUsername(userId) + " 更新了群公告";
        String messageId = chatMessageService.saveGroupSystemMessage(groupId, sysContent, userId);
        pushSystemMessage(groupId, sysContent, messageId, userId);

        log.info("--------->>>>>> 用户 {} 更新群公告 {}，长度={}，系统消息 {}", userId, groupId, content.length(), messageId);
        return true;
    }

    /**
     * 开启 / 关闭全员禁言
     * 群主与管理员可操作；开启后普通成员的消息在 center 发送链路会被直接拦截
     * @param groupId 群组id
     * @param muteAll 0：关闭；1：开启
     * @return Boolean 是否保存成功
     */
    @Override
    public Boolean updateMuteAll(String groupId, Integer muteAll) {
        String userId = UserContext.get().getUserId();
        if (StringUtils.isEmpty(groupId)) {
            throw new BusinessException("群组id不能为空");
        }
        if (muteAll == null || (muteAll != 0 && muteAll != 1)) {
            throw new BusinessException("全员禁言状态不合法");
        }
        assertCanManage(groupId, userId);

        int updateRows = baseMapper.update(new LambdaUpdateWrapper<Group>()
                .eq(Group::getId, groupId)
                .set(Group::getMuteAll, muteAll));
        if (updateRows <= 0) {
            throw new BusinessException("全员禁言设置失败，请稍后重试");
        }
        addGroup2Redis(groupId);

        String content = resolveUsername(userId) + (muteAll == 1 ? " 开启了全员禁言" : " 关闭了全员禁言");
        String messageId = chatMessageService.saveGroupSystemMessage(groupId, content, userId);
        pushSystemMessage(groupId, content, messageId, userId);

        log.info("--------->>>>>> 用户 {} 设置群 {} 全员禁言={}，系统消息 {}", userId, groupId, muteAll, messageId);
        return true;
    }

    /**
     * 单人禁言 / 解除禁言
     * 群主可禁言管理员与普通成员，管理员只能禁言普通成员；不能禁言自己与群主
     */
    @Override
    public Boolean muteMember(String groupId, String targetUserId, Integer durationMinutes) {
        String userId = UserContext.get().getUserId();
        if (StringUtils.isEmpty(groupId) || StringUtils.isEmpty(targetUserId)) {
            throw new BusinessException("群组id和目标成员不能为空");
        }
        if (durationMinutes == null || durationMinutes < 0) {
            throw new BusinessException("禁言时长不合法");
        }
        if (durationMinutes > 5256000) {
            throw new BusinessException("禁言时长不能超过 10 年");
        }
        if (userId.equals(targetUserId)) {
            throw new BusinessException("不能禁言自己");
        }

        getGroupOrThrow(groupId);
        Integer myRole = groupMemberService.getGroupRole(groupId, userId);
        if (myRole == null) {
            throw new BusinessException("你不在该群聊中");
        }
        Integer targetRole = groupMemberService.getGroupRole(groupId, targetUserId);
        if (targetRole == null) {
            throw new BusinessException("该成员不在群内");
        }
        if (GroupRole.OWNER.getType().equals(targetRole)) {
            throw new BusinessException("不能禁言群主");
        }
        if (GroupRole.GROUP_ADMIN.getType().equals(myRole)) {
            if (!GroupRole.MEMBER.getType().equals(targetRole)) {
                throw new BusinessException("管理员只能禁言普通成员");
            }
        } else if (!GroupRole.OWNER.getType().equals(myRole)) {
            throw new BusinessException("只有群主或管理员才能禁言成员");
        }

        LocalDateTime muteUntil = durationMinutes > 0 ? LocalDateTime.now().plusMinutes(durationMinutes) : null;
        Boolean updated = groupMemberService.updateMuteUntil(groupId, targetUserId, muteUntil);
        if (!Boolean.TRUE.equals(updated)) {
            throw new BusinessException("禁言设置失败，请稍后重试");
        }

        String durationText;
        if (durationMinutes <= 0) {
            durationText = "解除禁言";
        } else if (durationMinutes % 1440 == 0) {
            durationText = "禁言 " + (durationMinutes / 1440) + " 天";
        } else if (durationMinutes % 60 == 0) {
            durationText = "禁言 " + (durationMinutes / 60) + " 小时";
        } else {
            durationText = "禁言 " + durationMinutes + " 分钟";
        }
        String content = resolveUsername(userId) + " 将 " + resolveUsername(targetUserId) + " " + durationText;
        String messageId = chatMessageService.saveGroupSystemMessage(groupId, content, userId);
        pushSystemMessage(groupId, content, messageId, userId);

        log.info("--------->>>>>> 用户 {} 对群 {} 成员 {} 执行 {}（{} 分钟）", userId, groupId, targetUserId,
                durationText, durationMinutes);
        return true;
    }

    /**
     * 转让群主（仅现任群主）
     * 原群主降为普通成员，新群主升为群主，同时刷新群缓存
     * @param groupId 群组id
     * @param targetUserId 新群主id
     * @return Boolean 是否转让成功
     */
    @Override
    public Boolean transferOwner(String groupId, String targetUserId) {
        String userId = UserContext.get().getUserId();
        if (StringUtils.isEmpty(groupId) || StringUtils.isEmpty(targetUserId)) {
            throw new BusinessException("群组id和新群主不能为空");
        }
        if (userId.equals(targetUserId)) {
            throw new BusinessException("不能转让给自己");
        }

        Group group = getGroupOrThrow(groupId);
        if (!userId.equals(group.getOwnerId())) {
            throw new BusinessException("只有群主才能转让群主");
        }
        Integer targetRole = groupMemberService.getGroupRole(groupId, targetUserId);
        if (targetRole == null) {
            throw new BusinessException("该成员不在群内");
        }

        int updateRows = baseMapper.update(new LambdaUpdateWrapper<Group>()
                .eq(Group::getId, groupId)
                .set(Group::getOwnerId, targetUserId));
        if (updateRows <= 0) {
            throw new BusinessException("转让群主失败，请稍后重试");
        }
        groupMemberService.updateRole(groupId, userId, GroupRole.MEMBER.getType());
        groupMemberService.updateRole(groupId, targetUserId, GroupRole.OWNER.getType());

        addGroup2Redis(groupId);
        refreshGroupSessions(groupId);

        String content = resolveUsername(userId) + " 将群主转让给 " + resolveUsername(targetUserId);
        String messageId = chatMessageService.saveGroupSystemMessage(groupId, content, userId);
        pushSystemMessage(groupId, content, messageId, userId);

        log.info("--------->>>>>> 群主 {} 将群 {} 转让给 {}，系统消息 {}", userId, groupId, targetUserId, messageId);
        return true;
    }

    /**
     * 校验当前用户是否群主或管理员
     */
    private void assertCanManage(String groupId, String userId) {
        Integer role = groupMemberService.getGroupRole(groupId, userId);
        if (role == null) {
            throw new BusinessException("你不在该群聊中");
        }
        if (!GroupRole.OWNER.getType().equals(role) && !GroupRole.GROUP_ADMIN.getType().equals(role)) {
            throw new BusinessException("只有群主或管理员才能修改群资料");
        }
    }

    /**
     * 群名 / 头像变更后刷新所有成员会话列表里的群会话卡片
     */
    private void refreshGroupSessions(String groupId) {
        Group group = baseMapper.selectById(groupId);
        List<GroupMember> members = groupMemberService.getListByGroupId(groupId);
        if (group == null || CollectionUtils.isEmpty(members)) {
            return;
        }
        for (GroupMember member : members) {
            String memberId = member.getUserId();
            Object cache = redisClient.hashGet(RedisEnum.LAST_MES_CONTENT.getKey() + memberId, groupId);
            if (cache == null) {
                continue;
            }
            LastMessageVO lastMessageVO = JSONObject.parseObject(String.valueOf(cache), LastMessageVO.class);
            if (lastMessageVO == null) {
                continue;
            }
            lastMessageVO.setChatName(group.getGroupName());
            lastMessageVO.setAvatar(group.getGroupAvatar());
            redisClient.hashPut(RedisEnum.LAST_MES_CONTENT.getKey() + memberId, groupId, JSONObject.toJSONString(lastMessageVO));
        }
    }

    /**
     * 把群写进指定成员的会话列表（新成员入群后消息列表能看到这个群）
     * @author doubao
     * @date 2026/11/12
     * @param groupId 群组id
     * @param memberId 成员id
     */
    private void writeGroupSessionToMember(String groupId, String memberId) {
        Group group = getGroupOrThrow(groupId);
        if (group == null) {
            return;
        }
        LastMessageVO lastMessageVO = new LastMessageVO();
        lastMessageVO.setChatId(groupId);
        lastMessageVO.setChatType(ChatType.GROUP.getType());
        lastMessageVO.setChatName(group.getGroupName());
        lastMessageVO.setAvatar(group.getGroupAvatar());
        LastMessageContentBO contentBO = new LastMessageContentBO();
        contentBO.setContent("");
        lastMessageVO.setLastMessageContent(contentBO);
        long now = System.currentTimeMillis();
        lastMessageVO.setUpdateTime(now);
        redisClient.zsadd(RedisEnum.LAST_MES.getKey() + memberId, groupId, now);
        redisClient.hashPut(RedisEnum.LAST_MES_CONTENT.getKey() + memberId, groupId, JSONObject.toJSONString(lastMessageVO));
    }

    /**
     * 群内广播一条系统消息（本节点在线的成员直推，写库已由 chatMessageService 完成）
     * @author doubao
     * @date 2026/11/12
     * @param groupId 群组id
     * @param content 文案
     * @param messageId 已落库的消息id
     * @param operatorId 操作者id
     */
    private void pushSystemMessage(String groupId, String content, String messageId, String operatorId) {
        Set<String> memberIds = groupMemberService.getGroupMembers(groupId);
        if (CollectionUtils.isEmpty(memberIds)) {
            return;
        }
        MessageResponse response = new MessageResponse();
        response.setSenderId(operatorId);
        response.setReceiverId(groupId);
        response.setMessageType("chat");
        response.setSubType("system_mes");
        response.setMessage(content);
        response.setChatType(2);
        response.setContentType(6);
        response.setChatMessageId(messageId);
        response.setCreateTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        String json = JSONObject.toJSONString(response);

        for (String memberId : memberIds) {
            Collection<WebSocketSession> sessions = WsSessionHelper.getSessions(memberId);
            if (CollectionUtils.isEmpty(sessions)) {
                continue;
            }
            for (WebSocketSession session : sessions) {
                if (session == null || !session.isOpen()) {
                    continue;
                }
                try {
                    synchronized (session) {
                        session.sendMessage(new TextMessage(json));
                    }
                } catch (Exception e) {
                    log.warn("--------->>>>>> 系统消息推送失败 memberId={}：{}", memberId, e.getMessage());
                }
            }
        }
    }

    /**
     * 取用户名（取不到时退回 id，保证系统消息文案不缺人）
     * @author doubao
     * @date 2026/11/12
     * @param userId 用户id
     * @return String
     */
    private String resolveUsername(String userId) {
        if (StringUtils.isEmpty(userId)) {
            return "某成员";
        }
        User user = userService.getById(userId);
        return user != null && StringUtils.isNotEmpty(user.getUsername()) ? user.getUsername() : userId;
    }

    /**
     * 读取群信息：缓存优先，缓存缺失时回源数据库并回写缓存
     * @author doubao
     * @date 2026/11/6
     * @param groupId 群组id
     * @return Group
     */
    private Group getGroupOrThrow(String groupId) {
        String groupCache = redisClient.get(RedisEnum.GROUP.getKey() + groupId);
        Group group = null;
        if (StringUtils.isNotEmpty(groupCache)) {
            group = JSONObject.parseObject(groupCache, Group.class);
        }
        if (group == null) {
            group = baseMapper.selectById(groupId);
            if (group == null) {
                throw new BusinessException("群组不存在");
            }
            redisClient.set(RedisEnum.GROUP.getKey() + groupId, JSONObject.toJSONString(group));
        }
        return group;
    }

    /**
     * 获取所有群组
     * @author wangjinfei
     * @date 2025/8/17 9:55
     * @return List<Group>
     */
    @Override
    public List<Group> getAllGroup() {
        List<Group> groups = baseMapper.selectList(null);
        return groups;
    }

    /**
     * 分页查询群组
     * @author wangjinfei
     * @date 2026/3/7
     * @param groupName 群组名称
     * @param ownerName 群主名称
     * @param createTime 创建时间
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return PagePlus<Group>
     */
    @Override
    public com.xigua.common.mybatis.base.PagePlus<Group> page(String groupName, String ownerName, String createTime, Integer pageNum, Integer pageSize) {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Group> page = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Group> queryWrapper = new LambdaQueryWrapper<>();

        // 群组名称筛选
        if(StringUtils.isNotEmpty(groupName) && !"null".equals(groupName)){
            queryWrapper.like(Group::getGroupName, groupName);
        }

        // 群主名称筛选（需要先查询群主ID）
        if(StringUtils.isNotEmpty(ownerName) && !"null".equals(ownerName)){
            // 先根据群主名称查询用户ID列表
            List<User> userList = userService.lambdaQuery()
                    .like(User::getUsername, ownerName)
                    .list();
            if(CollectionUtils.isNotEmpty(userList)){
                List<String> ownerIds = userList.stream()
                        .map(User::getId)
                        .collect(java.util.stream.Collectors.toList());
                queryWrapper.in(Group::getOwnerId, ownerIds);
            } else {
                // 如果找不到匹配的用户，返回空结果
                queryWrapper.isNull(Group::getId);
            }
        }

        // 创建时间筛选（yyyy-MM-dd 格式，查询当天创建的记录）
        if(StringUtils.isNotEmpty(createTime) && !"null".equals(createTime)){
            try {
                // 当天起始时间 00:00:00
                String startTime = createTime + " 00:00:00";
                // 当天结束时间 23:59:59
                String endTime = createTime + " 23:59:59";
                queryWrapper.ge(Group::getCreateTime, startTime);
                queryWrapper.le(Group::getCreateTime, endTime);
            } catch (Exception e) {
                log.warn("创建时间格式错误: {}", createTime, e);
            }
        }

        queryWrapper.orderByDesc(Group::getCreateTime);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Group> groupPage = baseMapper.selectPage(page, queryWrapper);
        return com.xigua.common.mybatis.base.PagePlus.fromPage(groupPage);
    }

    @Override
    public Long countActiveGroups() {
        List<String> activeGroupIds = chatMessageService.getActiveGroupIds();
        return (long) activeGroupIds.size();
    }

    @Override
    public Long countYesterdayActiveGroups() {
        List<String> activeGroupIds = chatMessageService.getYesterdayActiveGroupIds();
        return (long) activeGroupIds.size();
    }

    @Override
    public java.util.List<com.xigua.domain.vo.admin.ActiveGroupRankItemVO> getActiveGroupRank() {
        // 查询前5个成员数最多的群组
        List<Group> groups = baseMapper.selectList(new LambdaQueryWrapper<Group>()
            .eq(Group::getDelFlag, 0)
            .orderByDesc(Group::getCurrentMember)
            .last("LIMIT 5")
        );

        return groups.stream()
            .map(group -> {
                com.xigua.domain.vo.admin.ActiveGroupRankItemVO item = com.xigua.domain.vo.admin.ActiveGroupRankItemVO.builder()
                    .groupId(group.getId())
                    .groupName(group.getGroupName())
                    .memberCount(group.getCurrentMember())
                    .memberCountDisplay(formatMemberCount(group.getCurrentMember()))
                    .build();
                return item;
            })
            .collect(Collectors.toList());
    }

    /**
     * 格式化成员数量显示（如 12400 -> 12.4k）
     * @param count 成员数
     * @return 格式化后的字符串
     */
    private String formatMemberCount(Integer count) {
        if (count == null) {
            return "0";
        }
        if (count >= 1000) {
            return String.format("%.1fk", count / 1000.0);
        }
        return String.valueOf(count);
    }

    @Override
    public Boolean adminSetMuteAll(String groupId, Integer muteAll) {
        if (StringUtils.isEmpty(groupId)) {
            throw new BusinessException("群组id不能为空");
        }
        if (muteAll == null || (muteAll != 0 && muteAll != 1)) {
            throw new BusinessException("全员禁言状态不合法");
        }
        getGroupOrThrow(groupId);
        int rows = baseMapper.update(new LambdaUpdateWrapper<Group>()
                .eq(Group::getId, groupId)
                .set(Group::getMuteAll, muteAll));
        if (rows <= 0) {
            throw new BusinessException("全员禁言设置失败，请稍后重试");
        }
        addGroup2Redis(groupId);
        log.info("--------->>>>>> 后台设置群 {} 全员禁言={}", groupId, muteAll);
        return true;
    }

    @Override
    public Boolean adminDisbandGroup(String groupId) {
        if (StringUtils.isEmpty(groupId)) {
            throw new BusinessException("群组id不能为空");
        }
        Group group = getGroupOrThrow(groupId);
        List<GroupMember> members = groupMemberService.getListByGroupId(groupId);

        groupMemberService.removeAllGroupMember(groupId);
        baseMapper.deleteById(groupId);

        redisClient.del(RedisEnum.GROUP.getKey() + groupId);
        redisClient.del(RedisEnum.GROUP_MEMBER_ID.getKey() + groupId);
        if (CollectionUtils.isNotEmpty(members)) {
            for (GroupMember member : members) {
                String memberId = member.getUserId();
                redisClient.del(RedisEnum.GROUP_MEMBER.getKey() + groupId + "_" + memberId);
                redisClient.zsrem(RedisEnum.LAST_MES.getKey() + memberId, groupId);
                redisClient.hashDel(RedisEnum.LAST_MES_CONTENT.getKey() + memberId, groupId);
            }
        }
        log.info("--------->>>>>> 后台解散群 {}（{}），成员数 {}",
                groupId, group.getGroupName(), members == null ? 0 : members.size());
        return true;
    }

    @Override
    public Boolean adminRemoveMember(String groupId, String targetUserId) {
        if (StringUtils.isEmpty(groupId) || StringUtils.isEmpty(targetUserId)) {
            throw new BusinessException("群组id和成员id不能为空");
        }
        Group group = getGroupOrThrow(groupId);
        if (targetUserId.equals(group.getOwnerId())) {
            throw new BusinessException("不能移出群主；如需处理请先解散群聊");
        }
        Integer targetRole = groupMemberService.getGroupRole(groupId, targetUserId);
        if (targetRole == null) {
            throw new BusinessException("该成员不在群内");
        }
        Boolean removed = groupMemberService.removeGroupMember(groupId, targetUserId);
        if (!Boolean.TRUE.equals(removed)) {
            throw new BusinessException("移出成员失败，请稍后重试");
        }
        baseMapper.update(new LambdaUpdateWrapper<Group>()
                .eq(Group::getId, groupId)
                .gt(Group::getCurrentMember, 0)
                .setSql("current_member = current_member - 1"));
        addGroup2Redis(groupId);

        redisClient.del(RedisEnum.GROUP_MEMBER.getKey() + groupId + "_" + targetUserId);
        redisClient.zsrem(RedisEnum.LAST_MES.getKey() + targetUserId, groupId);
        redisClient.hashDel(RedisEnum.LAST_MES_CONTENT.getKey() + targetUserId, groupId);
        log.info("--------->>>>>> 后台将 {} 移出群 {}", targetUserId, groupId);
        return true;
    }

    @Override
    public List<GroupJoinRequestVO> searchGroups(String keyword) {
        String kw = StringUtils.trimToEmpty(keyword);
        if (StringUtils.isEmpty(kw)) {
            throw new BusinessException("请输入群号或群名称");
        }
        String me = UserContext.get().getUserId();
        List<Group> groups = lambdaQuery()
                .and(w -> w.eq(Group::getId, kw).or().like(Group::getGroupName, kw))
                .last("limit 20")
                .list();
        List<GroupJoinRequestVO> result = new ArrayList<>();
        for (Group g : groups) {
            GroupJoinRequestVO vo = new GroupJoinRequestVO();
            vo.setGroupId(g.getId());
            vo.setGroupName(g.getGroupName());
            vo.setGroupAvatar(g.getGroupAvatar());
            vo.setGroupDescription(g.getDescription());
            vo.setCurrentMember(g.getCurrentMember());
            vo.setOwnerId(g.getOwnerId());
            vo.setJoined(groupMemberService.getGroupMembers(g.getId()).contains(me));
            result.add(vo);
        }
        return result;
    }

    @Override
    public Boolean requestJoin(String groupId, String reason) {
        String me = UserContext.get().getUserId();
        if (StringUtils.isEmpty(groupId)) {
            throw new BusinessException("群号不能为空");
        }
        Group group = getGroupOrThrow(groupId);
        if (groupMemberService.getGroupMembers(groupId).contains(me)) {
            throw new BusinessException("你已在该群聊中");
        }
        // 已有待审批申请不重复提交
        Long pending = groupJoinRequestMapper.selectCount(new LambdaQueryWrapper<GroupJoinRequest>()
                .eq(GroupJoinRequest::getGroupId, groupId)
                .eq(GroupJoinRequest::getUserId, me)
                .eq(GroupJoinRequest::getStatus, 0));
        if (pending != null && pending > 0) {
            throw new BusinessException("已提交申请，请等待群主审批");
        }
        GroupJoinRequest req = new GroupJoinRequest();
        req.setId(sequence.nextNo());
        req.setGroupId(groupId);
        req.setUserId(me);
        req.setReason(StringUtils.left(StringUtils.trimToEmpty(reason), 200));
        req.setStatus(0);
        req.setCreateTime(LocalDateTime.now());
        groupJoinRequestMapper.insert(req);
        log.info("--------->>>>>> 用户 {} 申请加入群 {} 申请单 {}", me, groupId, req.getId());
        return true;
    }

    @Override
    public List<GroupJoinRequestVO> listJoinRequests(String groupId) {
        String me = UserContext.get().getUserId();
        Group group = getGroupOrThrow(groupId);
        Integer role = groupMemberService.getGroupRole(groupId, me);
        if (role == null || (!GroupRole.OWNER.getType().equals(role) && !GroupRole.GROUP_ADMIN.getType().equals(role))) {
            throw new BusinessException("只有群主或管理员才能审批入群申请");
        }
        List<GroupJoinRequest> requests = groupJoinRequestMapper.selectList(new LambdaQueryWrapper<GroupJoinRequest>()
                .eq(GroupJoinRequest::getGroupId, groupId)
                .eq(GroupJoinRequest::getStatus, 0)
                .orderByAsc(GroupJoinRequest::getCreateTime));
        List<GroupJoinRequestVO> result = new ArrayList<>();
        for (GroupJoinRequest r : requests) {
            GroupJoinRequestVO vo = new GroupJoinRequestVO();
            vo.setId(r.getId());
            vo.setGroupId(r.getGroupId());
            vo.setUserId(r.getUserId());
            vo.setReason(r.getReason());
            vo.setStatus(r.getStatus());
            vo.setCreateTime(r.getCreateTime());
            vo.setGroupName(group.getGroupName());
            vo.setGroupAvatar(group.getGroupAvatar());
            User user = userService.getById(r.getUserId());
            vo.setUsername(user == null ? r.getUserId() : user.getUsername());
            result.add(vo);
        }
        return result;
    }

    @Override
    public Boolean handleJoinRequest(String requestId, Boolean approve) {
        String me = UserContext.get().getUserId();
        if (StringUtils.isEmpty(requestId)) {
            throw new BusinessException("申请单不能为空");
        }
        GroupJoinRequest req = groupJoinRequestMapper.selectById(requestId);
        if (req == null) {
            throw new BusinessException("申请单不存在");
        }
        if (!Integer.valueOf(0).equals(req.getStatus())) {
            throw new BusinessException("该申请已处理");
        }
        Group group = getGroupOrThrow(req.getGroupId());
        Integer role = groupMemberService.getGroupRole(req.getGroupId(), me);
        if (role == null || (!GroupRole.OWNER.getType().equals(role) && !GroupRole.GROUP_ADMIN.getType().equals(role))) {
            throw new BusinessException("只有群主或管理员才能审批入群申请");
        }
        boolean pass = Boolean.TRUE.equals(approve);
        if (pass) {
            // 复用邀请入群逻辑：写成员、补会话、发系统消息
            inviteMembers(req.getGroupId(), Collections.singletonList(req.getUserId()));
        }
        groupJoinRequestMapper.update(null, new LambdaUpdateWrapper<GroupJoinRequest>()
                .eq(GroupJoinRequest::getId, requestId)
                .set(GroupJoinRequest::getStatus, pass ? 1 : 2)
                .set(GroupJoinRequest::getHandlerId, me)
                .set(GroupJoinRequest::getHandleTime, LocalDateTime.now()));
        log.info("--------->>>>>> {} 审批入群申请 {}：{}", me, requestId, pass ? "通过" : "拒绝");
        return true;
    }

    private static final long GROUP_CALL_TTL_SECONDS = 7200L;
    private static final int GROUP_CALL_MAX_MEMBERS = 4;
    // 每 2 秒轮询一次 state 作为心跳，30 秒无心跳视为掉线，释放 4 人名额
    private static final long GROUP_CALL_STALE_MILLIS = 30000L;

    private String groupCallRoomKey(String groupId) {
        return "xigua:groupcall:" + groupId;
    }

    private String groupCallMembersKey(String groupId, String callId) {
        return "xigua:groupcall:" + groupId + ":" + callId + ":members";
    }

    private String groupCallSignalKey(String groupId, String callId, String userId) {
        return "xigua:groupcall:" + groupId + ":" + callId + ":sig:" + userId;
    }

    private GroupCallRoomVO readGroupCallRoom(String groupId) {
        String json = redisClient.get(groupCallRoomKey(groupId));
        if (StringUtils.isEmpty(json)) {
            return null;
        }
        try {
            return JSONObject.parseObject(json, GroupCallRoomVO.class);
        } catch (Exception e) {
            log.warn("群通话房间数据损坏，group={}", groupId, e);
            return null;
        }
    }

    private List<GroupCallParticipantVO> readGroupCallParticipants(String groupId, String callId) {
        String memberKey = groupCallMembersKey(groupId, callId);
        Map<Object, Object> entries = redisClient.redisTemplate.opsForHash().entries(memberKey);
        List<GroupCallParticipantVO> result = new ArrayList<>();
        long now = System.currentTimeMillis();
        for (Object value : entries.values()) {
            try {
                GroupCallParticipantVO participant = JSONObject.parseObject(String.valueOf(value), GroupCallParticipantVO.class);
                if (participant == null || participant.getUserId() == null) {
                    continue;
                }
                Long lastSeen = participant.getLastSeen();
                if (lastSeen == null || now - lastSeen > GROUP_CALL_STALE_MILLIS) {
                    redisClient.redisTemplate.opsForHash().delete(memberKey, participant.getUserId());
                    log.info("--------->>>>>> 清理群通话掉线成员 {}，group={}", participant.getUserId(), groupId);
                    continue;
                }
                result.add(participant);
            } catch (Exception e) {
                log.warn("群通话成员数据损坏，group={}, call={}", groupId, callId, e);
            }
        }
        result.sort(Comparator.comparing(GroupCallParticipantVO::getJoinedAt, Comparator.nullsLast(Long::compareTo)));
        return result;
    }

    private void touchGroupCallParticipant(String groupId, String callId, String userId) {
        String memberKey = groupCallMembersKey(groupId, callId);
        Object raw = redisClient.redisTemplate.opsForHash().get(memberKey, userId);
        GroupCallParticipantVO participant = null;
        if (raw != null) {
            try {
                participant = JSONObject.parseObject(String.valueOf(raw), GroupCallParticipantVO.class);
            } catch (Exception ignored) {
            }
        }
        if (participant == null) {
            participant = buildGroupCallParticipant(userId);
        }
        participant.setLastSeen(System.currentTimeMillis());
        redisClient.redisTemplate.opsForHash().put(memberKey, userId, JSONObject.toJSONString(participant));
        redisClient.redisTemplate.expire(memberKey, GROUP_CALL_TTL_SECONDS, TimeUnit.SECONDS);
    }

    private GroupCallRoomVO buildGroupCallRoom(String groupId, GroupCallRoomVO room) {
        room.setGroupId(groupId);
        room.setParticipants(readGroupCallParticipants(groupId, room.getCallId()));
        return room;
    }

    private GroupCallParticipantVO buildGroupCallParticipant(String userId) {
        GroupCallParticipantVO participant = new GroupCallParticipantVO();
        participant.setUserId(userId);
        participant.setMuted(false);
        participant.setJoinedAt(System.currentTimeMillis());
        participant.setLastSeen(System.currentTimeMillis());
        User user = userService.getById(userId);
        if (user != null) {
            participant.setUsername(user.getUsername());
            participant.setAvatar(user.getAvatar());
        }
        return participant;
    }

    private void requireGroupMember(String groupId, String userId) {
        if (!groupMemberService.getGroupMembers(groupId).contains(userId)) {
            throw new BusinessException("你不在该群聊中");
        }
    }

    @Override
    public GroupCallRoomVO startGroupCall(String groupId, Boolean video) {
        if (StringUtils.isEmpty(groupId)) {
            throw new BusinessException("群号不能为空");
        }
        String me = UserContext.get().getUserId();
        getGroupOrThrow(groupId);
        requireGroupMember(groupId, me);
        GroupCallRoomVO existing = readGroupCallRoom(groupId);
        if (existing != null && !readGroupCallParticipants(groupId, existing.getCallId()).isEmpty()) {
            return buildGroupCallRoom(groupId, existing);
        }
        GroupCallRoomVO room = new GroupCallRoomVO();
        room.setCallId(sequence.nextNo());
        room.setGroupId(groupId);
        room.setVideo(Boolean.TRUE.equals(video));
        room.setStarterId(me);
        room.setCreateTime(System.currentTimeMillis());
        redisClient.set(groupCallRoomKey(groupId), JSONObject.toJSONString(room), GROUP_CALL_TTL_SECONDS);
        redisClient.redisTemplate.opsForHash().put(groupCallMembersKey(groupId, room.getCallId()), me,
                JSONObject.toJSONString(buildGroupCallParticipant(me)));
        redisClient.redisTemplate.expire(groupCallMembersKey(groupId, room.getCallId()), GROUP_CALL_TTL_SECONDS, TimeUnit.SECONDS);
        log.info("--------->>>>>> 用户 {} 发起群通话 {}，video={}", me, room.getCallId(), room.getVideo());
        return buildGroupCallRoom(groupId, room);
    }

    @Override
    public GroupCallRoomVO joinGroupCall(String groupId, String callId) {
        if (StringUtils.isEmpty(groupId) || StringUtils.isEmpty(callId)) {
            throw new BusinessException("群号或通话id不能为空");
        }
        String me = UserContext.get().getUserId();
        getGroupOrThrow(groupId);
        requireGroupMember(groupId, me);
        GroupCallRoomVO room = readGroupCallRoom(groupId);
        if (room == null || !callId.equals(room.getCallId())) {
            throw new BusinessException("群通话已结束");
        }
        List<GroupCallParticipantVO> participants = readGroupCallParticipants(groupId, callId);
        boolean already = participants.stream().anyMatch(p -> me.equals(p.getUserId()));
        if (!already && participants.size() >= GROUP_CALL_MAX_MEMBERS) {
            throw new BusinessException("群通话最多支持 4 人同时在线");
        }
        if (!already) {
            redisClient.redisTemplate.opsForHash().put(groupCallMembersKey(groupId, callId), me,
                    JSONObject.toJSONString(buildGroupCallParticipant(me)));
        }
        redisClient.redisTemplate.expire(groupCallMembersKey(groupId, callId), GROUP_CALL_TTL_SECONDS, TimeUnit.SECONDS);
        log.info("--------->>>>>> 用户 {} 加入群通话 {}", me, callId);
        return buildGroupCallRoom(groupId, room);
    }

    @Override
    public GroupCallRoomVO getGroupCallState(String groupId) {
        if (StringUtils.isEmpty(groupId)) {
            throw new BusinessException("群号不能为空");
        }
        String me = UserContext.get().getUserId();
        requireGroupMember(groupId, me);
        GroupCallRoomVO room = readGroupCallRoom(groupId);
        if (room == null) {
            return null;
        }
        touchGroupCallParticipant(groupId, room.getCallId(), me);
        List<GroupCallParticipantVO> participants = readGroupCallParticipants(groupId, room.getCallId());
        if (participants.isEmpty()) {
            redisClient.del(groupCallRoomKey(groupId));
            return null;
        }
        room.setParticipants(participants);
        return room;
    }

    @Override
    public Boolean leaveGroupCall(String groupId, String callId) {
        if (StringUtils.isEmpty(groupId) || StringUtils.isEmpty(callId)) {
            return false;
        }
        String me = UserContext.get().getUserId();
        redisClient.redisTemplate.opsForHash().delete(groupCallMembersKey(groupId, callId), me);
        Long remain = redisClient.redisTemplate.opsForHash().size(groupCallMembersKey(groupId, callId));
        if (remain == null || remain == 0) {
            redisClient.del(groupCallRoomKey(groupId));
        }
        log.info("--------->>>>>> 用户 {} 离开群通话 {}，剩余 {}", me, callId, remain);
        return true;
    }

    @Override
    public Boolean sendGroupCallSignal(GroupCallSignalDTO dto) {
        if (dto == null || StringUtils.isEmpty(dto.getGroupId()) || StringUtils.isEmpty(dto.getCallId())
                || StringUtils.isEmpty(dto.getType())) {
            throw new BusinessException("信令参数不完整");
        }
        String me = UserContext.get().getUserId();
        requireGroupMember(dto.getGroupId(), me);
        GroupCallRoomVO room = readGroupCallRoom(dto.getGroupId());
        if (room == null || !dto.getCallId().equals(room.getCallId())) {
            throw new BusinessException("群通话已结束");
        }
        List<GroupCallParticipantVO> participants = readGroupCallParticipants(dto.getGroupId(), dto.getCallId());
        Set<String> targets = new HashSet<>();
        if (StringUtils.isNotEmpty(dto.getTargetUserId())) {
            targets.add(dto.getTargetUserId());
        } else {
            for (GroupCallParticipantVO p : participants) {
                if (!me.equals(p.getUserId())) {
                    targets.add(p.getUserId());
                }
            }
        }
        for (String target : targets) {
            if (!participants.stream().anyMatch(p -> target.equals(p.getUserId()))) {
                continue;
            }
            GroupCallSignalVO signal = new GroupCallSignalVO();
            signal.setId(sequence.nextNo());
            signal.setFromUserId(me);
            User self = userService.getById(me);
            signal.setFromUsername(self == null ? me : self.getUsername());
            signal.setType(dto.getType());
            signal.setPayload(dto.getPayload());
            signal.setTimestamp(System.currentTimeMillis());
            String key = groupCallSignalKey(dto.getGroupId(), dto.getCallId(), target);
            redisClient.redisTemplate.opsForList().rightPush(key, JSONObject.toJSONString(signal));
            redisClient.redisTemplate.expire(key, GROUP_CALL_TTL_SECONDS, TimeUnit.SECONDS);
        }
        return true;
    }

    @Override
    public List<GroupCallSignalVO> pollGroupCallSignals(String groupId, String callId) {
        if (StringUtils.isEmpty(groupId) || StringUtils.isEmpty(callId)) {
            return Collections.emptyList();
        }
        String me = UserContext.get().getUserId();
        requireGroupMember(groupId, me);
        String key = groupCallSignalKey(groupId, callId, me);
        List<Object> values = redisClient.redisTemplate.opsForList().range(key, 0, -1);
        if (CollectionUtils.isEmpty(values)) {
            return Collections.emptyList();
        }
        redisClient.del(key);
        List<GroupCallSignalVO> result = new ArrayList<>();
        for (Object value : values) {
            try {
                result.add(JSONObject.parseObject(String.valueOf(value), GroupCallSignalVO.class));
            } catch (Exception e) {
                log.warn("群通话信令数据损坏，group={}, call={}", groupId, callId, e);
            }
        }
        return result;
    }
}
