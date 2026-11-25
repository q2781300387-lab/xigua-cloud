<template>
	<view class="discover-page">
		<!-- 顶部导航栏 -->
		<view class="top-bar safe-top-bar">
			<view class="top-bar-content">
				<text class="page-title">发现</text>
			</view>
		</view>

		<!-- 主内容区 -->
		<scroll-view class="main-content" scroll-y>
			<view class="content-wrapper">
				<!-- 社交 -->
				<view class="discover-card">
					<view class="discover-row" hover-class="discover-row-hover" @click="goPage('/pages/moments/moments')">
						<view class="row-icon">
							<image class="row-icon-img" src="/static/icons/feature/moments.svg" mode="aspectFit"></image>
						</view>
						<text class="row-label">朋友圈</text>
						<image class="row-arrow" src="/static/icons/arrow/right.svg" mode="aspectFit"></image>
					</view>
					<view class="row-divider"></view>
					<view class="discover-row" hover-class="discover-row-hover" @click="goPage('/pages/treehole/treehole')">
						<view class="row-icon">
							<image class="row-icon-img" src="/static/icons/feature/treehole.svg" mode="aspectFit"></image>
						</view>
						<text class="row-label">匿名树洞</text>
						<image class="row-arrow" src="/static/icons/arrow/right.svg" mode="aspectFit"></image>
					</view>
					<view class="row-divider"></view>
					<view class="discover-row" hover-class="discover-row-hover" @click="goPage('/pages/bots/bots')">
						<view class="row-icon">
							<image class="row-icon-img" src="/static/icons/feature/robot.svg" mode="aspectFit"></image>
						</view>
						<text class="row-label">机器人</text>
						<image class="row-arrow" src="/static/icons/arrow/right.svg" mode="aspectFit"></image>
					</view>
					<view class="row-divider"></view>
					<view class="discover-row" hover-class="discover-row-hover" @click="goAiChat">
						<view class="row-icon">
							<image class="row-icon-img" src="/static/icons/feature/ai.svg" mode="aspectFit"></image>
						</view>
						<text class="row-label">AI 聊天</text>
						<image class="row-arrow" src="/static/icons/arrow/right.svg" mode="aspectFit"></image>
					</view>
				</view>

				<!-- 联系人 -->
				<view class="discover-card">
					<view class="discover-row" hover-class="discover-row-hover" @click="goPage('/pages/contacts/add-friend')">
						<view class="row-icon">
							<image class="row-icon-img" src="/static/icons/user/person-add.svg" mode="aspectFit"></image>
						</view>
						<text class="row-label">添加好友</text>
						<image class="row-arrow" src="/static/icons/arrow/right.svg" mode="aspectFit"></image>
					</view>
					<view class="row-divider"></view>
					<view class="discover-row" hover-class="discover-row-hover" @click="goPage('/pages/contacts/create-group')">
						<view class="row-icon">
							<image class="row-icon-img" src="/static/icons/user/group-outline.svg" mode="aspectFit"></image>
						</view>
						<text class="row-label">发起群聊</text>
						<image class="row-arrow" src="/static/icons/arrow/right.svg" mode="aspectFit"></image>
					</view>
				</view>

				<!-- 消息与通知 -->
				<view class="discover-card">
					<view class="discover-row" hover-class="discover-row-hover" @click="goPage('/pages/notice/notice-list')">
						<view class="row-icon">
							<image class="row-icon-img" src="/static/icons/notice/bell.svg" mode="aspectFit"></image>
						</view>
						<text class="row-label">通知中心</text>
						<text v-if="pendingCount > 0" class="row-note">{{ pendingCount > 99 ? '99+' : pendingCount }} 条待处理</text>
						<image class="row-arrow" src="/static/icons/arrow/right.svg" mode="aspectFit"></image>
					</view>
				</view>

				<!-- 效率 -->
				<view class="discover-card">
					<view class="discover-row" hover-class="discover-row-hover" @click="goPage('/pages/todo/todo')">
						<view class="row-icon">
							<image class="row-icon-img" src="/static/icons/feature/todo.svg" mode="aspectFit"></image>
						</view>
						<text class="row-label">待办</text>
						<image class="row-arrow" src="/static/icons/arrow/right.svg" mode="aspectFit"></image>
					</view>
				</view>

				<!-- 帮助与信息 -->
				<view class="discover-card">
					<view class="discover-row" hover-class="discover-row-hover" @click="goPage('/pages/settings/settings?panel=help')">
						<view class="row-icon">
							<image class="row-icon-img" src="/static/icons/misc/question.svg" mode="aspectFit"></image>
						</view>
						<text class="row-label">帮助与反馈</text>
						<image class="row-arrow" src="/static/icons/arrow/right.svg" mode="aspectFit"></image>
					</view>
					<view class="row-divider"></view>
					<view class="discover-row" hover-class="discover-row-hover" @click="goPage('/pages/settings/settings?panel=about')">
						<view class="row-icon">
							<image class="row-icon-img" src="/static/icons/misc/info-circle.svg" mode="aspectFit"></image>
						</view>
						<text class="row-label">关于西瓜 IM</text>
						<image class="row-arrow" src="/static/icons/arrow/right.svg" mode="aspectFit"></image>
					</view>
				</view>

			</view>
		</scroll-view>

		<BottomNav :current="currentPath" @change="handleNavChange" />
	</view>
</template>

<script>
import BottomNav from '@/components/BottomNav.vue'
import { getReceiveFriendRequestList } from '@/api/contact.js'

export default {
	components: {
		BottomNav
	},
	data() {
		return {
			currentPath: '/pages/discover/discover',
			pendingCount: 0
		}
	},
	onShow() {
		this.loadPendingCount()
	},
	methods: {
		async loadPendingCount() {
			try {
				const res = await getReceiveFriendRequestList()
				if (res && res.code === 200 && res.data) {
					const list = Array.isArray(res.data) ? res.data : (res.data.list || [])
					this.pendingCount = list.filter(item => Number(item.flowStatus) === 0).length
				}
			} catch (error) {
				console.error('发现页获取好友申请数量失败:', error)
				this.pendingCount = 0
			}
		},
		goPage(url) {
			uni.navigateTo({
				url,
				fail: (err) => {
					console.error('发现页跳转失败:', err)
				}
			})
		},
		goAiChat() {
			let currentBot = uni.getStorageSync('currentBot')
			if (typeof currentBot === 'string') {
				try {
					currentBot = JSON.parse(currentBot)
				} catch (e) {
					currentBot = null
				}
			}
			if (currentBot && currentBot.id) {
				uni.navigateTo({
					url: '/pages/bots/bot-chat?botId=' + encodeURIComponent(currentBot.id)
						+ '&name=' + encodeURIComponent(currentBot.name || '机器人')
						+ '&avatar=' + encodeURIComponent(currentBot.avatar || '')
				})
			} else {
				uni.navigateTo({ url: '/pages/bots/bots' })
			}
		},
		handleNavChange(path) {
			this.currentPath = path
		}
	}
}
</script>

<style lang="scss" scoped>
.discover-page {
	width: 100%;
	height: 100vh;
	background-color: #f2f2f7;
	display: flex;
	flex-direction: column;
	overflow: hidden;
}

.top-bar {
	position: relative;
	flex-shrink: 0;
	background: rgba(255, 255, 255, 0.78);
	backdrop-filter: saturate(180%) blur(20px);
	-webkit-backdrop-filter: saturate(180%) blur(20px);
	border-bottom: 1rpx solid rgba(60, 60, 67, 0.12);
}

.top-bar-content {
	display: flex;
	align-items: center;
	justify-content: center;
	padding: 30rpx 0;
}

.page-title {
	font-family: 'Manrope', 'Plus Jakarta Sans', -apple-system, BlinkMacSystemFont, sans-serif;
	font-size: 36rpx;
	font-weight: 700;
	line-height: 1.2;
	letter-spacing: -0.02em;
	color: #1c1c1e;
}

.main-content {
	flex: 1;
	min-height: 0;
	padding-top: 0;
	padding-bottom: 128rpx;
	box-sizing: border-box;
	overflow: hidden;
}

.main-content ::v-deep .uni-scroll-view {
	height: 100% !important;
}

.content-wrapper {
	display: block;
	width: 100%;
	box-sizing: border-box;
	padding: 28rpx 24rpx 0;
	max-width: 928rpx;
	margin: 0 auto;
}

.discover-card {
	background: #ffffff;
	border: 1rpx solid rgba(60, 60, 67, 0.12);
	border-radius: 20rpx;
	overflow: hidden;
	margin-bottom: 24rpx;
}

.discover-row {
	display: flex;
	align-items: center;
	min-height: 104rpx;
	padding: 0 28rpx;
}

.discover-row-hover {
	background: rgba(60, 60, 67, 0.05);
}

.row-icon {
	width: 64rpx;
	height: 64rpx;
	border-radius: 14rpx;
	background: #f2f2f7;
	display: flex;
	align-items: center;
	justify-content: center;
	flex-shrink: 0;
	margin-right: 22rpx;
}

.row-icon-img {
	width: 38rpx;
	height: 38rpx;
	opacity: 0.82;
}

.row-label {
	flex: 1;
	font-size: 29rpx;
	font-weight: 500;
	color: #1c1c1e;
	line-height: 1.3;
}

.row-note {
	font-size: 23rpx;
	color: rgba(60, 60, 67, 0.55);
	margin-right: 10rpx;
}

.row-arrow {
	width: 28rpx;
	height: 28rpx;
	opacity: 0.35;
	flex-shrink: 0;
}

.row-divider {
	height: 1rpx;
	background: rgba(60, 60, 67, 0.1);
	margin-left: 114rpx;
}

.page-footnote {
	display: block;
	text-align: center;
	font-size: 22rpx;
	color: rgba(60, 60, 67, 0.32);
	padding: 32rpx 0 24rpx;
}
</style>
