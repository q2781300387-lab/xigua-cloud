// 生产包本地验收服务：静态托管 dist/build/h5，并把 API/WS/媒体转发到本地后端
const path = require('path')
const express = require('express')
const { createProxyMiddleware } = require('http-proxy-middleware')

const app = express()
const apiProxy = createProxyMiddleware({
	target: 'http://127.0.0.1:8090',
	changeOrigin: true,
	ws: true
})
const mediaProxy = createProxyMiddleware({
	target: 'http://127.0.0.1:9000',
	changeOrigin: true,
	pathRewrite: { '^/media': '' }
})

app.use(['/client', '/center', '/ai', '/admin', '/search'], apiProxy)
app.use('/media', mediaProxy)
app.use(express.static(path.join(__dirname, 'dist/build/h5')))

// H5 路由回退
app.get('*', (req, res) => {
	res.sendFile(path.join(__dirname, 'dist/build/h5/index.html'))
})

const server = app.listen(5401, '0.0.0.0', () => {
	console.log('prod preview: http://127.0.0.1:5401')
})
server.on('upgrade', apiProxy.upgrade)
