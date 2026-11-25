<template>
	<view class="chat-list-container" @tap="handleContainerTap">
			<!-- WebSocket 连接保持组件 -->
			<WebSocketKeeper />
		<!-- 弹窗菜单 -->
		<view
			v-if="showPopup"
			class="popup-menu"
			:class="{ 'popup-menu-show': showPopup }"
			@tap.stop
		>
			<!-- 三角箭头 -->
			<view class="popup-arrow"></view>

			<!-- 添加好友 -->
			<view class="popup-item" hover-class="popup-item-hover" @tap="goToAddFriend">
				<image class="popup-icon" src="/static/icons/user/person-add.svg" mode="aspectFit"></image>
				<text class="popup-text">添加好友</text>
			</view>

			<!-- 分隔线 -->
			<view class="popup-divider"></view>

			<!-- 发起群聊 -->
			<view class="popup-item" hover-class="popup-item-hover" @tap="goToCreateGroup">
				<image class="popup-icon" src="/static/icons/user/group.svg" mode="aspectFit"></image>
				<text class="popup-text">发起群聊</text>
			</view>
		</view>

		<!-- 顶部导航栏 -->
		<view class="top-bar safe-top-bar">
			<view class="top-bar-content">
				<!-- 顶部第一行 -->
				<view class="header-row">
					<!-- 左侧：头像 + 标题 -->
					<view class="left-section">
						<view class="avatar-wrapper" @click="goToProfile">
							<image
								v-if="userInfo && userInfo.avatar"
								class="avatar"
								:src="userInfo.avatar"
								mode="aspectFill"
							/>
							<view v-else class="avatar-placeholder">
								<image class="avatar-icon" src="/static/icons/user/avatar.svg" mode="aspectFit"></image>
							</view>
						</view>
						<text class="header-title">消息</text>
						<!-- 未读消息总数徽章 -->
						<view v-if="totalUnreadCount > 0" class="total-unread-badge">
							<text class="total-unread-text">{{ totalUnreadCount > 99 ? '99+' : totalUnreadCount }}</text>
						</view>
					</view>

					<!-- 中间：WebSocket 连接状态指示器 -->
					<view class="status-indicator">
						<view class="status-dot" :class="getWsStatusClass()"></view>
						<text class="status-text">{{ getWsStatusText() }}</text>
					</view>

					<!-- 右侧：通话记录 + 按钮 -->
					<view class="right-section">
						<view class="calls-btn" @tap.stop="goToCallRecords" hover-class="add-btn-hover" title="通话记录">
							<image class="icon-add" src="/static/icons/chat/calls.svg" mode="aspectFit"></image>
							<view v-if="callUnread > 0" class="call-unread-badge">
								<text class="call-unread-text">{{ callUnread > 99 ? '99+' : callUnread }}</text>
							</view>
						</view>
						<view class="add-btn" @tap.stop="togglePopup" hover-class="add-btn-hover">
							<image class="icon-add" src="/static/icons/chat/plus.svg" mode="aspectFit"></image>
						</view>
					</view>
				</view>

				<!-- 搜索框 -->
				<view class="search-bar">
					<image class="search-icon" src="/static/icons/search/search.svg" mode="aspectFit"></image>
					<input
						class="search-input"
						v-model="searchKeyword"
						placeholder="搜索聊天记录、联系人"
						placeholder-class="search-placeholder"
						@focus="handleSearchFocus"
					/>
					<view v-if="searchKeyword" class="search-clear" @tap.stop="clearSearch">
						<text class="search-clear-text">×</text>
					</view>
				</view>
			</view>
		</view>

		<!-- 主要内容区域 -->
		<scroll-view
			class="main-content"
			scroll-y
			refresher-enabled
			:refresher-triggered="refreshing"
			@refresherrefresh="onRefresh"
			@scrolltolower="loadMore"
			:lower-threshold="200"
		>
			<!-- 空状态 -->
			<view v-if="filteredChatList.length === 0" class="empty-state">
				<image class="empty-icon" src="/static/icons/chat/bubble.svg" mode="aspectFit"></image>
				<text class="empty-text">{{ chatList.length === 0 ? '暂无聊天消息' : '没有找到相关聊天' }}</text>
			</view>

			<!-- 聊天列表 -->
			<view v-else class="chat-list">
				<view
					v-for="(chat, index) in filteredChatList"
					:key="index"
					class="chat-card"
					hover-class="chat-card-hover"
					@click="openChat(chat)"
				>
					<!-- 头像 -->
					<view class="chat-avatar-wrapper">
						<view class="chat-avatar-box">
							<text v-if="!isPlaceholderChatName(chat.name)" class="chat-avatar-letter">{{ avatarLetter(chat.name) }}</text>
							<image
								v-else
								class="chat-avatar-fallback"
								:src="fallbackAvatar(chat)"
								mode="aspectFit"
							/>
							<image
								v-if="showAvatar(chat)"
								class="chat-avatar"
								:src="chat.avatar"
								mode="aspectFill"
								@error="handleAvatarError(chat)"
							/>
						</view>
						<!-- 在线状态指示器 -->
						<view v-if="chat.online" class="online-indicator"></view>
					</view>

					<!-- 聊天内容 -->
					<view class="chat-content">
						<view class="chat-header">
							<view class="chat-name-row">
								<text class="chat-name" :class="{ 'chat-name-muted': chat.muted }">{{ chat.name }}</text>
								<text v-if="chat.pinned" class="chat-tag">置顶</text>
								<text v-if="chat.muted" class="chat-tag chat-tag-mute">静音</text>
								<text v-if="hasAtMe(chat)" class="chat-tag chat-tag-at">@我</text>
							</view>
							<text class="chat-time">{{ chat.time }}</text>
						</view>
						<view class="chat-footer">
							<text class="chat-message" :class="{ 'chat-message-muted': chat.muted }">{{ chat.draft ? '[草稿] ' + chat.draft : chat.lastMessage }}</text>
							<view v-if="chat.unreadCount" class="unread-badge">
								<text class="unread-text">{{ chat.unreadCount }}</text>
							</view>
						</view>
					</view>
				</view>
			</view>

			<!-- 加载更多提示 -->
			<view v-if="loading && pageNum > 1" class="loading-more">
				<text class="loading-more-text">加载中...</text>
			</view>
			<view v-if="!hasMore && chatList.length > 0 && !searchKeyword" class="no-more">
				<text class="no-more-text">没有更多了</text>
			</view>
		</scroll-view>

		<!-- 底部导航栏组件 -->
		<BottomNav :current="currentPath" :unreadCount="totalUnreadCount" @change="handleNavChange" />
	</view>
</template>

<script>
import BottomNav from '@/components/BottomNav.vue';
import { getLastMes, getUnreadInfo, getCallUnread, getAtMeUnread } from '@/api/chat.js';
import { getWebSocketClient, subscribeMessage, unsubscribeMessage } from '@/utils/websocket.js';
	import WebSocketKeeper from '@/components/WebSocketKeeper.vue';
import { loadChatPrefs, isMuted, isPinned } from '@/utils/chatPrefs.js';

export default {
	components: {
		BottomNav,
		WebSocketKeeper
	},
	onLoad() {
		this.loadUserInfo();
		this.initChat();
	},
	data() {
		return {
			userInfo: null,
			searchKeyword: '',
			currentPath: '/pages/chat/chat-list',
			chatList: [],
			refreshing: false,
			wsStatusKey: 0,
			wsStatusTimer: null,
			showPopup: false, // 控制弹窗显示
			// 分页相关
			pageNum: 1,
			pageSize: 20,
			hasMore: true,
			loading: false,
			loadedPages: new Set(),
			// 未读消息总数
			totalUnreadCount: 0,
			chatPrefs: {},
			// 未接来电角标（与 PC 共用服务端已读水位）
			callUnread: 0,
			callUnreadTimer: null,
			// 未读且 @ 我的会话 id，用于列表里的「@我」提示
			atMeChatIds: []
		};
	},
	computed: {
		filteredChatList() {
			const keyword = (this.searchKeyword || '').trim().toLowerCase()
			const list = keyword
				? this.chatList.filter(chat => {
					const name = String(chat.name || '').toLowerCase()
					const lastMessage = String(chat.lastMessage || '').toLowerCase()
					return name.includes(keyword) || lastMessage.includes(keyword)
				})
				: this.chatList
			// 置顶会话排前，其余保持后端返回顺序
			return list.slice().sort((a, b) => (b.pinned ? 1 : 0) - (a.pinned ? 1 : 0))
		}
	},
	onShow() {
		// 重新读取会话置顶/免打扰偏好，群详情里改过设置后返回即生效
		this.chatPrefs = loadChatPrefs();
		this.applyChatPrefs();
		// 刷新聊天列表，获取最新消息
		this.pageNum = 1;
		this.loadLastMessages();
		// 加载未读消息数量
		this.loadUnreadCount();
		// @我 未读会话
		this.loadAtMeUnread();
		// 未接来电角标：进入页面即查 + 30 秒轮询，PC 端查看后同步消失
		this.loadCallUnread();
		if (this.callUnreadTimer) clearInterval(this.callUnreadTimer);
		this.callUnreadTimer = setInterval(() => this.loadCallUnread(), 30000);
		// 启动定时更新 WebSocket 状态
		this.startWsStatusTimer();
		// 订阅 WebSocket 消息
		subscribeMessage(this.handleWebSocketMessage);
	},
	onHide() {
		// 停止定时更新
		this.stopWsStatusTimer();
		if (this.callUnreadTimer) {
			clearInterval(this.callUnreadTimer);
			this.callUnreadTimer = null;
		}
		// 取消订阅 WebSocket 消息
		unsubscribeMessage(this.handleWebSocketMessage);
		this.closePopup(); // 隐藏时关闭弹窗
	},
	onUnload() {
		// 停止定时更新
		this.stopWsStatusTimer();
		if (this.callUnreadTimer) {
			clearInterval(this.callUnreadTimer);
			this.callUnreadTimer = null;
		}
		// 取消订阅 WebSocket 消息
		unsubscribeMessage(this.handleWebSocketMessage);
	},
	methods: {
		applyChatPrefs() {
			this.chatList.forEach(chat => {
				chat.muted = isMuted(this.chatPrefs, chat.id)
				chat.pinned = isPinned(this.chatPrefs, chat.id)
			})
		},
		// 与 PC 端一致：后端查不到群/用户时会回「未知会话」「?」，不能原样丢给用户
		isPlaceholderChatName(name) {
			const value = String(name == null ? '' : name).trim();
			return value === '' || value === '?' || value === '未知会话' || value === '未命名会话' || value === '未命名群聊';
		},
		formatChatName(name, chatType) {
			if (this.isPlaceholderChatName(name)) {
				return Number(chatType) === 2 ? '未命名群聊' : '未命名会话';
			}
			return String(name).trim();
		},
		fallbackAvatar(chat) {
			const type = Number(chat && chat.chatType);
			return type === 2
				? '/static/icons/user/group.svg'
				: '/static/icons/user/avatar.svg';
		},
		avatarLetter(name) {
			if (!name) return '?';
			const first = String(name).replace(/^[\s、]+/, '').charAt(0);
			return first || '?';
		},
		/**
		 * 把最后一条消息转成列表里的简短预览，避免语音/图片的原始 JSON 直接露出
		 */
		formatLastMessage(content, type) {
			const raw = String(content == null ? '' : content).trim();
			if (!raw) return '';
			const asImage = /\.(png|jpe?g|gif|webp|bmp|svg)(\?|$)/i;
			const asVoice = /\.(webm|mp3|wav|m4a|aac|ogg)(\?|$)/i;

			if (type === 'voice') return '[语音]';
			if (type === 'image') return '[图片]';
			if (type === 'video') return '[视频]';
			if (type === 'file') return '[文件]';
			if (String(type) === '7') return '[聊天记录]';
			if (String(type) === '9') {
				const outcome = raw.indexOf('completed') >= 0 ? '通话结束' :
					raw.indexOf('rejected') >= 0 ? '未接听' :
					raw.indexOf('canceled') >= 0 ? '已取消' : '未接通';
				return `[视频通话] ${outcome}`;
			}
			if (String(type) === '8') {
				const title = raw.replace(/^【投票】/, '').split('||')[0];
				return title ? `[群投票] ${title}` : '[群投票]';
			}
			if (raw.indexOf('【投票】') === 0) {
				const title = raw.replace(/^【投票】/, '').split('||')[0];
				return title ? `[群投票] ${title}` : '[群投票]';
			}

			if (raw.charAt(0) === '{') {
				try {
					const payload = JSON.parse(raw);
					if (payload && payload.kind === 'video') {
						const outcome = payload.outcome === 'completed' ? '通话结束' :
							(payload.outcome === 'rejected' || payload.outcome === 'timeout') ? '未接听' :
							payload.outcome === 'busy' ? '忙线未接' :
							payload.outcome === 'canceled' ? '已取消' : '未接通';
						return `[视频通话] ${outcome}`;
					}
					if (payload && (payload.duration !== undefined || asVoice.test(payload.url || ''))) {
						const seconds = Math.max(1, Number(payload.duration) || 1);
						return `[语音] ${seconds}″`;
					}
					if (payload && payload.url) return asImage.test(payload.url) ? '[图片]' : '[文件]';
					if (payload && payload.transcript) return payload.transcript;
					if (payload && payload.content) return String(payload.content);
					return '[消息]';
				} catch (e) {
					return '[消息]';
				}
			}

			if (raw.indexOf('【聊天记录】') === 0) return '[聊天记录]';
			if (asVoice.test(raw)) return '[语音]';
			if (asImage.test(raw)) return '[图片]';
			return raw;
		},
		handleAvatarError(chat) {
			chat.avatar = '';
		},
		showAvatar(chat) {
			const url = chat && chat.avatar;
			return !!url && !String(url).includes('default-avatar');
		},
		loadUserInfo() {
			const userInfoStr = uni.getStorageSync('userInfo');
			if (userInfoStr) {
				try {
					this.userInfo = JSON.parse(userInfoStr);
				} catch (e) {
					console.error('解析用户信息失败:', e);
					this.userInfo = null;
				}
			}
		},
		/**
		 * 初始化聊天页面
		 */
		async initChat() {
			// 只读取 openChat（跨页面传参），忽略 currentChat 的自动打开
			// currentChat 只用于传递数据到详情页，不应该自动打开聊天
			let openChat = {};
			const openChatStr = uni.getStorageSync('openChat');
			if (openChatStr) {
				try {
					openChat = JSON.parse(openChatStr);
				} catch (e) {
					console.error('解析 openChat 失败:', e);
				}
				// 读取后立即删除
				uni.removeStorageSync('openChat');
			}

			// 重置分页状态
			this.pageNum = 1;
			this.hasMore = true;
			this.loadedPages = new Set();

			// 获取最近聊天列表
			await this.loadLastMessages();

			// 只有在 openChat 存在时才自动打开聊天（从其他页面跳转过来）
			if (openChat.chatId != null) {
				await this.openChatFromInit(openChat.chatId);
			}
		},
		/**
		 * 获取初始化参数
		 * 优先级：openChat（跨页面传参） > currentChatWindow（缓存）
		 */
		getInitChatParams() {
			// 优先从 localStorage 读取跨页面传参
			let openChat = {};
			const openChatStr = uni.getStorageSync('openChat');
			if (openChatStr) {
				try {
					openChat = JSON.parse(openChatStr);
				} catch (e) {
					console.error('解析 openChat 失败:', e);
				}
				// 读取后立即删除
				uni.removeStorageSync('openChat');
			}

			// 其次从 localStorage 读取缓存的聊天窗口
			let currentChatWindow = {};
			const currentChatStr = uni.getStorageSync('currentChat');
			if (currentChatStr) {
				try {
					currentChatWindow = JSON.parse(currentChatStr);
				} catch (e) {
					console.error('解析 currentChat 失败:', e);
				}
			}

			// 确定最终参数
			const chatId = openChat.chatId ?? currentChatWindow.chatId ?? null;
			const rank = openChat.rank ?? 0;

			console.log('初始化参数:', { chatId, rank, openChat, currentChatWindow });

			return { chatId, rank };
		},
		/**
		 * 从初始化参数打开聊天
		 */
		async openChatFromInit(chatId) {
			console.log('从初始化参数打开聊天:', chatId);

			// 查找对应的聊天项
			const chat = this.chatList.find(c => c.id === chatId);
			if (chat) {
				// 打开聊天窗口
				this.openChat(chat);
			} else {
				console.warn('未找到聊天项:', chatId);
			}
		},
		async loadLastMessages() {
			// 防止重复加载
			if (this.loading) return;
			this.loading = true;

			try {
				const res = await getLastMes({
					pageNum: this.pageNum,
					pageSize: this.pageSize
				});
				console.log('最后一条消息:', res);

				if (res.code === 200 && res.data && res.data.rows) {
					if (this.pageNum === 1) {
						// 第一页直接替换
						this.chatList = res.data.rows.map(item => ({
							id: item.chatId,
							name: this.formatChatName(item.chatName, item.chatType),
							avatar: item.avatar,
							lastMessage: this.formatLastMessage(item.lastMessageContent?.content, item.lastMessageContent?.type),
							time: this.formatTime(item.updateTime),
							unreadCount: item.unreadCount,
							online: item.isOnline,
							muted: isMuted(this.chatPrefs, item.chatId),
							pinned: isPinned(this.chatPrefs, item.chatId) || !!item.pinned,
							chatType: item.chatType,
							draft: item.draft || ''
						}));
					} else {
						// 后续页追加
						const newItems = res.data.rows.map(item => ({
							id: item.chatId,
							name: this.formatChatName(item.chatName, item.chatType),
							avatar: item.avatar,
							lastMessage: this.formatLastMessage(item.lastMessageContent?.content, item.lastMessageContent?.type),
							time: this.formatTime(item.updateTime),
							unreadCount: item.unreadCount,
							online: item.isOnline,
							muted: isMuted(this.chatPrefs, item.chatId),
							pinned: isPinned(this.chatPrefs, item.chatId) || !!item.pinned,
							chatType: item.chatType,
							draft: item.draft || ''
						}));
						this.chatList = [...this.chatList, ...newItems];
					}

					// 更新分页状态
					this.hasMore = this.pageNum < res.data.totalPage;
					this.loadedPages.add(this.pageNum);
					this.applyChatPrefs();

					console.log('获取最后消息成功，聊天列表已更新:', this.chatList);
				}
			} catch (error) {
				console.error('获取最后消息失败:', error);
			} finally {
				this.loading = false;
			}
		},
		/**
		 * 滚动加载更多
		 */
		loadMore() {
			if (!this.hasMore || this.loading) return;
			this.pageNum++;
			this.loadLastMessages();
		},

		/**
		 * 加载未读消息数量
		 */
		async loadUnreadCount() {
			try {
				const res = await getUnreadInfo();
				console.log('未读消息数量:', res);

				if (res.code === 200 && res.data) {
					this.totalUnreadCount = res.data.chatUnreadCount || 0;
				}
			} catch (error) {
				console.error('获取未读消息数量失败:', error);
			}
		},
		async loadCallUnread() {
			try {
				const res = await getCallUnread();
				if (res && res.code === 200) {
					this.callUnread = Math.max(0, Number(res.data) || 0);
				}
			} catch (error) {
				// 接口失败不阻塞消息列表
			}
		},
		formatTime(timestamp) {
			if (!timestamp) return '';

			const now = new Date();
			const date = new Date(timestamp);
			const diff = now - date;

			// 一分钟内
			if (diff < 60000) {
				return '刚刚';
			}

			// 一小时内
			if (diff < 3600000) {
				return `${Math.floor(diff / 60000)}分钟前`;
			}

			// 今天
			if (now.toDateString() === date.toDateString()) {
				const hours = date.getHours().toString().padStart(2, '0');
				const minutes = date.getMinutes().toString().padStart(2, '0');
				return `${hours}:${minutes}`;
			}

			// 昨天
			const yesterday = new Date(now);
			yesterday.setDate(yesterday.getDate() - 1);
			if (yesterday.toDateString() === date.toDateString()) {
				return '昨天';
			}

			// 一周内
			if (diff < 7 * 24 * 3600000) {
				const weekdays = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六'];
				return weekdays[date.getDay()];
			}

			// 更早
			const month = (date.getMonth() + 1).toString().padStart(2, '0');
			const day = date.getDate().toString().padStart(2, '0');
			return `${month}-${day}`;
		},
		goToProfile() {
			uni.navigateTo({
				url: '/pages/profile/profile'
			});
		},
		// 切换弹窗显示/隐藏
		togglePopup() {
			this.showPopup = !this.showPopup;
		},
		// 关闭弹窗
		closePopup() {
			this.showPopup = false;
		},
		// 点击容器时关闭弹窗
		handleContainerTap() {
			if (this.showPopup) {
				this.closePopup();
			}
		},
		// 跳转到添加好友页面
		goToAddFriend() {
			this.closePopup();
			uni.navigateTo({
				url: '/pages/contacts/add-friend'
			});
		},
		// 跳转到通话记录页
		goToCallRecords() {
			this.callUnread = 0;
			uni.navigateTo({
				url: '/pages/call/call-records'
			});
		},
		// 跳转到发起群聊页面
		goToCreateGroup() {
			this.closePopup();
			uni.navigateTo({
				url: '/pages/contacts/create-group'
			});
		},
		handleSearch() {
			console.log('Search clicked');
		},
		handleSearchFocus() {
			// 聊天列表只做本页过滤，点搜索框进入全局搜索页
			uni.navigateTo({ url: '/pages/chat/search' });
		},
		async loadAtMeUnread() {
			try {
				const userInfo = uni.getStorageSync('userInfo');
				const userId = userInfo && (userInfo.userId || userInfo.id);
				if (!userId) return;
				const res = await getAtMeUnread({ userId: String(userId) });
				if (res && res.code === 200) {
					this.atMeChatIds = (res.data || []).map(String);
				}
			} catch (e) {
				// 搜索服务不可用时不影响聊天列表
			}
		},
		hasAtMe(chat) {
			return !!(chat && this.atMeChatIds.indexOf(String(chat.id)) !== -1);
		},
		clearSearch() {
			this.searchKeyword = '';
		},
		openChat(chat) {
			console.log('Open chat:', chat);

			// 清空未读消息数量（前端立即清零）
			if (chat.unreadCount && chat.unreadCount > 0) {
				chat.unreadCount = 0;
			}

			// 存储当前聊天窗口信息
			const currentChat = {
				chatId: chat.id,
				chatType: chat.chatType || 1,
				chatName: chat.name,
				avatar: chat.avatar,
				lastMessageContent: chat.lastMessage ? { content: chat.lastMessage } : null,
				unreadCount: 0, // 已清零
				isOnline: chat.online || false,
				updateTime: Date.now()
			};

			try {
				uni.setStorageSync('currentChat', JSON.stringify(currentChat));
				console.log('已存储当前聊天窗口:', currentChat);
			} catch (e) {
				console.error('存储聊天信息失败:', e);
			}

			// 通知服务端切换聊天窗口
			this.notifySwitchChatWindow(chat.id);

			// 跳转到聊天详情页
			const chatType = chat.chatType || 1;
			const encodedName = encodeURIComponent(chat.name);
			uni.navigateTo({
				url: `/pages/chat/chat-detail?id=${chat.id}&name=${encodedName}&chatType=${chatType}`,
				success: () => {
					console.log('跳转到聊天详情页成功');
				},
				fail: (err) => {
					console.error('跳转失败:', err);
					uni.showToast({
						title: '打开聊天失败',
						icon: 'none'
					});
				}
			});
		},
		/**
		 * 通知服务端切换聊天窗口
		 */
		notifySwitchChatWindow(chatId) {
			if (!this.userInfo || !this.userInfo.id) {
				console.warn('用户信息不存在，无法通知服务端切换聊天窗口');
				return;
			}

			// 构造 WebSocket 消息
			const message = {
				senderId: this.userInfo.id,
				receiverId: chatId,
				message: '',
				messageType: 'notify',
				subType: 'switch_chat_window'
			};

			console.log('通知服务端切换聊天窗口:', message);

			// 发送 WebSocket 消息
			const wsClient = getWebSocketClient();
			wsClient.sendMessage(JSON.stringify(message));
		},
		handleNavChange(path) {
			console.log('Navigation changed to:', path);
			this.currentPath = path;
		},
		async onRefresh() {
			this.refreshing = true;
			// 重置分页状态
			this.pageNum = 1;
			this.hasMore = true;
			this.loadedPages = new Set();

			// 重新获取消息列表
			await this.loadLastMessages();

			// 重新获取未读消息数量
			await this.loadUnreadCount();

			this.refreshing = false;
		},
		updateWsStatus() {
			// 通过改变 wsStatusKey 来强制更新状态显示
			this.wsStatusKey++;
		},
		// 定时更新 WebSocket 状态，确保 UI 实时刷新
		startWsStatusTimer() {
			// 清除之前的定时器
			this.stopWsStatusTimer();

			// 每秒更新一次状态
			this.wsStatusTimer = setInterval(() => {
				this.updateWsStatus();
			}, 1000);
		},
		stopWsStatusTimer() {
			if (this.wsStatusTimer) {
				clearInterval(this.wsStatusTimer);
				this.wsStatusTimer = null;
			}
		},
		// 处理 WebSocket 消息
		handleWebSocketMessage(data) {
			console.log('收到 WebSocket 消息:', data);

			try {
				const message = JSON.parse(data);
				const messageType = message.messageType;
				const subType = message.subType;

				// 根据 messageType 和 subType 分发处理
				if (messageType === 'chat') {
					if (subType === 'mes_receive') {
						// 接收聊天消息 - 刷新左侧列表
						this.handleChatMessageReceive(message);
					} else if (subType === 'mes_send_ack') {
						// 消息发送确认 - 刷新左侧列表
						this.handleMessageSendAck(message);
					} else if (subType === 'mes_read') {
						// 消息已读通知 - 更新列表中的未读状态
						this.handleMessageReadNotify(message);
					} else {
						console.log('未知 subType:', subType);
					}
				} else if (messageType === 'notify') {
					if (subType === 'friend_online') {
						// 好友上线通知
						this.handleFriendOnlineNotify(message);
					} else if (subType === 'friend_offline') {
						// 好友下线通知
						this.handleFriendOfflineNotify(message);
					} else {
						console.log('未知 notify subType:', subType);
					}
				} else if (messageType === 'heart_beat') {
					console.log('收到心跳响应');
				} else {
					console.log('未知消息类型:', messageType);
				}
			} catch (error) {
				console.error('解析 WebSocket 消息失败:', error);
			}
		},
		// 处理接收到的聊天消息 (mes_receive)
		handleChatMessageReceive(message) {
			console.log('处理接收到的聊天消息:', message);

			const senderId = message.senderId;

			// 查找是否已存在该聊天
			const chatIndex = this.chatList.findIndex(chat => chat.id === senderId);

			if (chatIndex !== -1) {
				// 更新已存在的聊天
				const chat = this.chatList[chatIndex];
				chat.lastMessage = this.formatLastMessage(message.message || message.content, message.messageType);
				chat.time = this.formatTime(message.createTime || Date.now());
				chat.unreadCount = (chat.unreadCount || 0) + 1;
				chat.muted = isMuted(this.chatPrefs, chat.id);
				chat.pinned = isPinned(this.chatPrefs, chat.id);
				chat.draft = '';

				// 移到列表顶部
				this.chatList.splice(chatIndex, 1);
				this.chatList.unshift(chat);
			} else {
				// 添加新的聊天到列表顶部
				this.chatList.unshift({
					id: senderId,
					name: message.chatName || message.sender?.username || message.senderName || '新消息',
					avatar: message.avatar || message.sender?.avatar || message.senderAvatar || null,
					lastMessage: this.formatLastMessage(message.message || message.content, message.messageType),
					time: this.formatTime(message.createTime || Date.now()),
					unreadCount: 1,
					online: message.isOnline || false,
					muted: isMuted(this.chatPrefs, senderId),
					pinned: isPinned(this.chatPrefs, senderId) || false,
					chatType: message.chatType || 1,
					draft: ''
				});
			}

			// 刷新未读消息总数
			this.loadUnreadCount();

			// 播放提示音
			// this.playNotificationSound();
		},
		// 处理消息发送确认 (mes_send_ack)
		handleMessageSendAck(message) {
			console.log('处理消息发送确认:', message);

			// 延迟刷新左侧列表，等待服务端缓存更新完成
			setTimeout(() => {
				this.loadLastMessages();
			}, 500);
		},
		// 处理消息已读通知 (mes_read)
		handleMessageReadNotify(message) {
			console.log('处理消息已读通知:', message);

			// 解析已读消息ID列表
			let readMessageIds = [];
			try {
				const messageContent = JSON.parse(message.message);
				if (messageContent.readChatMessageIds) {
					readMessageIds = JSON.parse(messageContent.readChatMessageIds);
				}
			} catch (e) {
				console.error('解析已读消息ID失败:', e);
			}

			console.log('已读消息ID列表:', readMessageIds);

			// 消息已读通知主要用于聊天详情页更新已读状态
			// 在列表页中，这个消息可能影响某些状态，目前先不处理
		},
		// 处理好友上线通知 (friend_online)
		handleFriendOnlineNotify(message) {
			console.log('好友上线:', message.senderId);

			const senderId = message.senderId;

			// 更新列表中对应好友的在线状态
			const chatIndex = this.chatList.findIndex(chat => chat.id === senderId);
			if (chatIndex !== -1) {
				this.chatList[chatIndex].online = true;
				console.log(`好友 ${this.chatList[chatIndex].name} 上线状态已更新`);
			}

			// 延迟刷新列表，获取最新状态
			setTimeout(() => {
				this.loadLastMessages();
			}, 300);
		},
		// 处理好友下线通知 (friend_offline)
		handleFriendOfflineNotify(message) {
			console.log('好友下线:', message.senderId);

			const senderId = message.senderId;

			// 更新列表中对应好友的在线状态
			const chatIndex = this.chatList.findIndex(chat => chat.id === senderId);
			if (chatIndex !== -1) {
				this.chatList[chatIndex].online = false;
				console.log(`好友 ${this.chatList[chatIndex].name} 下线状态已更新`);
			}

			// 刷新列表
			this.loadLastMessages();
		},
		// 播放消息提示音
		playNotificationSound() {
			// 创建音频上下文并播放提示音
			const innerAudioContext = uni.createInnerAudioContext();
			innerAudioContext.src = '/static/sounds/message.mp3'; // 需要准备提示音文件
			innerAudioContext.play();
			innerAudioContext.onError((err) => {
				console.log('播放提示音失败:', err);
			});
		},
		getWsStatusClass() {
			// 使用 wsStatusKey 触发响应式更新
			const _ = this.wsStatusKey;

			const wsClient = getWebSocketClient();
			const reconnectAttempts = wsClient.getReconnectAttempts();
			const maxRetries = wsClient.getMaxRetries();

			if (reconnectAttempts >= maxRetries) {
				return 'status-error';
			}

			const readyState = wsClient.getReadyState();
			if (readyState === 1) { // OPEN
				return 'status-online';
			} else if (readyState === 0) { // CONNECTING
				// 首次连接时显示连接中
				return reconnectAttempts === 0 ? 'status-connecting' : 'status-reconnecting';
			} else if (wsClient.isReconnecting()) {
				// 重连时显示重连中
				return 'status-reconnecting';
			} else {
				return 'status-offline';
			}
		},
		getWsStatusText() {
			// 使用 wsStatusKey 触发响应式更新
			const _ = this.wsStatusKey;

			const wsClient = getWebSocketClient();
			const reconnectAttempts = wsClient.getReconnectAttempts();
			const maxRetries = wsClient.getMaxRetries();

			if (reconnectAttempts >= maxRetries) {
				return '连接失败';
			}

			const readyState = wsClient.getReadyState();
			if (readyState === 1) { // OPEN
				return '在线';
			} else if (readyState === 0) { // CONNECTING
				// 首次连接时显示连接中
				return reconnectAttempts === 0 ? '连接中' : '重连中';
			} else if (wsClient.isReconnecting()) {
				// 重连时显示重连中
				return '重连中';
			} else {
				return '离线';
			}
		}
	}
};
</script>

<style lang="scss" scoped>
.chat-list-container {
	width: 100%;
	height: 100vh;
	background-color: #f2f2f7;
	display: flex;
	flex-direction: column;
}

/* 弹窗菜单 */
.popup-menu {
	position: fixed;
	top: calc(var(--status-bar-height) + 120rpx);
	right: 48rpx;
	z-index: 60;
	width: 384rpx;
	background-color: #ffffff;
	border-radius: 32rpx;
	box-shadow: 0 24rpx 64rpx rgba(0, 0, 0, 0.12);
	border: 2rpx solid #f3f4f6;
	padding: 16rpx 0;
	transform-origin: top right;
	opacity: 0;
	transform: scale(0.95);
	transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

.popup-menu-show {
	opacity: 1;
	transform: scale(1);
}

/* 三角箭头 */
.popup-arrow {
	position: absolute;
	top: -8rpx;
	right: 32rpx;
	width: 32rpx;
	height: 32rpx;
	background-color: #ffffff;
	transform: rotate(45deg);
	border-top: 2rpx solid #f3f4f6;
	border-left: 2rpx solid #f3f4f6;
}

/* 弹窗菜单项 */
.popup-item {
	display: flex;
	align-items: center;
	gap: 24rpx;
	padding: 24rpx 32rpx;
	transition: background-color 0.2s;
}

.popup-item-hover {
	background-color: #f9fafb;
}

.popup-icon {
	width: 48rpx;
	height: 48rpx;
	color: #111111;
	flex-shrink: 0;
	transition: transform 0.2s;
}

.popup-item-hover .popup-icon {
	transform: scale(1.1);
}

.popup-text {
	font-size: 28rpx;
	font-weight: 600;
	color: #1c1c1e;
	font-family: 'Plus Jakarta Sans', 'PingFang SC', -apple-system, BlinkMacSystemFont, sans-serif;
}

/* 分隔线 */
.popup-divider {
	height: 2rpx;
	background-color: #f3f4f6;
	margin: 0 32rpx;
}

/* 顶部导航栏 */
.top-bar {
	position: fixed;
	top: 0;
	left: 0;
	right: 0;
	z-index: 50;
	background: rgba(255, 255, 255, 0.9);
	backdrop-filter: blur(60rpx);
	-webkit-backdrop-filter: blur(60rpx);
	border-bottom: 1rpx solid rgba(60, 60, 67, 0.12);
}

.top-bar-content {
	padding: 32rpx 32rpx 24rpx;
	max-width: 750rpx;
	margin: 0 auto;
}

/* 顶部第一行 */
.header-row {
	display: flex;
	align-items: center;
	justify-content: space-between;
	margin-bottom: 32rpx;
	position: relative;
}

.left-section {
	display: flex;
	align-items: center;
	gap: 23rpx;
}

.avatar-wrapper {
	width: 69rpx;
	height: 69rpx;
	border-radius: 50%;
	overflow: hidden;
	border: 4rpx solid rgba(17, 17, 17, 0.1);
}

.avatar {
	width: 100%;
	height: 100%;
}

.avatar-placeholder {
	width: 100%;
	height: 100%;
	border-radius: 50%;
	background: linear-gradient(135deg, rgba(17, 17, 17, 0.1) 0%, rgba(17, 17, 17, 0.2) 100%);
	display: flex;
	align-items: center;
	justify-content: center;
}

.avatar-icon {
	width: 35rpx;
	height: 35rpx;
}

.header-title {
	font-family: 'Manrope', -apple-system, BlinkMacSystemFont, sans-serif;
	font-size: 38rpx;
	font-weight: 700;
	color: #1c1c1e;
	letter-spacing: -0.02em;
}

/* 顶部未读消息总数徽章 */
.total-unread-badge {
	margin-left: 12rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	min-width: 40rpx;
	height: 40rpx;
	border-radius: 50%;
	background-color: #111111;
	padding: 0 8rpx;
}

.total-unread-text {
	font-family: 'Plus Jakarta Sans', -apple-system, BlinkMacSystemFont, sans-serif;
	font-size: 20rpx;
	font-weight: 700;
	color: #ffffff;
	line-height: 1;
}

/* 中间：在线状态 */
.status-indicator {
	position: absolute;
	left: 50%;
	transform: translateX(-50%);
	display: flex;
	align-items: center;
	gap: 12rpx;
	opacity: 0.6;
}

.status-dot {
	width: 12rpx;
	height: 12rpx;
	border-radius: 50%;
}

.status-dot.status-online {
	background-color: #10b981;
	box-shadow: 0 0 20rpx rgba(16, 185, 129, 0.6);
}

.status-dot.status-connecting {
	background-color: #3498db;
	box-shadow: 0 0 20rpx rgba(52, 152, 219, 0.6);
	animation: pulse 1.5s ease-in-out infinite;
}

.status-dot.status-reconnecting {
	background-color: #f39c12;
	box-shadow: 0 0 20rpx rgba(243, 156, 18, 0.6);
	animation: pulse 1.5s ease-in-out infinite;
}

.status-dot.status-offline {
	background-color: #95a5a6;
	box-shadow: 0 0 20rpx rgba(149, 165, 166, 0.6);
}

.status-dot.status-error {
	background-color: #e74c3c;
	box-shadow: 0 0 20rpx rgba(231, 76, 60, 0.6);
	animation: pulse 1s ease-in-out infinite;
}

@keyframes pulse {
	0%, 100% {
		opacity: 1;
	}
	50% {
		opacity: 0.5;
	}
}

.status-text {
	font-family: 'Plus Jakarta Sans', -apple-system, BlinkMacSystemFont, sans-serif;
	font-size: 21rpx;
	font-weight: 600;
	color: #1c1c1e;
	text-transform: uppercase;
	letter-spacing: 0.1em;
}

.right-section {
	display: flex;
	align-items: center;
	gap: 14rpx;
}

.calls-btn {
	width: 62rpx;
	height: 62rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	border-radius: 16rpx;
	border: 2rpx solid #e5e7eb;
	background-color: #ffffff;
	transition: all 0.2s;
	position: relative;
}

.call-unread-badge {
	position: absolute;
	top: -10rpx;
	right: -10rpx;
	min-width: 28rpx;
	height: 28rpx;
	padding: 0 6rpx;
	box-sizing: border-box;
	border-radius: 14rpx;
	background-color: #ff3b30;
	display: flex;
	align-items: center;
	justify-content: center;
	border: 2rpx solid #ffffff;
}
.call-unread-text {
	font-size: 18rpx;
	line-height: 1;
	color: #ffffff;
	font-weight: 600;
}

.add-btn {
	width: 62rpx;
	height: 62rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	border-radius: 16rpx;
	border: 2rpx solid #e5e7eb;
	background-color: #ffffff;
	transition: all 0.2s;
}

.add-btn-hover {
	background-color: #f3f4f6;
	transform: scale(0.95);
}

.icon-add {
	width: 38rpx;
	height: 38rpx;
}

/* 搜索框 */
.search-bar {
	position: relative;
	display: flex;
	align-items: center;
}

.search-icon {
	position: absolute;
	left: 26rpx;
	width: 34rpx;
	height: 34rpx;
	opacity: 0.45;
	z-index: 1;
}

.search-input {
	width: 100%;
	height: 72rpx;
	background-color: rgba(120, 120, 128, 0.12);
	border-radius: 18rpx;
	padding: 0 72rpx 0 76rpx;
	font-size: 27rpx;
	color: #1c1c1e;
	border: none;
	transition: all 0.2s;
}

.search-input:focus {
	box-shadow: 0 0 0 4rpx rgba(17, 17, 17, 0.08);
}

.search-placeholder {
	color: rgba(60, 60, 67, 0.38);
}

.search-clear {
	position: absolute;
	right: 18rpx;
	width: 40rpx;
	height: 40rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	border-radius: 50%;
	background-color: rgba(60, 60, 67, 0.18);
	z-index: 2;
}

.search-clear-text {
	font-size: 26rpx;
	line-height: 1;
	color: #ffffff;
}

/* 主要内容区域 */
.main-content {
	flex: 1;
	height: 0;
	padding-top: calc(var(--status-bar-height) + 224rpx);
	padding-bottom: 200rpx;
}

/* 空状态 */
.empty-state {
	display: flex;
	flex-direction: column;
	align-items: center;
	justify-content: center;
	padding: 120rpx 32rpx;
	gap: 24rpx;
}

.empty-icon {
	width: 120rpx;
	height: 120rpx;
	opacity: 0.3;
}

.empty-text {
	font-family: 'Plus Jakarta Sans', -apple-system, BlinkMacSystemFont, sans-serif;
	font-size: 28rpx;
	color: #9ca3af;
	text-align: center;
}

/* 聊天列表 */
.chat-list {
	margin: 0 24rpx;
	padding: 0;
	display: flex;
	flex-direction: column;
	gap: 0;
	background-color: #ffffff;
	border: 1rpx solid rgba(60, 60, 67, 0.12);
	border-radius: 20rpx;
	overflow: hidden;
}

.chat-card {
	background-color: #ffffff;
	padding: 24rpx;
	display: flex;
	align-items: center;
	gap: 24rpx;
	border-radius: 0;
	box-shadow: none;
	border: 0;
	border-bottom: 1rpx solid rgba(60, 60, 67, 0.12);
	transition: background-color 0.15s;
}

.chat-card:last-child {
	border-bottom: 0;
}

.chat-card-hover {
	background-color: #f2f2f7;
}

.chat-avatar-wrapper {
	position: relative;
	flex-shrink: 0;
	width: 96rpx;
	height: 96rpx;
}

.chat-avatar-box {
	position: relative;
	width: 96rpx;
	height: 96rpx;
	border-radius: 50%;
	background: #1c1c1e;
	display: flex;
	align-items: center;
	justify-content: center;
	overflow: hidden;
}

.chat-avatar {
	position: absolute;
	left: 0;
	top: 0;
	width: 96rpx;
	height: 96rpx;
	border-radius: 50%;
	display: block;
	background: #f2f2f7;
	overflow: hidden;
}

.chat-avatar-default {
	display: flex;
	align-items: center;
	justify-content: center;
	background: #1c1c1e;
}

.chat-avatar-letter {
	font-size: 36rpx;
	font-weight: 600;
	color: #ffffff;
	line-height: 1;
}

/* 无名会话不再显示「?」，改用克制的线性图标 */
.chat-avatar-fallback {
	width: 44rpx;
	height: 44rpx;
	opacity: 0.9;
	filter: invert(1);
}

.chat-avatar-icon {
	width: 48rpx;
	height: 48rpx;
	opacity: 0.35;
}

.online-indicator {
	position: absolute;
	bottom: 0;
	right: 0;
	width: 28rpx;
	height: 28rpx;
	background-color: #10b981;
	border: 4rpx solid #ffffff;
	border-radius: 50%;
}

.chat-content {
	flex: 1;
	min-width: 0;
	display: flex;
	flex-direction: column;
	gap: 4rpx;
}

.chat-header {
	display: flex;
	justify-content: space-between;
	align-items: center;
	margin-bottom: 4rpx;
}

.chat-name-row {
	display: flex;
	align-items: center;
	flex: 1;
	min-width: 0;
	gap: 8rpx;
}

.chat-name {
	font-size: 32rpx;
	font-weight: 700;
	color: #1c1c1e;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
	flex: 1;
	min-width: 0;
	font-family: 'Plus Jakarta Sans', -apple-system, BlinkMacSystemFont, sans-serif;
}

.chat-tag {
	flex-shrink: 0;
	padding: 2rpx 8rpx;
	border-radius: 6rpx;
	background: rgba(10, 132, 255, 0.1);
	color: #0a84ff;
	font-size: 18rpx;
	line-height: 1.5;
	font-weight: 600;
}

.chat-tag-mute {
	background: rgba(60, 60, 67, 0.08);
	color: rgba(60, 60, 67, 0.55);
}

.chat-tag-at {
	background: rgba(255, 59, 48, 0.10);
	color: #ff3b30;
	font-weight: 600;
}

.chat-name-muted {
	opacity: 0.8;
}

.chat-time {
	font-size: 22rpx;
	color: #6b7280;
	font-weight: 500;
	margin-left: 16rpx;
	flex-shrink: 0;
	font-family: 'Plus Jakarta Sans', -apple-system, BlinkMacSystemFont, sans-serif;
}

.chat-footer {
	display: flex;
	justify-content: space-between;
	align-items: center;
}

.chat-message {
	font-size: 28rpx;
	color: rgba(30, 27, 19, 0.6);
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
	flex: 1;
	padding-right: 32rpx;
	font-family: 'Plus Jakarta Sans', -apple-system, BlinkMacSystemFont, sans-serif;
}

.chat-message-muted {
	opacity: 0.5;
}

.unread-badge {
	background-color: #111111;
	color: #ffffff;
	font-size: 20rpx;
	font-weight: 700;
	padding: 4rpx 12rpx;
	border-radius: 9999rpx;
	min-width: 40rpx;
	text-align: center;
	flex-shrink: 0;
}

.unread-text {
	color: #ffffff;
	font-size: 20rpx;
	font-weight: 700;
	font-family: 'Plus Jakarta Sans', -apple-system, BlinkMacSystemFont, sans-serif;
}

	/* 加载中提示 */
	.loading-more {
		display: flex;
		align-items: center;
		justify-content: center;
		padding: 24rpx 0;
	}

	.loading-more-text {
		background: rgba(17, 17, 17, 0.1);
		color: #111111;
		font-size: 22rpx;
		font-weight: 500;
		padding: 10rpx 28rpx;
		border-radius: 9999rpx;
		letter-spacing: 0.08em;
		font-family: 'Plus Jakarta Sans', -apple-system, BlinkMacSystemFont, sans-serif;
	}

	.no-more {
		display: flex;
		align-items: center;
		justify-content: center;
		padding: 32rpx 0;
	}

	.no-more-text {
		font-size: 26rpx;
		color: #d1d5db;
	}
</style>
