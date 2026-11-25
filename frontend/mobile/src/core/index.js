import config from '@/config/index.js'
import { createHttpClient, createCore, getFileType } from '@xigua/core'
import { normalizeAssetUrlsDeep } from '@/utils/media.js'

let loadingCount = 0
let handlingUnauthorized = false

function showLoading(options) {
	if (options.loading === false) return
	loadingCount += 1
	if (loadingCount === 1) {
		uni.showLoading({
			title: options.loadingText || '加载中...',
			mask: true
		})
	}
}

function hideLoading(options) {
	if (options.loading === false) return
	loadingCount = Math.max(0, loadingCount - 1)
	if (loadingCount === 0) uni.hideLoading()
}

function handleUnauthorized(msg) {
	if (handlingUnauthorized) return
	handlingUnauthorized = true
	uni.showToast({
		title: msg || '登录已过期，请重新登录',
		icon: 'none',
		duration: 1500
	})
	uni.removeStorageSync(config.tokenKey)
	uni.removeStorageSync(config.userInfoKey)
	uni.removeStorageSync('userInfo')
	setTimeout(function () {
		handlingUnauthorized = false
		uni.reLaunch({ url: '/pages/login/login' })
	}, 1500)
}

function transport(options) {
	return new Promise(function (resolve, reject) {
		uni.request({
			url: options.url,
			method: options.method,
			timeout: options.timeout,
			header: options.header,
			data: options.data,
			success: function (res) {
				resolve({ statusCode: res.statusCode, data: normalizeAssetUrlsDeep(res.data) })
			},
			fail: reject
		})
	})
}

export const http = createHttpClient({
	baseURL: config.baseURL,
	timeout: config.timeout,
	tokenKey: config.tokenKey,
	getToken: function () {
		return uni.getStorageSync(config.tokenKey) || ''
	},
	transport,
	onBeforeRequest: showLoading,
	onAfterRequest: hideLoading,
	onUnauthorized: handleUnauthorized,
	onBusinessError: function (data, requestOptions) {
		if (requestOptions && requestOptions.silent) return
		uni.showToast({
			title: (data && data.msg) || '请求失败',
			icon: 'none'
		})
	},
	onHttpError: function (res, requestOptions) {
		if (requestOptions && requestOptions.silent) return
		uni.showToast({
			title: '请求失败(' + (res && res.statusCode) + ')',
			icon: 'none'
		})
	},
	onNetworkError: function (err, requestOptions) {
		if (requestOptions && requestOptions.silent) return
		uni.showToast({
			title: '网络连接失败',
			icon: 'none'
		})
	}
})

export const core = createCore(http)

export { getFileType }
