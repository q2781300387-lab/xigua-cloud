<script>
import config from '@/config/index.js'
import { installVideoCallListener } from '@/utils/videoCallBridge.js'
import { subscribeWebPush } from '@/utils/webPush.js'

// 不需要登录的页面白名单
const whiteList = [
	'/pages/login/login',
	'/pages/register/register',
	'/pages/auth/forgot-password',
	'/pages/auth/reset-password'
]

// 检查是否需要登录
function checkAuth(url) {
	const token = uni.getStorageSync(config.tokenKey)

	// 如果有 token，直接通过
	if (token) {
		return true
	}

	// 没有 token，检查是否在白名单中
	const isInWhiteList = whiteList.some(path => url.startsWith(path))

	if (isInWhiteList) {
		return true
	}

	// 未登录且不在白名单，跳转到登录页
	uni.reLaunch({
		url: '/pages/login/login'
	})
	return false
}

// 保存原始的导航方法
const originalNavigateTo = uni.navigateTo
const originalRedirectTo = uni.redirectTo
const originalSwitchTab = uni.switchTab
const originalReLaunch = uni.reLaunch

// 重写导航方法，添加登录拦截
uni.navigateTo = function(options) {
	if (checkAuth(options.url)) {
		return originalNavigateTo.call(this, options)
	}
}

uni.redirectTo = function(options) {
	if (checkAuth(options.url)) {
		return originalRedirectTo.call(this, options)
	}
}

uni.switchTab = function(options) {
	if (checkAuth(options.url)) {
		return originalSwitchTab.call(this, options)
	}
}

uni.reLaunch = function(options) {
	if (checkAuth(options.url)) {
		return originalReLaunch.call(this, options)
	}
}

export default {
	onLaunch: function() {
		console.log('App Launch')

		// 全局视频来电监听：任意页面都能接到来电
		installVideoCallListener()

		// 已登录时尝试恢复/创建 Web Push 订阅，让页面关闭后仍能收到消息通知
		if (uni.getStorageSync(config.tokenKey)) {
			setTimeout(() => {
				subscribeWebPush().catch(() => {})
			}, 1500)
		}

		// 获取当前页面栈
		const pages = getCurrentPages()
		if (pages.length > 0) {
			const currentPage = pages[pages.length - 1]
			const currentUrl = '/' + currentPage.route
			checkAuth(currentUrl)
		}
	},
	onShow: function() {
		console.log('App Show')
	},
	onHide: function() {
		console.log('App Hide')
	}
}
</script>

<style lang="scss">
	@import './styles/ios-theme.scss';

	/*每个页面公共css */

	/* 安全区域：顶部导航栏适配状态栏高度，防止内容被状态栏遮挡 */
	.safe-top-bar {
		padding-top: var(--status-bar-height) !important;
	}

	/* 桌面浏览器打开时：按手机宽度居中显示，避免移动端页面被全宽拉伸 */
	@media screen and (min-width: 480px) {
		body {
			background: #eef0f3;
		}

		uni-page,
		uni-page-head,
		uni-page-body {
			max-width: 480px;
			margin: 0 auto;
		}

		uni-page-body {
			box-shadow: 0 0 48px rgba(0, 0, 0, 0.08);
		}
	}
</style>
