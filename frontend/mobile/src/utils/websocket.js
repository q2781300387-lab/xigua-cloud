import { webSocketClient } from '@/core/websocket.js'

export function getWebSocketClient() {
	return webSocketClient
}

export function connectWebSocket() {
	webSocketClient.connect()
}

export function closeWebSocket() {
	webSocketClient.close()
}

export function subscribeMessage(listener) {
	webSocketClient.subscribe(listener)
}

export function unsubscribeMessage(listener) {
	webSocketClient.unsubscribe(listener)
}

export function sendWebSocketMessage(message) {
	return webSocketClient.sendMessage(message)
}

export function getWebSocketStatus() {
	return {
		isConnected: webSocketClient.isConnected(),
		reconnectAttempts: webSocketClient.getReconnectAttempts(),
		isReconnecting: webSocketClient.isReconnecting()
	}
}

export default webSocketClient
