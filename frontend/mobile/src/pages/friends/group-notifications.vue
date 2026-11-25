<template>
	<view class="page">
		<WebSocketKeeper />

		<view class="top-bar safe-top-bar">
			<view class="top-bar-inner">
				<view class="back-btn" hover-class="back-btn-hover" @click="goBack">
					<image class="back-icon" src="/static/icons/arrow/left.svg" mode="aspectFit"></image>
				</view>
				<text class="page-title">群通知</text>
				<view class="top-bar-placeholder"></view>
			</view>
		</view>

		<view class="main-content">
			<view class="empty-card">
				<view class="empty-icon-wrap">
					<image class="empty-icon" src="/static/icons/user/group-outline.svg" mode="aspectFit"></image>
				</view>
				<text class="empty-title">暂无群通知</text>
				<text class="empty-desc">群聊邀请与成员变更会显示在这里</text>
			</view>
		</view>
	</view>
</template>

<script>
import WebSocketKeeper from '@/components/WebSocketKeeper.vue'

export default {
	components: {
		WebSocketKeeper
	},
	methods: {
		goBack() {
			uni.navigateBack({
				fail: () => {
					uni.reLaunch({ url: '/pages/notice/notice-list' })
				}
			})
		}
	}
}
</script>

<style scoped>
.page {
	min-height: 100vh;
	background-color: #f2f2f7;
}

.top-bar {
	position: fixed;
	top: 0;
	left: 0;
	right: 0;
	background: rgba(255, 255, 255, 0.82);
	backdrop-filter: saturate(180%) blur(20px);
	-webkit-backdrop-filter: saturate(180%) blur(20px);
	border-bottom: 1rpx solid rgba(60, 60, 67, 0.12);
	z-index: 999;
}

.top-bar-inner {
	position: relative;
	height: 88rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	padding: 0 24rpx;
}

.back-btn {
	position: absolute;
	left: 14rpx;
	top: 0;
	width: 72rpx;
	height: 88rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	border-radius: 12rpx;
}

.back-btn-hover {
	background: rgba(120, 120, 128, 0.12);
}

.back-icon {
	width: 36rpx;
	height: 36rpx;
	opacity: 0.6;
}

.top-bar-placeholder {
	width: 72rpx;
	height: 88rpx;
}

.page-title {
	font-size: 32rpx;
	font-weight: 600;
	color: #1c1c1e;
	letter-spacing: -0.01em;
}

.main-content {
	padding-top: calc(var(--status-bar-height) + 136rpx);
	padding-left: 24rpx;
	padding-right: 24rpx;
}

.empty-card {
	background: #ffffff;
	border-radius: 20rpx;
	border: 1rpx solid rgba(60, 60, 67, 0.12);
	padding: 80rpx 32rpx;
	display: flex;
	flex-direction: column;
	align-items: center;
}

.empty-icon-wrap {
	width: 112rpx;
	height: 112rpx;
	border-radius: 50%;
	background: #f2f2f7;
	display: flex;
	align-items: center;
	justify-content: center;
	margin-bottom: 24rpx;
}

.empty-icon {
	width: 52rpx;
	height: 52rpx;
	opacity: 0.4;
}

.empty-title {
	font-size: 28rpx;
	font-weight: 600;
	color: #1c1c1e;
}

.empty-desc {
	margin-top: 10rpx;
	font-size: 23rpx;
	color: rgba(60, 60, 67, 0.45);
	text-align: center;
}
</style>
