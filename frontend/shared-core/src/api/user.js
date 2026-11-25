export function createUserApi(http) {
	return {
		getUserInfo() {
			return http.get('/client/user/getUserInfo', {}, { loading: false })
		},
		resetPassword(data) {
			return http.post('/client/user/resetPassword', data, { loadingText: '重置中...' })
		},
		updateUserInfo(data) {
			return http.post('/client/user/updateUserInfo', data, { loadingText: '保存中...' })
		},
		uploadAvatar(avatar) {
			return http.post(
				'/client/user/uploadAvatar?avatar=' + encodeURIComponent(avatar),
				{},
				{ loading: false }
			)
		},
		getListByName(username) {
			return http.post(
				'/client/user/getListByName?username=' + encodeURIComponent(username),
				{},
				{ loading: false }
			)
		}
	}
}
