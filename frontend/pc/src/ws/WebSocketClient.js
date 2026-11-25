import { createWebSocketClient } from '@xigua/core'
import { getObject, setObject } from '@/utils/localStorage'
import { getToken } from '@/utils/auth'
import { normalizeSameOriginText } from '@/utils/imageFallback'

/**
 * PC 端 WebSocket 适配层。
 * 连接、重连、心跳、订阅和消息分发由 @xigua/core 负责，
 * 这里只注入浏览器 WebSocket、本地登录态和 PC 原有日志行为。
 */

const WS_BASE_URL =
    import.meta.env.VITE_WS_BASE_URL ||
    `${window.location.protocol === 'https:' ? 'wss:' : 'ws:'}//${window.location.host}`

function readUserId() {
    const userInfo = getObject('userInfo')
    return userInfo && userInfo.id ? String(userInfo.id) : ''
}

function createSocket(url, handlers) {
    const socket = new WebSocket(url)
    socket.onopen = handlers.onOpen
    socket.onmessage = event => handlers.onMessage(normalizeSameOriginText(event.data))
    socket.onclose = handlers.onClose
    socket.onerror = error => handlers.onError(error)
    return socket
}

function sendSocket(socket, data, callbacks = {}) {
    socket.send(data)
    if (callbacks.onSuccess) callbacks.onSuccess()
}

function closeSocket(socket, callbacks = {}) {
    socket.close()
    if (callbacks.onSuccess) callbacks.onSuccess()
}

const client = createWebSocketClient({
    url: WS_BASE_URL,
    getUserId: readUserId,
    getToken,
    createSocket,
    send: sendSocket,
    close: closeSocket,
    logger: console
})

class WebSocketClient {
    /**
     * 对外入口：建立连接
     */
    connect() {
        if (readUserId()) {
            client.connect()
            return
        }

        // 兜底：localStorage 里没有 userInfo（但 cookie 仍有登录态）时，先补取一次用户信息
        this.fetchUserInfo().then(info => {
            if (!info || !info.id) {
                console.log('未登录，无法连接 WebSocket')
                return
            }
            client.connect()
        })
    }

    /**
     * 连接前兜底获取用户信息：PC 用 cookie 判断登录态，但连 WS 需要 userInfo.id
     */
    async fetchUserInfo() {
        try {
            const { getUserInfo } = await import('@/api/user')
            const res = await getUserInfo()
            const payload = (res && (res.data || res.userInfo || res)) || null
            const info = payload && payload.id ? payload : null
            if (info) {
                setObject('userInfo', info)
                return info
            }
            return null
        } catch (e) {
            console.warn('WebSocket 连接前补取用户信息失败', e)
            return null
        }
    }

    initWebSocket() {
        client.connect()
    }

    sendMessage(message) {
        return client.sendMessage(message)
    }

    subscribe(listener) {
        client.subscribe(listener)
    }

    unsubscribe(listener) {
        client.unsubscribe(listener)
    }

    subscribeConnectionState(listener) {
        client.subscribeConnectionState(listener)
    }

    unsubscribeConnectionState(listener) {
        client.unsubscribeConnectionState(listener)
    }

    close() {
        client.close()
    }

    getInstance() {
        return client.getInstance()
    }

    getReadyState() {
        return client.getReadyState()
    }

    getReconnectAttempts() {
        return client.getReconnectAttempts()
    }

    isReconnecting() {
        return client.isReconnecting()
    }

    getMaxRetries() {
        return client.getMaxRetries()
    }

    reconnect() {
        client.reconnect()
    }

    startHeartbeat() {
        client.startHeartbeat()
    }
}

/** =======================
 * 单例导出
 ======================= */

const webSocketClient = new WebSocketClient()

export function getWebSocketClient() {
    return webSocketClient
}

export function getSocketInstance() {
    return webSocketClient.getInstance()
}
