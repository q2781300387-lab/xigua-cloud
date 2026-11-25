<template>
	<view class="page">
		<WebSocketKeeper />

		<view class="top-bar safe-top-bar">
			<view class="top-bar-inner">
				<view class="back-btn" @tap="goBack" hover-class="back-btn-hover">
					<image class="back-icon" src="/static/icons/arrow/left.svg" mode="aspectFit"></image>
				</view>
				<text class="page-title">添加好友</text>
				<view class="top-placeholder"></view>
			</view>
		</view>

		<scroll-view class="content" scroll-y>
			<view class="search-card">
				<view class="search-box">
					<image class="search-icon" src="/static/icons/search/search.svg" mode="aspectFit"></image>
					<input
						class="search-input"
						type="text"
						v-model="searchKeyword"
						@input="handleSearch"
						placeholder="用户名或邮箱"
						placeholder-class="search-placeholder"
						confirm-type="search"
					/>
					<view v-if="searchKeyword" class="clear-btn" @tap="clearSearch">
						<text class="clear-icon">×</text>
					</view>
				</view>
				<text class="search-hint">输入对方的用户名或邮箱，搜索后可直接发送好友申请</text>
			</view>

			<view v-if="searchResults.length > 0" class="list-card">				<view
					v-for="(user, index) in searchResults"
					:key="index"
					class="result-row"
					@tap="user.isFriend || user.isLoading ? null : handleAddFriend(user, index)"
				>
					<view class="avatar-box">
						<image
							v-if="user.avatar"
							class="user-avatar"
							:src="user.avatar"
							mode="aspectFill"
						/>
						<text v-else class="avatar-letter">{{ userInitial(user) }}</text>
					</view>
					<view class="user-details">
						<view class="user-name-row">
							<text class="user-name">{{ user.username || user.userName }}</text>
							<text v-if="user.sex === 1" class="gender male">♂</text>
							<text v-if="user.sex === 2" class="gender female">♀</text>
						</view>
						<text class="user-email">{{ user.email || '暂无邮箱' }}</text>
					</view>
					<view class="add-btn" :class="{ 'is-friend': user.isFriend, 'is-loading': user.isLoading }">
						<text class="add-btn-text">{{ user.isFriend ? '已是好友' : (user.isLoading ? '发送中' : '添加') }}</text>
					</view>
				</view>
			</view>

			<view v-else-if="searchKeyword.trim()" class="empty-card">
				<text class="empty-title">未找到相关用户</text>
				<text class="empty-desc">换个用户名或邮箱试试，也可能是对方尚未注册</text>
			</view>

			<view v-else class="empty-card">
				<text class="empty-title">搜索并添加好友</text>
				<text class="empty-desc">输入用户名或邮箱后，这里会显示匹配的用户</text>
			</view>

			<!-- 加入群聊：群号 / 群名称搜索，提交后由群主审批 -->
			<view class="group-join-card">
				<text class="group-join-title">加入群聊</text>
				<view class="group-search-box">
					<input
						class="group-search-input"
						v-model="groupKeyword"
						placeholder="输入群号或群名称"
						placeholder-class="search-placeholder"
						confirm-type="search"
						@confirm="searchJoinGroups"
					/>
					<view class="group-search-btn" @tap="searchJoinGroups" hover-class="copy-btn-hover">
						<text class="group-search-btn-text">{{ groupLoading ? '搜索中' : '搜索' }}</text>
					</view>
				</view>
				<view v-if="groupResults.length" class="group-result-list">
					<view v-for="g in groupResults" :key="g.groupId" class="group-result-row">
						<view class="group-result-avatar">
							<image v-if="g.groupAvatar" class="group-avatar-img" :src="g.groupAvatar" mode="aspectFill" />
							<text v-else class="group-avatar-letter">{{ (g.groupName || '群')[0] }}</text>
						</view>
						<view class="group-result-info">
							<text class="group-result-name">{{ g.groupName }}</text>
							<text class="group-result-meta">群号 {{ g.groupId }} · {{ g.currentMember || 0 }} 人</text>
						</view>
						<view class="group-apply-btn" :class="{ joined: g.joined }" @tap="g.joined ? null : applyJoinGroup(g)" hover-class="copy-btn-hover">
							<text class="group-apply-text">{{ g.joined ? '已在群内' : '申请加入' }}</text>
						</view>
					</view>
				</view>
				<text v-else-if="groupSearched" class="group-search-empty">没有找到匹配的群聊</text>
			</view>

			<view class="me-card">
				<view class="me-info">
					<view class="me-avatar">
						<text class="me-letter">{{ myInitial }}</text>
					</view>
					<view class="me-text">
						<text class="me-label">我的微信号</text>
						<text class="me-id">xg_{{ userId }}</text>
					</view>
				</view>
				<view class="copy-btn" @tap="copyMyId" hover-class="copy-btn-hover">
					<text class="copy-text">复制</text>
				</view>
			</view>
		</scroll-view>
	</view>
</template>

<script>
import { getListByName } from '@/api/user.js'
import { sendFriendRequest } from '@/api/friend.js'
import { searchGroups, requestJoinGroup } from '@/api/group.js'
import WebSocketKeeper from '@/components/WebSocketKeeper.vue'

export default {
	components: {
		WebSocketKeeper
	},
	data() {
		return {
			searchKeyword: '',
			searchResults: [],
			searchTimer: null,
			groupKeyword: '',
			groupResults: [],
			groupLoading: false,
			groupSearched: false,
			userInfo: {}
		}
	},
	computed: {
		userId() {
			return (this.userInfo && this.userInfo.id) || ''
		},
		myInitial() {
			const name = (this.userInfo && (this.userInfo.username || this.userInfo.userName)) || '我'
			return String(name).slice(0, 1).toUpperCase()
		}
	},
	onLoad() {
		try {
			this.userInfo = JSON.parse(uni.getStorageSync('userInfo') || '{}')
		} catch (e) {
			this.userInfo = {}
		}
	},
	methods: {
		async searchJoinGroups() {
			const keyword = String(this.groupKeyword || '').trim()
			if (!keyword) {
				uni.showToast({ title: '请输入群号或群名称', icon: 'none' })
				return
			}
			this.groupLoading = true
			this.groupSearched = false
			try {
				const res = await searchGroups(keyword)
				this.groupResults = (res && res.data) || []
				this.groupSearched = true
			} catch (e) {
				this.groupResults = []
			} finally {
				this.groupLoading = false
			}
		},
		async applyJoinGroup(group) {
			try {
				const res = await requestJoinGroup({ groupId: group.groupId, reason: '' })
				if (res && res.code === 200) {
					uni.showToast({ title: '申请已提交，等待群主审批', icon: 'none' })
				}
			} catch (e) { /* 请求层已提示 */ }
		},
		goBack() {
			const pages = getCurrentPages()
			if (pages.length > 1) {
				uni.navigateBack()
			} else {
				uni.redirectTo({ url: '/pages/contacts/contact-list' })
			}
		},
		userInitial(user) {
			const name = user.username || user.userName || '?'
			return String(name).slice(0, 1).toUpperCase()
		},
		clearSearch() {
			this.searchKeyword = ''
			this.searchResults = []
			if (this.searchTimer) {
				clearTimeout(this.searchTimer)
				this.searchTimer = null
			}
		},
		handleSearch() {
			if (this.searchTimer) {
				clearTimeout(this.searchTimer)
			}
			if (!this.searchKeyword.trim()) {
				this.searchResults = []
				return
			}
			this.searchTimer = setTimeout(() => {
				this.searchUsers()
			}, 500)
		},
		async searchUsers() {
			try {
				const keyword = this.searchKeyword.trim()
				if (!keyword) {
					return
				}
				const res = await getListByName(keyword)
				if (res.code === 200 && res.data) {
					if (Array.isArray(res.data)) {
						this.searchResults = res.data
					} else if (res.data.list && Array.isArray(res.data.list)) {
						this.searchResults = res.data.list
					} else if (typeof res.data === 'object') {
						this.searchResults = [res.data]
					} else {
						this.searchResults = []
					}
				} else {
					this.searchResults = []
				}
			} catch (error) {
				console.error('搜索用户失败:', error)
				this.searchResults = []
			}
		},
		async handleAddFriend(user, index) {
			const name = user.username || user.userName
			uni.showModal({
				title: '添加好友',
				content: `确定要添加 ${name} 为好友吗？`,
				confirmColor: '#111111',
				success: async (res) => {
					if (!res.confirm) return
					this.$set(this.searchResults[index], 'isLoading', true)
					try {
						const result = await sendFriendRequest({
							friendId: user.id || user.userId,
							applyMsg: '你好，我想添加你为好友'
						})
						if (result.code === 200) {
							this.$set(this.searchResults[index], 'isFriend', true)
							uni.showToast({ title: '好友申请已发送', icon: 'success' })
						} else {
							uni.showToast({ title: result.msg || '发送失败', icon: 'none' })
						}
					} catch (error) {
						let errorMsg = '发送失败，请重试'
						if (error && error.msg) {
							errorMsg = error.msg
						} else if (error && error.message) {
							errorMsg = error.message
						}
						uni.showToast({ title: errorMsg, icon: 'none' })
					} finally {
						this.$set(this.searchResults[index], 'isLoading', false)
					}
				}
			})
		},
		copyMyId() {
			if (!this.userId) {
				uni.showToast({ title: '暂无微信号', icon: 'none' })
				return
			}
			uni.setClipboardData({
				data: `xg_${this.userId}`,
				success: () => {
					uni.showToast({ title: '微信号已复制', icon: 'success' })
				}
			})
		}
	},
	onUnload() {
		if (this.searchTimer) {
			clearTimeout(this.searchTimer)
		}
	}
}
</script>

<style lang="scss" scoped>
.page {
	display: block;
	width: 100%;
	height: 100vh;
	background: #f2f2f7;
}

.top-bar {
	position: fixed;
	top: 0;
	left: 0;
	right: 0;
	z-index: 50;
	background: rgba(255, 255, 255, 0.88);
	backdrop-filter: saturate(180%) blur(20px);
	-webkit-backdrop-filter: saturate(180%) blur(20px);
	border-bottom: 1rpx solid rgba(60, 60, 67, 0.12);
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

.back-btn-hover {
	opacity: 0.6;
}

.back-icon {
	width: 44rpx;
	height: 44rpx;
	opacity: 0.75;
}

.page-title {
	font-size: 34rpx;
	font-weight: 600;
	color: #1c1c1e;
}

.top-placeholder {
	width: 64rpx;
}

.content {
	display: block;
	height: 100vh;
	padding-top: calc(var(--status-bar-height) + 116rpx);
	padding-bottom: calc(48rpx + env(safe-area-inset-bottom));
	box-sizing: border-box;
}

.search-card {
	display: block;
	margin: 0 24rpx 24rpx;
	padding: 24rpx;
	background: #ffffff;
	border: 1rpx solid rgba(60, 60, 67, 0.12);
	border-radius: 20rpx;
}

.search-box {
	display: flex;
	align-items: center;
	height: 72rpx;
	padding: 0 20rpx;
	background: rgba(120, 120, 128, 0.12);
	border-radius: 18rpx;
}

.search-icon {
	width: 32rpx;
	height: 32rpx;
	opacity: 0.45;
	flex-shrink: 0;
}

.search-input {
	flex: 1;
	min-width: 0;
	height: 72rpx;
	margin-left: 12rpx;
	font-size: 27rpx;
	color: #1c1c1e;
}

.search-placeholder {
	color: rgba(60, 60, 67, 0.4);
	font-size: 27rpx;
}

.clear-btn {
	width: 40rpx;
	height: 40rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	border-radius: 50%;
	background: rgba(60, 60, 67, 0.16);
	flex-shrink: 0;
}

.clear-icon {
	font-size: 24rpx;
	line-height: 1;
	color: #ffffff;
}

.search-hint {
	display: block;
	margin-top: 14rpx;
	font-size: 22rpx;
	line-height: 1.5;
	color: rgba(60, 60, 67, 0.5);
}

.list-card {
	display: block;
	margin: 0 24rpx 24rpx;
	background: #ffffff;
	border: 1rpx solid rgba(60, 60, 67, 0.12);
	border-radius: 20rpx;
	overflow: hidden;
}

.result-row {
	display: flex;
	align-items: center;
	padding: 22rpx 24rpx;
	border-bottom: 1rpx solid rgba(60, 60, 67, 0.1);
}

.result-row:last-child {
	border-bottom: 0;
}

.avatar-box {
	width: 88rpx;
	height: 88rpx;
	border-radius: 12rpx;
	background: #f2f2f7;
	display: flex;
	align-items: center;
	justify-content: center;
	overflow: hidden;
	flex-shrink: 0;
}

.avatar-box img {
	width: 100%;
	height: 100%;
	object-fit: cover;
}

.user-avatar {
	width: 100%;
	height: 100%;
}

.avatar-letter {
	font-size: 32rpx;
	font-weight: 600;
	color: #1c1c1e;
}

.user-details {
	flex: 1;
	min-width: 0;
	margin-left: 20rpx;
}

.user-name-row {
	display: flex;
	align-items: center;
	gap: 8rpx;
}

.user-name {
	font-size: 29rpx;
	font-weight: 600;
	color: #1c1c1e;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
}

.gender {
	font-size: 22rpx;
	color: rgba(60, 60, 67, 0.55);
}

.user-email {
	display: block;
	margin-top: 6rpx;
	font-size: 23rpx;
	color: rgba(60, 60, 67, 0.55);
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
}

.add-btn {
	display: flex;
	align-items: center;
	justify-content: center;
	height: 56rpx;
	padding: 0 24rpx;
	border-radius: 999rpx;
	background: #111111;
	flex-shrink: 0;
}

.add-btn-text {
	font-size: 23rpx;
	font-weight: 600;
	color: #ffffff;
}

.add-btn.is-friend {
	background: rgba(120, 120, 128, 0.14);
}

.add-btn.is-friend .add-btn-text {
	color: rgba(60, 60, 67, 0.55);
}

.add-btn.is-loading {
	opacity: 0.6;
}

.empty-card {
	display: block;
	margin: 0 24rpx 24rpx;
	padding: 52rpx 32rpx;
	background: #ffffff;
	border: 1rpx solid rgba(60, 60, 67, 0.12);
	border-radius: 20rpx;
	text-align: center;
}

.empty-title {
	display: block;
	font-size: 28rpx;
	font-weight: 600;
	color: #1c1c1e;
}

.empty-desc {
	display: block;
	margin-top: 10rpx;
	font-size: 23rpx;
	line-height: 1.6;
	color: rgba(60, 60, 67, 0.5);
}

/* 加入群聊 */
.group-join-card {
	margin: 16px 16px 0;
	padding: 14px;
	background: var(--card, #fff);
	border-radius: 14px;
}
.group-join-title {
	display: block;
	font-size: 14px;
	font-weight: 600;
	color: var(--text, #111);
	margin-bottom: 10px;
}
.group-search-box {
	display: flex;
	align-items: center;
	gap: 8px;
}
.group-search-input {
	flex: 1;
	height: 36px;
	padding: 0 12px;
	font-size: 13px;
	border-radius: 10px;
	background: rgba(0, 0, 0, 0.04);
	color: var(--text, #111);
}
.group-search-btn {
	padding: 0 14px;
	height: 36px;
	display: flex;
	align-items: center;
	border-radius: 10px;
	background: #111;
}
.group-search-btn-text {
	font-size: 13px;
	color: #fff;
}
.group-result-list {
	margin-top: 10px;
	display: flex;
	flex-direction: column;
	gap: 10px;
}
.group-result-row {
	display: flex;
	align-items: center;
	gap: 10px;
	padding: 8px;
	border-radius: 10px;
	background: rgba(0, 0, 0, 0.03);
}
.group-result-avatar {
	width: 36px;
	height: 36px;
	border-radius: 10px;
	overflow: hidden;
	background: #111;
	display: flex;
	align-items: center;
	justify-content: center;
	flex: 0 0 auto;
}
.group-avatar-img { width: 100%; height: 100%; }
.group-avatar-letter { color: #fff; font-size: 14px; }
.group-result-info { flex: 1; min-width: 0; }
.group-result-name {
	display: block;
	font-size: 13px;
	font-weight: 600;
	color: var(--text, #111);
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
}
.group-result-meta {
	display: block;
	font-size: 11px;
	color: var(--text-2, #8e8e93);
	margin-top: 2px;
}
.group-apply-btn {
	padding: 6px 12px;
	border-radius: 8px;
	background: #111;
	flex: 0 0 auto;
}
.group-apply-btn.joined { background: rgba(0, 0, 0, 0.08); }
.group-apply-text { font-size: 12px; color: #fff; }
.group-apply-btn.joined .group-apply-text { color: #8e8e93; }
.group-search-empty {
	display: block;
	margin-top: 10px;
	font-size: 12px;
	color: var(--text-2, #8e8e93);
	text-align: center;
}

.me-card {
	display: flex;
	align-items: center;
	justify-content: space-between;
	margin: 0 24rpx;
	padding: 24rpx;
	background: #ffffff;
	border: 1rpx solid rgba(60, 60, 67, 0.12);
	border-radius: 20rpx;
}

.me-info {
	display: flex;
	align-items: center;
	min-width: 0;
}

.me-avatar {
	width: 80rpx;
	height: 80rpx;
	border-radius: 50%;
	background: #111111;
	display: flex;
	align-items: center;
	justify-content: center;
	flex-shrink: 0;
}

.me-letter {
	font-size: 30rpx;
	font-weight: 600;
	color: #ffffff;
}

.me-text {
	margin-left: 18rpx;
	min-width: 0;
}

.me-label {
	display: block;
	font-size: 22rpx;
	color: rgba(60, 60, 67, 0.5);
}

.me-id {
	display: block;
	margin-top: 4rpx;
	font-size: 26rpx;
	font-weight: 500;
	color: #1c1c1e;
}

.copy-btn {
	display: flex;
	align-items: center;
	justify-content: center;
	height: 56rpx;
	padding: 0 30rpx;
	border-radius: 999rpx;
	border: 1rpx solid rgba(60, 60, 67, 0.16);
	flex-shrink: 0;
}

.copy-btn-hover {
	background: rgba(120, 120, 128, 0.08);
}

.copy-text {
	font-size: 23rpx;
	font-weight: 500;
	color: #1c1c1e;
}
</style>
