export function createAuthApi(http) {
	return {
		login(data, options = {}) {
			return http.post('/client/auth/login', data, Object.assign({ loadingText: '登录中...' }, options))
		},
		logout() {
			return http.post('/client/auth/logout', {}, { loadingText: '退出中...' })
		},
		register(data, options = {}) {
			return http.post('/client/auth/register', data, Object.assign({ loadingText: '注册中...' }, options))
		},
		loginRisk(username) {
			return http.get('/client/auth/login-risk', { username }, { silent: true, loading: false })
		},
		captcha(username) {
			return http.get('/client/auth/captcha', { username }, { silent: true, loading: false })
		}
	}
}
