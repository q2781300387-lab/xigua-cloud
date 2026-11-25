<template>
	<view class="profile-container">
		<WebSocketKeeper />

	<!-- 顶部安全区 -->
		<view class="safe-top-bar"></view>

		<!-- 主要内容区域 -->
		<view class="main-content">
			<!-- 个人资料卡片 -->
			<view class="profile-card">
				<view class="profile-info">
					<view class="avatar-wrapper" @click="openProfileCard">
						<image
							v-if="userInfo && userInfo.avatar"
							class="avatar"
							:src="userInfo.avatar"
							mode="aspectFill"
						/>
						<view v-else class="avatar-placeholder">
							<text class="avatar-letter">{{ avatarLetter }}</text>
						</view>
						<view class="online-status"></view>
					</view>
					<view class="user-info" @click="openProfileCard">
						<text class="username">{{ userInfo && userInfo.username || '未登录' }}</text>
						<text class="user-id">微信号: xg_{{ userInfo && userInfo.id || '' }}</text>
						<text v-if="statusText" class="user-status">{{ statusText }}</text>
					</view>
					<view class="qrcode-icon" @click="openProfileCard">
						<image class="qrcode-img" src="/static/icons/chat/qr-code.svg" mode="aspectFit"></image>
					</view>
				</view>

				<!-- 操作按钮 -->
				<view class="action-buttons">
					<view class="action-btn" hover-class="action-btn-hover" @click="handleStatus">
						<image class="action-icon" src="/static/icons/profile/smiley.svg" mode="aspectFit"></image>
						<text class="action-text">{{ statusText || '设置状态' }}</text>
					</view>
					<view class="action-btn" hover-class="action-btn-hover" @click="handleEditProfile">
						<image class="action-icon" src="/static/icons/profile/pencil.svg" mode="aspectFit"></image>
						<text class="action-text">编辑资料</text>
					</view>
				</view>
			</view>

			<!-- 菜单列表 -->
			<view class="menu-list">
				<view class="menu-group">
					<view class="menu-item menu-item-border" hover-class="menu-item-hover" @click="handleNavigate('settings')">
						<view class="menu-icon-wrapper">
							<image class="menu-icon" src="/static/icons/auth/shield.svg" mode="aspectFit"></image>
						</view>
						<text class="menu-title">账号与安全</text>
						<image class="menu-arrow" src="/static/icons/arrow/right.svg" mode="aspectFit"></image>
					</view>

					<view class="menu-item menu-item-border" hover-class="menu-item-hover" @click="handleNavigate('card')">
						<view class="menu-icon-wrapper">
							<image class="menu-icon" src="/static/icons/chat/qr-code.svg" mode="aspectFit"></image>
						</view>
						<text class="menu-title">我的名片</text>
						<text class="menu-value">xg_{{ userInfo && userInfo.id || '' }}</text>
						<image class="menu-arrow" src="/static/icons/arrow/right.svg" mode="aspectFit"></image>
					</view>

					<view class="menu-item menu-item-border" hover-class="menu-item-hover" @click="handleNavigate('favorites')">
						<view class="menu-icon-wrapper">
							<image class="menu-icon" src="/static/icons/profile/star.svg" mode="aspectFit"></image>
						</view>
						<text class="menu-title">收藏夹</text>
						<image class="menu-arrow" src="/static/icons/arrow/right.svg" mode="aspectFit"></image>
					</view>

					<view class="menu-item" hover-class="menu-item-hover" @click="handleNavigate('settings')">
						<view class="menu-icon-wrapper">
							<image class="menu-icon" src="/static/icons/profile/gear.svg" mode="aspectFit"></image>
						</view>
						<text class="menu-title">设置</text>
						<image class="menu-arrow" src="/static/icons/arrow/right.svg" mode="aspectFit"></image>
					</view>
				</view>
			</view>
		</view>

		<!-- 名片弹层 -->
		<view v-if="showProfileCard" class="sheet-mask" @click="closeSheets">
			<view class="sheet-card" @click.stop>
				<view class="sheet-handle"></view>
				<view class="sheet-profile">
					<image v-if="userInfo && userInfo.avatar" class="sheet-avatar" :src="userInfo.avatar" mode="aspectFill" />
					<view v-else class="sheet-avatar sheet-avatar-placeholder">
						<text class="sheet-avatar-letter">{{ avatarLetter }}</text>
					</view>
					<text class="sheet-name">{{ userInfo && userInfo.username || '未登录' }}</text>
					<text v-if="statusText" class="sheet-status">{{ statusText }}</text>
					<text class="sheet-id">微信号：xg_{{ userInfo && userInfo.id || '' }}</text>
					<button class="sheet-copy-btn" hover-class="sheet-copy-hover" @click="copyUserId">复制微信号</button>
				</view>
				<view class="sheet-tip">对方可通过微信号搜索并添加你</view>
			</view>
		</view>

		<!-- 状态弹层 -->
		<view v-if="showStatusSheet" class="sheet-mask" @click="closeSheets">
			<view class="sheet-card" @click.stop>
				<view class="sheet-handle"></view>
				<view class="sheet-header">
					<text class="sheet-title">设置状态</text>
					<text class="sheet-action" @click="clearStatus">清除</text>
				</view>
				<view class="status-options">
					<view
						v-for="option in statusOptions"
						:key="option.label"
						class="status-option"
						:class="{ 'status-option-active': statusText === option.emoji + ' ' + option.label }"
						hover-class="status-option-hover"
						@click="selectStatus(option)"
					>
						<text class="status-emoji">{{ option.emoji }}</text>
						<text class="status-option-text">{{ option.label }}</text>
					</view>
				</view>
			</view>
		</view>

		<!-- 底部导航栏 -->
		<BottomNav :current="currentPath" @change="handleNavChange" />
	</view>
</template>

<script>
import BottomNav from '@/components/BottomNav.vue'
import WebSocketKeeper from '@/components/WebSocketKeeper.vue'

const STATUS_STORAGE_KEY = 'profileStatus'

export default {
	components: {
		WebSocketKeeper,
		BottomNav
	},
	onLoad() {
		this.loadUserInfo()
		this.loadStatus()
	},
	onShow() {
		this.loadUserInfo()
		this.loadStatus()
	},
	data() {
		return {
			userInfo: null,
			currentPath: '/pages/profile/profile',
			statusText: '',
			showProfileCard: false,
			showStatusSheet: false,
			statusOptions: [
				{ emoji: '😊', label: '在线' },
				{ emoji: '🎯', label: '专注' },
				{ emoji: '☕', label: '休息' },
				{ emoji: '🌙', label: '勿扰' }
			]
		}
	},
	computed: {
		avatarLetter() {
			const name = this.userInfo && this.userInfo.username
			return name ? String(name).charAt(0).toUpperCase() : 'X'
		}
	},
	methods: {
		loadUserInfo() {
			const userInfoStr = uni.getStorageSync('userInfo')
			if (userInfoStr) {
				try {
					this.userInfo = typeof userInfoStr === 'string' ? JSON.parse(userInfoStr) : userInfoStr
				} catch (e) {
					console.error('解析用户信息失败:', e)
					this.userInfo = null
				}
			}
		},
		loadStatus() {
			this.statusText = uni.getStorageSync(STATUS_STORAGE_KEY) || ''
		},
		openProfileCard() {
			this.showStatusSheet = false
			this.showProfileCard = true
		},
		handleStatus() {
			this.showProfileCard = false
			this.showStatusSheet = true
		},
		clearStatus() {
			this.statusText = ''
			uni.removeStorageSync(STATUS_STORAGE_KEY)
			this.showStatusSheet = false
		},
		selectStatus(option) {
			const value = `${option.emoji} ${option.label}`
			this.statusText = value
			uni.setStorageSync(STATUS_STORAGE_KEY, value)
			this.showStatusSheet = false
		},
		closeSheets() {
			this.showProfileCard = false
			this.showStatusSheet = false
		},
		copyUserId() {
			const id = this.userInfo && this.userInfo.id
			if (!id) {
				uni.showToast({ title: '暂无微信号', icon: 'none' })
				return
			}
			uni.setClipboardData({
				data: `xg_${id}`,
				success: () => {
					uni.showToast({ title: '已复制', icon: 'success' })
				}
			})
		},
		handleEditProfile() {
			uni.navigateTo({ url: '/pages/profile/edit-profile' })
		},
		handleNavigate(type) {
			if (type === 'settings') {
				uni.navigateTo({ url: '/pages/settings/settings' })
			} else if (type === 'card') {
				this.openProfileCard()
			} else if (type === 'favorites') {
				uni.navigateTo({ url: '/pages/profile/favorites' })
			}
		},
		handleNavChange(path) {
			this.currentPath = path
		}
	}
}
</script>

<style lang="scss" scoped>
.profile-container {
	width: 100%;
	height: 100vh;
	background-color: #f2f2f7;
	display: flex;
	flex-direction: column;
	overflow-y: auto;
}

/* 顶部导航栏 */
.header {
	position: relative;
	background: rgba(255, 255, 255, 0.78);
	backdrop-filter: saturate(180%) blur(20px);
	-webkit-backdrop-filter: saturate(180%) blur(20px);
	border-bottom: 1rpx solid rgba(60, 60, 67, 0.12);
}

/* 主要内容区域 */
.main-content {
	display: block;
	flex: 1;
	padding-bottom: 200rpx;
}

/* 个人资料卡片 */
.profile-card {
	display: block;
	margin: 24rpx;
	background-color: #ffffff !important;
	border-radius: 20rpx;
	padding: 36rpx 32rpx 28rpx;
	border: 1rpx solid rgba(60, 60, 67, 0.12);
	position: relative;
	overflow: hidden;
}

/* 个人信息 */
.profile-info {
	display: flex;
	align-items: center;
	gap: 24rpx;
}

.avatar-wrapper {
	position: relative;
	width: 128rpx;
	height: 128rpx;
	flex-shrink: 0;
}

.avatar {
	width: 128rpx;
	height: 128rpx;
	border-radius: 50%;
	display: block;
	object-fit: cover;
	overflow: hidden;
}

.avatar-placeholder {
	width: 100%;
	height: 100%;
	border-radius: 50%;
	background: #1c1c1e;
	display: flex;
	align-items: center;
	justify-content: center;
}

.avatar-letter {
	font-size: 44rpx;
	font-weight: 600;
	color: #ffffff;
}

.online-status {
	position: absolute;
	bottom: 6rpx;
	right: 6rpx;
	width: 26rpx;
	height: 26rpx;
	background-color: #10b981;
	border: 4rpx solid #ffffff;
	border-radius: 50%;
}

.user-info {
	min-width: 0;
	overflow: hidden;
	flex: 1;
	display: flex;
	flex-direction: column;
	gap: 4rpx;
}

.username {
	font-size: 40rpx;
	font-weight: 600;
	color: #1c1c1e;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
	letter-spacing: -0.02em;
}

.user-id {
	font-size: 23rpx;
	color: rgba(60, 60, 67, 0.55);
}

.user-status {
	font-size: 23rpx;
	color: #1c1c1e;
	margin-top: 2rpx;
}

.qrcode-icon {
	width: 56rpx;
	height: 56rpx;
	flex-shrink: 0;
	display: flex;
	align-items: center;
	justify-content: center;
	transition: transform 0.15s;
}

.qrcode-img {
	width: 44rpx;
	height: 44rpx;
	opacity: 0.5;
}

.qrcode-icon:active {
	transform: scale(0.92);
}

/* 操作按钮 */
.action-buttons {
	display: flex;
	gap: 16rpx;
	margin-top: 28rpx;
}

.action-btn {
	flex: 1;
	display: flex;
	align-items: center;
	justify-content: center;
	gap: 10rpx;
	height: 68rpx;
	background: #f2f2f7;
	border-radius: 12rpx;
}

.action-btn-hover {
	background: #e9e9ee;
}

.action-icon {
	width: 32rpx;
	height: 32rpx;
	opacity: 0.7;
}

.action-text {
	font-size: 25rpx;
	font-weight: 500;
	color: #1c1c1e;
	max-width: 240rpx;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
}

/* 菜单列表 */
.menu-list {
	padding: 0 24rpx;
}

.menu-group {
	display: block;
	background: #ffffff !important;
	border-radius: 20rpx;
	border: 1rpx solid rgba(60, 60, 67, 0.12);
	overflow: hidden;
	margin-bottom: 24rpx;
}

.menu-item {
	display: flex;
	align-items: center;
	padding: 24rpx;
	transition: background-color 0.15s;
}

.menu-item-border {
	border-bottom: 1rpx solid rgba(60, 60, 67, 0.12);
}

.menu-item-hover {
	background: #f2f2f7;
}

.menu-icon-wrapper {
	width: 64rpx;
	height: 64rpx;
	border-radius: 12rpx;
	background: #f2f2f7;
	display: flex;
	align-items: center;
	justify-content: center;
	margin-right: 20rpx;
}

.menu-icon {
	width: 34rpx;
	height: 34rpx;
	opacity: 0.55;
}

.menu-title {
	flex: 1;
	font-size: 28rpx;
	font-weight: 500;
	color: #1c1c1e;
}

.menu-value {
	font-size: 23rpx;
	color: rgba(60, 60, 67, 0.5);
	margin-right: 10rpx;
	max-width: 260rpx;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
}

.menu-arrow {
	width: 26rpx;
	height: 26rpx;
	opacity: 0.24;
	flex-shrink: 0;
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
}

.sheet-handle {
	width: 72rpx;
	height: 8rpx;
	border-radius: 999rpx;
	background: rgba(60, 60, 67, 0.2);
	margin: 0 auto 28rpx;
}

.sheet-profile {
	display: flex;
	flex-direction: column;
	align-items: center;
	padding: 8rpx 0 16rpx;
}

.sheet-avatar {
	width: 140rpx;
	height: 140rpx;
	border-radius: 50%;
	object-fit: cover;
	display: block;
	overflow: hidden;
}

.sheet-avatar-placeholder {
	background: #1c1c1e;
	display: flex;
	align-items: center;
	justify-content: center;
}

.sheet-avatar-letter {
	font-size: 52rpx;
	font-weight: 600;
	color: #ffffff;
}

.sheet-name {
	margin-top: 18rpx;
	font-size: 34rpx;
	font-weight: 600;
	color: #1c1c1e;
}

.sheet-status {
	margin-top: 6rpx;
	font-size: 23rpx;
	color: rgba(60, 60, 67, 0.6);
}

.sheet-id {
	margin-top: 10rpx;
	font-size: 24rpx;
	color: rgba(60, 60, 67, 0.5);
}

.sheet-copy-btn {
	margin-top: 28rpx;
	height: 80rpx;
	line-height: 80rpx;
	padding: 0 56rpx;
	border-radius: 14rpx;
	background: #111111;
	color: #ffffff;
	font-size: 27rpx;
	font-weight: 600;
	border: 0;
}

.sheet-copy-btn::after {
	border: 0;
}

.sheet-copy-hover {
	opacity: 0.86;
}

.sheet-tip {
	text-align: center;
	font-size: 22rpx;
	color: rgba(60, 60, 67, 0.4);
	margin-top: 20rpx;
}

.sheet-header {
	display: flex;
	align-items: center;
	justify-content: space-between;
	padding: 0 4rpx 20rpx;
}

.sheet-title {
	font-size: 30rpx;
	font-weight: 600;
	color: #1c1c1e;
}

.sheet-action {
	font-size: 25rpx;
	color: rgba(60, 60, 67, 0.6);
}

.status-options {
	display: flex;
	flex-direction: column;
}

.status-option {
	display: flex;
	align-items: center;
	height: 92rpx;
	padding: 0 8rpx;
	border-bottom: 1rpx solid rgba(60, 60, 67, 0.12);
}

.status-option:last-child {
	border-bottom: 0;
}

.status-option-active {
	background: #f2f2f7;
	border-radius: 12rpx;
}

.status-option-hover {
	background: #f2f2f7;
}

.status-emoji {
	font-size: 36rpx;
	margin-right: 16rpx;
}

.status-option-text {
	font-size: 28rpx;
	color: #1c1c1e;
}
</style>
