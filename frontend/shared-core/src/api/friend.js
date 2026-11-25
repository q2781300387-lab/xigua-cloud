export function createFriendApi(http) {
	return {
		getFriendDetail(friendId) {
			return http.get('/client/friend/relation/getFriendDetail', { friendId }, { loading: false })
		},
		delFriend(friendId) {
			return http.post(
				'/client/friend/relation/delFriend?friendId=' + encodeURIComponent(friendId),
				{},
				{ loadingText: '删除中...' }
			)
		},
		sendFriendRequest(data) {
			return http.post('/client/friend/relation/sendFriendRequest', data, {
				loadingText: '发送中...'
			})
		},
		friendVerify(data) {
			return http.post('/client/friend/relation/friendVerify', data, {
				loadingText: '处理中...'
			})
		}
	}
}
