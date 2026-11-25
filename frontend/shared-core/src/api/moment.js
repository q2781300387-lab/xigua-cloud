export function createMomentApi(http) {
	return {
		momentList(data) {
			return http.post('/center/moment/list', data, { loading: false })
		},
		momentPublish(data) {
			return http.post('/center/moment/publish', data, { loadingText: '发表中...' })
		},
		momentLike(data) {
			return http.post('/center/moment/like', data, { loading: false })
		},
		momentComment(data) {
			return http.post('/center/moment/comment', data, { loadingText: '发送中...' })
		},
		momentDelete(data) {
			return http.post('/center/moment/delete', data, { loadingText: '删除中...' })
		},
		momentDetail(data) {
			return http.post('/center/moment/detail', data, { loading: false })
		},
		momentCommentDelete(data) {
			return http.post('/center/moment/comment/delete', data, { loadingText: '删除中...' })
		}
	}
}
