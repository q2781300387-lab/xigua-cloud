<template>
	<view class="reset-password-page">
		<view class="top-bar safe-top-bar">
			<view class="top-bar-inner">
				<view class="back-btn" @tap="goBack" hover-class="back-btn-hover">
					<image class="back-icon" src="/static/icons/arrow/left.svg" mode="aspectFit"></image>
				</view>
				<text class="page-title-bar">设置新密码</text>
				<view class="top-placeholder"></view>
			</view>
		</view>

		<!-- 顶部空白区域 -->
		<view class="top-spacer"></view>

		<!-- 主内容区 -->
		<view class="main-content">
			<view class="content-wrapper">
				<!-- 头部区域 -->
				<view class="header-section">
					<view class="icon-container">
						<!-- 盾牌锁图标 -->
						<image class="header-icon" src="/static/icons/auth/lock.svg" mode="aspectFit"></image>
					</view>
					<text class="page-desc">请设置新密码，设置成功后需要用新密码重新登录</text>
				</view>

				<!-- 表单区域 -->
				<view class="form-section">
					<!-- 新密码输入框 -->
					<view class="input-wrapper">
						<view class="input-icon-wrapper">
							<image class="input-icon" src="/static/icons/auth/lock.svg" mode="aspectFit"></image>
						</view>
						<input
							class="input-field"
							v-model="newPassword"
							type="password"
							placeholder="新密码"
							@focus="newPasswordFocused = true"
							@blur="newPasswordFocused = false"
						/>
					</view>

					<!-- 确认新密码输入框 -->
					<view class="input-wrapper">
						<view class="input-icon-wrapper">
							<image class="input-icon" src="/static/icons/auth/lock.svg" mode="aspectFit"></image>
						</view>
						<input
							class="input-field"
							v-model="confirmPassword"
							type="password"
							placeholder="确认新密码"
							@focus="confirmPasswordFocused = true"
							@blur="confirmPasswordFocused = false"
						/>
					</view>

					<!-- 密码提示 -->
					<view class="password-hint">
						<image class="hint-icon" src="/static/icons/auth/info.svg" mode="aspectFit"></image>
						<text class="hint-text">密码需包含至少 8 位字符，建议包含数字与字母</text>
					</view>
				</view>

				<!-- 确认按钮 -->
				<view class="action-section">
					<view class="confirm-btn" @click="handleConfirm">
						<text class="confirm-btn-text">确认并重置</text>
					</view>

					<!-- 帮助链接 -->
					<view class="help-section">
						<text class="help-link" @click="handleHelp">遇到问题？联系客服</text>
					</view>
				</view>
			</view>
		</view>

		<!-- 底部装饰区域 -->
		<view class="bottom-decoration">
			<view class="gradient-overlay"></view>
			<view class="blur-circle blur-circle-1"></view>
			<view class="blur-circle blur-circle-2"></view>
		</view>
	</view>
</template>

<script>
import { resetPassword } from '@/api/user.js';

export default {
	data() {
		return {
			email: '',
			resetToken: '',
			newPassword: '',
			confirmPassword: '',
			newPasswordFocused: false,
			confirmPasswordFocused: false
		};
	},
	onLoad(options) {
		// 从URL参数中获取 email 和 resetToken
		if (options.email) {
			this.email = decodeURIComponent(options.email);
		}
		if (options.resetToken) {
			this.resetToken = decodeURIComponent(options.resetToken);
		}

		// 如果没有 resetToken，说明用户直接访问此页面，跳转到找回密码页面
		if (!this.resetToken) {
			uni.showToast({
				title: '请先验证邮箱',
				icon: 'none'
			});
			setTimeout(() => {
				uni.redirectTo({
					url: '/pages/auth/forgot-password'
				});
			}, 1500);
		}
	},
	methods: {
		async handleConfirm() {
			if (!this.newPassword) {
				uni.showToast({
					title: '请输入新密码',
					icon: 'none'
				});
				return;
			}

			if (this.newPassword.length < 8) {
				uni.showToast({
					title: '密码至少需要8位字符',
					icon: 'none'
				});
				return;
			}

			if (!this.confirmPassword) {
				uni.showToast({
					title: '请确认新密码',
					icon: 'none'
				});
				return;
			}

			if (this.newPassword !== this.confirmPassword) {
				uni.showToast({
					title: '两次密码输入不一致',
					icon: 'none'
				});
				return;
			}

			try {
				// 调用重置密码接口
				await resetPassword({
					email: this.email,
					resetToken: this.resetToken,
					newPassword: this.newPassword,
					confirmPassword: this.confirmPassword
				});

				uni.showToast({
					title: '密码重置成功',
					icon: 'success'
				});

				setTimeout(() => {
					uni.reLaunch({
						url: '/pages/login/login'
					});
				}, 1500);
			} catch (error) {
				console.error('密码重置失败:', error);
			}
		},
		goBack() {
			const pages = getCurrentPages();
			if (pages.length > 1) {
				uni.navigateBack();
			} else {
				uni.reLaunch({ url: '/pages/login/login' });
			}
		},
		handleHelp() {
			uni.showModal({
				title: '客服帮助',
				content: '如需帮助，请联系在线客服或拨打客服热线。',
				showCancel: false
			});
		}
	}
};
</script>

<style lang="scss" scoped>
/* 页面容器 */
.reset-password-page {
	width: 100%;
	min-height: 100vh;
	background-color: #f2f2f7;
	display: flex;
	flex-direction: column;
	align-items: center;
	overflow: hidden;
}

/* 顶栏 */
.top-bar {
	position: fixed;
	top: 0;
	left: 0;
	right: 0;
	z-index: 50;
	background: rgba(255, 255, 255, 0.88);
	backdrop-filter: saturate(180%) blur(20px);
	-webkit-backdrop-filter: saturate(180%) blur(20px);
	border-bottom: 1rpx solid rgba(60, 60, 67, 0.12);
}

.top-bar-inner {
	height: 96rpx;
	display: flex;
	align-items: center;
	justify-content: space-between;
	padding: 0 24rpx;
}

.back-btn {
	width: 64rpx;
	height: 64rpx;
	display: flex;
	align-items: center;
	justify-content: center;
}

.back-btn-hover {
	opacity: 0.6;
}

.back-icon {
	width: 44rpx;
	height: 44rpx;
	opacity: 0.75;
}

.page-title-bar {
	font-size: 34rpx;
	font-weight: 600;
	color: #1c1c1e;
}

.top-placeholder {
	width: 64rpx;
}

/* 顶部空白区域 */
.top-spacer {
	height: 120rpx;
}

/* 主内容区 */
.main-content {
	flex: 1;
	width: 100%;
	max-width: 928rpx;
	display: flex;
	justify-content: center;
	position: relative;
	z-index: 10;
}

.content-wrapper {
	width: 100%;
	padding: 0 24rpx;
	box-sizing: border-box;
}

/* 头部区域 */
.header-section {
	display: flex;
	flex-direction: column;
	align-items: center;
	margin-bottom: 44rpx;
}

.icon-container {
	width: 128rpx;
	height: 104rpx;
	background: #ffffff;
	border: 1rpx solid rgba(60, 60, 67, 0.12);
	border-radius: 50%;
	display: flex;
	align-items: center;
	justify-content: center;
	margin-bottom: 24rpx;
}

.header-icon {
	width: 44rpx;
	height: 44rpx;
	color: #111111;
}

.page-title {
	font-family: 'Manrope', -apple-system, BlinkMacSystemFont, sans-serif;
	font-size: 44rpx;
	font-weight: 600;
	line-height: 1.2;
	letter-spacing: -0.02em;
	color: #1c1c1e;
	margin-bottom: 24rpx;
}

.page-desc {
	font-size: 24rpx;
	color: rgba(60, 60, 67, 0.55);
	text-align: center;
	line-height: 1.5;
	padding: 0 32rpx;
}

/* 表单区域 */
.form-section {
	display: flex;
	flex-direction: column;
	gap: 20rpx;
}

.input-wrapper {
	position: relative;
	display: flex;
	align-items: center;
	height: 88rpx;
	background-color: #ffffff;
	border: 1rpx solid rgba(60, 60, 67, 0.12);
	border-radius: 14rpx;
	box-sizing: border-box;
	transition: all 0.3s;
}

.input-icon-wrapper {
	position: absolute;
	left: 24rpx;
	display: flex;
	align-items: center;
	pointer-events: none;
}

.input-icon {
	width: 32rpx;
	height: 32rpx;
	color: rgba(60, 60, 67, 0.45);
}

.input-field {
	flex: 1;
	height: 100%;
	padding: 0 24rpx 0 68rpx;
	background: transparent;
	border: none;
	font-size: 27rpx;
	color: #1c1c1e;
}

.input-field::placeholder {
	color: rgba(60, 60, 67, 0.6);
}

/* 密码提示 */
.password-hint {
	display: flex;
	align-items: flex-start;
	gap: 16rpx;
	padding: 8rpx;
}

.hint-icon {
	width: 32rpx;
	height: 32rpx;
	color: rgba(60, 60, 67, 0.6);
	margin-top: 4rpx;
	flex-shrink: 0;
}

.hint-text {
	flex: 1;
	font-size: 24rpx;
	line-height: 1.4;
	color: rgba(63, 72, 81, 1);
}

/* 确认按钮区域 */
.action-section {
	margin-top: 32rpx;
}

.confirm-btn {
	width: 100%;
	height: 88rpx;
	background-color: #111111;
	border-radius: 14rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	gap: 16rpx;
	box-shadow: 0 16rpx 40rpx rgba(17, 17, 17, 0.25);
	transition: all 0.2s;
}

.confirm-btn:active {
	opacity: 0.9;
	transform: scale(0.98);
}

.confirm-btn-text {
	font-family: 'Manrope', -apple-system, BlinkMacSystemFont, sans-serif;
	font-size: 29rpx;
	font-weight: 600;
	color: #ffffff;
}

/* 帮助区域 */
.help-section {
	margin-top: 32rpx;
	display: flex;
	justify-content: center;
}

.help-link {
	font-size: 28rpx;
	font-weight: 500;
	color: #111111;
	transition: opacity 0.2s;
}

.help-link:active {
	opacity: 0.7;
}

/* 底部装饰区域 */
.bottom-decoration {
	display: none;
	position: relative;
	width: 100%;
	height: 384rpx;
	pointer-events: none;
}

.gradient-overlay {
	position: absolute;
	inset: 0;
	bottom: 0;
	height: 512rpx;
	background: linear-gradient(to top, rgba(17, 17, 17, 0.1), transparent);
}

.blur-circle {
	position: absolute;
	border-radius: 50%;
	filter: blur(120rpx);
}

.blur-circle-1 {
	bottom: -20%;
	right: -10%;
	width: 512rpx;
	height: 512rpx;
	background-color: rgba(17, 17, 17, 0.05);
}

.blur-circle-2 {
	bottom: -10%;
	left: -5%;
	width: 384rpx;
	height: 384rpx;
	background-color: rgba(17, 17, 17, 0.05);
}
</style>
