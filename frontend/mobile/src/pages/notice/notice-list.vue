<template>
	<view class="notice-page">
		<WebSocketKeeper />

		<!-- 顶部导航栏 -->
		<view class="top-bar safe-top-bar">
			<view class="top-bar-content">
				<view class="back-btn" @click="goBack">
					<image class="back-icon" src="/static/icons/arrow/left.svg" mode="aspectFit"></image>
				</view>
				<text class="page-title">通知</text>
				<view class="top-bar-placeholder"></view>
			</view>
		</view>

		<!-- 主内容区 -->
		<scroll-view class="main-content" scroll-y>
			<view class="content-wrapper">
				<view v-if="loading" class="loading-block">
					<text class="loading-text">正在同步通知…</text>
				</view>
				<view v-else-if="loadFailed" class="loading-block retry-block" @click="loadNotificationState">
					<text class="loading-text">同步失败，点击重试</text>
				</view>

				<template v-else>
					<!-- 好友申请入口 -->
					<view class="group-card">
						<view class="entry-row" hover-class="entry-row-hover" @click="handleNewFriends">
							<view class="entry-icon">
								<image class="entry-icon-img" src="/static/icons/user/person-add.svg" mode="aspectFit"></image>
								<view v-if="pendingCount > 0" class="entry-badge">
									<text class="entry-badge-text">{{ pendingCount > 99 ? '99+' : pendingCount }}</text>
								</view>
							</view>
							<view class="entry-info">
								<text class="entry-title">新的朋友</text>
								<text class="entry-desc">{{ pendingCount > 0 ? `${pendingCount} 个待处理申请` : '暂无待处理申请' }}</text>
							</view>
							<image class="entry-arrow" src="/static/icons/arrow/right.svg" mode="aspectFit"></image>
						</view>

						<view class="entry-row" hover-class="entry-row-hover" @click="handleNewFriends">
							<view class="entry-icon entry-icon-plain">
								<image class="entry-icon-img" src="/static/icons/user/person-add-outline.svg" mode="aspectFit"></image>
							</view>
							<view class="entry-info">
								<text class="entry-title">发出的申请</text>
								<text class="entry-desc">{{ sentCount > 0 ? `${sentCount} 条等待验证` : '暂无发出申请' }}</text>
							</view>
							<image class="entry-arrow" src="/static/icons/arrow/right.svg" mode="aspectFit"></image>
						</view>
					</view>

					<!-- 系统通知 -->
					<view class="section-block">
						<view class="section-head">
							<text class="section-title">系统通知</text>
						</view>
						<view class="empty-card">
							<image class="empty-icon" src="/static/icons/notice/bell-off.svg" mode="aspectFit"></image>
							<text class="empty-title">暂无系统通知</text>
							<text class="empty-desc">账号安全、服务变更等通知会在这里出现</text>
						</view>
					</view>
				</template>
			</view>
		</scroll-view>
	</view>
</template>

<script>
import WebSocketKeeper from '@/components/WebSocketKeeper.vue'
import { getSendFriendRequestList, getReceiveFriendRequestList } from '@/api/contact.js'

export default {
	components: {
		WebSocketKeeper
	},
	data() {
		return {
			loading: false,
			loaded: false,
			loadFailed: false,
			pendingCount: 0,
			sentCount: 0
		}
	},
	onLoad() {
		this.loadNotificationState()
	},
	onShow() {
		// 页面重新可见时静默刷新，不再把已有内容切回“同步中”
		if (this.loaded) this.loadNotificationState()
	},
	methods: {
		/** 给单次请求加超时，避免任一申请接口挂起时整页一直停在“同步中” */
		withTimeout(promise, ms = 7000) {
			return Promise.race([
				promise,
				new Promise((resolve, reject) => {
					setTimeout(() => reject(new Error('request timeout')), ms)
				})
			])
		},
		async loadNotificationState() {
			if (this.loading) return
			this.loadFailed = false
			this.loading = !this.loaded
			try {
				const [receiveRes, sendRes] = await Promise.all([
					this.withTimeout(getReceiveFriendRequestList()),
					this.withTimeout(getSendFriendRequestList())
				])
				const receiveList = this.normalizeList(receiveRes)
				const sendList = this.normalizeList(sendRes)
				this.pendingCount = receiveList.filter(item => Number(item.flowStatus) === 0).length
				this.sentCount = sendList.filter(item => Number(item.flowStatus) === 0).length
			} catch (error) {
				console.error('加载通知状态失败:', error)
				this.pendingCount = 0
				this.sentCount = 0
				this.loadFailed = !this.loaded
			} finally {
				this.loading = false
				this.loaded = true
			}
		},
		normalizeList(res) {
			if (!res || res.code !== 200 || !res.data) return []
			return Array.isArray(res.data) ? res.data : (res.data.list || [])
		},
		handleNewFriends() {
			uni.navigateTo({ url: '/pages/friends/new-friends' })
		},
		goBack() {
			const pages = getCurrentPages()
			if (pages.length > 1) {
				uni.navigateBack()
			} else {
				uni.redirectTo({ url: '/pages/contacts/contact-list' })
			}
		}
	}
}
</script>

<style lang="scss" scoped>
.notice-page {
	width: 100%;
	min-height: 100vh;
	background-color: #f2f2f7;
	display: flex;
	flex-direction: column;
}

/* 顶部导航栏 */
.top-bar {
	position: relative;
	background: rgba(255, 255, 255, 0.78);
	backdrop-filter: saturate(180%) blur(20px);
	-webkit-backdrop-filter: saturate(180%) blur(20px);
	border-bottom: 1rpx solid rgba(60, 60, 67, 0.12);
}

.top-bar-content {
	display: flex;
	align-items: center;
	justify-content: space-between;
	padding: 20rpx 24rpx;
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

.top-bar-placeholder {
	width: 64rpx;
	height: 64rpx;
}

.page-title {
	font-size: 36rpx;
	font-weight: 700;
	line-height: 1.2;
	letter-spacing: -0.02em;
	color: #1c1c1e;
}

/* 主内容区 */
.main-content {
	flex: 1;
	padding-top: 0;
	padding-bottom: 64rpx;
}

.content-wrapper {
	display: block;
	padding: 28rpx 24rpx 0;
	max-width: 928rpx;
	margin: 0 auto;
}

.loading-block {
	padding: 120rpx 0;
	display: flex;
	align-items: center;
	justify-content: center;
}

.loading-text {
	font-size: 26rpx;
	color: rgba(60, 60, 67, 0.55);
}

.retry-block {
	padding: 200rpx 0;
}

/* 申请入口 */
.group-card {
	display: block;
	background: #ffffff;
	border: 1rpx solid rgba(60, 60, 67, 0.12);
	border-radius: 20rpx;
	overflow: hidden;
}

.entry-row {
	display: flex;
	align-items: center;
	padding: 24rpx;
	border-bottom: 1rpx solid rgba(60, 60, 67, 0.12);
}

.entry-row:last-child {
	border-bottom: 0;
}

.entry-row-hover {
	background: #f2f2f7;
}

.entry-icon {
	position: relative;
	width: 72rpx;
	height: 72rpx;
	border-radius: 14rpx;
	background: #1c1c1e;
	display: flex;
	align-items: center;
	justify-content: center;
	overflow: visible;
}

.entry-icon-plain {
	background: #f2f2f7;
}

.entry-icon-img {
	width: 44rpx;
	height: 44rpx;
	opacity: 0.9;
	filter: invert(1);
}

.entry-icon-plain .entry-icon-img {
	opacity: 0.6;
	filter: none;
}

.entry-badge {
	position: absolute;
	top: -12rpx;
	right: -12rpx;
	min-width: 34rpx;
	height: 34rpx;
	padding: 0 8rpx;
	border-radius: 999rpx;
	background: #d92d20;
	border: 3rpx solid #ffffff;
	display: flex;
	align-items: center;
	justify-content: center;
}

.entry-badge-text {
	font-size: 18rpx;
	font-weight: 700;
	color: #ffffff;
	line-height: 1;
}

.entry-info {
	flex: 1;
	min-width: 0;
	display: flex;
	flex-direction: column;
	gap: 4rpx;
	margin-left: 22rpx;
}

.entry-title {
	font-size: 30rpx;
	font-weight: 500;
	color: #1c1c1e;
}

.entry-desc {
	font-size: 23rpx;
	color: rgba(60, 60, 67, 0.55);
}

.entry-arrow {
	width: 28rpx;
	height: 28rpx;
	opacity: 0.25;
	flex-shrink: 0;
}

/* 系统通知 */
.section-block {
	display: block;
	margin-top: 36rpx;
}

.section-head {
	display: flex;
	align-items: center;
	justify-content: space-between;
	padding: 0 8rpx;
	margin-bottom: 14rpx;
}

.section-title {
	font-size: 26rpx;
	font-weight: 600;
	color: #1c1c1e;
}

.empty-card {
	background: #ffffff;
	border: 1rpx solid rgba(60, 60, 67, 0.12);
	border-radius: 20rpx;
	padding: 28rpx 24rpx;
	display: flex;
	flex-direction: column;
	align-items: center;
}

.empty-icon {
	width: 56rpx;
	height: 56rpx;
	opacity: 0.25;
	margin-bottom: 12rpx;
}

.empty-title {
	font-size: 28rpx;
	font-weight: 500;
	color: #1c1c1e;
}

.empty-desc {
	margin-top: 8rpx;
	font-size: 23rpx;
	color: rgba(60, 60, 67, 0.45);
	text-align: center;
	line-height: 1.5;
}
</style>
