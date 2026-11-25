export function createTreeholeApi(http) {
	return {
		treeholeList(params) {
			return http.get('/client/treehole/list', params, { loading: false })
		},
		treeholeAdd(data) {
			return http.post('/client/treehole/add', data, { loadingText: '发布中...' })
		},
		treeholeLike(id) {
			return http.post('/client/treehole/like', { id }, { loading: false })
		}
	}
}
