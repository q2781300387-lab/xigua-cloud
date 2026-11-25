import { getWebSocketClient } from './websocket.js'

let listenerInstalled = false
let activeCall = null
let pendingIntent = null
let lastIncomingKey = ''
let lastIncomingAt = 0

const INTENT_TTL = 10000

export function createCallId() {
	return 'call_' + Date.now() + '_' + Math.random().toString(36).slice(2, 8)
}

/**
 * 记录一次“即将打开通话页”的意图。
 * 通话页 onLoad 时消费；刷新后模块变量已被重置，消费不到即判定为
 * 非正常入口，避免 URL 里的 mode=outgoing 触发自动重拨。
 */
export function prepareCallIntent(intent) {
	if (!intent || !intent.callId) {
		pendingIntent = null
		return
	}
	pendingIntent = {
		callId: String(intent.callId),
		mode: intent.mode === 'incoming' ? 'incoming' : 'outgoing',
		peerId: intent.peerId == null ? '' : String(intent.peerId),
		at: Date.now()
	}
}

export function consumeCallIntent(callId, mode) {
	const intent = pendingIntent
	pendingIntent = null
	if (!intent) return null
	if (Date.now() - intent.at > INTENT_TTL) return null
	if (String(intent.callId) !== String(callId)) return null
	if (intent.mode !== mode) return null
	return intent
}

export function clearCallIntent(callId) {
	if (!pendingIntent) return
	if (!callId || pendingIntent.callId === String(callId)) pendingIntent = null
}

export function parseWsPayload(raw) {
	if (!raw) return null
	if (typeof raw === 'object') return raw
	try {
		return JSON.parse(raw)
	} catch (err) {
		return null
	}
}

export function getCurrentUserId() {
	const raw = uni.getStorageSync('userInfo')
	if (!raw) return ''
	try {
		const user = typeof raw === 'string' ? JSON.parse(raw) : raw
		return user && user.id != null ? String(user.id) : ''
	} catch (err) {
		return ''
	}
}

export function setActiveCall(call) {
	activeCall = call || null
}

export function clearActiveCall(callId) {
	if (!callId || (activeCall && activeCall.callId === callId)) {
		activeCall = null
	}
}

export function getActiveCall() {
	return activeCall
}

export function sendVideoSignal(subType, options) {
	const opts = options || {}
	const client = getWebSocketClient()
	if (!client.isConnected || !client.isConnected()) {
		client.connect()
	}
	const frame = {
		messageType: 'video_call',
		subType: subType,
		senderId: String(opts.senderId || getCurrentUserId()),
		receiverId: String(opts.receiverId || ''),
		callId: String(opts.callId || ''),
		message: opts.message == null ? '' : opts.message,
		timestamp: Date.now()
	}
	return client.sendMessage(frame)
}

/**
 * 全局来电监听。必须在页面栈之外安装，保证用户在任何页面都能收到来电。
 */
export function installVideoCallListener() {
	if (listenerInstalled) return
	listenerInstalled = true

	getWebSocketClient().subscribe(function (raw) {
		const msg = parseWsPayload(raw)
		if (!msg || msg.messageType !== 'video_call') return
		if (msg.subType !== 'video_call_incoming') return
		if (!msg.callId || !msg.senderId) return

		const key = String(msg.callId) + '_' + String(msg.senderId)
		const now = Date.now()
		if (key === lastIncomingKey && now - lastIncomingAt < 800) return
		lastIncomingKey = key
		lastIncomingAt = now

		// 正在通话中，直接拒绝第二通来电，避免覆盖当前通话
		if (activeCall && activeCall.callId && activeCall.callId !== String(msg.callId)) {
			if (msg.senderId) {
				sendVideoSignal('video_call_rejected', {
					receiverId: String(msg.senderId),
					callId: String(msg.callId),
					message: 'busy'
				})
			}
			return
		}

		const pages = typeof getCurrentPages === 'function' ? getCurrentPages() : []
		const current = pages.length ? pages[pages.length - 1] : null
		if (current && current.route === 'pages/call/video-call') return

		const sender = msg.sender || {}
		const name = sender.nickname || sender.username || sender.name || '好友'
		const avatar = sender.avatar || ''
		const url = '/pages/call/video-call?mode=incoming'
			+ '&callId=' + encodeURIComponent(String(msg.callId))
			+ '&userId=' + encodeURIComponent(String(msg.senderId))
			+ '&name=' + encodeURIComponent(name)
			+ '&avatar=' + encodeURIComponent(avatar)

		prepareCallIntent({
			callId: msg.callId,
			mode: 'incoming',
			peerId: msg.senderId
		})
		uni.navigateTo({
			url: url,
			fail: function () {
				clearCallIntent(msg.callId)
				activeCall = null
			}
		})
	})
}
