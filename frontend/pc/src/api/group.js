import request from '@/utils/request'
import { core } from '@/core'

// 创建群组
export function createGroup(data) {
    return core.group.createGroup(data && data.memberIds ? data.memberIds : data)
}

// 获取群组详情；config 可传 { silent: true } 关闭全局错误提示
export function getGroupDetail(groupId, config) {
    return core.group.getGroupDetail(groupId, config)
}

// 获取群成员列表
export function getGroupMembers(groupId) {
    return core.group.getGroupMembers(groupId)
}

// 群聊 AI 摘要与待办（PC 独有）
export function getGroupAiSummary(groupId) {
    return request({
        url: '/client/group/ai/summary',
        method: 'post',
        data: { groupId }
    })
}

// 群文件列表（PC 独有）
export function getGroupFiles(groupId, keyword = '', pageNum = 1, pageSize = 20, fileType = 'all') {
    return request({
        url: '/client/group/file/list',
        method: 'get',
        params: { groupId, keyword, fileType, pageNum, pageSize }
    })
}

// 登记群文件（PC 独有）
export function saveGroupFile(data) {
    return request({
        url: '/client/group/file/save',
        method: 'post',
        data
    })
}

// 删除群文件（PC 独有）
export function deleteGroupFile(fileId) {
    return request({
        url: '/client/group/file/delete',
        method: 'post',
        data: { fileId }
    })
}

// 邀请成员入群（PC 独有）
export function inviteMembers(groupId, userIds) {
    return request({
        url: '/client/group/inviteMembers',
        method: 'post',
        data: { groupId, userIds }
    })
}

// 将成员移出群聊（PC 独有）
export function kickMember(groupId, targetUserId, config = {}) {
    return request(Object.assign({
        url: '/client/group/kickMember?groupId=' + groupId + '&targetUserId=' + targetUserId,
        method: 'post',
    }, config || {}))
}

// 设置成员角色（PC 独有）
export function setMemberRole(groupId, targetUserId, role) {
    return request({
        url: '/client/group/setMemberRole?groupId=' + groupId + '&targetUserId=' + targetUserId + '&role=' + role,
        method: 'post',
    })
}

// 修改群资料（PC 独有）
export function updateGroupInfo(data) {
    return request({
        url: '/client/group/updateGroupInfo',
        method: 'post',
        data: data
    })
}

// 发布 / 编辑 / 清空群公告（PC 独有）
export function updateAnnouncement(groupId, announcement) {
    return request({
        url: '/client/group/updateAnnouncement',
        method: 'post',
        data: { groupId, announcement }
    })
}

// 全员禁言（PC 独有）
export function updateMuteAll(data) {
    return request({
        url: '/client/group/updateMuteAll',
        method: 'post',
        data
    })
}

// 单人禁言 / 解除禁言（PC 独有）
export function muteMember(data) {
    return request({
        url: '/client/group/muteMember',
        method: 'post',
        data
    })
}

// 转让群主（PC 独有）
export function transferOwner(groupId, targetUserId) {
    return request({
        url: '/client/group/transferOwner?groupId=' + groupId + '&targetUserId=' + targetUserId,
        method: 'post',
    })
}

// 退出群聊
export function quitGroup(groupId) {
    return core.group.quitGroup(groupId)
}

// 解散群聊
export function disbandGroup(groupId) {
    return core.group.disbandGroup(groupId)
}

// 发起群投票（PC 独有）
export function createVote(data) {
    return request({
        url: '/client/group/vote/create',
        method: 'post',
        data: data,
    })
}

// 投票详情与结果（PC 独有）
export function getVoteDetail(voteId) {
    return request({
        url: '/client/group/vote/detail?voteId=' + voteId,
        method: 'get',
    })
}

// 提交投票（PC 独有）
export function submitVote(data) {
    return request({
        url: '/client/group/vote/submit',
        method: 'post',
        data: data,
    })
}

// 取消投票（PC 独有）
export function cancelVote(voteId) {
    return request({
        url: '/client/group/vote/cancel',
        method: 'post',
        data: { voteId: voteId },
    })
}

// 结束投票（PC 独有）
export function closeVote(voteId) {
    return request({
        url: '/client/group/vote/close',
        method: 'post',
        data: { voteId: voteId },
    })
}

// 按群号/群名称搜索群聊
export function searchGroups(keyword) {
    return request({
        url: '/client/group/search',
        method: 'get',
        params: { keyword },
    })
}

// 提交入群申请
export function requestJoinGroup(data) {
    return request({
        url: '/client/group/joinRequest',
        method: 'post',
        data,
    })
}

// 查询待审批入群申请
export function getJoinRequests(groupId) {
    return request({
        url: '/client/group/joinRequests',
        method: 'get',
        params: { groupId },
    })
}

// 审批入群申请
export function handleJoinRequest(data) {
    return request({
        url: '/client/group/handleJoinRequest',
        method: 'post',
        data,
    })
}

// 发起群通话
export function startGroupCall(groupId, video) {
    return request({
        url: '/client/group/call/start',
        method: 'post',
        params: { groupId, video },
    })
}

// 加入群通话
export function joinGroupCall(groupId, callId) {
    return request({
        url: '/client/group/call/join',
        method: 'post',
        params: { groupId, callId },
    })
}

// 查询群通话状态
export function getGroupCallState(groupId) {
    return request({
        url: '/client/group/call/state',
        method: 'get',
        params: { groupId },
    })
}

// 离开群通话
export function leaveGroupCall(groupId, callId) {
    return request({
        url: '/client/group/call/leave',
        method: 'post',
        params: { groupId, callId },
    })
}

// 发送群通话信令
export function sendGroupCallSignal(data) {
    return request({
        url: '/client/group/call/signal',
        method: 'post',
        data,
    })
}

// 拉取群通话信令
export function pollGroupCallSignals(groupId, callId) {
    return request({
        url: '/client/group/call/signals',
        method: 'get',
        params: { groupId, callId },
    })
}
