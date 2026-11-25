<template>
	<view class="page">
		<WebSocketKeeper />

		<view class="top-bar safe-top-bar">
			<view class="top-bar-inner">
				<view class="top-btn" @tap="handleCancel">
					<text class="cancel-text">取消</text>
				</view>
				<text class="page-title">发起群聊</text>
				<view class="top-btn confirm-btn" :class="{ disabled: selectedCount < 2 }" @tap="handleConfirm">
					<text class="confirm-text">确定</text>
				</view>
			</view>
		</view>

		<scroll-view class="content" scroll-y>
			<view class="search-card">
				<view class="search-box">
					<image class="search-icon" src="/static/icons/search/search.svg" mode="aspectFit"></image>
					<input
						class="search-input"
						type="text"
						placeholder="搜索联系人"
						placeholder-class="search-placeholder"
						v-model="searchText"
					/>
					<view v-if="searchText" class="clear-btn" @tap="searchText = ''">
						<text class="clear-icon">×</text>
					</view>
				</view>
			</view>

			<view v-if="selectedCount > 0" class="selected-card">
				<view class="selected-head">
					<text class="selected-label">已选择 {{ selectedCount }} 人</text>
					<text class="selected-limit">最多 500 人</text>
				</view>
				<scroll-view class="selected-scroll" scroll-x>
					<view class="selected-row">
						<view
							v-for="contact in selectedContacts"
							:key="contact.id"
							class="selected-chip"
							@tap="toggleSelect(contact)"
						>
							<view class="selected-avatar-box">
								<image v-if="contact.avatar" :src="contact.avatar" class="selected-avatar" mode="aspectFill" />
								<text v-else class="selected-letter">{{ contact.letter }}</text>
							</view>
							<text class="selected-name">{{ contact.name }}</text>
						</view>
					</view>
				</scroll-view>
			</view>

			<view v-if="visibleGroups.length > 0" class="group-list">
				<view v-for="letter in visibleGroups" :key="letter" class="group-card">
					<text class="group-title">{{ letter }}</text>
					<view
						v-for="contact in getGroupContacts(letter)"
						:key="contact.id"
						class="contact-row"
						@tap="toggleSelect(contact)"
					>
						<view class="avatar-box">
							<image
								v-if="contact.avatar"
								:src="contact.avatar"
								class="contact-avatar"
								mode="aspectFill"
							/>
							<text v-else class="avatar-letter">{{ contact.letter }}</text>
						</view>
						<text class="contact-name">{{ contact.name }}</text>
						<view class="checkbox" :class="{ checked: contact.selected }">
							<text v-if="contact.selected" class="check-icon">✓</text>
						</view>
					</view>
				</view>
			</view>

			<view v-else class="empty-card">
				<text class="empty-title">没有匹配的联系人</text>
				<text class="empty-desc">换个关键词试试</text>
			</view>
		</scroll-view>
	</view>
</template>

<script>
import { firstLetterFriends } from '@/api/contact.js';
import { createGroup } from '@/api/group.js';
import WebSocketKeeper from '@/components/WebSocketKeeper.vue'

export default {
	components: {
		WebSocketKeeper
	},
	data() {
		return {
			searchText: '',
			indexLetters: [],
			contacts: [],
			loading: false
		}
	},
	onLoad() {
		this.loadFriendList();
	},
	computed: {
		selectedContacts() {
			return this.contacts.filter(c => c.selected)
		},
		selectedCount() {
			return this.selectedContacts.length
		},
		visibleGroups() {
			return this.indexLetters.filter(letter => this.getGroupContacts(letter).length > 0)
		}
	},
	methods: {
		async loadFriendList() {
			if (this.loading) return;

			try {
				this.loading = true;
				const res = await firstLetterFriends();

				if (res && res.code === 200 && res.data && Array.isArray(res.data)) {
					const allContacts = [];
					const letters = [];

					res.data.forEach(group => {
						const letter = group.letter || '#';
						letters.push(letter);

						if (Array.isArray(group.list)) {
							group.list.forEach(contact => {
								allContacts.push({
									id: contact.userId || contact.id,
									name: contact.username || contact.name || '未知',
									avatar: contact.avatar || '',
									letter: contact.username ? contact.username.charAt(0).toUpperCase() : '#',
									group: letter,
									selected: false
								});
							});
						}
					});

					this.contacts = allContacts;
					this.indexLetters = letters;
				} else {
					this.contacts = [];
					this.indexLetters = [];
				}
			} catch (error) {
				console.error('获取好友列表失败:', error);
				this.contacts = [];
				this.indexLetters = [];
			} finally {
				this.loading = false;
			}
		},
		getGroupContacts(letter) {
			const keyword = this.searchText.trim().toLowerCase()
			return this.contacts.filter(c =>
				c.group === letter &&
				(!keyword || String(c.name).toLowerCase().includes(keyword))
			)
		},
		toggleSelect(contact) {
			if (this.selectedCount >= 500 && !contact.selected) {
				uni.showToast({
					title: '最多选择500人',
					icon: 'none'
				})
				return
			}
			contact.selected = !contact.selected
		},
		handleCancel() {
			const pages = getCurrentPages()
			if (pages.length > 1) {
				uni.navigateBack()
			} else {
				uni.redirectTo({ url: '/pages/contacts/contact-list' })
			}
		},
		async handleConfirm() {
			if (this.selectedCount < 2) {
				uni.showToast({
					title: '请至少选择2位联系人',
					icon: 'none'
				})
				return
			}

			try {
				const userInfoStr = uni.getStorageSync('userInfo');
				let currentUserId = null;

				if (userInfoStr) {
					try {
						const userInfo = JSON.parse(userInfoStr);
						currentUserId = userInfo.userId || userInfo.id;
					} catch (e) {
						console.error('解析用户信息失败:', e);
					}
				}

				const selectedMemberIds = this.selectedContacts.map(contact => contact.id);

				const memberIds = currentUserId && !selectedMemberIds.includes(currentUserId)
					? [currentUserId, ...selectedMemberIds]
					: selectedMemberIds;

				const res = await createGroup(memberIds);

				if (res && res.code === 200) {
					uni.showToast({
						title: '创建群聊成功',
						icon: 'success'
					});
					uni.setStorageSync('contactsRefresh', true);

					setTimeout(() => {
						uni.navigateBack();
					}, 1200);
				} else {
					uni.showToast({
						title: res.msg || '创建群聊失败',
						icon: 'none',
						duration: 2000
					});
				}
			} catch (error) {
				console.error('创建群组失败:', error);
				const errorMsg = error.data?.msg || error.msg || '创建群聊失败';
				uni.showToast({
					title: errorMsg,
					icon: 'none',
					duration: 2000
				});
			}
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

.top-btn {
	min-width: 88rpx;
	height: 64rpx;
	display: flex;
	align-items: center;
}

.confirm-btn {
	justify-content: flex-end;
}

.cancel-text {
	font-size: 27rpx;
	color: rgba(60, 60, 67, 0.7);
}

.confirm-text {
	font-size: 27rpx;
	font-weight: 600;
	color: #111111;
}

.confirm-btn.disabled .confirm-text {
	color: rgba(60, 60, 67, 0.3);
}

.page-title {
	font-size: 34rpx;
	font-weight: 600;
	color: #1c1c1e;
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
	margin: 0 24rpx 20rpx;
	padding: 20rpx;
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

.selected-card {
	display: block;
	margin: 0 24rpx 20rpx;
	padding: 20rpx 24rpx;
	background: #ffffff;
	border: 1rpx solid rgba(60, 60, 67, 0.12);
	border-radius: 20rpx;
}

.selected-head {
	display: flex;
	align-items: center;
	justify-content: space-between;
	margin-bottom: 14rpx;
}

.selected-label {
	font-size: 24rpx;
	font-weight: 600;
	color: #1c1c1e;
}

.selected-limit {
	font-size: 21rpx;
	color: rgba(60, 60, 67, 0.4);
}

.selected-scroll {
	width: 100%;
	white-space: nowrap;
}

.selected-row {
	display: flex;
	align-items: center;
	gap: 16rpx;
}

.selected-chip {
	display: inline-flex;
	align-items: center;
	padding: 6rpx 18rpx 6rpx 6rpx;
	background: #f2f2f7;
	border-radius: 999rpx;
	flex-shrink: 0;
}

.selected-avatar-box {
	width: 48rpx;
	height: 48rpx;
	border-radius: 50%;
	background: #111111;
	display: flex;
	align-items: center;
	justify-content: center;
	overflow: hidden;
}

.selected-avatar {
	width: 100%;
	height: 100%;
}

.selected-letter {
	font-size: 22rpx;
	font-weight: 600;
	color: #ffffff;
}

.selected-name {
	margin-left: 10rpx;
	font-size: 22rpx;
	color: #1c1c1e;
}

.group-list {
	display: block;
	margin: 0 24rpx;
}

.group-card {
	display: block;
	margin-bottom: 20rpx;
	background: #ffffff;
	border: 1rpx solid rgba(60, 60, 67, 0.12);
	border-radius: 20rpx;
	overflow: hidden;
}

.group-title {
	display: block;
	padding: 14rpx 24rpx 6rpx;
	font-size: 21rpx;
	font-weight: 600;
	color: rgba(60, 60, 67, 0.45);
}

.contact-row {
	display: flex;
	align-items: center;
	padding: 18rpx 24rpx;
	border-bottom: 1rpx solid rgba(60, 60, 67, 0.1);
}

.contact-row:last-child {
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

.contact-avatar {
	width: 100%;
	height: 100%;
}

.avatar-letter {
	font-size: 30rpx;
	font-weight: 600;
	color: #1c1c1e;
}

.contact-name {
	flex: 1;
	min-width: 0;
	margin-left: 20rpx;
	font-size: 29rpx;
	font-weight: 500;
	color: #1c1c1e;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
}

.checkbox {
	width: 44rpx;
	height: 44rpx;
	border-radius: 50%;
	border: 2rpx solid rgba(60, 60, 67, 0.24);
	display: flex;
	align-items: center;
	justify-content: center;
	box-sizing: border-box;
	flex-shrink: 0;
}

.checkbox.checked {
	background: #111111;
	border-color: #111111;
}

.check-icon {
	font-size: 22rpx;
	line-height: 1;
	color: #ffffff;
}

.empty-card {
	display: block;
	margin: 0 24rpx;
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
	color: rgba(60, 60, 67, 0.5);
}
</style>
