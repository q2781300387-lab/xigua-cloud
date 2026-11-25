export function createChatApi(http) {
	return {
		getLastMes(data) {
			return http.post('/center/chat/message/getLastMes', data, { loading: false })
		},
		hideChat(chatId) {
			return http.post('/center/chat/hideChat?chatId=' + encodeURIComponent(chatId), null, { loading: false })
		},
		getHistoryMes(data, options) {
			return http.post('/center/chat/message/getHistoryMes', data, { loading: false, ...(options || {}) })
		},
		getCallRecords(data) {
			return http.post('/center/chat/message/callRecords', data, { loading: false })
		},
		getCallUnread() {
			return http.post('/center/chat/message/callRecords/unreadCount', {}, { loading: false })
		},
		markCallRead() {
			return http.post('/center/chat/message/callRecords/read', {}, { loading: false })
		},
		deleteCallRecord(data) {
			return http.post('/center/chat/message/callRecords/delete', data, { loading: false })
		},
		clearCallRecords() {
			return http.post('/center/chat/message/callRecords/clear', {}, { loading: false })
		},
		getUnreadInfo() {
			return http.get('/center/chat/getUnreadInfo', {}, { loading: false })
		},
		getMessageInteractions(data) {
			return http.post('/center/chat/message/interactions', data, { loading: false })
		},
		toggleMessageReaction(data) {
			return http.post('/center/chat/message/reaction/toggle', data, { loading: false })
		},
		toggleMessageFavorite(data) {
			return http.post('/center/chat/message/favorite/toggle', data, { loading: false })
		},
		getMessageFavorites(data) {
			return http.post('/center/chat/message/favorites', data, { loading: false })
		},
		removeMessageFavorite(data) {
			return http.post('/center/chat/message/favorite/remove', data, { loading: false })
		},
		searchGlobalMessages(data) {
			return http.post('/search/chat/message/global', data, { loading: false })
		},
		searchHistoryMessages(data) {
			return http.post('/search/chat/message/search', data, { loading: false })
		},
		searchContactsGroupsFiles(keyword) {
			return http.get('/client/search/all', { keyword }, { loading: false })
		},
		getAtMeUnread(data) {
			return http.post('/center/chat/message/atMeUnread', data, { loading: false })
		},
		editMessage(data) {
			return http.post('/center/chat/message/edit', data, { loadingText: '保存中...' })
		},
		saveConversationSetting(data) {
			return http.post('/center/chat/message/conversation/setting', data, { loading: false })
		},
		getConversationSetting(data) {
			return http.post('/center/chat/message/conversation/setting/get', data, { loading: false })
		}
	}
}
