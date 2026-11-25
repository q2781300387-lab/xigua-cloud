<template>
	<view class="favorites-page">
		<view class="page-nav">
			<view class="nav-back" @click="goBack">
				<text class="nav-back-icon">‹</text>
			</view>
			<text class="nav-title">收藏夹</text>
			<text class="nav-count">共 {{ totalCount }} 条</text>
		</view>

		<view class="search-bar">
			<text class="search-icon">⌕</text>
			<input
				v-model="keyword"
				class="search-input"
				type="text"
				placeholder="搜索内容、会话或发送人"
				confirm-type="search"
				@input="onSearchInput"
				@confirm="load(true)"
			/>
			<text v-if="keyword" class="search-clear" @click="clearKeyword">×</text>
		</view>

		<scroll-view scroll-x class="type-scroll" :show-scrollbar="false">
			<view class="type-row">
				<view
					v-for="tab in tabs"
					:key="tab.key"
					class="type-chip"
					:class="{ 'type-chip-active': currentType === tab.key }"
					@click="switchType(tab.key)"
				>{{ tab.label }}</view>
			</view>
		</scroll-view>

		<view v-if="loading && !items.length" class="state-box">
			<text class="state-text">正在加载收藏…</text>
		</view>

		<view v-else-if="errorText" class="state-box">
			<text class="state-text">{{ errorText }}</text>
			<text class="state-action" @click="load(true)">重试</text>
		</view>

		<view v-else-if="!items.length" class="state-box empty">
			<text class="empty-icon">☆</text>
			<text class="empty-title">{{ keyword ? '没有匹配的收藏' : '还没有收藏消息' }}</text>
			<text class="empty-hint">在聊天里长按消息，选择「收藏」即可加入这里</text>
		</view>

		<scroll-view
			v-else
			scroll-y
			class="favorite-scroll"
			@scrolltolower="loadMore"
			:lower-threshold="80"
		>
			<view
				v-for="item in items"
				:key="item.id"
				class="favorite-card"
				@click="openItem(item)"
			>
				<view class="card-head">
					<text class="chat-name">{{ item.chatName || '聊天' }}</text>
					<text class="type-label">{{ typeLabel(item.messageType) }}</text>
					<text class="card-time">{{ formatTime(item.createTime) }}</text>
				</view>

				<view class="card-body">
					<image
						v-if="Number(item.messageType) === 2 && isImageContent(item.content)"
						:src="item.content"
						class="card-thumb"
						mode="aspectFill"
					/>
					<text class="card-text">
						<text class="sender">{{ item.senderName || '对方' }}：</text>{{ previewText(item) }}
					</text>
				</view>

				<view class="card-foot">
					<text class="card-sender">{{ item.senderName || '对方' }}</text>
					<text class="card-remove" @click.stop="removeItem(item)">移除</text>
				</view>
			</view>

			<view v-if="hasMore" class="load-more" @click="loadMore">
				<text>{{ loadingMore ? '加载中…' : '加载更多' }}</text>
			</view>
			<view v-else class="load-end">没有更多了</view>
			<view class="list-bottom-space"></view>
		</scroll-view>

		<!-- 收藏详情 -->
		<view v-if="activeItem" class="detail-mask" @click="closeDetail">
			<view class="detail-panel" @click.stop>
				<view class="detail-head">
					<view class="detail-head-text">
						<text class="detail-chat">{{ activeItem.chatName || '聊天' }}</text>
						<text class="detail-time">{{ formatTime(activeItem.originalTime || activeItem.createTime) }}</text>
					</view>
					<text class="detail-close" @click="closeDetail">×</text>
				</view>

				<view class="detail-meta">
					<text>{{ activeItem.senderName || '对方' }}</text>
					<text class="detail-dot">·</text>
					<text>{{ typeLabel(activeItem.messageType) }}</text>
				</view>

				<scroll-view scroll-y class="detail-scroll">
					<image
						v-if="Number(activeItem.messageType) === 2 && isImageContent(activeItem.content)"
						:src="activeItem.content"
						class="detail-image"
						mode="widthFix"
						@click="previewImage(activeItem.content)"
					/>
					<view v-else-if="Number(activeItem.messageType) === 7" class="merge-detail">
						<text class="merge-detail-head">聊天记录 · 共 {{ parseMerged(activeItem.content).length }} 条</text>
						<view v-for="(it, index) in parseMerged(activeItem.content)" :key="index" class="merge-item">
							<view class="merge-item-head">
								<text class="merge-sender">{{ it.s || it.senderName || '成员' }}</text>
								<text class="merge-time">{{ it.t || it.time || '' }}</text>
							</view>
							<text class="merge-item-text">{{ it.c || it.content || '' }}</text>
						</view>
						<view v-if="!parseMerged(activeItem.content).length" class="merge-empty">聊天记录内容不可解析，已保留原始快照</view>
					</view>
					<text v-else class="detail-text">{{ detailText(activeItem) }}</text>
				</scroll-view>

				<view class="detail-actions">
					<view class="detail-btn" @click="copyContent">复制内容</view>
					<view class="detail-btn detail-btn-danger" @click="removeItem(activeItem)">移除收藏</view>
				</view>
			</view>
		</view>
	</view>
</template>

<script>
import { getMessageFavorites, removeMessageFavorite } from '@/api/chat.js'

export default {
	data() {
		return {
			tabs: [
				{ key: 'all', label: '全部' },
				{ key: '1', label: '文字' },
				{ key: '2', label: '图片' },
				{ key: '5', label: '文件' },
				{ key: '7', label: '聊天记录' }
			],
			currentType: 'all',
			keyword: '',
			items: [],
			totalCount: 0,
			pageNum: 1,
			pageSize: 20,
			totalPage: 1,
			loading: false,
			loadingMore: false,
			errorText: '',
			searchTimer: null,
			activeItem: null
		}
	},
	computed: {
		hasMore() {
			return this.pageNum < this.totalPage
		}
	},
	onLoad() {
		this.load(true)
	},
	beforeDestroy() {
		if (this.searchTimer) clearTimeout(this.searchTimer)
	},
	methods: {
		goBack() {
			uni.navigateBack()
		},
		async load(reset = false) {
			if (reset) {
				this.pageNum = 1
				this.items = []
				this.totalCount = 0
				this.totalPage = 1
				this.loading = true
				this.errorText = ''
				this.activeItem = null
			} else {
				this.loadingMore = true
			}
			try {
				const res = await getMessageFavorites({
					pageNum: this.pageNum,
					pageSize: this.pageSize,
					keyword: this.keyword.trim(),
					messageType: this.currentType === 'all' ? null : Number(this.currentType)
				})
				if (res.code !== 200) throw new Error(res.message || '收藏列表加载失败')
				const data = res.data || {}
				this.items = reset ? (data.rows || []) : [...this.items, ...(data.rows || [])]
				this.totalCount = Number(data.totalCount || 0)
				this.totalPage = Number(data.totalPage || 1)
			} catch (error) {
				this.errorText = (error && error.message) || '收藏列表加载失败，请稍后重试'
			} finally {
				this.loading = false
				this.loadingMore = false
			}
		},
		loadMore() {
			if (this.loadingMore || this.loading || !this.hasMore) return
			this.pageNum += 1
			this.load(false)
		},
		onSearchInput() {
			if (this.searchTimer) clearTimeout(this.searchTimer)
			this.searchTimer = setTimeout(() => this.load(true), 350)
		},
		clearKeyword() {
			this.keyword = ''
			this.load(true)
		},
		switchType(key) {
			if (this.currentType === key) return
			this.currentType = key
			this.load(true)
		},
		openItem(item) {
			this.activeItem = item
		},
		closeDetail() {
			this.activeItem = null
		},
		removeItem(item) {
			if (!item || !item.messageId) return
			uni.showModal({
				title: '移除收藏',
				content: '从收藏夹移除这条消息？仅影响你自己的收藏。',
				confirmText: '移除',
				confirmColor: '#111111',
				success: async (res) => {
					if (!res.confirm) return
					try {
						const result = await removeMessageFavorite({ messageId: item.messageId })
						if (result.code !== 200) throw new Error(result.message || '移除失败')
						this.items = this.items.filter(row => String(row.messageId) !== String(item.messageId))
						this.totalCount = Math.max(0, this.totalCount - 1)
						if (this.activeItem && String(this.activeItem.messageId) === String(item.messageId)) {
							this.activeItem = null
						}
						uni.showToast({ title: '已移除收藏', icon: 'none' })
					} catch (error) {
						uni.showToast({ title: (error && error.message) || '移除失败，请重试', icon: 'none' })
					}
				}
			})
		},
		copyContent() {
			const text = this.activeItem ? this.detailText(this.activeItem) : ''
			if (!text) return
			uni.setClipboardData({
				data: text,
				success: () => uni.showToast({ title: '已复制', icon: 'none' })
			})
		},
		previewImage(url) {
			if (!url) return
			uni.previewImage({ urls: [url], current: url })
		},
		typeLabel(type) {
			const map = { 1: '文字', 2: '图片', 3: '语音', 4: '视频', 5: '文件', 6: '系统', 7: '聊天记录', 8: '投票', 9: '通话' }
			return map[Number(type)] || '消息'
		},
		isImageContent(content) {
			return /^(https?:\/\/|\/media\/|data:image\/)/.test(String(content || ''))
		},
		previewText(item) {
			const type = Number(item.messageType)
			const raw = String(item.content || '')
			if (type === 2) return '[图片]'
			if (type === 3) return '[语音]'
			if (type === 4) return '[视频]'
			if (type === 5) {
				const name = this.fileNameFromUrl(raw)
				return name ? '[文件] ' + name : '[文件]'
			}
			if (type === 7) return `[聊天记录] ${this.parseMerged(raw).length} 条`
			if (type === 8) return '[投票]'
			if (type === 9) return '[通话记录]'
			return raw.replace(/\s+/g, ' ').slice(0, 90) || '[空消息]'
		},
		detailText(item) {
			const type = Number(item.messageType)
			if (type === 2) return '[图片] ' + item.content
			if (type === 3) return '[语音] ' + item.content
			if (type === 4) return '[视频] ' + item.content
			if (type === 5) return '[文件] ' + (this.fileNameFromUrl(item.content) || item.content)
			return String(item.content || '')
		},
		fileNameFromUrl(raw) {
			try {
				const url = String(raw || '')
				const last = url.split('?')[0].split('/').pop() || ''
				return decodeURIComponent(last)
			} catch (e) {
				return ''
			}
		},
		parseMerged(content) {
			try {
				let raw = String(content || '[]')
				if (raw.indexOf('【聊天记录】') === 0) raw = raw.slice('【聊天记录】'.length)
				const arr = JSON.parse(raw)
				return Array.isArray(arr) ? arr : []
			} catch (e) {
				return []
			}
		},
		formatTime(value) {
			if (!value) return ''
			const text = String(value).replace('T', ' ')
			return text.length >= 16 ? text.slice(0, 16) : text
		}
	}
}
</script>

<style lang="scss" scoped>
.favorites-page {
	width: 100%;
	height: 100vh;
	background: #ffffff;
	display: flex;
	flex-direction: column;
	overflow: hidden;
}

.page-nav {
	padding-top: var(--status-bar-height, 44px);
	min-height: 88rpx;
	display: flex;
	align-items: center;
	padding-left: 16rpx;
	padding-right: 24rpx;
	border-bottom: 1rpx solid rgba(0, 0, 0, 0.06);
	box-sizing: content-box;
}

.nav-back {
	width: 64rpx;
	height: 64rpx;
	display: flex;
	align-items: center;
	justify-content: center;
}

.nav-back-icon {
	font-size: 48rpx;
	color: #111111;
	line-height: 1;
}

.nav-title {
	flex: 1;
	text-align: center;
	font-size: 32rpx;
	font-weight: 600;
	color: #111111;
}

.nav-count {
	min-width: 100rpx;
	text-align: right;
	font-size: 22rpx;
	color: #8a8a8e;
}

.search-bar {
	margin: 20rpx 24rpx 12rpx;
	height: 72rpx;
	background: #f5f5f7;
	border-radius: 16rpx;
	display: flex;
	align-items: center;
	padding: 0 22rpx;
}

.search-icon {
	font-size: 30rpx;
	color: #8a8a8e;
	margin-right: 12rpx;
}

.search-input {
	flex: 1;
	height: 72rpx;
	font-size: 26rpx;
	color: #111111;
}

.search-clear {
	width: 48rpx;
	text-align: center;
	font-size: 32rpx;
	color: #8a8a8e;
}

.type-scroll {
	width: 100%;
	white-space: nowrap;
}

.type-row {
	display: flex;
	align-items: center;
	gap: 14rpx;
	padding: 10rpx 24rpx 18rpx;
}

.type-chip {
	padding: 12rpx 28rpx;
	border-radius: 999rpx;
	background: #f5f5f7;
	font-size: 24rpx;
	color: #555555;
}

.type-chip-active {
	background: #111111;
	color: #ffffff;
}

.favorite-scroll {
	flex: 1;
	padding: 0 24rpx;
	box-sizing: border-box;
}

.favorite-card {
	padding: 26rpx;
	margin-bottom: 18rpx;
	background: #ffffff;
	border: 1rpx solid rgba(0, 0, 0, 0.07);
	border-radius: 22rpx;
	box-shadow: 0 8rpx 22rpx rgba(0, 0, 0, 0.04);
}

.card-head {
	display: flex;
	align-items: center;
	gap: 12rpx;
}

.chat-name {
	flex: 1;
	font-size: 26rpx;
	font-weight: 600;
	color: #111111;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
}

.type-label {
	font-size: 20rpx;
	color: #666666;
	background: #f2f2f4;
	padding: 4rpx 14rpx;
	border-radius: 999rpx;
}

.card-time {
	font-size: 20rpx;
	color: #a0a0a5;
}

.card-body {
	margin-top: 16rpx;
	display: flex;
	align-items: flex-start;
	gap: 16rpx;
}

.card-thumb {
	width: 120rpx;
	height: 120rpx;
	border-radius: 12rpx;
	flex-shrink: 0;
}

.card-text {
	flex: 1;
	font-size: 24rpx;
	line-height: 1.55;
	color: #444444;
	word-break: break-all;
}

.sender {
	color: #8a8a8e;
}

.card-foot {
	margin-top: 18rpx;
	padding-top: 16rpx;
	border-top: 1rpx solid rgba(0, 0, 0, 0.05);
	display: flex;
	align-items: center;
	justify-content: space-between;
}

.card-sender {
	font-size: 21rpx;
	color: #8a8a8e;
}

.card-remove {
	font-size: 21rpx;
	color: #999999;
	padding: 4rpx 12rpx;
}

.load-more {
	text-align: center;
	padding: 24rpx 0;
	font-size: 24rpx;
	color: #555555;
}

.load-end {
	text-align: center;
	padding: 24rpx 0;
	font-size: 22rpx;
	color: #b0b0b5;
}

.list-bottom-space {
	height: 30rpx;
	width: 100%;
}

.state-box {
	flex: 1;
	display: flex;
	flex-direction: column;
	align-items: center;
	justify-content: center;
	text-align: center;
	padding: 40rpx;
}

.state-text {
	font-size: 25rpx;
	color: #666666;
}

.state-action {
	margin-top: 20rpx;
	font-size: 24rpx;
	color: #111111;
	text-decoration: underline;
}

.empty-icon {
	font-size: 76rpx;
	color: #c0c0c5;
	margin-bottom: 24rpx;
}

.empty-title {
	font-size: 28rpx;
	color: #111111;
	font-weight: 600;
}

.empty-hint {
	margin-top: 14rpx;
	font-size: 23rpx;
	line-height: 1.6;
	color: #8a8a8e;
}

.detail-mask {
	position: fixed;
	top: 0;
	right: 0;
	bottom: 0;
	left: 0;
	z-index: 1000;
	background: rgba(0, 0, 0, 0.42);
	display: flex;
	align-items: flex-end;
}

.detail-panel {
	width: 100%;
	height: 78vh;
	background: #ffffff;
	border-radius: 28rpx 28rpx 0 0;
	display: flex;
	flex-direction: column;
	overflow: hidden;
}

.detail-head {
	padding: 28rpx 32rpx 12rpx;
	display: flex;
	align-items: center;
	justify-content: space-between;
}

.detail-head-text {
	display: flex;
	flex-direction: column;
}

.detail-chat {
	font-size: 30rpx;
	font-weight: 600;
	color: #111111;
}

.detail-time {
	margin-top: 6rpx;
	font-size: 21rpx;
	color: #8a8a8e;
}

.detail-close {
	font-size: 40rpx;
	color: #8a8a8e;
	padding: 4rpx 12rpx;
}

.detail-meta {
	display: flex;
	align-items: center;
	gap: 10rpx;
	padding: 0 32rpx 20rpx;
	font-size: 23rpx;
	color: #666666;
	border-bottom: 1rpx solid rgba(0, 0, 0, 0.06);
}

.detail-dot {
	color: #c0c0c5;
}

.detail-scroll {
	flex: 1;
	padding: 24rpx 32rpx;
	box-sizing: border-box;
}

.detail-image {
	width: 100%;
	border-radius: 14rpx;
}

.detail-text {
	font-size: 26rpx;
	line-height: 1.65;
	color: #222222;
	word-break: break-all;
	white-space: pre-wrap;
}

.merge-detail {
	display: flex;
	flex-direction: column;
}

.merge-detail-head {
	font-size: 23rpx;
	color: #8a8a8e;
	margin-bottom: 12rpx;
}

.merge-item {
	padding: 18rpx 0;
	border-bottom: 1rpx solid rgba(0, 0, 0, 0.05);
}

.merge-item-head {
	display: flex;
	align-items: center;
	justify-content: space-between;
}

.merge-sender {
	font-size: 21rpx;
	color: #8a8a8e;
}

.merge-time {
	font-size: 20rpx;
	color: #b0b0b5;
}

.merge-item-text {
	display: block;
	margin-top: 8rpx;
	font-size: 25rpx;
	line-height: 1.55;
	color: #333333;
	word-break: break-all;
}

.merge-empty {
	font-size: 23rpx;
	color: #999999;
	padding: 30rpx 0;
}

.detail-actions {
	display: flex;
	gap: 16rpx;
	padding: 20rpx 32rpx 40rpx;
	border-top: 1rpx solid rgba(0, 0, 0, 0.06);
}

.detail-btn {
	flex: 1;
	height: 80rpx;
	border-radius: 16rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	font-size: 26rpx;
	background: #f5f5f7;
	color: #111111;
}

.detail-btn-danger {
	background: #ffffff;
	border: 1rpx solid rgba(0, 0, 0, 0.12);
	color: #333333;
}
</style>
