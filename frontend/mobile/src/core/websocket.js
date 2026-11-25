import config from '@/config/index.js'
import { createWebSocketClient } from '@xigua/core'

function readUserId() {
	const userInfoStr = uni.getStorageSync('userInfo')
	if (!userInfoStr) return null
	try {
		const userInfo = JSON.parse(userInfoStr)
		return userInfo && userInfo.id != null ? String(userInfo.id) : null
	} catch (err) {
		console.error('解析用户信息失败:', err)
		return null
	}
}

function createUniSocket(url, handlers) {
	const task = uni.connectSocket({
		url,
		success: function () {
			console.log('WebSocket 连接请求已发送')
		},
		fail: function (err) {
			if (handlers && handlers.onCreateFail) handlers.onCreateFail(err)
		}
	})
	if (!task) return null
	task.onOpen(handlers.onOpen)
	task.onMessage(handlers.onMessage)
	task.onClose(handlers.onClose)
	task.onError(handlers.onError)
	return task
}

function sendUniSocket(task, data, callbacks) {
	task.send({
		data,
		success: function () {
			if (callbacks && callbacks.onSuccess) callbacks.onSuccess()
		},
		fail: function (err) {
			if (callbacks && callbacks.onFail) callbacks.onFail(err)
		}
	})
}

function closeUniSocket(task, callbacks) {
	task.close({
		success: function () {
			if (callbacks && callbacks.onSuccess) callbacks.onSuccess()
		},
		fail: function (err) {
			if (callbacks && callbacks.onFail) callbacks.onFail(err)
		}
	})
}

export const webSocketClient = createWebSocketClient({
	url: config.wsURL,
	getUserId: readUserId,
	getToken: function () {
		return uni.getStorageSync(config.tokenKey) || ''
	},
	createSocket: createUniSocket,
	send: sendUniSocket,
	close: closeUniSocket,
	logger: console
})
