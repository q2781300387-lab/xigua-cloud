<template>
	<view class="edit-profile-page">
		<WebSocketKeeper />
		<!-- 顶部导航栏 -->
		<view class="top-bar safe-top-bar">
			<view class="top-bar-content">
				<text class="cancel-btn" @click="handleCancel">取消</text>
				<text class="page-title">编辑资料</text>
				<text class="save-btn" :class="{ 'save-disabled': saving }" @click="handleSave">{{ saving ? '保存中' : '保存' }}</text>
			</view>
		</view>

		<!-- 主内容区 -->
		<scroll-view class="main-content" scroll-y="true">
			<view class="content-wrapper">
				<!-- 头像区域 -->
				<view class="avatar-section">
					<view class="avatar-container" @click="handleChangeAvatar">
						<Avatar :src="profileData.avatar" :size="160" />
						<view class="camera-badge">
							<image class="camera-icon" src="/static/icons/media/camera.svg" mode="aspectFit"></image>
						</view>
					</view>
					<text class="change-avatar-btn" @click="handleChangeAvatar">修改头像</text>
				</view>

				<!-- 表单区域 -->
				<view class="form-section">
					<!-- 用户名（只读） -->
					<view class="form-item readonly-item">
						<view class="field-content">
							<view class="field-info">
								<text class="field-label">用户名</text>
								<text class="field-value readonly-value">{{ profileData.username || '未设置' }}</text>
							</view>
							<image class="lock-icon" src="/static/icons/profile/lock-outline.svg" mode="aspectFit"></image>
						</view>
					</view>

					<!-- 性别 -->
					<view class="form-item">
						<view class="field-content" @tap="chooseGender">
							<text class="field-label inline-label">性别</text>
							<view class="picker-view">
								<text class="picker-text">{{ genderOptions[genderIndex] }}</text>
								<image class="dropdown-icon" src="/static/icons/arrow/down.svg" mode="aspectFit"></image>
							</view>
						</view>
					</view>

					<!-- 地区 -->
					<view class="form-item">
						<view class="field-content" @tap="chooseRegion">
							<text class="field-label inline-label">地区</text>
							<view class="picker-view">
								<text class="picker-text">{{ regionOptions[regionIndex] }}</text>
								<image class="dropdown-icon" src="/static/icons/arrow/down.svg" mode="aspectFit"></image>
							</view>
						</view>
					</view>

					<!-- 邮箱 -->
					<view class="form-item clickable-item" @click="handleChangeEmail">
						<view class="field-content">
							<view class="field-info">
								<text class="field-label">邮箱</text>
								<text class="field-value">{{ profileData.email || '未绑定' }}</text>
							</view>
							<image class="arrow-icon" src="/static/icons/arrow/right.svg" mode="aspectFit"></image>
						</view>
					</view>

					<!-- 个性签名 -->
					<view class="form-item signature-item">
						<text class="field-label">个性签名</text>
						<textarea
							class="signature-input"
							v-model="profileData.signature"
							placeholder="写一句介绍自己"
							maxlength="100"
							auto-height
						/>
						<text class="signature-count">{{ (profileData.signature || '').length }}/100</text>
					</view>
				</view>

				<text class="form-tip">头像与资料会展示给你的好友</text>
			</view>
		</scroll-view>
		<AvatarCropper
			:visible="cropOpen"
			:src="cropSrc"
			@cancel="closeAvatarCrop"
			@confirm="confirmAvatarCrop"
			@error="handleCropError"
		/>
	</view>
</template>

<script>
import WebSocketKeeper from '@/components/WebSocketKeeper.vue'
import Avatar from '@/components/avatar/avatar.vue'
import AvatarCropper from '@/components/avatar-cropper/avatar-cropper.vue'
import { getUserInfo, updateUserInfo, uploadAvatar } from '@/api/user.js'
import { uploadFile } from '@/api/file.js'
import config from '@/config/index.js'

export default {
	components: {
		WebSocketKeeper,
		Avatar,
		AvatarCropper
	},
	data() {
		return {
			profileData: {
				avatar: '',
				username: '',
				email: '',
				signature: ''
			},
			genderOptions: ['男', '女', '保密'],
			genderIndex: 2,
			regionOptions: ['中国 河北 廊坊', '中国 北京', '中国 上海', '中国 广州', '中国 深圳', '中国 杭州', '海外'],
			regionIndex: 0,
			saving: false,
			cropOpen: false,
			cropSrc: ''
		};
	},
	onLoad() {
		this.loadUserInfo();
	},
	methods: {
		async loadUserInfo() {
			let cached = {};
			try {
				const raw = uni.getStorageSync(config.userInfoKey);
				cached = raw ? (typeof raw === 'string' ? JSON.parse(raw) : raw) : {};
			} catch (e) {
				cached = {};
			}
			this.applyUser(cached);

			try {
				const res = await getUserInfo();
				if (res && res.data) {
					this.applyUser(res.data);
					uni.setStorageSync(config.userInfoKey, JSON.stringify(res.data));
				}
			} catch (e) {
				console.warn('获取用户信息失败，使用本地缓存', e);
			}
		},
		applyUser(user) {
			this.profileData.avatar = user.avatar || '';
			this.profileData.username = user.username || '';
			this.profileData.email = user.email || '';
			this.profileData.signature = user.signature || '';
			const sex = Number(user.sex);
			this.genderIndex = sex === 1 ? 0 : (sex === 2 ? 1 : 2);
			const region = user.region || '';
			if (region) {
				const idx = this.regionOptions.indexOf(region);
				if (idx >= 0) {
					this.regionIndex = idx;
				} else {
					this.regionOptions.unshift(region);
					this.regionIndex = 0;
				}
			}
		},
		handleCancel() {
			uni.navigateBack();
		},
		async handleSave() {
			if (this.saving) {
				return;
			}
			if (this.profileData.signature && this.profileData.signature.length > 100) {
				uni.showToast({ title: '签名最多 100 字', icon: 'none' });
				return;
			}
			this.saving = true;
			try {
				const payload = {
					username: this.profileData.username,
					email: this.profileData.email,
					avatar: this.profileData.avatar,
					sex: this.genderIndex === 0 ? 1 : (this.genderIndex === 1 ? 2 : 0),
					region: this.regionOptions[this.regionIndex] || '',
					signature: this.profileData.signature || ''
				};
				await updateUserInfo(payload);

				const cached = JSON.parse(uni.getStorageSync(config.userInfoKey) || '{}');
				uni.setStorageSync(config.userInfoKey, JSON.stringify(Object.assign({}, cached, payload)));

				uni.showToast({ title: '保存成功', icon: 'success' });
				setTimeout(() => {
					uni.navigateBack();
				}, 1200);
			} catch (e) {
				uni.showToast({ title: e.message || '保存失败', icon: 'none' });
			} finally {
				this.saving = false;
			}
		},
		handleChangeAvatar() {
			uni.chooseImage({
				count: 1,
				sizeType: ['compressed'],
				sourceType: ['album', 'camera'],
				success: async (res) => {
					const info = (res.tempFiles && res.tempFiles[0]) || {};
					if (info.size && info.size > 8 * 1024 * 1024) {
						uni.showToast({ title: '图片不能超过 8MB', icon: 'none' });
						return;
					}
					// #ifdef H5
					this.cropSrc = res.tempFilePaths[0];
					this.cropOpen = true;
					// #endif
					// #ifndef H5
					this.uploadSelectedAvatar(res.tempFilePaths[0]);
					// #endif
				}
			});
		},
		closeAvatarCrop() {
			this.cropOpen = false;
			this.releaseCropSrc();
		},
		releaseCropSrc() {
			// #ifdef H5
			if (this.cropSrc && this.cropSrc.indexOf('blob:') === 0) {
				try {
					URL.revokeObjectURL(this.cropSrc);
				} catch (e) {
					// ignore
				}
			}
			// #endif
			this.cropSrc = '';
		},
		handleCropError(message) {
			this.closeAvatarCrop();
			uni.showToast({ title: message || '裁剪失败，请重试', icon: 'none' });
		},
		confirmAvatarCrop(blob) {
			this.cropOpen = false;
			this.releaseCropSrc();
			this.uploadSelectedAvatar(blob);
		},
		async uploadSelectedAvatar(fileArg) {
			uni.showLoading({ title: '上传中...', mask: true });
			try {
				const uploadRes = await uploadFile(fileArg);
				const url = uploadRes.data;
				if (!url) {
					throw new Error('上传失败');
				}
				await uploadAvatar(url);
				this.profileData.avatar = url;
				const cached = JSON.parse(uni.getStorageSync(config.userInfoKey) || '{}');
				uni.setStorageSync(config.userInfoKey, JSON.stringify(Object.assign({}, cached, { avatar: url })));
				uni.showToast({ title: '头像已更新', icon: 'success' });
			} catch (e) {
				uni.showToast({ title: e.message || '头像上传失败', icon: 'none' });
			} finally {
				uni.hideLoading();
			}
		},
		handleChangeEmail() {
			uni.navigateTo({ url: '/pages/profile/change-email' });
		},
		chooseGender() {
			uni.showActionSheet({
				itemList: this.genderOptions,
				success: (res) => {
					this.genderIndex = res.tapIndex;
				}
			});
		},
		chooseRegion() {
			uni.showActionSheet({
				itemList: this.regionOptions,
				success: (res) => {
					this.regionIndex = res.tapIndex;
				}
			});
		}
	}
};
</script>

<style lang="scss" scoped>
.edit-profile-page {
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
.save-btn {
	font-size: 30rpx;
	font-weight: 500;
	color: #1c1c1e;
}

.save-btn {
	font-weight: 600;
}

.save-disabled {
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
	padding-bottom: 48rpx;
	height: 100vh;
	box-sizing: border-box;
}

.content-wrapper {
	display: block;
	width: 100%;
	max-width: 928rpx;
	margin: 0 auto;
	padding: 48rpx 28rpx 0;
	box-sizing: border-box;
}

/* 头像区域 */
.avatar-section {
	display: flex;
	flex-direction: column;
	align-items: center;
	margin-bottom: 40rpx;
}

.avatar-container {
	position: relative;
	width: 160rpx;
	height: 160rpx;
	border-radius: 50%;
}

.camera-badge {
	position: absolute;
	bottom: 0;
	right: 0;
	width: 52rpx;
	height: 52rpx;
	background-color: #111111;
	border-radius: 50%;
	border: 4rpx solid #f2f2f7;
	display: flex;
	align-items: center;
	justify-content: center;
}

.camera-icon {
	width: 28rpx;
	height: 28rpx;
}

.change-avatar-btn {
	margin-top: 20rpx;
	font-size: 25rpx;
	font-weight: 500;
	color: rgba(60, 60, 67, 0.6);
}

.change-avatar-btn:active {
	opacity: 0.7;
}

/* 表单区域 */
.form-section {
	display: block;
	background: #ffffff;
	border-radius: 18rpx;
	border: 1rpx solid rgba(60, 60, 67, 0.08);
	overflow: hidden;
}

.form-item {
	padding: 26rpx 28rpx;
	border-bottom: 1rpx solid rgba(60, 60, 67, 0.1);
}

.form-item:last-child {
	border-bottom: none;
}

.field-content {
	display: flex;
	align-items: center;
	justify-content: space-between;
}

.field-info {
	flex: 1;
	min-width: 0;
}

.field-label {
	display: block;
	font-size: 23rpx;
	font-weight: 500;
	color: rgba(60, 60, 67, 0.55);
	margin-bottom: 8rpx;
}

.inline-label {
	margin-bottom: 0;
	font-size: 28rpx;
	color: #1c1c1e;
	font-weight: 400;
}

.field-value {
	display: block;
	font-size: 29rpx;
	font-weight: 500;
	color: #1c1c1e;
	word-break: break-all;
}

.readonly-value {
	color: rgba(60, 60, 67, 0.5);
}

.lock-icon,
.arrow-icon {
	width: 32rpx;
	height: 32rpx;
	opacity: 0.45;
	flex-shrink: 0;
}

.picker-view {
	display: flex;
	align-items: center;
	gap: 10rpx;
}

.picker-text {
	font-size: 29rpx;
	color: rgba(60, 60, 67, 0.75);
}

.dropdown-icon {
	width: 26rpx;
	height: 26rpx;
	opacity: 0.4;
}

.signature-input {
	width: 100%;
	min-height: 90rpx;
	font-size: 29rpx;
	color: #1c1c1e;
	line-height: 1.5;
}

.signature-count {
	display: block;
	text-align: right;
	font-size: 21rpx;
	color: rgba(60, 60, 67, 0.35);
	margin-top: 6rpx;
}

.form-tip {
	display: block;
	text-align: center;
	font-size: 22rpx;
	color: rgba(60, 60, 67, 0.4);
	margin-top: 24rpx;
}
</style>
