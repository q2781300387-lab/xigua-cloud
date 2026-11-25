export function createContactApi(http) {
	return {
		firstLetterFriends() {
			return http.get('/client/contact/firstLetterFriends', {}, { loading: false })
		},
		getSendFriendRequestList() {
			return http.get('/client/contact/getSendFriendRequestList', {}, { loading: false })
		},
		getReceiveFriendRequestList() {
			return http.get('/client/contact/getReceiveFriendRequestList', {}, { loading: false })
		},
		getGroupList() {
			return http.get('/client/contact/getGroupList', {}, { loading: false })
		},
		getContactCount() {
			return http.get('/client/contact/getContactCount', {}, { loading: false })
		}
	}
}
