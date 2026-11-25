<template>
	<view class="detail-page">
		<view class="top-bar safe-top-bar">
			<view class="top-bar-content">
				<view class="back-btn" @click="goBack">
					<image class="back-icon" src="/static/icons/arrow/left.svg" mode="aspectFit"></image>
				</view>
				<text class="page-title">动态详情</text>
				<view class="top-action">
					<text v-if="moment && moment.canDelete" class="top-action-text danger" @click="removeMoment">删除</text>
				</view>
			</view>
		</view>

		<scroll-view class="main-content" scroll-y>
			<view v-if="loading" class="detail-state">加载中…</view>
			<view v-else-if="!moment" class="detail-state">动态不存在或已删除</view>

			<view v-else class="detail-body">
				<view class="detail-card">
					<view class="author">
						<image class="avatar" :src="moment.avatar" mode="aspectFill" @error="handleAvatarError"></image>
						<view class="author-meta">
							<text class="name">{{ moment.nickname }}</text>
							<text class="time">{{ moment.time }}</text>
						</view>
					</view>

					<text v-if="moment.location" class="loc">📍 {{ moment.location }}</text>
					<text class="text">{{ moment.text }}</text>

					<view v-if="moment.images.length" class="grid" :class="'grid-' + Math.min(moment.images.length, 3)">
						<image
							v-for="(img, i) in moment.images.slice(0, 9)"
							:key="i"
							class="cell"
							:src="img"
							mode="aspectFill"
							@click="openPreview(i)"
						></image>
					</view>

					<view class="meta-line">
						<text v-if="moment.likes.length" class="meta-like" @click="likesOpen = true">❤ {{ moment.likes.length }} 人赞过</text>
						<text class="meta-comment">评论 {{ moment.comments.length }}</text>
					</view>

					<view v-if="moment.comments.length" class="comment-list">
						<view v-for="(c, ci) in moment.comments" :key="c.id || ci" class="comment-row">
							<text class="comment-name">{{ c.name }}</text>
							<text class="comment-text">{{ c.text }}</text>
							<text v-if="c.mine" class="comment-del" @click="removeComment(c)">删除</text>
						</view>
					</view>

					<view class="actions">
						<view class="action-btn" :class="{ liked: moment.liked }" @click="toggleLike">
							<text>{{ moment.liked ? '❤ 已赞' : '♡ 赞' }}</text>
						</view>
						<view class="action-btn" @click="commenting = true">
							<text>评论</text>
						</view>
					</view>
				</view>
			</view>
		</scroll-view>

		<view v-if="likesOpen" class="mask" @click.self="likesOpen = false">
			<view class="sheet">
				<view class="handle"></view>
				<view class="sheet-head">点赞 · {{ moment.likes.length }} 人</view>
				<scroll-view class="like-list" scroll-y>
					<view v-for="(n, i) in moment.likes" :key="i" class="like-item">❤ {{ n }}</view>
				</scroll-view>
				<view class="sheet-close" @click="likesOpen = false">关闭</view>
			</view>
		</view>

		<view v-if="commenting" class="mask" @click.self="closeComment">
			<view class="comment-sheet">
				<view class="handle"></view>
				<textarea
					v-model="draft"
					class="comment-input"
					placeholder="说点什么…"
					:maxlength="200"
					auto-height
					:focus="commenting"
				></textarea>
				<view class="comment-actions">
					<view class="comment-cancel" @click="closeComment">取消</view>
					<view class="comment-send" :class="{ disabled: !draft.trim() || sending }" @click="submitComment">发送</view>
				</view>
			</view>
		</view>
	</view>
</template>

<script>
import { momentDetail, momentLike, momentComment, momentDelete, momentCommentDelete } from '@/api/moment.js'

export default {
	data() {
		return {
			userInfo: {},
			moment: null,
			loading: true,
			commenting: false,
			draft: '',
			sending: false,
			likesOpen: false,
			defaultAvatar: '/static/icons/user/default-avatar.svg'
		}
	},
	onLoad(options) {
		try {
			this.userInfo = JSON.parse(uni.getStorageSync('userInfo') || '{}')
		} catch (e) {
			this.userInfo = {}
		}
		this.momentId = options.id || options.momentId || ''
		this.load()
	},
	methods: {
		async load() {
			if (!this.userInfo.id || !this.momentId) {
				this.loading = false
				return
			}
			this.loading = true
			try {
				const res = await momentDetail({ userId: this.userInfo.id, momentId: this.momentId })
				const v = res && res.data
				this.moment = v ? this.mapMoment(v) : null
			} catch (e) {
				this.moment = null
			} finally {
				this.loading = false
			}
		},
		mapMoment(v) {
			return {
				id: v.id,
				nickname: v.nickname || '用户',
				avatar: v.avatar || this.defaultAvatar,
				time: this.relTime(v.createTime),
				location: v.location || '',
				text: v.content || '',
				images: v.images || [],
				likes: v.likes || [],
				comments: this.mapComments(v.comments),
				liked: !!v.liked,
				canDelete: !!this.userInfo.id && v.userId === this.userInfo.id
			}
		},
		mapComments(list) {
			return (list || []).map(c => ({
				id: c.id,
				userId: c.userId,
				name: c.nickname,
				text: c.text,
				mine: !!this.userInfo.id && c.userId === this.userInfo.id
			}))
		},
		relTime(text) {
			if (!text) return ''
			const t = new Date(String(text).replace(/-/g, '/')).getTime()
			if (isNaN(t)) return text
			const diff = Date.now() - t
			if (diff < 60000) return '刚刚'
			if (diff < 3600000) return Math.floor(diff / 60000) + ' 分钟前'
			if (diff < 86400000) return Math.floor(diff / 3600000) + ' 小时前'
			if (diff < 7 * 86400000) return Math.floor(diff / 86400000) + ' 天前'
			return String(text).slice(0, 10)
		},
		handleAvatarError(event) {
			if (event && event.target) event.target.src = this.defaultAvatar
		},
		goBack() {
			uni.navigateBack({
				fail: () => uni.redirectTo({ url: '/pages/moments/moments' })
			})
		},
		openPreview(i) {
			uni.previewImage({ current: this.moment.images[i], urls: this.moment.images })
		},
		async toggleLike() {
			if (!this.moment) return
			try {
				const res = await momentLike({ userId: this.userInfo.id, momentId: this.moment.id })
				const v = res && res.data
				if (v) {
					this.moment.liked = !!v.liked
					this.moment.likes = v.likes || []
				}
			} catch (e) {
				uni.showToast({ title: e.message || '操作失败', icon: 'none' })
			}
		},
		closeComment() {
			this.commenting = false
			this.draft = ''
		},
		async submitComment() {
			const text = (this.draft || '').trim()
			if (!text || this.sending || !this.moment) return
			this.sending = true
			try {
				const res = await momentComment({ userId: this.userInfo.id, momentId: this.moment.id, content: text })
				const v = res && res.data
				if (v) this.moment.comments = this.mapComments(v.comments)
				this.closeComment()
			} catch (e) {
				uni.showToast({ title: e.message || '评论失败', icon: 'none' })
			} finally {
				this.sending = false
			}
		},
		removeComment(c) {
			if (!c.mine) return
			uni.showModal({
				title: '删除评论',
				content: '删除这条评论？',
				confirmText: '删除',
				confirmColor: '#ff3b30',
				success: async (r) => {
					if (!r.confirm) return
					try {
						const res = await momentCommentDelete({ userId: this.userInfo.id, commentId: c.id })
						const v = res && res.data
						if (v) this.moment.comments = this.mapComments(v.comments)
					} catch (e) {
						uni.showToast({ title: e.message || '删除失败', icon: 'none' })
					}
				}
			})
		},
		removeMoment() {
			if (!this.moment || !this.moment.canDelete) return
			uni.showModal({
				title: '删除动态',
				content: '删除后不可恢复',
				confirmText: '删除',
				confirmColor: '#ff3b30',
				success: async (r) => {
					if (!r.confirm) return
					try {
						await momentDelete({ userId: this.userInfo.id, momentId: this.moment.id })
						uni.showToast({ title: '已删除', icon: 'success' })
						setTimeout(() => this.goBack(), 600)
					} catch (e) {
						uni.showToast({ title: e.message || '删除失败', icon: 'none' })
					}
				}
			})
		}
	}
}
</script>

<style lang="scss" scoped>
.detail-page {
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

.top-action-text.danger {
	color: #ff3b30;
}

.main-content {
	flex: 1;
	min-height: 0;
	overflow: hidden;
}

.main-content ::v-deep .uni-scroll-view {
	height: 100% !important;
}

.detail-state {
	padding: 120rpx 0;
	text-align: center;
	font-size: 26rpx;
	color: #8e8e93;
}

.detail-body {
	padding: 24rpx;
}

.detail-card {
	background: #ffffff;
	border-radius: 26rpx;
	padding: 28rpx 26rpx;
	box-shadow: 0 2rpx 16rpx rgba(0, 0, 0, 0.04);
}

.author {
	display: flex;
	align-items: center;
	gap: 18rpx;
}

.avatar {
	width: 84rpx;
	height: 84rpx;
	border-radius: 12rpx;
	background: #e5e5ea;
	flex-shrink: 0;
}

.author-meta {
	display: flex;
	flex-direction: column;
	gap: 6rpx;
}

.name {
	font-size: 28rpx;
	font-weight: 700;
	color: #1c1c1e;
}

.time {
	font-size: 22rpx;
	color: #8e8e93;
}

.loc {
	display: block;
	margin-top: 18rpx;
	font-size: 24rpx;
	color: #8e8e93;
}

.text {
	display: block;
	margin-top: 18rpx;
	font-size: 30rpx;
	line-height: 1.62;
	color: #1c1c1e;
	white-space: pre-wrap;
	word-break: break-word;
}

.grid {
	margin-top: 20rpx;
	display: grid;
	gap: 8rpx;
}

.grid-1 {
	grid-template-columns: 1fr;
}

.grid-2 {
	grid-template-columns: repeat(2, 1fr);
}

.grid-3 {
	grid-template-columns: repeat(3, 1fr);
}

.cell {
	width: 100%;
	aspect-ratio: 1;
	border-radius: 12rpx;
	background: #e5e5ea;
}

.meta-line {
	margin-top: 20rpx;
	display: flex;
	align-items: center;
	gap: 24rpx;
	font-size: 24rpx;
	color: #8e8e93;
}

.meta-like {
	color: #ff3b30;
}

.comment-list {
	margin-top: 18rpx;
	padding-top: 18rpx;
	border-top: 1rpx solid rgba(60, 60, 67, 0.1);
	display: flex;
	flex-direction: column;
	gap: 14rpx;
}

.comment-row {
	display: flex;
	align-items: flex-start;
	flex-wrap: wrap;
	gap: 10rpx;
	font-size: 26rpx;
	line-height: 1.5;
}

.comment-name {
	color: #576b95;
	font-weight: 600;
}

.comment-text {
	color: #1c1c1e;
	flex: 1;
	min-width: 0;
}

.comment-del {
	margin-left: auto;
	font-size: 24rpx;
	color: #ff3b30;
}

.actions {
	margin-top: 22rpx;
	padding-top: 18rpx;
	border-top: 1rpx solid rgba(60, 60, 67, 0.1);
	display: flex;
	gap: 18rpx;
}

.action-btn {
	min-width: 140rpx;
	height: 60rpx;
	padding: 0 24rpx;
	border-radius: 16rpx;
	background: #f2f2f7;
	display: flex;
	align-items: center;
	justify-content: center;
	font-size: 26rpx;
	color: #3a3a3c;
}

.action-btn.liked {
	background: rgba(255, 59, 48, 0.1);
	color: #ff3b30;
}

.mask {
	position: fixed;
	inset: 0;
	z-index: 120;
	background: rgba(0, 0, 0, 0.4);
	display: flex;
	align-items: flex-end;
}

.sheet {
	width: 100%;
	background: #ffffff;
	border-radius: 30rpx 30rpx 0 0;
	padding: 12rpx 28rpx 28rpx;
	box-sizing: border-box;
}

.handle {
	width: 72rpx;
	height: 8rpx;
	border-radius: 4rpx;
	background: rgba(60, 60, 67, 0.2);
	margin: 8rpx auto 18rpx;
}

.sheet-head {
	text-align: center;
	font-size: 28rpx;
	font-weight: 700;
	color: #1c1c1e;
	padding-bottom: 16rpx;
}

.like-list {
	max-height: 420rpx;
}

.like-item {
	padding: 18rpx 0;
	font-size: 26rpx;
	color: #3a3a3c;
	border-bottom: 1rpx solid rgba(60, 60, 67, 0.08);
}

.sheet-close {
	margin-top: 20rpx;
	height: 84rpx;
	border-radius: 18rpx;
	background: #f2f2f7;
	display: flex;
	align-items: center;
	justify-content: center;
	font-size: 28rpx;
	font-weight: 600;
	color: #1c1c1e;
}

.comment-sheet {
	width: 100%;
	background: #ffffff;
	border-radius: 30rpx 30rpx 0 0;
	padding: 12rpx 28rpx calc(28rpx + env(safe-area-inset-bottom));
	box-sizing: border-box;
}

.comment-input {
	width: 100%;
	min-height: 150rpx;
	box-sizing: border-box;
	padding: 20rpx;
	border-radius: 16rpx;
	background: #f2f2f7;
	font-size: 28rpx;
	line-height: 1.5;
	color: #1c1c1e;
}

.comment-actions {
	margin-top: 20rpx;
	display: flex;
	gap: 18rpx;
}

.comment-cancel,
.comment-send {
	flex: 1;
	height: 84rpx;
	border-radius: 18rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	font-size: 28rpx;
	font-weight: 600;
}

.comment-cancel {
	background: #f2f2f7;
	color: #3a3a3c;
}

.comment-send {
	background: #111111;
	color: #ffffff;
}

.comment-send.disabled {
	opacity: 0.4;
}
</style>
