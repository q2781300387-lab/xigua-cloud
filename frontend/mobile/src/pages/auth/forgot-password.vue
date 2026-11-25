<template>
	<view class="forgot-password-page">
		<view class="top-bar safe-top-bar">
			<view class="top-bar-inner">
				<view class="back-btn" @tap="goBack" hover-class="back-btn-hover">
					<image class="back-icon" src="/static/icons/arrow/left.svg" mode="aspectFit"></image>
				</view>
				<text class="page-title-bar">找回密码</text>
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
						<!-- 邮件图标 -->
						<image class="header-icon" src="/static/icons/auth/mail.svg" mode="aspectFit"></image>
					</view>
					<text class="page-desc">请输入你的注册邮箱，我们会发送验证码用于重置密码</text>
				</view>

				<!-- 表单区域 -->
				<view class="form-section">
					<!-- 邮箱输入框 -->
					<view class="input-wrapper">
						<view class="input-icon-wrapper">
							<image class="input-icon" src="/static/icons/auth/mail.svg" mode="aspectFit"></image>
						</view>
						<input
							class="input-field"
							v-model="email"
							type="text"
							placeholder="邮箱地址"
							@focus="emailFocused = true"
							@blur="emailFocused = false"
						/>
					</view>

					<!-- 验证码输入框和按钮 -->
					<view class="verify-row">
						<view class="input-wrapper verify-input">
							<view class="input-icon-wrapper">
								<image class="input-icon" src="/static/icons/auth/shield-gray.svg" mode="aspectFit"></image>
							</view>
							<input
								class="input-field verify-code-input"
								v-model="verifyCode"
								type="text"
								placeholder="验证码"
								maxlength="6"
								@focus="verifyCodeFocused = true"
								@blur="verifyCodeFocused = false"
							/>
						</view>
						<view class="verify-btn" :class="{ 'btn-disabled': countdown > 0 }" @click="handleGetCode">
							<text class="btn-text">{{ countdown > 0 ? `${countdown}秒后重试` : '获取验证码' }}</text>
						</view>
					</view>

					<!-- 下一步按钮 -->
					<view class="next-btn-wrapper">
						<view class="next-btn" @click="handleNext">
							<text class="next-btn-text">下一步</text>
						</view>
					</view>
				</view>

				<!-- 帮助链接 -->
				<view class="help-section">
					<text class="help-link" @click="handleHelp">遇到问题？联系客服</text>
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
import { sendEmailCode, verifyResetCode } from '@/api/email.js';

export default {
	data() {
		return {
			email: '',
			verifyCode: '',
			emailFocused: false,
			verifyCodeFocused: false,
			countdown: 0,
			timer: null
		};
	},
	beforeDestroy() {
		if (this.timer) {
			clearInterval(this.timer);
		}
	},
	methods: {
		async handleGetCode() {
			if (this.countdown > 0) {
				return;
			}

			if (!this.email) {
				uni.showToast({
					title: '请先输入邮箱',
					icon: 'none'
				});
				return;
			}

			// 邮箱格式验证
			const emailReg = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
			if (!emailReg.test(this.email)) {
				uni.showToast({
					title: '请输入正确的邮箱格式',
					icon: 'none'
				});
				return;
			}

			try {
				// 调用发送验证码接口
				await sendEmailCode({ email: this.email });

				// 开始倒计时
				this.countdown = 60;
				this.timer = setInterval(() => {
					this.countdown--;
					if (this.countdown <= 0) {
						clearInterval(this.timer);
						this.timer = null;
					}
				}, 1000);

				uni.showToast({
					title: '验证码已发送',
					icon: 'success'
				});
			} catch (error) {
				console.error('发送验证码失败:', error);
			}
		},
		async handleNext() {
			if (!this.email) {
				uni.showToast({
					title: '请输入邮箱',
					icon: 'none'
				});
				return;
			}
			if (!this.verifyCode) {
				uni.showToast({
					title: '请输入验证码',
					icon: 'none'
				});
				return;
			}

			try {
				// 调用验证验证码接口
				const res = await verifyResetCode({
					email: this.email,
					code: this.verifyCode
				});

				// 验证成功，获取resetToken
				const resetToken = res.data?.resetToken;
				if (!resetToken) {
					uni.showToast({
						title: '验证失败，请重试',
						icon: 'none'
					});
					return;
				}

				console.log('验证通过，准备跳转到设置新密码页面');

				// 将 email 和 resetToken 传递到设置新密码页面
				// 在 H5 环境下直接使用 hash 路由跳转
				// #ifdef H5
				window.location.hash = `#/pages/auth/reset-password?email=${encodeURIComponent(this.email)}&resetToken=${encodeURIComponent(resetToken)}`;
				// #endif

				// 在 APP 环境下使用 navigateTo
				// #ifndef H5
				uni.navigateTo({
					url: `/pages/auth/reset-password?email=${encodeURIComponent(this.email)}&resetToken=${encodeURIComponent(resetToken)}`
				});
				// #endif
			} catch (error) {
				console.error('验证码验证失败:', error);
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
.forgot-password-page {
	width: 100%;
	min-height: 100vh;
	background-color: #f2f2f7;
	display: flex;
	flex-direction: column;
	align-items: center;
	overflow: hidden;
}

/* 顶部空白区域 */
.top-spacer {
	height: 120rpx;
}

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
	width: 104rpx;
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
	margin-bottom: 12rpx;
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
	transition: all 0.3s;
	box-sizing: border-box;
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
	color: rgba(60, 60, 67, 0.4);
}

/* 验证码行 */
.verify-row {
	display: flex;
	align-items: center;
	gap: 16rpx;
}

.verify-input {
	flex: 1;
	min-width: 0;
}

.verify-code-input {
	letter-spacing: 0.15em;
}

.verify-btn {
	height: 88rpx;
	padding: 0 24rpx;
	background-color: #111111;
	border-radius: 14rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	white-space: nowrap;
	transition: all 0.2s;
	flex-shrink: 0;
}

.verify-btn:active {
	opacity: 0.85;
}

.btn-disabled {
	background-color: rgba(120, 120, 128, 0.25);
	opacity: 0.7;
}

.btn-text {
	font-size: 24rpx;
	font-weight: 600;
	color: #ffffff;
}

/* 下一步按钮 */
.next-btn-wrapper {
	padding-top: 12rpx;
}

.next-btn {
	width: 100%;
	height: 88rpx;
	background-color: #111111;
	border-radius: 14rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	box-shadow: none;
	transition: all 0.2s;
}

.next-btn:active {
	opacity: 0.85;
}

.next-btn-text {
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
	font-size: 23rpx;
	font-weight: 500;
	color: rgba(60, 60, 67, 0.6);
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
