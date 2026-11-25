<template>
	<view v-show="false"></view>
</template>

<script>
	import { connectWebSocket, getWebSocketClient } from '@/utils/websocket.js'

	export default {
		name: 'WebSocketKeeper',
		mounted() {
			const wsClient = getWebSocketClient()
			if (!wsClient.isConnected()) {
				console.log('[WebSocketKeeper] 检测到未连接，建立连接')
				connectWebSocket()
			}
		}
	}
</script>
