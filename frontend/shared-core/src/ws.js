const DEFAULT_RETRY_DELAY = [2000, 5000, 10000, 30000, 60000]

/**
 * 框架无关的 WebSocket 客户端。
 * 平台通过 createSocket / send / close 注入 uni.connectSocket 或浏览器 WebSocket。
 */
export function createWebSocketClient(options) {
	const config = options || {}
	const baseURL = config.url || ''
	const heartbeatInterval = config.heartbeatInterval || 60000
	const maxRetries = config.maxRetries || 5
	const retryDelay = config.retryDelay || DEFAULT_RETRY_DELAY
	const logger = config.logger || console

	let socket = null
	let reconnectAttempts = 0
	let retryTimer = null
	let heartbeatTimer = null
	let reconnecting = false
	let manuallyClosed = false
	let connected = false
	const messageListeners = new Set()
	const connectionListeners = new Set()

	function emitConnectionState(event) {
		connectionListeners.forEach(function (listener) {
			try {
				listener(event)
			} catch (err) {
				errorLog('连接状态监听器执行出错:', err)
			}
		})
	}

	function log() {
		if (logger && logger.log) logger.log.apply(logger, arguments)
	}

	function warn() {
		if (logger && logger.warn) logger.warn.apply(logger, arguments)
	}

	function errorLog() {
		if (logger && logger.error) logger.error.apply(logger, arguments)
	}

	function readUserId() {
		if (config.getUserId) return config.getUserId()
		return null
	}

	function buildSocketUrl(userId) {
		const token = config.getToken ? config.getToken() : ''
		return baseURL + '/client/connect/' + encodeURIComponent(String(userId)) +
			(token ? '?token=' + encodeURIComponent(token) : '')
	}

	function handleOpen() {
		log('WebSocket 连接已建立')
		connected = true
		reconnectAttempts = 0
		reconnecting = false
		emitConnectionState({ state: 'open', attempt: 0, max: maxRetries })
	}

	function handleMessage(res) {
		const data = res && Object.prototype.hasOwnProperty.call(res, 'data') ? res.data : res
		log('收到 WebSocket 消息:', data)
		messageListeners.forEach(function (listener) {
			try {
				listener(data)
			} catch (err) {
				errorLog('消息监听器执行出错:', err)
			}
		})
	}

	function handleClose() {
		log('WebSocket 连接已关闭')
		connected = false
		socket = null
		if (!manuallyClosed) {
			if (reconnectAttempts < maxRetries) {
				emitConnectionState({ state: 'closed', attempt: reconnectAttempts, max: maxRetries })
			}
			reconnect()
		}
	}

	function handleError(err) {
		errorLog('WebSocket 发生错误:', err)
		connected = false
		const current = socket
		socket = null
		if (current && config.close) config.close(current, {})
		if (!manuallyClosed) reconnect()
	}

	function handleCreateFail(err) {
		errorLog('WebSocket 连接失败:', err)
		socket = null
		connected = false
		if (!manuallyClosed) reconnect()
	}

	function initWebSocket(userId) {
		if (socket && connected) {
			log('WebSocket 已连接，无需重复连接')
			return
		}
		if (socket && !connected && !manuallyClosed) {
			log('WebSocket 正在连接中...')
			return
		}

		manuallyClosed = false
		const wsUrl = buildSocketUrl(userId)
		log('正在连接 WebSocket:', wsUrl)

		socket = config.createSocket(wsUrl, {
			onOpen: handleOpen,
			onMessage: handleMessage,
			onClose: handleClose,
			onError: handleError,
			onCreateFail: handleCreateFail
		})
	}

	function sendMessage(message) {
		if (!socket || !connected) {
			warn('WebSocket 未连接，无法发送消息')
			return false
		}
		try {
			const data = typeof message === 'string' ? message : JSON.stringify(message)
			config.send(socket, data, {
				onSuccess: function () {
					log('消息发送成功')
				},
				onFail: function (err) {
					errorLog('消息发送失败:', err)
				}
			})
			return true
		} catch (err) {
			errorLog('发送消息时出错:', err)
			return false
		}
	}

	function startHeartbeat() {
		if (heartbeatTimer) clearInterval(heartbeatTimer)
		heartbeatTimer = setInterval(function () {
			if (!connected || !socket) return
			const userId = readUserId()
			if (!userId) return
			const heartbeat = config.heartbeatFactory
				? config.heartbeatFactory(userId)
				: {
					senderId: String(userId),
					message: 'ping',
					messageType: 'heart_beat',
					subType: 'ping',
					createTime: Date.now()
				}
			sendMessage(heartbeat)
			log('发送心跳包')
		}, heartbeatInterval)
	}

	function connect() {
		if (connected || reconnecting || (socket && !manuallyClosed)) {
			return
		}
		const userId = readUserId()
		if (!userId) {
			log('未登录，无法连接 WebSocket')
			return
		}
		initWebSocket(userId)
		startHeartbeat()
	}

	function reconnect() {
		if (manuallyClosed) {
			log('已主动关闭连接，不进行重连')
			return
		}
		if (reconnecting) {
			log('正在重连中，跳过本次重连')
			return
		}
		if (connected) {
			log('连接正常，无需重连')
			return
		}
		if (reconnectAttempts >= maxRetries) {
			log('达到最大重连次数，停止重连')
			emitConnectionState({ state: 'failed', attempt: reconnectAttempts, max: maxRetries })
			return
		}

		const delay = retryDelay[reconnectAttempts] || retryDelay[retryDelay.length - 1]
		reconnectAttempts += 1
		reconnecting = true
		log('准备重连 WebSocket：第 ' + reconnectAttempts + ' 次，' + (delay / 1000) + '秒后重试')
		emitConnectionState({
			state: 'reconnecting',
			attempt: reconnectAttempts,
			max: maxRetries,
			delay: delay
		})

		if (retryTimer) clearTimeout(retryTimer)
		retryTimer = setTimeout(function () {
			reconnecting = false
			const userId = readUserId()
			if (!userId) {
				log('用户信息不存在，停止重连')
				return
			}
			initWebSocket(userId)
		}, delay)
	}

	function close() {
		log('主动关闭 WebSocket 连接')
		manuallyClosed = true
		connected = false
		const current = socket
		socket = null
		if (current && config.close) {
			config.close(current, {
				onSuccess: function () {
					log('WebSocket 已关闭')
				},
				onFail: function (err) {
					errorLog('关闭 WebSocket 失败:', err)
				}
			})
		}
		if (retryTimer) {
			clearTimeout(retryTimer)
			retryTimer = null
		}
		if (heartbeatTimer) {
			clearInterval(heartbeatTimer)
			heartbeatTimer = null
		}
		reconnectAttempts = 0
		reconnecting = false
	}

	function subscribe(listener) {
		if (typeof listener === 'function') {
			messageListeners.add(listener)
			log('添加消息订阅，当前订阅数:', messageListeners.size)
		}
	}

	function unsubscribe(listener) {
		if (listener) {
			messageListeners.delete(listener)
			log('取消消息订阅，当前订阅数:', messageListeners.size)
		}
	}

	function subscribeConnectionState(listener) {
		if (typeof listener === 'function') {
			connectionListeners.add(listener)
		}
	}

	function unsubscribeConnectionState(listener) {
		if (listener) {
			connectionListeners.delete(listener)
		}
	}

	function clearSubscribers() {
		messageListeners.clear()
		log('已清空所有消息订阅')
	}

	function isConnected() {
		return connected && socket !== null
	}

	function getReconnectAttempts() {
		return reconnectAttempts
	}

	function isReconnecting() {
		return reconnecting
	}

	function getMaxRetries() {
		return maxRetries
	}

	function getReadyState() {
		if (!socket) return 3
		if (connected) return 1
		if (typeof socket.readyState === 'number') return socket.readyState
		// 适配器没有 readyState 时，socket 已建立但尚未 open，视为 CONNECTING
		return 0
	}

	function getInstance() {
		return socket
	}

	return {
		connect,
		initWebSocket,
		sendMessage,
		subscribe,
		unsubscribe,
		subscribeConnectionState,
		unsubscribeConnectionState,
		clearSubscribers,
		close,
		isConnected,
		getReconnectAttempts,
		isReconnecting,
		getMaxRetries,
		getReadyState,
		getInstance,
		reconnect,
		startHeartbeat
	}
}
