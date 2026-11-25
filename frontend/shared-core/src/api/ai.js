export function createAiApi(http) {
	return {
		createSession(data) {
			return http.post('/ai/chat/session/create', data, { loadingText: '创建会话...' })
		},
		getHistoryMessages(data) {
			return http.post('/ai/chat/session/messages', data, { loading: false })
		},
		pageSessions(botId, data) {
			return http.post('/ai/chat/session/' + encodeURIComponent(botId), data, { loading: false })
		},
		renameSession(sessionId, title) {
			return http.post(
				'/ai/chat/session/' + encodeURIComponent(sessionId) +
					'/rename?title=' + encodeURIComponent(title),
				{},
				{ loading: false }
			)
		},
		updateSessionModel(sessionId, model) {
			return http.post(
				'/ai/chat/session/' + encodeURIComponent(sessionId) +
					'/model?model=' + encodeURIComponent(model),
				{},
				{ loading: false }
			)
		},
		generateImage(data) {
			return http.post('/ai/chat/session/image', data, { loadingText: '正在绘画...' })
		},
		smartReply(data) {
			return http.post('/ai/chat/smart-reply', data, { loading: false })
		},
		translateMessage(data) {
			return http.post('/ai/chat/translate', data, { loadingText: '翻译中...' })
		},
		deleteSession(sessionId) {
			return http.del('/ai/chat/session/' + encodeURIComponent(sessionId), {}, { loading: false })
		}
	}
}
