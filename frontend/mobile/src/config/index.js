// API 配置：H5 走同源，App 走独立 HTTPS 域名
const isH5Runtime = typeof window !== 'undefined' && !!window.location
const isDev = process.env.NODE_ENV === 'development'
const appOrigin = 'https://xiguaim.eu.cc'

function h5WsOrigin() {
	if (!isH5Runtime) return ''
	const protocol = window.location.protocol === 'https:' ? 'wss://' : 'ws://'
	return protocol + window.location.host
}

export default {
	// H5 部署时由同源反向代理转发 /client、/center、/ai 等请求
	baseURL: isH5Runtime ? '' : (isDev ? 'http://127.0.0.1:8090' : appOrigin),

	// WebSocket：H5 使用当前页面协议和域名，App 使用固定 HTTPS/WSS
	wsURL: isH5Runtime ? h5WsOrigin() : (isDev ? 'ws://127.0.0.1:8090' : appOrigin.replace('https://', 'wss://')),

	timeout: 10000,

	tokenKey: 'xigua-token',

	userInfoKey: 'userInfo'
}
