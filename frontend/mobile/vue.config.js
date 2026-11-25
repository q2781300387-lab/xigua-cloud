const path = require('path')
const fs = require('fs')
process.env.UNI_INPUT_DIR = path.resolve(__dirname, 'src')
process.env.UNI_CLI_CONTEXT = process.cwd()

// 本地开发时共享核心在 ../packages/xigua-core，仓库内位于 ../shared-core
const sharedCore = fs.existsSync(path.resolve(__dirname, '..', 'shared-core', 'src'))
	? path.resolve(__dirname, '..', 'shared-core', 'src')
	: path.resolve(__dirname, '..', 'packages', 'xigua-core', 'src')

module.exports = {
	devServer: {
		port: 5400,
		// 允许外部访问
		host: '0.0.0.0',
		// 自动打开浏览器
		open: false,
		// 热重载
		hot: true,
		// 禁用 HTTPS（开发环境用 HTTP 即可）
		https: false,
		// H5 与 API 同源，开发环境全部转发到本地网关
		proxy: {
			'/client': { target: 'http://127.0.0.1:8090', changeOrigin: true, ws: true },
			'/center': { target: 'http://127.0.0.1:8090', changeOrigin: true, ws: true },
			'/ai': { target: 'http://127.0.0.1:8090', changeOrigin: true, ws: true },
			'/admin': { target: 'http://127.0.0.1:8090', changeOrigin: true, ws: true },
			'/search': { target: 'http://127.0.0.1:8090', changeOrigin: true, ws: true },
			'/media': {
				target: 'http://127.0.0.1:9000',
				changeOrigin: true,
				pathRewrite: { '^/media': '' }
			}
		}
	},
	// 配置跨域代理
	transpileDependencies: ['uni-simple-router'],
	chainWebpack: (config) => {
		config.resolve.alias.set(
			'@xigua/core',
			sharedCore
		)
	}
}
