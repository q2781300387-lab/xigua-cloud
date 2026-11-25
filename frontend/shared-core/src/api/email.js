export function createEmailApi(http) {
	return {
		sendEmailCode(data, options = {}) {
			return http.post(
				'/client/email/send?email=' + encodeURIComponent((data && data.email) || ''),
				{},
				Object.assign({ loadingText: '发送中...' }, options)
			)
		},
		verifyResetCode(data) {
			return http.post('/client/email/verifyResetCode', data, { loadingText: '验证中...' })
		}
	}
}
