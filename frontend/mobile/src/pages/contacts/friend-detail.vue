<template>
	<view class="page">
		<!-- 顶部导航 -->
		<view class="top-bar safe-top-bar">
			<view class="top-bar-inner">
				<view class="back-btn" @click="goBack">
					<image class="back-icon" src="/static/icons/arrow/left.svg" mode="aspectFit"></image>
				</view>
				<text class="page-title">好友详情</text>
				<view class="top-bar-placeholder"></view>
			</view>
		</view>

		<scroll-view class="main-content" scroll-y>
			<view class="content">
				<!-- 资料卡 -->
				<view class="hero-card">
					<avatar :src="detail.avatar" :size="160" shape="circle" />
					<text class="hero-name">{{ detail.username || detail.nickname || '好友' }}</text>
					<view class="hero-status" :class="{ 'hero-status-online': detail.isOnline }">
						<view class="status-dot" :class="{ 'status-dot-online': detail.isOnline }"></view>
						<text class="status-label">{{ detail.isOnline ? '在线' : '离线' }}</text>
					</view>
					<text v-if="detail.signature" class="hero-signature">{{ detail.signature }}</text>
					<text v-else class="hero-signature hero-signature-muted">这个人很安静，还没有留下签名</text>
				</view>

				<!-- 基本信息 -->
				<view class="info-card">
					<view class="info-row">
						<text class="info-label">昵称</text>
						<text class="info-value">{{ detail.username || '未设置' }}</text>
					</view>
					<view class="info-row">
						<text class="info-label">性别</text>
						<text class="info-value">{{ genderText }}</text>
					</view>
					<view class="info-row">
						<text class="info-label">地区</text>
						<text class="info-value">{{ detail.region || '未设置' }}</text>
					</view>
					<view class="info-row">
						<text class="info-label">邮箱</text>
						<text class="info-value">{{ detail.email || '未公开' }}</text>
					</view>
					<view class="info-row">
						<text class="info-label">成为好友</text>
						<text class="info-value">{{ formatDateTime(detail.createFriendTime) }}</text>
					</view>
				</view>

				<!-- 操作 -->
				<view class="action-group">
					<view class="primary-btn" hover-class="primary-btn-hover" @click="sendMessage">发消息</view>
					<view class="block-btn" hover-class="block-btn-hover" @click="toggleBlock">{{ blocked ? '移出黑名单' : '加入黑名单' }}</view>
					<view class="danger-btn" hover-class="danger-btn-hover" @click="removeFriend">删除好友</view>
				</view>

				<text class="page-hint">删除好友后，双方将不再出现在彼此的联系人中。</text>
			</view>
		</scroll-view>
	</view>
</template>

<script>
import Avatar from '@/components/avatar/avatar.vue'
import { getFriendDetail, delFriend } from '@/api/friend.js'
import { blockUser, unblockUser, isUserBlocked } from '@/api/privacy.js'

export default {
	components: {
		Avatar
	},
	data() {
		return {
			friendId: '',
			blocked: false,
			detail: {
				userId: '',
				username: '',
				avatar: '',
				signature: '',
				sex: null,
				region: '',
				email: '',
				phone: '',
				registerTime: '',
				createFriendTime: '',
				isOnline: false
			}
		}
	},
	computed: {
		genderText() {
			if (this.detail.sex === 1) return '男'
			if (this.detail.sex === 2) return '女'
			return '未设置'
		}
	},
	onLoad(options) {
		this.friendId = decodeURIComponent(options.id || '')
		// 先用列表页传入的信息占位，避免详情请求返回前白屏
		this.detail.userId = this.friendId
		this.detail.username = decodeURIComponent(options.name || '')
		this.detail.avatar = decodeURIComponent(options.avatar || '')
		this.loadDetail()
	},
	methods: {
		async loadDetail() {
			if (!this.friendId) {
				uni.showToast({ title: '缺少好友信息', icon: 'none' })
				return
			}
			try {
				const res = await getFriendDetail(this.friendId)
				if (res && res.code === 200 && res.data) {
					this.detail = { ...this.detail, ...res.data }
				}
				try {
					const blockRes = await isUserBlocked(this.friendId)
					this.blocked = !!(blockRes && blockRes.data)
				} catch (e) {}
			} catch (error) {
				console.error('获取好友详情失败:', error)
			}
		},
		formatDateTime(value) {
			if (!value) return '未知'
			const date = new Date(value)
			if (Number.isNaN(date.getTime())) return String(value)
			const year = date.getFullYear()
			const month = String(date.getMonth() + 1).padStart(2, '0')
			const day = String(date.getDate()).padStart(2, '0')
			return `${year}-${month}-${day}`
		},
		sendMessage() {
			const currentChat = {
				chatId: this.detail.userId || this.friendId,
				chatType: 1,
				chatName: this.detail.username || '好友',
				avatar: this.detail.avatar || '',
				lastMessageContent: null,
				unreadCount: 0,
				isOnline: !!this.detail.isOnline,
				updateTime: Date.now()
			}
			uni.setStorageSync('currentChat', JSON.stringify(currentChat))
			uni.navigateTo({
				url: `/pages/chat/chat-detail?id=${encodeURIComponent(currentChat.chatId)}&name=${encodeURIComponent(currentChat.chatName)}&chatType=1`
			})
		},
		async toggleBlock() {
			if (this.blocked) {
				try {
					await unblockUser(this.friendId)
					this.blocked = false
					uni.showToast({ title: '已移出黑名单', icon: 'none' })
				} catch (e) {}
				return
			}
			uni.showModal({
				title: '加入黑名单',
				content: `加入后「${this.detail.username || '该好友'}」发来的消息将被屏蔽。`,
				confirmText: '加入',
				confirmColor: '#111111',
				success: async (res) => {
					if (!res.confirm) return
					try {
						await blockUser(this.friendId)
						this.blocked = true
						uni.showToast({ title: '已加入黑名单', icon: 'none' })
					} catch (e) {}
				}
			})
		},
		removeFriend() {
			uni.showModal({
				title: '删除好友',
				content: `确定删除“${this.detail.username || '该好友'}”吗？`,
				confirmText: '删除',
				confirmColor: '#d92d20',
				success: async (res) => {
					if (!res.confirm) return
					try {
						await delFriend(this.friendId)
						uni.showToast({ title: '已删除好友', icon: 'success' })
						uni.setStorageSync('contactsRefresh', 1)
						setTimeout(() => {
							uni.navigateBack()
						}, 800)
					} catch (error) {
						console.error('删除好友失败:', error)
					}
				}
			})
		},
		goBack() {
			uni.navigateBack({
				fail: () => {
					uni.redirectTo({ url: '/pages/contacts/contact-list' })
				}
			})
		}
	}
}
</script>

<style lang="scss" scoped>
.page {
	display: block;
	min-height: 100vh;
	background-color: #f2f2f7;
}

.top-bar {
	position: fixed;
	top: 0;
	left: 0;
	right: 0;
	z-index: 50;
	background: rgba(255, 255, 255, 0.86);
	border-bottom: 1rpx solid rgba(60, 60, 67, 0.12);
	backdrop-filter: saturate(180%) blur(20px);
	-webkit-backdrop-filter: saturate(180%) blur(20px);
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

.back-icon {
	width: 40rpx;
	height: 40rpx;
	opacity: 0.75;
}

.page-title {
	font-size: 34rpx;
	font-weight: 600;
	color: #1c1c1e;
	letter-spacing: -0.01em;
}

.top-bar-placeholder {
	width: 64rpx;
}

.main-content {
	height: 100vh;
	padding-top: 0;
	padding-bottom: 0;
}

.content {
	display: block;
	padding: calc(var(--status-bar-height) + 124rpx) 24rpx 64rpx;
	max-width: 720rpx;
	margin: 0 auto;
}

.hero-card {
	background: #ffffff;
	border: 1rpx solid rgba(60, 60, 67, 0.12);
	border-radius: 20rpx;
	padding: 48rpx 32rpx 40rpx;
	display: flex;
	flex-direction: column;
	align-items: center;
}

.hero-name {
	margin-top: 24rpx;
	font-size: 40rpx;
	font-weight: 600;
	color: #1c1c1e;
	letter-spacing: -0.02em;
}

.hero-status {
	margin-top: 12rpx;
	display: flex;
	align-items: center;
	gap: 8rpx;
	padding: 4rpx 16rpx;
	border-radius: 999rpx;
	background: rgba(120, 120, 128, 0.12);
}

.status-dot {
	width: 12rpx;
	height: 12rpx;
	border-radius: 50%;
	background: rgba(60, 60, 67, 0.4);
}

.status-dot-online {
	background: #10b981;
}

.status-label {
	font-size: 22rpx;
	color: rgba(60, 60, 67, 0.65);
}

.hero-status-online .status-label {
	color: #0b7a56;
}

.hero-signature {
	margin-top: 24rpx;
	font-size: 26rpx;
	line-height: 1.6;
	color: rgba(60, 60, 67, 0.7);
	text-align: center;
	padding: 0 24rpx;
}

.hero-signature-muted {
	color: rgba(60, 60, 67, 0.4);
}

.info-card {
	display: block;
	margin-top: 24rpx;
	background: #ffffff;
	border: 1rpx solid rgba(60, 60, 67, 0.12);
	border-radius: 20rpx;
	overflow: hidden;
}

.info-row {
	display: flex;
	align-items: center;
	justify-content: space-between;
	gap: 32rpx;
	padding: 26rpx 28rpx;
	border-bottom: 1rpx solid rgba(60, 60, 67, 0.12);
}

.info-row:last-child {
	border-bottom: 0;
}

.info-label {
	font-size: 26rpx;
	color: rgba(60, 60, 67, 0.6);
	flex-shrink: 0;
}

.info-value {
	font-size: 27rpx;
	color: #1c1c1e;
	text-align: right;
	word-break: break-all;
}

.action-group {
	margin-top: 36rpx;
	display: flex;
	flex-direction: column;
	gap: 20rpx;
}

.primary-btn {
	display: flex;
	align-items: center;
	justify-content: center;
	height: 92rpx;
	border-radius: 16rpx;
	background: #111111;
	color: #ffffff;
	font-size: 30rpx;
	font-weight: 600;
	border: 0;
	box-sizing: border-box;
}

.primary-btn::after {
	border: 0;
}

.primary-btn-hover {
	opacity: 0.86;
}

.block-btn {
	display: flex;
	align-items: center;
	justify-content: center;
	height: 92rpx;
	border-radius: 16rpx;
	background: #ffffff;
	color: #1c1c1e;
	font-size: 30rpx;
	font-weight: 500;
	border: 1rpx solid rgba(0, 0, 0, 0.16);
	box-sizing: border-box;
}

.block-btn-hover {
	background: #f2f2f7;
}

.danger-btn {
	display: flex;
	align-items: center;
	justify-content: center;
	height: 92rpx;
	border-radius: 16rpx;
	background: #ffffff;
	color: #d92d20;
	font-size: 30rpx;
	font-weight: 500;
	border: 1rpx solid rgba(217, 45, 32, 0.25);
	box-sizing: border-box;
}

.danger-btn::after {
	border: 0;
}

.danger-btn-hover {
	background: rgba(217, 45, 32, 0.06);
}

.page-hint {
	display: block;
	margin-top: 24rpx;
	text-align: center;
	font-size: 22rpx;
	color: rgba(60, 60, 67, 0.4);
}
</style>
