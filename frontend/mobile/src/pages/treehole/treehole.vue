<template>
	<view class="treehole-page">
		<view class="top-bar safe-top-bar">
			<view class="top-bar-content">
				<view class="back-btn" @click="goBack">
					<image class="back-icon" src="/static/icons/arrow/left.svg" mode="aspectFit"></image>
				</view>
				<text class="page-title">匿名树洞</text>
				<view class="top-action" @click="openComposer">
					<text class="top-action-text">写一条</text>
				</view>
			</view>
		</view>

		<scroll-view class="main-content" scroll-y @scrolltolower="loadMore">
			<view class="content-wrapper">
				<view class="hero-card">
					<text class="hero-title">这里没有名字，只有心事</text>
					<text class="hero-desc">把你的烦恼悄悄扔进树洞，会收到陌生人的温暖回应</text>
				</view>

				<view v-if="loading" class="state-card">
					<text class="state-title">正在听树洞的回声…</text>
				</view>
				<view v-else-if="loadFailed" class="state-card" @click="loadHoles(true)">
					<text class="state-title">加载失败</text>
					<text class="state-desc">点击重试</text>
				</view>
				<view v-else-if="holes.length === 0" class="state-card">
					<text class="state-title">树洞还是空的</text>
					<text class="state-desc">第一个把心事放进来吧</text>
				</view>

				<view v-for="item in holes" :key="item.id" class="hole-card">
					<view class="hole-head">
						<view class="hole-avatar">
							<text class="hole-avatar-text">匿</text>
						</view>
						<view class="hole-meta">
							<text class="hole-name">匿名同学</text>
							<text class="hole-time">{{ item.timeText }}</text>
						</view>
					</view>
					<text class="hole-content">{{ item.content }}</text>
					<view class="hole-foot">
						<view class="like-btn" :class="{ 'like-active': item.liked }" @click="toggleLike(item)">
							<text class="like-text">{{ item.liked ? '已暖心' : '暖心' }}</text>
							<text class="like-count">{{ item.likeCount || 0 }}</text>
						</view>
						<text class="hole-safe">发布者信息已隐藏</text>
					</view>
				</view>

				<view v-if="holes.length > 0" class="feed-end">
					<text class="feed-end-text">{{ hasMore ? '上拉加载更多' : '— 树洞到此，愿心事都被听见 —' }}</text>
				</view>
			</view>
		</scroll-view>

		<view v-if="composerOpen" class="sheet-mask" @click="closeComposer">
			<view class="sheet-panel" @click.stop>
				<view class="sheet-handle"></view>
				<view class="sheet-head">
					<text class="sheet-cancel" @click="closeComposer">取消</text>
					<text class="sheet-title">扔进树洞</text>
					<text class="sheet-submit" :class="{ 'sheet-submit-disabled': publishing }" @click="publishHole">{{ publishing ? '发布中' : '匿名发布' }}</text>
				</view>
				<textarea
					class="publish-textarea"
					v-model="draft"
					placeholder="把不敢说出口的话，悄悄写在这里…"
					maxlength="500"
					:adjust-position="true"
				></textarea>
				<view class="sheet-tip">
					<text class="sheet-tip-text">发布后不展示任何账号信息</text>
				</view>
			</view>
		</view>
	</view>
</template>

<script>
import { treeholeList, treeholeAdd, treeholeLike } from '@/api/treehole.js'

export default {
	data() {
		return {
			holes: [],
			loading: true,
			loadFailed: false,
			page: 1,
			size: 20,
			hasMore: true,
			composerOpen: false,
			publishing: false,
			draft: '',
			likedIds: []
		}
	},
	onLoad() {
		this.loadHoles(true)
	},
	methods: {
		goBack() {
			const pages = getCurrentPages()
			if (pages.length > 1) {
				uni.navigateBack()
			} else {
				uni.redirectTo({ url: '/pages/discover/discover' })
			}
		},
		toTimestamp(text) {
			if (!text) return 0
			const time = new Date(String(text).replace(/-/g, '/')).getTime()
			return isNaN(time) ? 0 : time
		},
		formatTime(text) {
			const time = this.toTimestamp(text)
			if (!time) return ''
			const diff = Math.floor((Date.now() - time) / 1000)
			if (diff < 60) return '刚刚'
			if (diff < 3600) return Math.floor(diff / 60) + ' 分钟前'
			if (diff < 86400) return Math.floor(diff / 3600) + ' 小时前'
			const date = new Date(time)
			const pad = n => String(n).padStart(2, '0')
			return `${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
		},
		normalize(res) {
			const data = (res && res.data) || {}
			const records = data.records || []
			return { records, total: data.total || 0 }
		},
		async loadHoles(reset) {
			if (reset) {
				this.page = 1
				this.loading = true
				this.loadFailed = false
				this.hasMore = true
			}
			try {
				const res = await treeholeList({ page: this.page, size: this.size })
				const records = this.normalize(res).records
				const list = records.map(item => ({
					id: item.id,
					content: item.content || '',
					likeCount: item.likeCount || 0,
					timeText: this.formatTime(item.createTime),
					liked: this.likedIds.indexOf(String(item.id)) !== -1
				}))
				this.holes = reset ? list : this.holes.concat(list)
				this.hasMore = records.length === this.size
			} catch (e) {
				console.error('加载树洞失败:', e)
				if (reset) this.loadFailed = true
			} finally {
				this.loading = false
			}
		},
		loadMore() {
			if (this.loading || !this.hasMore) return
			this.page += 1
			this.loadHoles(false)
		},
		openComposer() {
			this.draft = ''
			this.composerOpen = true
		},
		closeComposer() {
			if (this.publishing) return
			this.composerOpen = false
		},
		async publishHole() {
			const content = this.draft.trim()
			if (!content || this.publishing) {
				if (!content) uni.showToast({ title: '写点什么再发布吧', icon: 'none' })
				return
			}
			this.publishing = true
			try {
				const res = await treeholeAdd({ content })
				if (res && res.code === 200) {
					this.composerOpen = false
					this.draft = ''
					await this.loadHoles(true)
					uni.showToast({ title: '已匿名发布', icon: 'success' })
				}
			} catch (e) {
				console.error('发布树洞失败:', e)
				uni.showToast({ title: '发布失败，请重试', icon: 'none' })
			} finally {
				this.publishing = false
			}
		},
		async toggleLike(item) {
			if (item.liked) return
			try {
				const res = await treeholeLike(item.id)
				if (res && res.code === 200) {
					item.likeCount = (item.likeCount || 0) + 1
					item.liked = true
					this.likedIds.push(String(item.id))
				}
			} catch (e) {
				console.error('树洞点赞失败:', e)
			}
		}
	}
}
</script>

<style lang="scss" scoped>
.treehole-page {
	width: 100%;
	height: 100vh;
	background: #f2f2f7;
	display: flex;
	flex-direction: column;
	overflow: hidden;
}

.top-bar {
	position: relative;
	background: rgba(255, 255, 255, 0.9);
	backdrop-filter: saturate(180%) blur(20px);
	-webkit-backdrop-filter: saturate(180%) blur(20px);
	border-bottom: 1rpx solid rgba(60, 60, 67, 0.12);
	flex-shrink: 0;
}

.top-bar-content {
	height: 96rpx;
	display: flex;
	align-items: center;
	padding: 0 20rpx;
}

.back-btn {
	width: 68rpx;
	height: 68rpx;
	display: flex;
	align-items: center;
	justify-content: center;
}

.back-icon {
	width: 34rpx;
	height: 34rpx;
	opacity: 0.6;
}

.page-title {
	flex: 1;
	text-align: center;
	font-size: 34rpx;
	font-weight: 700;
	color: #1c1c1e;
}

.top-action {
	width: 110rpx;
	height: 68rpx;
	display: flex;
	align-items: center;
	justify-content: flex-end;
}

.top-action-text {
	font-size: 27rpx;
	font-weight: 600;
	color: #111111;
}

.main-content {
	flex: 1;
	min-height: 0;
	overflow: hidden;
}

.main-content ::v-deep .uni-scroll-view {
	height: 100% !important;
}

.content-wrapper {
	padding: 24rpx 24rpx 60rpx;
}

.hero-card {
	position: relative;
	background: radial-gradient(circle at 50% 0%, rgba(255, 159, 10, 0.16), rgba(255, 159, 10, 0) 72%), transparent;
	border-radius: 28rpx;
	padding: 30rpx 24rpx 40rpx;
	margin-bottom: 8rpx;
	text-align: center;
	overflow: hidden;
}

.hero-title {
	display: block;
	font-size: 32rpx;
	font-weight: 700;
	color: #1c1c1e;
	letter-spacing: 0.04em;
}

.hero-desc {
	display: block;
	font-size: 23rpx;
	line-height: 1.7;
	color: rgba(60, 60, 67, 0.6);
	margin-top: 14rpx;
	letter-spacing: 0.02em;
}

.state-card {
	background: #ffffff;
	border: 1rpx solid rgba(60, 60, 67, 0.12);
	border-radius: 20rpx;
	padding: 72rpx 32rpx;
	display: flex;
	flex-direction: column;
	align-items: center;
}

.state-title {
	font-size: 28rpx;
	font-weight: 600;
	color: #1c1c1e;
}

.state-desc {
	font-size: 24rpx;
	color: rgba(60, 60, 67, 0.6);
	margin-top: 14rpx;
}

.hole-card {
	background: #ffffff;
	border: 1rpx solid rgba(60, 60, 67, 0.12);
	border-radius: 20rpx;
	padding: 28rpx;
	margin-bottom: 22rpx;
}

.hole-head {
	display: flex;
	align-items: center;
}

.hole-avatar {
	width: 76rpx;
	height: 76rpx;
	border-radius: 50%;
	background: #f2f2f7;
	display: flex;
	align-items: center;
	justify-content: center;
	flex-shrink: 0;
}

.hole-avatar-text {
	font-size: 26rpx;
	font-weight: 600;
	color: rgba(60, 60, 67, 0.65);
}

.hole-meta {
	margin-left: 18rpx;
	display: flex;
	flex-direction: column;
}

.hole-name {
	font-size: 27rpx;
	font-weight: 600;
	color: #1c1c1e;
}

.hole-time {
	font-size: 22rpx;
	color: rgba(60, 60, 67, 0.5);
	margin-top: 4rpx;
}

.hole-content {
	display: block;
	font-size: 29rpx;
	color: #1c1c1e;
	line-height: 1.65;
	margin-top: 20rpx;
	white-space: pre-wrap;
	word-break: break-word;
}

.hole-foot {
	display: flex;
	align-items: center;
	justify-content: space-between;
	margin-top: 22rpx;
	padding-top: 18rpx;
	border-top: 1rpx solid rgba(60, 60, 67, 0.1);
}

.like-btn {
	display: flex;
	align-items: center;
	padding: 6rpx 0;
}

.like-text {
	font-size: 24rpx;
	color: rgba(60, 60, 67, 0.65);
}

.like-active .like-text {
	color: #111111;
	font-weight: 600;
}

.like-count {
	font-size: 23rpx;
	color: rgba(60, 60, 67, 0.45);
	margin-left: 8rpx;
}

.hole-safe {
	font-size: 21rpx;
	color: rgba(60, 60, 67, 0.35);
}

.feed-end {
	padding: 20rpx 0;
	display: flex;
	justify-content: center;
}

.feed-end-text {
	font-size: 22rpx;
	color: rgba(60, 60, 67, 0.35);
}

.sheet-mask {
	position: fixed;
	left: 0;
	right: 0;
	top: 0;
	bottom: 0;
	background: rgba(0, 0, 0, 0.35);
	z-index: 800;
	display: flex;
	align-items: flex-end;
}

.sheet-panel {
	width: 100%;
	max-height: 84vh;
	overflow-y: auto;
	-webkit-overflow-scrolling: touch;
	background: #ffffff;
	border-radius: 26rpx 26rpx 0 0;
	padding: 16rpx 28rpx calc(28rpx + env(safe-area-inset-bottom));
	box-sizing: border-box;
}

.sheet-handle {
	width: 72rpx;
	height: 8rpx;
	border-radius: 4rpx;
	background: rgba(60, 60, 67, 0.2);
	margin: 0 auto 16rpx;
}

.sheet-head {
	display: flex;
	align-items: center;
	height: 76rpx;
}

.sheet-cancel {
	width: 110rpx;
	font-size: 26rpx;
	color: rgba(60, 60, 67, 0.7);
}

.sheet-title {
	flex: 1;
	text-align: center;
	font-size: 30rpx;
	font-weight: 600;
	color: #1c1c1e;
}

.sheet-submit {
	width: 130rpx;
	text-align: right;
	font-size: 26rpx;
	font-weight: 600;
	color: #111111;
}

.sheet-submit-disabled {
	opacity: 0.4;
}

.publish-textarea {
	width: 100%;
	min-height: 240rpx;
	font-size: 28rpx;
	color: #1c1c1e;
	line-height: 1.65;
	padding: 18rpx 0;
	box-sizing: border-box;
}

.sheet-tip {
	padding-top: 16rpx;
	border-top: 1rpx solid rgba(60, 60, 67, 0.1);
}

.sheet-tip-text {
	font-size: 22rpx;
	color: rgba(60, 60, 67, 0.45);
}
</style>
