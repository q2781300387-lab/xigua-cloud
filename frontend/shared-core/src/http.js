/**
 * 框架无关的 HTTP 客户端。
 * 平台通过 transport 注入 uni.request / axios / fetch，通过 hooks 处理 loading、提示和登录失效。
 */
export function createHttpClient(options) {
	const config = options || {}
	const baseURL = config.baseURL || ''
	const defaultTimeout = config.timeout || 10000
	const tokenKey = config.tokenKey || 'xigua-token'
	const isSuccess = config.isSuccess || function (data) {
		return !!(data && (data.code === 200 || data.code === 0))
	}

	function joinURL(base, url) {
		if (!base) return url
		if (/^https?:\/\//i.test(url)) return url
		return String(base).replace(/\/+$/, '') + '/' + String(url).replace(/^\/+/, '')
	}

	function request(requestOptions) {
		const token = config.getToken ? config.getToken(requestOptions) : ''
		const header = Object.assign(
			{ 'Content-Type': 'application/json' },
			token ? { [tokenKey]: token } : {},
			requestOptions.header || {}
		)

		const transportOptions = {
			url: joinURL(baseURL, requestOptions.url),
			method: requestOptions.method || 'GET',
			timeout: requestOptions.timeout || defaultTimeout,
			header,
			data: requestOptions.data || {},
			responseType: requestOptions.responseType,
			withCredentials: requestOptions.withCredentials,
			onUploadProgress: requestOptions.onUploadProgress,
			// 上传取消依赖 AbortController；必须透传到 axios 等 transport
			signal: requestOptions.signal
		}

		return Promise.resolve()
			.then(function () {
				if (config.onBeforeRequest) config.onBeforeRequest(requestOptions)
				return config.transport(transportOptions)
			})
			.then(function (res) {
				if (config.onAfterRequest) config.onAfterRequest(requestOptions)

				const statusCode = res && res.statusCode
				const data = res && res.data

				if (statusCode === 401) {
					if (config.onUnauthorized) {
						config.onUnauthorized((data && data.msg) || '未授权，请登录')
					}
					throw data || res
				}

				if (data && data.code === 401) {
					if (config.onUnauthorized) config.onUnauthorized(data.msg)
					throw data
				}

				if (statusCode === 200 && isSuccess(data)) {
					return data
				}

				if (statusCode === 200) {
					if (config.onBusinessError) config.onBusinessError(data, requestOptions)
					throw data
				}

				if (config.onHttpError) config.onHttpError(res, requestOptions)
				throw res
			}, function (err) {
				if (config.onAfterRequest) config.onAfterRequest(requestOptions)
				if (config.onNetworkError) config.onNetworkError(err, requestOptions)
				throw err
			})
	}

	return {
		request,
		get: function (url, data, requestOptions) {
			return request(Object.assign({ url, method: 'GET', data }, requestOptions || {}))
		},
		post: function (url, data, requestOptions) {
			return request(Object.assign({ url, method: 'POST', data }, requestOptions || {}))
		},
		put: function (url, data, requestOptions) {
			return request(Object.assign({ url, method: 'PUT', data }, requestOptions || {}))
		},
		del: function (url, data, requestOptions) {
			return request(Object.assign({ url, method: 'DELETE', data }, requestOptions || {}))
		}
	}
}
