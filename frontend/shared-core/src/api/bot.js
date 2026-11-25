export function createBotApi(http) {
	return {
		getBots() {
			return http.post('/ai/bot/getBots', {}, { loading: false })
		},
		getBotDetail(botId) {
			return http.post(
				'/ai/bot/getBotDetail?botId=' + encodeURIComponent(botId),
				{},
				{ loading: false }
			)
		},
		createBot(data) {
			return http.post('/ai/bot/createBot', data, { loadingText: '创建中...' })
		},
		updateBot(botId, data) {
			return http.post(
				'/ai/bot/updateBot?botId=' + encodeURIComponent(botId),
				data,
				{ loadingText: '保存中...' }
			)
		},
		deleteBot(botId) {
			return http.post(
				'/ai/bot/deleteBot?botId=' + encodeURIComponent(botId),
				{},
				{ loadingText: '删除中...' }
			)
		}
	}
}
