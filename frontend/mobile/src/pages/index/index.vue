<template>
	<view class="launch-page">
		<view class="brand-mark">
			<text class="brand-word">西</text>
		</view>
		<text class="brand-name">西瓜 IM</text>
		<text class="brand-slogan">连接你关心的人</text>
	</view>
</template>

<script>
	import config from '@/config/index.js'

	export default {
		onLoad() {
			const token = uni.getStorageSync(config.tokenKey)
			setTimeout(() => {
				uni.reLaunch({
					url: token ? '/pages/chat/chat-list' : '/pages/login/login'
				})
			}, 500)
		}
	}
</script>

<style lang="scss" scoped>
	.launch-page {
		display: flex;
		flex-direction: column;
		align-items: center;
		justify-content: center;
		width: 100%;
		height: 100vh;
		background: #f2f2f7;
	}

	.brand-mark {
		width: 128rpx;
		height: 128rpx;
		display: flex;
		align-items: center;
		justify-content: center;
		background: #111111;
		border-radius: 28rpx;
	}

	.brand-word {
		font-size: 64rpx;
		font-weight: 600;
		line-height: 1;
		color: #ffffff;
	}

	.brand-name {
		margin-top: 28rpx;
		font-size: 40rpx;
		font-weight: 600;
		letter-spacing: -0.02em;
		color: #1c1c1e;
	}

	.brand-slogan {
		margin-top: 12rpx;
		font-size: 24rpx;
		color: rgba(60, 60, 67, 0.5);
	}
</style>
