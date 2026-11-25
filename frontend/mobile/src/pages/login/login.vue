<template>
	<view class="login-container">
		<!-- 背景：与 PC 登录页同一套网格 + 柔光 -->
		<view class="bg-grid"></view>
		<view class="bg-blob blob-left"></view>
		<view class="bg-blob blob-right"></view>

		<!-- 顶部品牌：PC 登录页同款横版标识 -->
		<view class="login-header">
			<view class="brand-lockup" aria-label="西瓜 IM"></view>
		</view>

		<!-- 主区：手机端只保留 PC 在窄屏下的单卡片布局 -->
		<view class="login-main">
			<view class="auth-card">
				<view class="auth-title">
					<text class="auth-heading">欢迎回来</text>
					<text class="auth-subtitle">请输入您的凭据以访问您的工作空间</text>
				</view>

				<view class="login-form">
					<!-- 账号 / 密码合并为一个输入组，减少两个灰色块的割裂感 -->
					<view class="credential-group" :class="{ 'is-focus': !!activeField }">
						<view class="credential-row" :class="{ 'is-focused': activeField === 'username' }" @click="focusField('username')">
							<view class="field-icon icon-person"></view>
							<view class="credential-main">
								<text class="field-label">账号</text>
								<input
									ref="usernameInput"
									class="input-field"
									v-model="formData.username"
									placeholder="用户名或邮箱"
									placeholder-class="input-placeholder"
									type="text"
									autocomplete="username"
									aria-label="账号"
									@focus="activeField = 'username'"
									@blur="activeField = ''"
								/>
							</view>
						</view>

						<view class="credential-divider"></view>

						<view class="credential-row" :class="{ 'is-focused': activeField === 'password' }" @click="focusField('password')">
							<view class="field-icon icon-lock"></view>
							<view class="credential-main">
								<text class="field-label">密码</text>
								<input
									ref="passwordInput"
									class="input-field password-field"
									v-model="formData.password"
									placeholder="请输入密码"
									placeholder-class="input-placeholder"
									:password="!showPassword"
									type="text"
									autocomplete="current-password"
									aria-label="密码"
									@focus="activeField = 'password'"
									@blur="activeField = ''"
									@confirm="handleLogin"
								/>
							</view>
							<view class="pwd-toggle" @click.stop="togglePassword">
								<view class="pwd-eye" :class="{ 'is-visible': showPassword }"></view>
							</view>
						</view>
					</view>

					<!-- 登录风控：连续失败后要求算术验证码 -->
					<view v-if="captchaRequired" class="captcha-card">
						<view class="captcha-head">
							<text class="captcha-label">安全验证</text>
							<text class="captcha-refresh" @click="loadCaptcha">换一题</text>
						</view>
						<view class="captcha-row">
							<text class="captcha-question">{{ captchaQuestion || '加载中…' }}</text>
							<input
								class="captcha-input"
								v-model="formData.captchaCode"
								placeholder="答案"
								placeholder-class="input-placeholder"
								type="number"
								maxlength="4"
							/>
						</view>
						<text class="captcha-tip">连续登录失败次数过多，请输入计算结果后重试</text>
					</view>

					<!-- 记住账号 / 忘记密码：与 PC 底部操作行一致 -->
					<view class="form-row">
						<view class="remember" @click="toggleRemember">
							<view class="checkbox" :class="{ checked: remember }">
								<text v-if="remember" class="checkbox-mark">✓</text>
							</view>
							<text class="remember-text">记住账号</text>
						</view>
						<text class="forgot-link" @click="handleForgotPassword">忘记密码</text>
					</view>

					<!-- 主按钮：PC 同款蓝色胶囊 + 箭头 -->
					<button
						class="login-btn"
						:class="{ 'is-loading': isLoading }"
						:disabled="isLoading"
						hover-class="login-btn-hover"
						@click="handleLogin"
					>
						<view v-if="isLoading" class="spinner"></view>
						<text class="login-btn-text">{{ isLoading ? '登录中...' : '登录' }}</text>
						<view v-if="!isLoading" class="btn-arrow"></view>
					</button>

					<view class="switch-link">
						<text class="switch-muted">还没有账号？</text>
						<text class="switch-action" @click="goToRegister">立即注册</text>
					</view>
				</view>
			</view>
		</view>

		<view class="login-footer">
			<text class="footer-secure">SECURE · ENCRYPTED · PRIVATE</text>
			<text class="footer-copy">© 2026 西瓜 IM</text>
		</view>
	</view>
</template>

<script>
import { login, loginRisk, loginCaptcha } from '@/api/auth.js'
import { getUserInfo } from '@/api/user.js'
import config from '@/config/index.js'

const REMEMBER_KEY = 'xigua_login_username'

export default {
	data() {
		return {
			formData: {
				username: '',
				password: '',
				captchaId: '',
				captchaCode: ''
			},
			captchaRequired: false,
			captchaQuestion: '',
			showPassword: false,
			isLoading: false,
			activeField: '',
			remember: false
		};
	},
	onLoad() {
		try {
			const savedName = uni.getStorageSync(REMEMBER_KEY);
			if (savedName) {
				this.formData.username = savedName;
				this.remember = true;
			}
		} catch (e) {
			// 隐私模式或存储不可用时忽略，不影响登录
		}
	},
	methods: {
		togglePassword() {
			this.showPassword = !this.showPassword;
			// #ifdef H5
			setTimeout(() => {
				this.focusField('password');
			}, 50);
			// #endif
		},
		focusField(field) {
			this.activeField = field;
			// #ifdef H5
			const inputRef = field === 'password' ? this.$refs.passwordInput : this.$refs.usernameInput;
			if (inputRef && inputRef.$el) {
				const nativeInput = inputRef.$el.querySelector('input');
				if (nativeInput) {
					nativeInput.focus();
				}
			}
			// #endif
		},
		toggleRemember() {
			this.remember = !this.remember;
		},
		persistRememberedName() {
			try {
				if (this.remember) {
					uni.setStorageSync(REMEMBER_KEY, this.formData.username);
				} else {
					uni.removeStorageSync(REMEMBER_KEY);
				}
			} catch (e) {
				// 忽略存储失败，不影响登录
			}
		},
		handleForgotPassword() {
			// H5 使用 hash 路由跳转；App 使用 navigateTo
			// #ifdef H5
			window.location.hash = '#/pages/auth/forgot-password';
			// #endif

			// #ifndef H5
			uni.navigateTo({
				url: '/pages/auth/forgot-password',
				fail: () => {
					uni.showToast({
						title: '页面跳转失败',
						icon: 'none'
					});
				}
			});
			// #endif
		},
		async handleLogin() {
			if (!this.formData.username) {
				uni.showToast({
					title: '请输入用户名或邮箱',
					icon: 'none'
				});
				return;
			}
			if (!this.formData.password) {
				uni.showToast({
					title: '请输入密码',
					icon: 'none'
				});
				return;
			}

			if (this.isLoading) {
				return;
			}

			try {
				const riskRes = await loginRisk(this.formData.username);
				const risk = riskRes && riskRes.data ? riskRes.data : {};
				if (risk.captchaRequired) {
					this.captchaRequired = true;
					if (!this.formData.captchaId) {
						await this.loadCaptcha();
					}
					if (!this.formData.captchaCode) {
						uni.showToast({ title: '请先完成安全验证', icon: 'none' });
						return;
					}
				}
			} catch (e) {
				// 风控接口不可用时保持正常登录流程
			}

			try {
				this.isLoading = true;

				const res = await login({
					username: this.formData.username,
					password: this.formData.password,
					captchaId: this.formData.captchaId,
					captchaCode: this.formData.captchaCode
				});

				console.log('登录成功:', res);

				if (res.data && res.data.token) {
					uni.setStorageSync(config.tokenKey, res.data.token);
				}

				this.persistRememberedName();

				try {
					const userRes = await getUserInfo();
					if (userRes.data) {
						uni.setStorageSync(config.userInfoKey, JSON.stringify(userRes.data));
					}
				} catch (error) {
					console.warn('获取用户信息失败:', error);
				}

				uni.showToast({
					title: '登录成功',
					icon: 'success',
					duration: 1500
				});

				setTimeout(() => {
					uni.reLaunch({
						url: '/pages/chat/chat-list'
					});
				}, 1500);

			} catch (error) {
				console.error('登录失败:', error);
				// 错误提示已在 request.js 中处理
				if (this.captchaRequired) {
					this.formData.captchaId = '';
					this.formData.captchaCode = '';
					this.loadCaptcha();
				}
			} finally {
				this.isLoading = false;
			}
		},
		async loadCaptcha() {
			try {
				const res = await loginCaptcha(this.formData.username);
				const data = res && res.data ? res.data : {};
				this.captchaRequired = true;
				this.formData.captchaId = data.captchaId || '';
				this.formData.captchaCode = '';
				this.captchaQuestion = data.question || '';
			} catch (e) {
				this.captchaQuestion = '验证码加载失败，点此重试';
			}
	},
		goToRegister() {
			uni.navigateTo({
				url: '/pages/register/register'
			});
		}
	}
};
</script>

<style lang="scss" scoped>
/* 登录风控验证码 */
.captcha-card {
	margin-top: 16px;
	padding: 14px 16px;
	border-radius: 16px;
	background: #ffffff;
	border: 1px solid rgba(60, 60, 67, 0.12);
}

.captcha-head {
	display: flex;
	align-items: center;
	justify-content: space-between;
}

.captcha-label {
	font-size: 12px;
	font-weight: 600;
	color: #1c1c1e;
	letter-spacing: 0.04em;
}

.captcha-refresh {
	font-size: 12px;
	color: #111111;
	opacity: 0.55;
}

.captcha-row {
	display: flex;
	align-items: center;
	gap: 12px;
	margin-top: 10px;
}

.captcha-question {
	flex: 1;
	font-size: 15px;
	font-weight: 600;
	color: #1c1c1e;
}

.captcha-input {
	width: 80px;
	height: 36px;
	padding: 0 12px;
	border-radius: 10px;
	background: #f2f2f7;
	font-size: 15px;
	color: #1c1c1e;
	text-align: center;
}

.captcha-tip {
	display: block;
	margin-top: 8px;
	font-size: 11px;
	line-height: 1.5;
	color: rgba(60, 60, 67, 0.45);
}

.login-container {
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

/* 背景网格：PC 登录页同款 */
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

/* 背景柔光斑：PC 同款冷蓝 + 浅绿 */
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

/* 顶部品牌 */
.login-header {
	position: relative;
	z-index: 2;
	width: 100%;
	max-width: 944px;
	margin: 0 auto;
	padding: calc(12px + var(--status-bar-height, 0px)) 20px 0;
	display: flex;
	align-items: center;
	box-sizing: border-box;
	flex: 0 0 auto;
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

/* uni-image 的内层 img 尺寸由文末全局样式统一约束，避免横版标识被原始尺寸截断 */

/* 主区 */
.login-main {
	position: relative;
	z-index: 2;
	width: 100%;
	max-width: 1040px;
	margin: 0 auto;
	padding: 20px 16px 0;
	display: flex;
	align-items: center;
	justify-content: center;
	box-sizing: border-box;
	flex: 1 0 auto;
}

/* 表单卡片：PC 的 448px 固定卡宽，手机端自动收到屏宽 */
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

.login-form {
	display: flex;
	flex-direction: column;
}

/* 输入组：两个字段共用一个柔和底色，用细分割线代替两块独立灰块 */
.credential-group {
	position: relative;
	margin-bottom: 18px;
	overflow: hidden;
	background: linear-gradient(180deg, rgba(118, 118, 128, 0.085) 0%, rgba(118, 118, 128, 0.07) 100%);
	border: 1px solid rgba(60, 60, 67, 0.055);
	border-radius: 16px;
	box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.64);
	transition: background-color 0.22s ease, border-color 0.22s ease, box-shadow 0.22s ease;
}

.credential-group.is-focus {
	background: rgba(255, 255, 255, 0.98);
	border-color: rgba(0, 122, 255, 0.30);
	box-shadow: 0 0 0 3px rgba(0, 122, 255, 0.08);
}

.credential-row {
	position: relative;
	display: flex;
	align-items: center;
	min-height: 58px;
	padding: 0 14px;
	box-sizing: border-box;
	transition: background-color 0.18s ease;
}

.credential-row.is-focused {
	background: rgba(0, 122, 255, 0.025);
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
	transition: color 0.18s ease;
}

.credential-row.is-focused .field-label {
	color: rgba(0, 122, 255, 0.72);
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

.credential-divider {
	height: 1px;
	margin-left: 44px;
	background: rgba(60, 60, 67, 0.09);
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

/* 记住账号 / 忘记密码 */
.form-row {
	display: flex;
	align-items: center;
	justify-content: space-between;
	margin: 2px 4px 22px;
}

.remember {
	display: flex;
	align-items: center;
	cursor: pointer;
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

.remember-text {
	font-size: 12px;
	color: rgba(60, 60, 67, 0.66);
}

.forgot-link {
	font-size: 12px;
	font-weight: 500;
	color: #007aff;
	cursor: pointer;
}

/* 主按钮 */
.login-btn {
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

.login-btn::after {
	border: none;
}

.login-btn-hover {
	transform: translateY(1px);
	box-shadow: 0 3px 10px rgba(0, 122, 255, 0.22);
	background: #0066d6;
}

.login-btn[disabled] {
	background: linear-gradient(180deg, #3395ff 0%, #007aff 100%);
	color: #ffffff;
	opacity: 0.72;
}

.login-btn-text {
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
	animation: login-spin 0.8s linear infinite;
}

/* 注册入口 */
.switch-link {
	margin-top: auto;
	padding-top: 20px;
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

/* 页脚：PC 同款安全标识 */
.login-footer {
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

@keyframes login-spin {
	to {
		transform: rotate(360deg);
	}
}

/* 桌面宽度：完全对齐 PC 登录页 */
@media (min-width: 768px) {
	.login-header {
		height: 80px;
		padding: 0 24px;
	}

	.brand-lockup {
		width: 150px;
		height: 44px;
	}

	.login-main {
		padding: 28px 24px 12px;
	}

	.auth-card {
		min-height: 580px;
		padding: 40px 40px 32px;
		border-radius: 24px;
	}

	.auth-heading {
		font-size: 27px;
	}

	.auth-subtitle {
		font-size: 14px;
	}

	.login-footer {
		margin-bottom: 22px;
		padding: 8px 16px 0;
	}

	.footer-secure,
	.footer-copy {
		font-size: 10px;
	}
}

/* 手机宽度：卡片靠近顶部，避免在高屏上被垂直居中推得过低 */
@media (max-width: 767px) {
	.login-main {
		align-items: flex-start;
	}
}

/* 小屏高度兜底：保证 375×535 也能滚动到按钮与注册入口 */
@media (max-height: 640px) {
	.login-header {
		padding-top: calc(8px + var(--status-bar-height, 0px));
	}

	.brand-lockup {
		height: 30px;
		width: 100px;
	}

	.login-main {
		padding-top: 11px;
	}

	.auth-card {
		padding: 20px 20px 18px;
	}

	.auth-title {
		margin-bottom: 18px;
	}

	.auth-heading {
		font-size: 22px;
	}

	.credential-group {
		margin-bottom: 13px;
	}

	.form-row {
		margin-bottom: 16px;
	}

	.login-footer {
		padding-top: 11px;
		padding-bottom: calc(9px + env(safe-area-inset-bottom));
	}
}

/* H5 下 uni-input 内部会把占位层和输入层上下堆叠；这里用深度选择器只修正本页输入组 */
.credential-group ::v-deep .uni-input-wrapper {
	position: relative !important;
	display: flex !important;
	align-items: center !important;
	width: 100% !important;
	height: 22px !important;
	flex-direction: row !important;
}

.credential-group ::v-deep .uni-input-placeholder {
	position: absolute !important;
	top: 0 !important;
	left: 0 !important;
	width: 100% !important;
	height: 22px !important;
	line-height: 22px !important;
	pointer-events: none !important;
}

.credential-group ::v-deep .uni-input-input {
	width: 100% !important;
	height: 22px !important;
	padding: 0 !important;
	box-sizing: border-box !important;
	line-height: 22px !important;
	font-size: 14px !important;
	font-family: inherit !important;
}
</style>

