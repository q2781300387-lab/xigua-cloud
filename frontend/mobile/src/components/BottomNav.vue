<template>
		<view class="bottom-nav">
			<view
				v-for="(item, index) in navItems"
				:key="index"
				class="nav-item"
				:class="{ 'nav-item-active': currentPath === item.path }"
				hover-class="nav-item-hover"
				@click="switchTab(item)"
			>
				<view class="nav-icon-wrapper">
					<image class="nav-icon" :src="currentPath === item.path ? item.activeIcon : item.icon" mode="aspectFit"></image>

					<!-- 聊天未读消息数量徽章 -->
					<view v-if="item.key === 'chat' && displayUnreadCount > 0" class="nav-unread-badge">
						<text class="nav-unread-text">{{ displayUnreadCount > 99 ? '99+' : displayUnreadCount }}</text>
					</view>

					<!-- 好友申请红点：通知已合并到通讯录，红点跟随通讯录页签 -->
					<view v-else-if="item.key === 'contacts' && noticeCount > 0" class="nav-badge"></view>
				</view>
				<text class="nav-label" :class="{ 'nav-label-active': currentPath === item.path }">{{ item.label }}</text>
			</view>
		</view>
	</template>

	<script>
	import { getUnreadInfo } from '@/api/chat.js';
	import { getReceiveFriendRequestList } from '@/api/contact.js';

	export default {
		name: 'BottomNav',
		props: {
			current: {
				type: String,
				default: '/pages/chat/chat-list'
			},
			unreadCount: {
				type: Number,
				default: 0
			}
		},
		data() {
			return {
				currentPath: '',
				internalUnreadCount: 0,
				noticeCount: 0,
				navItems: [
					{
						key: 'chat',
						label: '聊天',
						path: '/pages/chat/chat-list',
						icon: '/static/icons/tabbar/chat.svg',
						activeIcon: '/static/icons/tabbar/chat-active.svg',
						badge: false
					},
					{
						key: 'contacts',
						label: '通讯录',
						path: '/pages/contacts/contact-list',
						icon: '/static/icons/tabbar/contacts.svg',
						activeIcon: '/static/icons/tabbar/contacts-active.svg',
						badge: false
					},
					{
						key: 'discover',
						label: '发现',
						path: '/pages/discover/discover',
						icon: '/static/icons/tabbar/discover.svg',
						activeIcon: '/static/icons/tabbar/discover-active.svg',
						badge: false
					},
					{
						key: 'profile',
						label: '我',
						path: '/pages/profile/profile',
						icon: '/static/icons/tabbar/profile.svg',
						activeIcon: '/static/icons/tabbar/profile-active.svg',
						badge: false
					}
				]
			};
		},
		computed: {
			// 优先使用父组件传递的值，否则使用组件内部维护的值
			displayUnreadCount() {
				return this.unreadCount || this.internalUnreadCount;
			}
		},
		created() {
			this.currentPath = this.current;
			// 组件创建时加载未读数量
			this.loadUnreadCount();
			this.loadNoticeCount();
		},
		watch: {
			current(newVal) {
				this.currentPath = newVal;
				// 路径变化时刷新未读数量
				this.loadUnreadCount();
				this.loadNoticeCount();
			}
		},
		methods: {
			/**
			 * 加载未读消息数量
			 */
			async loadUnreadCount() {
				try {
					const res = await getUnreadInfo();
					if (res.code === 200 && res.data) {
						this.internalUnreadCount = res.data.chatUnreadCount || 0;
					}
				} catch (error) {
					console.error('BottomNav 获取未读消息数量失败:', error);
				}
			},
			/**
			 * 加载待处理好友申请数量，决定通知页签红点
			 */
			async loadNoticeCount() {
				try {
					const res = await getReceiveFriendRequestList();
					if (res && res.code === 200 && res.data) {
						const list = Array.isArray(res.data) ? res.data : (res.data.list || []);
						this.noticeCount = list.filter(item => Number(item.flowStatus) === 0).length;
					}
				} catch (error) {
					console.error('BottomNav 获取好友申请数量失败:', error);
					this.noticeCount = 0;
				}
			},
			switchTab(item) {
				if (this.currentPath === item.path) {
					return;
				}

				this.currentPath = item.path;

				// 触发父组件事件
				this.$emit('change', item.path);

				// 页面跳转
				uni.redirectTo({
					url: item.path,
					fail: (err) => {
						console.error('页面跳转失败:', err);
					}
				});
			}
		}
	};
	</script>

	<style lang="scss" scoped>
	/* 底部导航栏 */
	.bottom-nav {
		position: fixed;
		bottom: 0;
		left: 0;
		right: 0;
		display: flex;
		justify-content: space-around;
		align-items: center;
		padding: 10rpx 24rpx calc(14rpx + env(safe-area-inset-bottom));
		background-color: rgba(255, 255, 255, 0.94);
		border-top: 1rpx solid rgba(60, 60, 67, 0.12);
		border-radius: 0;
		box-shadow: 0 -2rpx 16rpx rgba(0, 0, 0, 0.04);
		backdrop-filter: saturate(180%) blur(20px);
		-webkit-backdrop-filter: saturate(180%) blur(20px);
		z-index: 50;
	}

	.nav-item {
		display: flex;
		flex-direction: column;
		align-items: center;
		justify-content: center;
		padding: 8rpx 24rpx;
		transition: all 0.3s;
		border-radius: 18rpx;
	}

	.nav-item-hover {
		background-color: rgba(245, 237, 223, 0.5);
	}

	.nav-item-active {
		// 激活状态样式由图标切换控制
	}

	.nav-icon-wrapper {
		position: relative;
		margin-bottom: 4rpx;
		width: 52rpx;
		height: 52rpx;
		display: flex;
		align-items: center;
		justify-content: center;
	}

	.nav-icon {
		width: 52rpx;
		height: 52rpx;
	}

	.nav-badge {
		position: absolute;
		top: -4rpx;
		right: -4rpx;
		width: 16rpx;
		height: 16rpx;
		background-color: #d92d20;
		border-radius: 50%;
		border: 3rpx solid #ffffff;
	}

	/* 聊天未读消息数量徽章 */
	.nav-unread-badge {
		position: absolute;
		top: -6rpx;
		right: -10rpx;
		display: flex;
		align-items: center;
		justify-content: center;
		min-width: 30rpx;
		height: 30rpx;
		background-color: #111111;
		border-radius: 50%;
		border: 3rpx solid #ffffff;
		padding: 0 5rpx;
	}

	.nav-unread-text {
		font-family: 'Plus Jakarta Sans', -apple-system, BlinkMacSystemFont, sans-serif;
		font-size: 17rpx;
		font-weight: 700;
		color: #ffffff;
		line-height: 1;
	}

	.nav-label {
		font-size: 24rpx;
		font-weight: 600;
		color: rgba(30, 27, 19, 0.4);
		font-family: 'Plus Jakarta Sans', -apple-system, BlinkMacSystemFont, sans-serif;
		transition: color 0.3s;
	}

	.nav-label-active {
		color: #111111;
	}
	</style>
