<template>
	<view class="contacts-page">
			<WebSocketKeeper />
		<!-- 顶部导航栏 -->
		<view class="top-bar safe-top-bar">
			<view class="top-bar-content">
				<view class="title-wrapper">
					<text class="page-title">通讯录</text>
				</view>
				<view class="add-btn" @click="handleAddFriend">
					<!-- 空心添加好友图标 -->
					<image class="add-icon" src="/static/icons/user/person-add-outline.svg" mode="aspectFit"></image>
				</view>
			</view>
		</view>

		<!-- 主内容区 -->
		<view class="main-content">
			<!-- 搜索栏 -->
			<view class="search-wrapper">
				<view class="search-box">
					<image class="search-icon" src="/static/icons/search/search.svg" mode="aspectFit"></image>
					<input class="search-input" type="text" placeholder="搜索好友或群聊" placeholder-class="search-placeholder" v-model="searchText" />
					<view v-if="searchText" class="search-clear" @click="clearSearch">
						<text class="search-clear-text">×</text>
					</view>
				</view>
			</view>

			<!-- Tab 切换 -->
			<view class="tab-switcher">
				<view class="tab-container">
					<view class="tab-item" :class="{ 'tab-item-active': activeTab === 'friends' }" @click="switchTab('friends')">
						<text class="tab-text" :class="{ 'tab-text-active': activeTab === 'friends' }">好友({{ friendCount }})</text>
					</view>
					<view class="tab-item" :class="{ 'tab-item-active': activeTab === 'groups' }" @click="switchTab('groups')">
						<text class="tab-text" :class="{ 'tab-text-active': activeTab === 'groups' }">群聊({{ groupCount }})</text>
					</view>
				</view>
			</view>

			<!-- 联系人列表 -->
			<scroll-view class="contact-list" scroll-y="true" :scroll-into-view="scrollIntoView" :scroll-with-animation="true">
				<view class="list-content">
					<!-- 通知入口：原底部通知页签合并到通讯录 -->
					<view v-if="!searchText" class="notice-entry" hover-class="notice-entry-hover" @click="handleNotice">
						<view class="notice-icon-wrap">
							<image class="notice-bell" src="/static/icons/notice/bell.svg" mode="aspectFit"></image>
							<view v-if="noticeCount > 0" class="notice-dot"></view>
						</view>
						<view class="notice-copy">
							<text class="notice-title">通知</text>
							<text class="notice-desc">{{ noticeCount > 0 ? `${noticeCount} 个待处理申请` : '好友申请与系统消息' }}</text>
						</view>
						<image class="notice-arrow" src="/static/icons/arrow/right.svg" mode="aspectFit"></image>
					</view>

					<!-- 搜索结果为空 -->
					<view v-if="searchText && searchResultCount === 0" class="search-empty">
						<image class="search-empty-icon" src="/static/icons/search/search.svg" mode="aspectFit"></image>
						<text class="search-empty-text">没有找到“{{ searchText }}”</text>
					</view>

					<!-- 好友列表 -->
					<FriendList
						v-else-if="activeTab === 'friends'"
						:contactGroups="filteredContactGroups"
						:loading="loading"
						@contact-click="handleContactClick"
						@add-friend="handleAddFriend"
					/>

					<!-- 群聊列表 -->
					<GroupList
						v-else-if="activeTab === 'groups'"
						:groupList="filteredGroupList"
						@group-click="handleGroupClick"
						@create-group="handleCreateGroup"
					/>
				</view>
			</scroll-view>

			<!-- 右侧字母索引（仅好友列表显示） -->
			<view v-if="activeTab === 'friends' && visibleIndexLetters.length > 0" class="index-bar">
				<view v-for="(letter, index) in visibleIndexLetters" :key="index" class="index-item" :class="{ 'index-active': activeLetter === letter }" @click="scrollToLetter(letter)">
					<text class="index-text">{{ letter }}</text>
				</view>
			</view>
		</view>

		<!-- 底部导航栏 -->
		<BottomNav :current="currentPath" @change="handleNavChange" />
	</view>
</template>

<script>
import BottomNav from '@/components/BottomNav.vue';
import FriendList from '@/components/contacts/FriendList.vue';
import GroupList from '@/components/contacts/GroupList.vue';
import { firstLetterFriends, getGroupList, getContactCount, getReceiveFriendRequestList } from '@/api/contact.js';
import WebSocketKeeper from '@/components/WebSocketKeeper.vue'

export default {
	components: {
		WebSocketKeeper,
		BottomNav,
		FriendList,
		GroupList
	},
	data() {
		return {
			currentPath: '/pages/contacts/contact-list',
			activeTab: 'friends',
			searchText: '',
			activeLetter: '',
			scrollIntoView: '',
			indexLetters: [],
			loading: false,
			errorMsg: '',
			contactGroups: [],
			groupList: [],
			friendCount: 0,
			groupCount: 0,
			noticeCount: 0
		};
	},
	computed: {
		filteredContactGroups() {
			const keyword = (this.searchText || '').trim().toLowerCase()
			if (!keyword) return this.contactGroups
			return this.contactGroups
				.map(group => ({
					...group,
					contacts: group.contacts.filter(contact =>
						String(contact.name || '').toLowerCase().includes(keyword)
					)
				}))
				.filter(group => group.contacts.length > 0)
		},
		filteredGroupList() {
			const keyword = (this.searchText || '').trim().toLowerCase()
			if (!keyword) return this.groupList
			return this.groupList.filter(group =>
				String(group.name || '').toLowerCase().includes(keyword)
			)
		},
		visibleIndexLetters() {
			return this.filteredContactGroups.map(group => group.letter)
		},
		searchResultCount() {
			return this.activeTab === 'friends'
				? this.filteredContactGroups.reduce((total, group) => total + group.contacts.length, 0)
				: this.filteredGroupList.length
		}
	},
	onLoad() {
		this.loadFriendList();
		this.loadGroupList();
		this.loadContactCount();
		this.loadNoticeCount();
	},
	onShow() {
		this.loadNoticeCount();
		// 从好友详情/群详情返回后按需刷新，避免删除或退群后列表残留
		if (uni.getStorageSync('contactsRefresh')) {
			uni.removeStorageSync('contactsRefresh');
			this.loadFriendList();
			this.loadGroupList();
			this.loadContactCount();
		}
	},
	methods: {
		handleNavChange(path) {
			this.currentPath = path;
		},
		switchTab(tab) {
			this.activeTab = tab;
		},
		async loadFriendList() {
			if (this.loading) return;

			try {
				this.loading = true;
				this.errorMsg = '';
				const res = await firstLetterFriends();

				if (res && res.code === 200 && res.data && Array.isArray(res.data)) {
					// 处理接口返回的数据
					this.contactGroups = res.data.map(group => ({
						letter: group.letter || '#',
						contacts: Array.isArray(group.list) ? group.list.map(contact => ({
							id: contact.userId || contact.id,
							name: contact.username || contact.name || contact.nickname || '未知',
							status: contact.isOnline ? '在线' : '离线',
							avatar: contact.avatar || ''
						})) : []
					}));

					// 生成字母索引列表
					this.indexLetters = this.contactGroups.map(group => group.letter);

					// 设置第一个字母为激活状态
					if (this.indexLetters.length > 0) {
						this.activeLetter = this.indexLetters[0];
					}
				} else {
					throw new Error('接口返回数据格式错误');
				}
			} catch (error) {
				console.error('获取好友列表失败:', error);
				this.errorMsg = error.message || '获取好友列表失败';
				this.contactGroups = [];
				this.indexLetters = [];
			} finally {
				this.loading = false;
			}
		},
		async loadGroupList() {
			try {
				const res = await getGroupList();

				if (res && res.code === 200 && res.data && Array.isArray(res.data)) {
					// 处理接口返回的数据
					this.groupList = res.data.map(group => ({
						id: group.id || group.groupId,
						name: group.name || group.groupName || '未知群组',
						memberCount: group.currentMember || group.memberCount || group.memberNum || 0,
						avatar: group.avatar || group.groupAvatar || ''
					}));
				} else {
					console.error('获取群列表失败:', res);
					this.groupList = [];
				}
			} catch (error) {
				console.error('获取群列表失败:', error);
				this.groupList = [];
			}
		},
		async loadContactCount() {
			try {
				const res = await getContactCount();
				if (res && res.code === 200 && res.data) {
					this.friendCount = res.data.friendCount || 0;
					this.groupCount = res.data.groupCount || 0;
				}
			} catch (error) {
				console.error('获取联系人数量失败:', error);
			}
		},
		async loadNoticeCount() {
			try {
				const res = await getReceiveFriendRequestList();
				if (res && res.code === 200 && res.data) {
					const list = Array.isArray(res.data) ? res.data : (res.data.list || []);
					this.noticeCount = list.filter(item => Number(item.flowStatus) === 0).length;
				}
			} catch (error) {
				console.error('获取通知数量失败:', error);
				this.noticeCount = 0;
			}
		},
		handleNotice() {
			uni.navigateTo({
				url: '/pages/notice/notice-list'
			});
		},
		handleAddFriend() {
			uni.navigateTo({
				url: '/pages/contacts/add-friend'
			});
		},
		handleContactClick(contact) {
			uni.navigateTo({
				url: `/pages/contacts/friend-detail?id=${encodeURIComponent(contact.id)}&name=${encodeURIComponent(contact.name)}&avatar=${encodeURIComponent(contact.avatar || '')}`
			});
		},
		handleGroupClick(group) {
			uni.navigateTo({
				url: `/pages/contacts/group-detail?id=${encodeURIComponent(group.id)}&name=${encodeURIComponent(group.name)}&avatar=${encodeURIComponent(group.avatar || '')}`
			});
		},
		clearSearch() {
			this.searchText = '';
		},
		handleCreateGroup() {
			uni.navigateTo({
				url: '/pages/contacts/create-group'
			});
		},
		scrollToLetter(letter) {
			this.activeLetter = letter;

			// 重置 scrollIntoView，确保每次点击都能触发滚动
			this.scrollIntoView = '';

			// 使用 nextTick 确保 DOM 更新后再设置滚动目标
			this.$nextTick(() => {
				this.scrollIntoView = 'group-' + letter;
			});
		}
	}
};
</script>

<style lang="scss" scoped>
/* 页面容器 */
.contacts-page {
	width: 100%;
	height: 100vh;
	min-height: 100vh;
	overflow: hidden;
	background-color: #f2f2f7;
	display: flex;
	flex-direction: column;
}

/* 顶部导航栏 */
.top-bar {
	position: fixed;
	top: 0;
	left: 0;
	right: 0;
	z-index: 50;
	background: rgba(255, 255, 255, 0.8);
	backdrop-filter: blur(40rpx);
	-webkit-backdrop-filter: blur(40rpx);
	border-bottom: 1rpx solid rgba(60, 60, 67, 0.12);
}

.top-bar-content {
	display: flex;
	justify-content: space-between;
	align-items: center;
	padding: 32rpx 48rpx;
}

.title-wrapper {
	display: flex;
	align-items: center;
}

.page-title {
	font-family: 'Manrope', 'Plus Jakarta Sans', -apple-system, BlinkMacSystemFont, sans-serif;
	font-size: 38rpx;
	font-weight: 700;
	line-height: 1.2;
	letter-spacing: -0.02em;
	color: #1c1c1e;
}

.add-btn {
	width: 80rpx;
	height: 80rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	color: rgba(30, 27, 19, 0.4);
	transition: all 0.2s;
}

.add-btn:active {
	transform: scale(0.95);
}

.add-icon {
	width: 56rpx;
	height: 56rpx;
}

/* 主内容区 */
.main-content {
	flex: 1;
	display: flex;
	flex-direction: column;
	/* 顶部留出导航栏空间，底部留出底部导航空间 */
	padding-top: calc(var(--status-bar-height) + 176rpx);
	padding-bottom: 128rpx;
	padding-left: 32rpx;
	padding-right: 32rpx;
	position: relative;
	overflow: hidden;
}

/* 通知入口 */
.notice-entry {
	display: flex;
	align-items: center;
	background: #ffffff;
	border: 1rpx solid rgba(60, 60, 67, 0.12);
	border-radius: 18rpx;
	padding: 24rpx;
	margin-bottom: 20rpx;
}

.notice-entry-hover {
	background: rgba(60, 60, 67, 0.04);
}

.notice-icon-wrap {
	position: relative;
	width: 64rpx;
	height: 64rpx;
	border-radius: 14rpx;
	background: #f2f2f7;
	display: flex;
	align-items: center;
	justify-content: center;
	flex-shrink: 0;
	margin-right: 20rpx;
}

.notice-bell {
	width: 38rpx;
	height: 38rpx;
	opacity: 0.82;
}

.notice-dot {
	position: absolute;
	top: -4rpx;
	right: -4rpx;
	width: 16rpx;
	height: 16rpx;
	background: #d92d20;
	border-radius: 50%;
	border: 3rpx solid #ffffff;
}

.notice-copy {
	flex: 1;
	display: flex;
	flex-direction: column;
}

.notice-title {
	font-size: 29rpx;
	font-weight: 600;
	color: #1c1c1e;
	line-height: 1.3;
}

.notice-desc {
	font-size: 23rpx;
	color: rgba(60, 60, 67, 0.55);
	line-height: 1.4;
	margin-top: 2rpx;
}

.notice-arrow {
	width: 28rpx;
	height: 28rpx;
	opacity: 0.35;
	flex-shrink: 0;
}

/* 搜索栏 */
.search-wrapper {
	margin-bottom: 24rpx;
}

.search-box {
	position: relative;
	display: flex;
	align-items: center;
	height: 72rpx;
	background-color: rgba(120, 120, 128, 0.12);
	border-radius: 18rpx;
	padding: 0 72rpx 0 24rpx;
	transition: all 0.2s;
}

.search-icon {
	width: 34rpx;
	height: 34rpx;
	margin-right: 20rpx;
	opacity: 0.45;
	flex-shrink: 0;
}

.search-input {
	flex: 1;
	height: 100%;
	font-size: 27rpx;
	color: #1c1c1e;
	background: transparent;
	border: none;
	outline: none;
}

.search-placeholder {
	color: rgba(60, 60, 67, 0.38);
}

.search-clear {
	position: absolute;
	right: 16rpx;
	width: 40rpx;
	height: 40rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	border-radius: 50%;
	background-color: rgba(60, 60, 67, 0.18);
}

.search-clear-text {
	font-size: 26rpx;
	line-height: 1;
	color: #ffffff;
}

/* Tab 切换 */
.tab-switcher {
	margin-bottom: 28rpx;
}

.tab-container {
	display: flex;
	padding: 6rpx;
	background-color: rgba(120, 120, 128, 0.12);
	border-radius: 18rpx;
}

.tab-item {
	flex: 1;
	display: flex;
	align-items: center;
	justify-content: center;
	padding: 14rpx 0;
	border-radius: 14rpx;
	transition: all 0.2s;
}

.tab-item-active {
	background-color: #ffffff;
	box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.08);
}

.tab-text {
	font-size: 26rpx;
	font-weight: 500;
	color: rgba(60, 60, 67, 0.6);
	transition: all 0.2s;
}

.tab-text-active {
	font-weight: 600;
	color: #1c1c1e;
}

/* 搜索结果为空 */
.search-empty {
	display: flex;
	flex-direction: column;
	align-items: center;
	justify-content: center;
	padding: 120rpx 32rpx;
	gap: 20rpx;
}

.search-empty-icon {
	width: 88rpx;
	height: 88rpx;
	opacity: 0.25;
}

.search-empty-text {
	font-size: 27rpx;
	color: rgba(60, 60, 67, 0.6);
}

/* 联系人列表 */
.contact-list {
	flex: 1;
	min-height: 0;
	overflow: hidden;
}

.list-content {
	padding-bottom: 32rpx;
}

/* 右侧字母索引 */
.index-bar {
	position: fixed;
	right: 16rpx;
	top: 50%;
	transform: translateY(-50%);
	display: flex;
	flex-direction: column;
	align-items: center;
	gap: 12rpx;
	z-index: 40;
}

.index-item {
	padding: 4rpx 8rpx;
	transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.index-item:active {
	transform: scale(1.3);
}

.index-text {
	font-size: 20rpx;
	font-weight: 700;
	color: rgba(30, 27, 19, 0.3);
	transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.index-active .index-text {
	color: #111111;
	font-size: 24rpx;
	text-shadow: 0 2rpx 8rpx rgba(17, 17, 17, 0.3);
}

.index-active {
	background-color: rgba(17, 17, 17, 0.1);
	border-radius: 8rpx;
}

</style>
