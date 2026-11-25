import { core } from '@/core/index.js'

export function getGroupDetail(groupId, options = {}) {
	return core.group.getGroupDetail(groupId, options)
}

export function getGroupMembers(groupId) {
	return core.group.getGroupMembers(groupId)
}

export function quitGroup(groupId) {
	return core.group.quitGroup(groupId)
}

export function disbandGroup(groupId) {
	return core.group.disbandGroup(groupId)
}

export function createGroup(memberIds) {
	return core.group.createGroup(memberIds)
}

export function inviteMembers(groupId, userIds) {
	return core.group.inviteMembers(groupId, userIds)
}

export function kickMember(groupId, targetUserId, options = {}) {
	return core.group.kickMember(groupId, targetUserId, options)
}

export function setMemberRole(groupId, targetUserId, role, options = {}) {
	return core.group.setMemberRole(groupId, targetUserId, role, options)
}

export function updateGroupInfo(data) {
	return core.group.updateGroupInfo(data)
}

export function updateAnnouncement(groupId, announcement) {
	return core.group.updateAnnouncement(groupId, announcement)
}

export function updateMuteAll(data) {
	return core.group.updateMuteAll(data)
}

export function muteMember(data) {
	return core.group.muteMember(data)
}

export function transferOwner(groupId, targetUserId) {
	return core.group.transferOwner(groupId, targetUserId)
}

export function getGroupAiSummary(groupId, options = {}) {
	return core.group.getGroupAiSummary(groupId, options)
}

export function getGroupFiles(groupId, keyword = '', pageNum = 1, pageSize = 20, fileType = 'all', options = {}) {
	return core.group.getGroupFiles(groupId, keyword, pageNum, pageSize, fileType, options)
}

export function saveGroupFile(data) {
	return core.group.saveGroupFile(data)
}

export function deleteGroupFile(fileId) {
	return core.group.deleteGroupFile(fileId)
}

export function createVote(data) {
	return core.group.createVote(data)
}

export function getVoteDetail(voteId) {
	return core.group.getVoteDetail(voteId)
}

export function submitVote(data) {
	return core.group.submitVote(data)
}

export function cancelVote(voteId) {
	return core.group.cancelVote(voteId)
}

export function closeVote(voteId) {
	return core.group.closeVote(voteId)
}

export function searchGroups(keyword) {
	return core.group.searchGroups(keyword)
}

export function requestJoinGroup(data) {
	return core.group.requestJoinGroup(data)
}

export function getJoinRequests(groupId) {
	return core.group.getJoinRequests(groupId)
}

export function handleJoinRequest(data) {
	return core.group.handleJoinRequest(data)
}

export function startGroupCall(groupId, video) {
	return core.group.startGroupCall(groupId, video)
}

export function joinGroupCall(groupId, callId) {
	return core.group.joinGroupCall(groupId, callId)
}

export function getGroupCallState(groupId) {
	return core.group.getGroupCallState(groupId)
}

export function leaveGroupCall(groupId, callId) {
	return core.group.leaveGroupCall(groupId, callId)
}

export function sendGroupCallSignal(data) {
	return core.group.sendGroupCallSignal(data)
}

export function pollGroupCallSignals(groupId, callId) {
	return core.group.pollGroupCallSignals(groupId, callId)
}
