export function createGroupApi(http) {
	return {
		getGroupDetail(groupId, options = {}) {
			return http.get('/client/group/getGroupDetail', { groupId }, Object.assign({ loading: false }, options))
		},
		getGroupMembers(groupId) {
			return http.get('/client/group/getGroupMembers', { groupId }, { loading: false })
		},
		quitGroup(groupId) {
			return http.post(
				'/client/group/quitGroup?groupId=' + encodeURIComponent(groupId),
				{},
				{ loadingText: '退出中...' }
			)
		},
		disbandGroup(groupId) {
			return http.post(
				'/client/group/disbandGroup?groupId=' + encodeURIComponent(groupId),
				{},
				{ loadingText: '解散中...' }
			)
		},
		createGroup(memberIds) {
			return http.post('/client/group/createGroup', { memberIds }, { loading: true })
		},
		inviteMembers(groupId, userIds) {
			return http.post('/client/group/inviteMembers', { groupId, userIds }, { loadingText: '邀请中...' })
		},
		kickMember(groupId, targetUserId, options = {}) {
			return http.post('/client/group/kickMember?groupId=' + encodeURIComponent(groupId) + '&targetUserId=' + encodeURIComponent(targetUserId), {}, Object.assign({ loadingText: '移出中...' }, options))
		},
		setMemberRole(groupId, targetUserId, role, options = {}) {
			return http.post('/client/group/setMemberRole?groupId=' + encodeURIComponent(groupId) + '&targetUserId=' + encodeURIComponent(targetUserId) + '&role=' + encodeURIComponent(role), {}, Object.assign({ loadingText: '设置中...' }, options))
		},
		updateGroupInfo(data) {
			return http.post('/client/group/updateGroupInfo', data, { loadingText: '保存中...' })
		},
		updateAnnouncement(groupId, announcement) {
			return http.post('/client/group/updateAnnouncement', { groupId, announcement }, { loadingText: '发布中...' })
		},
		updateMuteAll(data) {
			return http.post('/client/group/updateMuteAll', data, { loadingText: '设置中...' })
		},
		muteMember(data) {
			return http.post('/client/group/muteMember', data, { loadingText: '设置中...' })
		},
		transferOwner(groupId, targetUserId) {
			return http.post('/client/group/transferOwner?groupId=' + encodeURIComponent(groupId) + '&targetUserId=' + encodeURIComponent(targetUserId), {}, { loadingText: '转让中...' })
		},
		getGroupAiSummary(groupId, options = {}) {
			return http.post('/client/group/ai/summary', { groupId }, Object.assign({ loadingText: '生成中...' }, options))
		},
		getGroupFiles(groupId, keyword = '', pageNum = 1, pageSize = 20, fileType = 'all', options = {}) {
			return http.get('/client/group/file/list', { groupId, keyword, pageNum, pageSize, fileType }, Object.assign({ loading: false }, options))
		},
		saveGroupFile(data) {
			return http.post('/client/group/file/save', data, { loading: false })
		},
		deleteGroupFile(fileId) {
			return http.post('/client/group/file/delete', { fileId }, { loadingText: '删除中...' })
		},
		createVote(data) {
			return http.post('/client/group/vote/create', data, { loadingText: '发起中...' })
		},
		getVoteDetail(voteId) {
			return http.get('/client/group/vote/detail', { voteId }, { loading: false })
		},
		submitVote(data) {
			return http.post('/client/group/vote/submit', data, { loadingText: '提交中...' })
		},
		cancelVote(voteId) {
			return http.post('/client/group/vote/cancel', { voteId }, { loadingText: '取消中...' })
		},
		closeVote(voteId) {
			return http.post('/client/group/vote/close', { voteId }, { loadingText: '结束中...' })
		},
		searchGroups(keyword) {
			return http.get('/client/group/search', { keyword }, { loading: false })
		},
		requestJoinGroup(data) {
			return http.post('/client/group/joinRequest', data, { loadingText: '提交申请...' })
		},
		getJoinRequests(groupId) {
			return http.get('/client/group/joinRequests', { groupId }, { loading: false })
		},
		handleJoinRequest(data) {
			return http.post('/client/group/handleJoinRequest', data, { loadingText: '处理中...' })
		},
		startGroupCall(groupId, video) {
			return http.post('/client/group/call/start?groupId=' + encodeURIComponent(groupId) + '&video=' + (video ? 'true' : 'false'), null, { loadingText: '发起通话...' })
		},
		joinGroupCall(groupId, callId) {
			return http.post('/client/group/call/join?groupId=' + encodeURIComponent(groupId) + '&callId=' + encodeURIComponent(callId), null, { loadingText: '加入通话...' })
		},
		getGroupCallState(groupId) {
			return http.get('/client/group/call/state', { groupId }, { loading: false })
		},
		leaveGroupCall(groupId, callId) {
			return http.post('/client/group/call/leave?groupId=' + encodeURIComponent(groupId) + '&callId=' + encodeURIComponent(callId), null, { loading: false })
		},
		sendGroupCallSignal(data) {
			return http.post('/client/group/call/signal', data, { loading: false })
		},
		pollGroupCallSignals(groupId, callId) {
			return http.get('/client/group/call/signals', { groupId, callId }, { loading: false })
		}
	}
}
