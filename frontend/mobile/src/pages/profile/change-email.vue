<template>
	<view class="change-email-page">
		<WebSocketKeeper />
		<!-- 顶部导航栏 -->
		<view class="top-bar safe-top-bar">
			<view class="top-bar-content">
				<text class="cancel-btn" @click="handleCancel">取消</text>
				<text class="page-title">更换邮箱</text>
				<text class="done-btn" :class="{ 'done-disabled': submitting }" @click="handleDone">{{ submitting ? '提交中' : '完成' }}</text>
			</view>
		</view>

		<!-- 主内容区 -->
		<scroll-view class="main-content" scroll-y="true">
			<view class="content-wrapper">
				<view class="header-section">
					<view class="icon-circle">
						<image class="email-icon" src="/static/icons/auth/at-sign.svg" mode="aspectFit"></image>
					</view>
					<text class="hint-text">更换后，原邮箱将无法再用于登录</text>
				</view>

				<view class="form-section">
					<view class="form-item">
						<text class="form-label">当前邮箱</text>
						<view class="value-row">
							<image class="input-icon" src="/static/icons/auth/mail.svg" mode="aspectFit"></image>
							<text class="current-text">{{ currentEmail || '未绑定' }}</text>
						</view>
					</view>

					<view class="form-item">
						<text class="form-label">新邮箱</text>
						<view class="input-row">
							<image class="input-icon" src="/static/icons/auth/mail-badge.svg" mode="aspectFit"></image>
							<input
								class="input-field"
								v-model="newEmail"
								type="text"
								placeholder="请输入新邮箱地址"
								@focus="newEmailFocused = true"
								@blur="newEmailFocused = false"
							/>
						</view>
					</view>

					<view class="form-item">
						<text class="form-label">验证码</text>
						<view class="verify-row">
							<view class="input-row verify-input">
								<image class="input-icon" src="/static/icons/auth/shield.svg" mode="aspectFit"></image>
								<input
									class="input-field"
									v-model="verifyCode"
									type="number"
									placeholder="6 位验证码"
									maxlength="6"
								/>
							</view>
							<view class="verify-btn" :class="{ 'btn-disabled': countdown > 0 || sending }" @click="handleGetCode">
								<text class="btn-text">{{ countdown > 0 ? countdown + 's' : (sending ? '发送中' : '获取验证码') }}</text>
							</view>
						</view>
					</view>
				</view>

				<view class="help-section" @click="handleHelp">
					<image class="help-icon" src="/static/icons/misc/question-gray.svg" mode="aspectFit"></image>
					<text class="help-text">收不到验证码？</text>
				</view>
			</view>
		</scroll-view>
	</view>
</template>

<script>
import WebSocketKeeper from '@/components/WebSocketKeeper.vue'
import { getUserInfo, updateUserInfo } from '@/api/user.js'
import { sendEmailCode, verifyResetCode } from '@/api/email.js'
import config from '@/config/index.js'

export default {
	components: {
		WebSocketKeeper
	},
	data() {
		return {
			user: {},
			currentEmail: '',
			newEmail: '',
			verifyCode: '',
			newEmailFocused: false,
			countdown: 0,
			timer: null,
			sending: false,
			submitting: false
		};
	},
	onLoad() {
		this.loadUser();
	},
	beforeDestroy() {
		if (this.timer) {
			clearInterval(this.timer);
		}
	},
	methods: {
		async loadUser() {
			let cached = {};
			try {
				const raw = uni.getStorageSync(config.userInfoKey);
				cached = raw ? (typeof raw === 'string' ? JSON.parse(raw) : raw) : {};
			} catch (e) {
				cached = {};
			}
			this.user = cached;
			this.currentEmail = cached.email || '';
			try {
				const res = await getUserInfo();
				if (res && res.data) {
					this.user = res.data;
					this.currentEmail = res.data.email || '';
					uni.setStorageSync(config.userInfoKey, JSON.stringify(res.data));
				}
			} catch (e) {
				console.warn('获取用户信息失败', e);
			}
		},
		handleCancel() {
			uni.navigateBack();
		},
		isValidEmail(email) {
			return /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/.test(email);
		},
		async handleGetCode() {
			if (this.countdown > 0 || this.sending) {
				return;
			}
			if (!this.newEmail) {
				uni.showToast({ title: '请先输入新邮箱', icon: 'none' });
				return;
			}
			if (!this.isValidEmail(this.newEmail)) {
				uni.showToast({ title: '邮箱格式不正确', icon: 'none' });
				return;
			}
			if (this.newEmail === this.currentEmail) {
				uni.showToast({ title: '新邮箱与当前邮箱相同', icon: 'none' });
				return;
			}
			this.sending = true;
			try {
				await sendEmailCode({ email: this.newEmail });
				this.countdown = 60;
				this.timer = setInterval(() => {
					this.countdown--;
					if (this.countdown <= 0) {
						clearInterval(this.timer);
						this.timer = null;
					}
				}, 1000);
				uni.showToast({ title: '验证码已发送', icon: 'success' });
			} catch (e) {
				uni.showToast({ title: e.message || '发送失败', icon: 'none' });
			} finally {
				this.sending = false;
			}
		},
		async handleDone() {
			if (this.submitting) {
				return;
			}
			if (!this.newEmail) {
				uni.showToast({ title: '请输入新邮箱', icon: 'none' });
				return;
			}
			if (!this.isValidEmail(this.newEmail)) {
				uni.showToast({ title: '邮箱格式不正确', icon: 'none' });
				return;
			}
			if (!this.verifyCode) {
				uni.showToast({ title: '请输入验证码', icon: 'none' });
				return;
			}
			this.submitting = true;
			try {
				await verifyResetCode({ email: this.newEmail, code: this.verifyCode });
				await updateUserInfo({
					username: this.user.username,
					email: this.newEmail,
					avatar: this.user.avatar,
					sex: this.user.sex,
					region: this.user.region,
					signature: this.user.signature
				});
				const updated = Object.assign({}, this.user, { email: this.newEmail });
				uni.setStorageSync(config.userInfoKey, JSON.stringify(updated));
				this.user = updated;
				this.currentEmail = this.newEmail;
				uni.showToast({ title: '邮箱更换成功', icon: 'success' });
				setTimeout(() => {
					uni.navigateBack();
				}, 1200);
			} catch (e) {
				uni.showToast({ title: e.message || '更换失败', icon: 'none' });
			} finally {
				this.submitting = false;
			}
		},
		handleHelp() {
			uni.showModal({
				title: '验证码帮助',
				content: '如果长时间收不到验证码，请检查邮箱垃圾箱，或稍后再试。',
				showCancel: false
			});
		}
	}
};
</script>

<style lang="scss" scoped>
.change-email-page {
	display: block;
	width: 100%;
	min-height: 100vh;
	background-color: #f2f2f7;
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

.top-bar-content {
	display: flex;
	justify-content: space-between;
	align-items: center;
	height: 96rpx;
	padding: 0 28rpx;
}

.cancel-btn,
.done-btn {
	font-size: 30rpx;
	font-weight: 500;
	color: #1c1c1e;
}

.done-btn {
	font-weight: 600;
}

.done-disabled {
	color: rgba(60, 60, 67, 0.35);
}

.page-title {
	font-size: 34rpx;
	font-weight: 600;
	color: #1c1c1e;
}

.main-content {
	display: block;
	padding-top: calc(var(--status-bar-height) + 96rpx);
	height: 100vh;
	box-sizing: border-box;
}

.content-wrapper {
	display: block;
	width: 100%;
	max-width: 928rpx;
	margin: 0 auto;
	padding: 48rpx 28rpx;
	box-sizing: border-box;
}

.header-section {
	display: flex;
	flex-direction: column;
	align-items: center;
	margin-bottom: 36rpx;
}

.icon-circle {
	width: 104rpx;
	height: 104rpx;
	background: #ffffff;
	border: 1rpx solid rgba(60, 60, 67, 0.12);
	border-radius: 50%;
	display: flex;
	align-items: center;
	justify-content: center;
	margin-bottom: 20rpx;
}

.email-icon {
	width: 44rpx;
	height: 44rpx;
}

.hint-text {
	font-size: 24rpx;
	color: rgba(60, 60, 67, 0.55);
	text-align: center;
}

.form-section {
	display: block;
	background: #ffffff;
	border-radius: 18rpx;
	border: 1rpx solid rgba(60, 60, 67, 0.08);
	overflow: hidden;
}

.form-item {
	padding: 24rpx 28rpx;
	border-bottom: 1rpx solid rgba(60, 60, 67, 0.1);
}

.form-item:last-child {
	border-bottom: none;
}

.form-label {
	display: block;
	font-size: 22rpx;
	font-weight: 500;
	color: rgba(60, 60, 67, 0.55);
	margin-bottom: 10rpx;
}

.value-row,
.input-row {
	display: flex;
	align-items: center;
	height: 56rpx;
	gap: 14rpx;
}

.input-icon {
	width: 30rpx;
	height: 30rpx;
	opacity: 0.55;
	flex-shrink: 0;
}

.current-text,
.input-field {
	flex: 1;
	min-width: 0;
	font-size: 28rpx;
	color: #1c1c1e;
}

.current-text {
	color: rgba(60, 60, 67, 0.6);
}

.input-field {
	height: 56rpx;
	background: transparent;
}

.verify-row {
	display: flex;
	align-items: center;
	gap: 16rpx;
}

.verify-input {
	flex: 1;
	min-width: 0;
}

.verify-btn {
	height: 56rpx;
	padding: 0 22rpx;
	background: #111111;
	border-radius: 10rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	flex-shrink: 0;
}

.btn-disabled {
	background: rgba(120, 120, 128, 0.3);
}

.btn-text {
	font-size: 22rpx;
	color: #ffffff;
	font-weight: 500;
}

.help-section {
	display: flex;
	align-items: center;
	justify-content: center;
	gap: 8rpx;
	margin-top: 28rpx;
}

.help-icon {
	width: 26rpx;
	height: 26rpx;
	opacity: 0.5;
}

.help-text {
	font-size: 23rpx;
	color: rgba(60, 60, 67, 0.5);
}
</style>
