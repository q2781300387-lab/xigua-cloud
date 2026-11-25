export function createPrivacyApi(http) {
	return {
		getSettings() {
			return http.get('/client/privacy/settings', {}, { loading: false })
		},
		saveSettings(data) {
			return http.put('/client/privacy/settings', data, { loadingText: '保存中...' })
		},
		getBlocks() {
			return http.get('/client/privacy/blocks', {}, { loading: false })
		},
		block(targetId) {
			return http.post('/client/privacy/blocks', { targetId }, { loadingText: '处理中...' })
		},
		unblock(targetId) {
			return http.del('/client/privacy/blocks/' + targetId, {}, { loadingText: '处理中...' })
		},
		isBlocked(targetId) {
			return http.get('/client/privacy/blocks/' + targetId + '/exists', {}, { loading: false, silent: true })
		},
		getDevices() {
			return http.get('/client/privacy/devices', {}, { loading: false })
		},
		kickOthers() {
			return http.post('/client/privacy/devices/kick-others', {}, { loadingText: '处理中...' })
		},
		report(data) {
			return http.post('/client/privacy/report', data, { loadingText: '提交中...' })
		},
		exportData() {
			return http.get('/client/privacy/export', {}, { loading: false })
		},
		deactivate(data) {
			return http.post('/client/privacy/deactivate', data, { loadingText: '注销中...' })
		}
	}
}
