<template>
	<view class="register-container">
		<!-- 背景：与登录页同一套网格 + 柔光 -->
		<view class="bg-grid"></view>
		<view class="bg-blob blob-left"></view>
		<view class="bg-blob blob-right"></view>

		<!-- 顶部：返回 + 横版品牌标识，与登录页保持一致 -->
		<view class="register-header">
			<view class="back-btn" @tap="goToLogin" hover-class="back-btn-hover">
				<image class="back-icon" src="/static/icons/arrow/left.svg" mode="aspectFit"></image>
			</view>
			<view class="brand-lockup" aria-label="西瓜 IM"></view>
		</view>

		<!-- 主区：宽屏固定 448px 卡片，窄屏自动收到屏宽 -->
		<view class="register-main">
			<view class="auth-card">
				<view class="auth-title">
					<text class="auth-heading">创建账号</text>
					<text class="auth-subtitle">注册后即可添加好友、发起群聊</text>
				</view>

				<view class="register-form">
					<view class="credential-group">
						<view class="credential-row">
							<view class="field-icon icon-person"></view>
							<view class="credential-main">
								<text class="field-label">用户名</text>
								<input
									class="input-field"
									v-model="formData.username"
									placeholder="请输入用户名"
									placeholder-class="input-placeholder"
									type="text"
									autocomplete="username"
									aria-label="用户名"
								/>
							</view>
						</view>

						<view class="credential-divider"></view>

						<view class="credential-row">
							<view class="field-icon icon-mail"></view>
							<view class="credential-main">
								<text class="field-label">邮箱地址</text>
								<input
									class="input-field"
									v-model="formData.email"
									placeholder="请输入邮箱地址"
									placeholder-class="input-placeholder"
									type="text"
									autocomplete="email"
									aria-label="邮箱地址"
								/>
							</view>
						</view>

						<view class="credential-divider"></view>

						<view class="credential-row code-row">
							<view class="field-icon icon-card"></view>
							<view class="credential-main">
								<text class="field-label">邮箱验证码</text>
								<input
									class="input-field"
									v-model="formData.code"
									placeholder="6 位验证码"
									placeholder-class="input-placeholder"
									type="text"
									maxlength="6"
									aria-label="邮箱验证码"
								/>
							</view>
							<view
								class="code-btn"
								:class="{ disabled: codeCountdown > 0 }"
								@tap="handleGetCode"
							>
								<text class="code-text">{{ codeCountdown > 0 ? codeCountdown + 's' : '获取验证码' }}</text>
							</view>
						</view>

						<view class="credential-divider"></view>

						<view class="credential-row">
							<view class="field-icon icon-lock"></view>
							<view class="credential-main">
								<text class="field-label">设置密码</text>
								<input
									class="input-field password-field"
									v-model="formData.password"
									placeholder="至少 6 位"
									placeholder-class="input-placeholder"
									:password="!showPassword"
									type="text"
									autocomplete="new-password"
									aria-label="设置密码"
									@confirm="handleRegister"
								/>
							</view>
							<view class="pwd-toggle" @tap="togglePassword">
								<view class="pwd-eye" :class="{ 'is-visible': showPassword }"></view>
							</view>
						</view>
					</view>

					<view class="agreement-row">
						<view class="checkbox" :class="{ checked: agreed }" @tap="toggleAgreed">
							<text v-if="agreed" class="checkbox-mark">✓</text>
						</view>
						<text class="agreement-text">我已阅读并同意</text>
						<text class="agreement-link" @tap="handleShowAgreement('service')">服务协议</text>
						<text class="agreement-text">和</text>
						<text class="agreement-link" @tap="handleShowAgreement('privacy')">隐私政策</text>
					</view>

					<button
						class="register-btn"
						:class="{ 'is-loading': isRegistering }"
						:disabled="isRegistering"
						hover-class="register-btn-hover"
						@click="handleRegister"
					>
						<view v-if="isRegistering" class="spinner"></view>
						<text class="register-btn-text">{{ isRegistering ? '注册中…' : '注册' }}</text>
						<view v-if="!isRegistering" class="btn-arrow"></view>
					</button>

					<view class="switch-link">
						<text class="switch-muted">已有账号？</text>
						<text class="switch-action" @tap="goToLogin">去登录</text>
					</view>
				</view>
			</view>
		</view>

		<view class="register-footer">
			<text class="footer-secure">SECURE · ENCRYPTED · PRIVATE</text>
			<text class="footer-copy">© 2026 西瓜 IM</text>
		</view>
	</view>
</template>

<script>
import { sendEmailCode } from '@/api/email.js';
import { register } from '@/api/auth.js';
import config from '@/config/index.js';

export default {
	data() {
		return {
			formData: {
				username: '',
				email: '',
				code: '',
				password: ''
			},
			showPassword: false,
			agreed: false,
			codeCountdown: 0,
			timer: null,
			isRegistering: false
		};
	},
	methods: {
		togglePassword() {
			this.showPassword = !this.showPassword;
		},
		handleAgreementChange(e) {
			this.agreed = e.detail.value.includes('agree');
		},
		async handleGetCode() {
			if (!this.formData.email) {
				uni.showToast({
					title: '请输入邮箱地址',
					icon: 'none'
				});
				return;
			}

			// 邮箱格式验证
			const emailReg = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
			if (!emailReg.test(this.formData.email)) {
				uni.showToast({
					title: '请输入有效的邮箱地址',
					icon: 'none'
				});
				return;
			}

			try {
				// 调用发送验证码接口
				await sendEmailCode({ email: this.formData.email });

				// 开始倒计时
				this.codeCountdown = 60;
				this.timer = setInterval(() => {
					this.codeCountdown--;
					if (this.codeCountdown <= 0) {
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
				uni.showToast({
					title: error.msg || '发送验证码失败',
					icon: 'none'
				});
			}
		},
		async handleRegister() {
			if (!this.formData.username) {
				uni.showToast({
					title: '请输入用户名',
					icon: 'none'
				});
				return;
			}
			if (!this.formData.email) {
				uni.showToast({
					title: '请输入邮箱地址',
					icon: 'none'
				});
				return;
			}
			if (!this.formData.code) {
				uni.showToast({
					title: '请输入验证码',
					icon: 'none'
				});
				return;
			}
			if (!this.formData.password) {
				uni.showToast({
					title: '请设置密码',
					icon: 'none'
				});
				return;
			}
			if (this.formData.password.length < 6) {
				uni.showToast({
					title: '密码至少6位',
					icon: 'none'
				});
				return;
			}
			if (!this.agreed) {
				uni.showToast({
					title: '请阅读并同意协议',
					icon: 'none'
				});
				return;
			}

			// 防止重复提交
			if (this.isRegistering) return;

			try {
				this.isRegistering = true;

				// 调用注册接口
				const res = await register({
					username: this.formData.username,
					email: this.formData.email,
					code: this.formData.code,
					password: this.formData.password
				});

				// 存储 token 和用户信息
				if (res.data && res.data.token) {
					uni.setStorageSync(config.tokenKey, res.data.token);
				}
				if (res.data && res.data.userInfo) {
					uni.setStorageSync(config.userInfoKey, res.data.userInfo);
				}

				uni.showToast({
					title: '注册成功',
					icon: 'success',
					duration: 1500
				});

				// 延迟跳转到聊天列表页
				setTimeout(() => {
					uni.reLaunch({
						url: '/pages/chat/chat-list'
					});
				}, 1500);
			} catch (error) {
				console.error('注册失败:', error);
				uni.showToast({
					title: error.msg || '注册失败',
					icon: 'none'
				});
			} finally {
				this.isRegistering = false;
			}
		},
		toggleAgreed() {
			this.agreed = !this.agreed;
		},
		handleShowAgreement(type) {
			const title = type === 'service' ? '用户服务协议' : '隐私政策';
			const content = type === 'service'
				? '西瓜 IM 用于即时通讯学习与演示。请勿发送违法、侵权或骚扰内容，账号违规将被限制使用。'
				: '我们仅保存完成即时通讯所必需的账号、好友关系和消息数据，不向第三方出售或提供。';
			uni.showModal({
				title,
				content,
				showCancel: false,
				confirmColor: '#111111'
			});
		},
		goToLogin() {
			uni.navigateTo({
				url: '/pages/login/login'
			});
		}
	},
	beforeDestroy() {
		if (this.timer) {
			clearInterval(this.timer);
		}
	}
};
</script>

<style lang="scss" scoped>
.register-container {
	position: relative;
	width: 100%;
	min-height: 100vh;
	display: flex;
	flex-direction: column;
	overflow-x: hidden;
	overflow-y: auto;
	background: linear-gradient(180deg, #f8f8fa 0%, #f2f2f7 52%, #eceef2 100%);
	font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Text', 'PingFang SC',
		'Microsoft YaHei', Arial, sans-serif;
}

/* 背景网格：与登录页一致 */
.bg-grid {
	position: fixed;
	top: 0;
	right: 0;
	bottom: 0;
	left: 0;
	background-image: linear-gradient(rgba(60, 60, 67, 0.055) 1px, transparent 1px),
		linear-gradient(90deg, rgba(60, 60, 67, 0.055) 1px, transparent 1px);
	background-size: 40px 40px;
	opacity: 0.55;
	pointer-events: none;
	z-index: 0;
}

.bg-blob {
	position: fixed;
	border-radius: 9999px;
	filter: blur(80px);
	pointer-events: none;
	z-index: 0;
}

.blob-left {
	left: -120px;
	top: 16%;
	width: 360px;
	height: 360px;
	background: rgba(0, 122, 255, 0.06);
}

.blob-right {
	right: -130px;
	bottom: 2%;
	width: 380px;
	height: 380px;
	background: rgba(52, 199, 89, 0.035);
}

/* 顶部：返回 + 品牌 */
.register-header {
	position: relative;
	z-index: 2;
	width: 100%;
	max-width: 944px;
	margin: 0 auto;
	padding: calc(12px + var(--status-bar-height, 0px)) 20px 0;
	display: flex;
	align-items: center;
	gap: 12px;
	box-sizing: border-box;
	flex: 0 0 auto;
}

.back-btn {
	width: 36px;
	height: 36px;
	display: flex;
	align-items: center;
	justify-content: center;
	border-radius: 10px;
	cursor: pointer;
}

.back-btn-hover {
	opacity: 0.6;
}

.back-icon {
	width: 20px;
	height: 20px;
	opacity: 0.62;
}

.brand-lockup {
	width: 124px;
	height: 38px;
	display: block;
	background-image: url('/static/brand-lockup.png');
	background-position: center center;
	background-repeat: no-repeat;
	background-size: contain;
}

/* 主区 */
.register-main {
	position: relative;
	z-index: 2;
	width: 100%;
	max-width: 1040px;
	margin: 0 auto;
	padding: 20px 16px 0;
	display: flex;
	align-items: flex-start;
	justify-content: center;
	box-sizing: border-box;
	flex: 1 0 auto;
}

/* 与登录页同一张 448px 卡片 */
.auth-card {
	width: 100%;
	max-width: 448px;
	display: flex;
	flex-direction: column;
	box-sizing: border-box;
	padding: 26px 20px 22px;
	background: rgba(255, 255, 255, 0.96);
	border: 1px solid rgba(15, 23, 42, 0.05);
	border-radius: 22px;
	box-shadow: 0 20px 50px rgba(15, 23, 42, 0.08), 0 2px 8px rgba(15, 23, 42, 0.035);
	-webkit-backdrop-filter: saturate(180%) blur(30px);
	backdrop-filter: saturate(180%) blur(30px);
	overflow: hidden;
}

.auth-title {
	margin-bottom: 24px;
}

.auth-heading {
	display: block;
	font-size: 24px;
	line-height: 1.2;
	font-weight: 700;
	letter-spacing: -0.5px;
	color: #1c1c1e;
}

.auth-subtitle {
	display: block;
	margin-top: 7px;
	font-size: 13px;
	line-height: 1.5;
	color: rgba(60, 60, 67, 0.62);
}

.register-form {
	display: flex;
	flex-direction: column;
	min-width: 0;
}

/* 四个字段共用一个输入组，与登录页输入组的视觉语言一致 */
.credential-group {
	position: relative;
	margin-bottom: 14px;
	overflow: hidden;
	background: linear-gradient(180deg, rgba(118, 118, 128, 0.085) 0%, rgba(118, 118, 128, 0.07) 100%);
	border: 1px solid rgba(60, 60, 67, 0.055);
	border-radius: 16px;
	box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.64);
}

.credential-row {
	position: relative;
	display: flex;
	align-items: center;
	min-height: 58px;
	padding: 0 14px;
	box-sizing: border-box;
}

.credential-divider {
	height: 1px;
	margin-left: 44px;
	background: rgba(60, 60, 67, 0.09);
}

.field-icon {
	width: 18px;
	height: 18px;
	flex: 0 0 18px;
	margin-right: 12px;
	opacity: 0.34;
	background-position: center center;
	background-repeat: no-repeat;
	background-size: contain;
}

.icon-person {
	background-image: url('/static/icons/auth/person.svg');
}

.icon-mail {
	background-image: url('/static/icons/auth/mail.svg');
}

.icon-card {
	background-image: url('/static/icons/auth/card.svg');
}

.icon-lock {
	background-image: url('/static/icons/auth/lock.svg');
}

.credential-main {
	display: flex;
	flex: 1 1 auto;
	align-items: stretch;
	flex-direction: column;
	justify-content: center;
	min-width: 0;
}

.field-label {
	display: block;
	margin: 0 0 2px;
	font-size: 11px;
	line-height: 1.2;
	font-weight: 600;
	letter-spacing: 0.3px;
	color: rgba(60, 60, 67, 0.52);
}

.input-field {
	width: 100%;
	height: 22px;
	padding: 0;
	box-sizing: border-box;
	border: none;
	background: transparent;
	font-size: 14px;
	line-height: 22px;
	font-family: inherit;
	color: #1c1c1e;
}

.input-placeholder {
	color: rgba(60, 60, 67, 0.42);
	font-size: 13px;
	font-weight: 400;
	pointer-events: none;
}

.code-row {
	padding-right: 10px;
}

.code-btn {
	display: flex;
	align-items: center;
	justify-content: center;
	flex: 0 0 auto;
	min-width: 82px;
	height: 32px;
	margin-left: 8px;
	padding: 0 10px;
	box-sizing: border-box;
	border: 1px solid rgba(0, 122, 255, 0.16);
	border-radius: 999px;
	background: rgba(0, 122, 255, 0.08);
	cursor: pointer;
	transition: opacity 0.18s ease, background-color 0.18s ease;
}

.code-btn.disabled {
	opacity: 0.48;
	pointer-events: none;
}

.code-text {
	font-size: 11px;
	font-weight: 600;
	color: #007aff;
	white-space: nowrap;
}

.pwd-toggle {
	position: relative;
	display: flex;
	align-items: center;
	justify-content: center;
	flex: 0 0 44px;
	width: 44px;
	height: 44px;
	margin-right: -6px;
	border-radius: 10px;
	cursor: pointer;
}

.pwd-eye {
	width: 19px;
	height: 19px;
	opacity: 0.42;
	background-image: url('/static/icons/auth/eye-open.svg');
	background-position: center center;
	background-repeat: no-repeat;
	background-size: contain;
}

.pwd-eye.is-visible {
	background-image: url('/static/icons/auth/eye-close.svg');
}

/* 协议勾选 */
.agreement-row {
	display: flex;
	align-items: center;
	flex-wrap: wrap;
	margin: 4px 4px 18px;
	line-height: 1.6;
}

.checkbox {
	width: 18px;
	height: 18px;
	margin-right: 8px;
	display: flex;
	align-items: center;
	justify-content: center;
	box-sizing: border-box;
	border: 1.5px solid rgba(60, 60, 67, 0.26);
	border-radius: 5px;
	background: #ffffff;
	transition: background-color 0.18s ease, border-color 0.18s ease;
}

.checkbox.checked {
	background: #007aff;
	border-color: #007aff;
}

.checkbox-mark {
	font-size: 12px;
	line-height: 1;
	font-weight: 700;
	color: #ffffff;
}

.agreement-text {
	margin-left: 2px;
	font-size: 11px;
	color: rgba(60, 60, 67, 0.62);
}

.agreement-link {
	margin-left: 2px;
	font-size: 11px;
	font-weight: 500;
	color: #007aff;
	cursor: pointer;
}

/* 主按钮：与登录页同款蓝色胶囊 + 箭头 */
.register-btn {
	position: relative;
	width: 100%;
	height: 50px;
	margin: 0;
	padding: 0 18px;
	display: flex;
	align-items: center;
	justify-content: center;
	box-sizing: border-box;
	border: none;
	border-radius: 999px;
	background: linear-gradient(180deg, #3395ff 0%, #007aff 100%);
	box-shadow: 0 5px 16px rgba(0, 122, 255, 0.30);
	color: #ffffff;
	font-family: inherit;
	font-size: 15px;
	font-weight: 600;
	letter-spacing: 0.3px;
	line-height: 1;
	overflow: visible;
	transition: transform 0.16s ease, box-shadow 0.16s ease, opacity 0.16s ease;
}

.register-btn::after {
	border: none;
}

.register-btn-hover {
	transform: translateY(1px);
	box-shadow: 0 3px 10px rgba(0, 122, 255, 0.22);
	background: #0066d6;
}

.register-btn[disabled] {
	background: linear-gradient(180deg, #3395ff 0%, #007aff 100%);
	color: #ffffff;
	opacity: 0.72;
}

.register-btn-text {
	display: block;
}

.btn-arrow {
	width: 13px;
	height: 13px;
	margin-left: 9px;
	display: block;
	background-image: url('/static/icons/auth/arrow-white.svg');
	background-position: center center;
	background-repeat: no-repeat;
	background-size: contain;
}

.spinner {
	width: 14px;
	height: 14px;
	margin-right: 8px;
	box-sizing: border-box;
	border: 2px solid rgba(255, 255, 255, 0.35);
	border-top-color: #ffffff;
	border-radius: 50%;
	animation: register-spin 0.8s linear infinite;
}

/* 已有账号入口 */
.switch-link {
	margin-top: 18px;
	text-align: center;
	font-size: 12px;
	line-height: 1.5;
}

.switch-muted {
	color: rgba(60, 60, 67, 0.58);
}

.switch-action {
	margin-left: 5px;
	font-weight: 600;
	color: #007aff;
	cursor: pointer;
}

/* 页脚：与登录页同款安全标识 */
.register-footer {
	position: relative;
	z-index: 2;
	display: flex;
	flex-direction: column;
	align-items: center;
	box-sizing: border-box;
	margin: 0 auto;
	padding: 18px 16px calc(14px + env(safe-area-inset-bottom));
	flex: 0 0 auto;
}

.footer-secure {
	font-size: 9px;
	font-weight: 500;
	letter-spacing: 1.1px;
	color: rgba(60, 60, 67, 0.35);
}

.footer-copy {
	margin-top: 6px;
	font-size: 9px;
	letter-spacing: 0.2px;
	color: rgba(60, 60, 67, 0.32);
}

@keyframes register-spin {
	to {
		transform: rotate(360deg);
	}
}

/* 桌面宽度：与登录页完全同宽 */
@media (min-width: 768px) {
	.register-header {
		height: 80px;
		padding: 0 24px;
	}

	.brand-lockup {
		width: 150px;
		height: 44px;
	}

	.register-main {
		padding: 28px 24px 12px;
		align-items: center;
	}

	.auth-card {
		min-height: 588px;
		padding: 40px 40px 32px;
		border-radius: 24px;
	}

	.auth-heading {
		font-size: 27px;
	}

	.auth-subtitle {
		font-size: 14px;
	}

	.switch-link {
		margin-top: auto;
		padding-top: 18px;
	}

	.register-footer {
		margin-bottom: 22px;
		padding: 8px 16px 0;
	}

	.footer-secure,
	.footer-copy {
		font-size: 10px;
	}
}

/* 手机宽度：卡片靠上，避免高屏垂直居中后按钮被推到屏幕外 */
@media (max-width: 767px) {
	.register-main {
		align-items: flex-start;
	}
}

/* 小屏高度兜底 */
@media (max-height: 640px) {
	.register-header {
		padding-top: calc(8px + var(--status-bar-height, 0px));
	}

	.brand-lockup {
		height: 30px;
		width: 100px;
	}

	.register-main {
		padding-top: 11px;
	}

	.auth-card {
		padding: 20px 20px 18px;
	}

	.auth-title {
		margin-bottom: 18px;
	}
}
</style>
