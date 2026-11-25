<template>
	<view class="call-records-page">
		<!-- 顶部导航 -->
		<view class="top-bar safe-top-bar">
			<view class="nav-row">
				<view class="nav-back" hover-class="nav-back-hover" @tap="goBack">
					<text class="nav-back-icon">‹</text>
				</view>
				<text class="nav-title">通话记录</text>
				<view class="nav-actions">
					<text class="nav-clear" @tap="clearAll">清空</text>
					<view class="nav-refresh" hover-class="nav-refresh-hover" @tap="reload">
						<text class="nav-refresh-text">刷新</text>
					</view>
				</view>
			</view>
			<view class="filter-row">
				<view
					v-for="tab in tabs"
					:key="tab.key"
					class="filter-chip"
					:class="{ 'filter-chip-active': filter === tab.key }"
					@tap="switchFilter(tab.key)"
				>
					<text class="filter-text">{{ tab.label }}</text>
				</view>
				<text class="total-hint">共 {{ totalCount }} 条</text>
			</view>
			<view class="search-row">
				<view class="search-box">
					<text class="search-icon">⌕</text>
					<input
						class="search-input"
						v-model="keyword"
						type="text"
						placeholder="搜索联系人"
						placeholder-class="search-placeholder"
						confirm-type="search"
						@input="onSearchInput"
						@confirm="applySearch"
					/>
				</view>
				<picker
					class="range-picker"
					mode="selector"
					:range="rangeOptions"
					range-key="label"
					:value="rangeIndex"
					@change="onRangeChange"
				>
					<view class="range-chip">
						<text class="range-text">{{ rangeOptions[rangeIndex].label }}</text>
					</view>
				</picker>
			</view>
		</view>

		<scroll-view class="list-scroll" scroll-y :scroll-top="scrollTop" @scrolltolower="loadMore">
			<view v-if="loading && !records.length" class="state-box">
				<view class="loading-ring"></view>
				<text class="state-title">正在加载通话记录…</text>
			</view>

			<view v-else-if="errorText" class="state-box">
				<text class="state-title">{{ errorText }}</text>
				<view class="retry-btn" hover-class="retry-btn-hover" @tap="reload">
					<text class="retry-text">重试</text>
				</view>
			</view>

			<view v-else-if="!records.length" class="state-box">
				<image class="empty-icon" src="/static/icons/chat/calls.svg" mode="aspectFit"></image>
				<text class="state-title">{{ filter === 'missed' ? '没有未接来电' : '还没有通话记录' }}</text>
				<text class="state-sub">视频通话结束后会自动出现在这里</text>
			</view>

			<view v-else class="record-list">
				<view
					v-for="item in records"
					:key="item.messageId"
					class="record-row"
					hover-class="record-row-hover"
					@tap="openPeerChat(item)"
				>
					<view class="avatar">
						<image v-if="item.peerAvatar" class="avatar-img" :src="item.peerAvatar" mode="aspectFill"></image>
						<text v-else class="avatar-letter">{{ (item.peerName || '?').slice(0, 1) }}</text>
					</view>

					<view class="record-main">
						<view class="record-title">
							<text class="peer-name" :class="{ 'peer-name-missed': item.missed }">{{ item.peerName || '未知用户' }}</text>
							<text class="record-time">{{ formatTime(item.createTime) }}</text>
						</view>
						<view class="record-sub">
							<text class="direction-icon" :class="item.direction === 'outgoing' ? 'direction-out' : 'direction-in'">
								{{ item.direction === 'outgoing' ? '↗' : '↙' }}
							</text>
							<text class="outcome-text" :class="{ 'outcome-danger': item.missed }">{{ outcomeText(item) }}</text>
							<text v-if="item.outcome === 'completed' && item.duration > 0" class="duration">{{ formatDuration(item.duration) }}</text>
						</view>
					</view>

					<view class="row-actions">
						<view class="row-btn row-btn-call" hover-class="row-btn-hover" @tap.stop="redial(item)">
							<image class="row-call-icon" src="/static/icons/chat/calls.svg" mode="aspectFit"></image>
						</view>
						<view class="row-btn row-btn-delete" hover-class="row-btn-hover" @tap.stop="deleteOne(item)">
							<text class="row-delete-text">删</text>
						</view>
					</view>
				</view>

				<view class="list-foot">
					<text v-if="loadingMore" class="foot-text">加载中…</text>
					<text v-else-if="hasMore" class="foot-text">上滑加载更多</text>
					<text v-else class="foot-text">已经到底了</text>
				</view>
			</view>
		</scroll-view>
	</view>
</template>

<script>
import { getCallRecords, markCallRead, deleteCallRecord, clearCallRecords } from '@/api/chat.js'

const PAGE_SIZE = 20

export default {
	data() {
		return {
			records: [],
			pageNum: 1,
			totalPage: 1,
			totalCount: 0,
			filter: 'all',
			keyword: '',
			loading: false,
			loadingMore: false,
			errorText: '',
			requestSeq: 0,
			scrollTop: 0,
			searchTimer: null,
			rangeIndex: 0,
			rangeOptions: [
				{ key: 'all', label: '全部时间' },
				{ key: 'today', label: '今天' },
				{ key: 'week', label: '近 7 天' },
				{ key: 'month', label: '近 30 天' }
			],
			tabs: [
				{ key: 'all', label: '全部' },
				{ key: 'missed', label: '未接' }
			]
		}
	},
	computed: {
		hasMore() {
			return this.pageNum < this.totalPage
		}
	},
	onLoad() {
		this.fetchRecords(true)
	},
	onUnload() {
		if (this.searchTimer) clearTimeout(this.searchTimer)
	},
	methods: {
		goBack() {
			uni.navigateBack({
				fail: () => {
					uni.reLaunch({ url: '/pages/chat/chat-list' })
				}
			})
		},
		reload() {
			this.fetchRecords(true)
		},
		switchFilter(key) {
			if (this.filter === key) return
			this.filter = key
			this.fetchRecords(true)
		},
		onSearchInput() {
			if (this.searchTimer) clearTimeout(this.searchTimer)
			this.searchTimer = setTimeout(() => this.fetchRecords(true), 320)
		},
		applySearch() {
			if (this.searchTimer) clearTimeout(this.searchTimer)
			this.fetchRecords(true)
		},
		onRangeChange(e) {
			const idx = Number((e && e.detail && e.detail.value) || 0)
			if (idx === this.rangeIndex) return
			this.rangeIndex = idx
			this.fetchRecords(true)
		},
		rangeParams() {
			const option = this.rangeOptions[this.rangeIndex] || {}
			if (!option.key || option.key === 'all') return {}
			const now = new Date()
			const start = new Date(now)
			if (option.key === 'today') {
				start.setHours(0, 0, 0, 0)
			} else {
				start.setDate(start.getDate() - (option.key === 'week' ? 6 : 29))
				start.setHours(0, 0, 0, 0)
			}
			return {
				startTime: this.toDateTime(start),
				endTime: this.toDateTime(now)
			}
		},
		toDateTime(date) {
			const pad = (n) => String(n).padStart(2, '0')
			return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ` +
				`${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
		},
		async fetchRecords(reset) {
			const seq = ++this.requestSeq
			if (reset) {
				this.loading = true
				this.errorText = ''
				this.pageNum = 1
			} else {
				this.loadingMore = true
			}
			try {
				const res = await getCallRecords({
					pageNum: reset ? 1 : this.pageNum + 1,
					pageSize: PAGE_SIZE,
					missedOnly: this.filter === 'missed',
					keyword: this.keyword.trim(),
					...this.rangeParams()
				})
				if (seq !== this.requestSeq) return
				if (!res || res.code !== 200 || !res.data) {
					throw new Error((res && res.msg) || '加载失败')
				}
				const page = res.data
				const rows = Array.isArray(page.rows) ? page.rows : []
				this.records = reset ? rows : this.records.concat(rows)
				this.pageNum = page.pageNum || 1
				this.totalPage = page.totalPage || 1
				this.totalCount = page.totalCount || 0
				if (reset) {
					this.scrollTop = 0
					// 成功进入记录页即清空未接角标，PC / 手机共用同一服务端水位
					markCallRead().catch(() => {})
				}
			} catch (e) {
				if (seq !== this.requestSeq) return
				this.errorText = (e && (e.msg || e.message)) || '加载失败，请稍后重试'
				if (reset) {
					this.records = []
					this.totalCount = 0
				}
			} finally {
				if (seq === this.requestSeq) {
					this.loading = false
					this.loadingMore = false
				}
			}
		},
		loadMore() {
			if (this.loadingMore || !this.hasMore) return
			this.fetchRecords(false)
		},
		outcomeText(item) {
			if (item.direction === 'outgoing') {
				if (item.outcome === 'completed') return '通话时长'
				if (item.outcome === 'busy') return '对方忙线中'
				if (item.outcome === 'canceled') return '已取消'
				if (item.outcome === 'timeout') return '对方无应答'
				if (item.outcome === 'rejected') return '对方已拒绝'
				return '未接通'
			}
			if (item.outcome === 'completed') return '通话时长'
			if (item.outcome === 'busy') return '忙线未接'
			if (item.outcome === 'timeout') return '未接来电'
			if (item.outcome === 'rejected') return '已拒绝'
			if (item.outcome === 'canceled') return '对方已取消'
			return '未接通'
		},
		formatDuration(sec) {
			const s = Number(sec) || 0
			if (s < 60) return `${s} 秒`
			const m = Math.floor(s / 60)
			const r = s % 60
			return r ? `${m} 分 ${r} 秒` : `${m} 分钟`
		},
		formatTime(value) {
			if (!value) return ''
			const date = new Date(value.replace(/-/g, '/'))
			if (Number.isNaN(date.getTime())) return value
			const now = new Date()
			const sameDay = date.toDateString() === now.toDateString()
			const pad = (n) => String(n).padStart(2, '0')
			if (sameDay) return `今天 ${pad(date.getHours())}:${pad(date.getMinutes())}`
			const yesterday = new Date(now.getTime() - 86400000)
			if (date.toDateString() === yesterday.toDateString()) {
				return `昨天 ${pad(date.getHours())}:${pad(date.getMinutes())}`
			}
			if (date.getFullYear() === now.getFullYear()) {
				return `${date.getMonth() + 1}月${date.getDate()}日 ${pad(date.getHours())}:${pad(date.getMinutes())}`
			}
			return `${date.getFullYear()}年${date.getMonth() + 1}月${date.getDate()}日`
		},
		openPeerChat(item, autoCall) {
			if (!item.peerId) return
			const name = encodeURIComponent(item.peerName || '')
			const extra = autoCall ? '&autoVideoCall=1' : ''
			uni.navigateTo({
				url: `/pages/chat/chat-detail?id=${item.peerId}&name=${name}&chatType=1${extra}`
			})
		},
		redial(item) {
			this.openPeerChat(item, true)
		},
		deleteOne(item) {
			if (!item.messageId) return
			uni.showModal({
				title: '删除记录',
				content: '仅你自己不可见，对方记录不受影响。',
				confirmText: '删除',
				confirmColor: '#ff3b30',
				success: async (res) => {
					if (!res.confirm) return
					try {
						const r = await deleteCallRecord({ messageId: item.messageId })
						if (!r || r.code !== 200) throw new Error((r && r.msg) || '删除失败')
						this.records = this.records.filter((row) => row.messageId !== item.messageId)
						this.totalCount = Math.max(0, this.totalCount - 1)
					} catch (e) {
						uni.showToast({ title: (e && (e.msg || e.message)) || '删除失败', icon: 'none' })
					}
				}
			})
		},
		clearAll() {
			if (!this.records.length) return
			uni.showModal({
				title: '清空通话记录',
				content: '仅你自己不可见，对方记录不受影响。',
				confirmText: '清空',
				confirmColor: '#ff3b30',
				success: async (res) => {
					if (!res.confirm) return
					try {
						const r = await clearCallRecords()
						if (!r || r.code !== 200) throw new Error((r && r.msg) || '清空失败')
						this.records = []
						this.pageNum = 1
						this.totalPage = 1
						this.totalCount = 0
						uni.showToast({ title: '已清空', icon: 'none' })
					} catch (e) {
						uni.showToast({ title: (e && (e.msg || e.message)) || '清空失败', icon: 'none' })
					}
				}
			})
		}
	}
}
</script>

<style scoped>
.call-records-page {
	height: 100vh;
	display: flex;
	flex-direction: column;
	background-color: #f2f2f7;
	box-sizing: border-box;
}

.top-bar {
	flex: none;
	background-color: #ffffff;
	border-bottom: 1rpx solid #ececf0;
}
.nav-row {
	height: 92rpx;
	display: flex;
	align-items: center;
	padding: 0 24rpx;
}
.nav-back {
	width: 68rpx;
	height: 68rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	border-radius: 34rpx;
}
.nav-back-hover { background-color: #f3f4f6; }
.nav-back-icon {
	font-size: 44rpx;
	line-height: 44rpx;
	color: #1c1c1e;
}
.nav-title {
	flex: 1;
	text-align: center;
	font-size: 34rpx;
	font-weight: 600;
	color: #1c1c1e;
}
.nav-refresh {
	padding: 10rpx 22rpx;
	border-radius: 24rpx;
	background-color: #f2f2f7;
}
.nav-refresh-hover { opacity: 0.7; }
.nav-refresh-text {
	font-size: 26rpx;
	color: #007aff;
}
.nav-actions {
	display: flex;
	align-items: center;
	gap: 14rpx;
}
.nav-clear {
	padding: 10rpx 8rpx;
	font-size: 26rpx;
	color: #ff3b30;
}

.filter-row {
	display: flex;
	align-items: center;
	padding: 0 24rpx 20rpx;
	gap: 16rpx;
}
.filter-chip {
	padding: 10rpx 30rpx;
	border-radius: 28rpx;
	background-color: #f2f2f7;
}
.filter-chip-active {
	background-color: #007aff;
	box-shadow: 0 6rpx 18rpx rgba(0, 122, 255, 0.25);
}
.filter-text {
	font-size: 26rpx;
	color: #6b7280;
}
.filter-chip-active .filter-text { color: #ffffff; }
.total-hint {
	margin-left: auto;
	font-size: 22rpx;
	color: #9ca3af;
}
.search-row {
	display: flex;
	align-items: center;
	gap: 16rpx;
	padding: 0 24rpx 20rpx;
}
.search-box {
	flex: 1;
	height: 66rpx;
	display: flex;
	align-items: center;
	gap: 10rpx;
	padding: 0 22rpx;
	border-radius: 33rpx;
	background-color: #f2f2f7;
}
.search-icon {
	font-size: 28rpx;
	color: #9ca3af;
}
.search-input {
	flex: 1;
	height: 66rpx;
	font-size: 27rpx;
	color: #1c1c1e;
}
.search-placeholder { color: #9ca3af; }
.range-picker { flex: none; }
.range-chip {
	height: 66rpx;
	display: flex;
	align-items: center;
	padding: 0 24rpx;
	border-radius: 33rpx;
	background-color: #f2f2f7;
}
.range-text {
	font-size: 25rpx;
	color: #6b7280;
}

.list-scroll {
	flex: 1;
	height: 0;
	padding: 24rpx 24rpx 40rpx;
	box-sizing: border-box;
}

.record-list {
	background-color: #ffffff;
	border-radius: 28rpx;
	overflow: hidden;
	box-shadow: 0 2rpx 10rpx rgba(0, 0, 0, 0.04);
}
.record-row {
	display: flex;
	align-items: center;
	padding: 26rpx 28rpx;
	border-bottom: 1rpx solid #f1f1f4;
	gap: 22rpx;
}
.record-row:last-child { border-bottom: none; }
.record-row-hover { background-color: #f8f8fa; }

.avatar {
	flex: none;
	width: 88rpx;
	height: 88rpx;
	border-radius: 22rpx;
	background: linear-gradient(145deg, #e8f1ff, #d4e4ff);
	display: flex;
	align-items: center;
	justify-content: center;
	overflow: hidden;
}
.avatar-img { width: 100%; height: 100%; display: block; }
.avatar-letter {
	font-size: 32rpx;
	font-weight: 600;
	color: #007aff;
}

.record-main {
	flex: 1;
	min-width: 0;
}
.record-title {
	display: flex;
	align-items: center;
	justify-content: space-between;
	gap: 16rpx;
}
.peer-name {
	font-size: 30rpx;
	font-weight: 600;
	color: #1c1c1e;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
}
.peer-name-missed { color: #ff3b30; }
.record-time {
	flex: none;
	font-size: 23rpx;
	color: #a1a1aa;
}
.record-sub {
	display: flex;
	align-items: center;
	gap: 10rpx;
	margin-top: 8rpx;
}
.direction-icon { font-size: 24rpx; }
.direction-out { color: #007aff; }
.direction-in { color: #9ca3af; }
.outcome-text {
	font-size: 25rpx;
	color: #6b7280;
}
.outcome-danger { color: #ff3b30; }
.duration {
	font-size: 23rpx;
	color: #a1a1aa;
}
.row-actions {
	flex: none;
	display: flex;
	align-items: center;
	gap: 12rpx;
}
.row-btn {
	width: 64rpx;
	height: 64rpx;
	border-radius: 32rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	background-color: #f2f2f7;
}
.row-btn-hover { opacity: 0.75; }
.row-btn-call { background-color: #e8f1ff; }
.row-call-icon {
	width: 34rpx;
	height: 34rpx;
}
.row-btn-delete { background-color: #fff0ef; }
.row-delete-text {
	font-size: 24rpx;
	color: #ff3b30;
}

.state-box {
	margin-top: 40rpx;
	padding: 110rpx 30rpx;
	background-color: #ffffff;
	border-radius: 28rpx;
	display: flex;
	flex-direction: column;
	align-items: center;
	box-shadow: 0 2rpx 10rpx rgba(0, 0, 0, 0.04);
}
.empty-icon {
	width: 88rpx;
	height: 88rpx;
	opacity: 0.55;
}
.loading-ring {
	width: 40rpx;
	height: 40rpx;
	border-radius: 50%;
	border: 4rpx solid #e5e7eb;
	border-top-color: #007aff;
	animation: spin 0.8s linear infinite;
}
@keyframes spin {
	to { transform: rotate(360deg); }
}
.state-title {
	margin-top: 22rpx;
	font-size: 27rpx;
	color: #6b7280;
	text-align: center;
}
.state-sub {
	margin-top: 8rpx;
	font-size: 23rpx;
	color: #a1a1aa;
}
.retry-btn {
	margin-top: 24rpx;
	padding: 14rpx 44rpx;
	border-radius: 28rpx;
	background-color: #007aff;
}
.retry-btn-hover { opacity: 0.8; }
.retry-text { font-size: 26rpx; color: #ffffff; }

.list-foot {
	padding: 28rpx 0 36rpx;
	display: flex;
	justify-content: center;
}
.foot-text {
	font-size: 23rpx;
	color: #a1a1aa;
}
</style>
