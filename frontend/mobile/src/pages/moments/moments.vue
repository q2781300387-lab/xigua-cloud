<template>
	<view class="moments-page">
		<view class="top-bar safe-top-bar">
			<view class="top-bar-content">
				<view class="back-btn" @click="goBack">
					<image class="back-icon" src="/static/icons/arrow/left.svg" mode="aspectFit"></image>
				</view>
				<text class="page-title">朋友圈</text>
				<view class="top-action" @click="openComposer">
					<text class="top-action-text">发表</text>
				</view>
			</view>
		</view>

		<scroll-view class="main-content" scroll-y @scrolltolower="loadMore">
			<view class="cover-wrap">
				<view class="moments-cover">
					<image class="cover-photo" src="/static/images/moments-cover.jpg" mode="aspectFill"></image>
					<view class="cover-overlay"></view>
					<view class="cover-user">
						<text class="cover-name">{{ userInfo.username || userInfo.nickname || '我' }}</text>
						<image
							class="cover-avatar"
							:src="userInfo.avatar || defaultAvatar"
							mode="aspectFill"
							@error="handleAvatarError"
						></image>
					</view>
				</view>
			</view>
			<view class="content-wrapper">
				<view v-if="loading" class="state-card">
					<text class="state-title">正在加载朋友圈…</text>
				</view>
				<view v-else-if="loadFailed" class="state-card" @click="loadMoments(true)">
					<text class="state-title">加载失败</text>
					<text class="state-desc">点击重新加载</text>
				</view>
				<view v-else-if="moments.length === 0" class="state-card">
					<text class="state-title">还没有动态</text>
					<text class="state-desc">点右上角“发表”，记录第一条吧</text>
				</view>

				<view v-for="item in moments" :key="item.id" class="moment-card">
					<view class="moment-head" @click="openDetail(item)">
						<image class="moment-avatar" :src="item.avatar || defaultAvatar" mode="aspectFill" @error="handleAvatarError"></image>
						<view class="moment-meta">
							<text class="moment-name">{{ item.nickname || '用户' }}</text>
							<text class="moment-time">{{ item.timeText }}</text>
						</view>
						<view v-if="item.canDelete" class="moment-delete" @click.stop="removeMoment(item)">
							<text class="moment-delete-text">删除</text>
						</view>
					</view>

					<text v-if="item.text" class="moment-text" @click="openDetail(item)">{{ item.text }}</text>

					<view v-if="item.images.length" class="image-grid" :class="'grid-' + Math.min(item.images.length, 3)">
						<image
							v-for="(img, index) in item.images"
							:key="index"
							class="grid-image"
							:src="img"
							mode="aspectFill"
							@click="previewImage(item.images, img)"
						></image>
					</view>

					<text v-if="item.location" class="moment-location">{{ item.location }}</text>

					<view class="moment-actions">
						<view class="action-btn" :class="{ 'action-active': item.liked }" @click="toggleLike(item)">
							<text class="action-text">{{ item.liked ? '已赞' : '赞' }}</text>
							<text v-if="item.likeCount" class="action-count">{{ item.likeCount }}</text>
						</view>
						<view class="action-btn" @click="openComment(item)">
							<text class="action-text">评论</text>
							<text v-if="item.comments.length" class="action-count">{{ item.comments.length }}</text>
						</view>
					</view>

					<view v-if="item.likes.length" class="likes-row" @click="openLikes(item)">
						<text class="likes-icon">❤️</text>
						<text class="likes-names">{{ likeNames(item.likes) }}</text>
						<text class="likes-more">共 {{ item.likes.length }} 人 ›</text>
					</view>

					<view v-if="item.comments.length" class="comment-block">
						<view v-for="comment in item.comments" :key="comment.id" class="comment-row">
							<text class="comment-name">{{ comment.name || '用户' }}</text>
							<text class="comment-text">{{ comment.text }}</text>
							<text v-if="comment.mine" class="comment-delete" @click.stop="removeComment(item, comment)">删除</text>
						</view>
					</view>

					<view class="detail-entry" @click="openDetail(item)">
						<text class="detail-entry-text">查看详情 ›</text>
					</view>
				</view>

				<view v-if="moments.length > 0" class="feed-end">
					<text class="feed-end-text">{{ hasMore ? '上拉加载更多' : '已经到底了' }}</text>
				</view>
			</view>
		</scroll-view>

		<!-- 发表动态 -->
		<view v-if="composerOpen" class="sheet-mask" @click="closeComposer">
			<view class="sheet-panel" @click.stop>
				<view class="sheet-handle"></view>
				<view class="sheet-head">
					<text class="sheet-cancel" @click="closeComposer">取消</text>
					<text class="sheet-title">发表动态</text>
					<text class="sheet-submit" :class="{ 'sheet-submit-disabled': publishing }" @click="publishMoment">{{ publishing ? '发表中' : '发表' }}</text>
				</view>
				<textarea
					class="publish-textarea"
					v-model="draftText"
					placeholder="这一刻的想法…"
					maxlength="500"
					:adjust-position="true"
				></textarea>
				<view class="draft-images">
					<view v-for="(img, index) in draftImages" :key="index" class="draft-image-wrap">
						<image class="draft-image" :src="img.path" mode="aspectFill"></image>
						<text class="draft-image-remove" @click="removeDraftImage(index)">×</text>
					</view>
					<view v-if="draftImages.length < 9" class="draft-add" @click="chooseImages">
						<text class="draft-add-plus">+</text>
						<text class="draft-add-text">图片</text>
					</view>
				</view>
				<view class="publish-location">
					<text class="publish-location-label">位置</text>
					<input class="publish-location-input" v-model="draftLocation" placeholder="可不填" maxlength="40" />
				</view>
			</view>
		</view>

		<!-- 评论输入 -->
		<view v-if="commentTarget" class="sheet-mask" @click="closeComment">
			<view class="comment-sheet" @click.stop>
				<view class="comment-sheet-head">
					<text class="comment-sheet-title">评论</text>
					<text class="comment-sheet-close" @click="closeComment">取消</text>
				</view>
				<textarea
					class="comment-textarea"
					v-model="commentDraft"
					placeholder="说点什么…"
					maxlength="200"
					:focus="commentFocused"
					:adjust-position="true"
				></textarea>
				<view class="comment-send" :class="{ 'comment-send-disabled': !commentDraft.trim() }" @click="submitComment">发送</view>
			</view>
		</view>

		<!-- 点赞列表 -->
		<view v-if="likesTarget" class="sheet-mask" @click="closeLikes">
			<view class="likes-sheet" @click.stop>
				<view class="sheet-handle"></view>
				<text class="likes-title">点赞 · {{ likesTarget.likes.length }} 人</text>
				<scroll-view class="likes-list" scroll-y>
					<view v-for="(name, index) in likesTarget.likes" :key="index" class="likes-item">
						<text class="likes-heart">❤</text>
						<text class="likes-item-name">{{ name }}</text>
					</view>
				</scroll-view>
				<view class="likes-close" @click="closeLikes">关闭</view>
			</view>
		</view>
	</view>
</template>

<script>
import config from '@/config/index.js'
import { uploadFile } from '@/api/file.js'
import { momentList, momentPublish, momentLike, momentComment, momentDelete, momentCommentDelete } from '@/api/moment.js'

export default {
	data() {
		return {
			userInfo: {},
			moments: [],
			loading: true,
			loadFailed: false,
			pageNum: 1,
			pageSize: 10,
			hasMore: true,
			composerOpen: false,
			publishing: false,
			draftText: '',
			draftLocation: '',
			draftImages: [],
			commentTarget: null,
			commentDraft: '',
			commentFocused: false,
			likesTarget: null,
			defaultAvatar: '/static/icons/user/default-avatar.svg'
		}
	},
	onLoad() {
		try {
			this.userInfo = JSON.parse(uni.getStorageSync(config.userInfoKey) || '{}')
		} catch (e) {
			this.userInfo = {}
		}
		this.loadMoments(true)
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
		handleAvatarError(event) {
			if (event && event.target) event.target.src = this.defaultAvatar
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
			if (diff < 86400 * 7) return Math.floor(diff / 86400) + ' 天前'
			const date = new Date(time)
			const pad = n => String(n).padStart(2, '0')
			return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
		},
		mapComment(comment) {
			return {
				id: comment.id,
				name: comment.nickname || '用户',
				text: comment.text || '',
				mine: String(comment.userId) === String(this.userInfo.id || '')
			}
		},
		mapMoment(raw) {
			return {
				id: raw.id,
				userId: raw.userId,
				nickname: raw.nickname || '用户',
				avatar: raw.avatar || this.defaultAvatar,
				text: raw.content || '',
				images: raw.images || [],
				location: raw.location || '',
				likes: raw.likes || [],
				likeCount: raw.likeCount || (raw.likes || []).length,
				liked: !!raw.liked,
				comments: (raw.comments || []).map(item => this.mapComment(item)),
				canDelete: String(raw.userId) === String(this.userInfo.id || ''),
				timeText: this.formatTime(raw.createTime)
			}
		},
		async loadMoments(reset) {
			if (reset) {
				this.loading = true
				this.loadFailed = false
				this.pageNum = 1
				this.hasMore = true
			}
			try {
				const res = await momentList({
					userId: this.userInfo.id,
					pageNum: this.pageNum,
					pageSize: this.pageSize
				})
				const rows = (res && res.data) || []
				const mapped = rows.map(item => this.mapMoment(item))
				this.moments = reset ? mapped : this.moments.concat(mapped)
				this.hasMore = rows.length === this.pageSize
			} catch (e) {
				console.error('加载朋友圈失败:', e)
				if (reset) this.loadFailed = true
			} finally {
				this.loading = false
			}
		},
		loadMore() {
			if (this.loading || !this.hasMore) return
			this.pageNum += 1
			this.loadMoments(false)
		},
		openComposer() {
			this.draftText = ''
			this.draftLocation = ''
			this.draftImages = []
			this.composerOpen = true
		},
		closeComposer() {
			if (this.publishing) return
			this.composerOpen = false
		},
		chooseImages() {
			const remain = 9 - this.draftImages.length
			if (remain <= 0) return
			uni.chooseImage({
				count: remain,
				sizeType: ['compressed'],
				sourceType: ['album', 'camera'],
				success: (res) => {
					const paths = res.tempFilePaths || []
					const files = res.tempFiles || []
					paths.forEach((path, index) => {
						const file = files[index] && files[index].file ? files[index].file : path
						this.draftImages.push({ path, file })
					})
				}
			})
		},
		removeDraftImage(index) {
			this.draftImages.splice(index, 1)
		},
		async publishMoment() {
			if (this.publishing) return
			const text = this.draftText.trim()
			if (!text && this.draftImages.length === 0) {
				uni.showToast({ title: '写点什么或选张图片吧', icon: 'none' })
				return
			}
			this.publishing = true
			uni.showLoading({ title: '发表中...', mask: true })
			try {
				const images = []
				for (const item of this.draftImages) {
					if (String(item.path).startsWith('http')) {
						images.push(item.path)
					} else {
						const uploadRes = await uploadFile(item.file)
						if (!uploadRes || !uploadRes.data) throw new Error('图片上传失败')
						images.push(uploadRes.data)
					}
				}
				const res = await momentPublish({
					userId: this.userInfo.id,
					content: text,
					images,
					location: this.draftLocation.trim()
				})
				if (!res || !res.data) throw new Error('发表失败')
				this.composerOpen = false
				this.moments.unshift(this.mapMoment(res.data))
				uni.showToast({ title: '已发表', icon: 'success' })
			} catch (e) {
				console.error('发表朋友圈失败:', e)
				uni.showToast({ title: e.message || '发表失败，请重试', icon: 'none' })
			} finally {
				uni.hideLoading()
				this.publishing = false
			}
		},
		previewImage(list, current) {
			uni.previewImage({ urls: list, current })
		},
		async toggleLike(item) {
			try {
				const res = await momentLike({ userId: this.userInfo.id, momentId: item.id })
				const data = res && res.data
				if (data) {
					item.liked = !!data.liked
					item.likes = data.likes || []
					item.likeCount = data.likeCount || (data.likes || []).length
				}
			} catch (e) {
				console.error('点赞失败:', e)
			}
		},
		likeNames(likes) {
			const names = (likes || []).slice(0, 4)
			return names.join('，') + ((likes || []).length > 4 ? '…' : '')
		},
		openLikes(item) {
			this.likesTarget = item
		},
		closeLikes() {
			this.likesTarget = null
		},
		removeComment(item, comment) {
			uni.showModal({
				title: '删除评论',
				content: '删除后不可恢复，确定删除吗？',
				confirmText: '删除',
				confirmColor: '#d92d20',
				success: async (result) => {
					if (!result.confirm) return
					try {
						const res = await momentCommentDelete({
							userId: this.userInfo.id,
							commentId: comment.id
						})
						const data = res && res.data
						if (data) {
							item.comments = (data.comments || []).map(row => this.mapComment(row))
						}
						uni.showToast({ title: '已删除', icon: 'success' })
					} catch (e) {
						console.error('删除评论失败:', e)
					}
				}
			})
		},
		openDetail(item) {
			uni.navigateTo({ url: '/pages/moments/moment-detail?id=' + encodeURIComponent(item.id) })
		},
		openComment(item) {
			this.commentTarget = item
			this.commentDraft = ''
			this.commentFocused = false
			this.$nextTick(() => {
				this.commentFocused = true
			})
		},
		closeComment() {
			this.commentTarget = null
			this.commentDraft = ''
			this.commentFocused = false
		},
		async submitComment() {
			const text = this.commentDraft.trim()
			if (!text || !this.commentTarget) return
			try {
				const res = await momentComment({
					userId: this.userInfo.id,
					momentId: this.commentTarget.id,
					content: text
				})
				const data = res && res.data
				if (data) {
					this.commentTarget.comments = (data.comments || []).map(item => this.mapComment(item))
				}
				this.closeComment()
				uni.showToast({ title: '评论成功', icon: 'success' })
			} catch (e) {
				console.error('评论失败:', e)
			}
		},
		removeMoment(item) {
			uni.showModal({
				title: '删除动态',
				content: '删除后不可恢复，确定删除吗？',
				confirmText: '删除',
				confirmColor: '#d92d20',
				success: async (result) => {
					if (!result.confirm) return
					try {
						await momentDelete({ userId: this.userInfo.id, momentId: item.id })
						this.moments = this.moments.filter(moment => moment.id !== item.id)
						uni.showToast({ title: '已删除', icon: 'success' })
					} catch (e) {
						console.error('删除动态失败:', e)
					}
				}
			})
		}
	}
}
</script>

<style lang="scss" scoped>
.moments-page {
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
	letter-spacing: -0.02em;
}

.top-action {
	width: 96rpx;
	height: 68rpx;
	display: flex;
	align-items: center;
	justify-content: flex-end;
}

.top-action-text {
	font-size: 28rpx;
	font-weight: 600;
	color: #111111;
}

.main-content {
	flex: 1;
	min-height: 0;
	padding-top: 0;
	overflow: hidden;
}

.main-content ::v-deep .uni-scroll-view {
	height: 100% !important;
}

.cover-wrap {
	padding: 24rpx 24rpx 0;
}

.moments-cover {
	position: relative;
	height: 300rpx;
	border-radius: 26rpx;
	overflow: hidden;
	background: #d9d9de;
}

.cover-photo {
	display: block;
	width: 100%;
	height: 100%;
}

.cover-overlay {
	position: absolute;
	left: 0;
	right: 0;
	bottom: 0;
	height: 58%;
	background: linear-gradient(180deg, rgba(0, 0, 0, 0) 0%, rgba(0, 0, 0, 0.42) 100%);
}

.cover-user {
	position: absolute;
	right: 26rpx;
	bottom: 24rpx;
	display: flex;
	align-items: center;
	gap: 16rpx;
}

.cover-name {
	font-size: 27rpx;
	font-weight: 600;
	color: #ffffff;
	text-shadow: 0 2rpx 6rpx rgba(0, 0, 0, 0.36);
}

.cover-avatar {
	width: 78rpx;
	height: 78rpx;
	border-radius: 14rpx;
	border: 2rpx solid rgba(255, 255, 255, 0.86);
	background: #ffffff;
	box-shadow: 0 6rpx 16rpx rgba(0, 0, 0, 0.2);
}

.content-wrapper {
	padding: 24rpx 24rpx 60rpx;
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

.moment-card {
	background: #ffffff;
	border: 1rpx solid rgba(60, 60, 67, 0.12);
	border-radius: 20rpx;
	padding: 28rpx;
	margin-bottom: 24rpx;
}

.moment-head {
	display: flex;
	align-items: center;
}

.moment-avatar {
	width: 84rpx;
	height: 84rpx;
	border-radius: 14rpx;
	background: #f2f2f7;
	flex-shrink: 0;
}

.moment-meta {
	flex: 1;
	display: flex;
	flex-direction: column;
	margin-left: 20rpx;
	min-width: 0;
}

.moment-name {
	font-size: 29rpx;
	font-weight: 600;
	color: #1c1c1e;
}

.moment-time {
	font-size: 22rpx;
	color: rgba(60, 60, 67, 0.5);
	margin-top: 4rpx;
}

.moment-delete {
	font-size: 23rpx;
	color: #d92d20;
	padding: 8rpx 0 8rpx 16rpx;
}

.moment-text {
	display: block;
	font-size: 29rpx;
	color: #1c1c1e;
	line-height: 1.62;
	margin-top: 22rpx;
	white-space: pre-wrap;
	word-break: break-word;
}

.image-grid {
	display: grid;
	grid-gap: 8rpx;
	margin-top: 20rpx;
}

.grid-1 {
	grid-template-columns: 1fr;
	max-width: 460rpx;
}

.grid-2 {
	grid-template-columns: repeat(2, 1fr);
}

.grid-3 {
	grid-template-columns: repeat(3, 1fr);
}

.grid-image {
	width: 100%;
	height: 210rpx;
	border-radius: 10rpx;
	background: #f2f2f7;
}

.grid-1 .grid-image {
	height: 340rpx;
}

.moment-location {
	display: block;
	font-size: 23rpx;
	color: rgba(60, 60, 67, 0.6);
	margin-top: 16rpx;
}

.moment-actions {
	display: flex;
	align-items: center;
	margin-top: 22rpx;
	padding-top: 20rpx;
	border-top: 1rpx solid rgba(60, 60, 67, 0.1);
}

.action-btn {
	display: flex;
	align-items: center;
	margin-right: 48rpx;
	padding: 8rpx 0;
}

.action-text {
	font-size: 25rpx;
	color: rgba(60, 60, 67, 0.7);
}

.action-active .action-text {
	color: #111111;
	font-weight: 600;
}

.action-count {
	font-size: 23rpx;
	color: rgba(60, 60, 67, 0.5);
	margin-left: 8rpx;
}

.likes-row {
	display: flex;
	align-items: center;
	margin-top: 16rpx;
	padding: 14rpx 18rpx;
	background: #f7f7fa;
	border-radius: 12rpx;
}

.likes-icon {
	font-size: 24rpx;
	margin-right: 10rpx;
}

.likes-names {
	flex: 1;
	font-size: 24rpx;
	color: #3a3a3c;
	line-height: 1.4;
}

.likes-more {
	flex-shrink: 0;
	font-size: 22rpx;
	color: rgba(60, 60, 67, 0.5);
	margin-left: 12rpx;
}

.comment-block {
	margin-top: 18rpx;
	background: #f7f7fa;
	border-radius: 12rpx;
	padding: 16rpx 20rpx;
}

.comment-row {
	display: flex;
	align-items: flex-start;
	line-height: 1.55;
	margin-bottom: 8rpx;
}

.comment-row:last-child {
	margin-bottom: 0;
}

.comment-name {
	font-size: 24rpx;
	font-weight: 600;
	color: #1c1c1e;
	flex-shrink: 0;
	margin-right: 10rpx;
}

.comment-text {
	font-size: 24rpx;
	color: rgba(60, 60, 67, 0.8);
	word-break: break-word;
}

.comment-delete {
	flex-shrink: 0;
	font-size: 21rpx;
	color: rgba(60, 60, 67, 0.42);
	margin-left: 12rpx;
	padding: 4rpx 0;
}

.detail-entry {
	padding-top: 12rpx;
	display: flex;
	justify-content: flex-end;
}

.detail-entry-text {
	font-size: 22rpx;
	color: rgba(60, 60, 67, 0.5);
}

.feed-end {
	padding: 24rpx 0 10rpx;
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

.likes-sheet {
	width: 100%;
	max-height: 68vh;
	display: flex;
	flex-direction: column;
	background: #ffffff;
	border-radius: 26rpx 26rpx 0 0;
	padding: 16rpx 28rpx calc(22rpx + env(safe-area-inset-bottom));
	box-sizing: border-box;
}

.likes-title {
	display: block;
	text-align: center;
	font-size: 27rpx;
	font-weight: 600;
	color: #1c1c1e;
	padding: 10rpx 0 18rpx;
}

.likes-list {
	max-height: 48vh;
}

.likes-item {
	display: flex;
	align-items: center;
	height: 84rpx;
	border-top: 1rpx solid rgba(60, 60, 67, 0.08);
}

.likes-heart {
	font-size: 24rpx;
	color: #ff453a;
	margin-right: 16rpx;
}

.likes-item-name {
	font-size: 26rpx;
	color: #1c1c1e;
}

.likes-close {
	margin-top: 18rpx;
	height: 84rpx;
	line-height: 84rpx;
	text-align: center;
	font-size: 27rpx;
	color: #1c1c1e;
	background: #f2f2f7;
	border-radius: 18rpx;
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
	width: 110rpx;
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
	min-height: 200rpx;
	font-size: 28rpx;
	color: #1c1c1e;
	line-height: 1.6;
	padding: 18rpx 0;
	box-sizing: border-box;
}

.draft-images {
	display: flex;
	flex-wrap: wrap;
	margin-top: 8rpx;
}

.draft-image-wrap {
	position: relative;
	width: 150rpx;
	height: 150rpx;
	margin: 0 12rpx 12rpx 0;
}

.draft-image {
	width: 150rpx;
	height: 150rpx;
	border-radius: 10rpx;
	background: #f2f2f7;
}

.draft-image-remove {
	position: absolute;
	top: -10rpx;
	right: -10rpx;
	width: 36rpx;
	height: 36rpx;
	line-height: 34rpx;
	text-align: center;
	border-radius: 50%;
	background: rgba(0, 0, 0, 0.66);
	color: #ffffff;
	font-size: 24rpx;
}

.draft-add {
	width: 150rpx;
	height: 150rpx;
	border-radius: 10rpx;
	background: #f7f7fa;
	border: 1rpx dashed rgba(60, 60, 67, 0.25);
	display: flex;
	flex-direction: column;
	align-items: center;
	justify-content: center;
}

.draft-add-plus {
	font-size: 42rpx;
	line-height: 1;
	color: rgba(60, 60, 67, 0.45);
}

.draft-add-text {
	font-size: 22rpx;
	color: rgba(60, 60, 67, 0.5);
	margin-top: 8rpx;
}

.publish-location {
	display: flex;
	align-items: center;
	margin-top: 18rpx;
	padding-top: 18rpx;
	border-top: 1rpx solid rgba(60, 60, 67, 0.1);
}

.publish-location-label {
	font-size: 25rpx;
	color: rgba(60, 60, 67, 0.6);
	width: 90rpx;
}

.publish-location-input {
	flex: 1;
	font-size: 26rpx;
	color: #1c1c1e;
}

.comment-sheet {
	width: 100%;
	background: #ffffff;
	border-radius: 26rpx 26rpx 0 0;
	padding: 24rpx 28rpx calc(24rpx + env(safe-area-inset-bottom));
	box-sizing: border-box;
}

.comment-sheet-head {
	display: flex;
	align-items: center;
	justify-content: space-between;
}

.comment-sheet-title {
	font-size: 28rpx;
	font-weight: 600;
	color: #1c1c1e;
}

.comment-sheet-close {
	font-size: 25rpx;
	color: rgba(60, 60, 67, 0.6);
}

.comment-textarea {
	width: 100%;
	min-height: 150rpx;
	font-size: 27rpx;
	color: #1c1c1e;
	line-height: 1.6;
	padding: 18rpx 0;
	box-sizing: border-box;
}

.comment-send {
	height: 80rpx;
	border-radius: 16rpx;
	background: #111111;
	color: #ffffff;
	font-size: 27rpx;
	font-weight: 600;
	display: flex;
	align-items: center;
	justify-content: center;
}

.comment-send-disabled {
	opacity: 0.35;
}
</style>
