<template>
	<view
		class="settings-container"
		@touchstart="handleTouchStart"
		@touchmove="handleTouchMove"
		@touchend="handleTouchEnd"
	>
		<WebSocketKeeper />

		<!-- 顶部标题栏 -->
		<view class="header safe-top-bar">
			<view class="header-back" hover-class="header-back-hover" @click="handleBack">
				<image class="back-icon" src="/static/icons/arrow/left.svg" mode="aspectFit"></image>
			</view>
			<text class="header-title">设置</text>
		</view>

		<!-- 主要内容区域 -->
		<scroll-view class="main-content" scroll-y>
			<view class="content-wrapper">
				<!-- 第一组：账号 -->
				<view class="settings-group">
					<view class="setting-item setting-item-border" hover-class="setting-item-hover" @click="openSheet('account')">
						<view class="item-icon-wrapper">
							<image class="item-icon" src="/static/icons/auth/shield.svg" mode="aspectFit"></image>
						</view>
						<text class="item-title">账号与安全</text>
						<text class="item-value">{{ userInfo && userInfo.username || '' }}</text>
						<image class="item-arrow" src="/static/icons/arrow/right.svg" mode="aspectFit"></image>
					</view>

					<view class="setting-item setting-item-border" hover-class="setting-item-hover" @click="navigateTo('/pages/settings/privacy')">
						<view class="item-icon-wrapper">
							<image class="item-icon" src="/static/icons/auth/shield.svg" mode="aspectFit"></image>
						</view>
						<text class="item-title">隐私与安全</text>
						<text class="item-value">通知 / 黑名单 / 设备</text>
						<image class="item-arrow" src="/static/icons/arrow/right.svg" mode="aspectFit"></image>
					</view>

					<view class="setting-item setting-item-border" hover-class="setting-item-hover" @click="openSheet('notify')">
						<view class="item-icon-wrapper">
							<image class="item-icon" src="/static/icons/notice/bell.svg" mode="aspectFit"></image>
						</view>
						<text class="item-title">消息通知</text>
						<text class="item-value">{{ notifyStatusText }}</text>
						<image class="item-arrow" src="/static/icons/arrow/right.svg" mode="aspectFit"></image>
					</view>

					<view class="setting-item" hover-class="setting-item-hover" @click="openSheet('storage')">
						<view class="item-icon-wrapper">
							<image class="item-icon" src="/static/icons/profile/gear.svg" mode="aspectFit"></image>
						</view>
						<text class="item-title">通用与存储</text>
						<text class="item-value">{{ storageSize }}</text>
						<image class="item-arrow" src="/static/icons/arrow/right.svg" mode="aspectFit"></image>
					</view>
				</view>

				<!-- 第二组：帮助 -->
				<view class="settings-group">
					<view class="setting-item setting-item-border" hover-class="setting-item-hover" @click="openSheet('help')">
						<view class="item-icon-wrapper">
							<image class="item-icon" src="/static/icons/misc/question.svg" mode="aspectFit"></image>
						</view>
						<text class="item-title">帮助与反馈</text>
						<image class="item-arrow" src="/static/icons/arrow/right.svg" mode="aspectFit"></image>
					</view>

					<view class="setting-item" hover-class="setting-item-hover" @click="openSheet('about')">
						<view class="item-icon-wrapper">
							<image class="item-icon" src="/static/icons/auth/info.svg" mode="aspectFit"></image>
						</view>
						<text class="item-title">关于西瓜 IM</text>
						<text class="item-value">{{ appVersion }}</text>
						<image class="item-arrow" src="/static/icons/arrow/right.svg" mode="aspectFit"></image>
					</view>
				</view>

				<!-- 退出登录按钮 -->
				<view class="logout-section">
					<view class="logout-btn" hover-class="logout-btn-hover" @click="handleLogout">
						退出登录
					</view>
				</view>
			</view>
		</scroll-view>

		<!-- 通用弹层 -->
		<view v-if="activeSheet" class="sheet-mask" @click="closeSheet">
			<view class="sheet-card" @click.stop>
				<view class="sheet-handle"></view>

				<!-- 账号与安全 -->
				<view v-if="activeSheet === 'account'" class="sheet-body">
					<text class="sheet-title">账号与安全</text>
					<view class="account-block">
						<view class="account-row">
							<text class="account-label">用户名</text>
							<text class="account-value">{{ userInfo && userInfo.username || '未设置' }}</text>
						</view>
						<view class="account-row">
							<text class="account-label">微信号</text>
							<text class="account-value">xg_{{ userInfo && userInfo.id || '' }}</text>
						</view>
						<view class="account-row">
							<text class="account-label">绑定邮箱</text>
							<text class="account-value">{{ userInfo && userInfo.email || '未绑定' }}</text>
						</view>
					</view>
					<button class="sheet-primary-btn" hover-class="sheet-btn-hover" @click="navigateTo('/pages/profile/edit-profile')">编辑个人资料</button>
					<button class="sheet-secondary-btn" hover-class="sheet-secondary-hover" @click="navigateTo('/pages/profile/change-email')">更换绑定邮箱</button>
				</view>

				<!-- 通用与存储 -->
				<view v-else-if="activeSheet === 'notify'" class="sheet-body">
					<text class="sheet-title">消息通知</text>
					<view class="account-block">
						<view class="account-row">
							<text class="account-label">通知权限</text>
							<text class="account-value">{{ notifyPermissionText }}</text>
						</view>
						<view class="account-row">
							<text class="account-label">离线推送</text>
							<text class="account-value">{{ notifySupported ? (notifyEnabled ? '已开启' : '未开启') : '当前浏览器不支持' }}</text>
						</view>
					</view>
					<text class="sheet-note">开启后，即使手机锁屏或页面在后台，也能收到新消息通知。</text>
					<button class="sheet-secondary-btn" hover-class="sheet-secondary-hover" :disabled="!notifySupported || notifyBusy" @click="toggleNotify">
						{{ notifyBusy ? '处理中…' : (notifyEnabled ? '关闭通知' : '开启通知') }}
					</button>
				</view>

				<view v-else-if="activeSheet === 'storage'" class="sheet-body">
					<text class="sheet-title">通用与存储</text>
					<view class="account-block">
						<view class="account-row">
							<text class="account-label">本地缓存</text>
							<text class="account-value">{{ storageSize }}</text>
						</view>
						<view class="account-row">
							<text class="account-label">应用版本</text>
							<text class="account-value">{{ appVersion }}</text>
						</view>
					</view>
					<button class="sheet-secondary-btn" hover-class="sheet-secondary-hover" @click="refreshStorage">刷新存储信息</button>
					<text class="sheet-note">聊天记录与账号信息存储在服务器，清理本地缓存不会删除聊天记录。</text>
				</view>

				<!-- 帮助与反馈 -->
				<view v-else-if="activeSheet === 'help'" class="sheet-body">
					<text class="sheet-title">帮助与反馈</text>
					<view class="faq-list">
						<view v-for="(faq, index) in faqs" :key="index" class="faq-item">
							<text class="faq-question">{{ faq.q }}</text>
							<text class="faq-answer">{{ faq.a }}</text>
						</view>
					</view>
					<button class="sheet-secondary-btn" hover-class="sheet-secondary-hover" @click="copyFeedback">复制问题反馈方式</button>
					<text class="sheet-note">反馈时请附上账号、发生时间与操作步骤，便于定位问题。</text>
				</view>

				<!-- 关于 -->
				<view v-else-if="activeSheet === 'about'" class="sheet-body">
					<view class="about-logo">
						<text class="about-logo-text">西</text>
					</view>
					<text class="about-name">西瓜 IM</text>
					<text class="about-version">版本 {{ appVersion }}</text>
					<text class="about-desc">一个基于 Spring Cloud 微服务架构的即时通讯应用，支持单聊、群聊、好友关系与实时消息。</text>
					<view class="about-divider"></view>
					<text class="about-copyright">© 2026 西瓜 IM</text>
				</view>
			</view>
		</view>
	</view>
</template>

<script>
import swipeBack from '@/mixins/swipeBack.js';
import { logout } from '@/api/auth.js';
import config from '@/config/index.js';
import { closeWebSocket } from '@/utils/websocket.js';
import WebSocketKeeper from '@/components/WebSocketKeeper.vue'
import { subscribeWebPush, unsubscribeWebPush, pushSupported } from '@/utils/webPush.js'

export default {
	components: {
		WebSocketKeeper
	},
	mixins: [swipeBack],
	data() {
		return {
			isLoggingOut: false,
			activeSheet: '',
			userInfo: null,
			storageSize: '0 KB',
			appVersion: '1.0.0',
			notifySupported: false,
			notifyEnabled: false,
			notifyPermission: 'default',
			notifyBusy: false,
			faqs: [
				{ q: '消息一直显示“重连中”怎么办？', a: '请确认网络可用，并保持应用在前台。若长时间无法连接，退出账号后重新登录可重建连接。' },
				{ q: '收不到新消息提醒？', a: '请检查系统是否允许通知，并确认应用处于运行状态；部分浏览器在后台会限制推送。' },
				{ q: '如何添加好友或建群？', a: '在“通讯录”页点击右上角加号，可搜索用户添加好友，或选择多位好友创建群聊。' },
				{ q: '如何退出或解散群聊？', a: '进入群详情页，群主可解散群聊，普通成员可退出群聊。' }
			]
		};
	},
	computed: {
		notifyStatusText() {
			if (!this.notifySupported) return '不支持'
			return this.notifyEnabled ? '已开启' : '未开启'
		},
		notifyPermissionText() {
			if (!this.notifySupported) return '当前浏览器不支持'
			const map = { granted: '已允许', denied: '已拒绝', default: '未询问' }
			return map[this.notifyPermission] || '未询问'
		}
	},
	onLoad(options = {}) {
		this.loadUserInfo();
		this.refreshStorage();
		this.refreshNotifyState();
		if (options.panel === 'help' || options.panel === 'about') {
			setTimeout(() => {
				this.openSheet(options.panel);
			}, 300);
		}
	},
	methods: {
		loadUserInfo() {
			const raw = uni.getStorageSync('userInfo');
			if (!raw) return;
			try {
				this.userInfo = typeof raw === 'string' ? JSON.parse(raw) : raw;
			} catch (e) {
				console.error('解析用户信息失败:', e);
			}
		},
		openSheet(type) {
			this.activeSheet = type;
			if (type === 'notify') this.refreshNotifyState();
		},
		async refreshNotifyState() {
			this.notifySupported = pushSupported();
			if (!this.notifySupported) return;
			this.notifyPermission = (typeof Notification !== 'undefined' && Notification.permission) || 'default';
			try {
				const reg = await navigator.serviceWorker.getRegistration('/');
				const sub = reg ? await reg.pushManager.getSubscription() : null;
				this.notifyEnabled = !!sub && this.notifyPermission === 'granted';
			} catch (e) {
				this.notifyEnabled = false;
			}
		},
		async toggleNotify() {
			if (!this.notifySupported || this.notifyBusy) return;
			this.notifyBusy = true;
			try {
				if (this.notifyEnabled) {
					await unsubscribeWebPush();
					uni.showToast({ title: '已关闭消息通知', icon: 'none' });
				} else {
					const result = await subscribeWebPush();
					if (!result.ok) {
						const reason = result.reason;
						uni.showToast({
							title: reason === 'denied' ? '通知权限被拒绝，请在浏览器设置中允许'
								: reason === 'unsupported' ? '当前浏览器不支持消息推送'
								: '开启失败，请稍后重试',
							icon: 'none'
						});
						return;
					}
					uni.showToast({ title: '消息通知已开启', icon: 'none' });
				}
			} finally {
				this.notifyBusy = false;
				this.refreshNotifyState();
			}
		},
		closeSheet() {
			this.activeSheet = '';
		},
		navigateTo(url) {
			this.closeSheet();
			uni.navigateTo({ url });
		},
		refreshStorage() {
			try {
				const info = uni.getStorageInfoSync();
				const kb = Number(info.currentSize || 0);
				this.storageSize = kb >= 1024 ? `${(kb / 1024).toFixed(1)} MB` : `${kb} KB`;
			} catch (e) {
				this.storageSize = '0 KB';
			}
		},
		copyFeedback() {
			uni.setClipboardData({
				data: '西瓜 IM 问题反馈：账号 / 发生时间 / 操作步骤 / 预期结果',
				success: () => {
					uni.showToast({ title: '已复制反馈模板', icon: 'success' });
				}
			});
		},
		handleBack() {
			uni.navigateBack({
				fail: () => {
					uni.reLaunch({ url: '/pages/profile/profile' });
				}
			});
		},
		async handleLogout() {
			uni.showModal({
				title: '退出登录',
				content: '确定要退出当前账号吗？',
				confirmColor: '#d92d20',
				success: async (res) => {
					if (res.confirm) {
						if (this.isLoggingOut) return;

						try {
							this.isLoggingOut = true;
							closeWebSocket();
							await logout();
							uni.removeStorageSync(config.tokenKey);
							uni.removeStorageSync(config.userInfoKey);
							uni.removeStorageSync('userInfo');
							uni.reLaunch({ url: '/pages/login/login' });
						} catch (error) {
							console.error('登出失败:', error);
							uni.removeStorageSync(config.tokenKey);
							uni.removeStorageSync(config.userInfoKey);
							uni.removeStorageSync('userInfo');
							uni.reLaunch({ url: '/pages/login/login' });
						} finally {
							this.isLoggingOut = false;
						}
					}
				}
			});
		}
	}
};
</script>

<style lang="scss" scoped>
.settings-container {
	width: 100%;
	height: 100vh;
	background-color: #f2f2f7;
	display: flex;
	flex-direction: column;
	overflow: hidden;
}

.header {
	position: relative;
	height: 88rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	background: rgba(255, 255, 255, 0.78);
	backdrop-filter: saturate(180%) blur(20px);
	-webkit-backdrop-filter: saturate(180%) blur(20px);
	border-bottom: 1rpx solid rgba(60, 60, 67, 0.12);
	z-index: 10;
	flex-shrink: 0;
}

.header-back {
	position: absolute;
	left: 16rpx;
	bottom: 0;
	width: 72rpx;
	height: 88rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	border-radius: 12rpx;
}

.header-back-hover {
	background: rgba(120, 120, 128, 0.12);
}

.back-icon {
	width: 36rpx;
	height: 36rpx;
	opacity: 0.6;
}

.header-title {
	font-size: 32rpx;
	font-weight: 600;
	color: #1c1c1e;
	letter-spacing: -0.01em;
}

.main-content {
	flex: 1;
	overflow: hidden;
}

.content-wrapper {
	display: block;
	width: 100%;
	padding: 28rpx 24rpx 200rpx;
	box-sizing: border-box;
}

.settings-group {
	display: block;
	background: #ffffff;
	border-radius: 20rpx;
	border: 1rpx solid rgba(60, 60, 67, 0.12);
	overflow: hidden;
	margin-bottom: 24rpx;
	box-sizing: border-box;
}

.setting-item {
	display: flex;
	align-items: center;
	padding: 24rpx;
	background: #ffffff;
	transition: background-color 0.15s;
}

.setting-item-border {
	border-bottom: 1rpx solid rgba(60, 60, 67, 0.12);
}

.setting-item-hover {
	background: #f2f2f7;
}

.item-icon-wrapper {
	width: 64rpx;
	height: 64rpx;
	border-radius: 12rpx;
	background: #f2f2f7;
	display: flex;
	align-items: center;
	justify-content: center;
	margin-right: 20rpx;
	flex-shrink: 0;
}

.item-icon {
	width: 34rpx;
	height: 34rpx;
	opacity: 0.55;
}

.item-title {
	flex: 1;
	font-size: 28rpx;
	font-weight: 500;
	color: #1c1c1e;
}

.item-value {
	font-size: 23rpx;
	color: rgba(60, 60, 67, 0.48);
	margin-right: 10rpx;
	max-width: 240rpx;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
}

.item-arrow {
	width: 26rpx;
	height: 26rpx;
	opacity: 0.24;
	flex-shrink: 0;
}

.logout-section {
	margin-top: 32rpx;
}

.logout-btn {
	width: 100%;
	height: 88rpx;
	border-radius: 16rpx;
	background: #ffffff;
	color: #d92d20;
	font-size: 29rpx;
	font-weight: 500;
	border: 1rpx solid rgba(60, 60, 67, 0.12);
	display: flex;
	align-items: center;
	justify-content: center;
}

.logout-btn::after {
	border: 0;
}

.logout-btn-hover {
	background: #f2f2f7;
}

/* 弹层 */
.sheet-mask {
	position: fixed;
	top: 0;
	left: 0;
	right: 0;
	bottom: 0;
	background: rgba(0, 0, 0, 0.35);
	z-index: 100;
	display: flex;
	align-items: flex-end;
}

.sheet-card {
	width: 100%;
	background: #ffffff;
	border-radius: 28rpx 28rpx 0 0;
	padding: 16rpx 28rpx calc(40rpx + env(safe-area-inset-bottom));
	max-width: 750rpx;
	margin: 0 auto;
	max-height: 82vh;
	overflow-y: auto;
}

.sheet-handle {
	width: 72rpx;
	height: 8rpx;
	border-radius: 999rpx;
	background: rgba(60, 60, 67, 0.2);
	margin: 0 auto 24rpx;
}

.sheet-body {
	display: flex;
	flex-direction: column;
	max-height: 78vh;
	overflow-y: auto;
}

.sheet-title {
	font-size: 30rpx;
	font-weight: 600;
	color: #1c1c1e;
	margin-bottom: 20rpx;
}

.account-block {
	background: #f2f2f7;
	border-radius: 14rpx;
	padding: 4rpx 20rpx;
	margin-bottom: 24rpx;
}

.account-row {
	display: flex;
	align-items: center;
	justify-content: space-between;
	min-height: 80rpx;
	border-bottom: 1rpx solid rgba(60, 60, 67, 0.1);
}

.account-row:last-child {
	border-bottom: 0;
}

.account-label {
	font-size: 25rpx;
	color: rgba(60, 60, 67, 0.6);
}

.account-value {
	font-size: 25rpx;
	color: #1c1c1e;
	max-width: 420rpx;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
}

.sheet-primary-btn {
	width: 100%;
	height: 84rpx;
	line-height: 84rpx;
	border-radius: 14rpx;
	background: #111111;
	color: #ffffff;
	font-size: 27rpx;
	font-weight: 600;
	border: 0;
}

.sheet-primary-btn::after {
	border: 0;
}

.sheet-btn-hover {
	opacity: 0.86;
}

.sheet-secondary-btn {
	width: 100%;
	height: 84rpx;
	line-height: 84rpx;
	border-radius: 14rpx;
	background: #f2f2f7;
	color: #1c1c1e;
	font-size: 27rpx;
	font-weight: 500;
	border: 0;
	margin-top: 16rpx;
}

.sheet-secondary-btn::after {
	border: 0;
}

.sheet-secondary-hover {
	background: #e9e9ee;
}

.sheet-note {
	font-size: 21rpx;
	line-height: 32rpx;
	color: rgba(60, 60, 67, 0.42);
	margin-top: 20rpx;
	text-align: center;
}

.faq-list {
	margin-bottom: 16rpx;
}

.faq-item {
	padding: 20rpx 0;
	border-bottom: 1rpx solid rgba(60, 60, 67, 0.1);
	display: flex;
	flex-direction: column;
	gap: 8rpx;
}

.faq-question {
	font-size: 26rpx;
	font-weight: 600;
	color: #1c1c1e;
}

.faq-answer {
	font-size: 23rpx;
	line-height: 34rpx;
	color: rgba(60, 60, 67, 0.6);
}

.about-logo {
	width: 120rpx;
	height: 120rpx;
	border-radius: 28rpx;
	background: #111111;
	display: flex;
	align-items: center;
	justify-content: center;
	margin: 20rpx auto 18rpx;
}

.about-logo-text {
	font-size: 52rpx;
	font-weight: 600;
	color: #ffffff;
}

.about-name {
	text-align: center;
	font-size: 32rpx;
	font-weight: 600;
	color: #1c1c1e;
}

.about-version {
	text-align: center;
	font-size: 23rpx;
	color: rgba(60, 60, 67, 0.45);
	margin-top: 6rpx;
}

.about-desc {
	font-size: 23rpx;
	line-height: 36rpx;
	color: rgba(60, 60, 67, 0.6);
	margin-top: 22rpx;
	text-align: center;
}

.about-divider {
	height: 1rpx;
	background: rgba(60, 60, 67, 0.1);
	margin: 26rpx 0 18rpx;
}

.about-copyright {
	text-align: center;
	font-size: 21rpx;
	color: rgba(60, 60, 67, 0.35);
}
</style>
