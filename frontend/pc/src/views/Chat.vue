<template>
  <!-- 联系人列表 + 聊天窗口 -->
  <div class="main-content" :class="{ 'mobile-list-visible': mobileListVisible }">
    <!-- 左侧消息列表 -->
    <div class="left-container">
      <div class="search-bar sticky">
        <div class="search-input-wrap">
          <input
              v-model="conversationSearch"
              type="text"
              placeholder="搜索会话"
              aria-label="搜索会话"
              @keydown.esc="clearConversationSearch"
          >
          <span v-if="hasConversationSearch" class="search-clear" title="清空搜索" @click="clearConversationSearch">×</span>
        </div>
        <button type="button" class="global-search-entry" title="全局搜索所有会话的消息" @click="openGlobalSearch">
          <svg viewBox="0 0 24 24" width="17" height="17" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
            <circle cx="11" cy="11" r="7"></circle>
            <path d="M16.5 16.5L21 21" stroke-linecap="round"></path>
          </svg>
        </button>
        <span class="back-btn" @click="openAddFriend">+</span>
      </div>
      <div class="message-list" @scroll="messageScroll" ref="messageList">
        <!-- 加载骨架屏 -->
        <div v-if="loading && messages.length === 0" class="chat-skeleton" aria-hidden="true">
          <div v-for="i in 4" :key="i" class="skeleton-row">
            <div class="skeleton-avatar"></div>
            <div class="skeleton-lines">
              <div class="skeleton-line w60"></div>
              <div class="skeleton-line w85"></div>
            </div>
            <div class="skeleton-time w25"></div>
          </div>
        </div>

        <!-- 列表加载失败：给重试出口，不要伪装成「暂无聊天记录」 -->
        <div v-else-if="listLoadFailed" class="chat-empty-state">
          <p class="empty-title">会话列表加载失败</p>
          <p class="empty-desc">
            网络或服务异常，
            <span class="chat-list-retry" @click="getLastMes(0)">点击重试</span>
          </p>
        </div>

        <!-- 空态插画 -->
        <div v-else-if="messages.length === 0" class="chat-empty-state">
          <svg class="empty-art" viewBox="0 0 72 72" fill="none" aria-hidden="true">
            <defs>
              <linearGradient id="chatGrad" x1="0" y1="0" x2="1" y2="1">
                <stop offset="0" stop-color="#007aff" stop-opacity="0.18"/>
                <stop offset="1" stop-color="#af52de" stop-opacity="0.08"/>
              </linearGradient>
            </defs>
            <circle cx="36" cy="36" r="30" fill="url(#chatGrad)"/>
            <rect x="13" y="19" width="35" height="24" rx="12" style="fill: var(--card)" stroke="currentColor" stroke-width="2"/>
            <path d="M13 44l8-2.5" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
            <rect x="29" y="34" width="27" height="19" rx="9.5" style="fill: var(--card)" stroke="currentColor" stroke-width="2" opacity="0.7"/>
            <circle cx="23" cy="31" r="2.2" fill="currentColor"/>
            <circle cx="32" cy="31" r="2.2" fill="currentColor"/>
            <path d="M51 51l4.5 4.5-3-.6" stroke="#ff9500" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
          <p class="empty-title">暂无聊天记录</p>
          <p class="empty-desc">点击右上角 + 发起你的第一段对话</p>
        </div>

        <!-- 搜索无结果：和「真的没有会话」区分开，并给出清空搜索的出口 -->
        <div v-else-if="hasConversationSearch && filteredMessages.length === 0" class="chat-empty-state chat-search-empty">
          <svg class="empty-art search-empty-art" viewBox="0 0 72 72" fill="none" aria-hidden="true">
            <circle cx="33" cy="33" r="21" stroke="currentColor" stroke-width="2" opacity="0.55"/>
            <path d="M48 48l11 11" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" opacity="0.7"/>
            <path d="M25 33h16" stroke="currentColor" stroke-width="2" stroke-linecap="round" opacity="0.5"/>
          </svg>
          <p class="empty-title">没有匹配的会话</p>
          <p class="empty-desc">试试别的关键词，或 <span class="chat-list-retry" @click="clearConversationSearch">清空搜索</span></p>
        </div>

        <div
            v-for="message in filteredMessages"
            :key="message.chatId"
            class="message-item"
            :data-chat-id="message.chatId"
            :class="{ active: currentChatWindow && currentChatWindow.chatId === message.chatId, pinned: isPinned(message.chatId) }"
            @click="openChatWindow(message.chatId)"
            @contextmenu.prevent="showChatClickMenu($event, message.chatId)"
        >
          <div class="message-avatar-container">
            <div class="avatar">
              <img v-if="message.avatar" :src="message.avatar" :alt="message.chatName" @error="handleAvatarError(message)">
              <span v-else-if="avatarLetter(message)">{{ avatarLetter(message) }}</span>
              <svg v-else class="avatar-fallback-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                <template v-if="message.chatType === 2">
                  <circle cx="9" cy="8" r="3"></circle>
                  <path d="M3.5 19c.6-3.2 2.8-5 5.5-5s4.9 1.8 5.5 5"></path>
                  <circle cx="17" cy="9" r="2.4"></circle>
                  <path d="M14.8 14.6c2.4-.6 4.7.8 5.7 3.9"></path>
                </template>
                <template v-else>
                  <circle cx="12" cy="8" r="3.6"></circle>
                  <path d="M4.8 20c.9-4.1 3.6-6.2 7.2-6.2s6.3 2.1 7.2 6.2"></path>
                </template>
              </svg>
            </div>
            <div
                v-if="message.unreadCount > 0"
                class="friend-unread-badge"
                :data-count="message.unreadCount > 99 ? '99+' : ''"
            >
              {{ message.unreadCount > 99 ? '99+' : message.unreadCount }}
            </div>
          </div>
          <div class="info">
            <div class="name"><span class="name-text">{{ formatChatName(message.chatName, message.chatType) }}</span><span v-if="isPinned(message.chatId)" class="pin-tag">置顶</span><span v-if="isAtMeChat(message.chatId)" class="at-me-tag">@我</span>
              <svg v-if="isMuted(message.chatId)" class="mute-tag" viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-label="消息免打扰">
                <path d="M13.7 21a2 2 0 0 1-3.4 0"/>
                <path d="M18 15.5V10a6 6 0 0 0-9.3-5"/>
                <path d="M6 9.3V15.5L4 18h11"/>
                <path d="M3 3l18 18"/>
              </svg>
            </div>
            <div class="last-msg">
              <span v-if="typingChats[message.chatId]" class="typing-preview">
                <span class="typing-dots"><i></i><i></i><i></i></span><span class="typing-preview-text">{{ typingChats[message.chatId] }}</span>
              </span>
              <template v-else>
                <span v-if="isAtMeChat(message.chatId)" class="at-me-prefix">[有人@我]</span><span v-if="message.draft" class="draft-prefix">[草稿]</span><span class="last-msg-text">{{ formatLastMessage(message.draft || message?.lastMessageContent?.content) }}</span>
              </template>
            </div>
          </div>
          <div class="status-time">
            <div class="time">{{ formatTime(message.updateTime) }}</div>
            <div class="friend-status" v-if="message.chatType === 1">
              <span class="online-dot" :class="{ online: message.isOnline }"></span>
              <span class="status-label">{{ message.isOnline ? '在线' : '离线' }}</span>
            </div>
            <div v-if="message.chatType === 2" class="group-status">
              <span class="group-tag">群聊</span>
            </div>
            <div v-if="message.chatType === 3" class="bot-status">
              <span class="bot-tag">机器人</span>
            </div>
          </div>
        </div>

        <!-- 加载更多提示 -->
        <div v-if="loading" class="loading-more">
          加载中...
        </div>
        <div v-if="showNoMoreData" class="no-more-data">
          没有更多数据了
        </div>
      </div>
    </div>

    <div class="right-container">
      <!-- 聊天窗口组件 -->
      <ChatWindow v-if="showChatWindow" ref="chatWindow" @close="closeChatWindow" @withdraw="handleMessageWithdraw" @open-group-manage="openGroupManage" @back="handleMobileBack" @refresh-conversation-list="refreshConversationList" @hide-chat="hideChat" />
      <WelcomeWelcome v-else />
    </div>

    <!-- 群管理：全屏覆盖会话列表 + 聊天窗口，返回后原会话状态不丢 -->
    <GroupManage
        v-if="manageGroupId"
        :group-id="manageGroupId"
        @close="closeGroupManage"
        @changed="refreshGroupList"
        @exit="exitGroupManage"
        @prefs-change="syncGroupPrefs"
    />

    <!-- 好友右键菜单 -->
    <div class="right-click-menu"
         v-show="chatRightClickMenu.visible"
         :style="{ top: chatRightClickMenu.top + 'px', left: chatRightClickMenu.left + 'px' }"
         @mouseleave="hideChatRightClickMenu">
      <div class="menu-item" @click="handleChatRightClickMenu('toggle-pin')">
        {{ isPinned(chatRightClickMenu.chatId) ? '取消置顶' : '置顶聊天' }}
      </div>
      <div class="menu-item" @click="handleChatRightClickMenu('toggle-mute')">
        {{ isMuted(chatRightClickMenu.chatId) ? '取消消息免打扰' : '消息免打扰' }}
      </div>
      <div class="menu-divider"></div>
      <div class="menu-item" @click="handleChatRightClickMenu('hide-chat')">
        不显示聊天
      </div>
      <div class="menu-divider"></div>
    </div>

    <!-- 添加好友组件 -->
    <AddFriend v-if="showAddFriend" @close="closeAddFriend" />

    <!-- 全局搜索：跨会话检索消息，点结果直接跳到对应会话并定位 -->
    <GlobalSearchPanel
        :visible="globalSearchVisible"
        @close="globalSearchVisible = false"
        @jump="handleGlobalSearchJump"
    />
  </div>
</template>

<script>
import AddFriend from "./AddFriend.vue";
import ChatWindow from "./ChatWindow.vue";
import {getLastMes, getAtMeUnread} from "@/api/chatMessage.js";
import {hideChat} from "@/api/chat.js";
import {get, getObject, remove, setObject} from '@/utils/localStorage.js'
import "vue3-emoji-picker/css";
import {getSocketInstance, getWebSocketClient} from "@/ws/WebSocketClient.js";
import WelcomeWelcome from "@/components/WelcomeWelcome.vue";
import GlobalSearchPanel from "@/components/GlobalSearchPanel.vue";
import BotDetail from "@/views/BotDetail.vue";
import FriendDetail from "@/views/FriendDetail.vue";
import GroupDetail from "@/views/GroupDetail.vue";
import GroupManage from "@/views/GroupManage.vue";
import { loadChatPrefs, togglePref, isPinned as pinnedOf, isMuted as mutedOf, sortByPinned } from "@/utils/chatPrefs.js";

// 「@我」会话标记要跨刷新保留：存 { ownerId, ids }，直到用户打开该会话才清除。
// 用固定 key 而不是按用户分 key，是因为刷新瞬间 userInfo 可能还没回填（WS 兜底才写入），
// 分 key 会读不到；读出来后再按 ownerId 校验，避免同一浏览器切号串标记。
const AT_ME_STORAGE_KEY = 'atMeChatIds';
function readAtMeChatStore() {
  let store = null;
  try {
    store = JSON.parse(localStorage.getItem(AT_ME_STORAGE_KEY) || 'null');
  } catch (e) {
    store = null;
  }
  if (store && Array.isArray(store.ids)) {
    return { ownerId: String(store.ownerId || ''), ids: store.ids.map(String) };
  }
  // 兼容上一版按用户 id 存的 key
  const user = getObject('userInfo') || {};
  if (user.id) {
    try {
      const legacy = JSON.parse(localStorage.getItem('atMeChatIds_' + user.id) || '[]');
      if (Array.isArray(legacy)) {
        return { ownerId: String(user.id), ids: legacy.map(String) };
      }
    } catch (e) {
      // 忽略损坏数据
    }
  }
  return { ownerId: '', ids: [] };
}

export default {
  name: 'WeChatApp',
  mounted() {
    // 刷新后按 owner 再校验一次「@我」标记，避免同一浏览器切换账号时串标记
    const atMeStore = readAtMeChatStore();
    const loggedUser = getObject('userInfo') || {};
    if (!atMeStore.ownerId || !loggedUser.id || atMeStore.ownerId === String(loggedUser.id)) {
      this.atMeChatIds = atMeStore.ids;
    } else {
      this.atMeChatIds = [];
    }

    // 确保初始加载时路由到chat
    if (this.$route.path === '/') {
      this.$router.replace('/chat')
    }

    this.wsListener = (message) => {
      const data = JSON.parse(message);
      // 处理收到的消息
      this.handleReceiveMessage(data);
    };

    // 订阅websocket消息
    getWebSocketClient().subscribe(this.wsListener);
  },

  beforeDestroy() {
    if (this.wsListener) {
      getWebSocketClient().unsubscribe(this.wsListener);
      this.wsListener = null;
    }
    Object.keys(this.typingChatTimers || {}).forEach(k => clearTimeout(this.typingChatTimers[k]));
    this.typingChatTimers = {};
  },
  components: {
    GroupDetail, FriendDetail, BotDetail, GroupManage,
    AddFriend,
    ChatWindow,
    WelcomeWelcome,
    GlobalSearchPanel
  },
  data() {
    return {
      // 添加好友对话框状态
      showAddFriend: false,
      // 聊天窗口状态
      showChatWindow: false,
      // 窄屏：先显示会话列表，点开会话后整屏显示聊天，返回按钮切回列表
      mobileListVisible: true,
      // 群管理页：非空时覆盖在聊天页上，返回后保留原会话
      manageGroupId: '',
      // 顶部用户ID (其他页面带过来的)
      topUserId: null,
      // 左侧消息列表
      messages: [],
      // 会话列表搜索：匹配会话名与最后一条消息，实时过滤
      conversationSearch: '',
      // 全局搜索浮层（跨会话搜消息并跳转）
      globalSearchVisible: false,
      // 会话列表是否加载失败：失败要和「真的没有会话」区分开
      listLoadFailed: false,
      // 当前选中的聊天窗口
      currentChatWindow: {},
      showEmptyWarn: false, // 空白消息提示
      newMessage: '', // 输入框内容
      webSocket: null, // WebSocket实例,
      wsListener: null,
      chatMessages: [],
      currentUser: {},
      atMeChatIds: readAtMeChatStore().ids, // 收到过“@我”消息且尚未打开的会话 id 列表（持久化，刷新不丢）
      pageNum: 1,           // 当前页码
      pageSize: 20,         // 每页条数
      hasMore: true,        // 是否还有更多数据
      showNoMoreData: false, // 是否显示"没有更多数据"提示，切换会话时重置
      loading: false,       // 是否正在加载
      isScrollingUp: false, // 是否向上滚动
      lastScrollTop: 0,     // 上次滚动位置
      loadedPages: new Set(),
      chatRightClickMenu: {
        visible: false,
        top: 0,
        left: 0,
        chatId:null
      },
      openChat: {}, // 从其他页面打开聊天窗口的参数
      // 会话偏好：置顶 / 免打扰，按用户存在 localStorage
      chatPrefs: {},
      // 会话列表里的「对方正在输入」：{ chatId: 文案 }，5 秒无新帧自动恢复最后一条消息
      typingChats: {},
      typingChatTimers: {}
    }
  },
  computed: {
    // 置顶会话排在最前，其余沿用后端返回的顺序
    sortedMessages() {
      return sortByPinned(this.messages, this.chatPrefs)
    },
    hasConversationSearch() {
      return this.conversationSearch.trim().length > 0
    },
    // 只在本地过滤已有会话：会话名 + 最后一条消息；不发网络请求，输入即时反馈
    filteredMessages() {
      if (!this.hasConversationSearch) {
        return this.sortedMessages
      }
      const keyword = this.conversationSearch.trim().toLowerCase()
      return this.sortedMessages.filter(item => {
        const name = String(item.chatName || '').toLowerCase()
        const lastMessage = String(item?.lastMessageContent?.content || '').toLowerCase()
        return name.includes(keyword) || lastMessage.includes(keyword)
      })
    }
  },
  async created() {
    this.chatPrefs = loadChatPrefs()
    this.currentUser = getObject('userInfo') || {};
    // userInfo 丢失（只剩 cookie 登录态）时先补取一次，否则 isSelf /「@我」判断都会失真
    if (!this.currentUser.id) {
      try {
        const { getUserInfo } = await import('@/api/user');
        const res = await getUserInfo();
        const payload = (res && (res.data || res.userInfo || res)) || null;
        if (payload && payload.id) {
          this.currentUser = payload;
          setObject('userInfo', payload);
        }
      } catch (e) {
        console.warn('聊天页补取用户信息失败', e);
      }
    }
    this.currentChatWindow = getObject('currentChatWindow') || {};

    const { chatId, rank, autoVideoCall } = this.getInitChatParams();

    await this.initChat(chatId, rank)
    // 通话记录页“回拨”直接拉起视频通话
    if (autoVideoCall) this.maybeAutoVideoCall()
    // 补拉离线期间收到的「@我」提醒
    await this.loadAtMeUnread()

  },
  watch: {
    // 从横幅/其他入口直达会话：路由变化时消费 openChat（同页跳转也生效）
    '$route'(route) {
      if (route.path !== '/chat') return
      const openChat = get("openChat")
      if (!openChat || openChat === '{}') return
      const { chatId, rank, autoVideoCall } = this.getInitChatParams()
      if (chatId != null) {
        this.initChat(chatId, rank).then(() => {
          if (autoVideoCall) this.maybeAutoVideoCall()
        })
      }
    }
  },
  methods: {
    // 模板里读会话偏好：模块作用域的函数进不了模板，必须挂到 methods
    isPinned(chatId) {
      return pinnedOf(this.chatPrefs, chatId);
    },
    isMuted(chatId) {
      return mutedOf(this.chatPrefs, chatId);
    },
    // 打开添加好友界面
    openAddFriend() {
      this.showAddFriend = true;
    },

    // 清空会话列表搜索，回到全部会话
    clearConversationSearch() {
      this.conversationSearch = '';
    },

    // 关闭添加好友界面
    closeAddFriend(){
      this.showAddFriend = false;
    },

    // 撤回后同步左侧会话列表的最后一条消息预览
    handleMessageWithdraw(payload) {
      if (!payload || !payload.chatId || !payload.isLast) return;
      const item = this.messages.find(m => m.chatId === payload.chatId);
      if (!item) return;
      const content = payload.isSelf ? '你撤回了一条消息' : '对方撤回了一条消息';
      item.lastMessageContent = Object.assign({}, item.lastMessageContent || {}, { content: content });
    },

    // 转发到其他会话后，服务端预览已更新；重拉第一页，避免侧栏一直显示旧消息
    async refreshConversationList() {
      // 转发帧刚发出，等落库/缓存更新后再拉，否则可能拿到转发前的预览
      await new Promise(resolve => setTimeout(resolve, 700));
      this.pageNum = 1;
      this.pageSize = 20;
      this.loadedPages.clear();
      await this.getLastMes(0);
    },

    getInitChatParams() {
      // 跨页面传参（优先级最高）
      const openChat = JSON.parse(get("openChat") || "{}");
      remove("openChat");

      // 本地缓存
      const currentChatWindow = getObject("currentChatWindow") || {};

      const chatId = openChat.chatId ?? currentChatWindow.chatId;
      const rank = openChat.rank ?? 0;

      return { chatId, rank, autoVideoCall: openChat.autoVideoCall === true };
    },

    // 等聊天窗真正挂载后再发起视频，避免通话组件还没拿到 chatId
    maybeAutoVideoCall() {
      this.$nextTick(() => {
        setTimeout(() => {
          const win = this.$refs.chatWindow;
          if (win && typeof win.openVideoChat === 'function') {
            win.openVideoChat();
          }
        }, 260);
      });
    },

    // 分页获取左侧好友列表
    async getLastMes(rank) {
      if(rank == null){
        rank = 0;
      }
      if (this.loading) return;

      this.loading = true;

      const DEFAULT_PAGE_SIZE = 20;
      // 当排名>0时，需要一次分页直接查询到排名的位置
      if (rank != null && rank > 0) {
        this.pageSize = Math.ceil(rank / this.pageSize) * this.pageSize;
      }
      const data = {
        pageNum: this.pageNum,
        pageSize: this.pageSize
      }

      try {
        const res = await getLastMes(data);
        if (res.data.rows && res.data.rows.length > 0) {
          // 如果是第一页，直接替换
          if (this.pageNum === 1) {
            this.messages = res.data.rows;
          } else {
            // 后续页追加数据
            this.messages = [...this.messages, ...res.data.rows];
          }

          // 更新分页状态
          this.hasMore = this.pageNum < res.data.totalPage;

          // 记录已加载页码
          this.loadedPages.add(this.pageNum);
        } else {
          this.hasMore = false;
        }
        this.listLoadFailed = false;
      } catch (error) {
        // 失败不能落进「暂无聊天记录」，否则用户会以为真的没有会话
        this.listLoadFailed = true;
        console.error("加载消息列表失败:", error);
      } finally {
        this.loading = false;
      }

      // 当排名>0时，在恢复成默认分页大小
      if (rank != null && rank > 0) {
        this.pageNum = (this.pageSize / DEFAULT_PAGE_SIZE);
        this.pageSize = DEFAULT_PAGE_SIZE;
      }
    },

    // 显示"没有更多数据"提示
    showNoMoreDataTip() {
      this.showNoMoreData = true;

      // 提示插入后列表会变高，滚动位置不会自动跟随，
      // 不补这一次滚动的话它只会贴在容器底部露出几个像素
      this.$nextTick(() => {
        const el = this.$refs.messageList;
        if (el) {
          el.scrollTop = el.scrollHeight - el.clientHeight;
        }
      });
    },

    // 加载聊天
    async initChat(chatId, rank){
      // 重置分页状态
      this.pageNum = 1;
      this.hasMore = true;
      this.showNoMoreData = false;
      this.loadedPages.clear();
      await this.getLastMes(rank).then(async () => {
        // 如果有好友ID，则锁定聊天窗口
        if (chatId != null) {
          // 滑动效果
          await this.scrollFixed(chatId);

          // 打开聊天窗口
          await this.openChatWindow(chatId);
        }
      });
    },

    // 滑动固定消息
    async scrollFixed(chatId){
      // 等待DOM更新，确保消息列表已渲染
      await this.$nextTick();

      // 查找目标聊天项
      const targetItem = this.$refs.messageList.querySelector(`[data-chat-id="${chatId}"]`);
      if (targetItem) {
        // 添加高亮动画类
        targetItem.classList.add('highlight-target');

        // 平滑滚动到目标位置
        targetItem.scrollIntoView({
          behavior: 'smooth',
          block: 'center'
        });

        // 3秒后移除高亮样式
        setTimeout(() => {
          targetItem.classList.remove('highlight-target');
        }, 3000);
      }
    },

    // 加载聊天
    async initChat2(friend){
      // 消息列表如果有该好友，直接从消息列表打开，并打开聊天窗口

      // 消息列表没有该好友，要在消息列表创建一条空消息，再打开聊天窗口
    },

    // 打开全局搜索浮层
    openGlobalSearch() {
      this.globalSearchVisible = true;
    },

    // 全局搜索结果点击：切到对应会话，并把目标消息滚到视口中央
    handleGlobalSearchJump(payload) {
      if (!payload || payload.conversationId == null) {
        return;
      }
      this.globalSearchVisible = false;
      this.openChatWindow(payload.conversationId, payload);
    },

    // 打开聊天窗口
    async openChatWindow(chatId, target = null) {
      if(chatId == null){
        return
      }
      this.showChatWindow = true;
      // 窄屏点开会话后切到聊天全屏；桌面端该状态无视觉影响
      this.mobileListVisible = false;

      // 打开了这个会话，清掉它的「@我」标记
      const openedId = String(chatId);
      if (this.atMeChatIds.indexOf(openedId) !== -1) {
        this.atMeChatIds = this.atMeChatIds.filter(id => id !== openedId);
        this.saveAtMeChatIds();
      }

      // 无论是否是首次切换，只要用户打开了这个会话，就清掉未读
      // （页面恢复上次会话时 chatId 与 currentChatWindow 相同，旧逻辑会漏清）
      let openedChat = this.messages.find(chat => String(chat.chatId) === String(chatId));
      if (openedChat == null && target && target.conversationName) {
        // 全局搜索可能命中一个不在当前列表里的会话：临时补一条，保证能真实打开
        openedChat = {
          chatId: openedId,
          chatName: target.conversationName,
          avatar: target.conversationAvatar || '',
          chatType: Number(target.conversationChatType) || 1,
          lastMessage: '',
          lastTime: '',
          unreadCount: 0
        };
        this.messages.unshift(openedChat);
      }
      if (openedChat == null) {
        return;
      }
      if (openedChat.unreadCount > 0) {
        openedChat.unreadCount = 0;
      }
      if (this.currentChatWindow && this.currentChatWindow.unreadCount > 0) {
        this.currentChatWindow.unreadCount = 0;
      }

      // 切换了聊天窗口
      const switchingChat = this.currentChatWindow == null || String(chatId) !== String(this.currentChatWindow.chatId)
      if (switchingChat) {
        this.currentChatWindow = openedChat;

        // 更新当前聊天窗口缓存
        setObject('currentChatWindow', this.currentChatWindow);

        // 已挂载的窗口立即初始化；首次挂载由 ChatWindow.created 读取缓存初始化
        if (this.$refs.chatWindow != null) {
          await this.$refs.chatWindow.initChatWindow()
        }

        // 告诉服务端当前打开的聊天窗口
        this.notifySwitchChatWindow();
      }

      await this.$nextTick();
      if (target && target.messageId && this.$refs.chatWindow) {
        this.$refs.chatWindow.jumpToMessage(target.messageId);
      }
    },

    // 窄屏从聊天返回会话列表：保留当前会话，只切换显示层
    handleMobileBack() {
      this.mobileListVisible = true;
    },

    // 告诉服务端当前打开聊天窗口
    notifySwitchChatWindow(){
      const message = {
        senderId: this.currentUser.id,
        receiverId: this.currentChatWindow.chatId,
        message: this.newMessage.trim(),
        messageType: 'notify',
        subType: 'switch_chat_window'
      };
      getWebSocketClient().sendMessage(JSON.stringify(message));
    },

    // 告诉服务端关闭当前聊天窗口
    notifyCloseChatWindow(chatId){
      const message = {
        senderId: this.currentUser.id,
        receiverId: chatId,
        message: "",
        messageType: 'notify',
        subType: 'close_chat_window'
      };
      getWebSocketClient().sendMessage(JSON.stringify(message));
    },

    // 打开群管理：只接管当前群会话，页面本身不创建新会话
    openGroupManage(groupId) {
      if (groupId == null || groupId === '') {
        return;
      }
      this.manageGroupId = String(groupId);
    },

    // 返回聊天：不销毁 ChatWindow，输入框草稿与滚动位置都保留
    closeGroupManage() {
      this.manageGroupId = '';
    },

    // 邀请/设角色/移出成员/改群资料后，左侧群名人数与预览要跟着刷新
    async refreshGroupList() {
      await this.getLastMes();
      // 会话列表刷新后，同步当前聊天窗口的群名与头像；否则返回聊天仍是旧群名
      const currentId = this.currentChatWindow && this.currentChatWindow.chatId;
      if (currentId == null) {
        return;
      }
      const fresh = this.messages.find(chat => String(chat.chatId) === String(currentId));
      if (!fresh) {
        return;
      }
      this.currentChatWindow = { ...this.currentChatWindow, chatName: fresh.chatName, avatar: fresh.avatar };
      setObject('currentChatWindow', this.currentChatWindow);
      if (this.$refs.chatWindow) {
        this.$refs.chatWindow.syncChatMeta(fresh);
      }
    },

    // 退出或解散后原会话已失效：关闭聊天窗口，回到会话列表
    exitGroupManage() {
      const groupId = this.manageGroupId;
      this.manageGroupId = '';
      if (groupId) {
        this.closeChatWindow(groupId);
      }
      this.getLastMes();
    },

    // 群管理里改了置顶/免打扰，左侧列表立即使用同一份偏好
    syncGroupPrefs(prefs) {
      this.chatPrefs = prefs || {};
    },

    // 关闭聊天窗口方法
    closeChatWindow(chatId) {
      this.showChatWindow = false;
      if(chatId == null){
        return
      }

      // 告诉服务端关闭当前聊天窗口
      this.notifyCloseChatWindow(chatId);
      remove('currentChatWindow');
      this.currentChatWindow = {};
    },

    // 根据不同类型 处理收到的消息
    handleReceiveMessage(data) {
      const messageType = data.messageType;
      const subType = data.subType;
      if(messageType == 'chat' && subType == 'mes_receive'){ // 聊天消息
        this.receiveChatMessage(data);
      }else if(messageType == 'chat' && subType == 'mes_send_ack'){
        this.receiveAck(data)
      }else if(messageType == 'notify' && subType == 'friend_online'){ // 好友上线通知
        this.receiveFriendOnlineNotify(data);
      }else if(messageType == 'notify' && subType == 'friend_offline'){  // 好友下线通知
        this.receiveFriendOfflineNotify(data);
      }else if(messageType == 'chat' && subType == 'typing'){ // 对方正在输入
        this.receiveTyping(data);
      }
    },

    // 收到输入状态：把左侧会话的最后一条消息预览临时换成「正在输入…」，5 秒后恢复
    receiveTyping(data) {
      if (!data || !this.currentUser || !this.currentUser.id) return;
      let payload = data;
      const raw = data.message;
      if (typeof raw === 'string' && raw.charAt(0) === '{') {
        try {
          payload = JSON.parse(raw);
        } catch (e) {
          return;
        }
      }
      const senderId = String(payload.senderId || data.senderId || '');
      if (!senderId || senderId === String(this.currentUser.id)) return;
      const chatType = payload.chatType != null ? payload.chatType : data.chatType;
      if (chatType !== 1 && chatType !== 2) return;
      // 单聊会话 id 是对方 id；群聊会话 id 是群 id
      const chatId = chatType === 1 ? senderId : String(payload.receiverId || '');
      if (!chatId) return;

      let text = '对方正在输入…';
      if (chatType === 2) {
        const name = payload.senderName || this.resolveTypingName(senderId);
        text = `${name} 正在输入…`;
      }
      this.typingChats = { ...this.typingChats, [chatId]: text };
      if (this.typingChatTimers[chatId]) {
        clearTimeout(this.typingChatTimers[chatId]);
      }
      this.typingChatTimers[chatId] = setTimeout(() => {
        this.clearTypingChat(chatId);
      }, 5000);
    },

    // 群聊里解析输入者昵称：从已加载的会话成员/消息里找，找不到用「有人」
    resolveTypingName(senderId) {
      const id = String(senderId);
      const list = this.messages || [];
      for (let i = 0; i < list.length; i++) {
        const m = list[i];
        if (m && String(m.chatId) === id && m.chatName) return m.chatName;
      }
      return '有人';
    },

    // 清除某个会话的输入态
    clearTypingChat(chatId) {
      if (this.typingChatTimers[chatId]) {
        clearTimeout(this.typingChatTimers[chatId]);
        delete this.typingChatTimers[chatId];
      }
      if (this.typingChats[chatId]) {
        const next = { ...this.typingChats };
        delete next[chatId];
        this.typingChats = next;
      }
    },

    // 解析 atUsers（JSON 字符串 / 数组 / 空值）为字符串数组
    parseAtUsers(value) {
      if (!value) return [];
      if (Array.isArray(value)) return value.map(v => String(v));
      if (typeof value !== 'string') return [];
      const raw = value.trim();
      if (!raw) return [];
      try {
        const arr = JSON.parse(raw);
        return Array.isArray(arr) ? arr.map(v => String(v)) : [];
      } catch (e) {
        return [];
      }
    },

    // 这条推送是否 @ 了我
    isAtMeMessage(data) {
      if (!data || !this.currentUser || this.currentUser.id == null) return false;
      if (String(data.senderId) === String(this.currentUser.id)) return false;
      return this.parseAtUsers(data.atUsers).indexOf(String(this.currentUser.id)) !== -1;
    },

    // 左侧会话是否有未查看的 @我
    isAtMeChat(chatId) {
      return this.atMeChatIds.indexOf(String(chatId)) !== -1;
    },

    // 离线期间被 @ 的消息：进入聊天页时从服务端补拉，合并进「@我」标记
    async loadAtMeUnread() {
      if (!this.currentUser || !this.currentUser.id) {
        return;
      }
      try {
        const res = await getAtMeUnread({ userId: String(this.currentUser.id) });
        const ids = (res.data || []).map(String);
        let changed = false;
        ids.forEach(id => {
          if (this.atMeChatIds.indexOf(id) === -1) {
            this.atMeChatIds.push(id);
            changed = true;
          }
        });
        if (changed) {
          this.saveAtMeChatIds();
        }
      } catch (e) {
        // 补拉失败不影响页面其它功能
      }
    },

    // 把「@我」标记写回本地，刷新页面后仍能提示，直到点开会话
    saveAtMeChatIds() {
      try {
        const user = getObject('userInfo') || {};
        localStorage.setItem(AT_ME_STORAGE_KEY, JSON.stringify({
          ownerId: String(user.id || ''),
          ids: this.atMeChatIds
        }));
      } catch (e) {
        // 本地存储不可用（隐私模式/配额满）时只是丢失刷新后的提醒，不影响当前会话内提醒
      }
    },

    // 接收聊天消息
    receiveChatMessage(data){
      if(data == null){
        return
      }
      const chatType = data.chatType;
      if(chatType == null){
        return;
      }

      // 新消息到达即收掉这条会话的「正在输入」预览
      const arrivedChatId = chatType == 1 ? String(data.senderId) : String(data.receiverId);
      this.clearTypingChat(arrivedChatId);

      // 有人在群里 @ 我：给左侧会话打「@我」标记（当前已打开的这个会话由聊天窗口内的高亮负责）
      if (this.isAtMeMessage(data)) {
        // 单聊会话 id 是对方 id；群聊会话 id 是群 id（不能用发送人 id，否则左侧群会话对不上）
        const cid = chatType == 1 ? String(data.senderId) : String(data.receiverId);
        // 只有聊天窗真的打开着才算“当前正在看”；关闭窗口后 currentChatWindow 仍保留，不能据此漏标
        const isCurrent = this.showChatWindow && this.currentChatWindow && String(this.currentChatWindow.chatId) === cid;
        if (!isCurrent && this.atMeChatIds.indexOf(cid) === -1) {
          this.atMeChatIds.push(cid);
          this.saveAtMeChatIds();
        }
      }

      if(chatType == 1){ // 单聊
        // 刷新左侧好友消息列表
        this.getLastMes()
      }else if(chatType == 2){ // 群聊
        setTimeout(() => {
          // 延迟 刷新左侧好友消息列表 （如果直接刷新，服务端缓存还未添加，查出的数据会有问题）
          this.getLastMes()
        }, 500)
      }
    },

    receiveAck(data){
      if(data == null){
        return
      }
      setTimeout(() => {
        // 延迟 刷新左侧好友消息列表 （如果直接刷新，服务端缓存还未添加，查出的数据会有问题）
        this.getLastMes()
      }, 500)
    },

    // 接收好友上线通知
    receiveFriendOnlineNotify(data){
      const senderId = data.senderId;
      console.log('好友上线了', senderId)
      // 好友上线的是当前聊天窗口的好友，设置为在线
      if (this.currentChatWindow && this.currentChatWindow.chatId === senderId){
        this.currentChatWindow.isOnline = true;
      }

      // 延迟300毫秒，可能存在好友在上线过程中，提前返回相应
      setTimeout(() => {
        // 刷新左侧好友列表
        this.getLastMes()
      }, 300);
    },

    // 接收好友下线通知
    receiveFriendOfflineNotify(data){
      const senderId = data.senderId;
      console.log('好友下线了', senderId)
      // 好友下线的是当前聊天窗口的好友，设置为离线
      if (this.currentChatWindow && this.currentChatWindow.chatId === senderId){
        this.currentChatWindow.isOnline = false;
      }

      // 刷新左侧好友列表
      this.getLastMes()
    },

    // 滚动事件处理
    messageScroll(event) {
      const container = event.target;
      const scrollTop = container.scrollTop;

      // 判断滚动方向
      this.isScrollingUp = scrollTop < this.lastScrollTop;
      this.lastScrollTop = scrollTop;

      // 向上滚动或正在加载时不做任何处理
      if (this.isScrollingUp || this.loading) {
        return;
      }

      // 向下滚动且已经没有更多数据：给一次“到底了”的提示
      if (!this.hasMore) {
        this.showNoMoreDataTip();
        return;
      }

      // 检查是否需要加载更多
      const scrollHeight = container.scrollHeight;
      const clientHeight = container.clientHeight;

      // 距离底部100px时触发加载
      if (scrollHeight - scrollTop - clientHeight < 100) {
        this.loadMoreMessages();
      }
    },

    // 加载更多消息
    async loadMoreMessages() {
      // 防止重复加载同一页
      if (this.loadedPages.has(this.pageNum + 1)) return;

      this.pageNum++;
      await this.getLastMes();
    },

    // 会话列表预览：合并转发 / 群投票只展示可读摘要，不暴露内部编码
    formatLastMessage(content) {
      const text = content == null ? '' : String(content)
      if (text.startsWith('【聊天记录】')) {
        const count = (() => {
          try {
            const arr = JSON.parse(text.slice('【聊天记录】'.length))
            return Array.isArray(arr) ? arr.length : 0
          } catch (e) { return 0 }
        })()
        return count ? `[聊天记录] ${count} 条` : '[聊天记录]'
      }
      if (text.startsWith('【投票】')) {
        return '[投票] ' + text.slice('【投票】'.length).split('||')[0]
      }
      if (text.charAt(0) === '{') {
        try {
          const payload = JSON.parse(text)
          if (payload && payload.kind === 'video') {
            const outcome = payload.outcome === 'completed' ? '通话结束' :
              (payload.outcome === 'rejected' || payload.outcome === 'timeout') ? '未接听' :
              payload.outcome === 'busy' ? '忙线未接' :
              payload.outcome === 'canceled' ? '已取消' : '未接通'
            return '[视频通话] ' + outcome
          }
        } catch (e) {}
      }
      return text
    },

    // 格式化时间
    formatTime(timestamp) {
      if(timestamp == null || timestamp == ''){
        return '';
      }
      const date = new Date(timestamp)
      const now = new Date()

      // 获取今天、昨天和输入日期的00:00:00时间戳
      const todayStart = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime()
      const yesterdayStart = todayStart - 86400000
      const inputDateStart = new Date(date.getFullYear(), date.getMonth(), date.getDate()).getTime()

      // 今天 - 显示时间（去掉小时前导零）
      if (inputDateStart >= todayStart) {
        const hours = date.getHours()
        const minutes = date.getMinutes().toString().padStart(2, '0')
        return `${hours}:${minutes}`
      }else if (inputDateStart >= yesterdayStart) { // 昨天 - 总是显示"昨天"
        return '昨天'
      }else if (this.isInCurrentWeek(date, now)) { // 本周内判断
        // 本周内（周一至周五） - 显示星期几
        const weekday = date.getDay()
        if (weekday >= 1 && weekday <= 5) {
          const weekdays = ['星期一', '星期二', '星期三', '星期四', '星期五']
          return weekdays[weekday - 1]
        }
        // 如果是周六日，则显示日期格式
        return this.formatDate(date)
      }else { // 更早时间 - 显示完整年月日（保留前导零）
        return this.formatDate(date)
      }
    },

    /**
     * 判断日期是否在本周
     */
    isInCurrentWeek(date, now) {
      const nowStartOfWeek = new Date(now.getFullYear(), now.getMonth(), now.getDate() - (now.getDay() || 7) + 1)
      const dateStartOfWeek = new Date(date.getFullYear(), date.getMonth(), date.getDate() - (date.getDay() || 7) + 1)
      return nowStartOfWeek.getTime() === dateStartOfWeek.getTime()
    },

    /**
     * 格式化日期为YY/MM/DD
     */
    formatDate(date) {
      const year = date.getFullYear().toString().slice(-2).padStart(2, '0')
      const month = (date.getMonth() + 1).toString().padStart(2, '0')
      const day = date.getDate().toString().padStart(2, '0')
      return `${year}/${month}/${day}`
    },

    // 服务端对已解散/已退出、又查不到名字的会话会回「未知会话」「?」，
    // 直接展示既刺眼又像故障；这里统一收口成可读的匿名名称。
    isPlaceholderChatName(chatName){
      const name = String(chatName == null ? '' : chatName).trim();
      return name === '' || name === '?' || name === '未知会话' || name === '未命名会话' || name === '未命名群聊';
    },

    avatarLetter(message){
      if (!message || this.isPlaceholderChatName(message.chatName)) {
        return '';
      }
      return String(message.chatName).trim().charAt(0);
    },

    handleAvatarError(message){
      // 头像 404 时回落到首字/群聊图标，避免留下一个空白圆
      if (message) {
        message.avatar = '';
      }
    },

    // 格式化聊天名称
    formatChatName(chatName, chatType){
      if(this.isPlaceholderChatName(chatName)){
        return Number(chatType) === 2 ? '未命名群聊' : '未命名会话';
      }
      const name = String(chatName).trim();

      if(name.length > 8){
        // 名称长度大于8 截取前8个字符
        return name.substring(0, 8) + '...';
      }

      return name;
    },

    showChatClickMenu(event, chatId) {
      this.chatRightClickMenu = {
        visible: true,
        top: event.clientY,
        left: event.clientX,
        chatId: chatId
      }
      // 点击其他地方关闭菜单
      document.addEventListener('click', this.hideChatRightClickMenu)
    },

    hideChatRightClickMenu() {
      this.chatRightClickMenu.visible = false
      document.removeEventListener('click', this.hideChatRightClickMenu)
    },

    handleChatRightClickMenu(action) {
      this.hideChatRightClickMenu()
      if (!this.chatRightClickMenu.chatId) return

      const chatId = this.chatRightClickMenu.chatId
      switch(action) {
        case 'toggle-pin':
          this.chatPrefs = togglePref(this.chatPrefs, chatId, 'pinned')
          break
        case 'toggle-mute':
          this.chatPrefs = togglePref(this.chatPrefs, chatId, 'muted')
          break
        case 'hide-chat':
          this.hideChat(chatId)
          break
      }
    },

    // 隐藏聊天
    hideChat(chatId){
      if(this.showChatWindow){
        this.showChatWindow = false
      }
      hideChat(chatId).then(res => {
        if(res.code == 200){
          // 刷新左侧好友消息列表
          this.getLastMes()
        }
      })
    }
  }
}
</script>

<style scoped>
/* ===== 聊天页 · iOS 设计语言 ===== */
.main-content {
  display: flex;
  flex: 1;
  width: 100%;
  height: 100%;
  min-width: 0;
  overflow: hidden;
  position: relative;
  background: var(--bg);
  font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', 'SF Pro Text', 'PingFang SC', 'Helvetica Neue', 'Microsoft YaHei', sans-serif;
  -webkit-font-smoothing: antialiased;
}

/* ---------- 左侧消息列表 ---------- */
.left-container {
  width: 30%;
  flex: 0 0 30%;
  min-width: 180px;
  height: 100%;
  background: var(--bg-blur-strong);
  border-right: 1px solid var(--separator);
  display: flex;
  flex-direction: column;
}

.right-container {
  flex: 1 1 0;
  width: auto;
  min-width: 0;
  height: 100%;
  display: flex;
  flex-direction: column;
}

/* 搜索栏 · iOS 搜索框 */
.search-bar {
  display: flex;
  align-items: center;
  padding: 10px 14px;
  background: rgba(249, 249, 252, 0.95);
  -webkit-backdrop-filter: saturate(180%) blur(24px);
  backdrop-filter: saturate(180%) blur(24px);
  border-bottom: 1px solid var(--separator);
  gap: 10px;
}

.sticky {
  top: 0;
  z-index: 10;
}

.search-input-wrap {
  flex: 1;
  min-width: 0;
  position: relative;
  display: flex;
  align-items: center;
}

.search-bar input {
  width: 100%;
  flex: 1;
  padding: 8px 32px 8px 14px;
  border: none;
  border-radius: 10px;
  background: rgba(118, 118, 128, 0.14);
  outline: none;
  font-size: 14px;
  font-family: inherit;
  color: var(--label);
  transition: background-color 0.3s cubic-bezier(0.32, 0.72, 0, 1);
}

.search-bar input::placeholder {
  color: var(--label-tertiary);
}

.search-bar input:focus {
  background: var(--card);
  box-shadow: 0 0 0 1px rgba(0, 122, 255, 0.25);
}

.search-clear {
  position: absolute;
  right: 8px;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  line-height: 1;
  color: var(--label-secondary);
  background: rgba(118, 118, 128, 0.18);
  cursor: pointer;
  user-select: none;
  transition: background-color 0.2s ease, color 0.2s ease;
}

.search-clear:hover {
  color: var(--label);
  background: rgba(118, 118, 128, 0.3);
}

.global-search-entry {
  width: 34px;
  height: 34px;
  flex: none;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 0;
  border-radius: 10px;
  background: rgba(118, 118, 128, 0.14);
  color: var(--label-secondary);
  cursor: pointer;
  transition: background-color 0.18s ease, color 0.18s ease;
}
.global-search-entry:hover { background: rgba(0, 122, 255, 0.12); color: #007aff; }
.global-search-entry:focus-visible { outline: 2px solid rgba(0, 122, 255, 0.65); outline-offset: 2px; }

.search-bar .back-btn {
  margin-left: 0;
  font-size: 22px;
  font-weight: 400;
  cursor: pointer;
  color: #007aff;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: rgba(0, 122, 255, 0.1);
  line-height: 1;
  transition: all 0.3s cubic-bezier(0.32, 0.72, 0, 1);
}

.search-bar .back-btn:hover {
  background: rgba(0, 122, 255, 0.18);
}

.message-list {
  overflow-y: auto;
  padding: 6px 10px;
}

/* 聊天空态插画 */
.chat-empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 72px 20px;
  text-align: center;
}

/* 消息列表加载骨架 */
.chat-skeleton {
  padding: 6px 10px;
}

.skeleton-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 6px;
}

.skeleton-avatar {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  flex-shrink: 0;
  background: linear-gradient(100deg, var(--fill) 40%, color-mix(in srgb, var(--fill) 40%, #fff 60%) 50%, var(--fill) 60%);
  background-size: 200% 100%;
  animation: skeletonShimmer 1.4s ease-in-out infinite;
}

.skeleton-lines {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.skeleton-line {
  height: 11px;
  border-radius: 6px;
  background: linear-gradient(100deg, var(--fill) 40%, color-mix(in srgb, var(--fill) 40%, #fff 60%) 50%, var(--fill) 60%);
  background-size: 200% 100%;
  animation: skeletonShimmer 1.4s ease-in-out infinite;
}

.skeleton-time {
  height: 9px;
  border-radius: 5px;
  flex-shrink: 0;
  background: linear-gradient(100deg, var(--fill) 40%, color-mix(in srgb, var(--fill) 40%, #fff 60%) 50%, var(--fill) 60%);
  background-size: 200% 100%;
  animation: skeletonShimmer 1.4s ease-in-out infinite;
}

.w60 { width: 60%; }
.w85 { width: 85%; }
.w25 { width: 32px; }

@keyframes skeletonShimmer {
  0% { background-position: 160% 0; }
  100% { background-position: -40% 0; }
}

@media (prefers-reduced-motion: reduce) {
  .skeleton-avatar,
  .skeleton-line,
  .skeleton-time {
    animation: none;
  }
}

.chat-empty-state .empty-art {
  width: 84px;
  height: 84px;
  color: var(--accent);
  filter: drop-shadow(0 10px 24px rgba(0, 122, 255, 0.18));
  margin-bottom: 14px;
}

.chat-empty-state .empty-title {
  margin: 0 0 6px;
  font-size: 15px;
  font-weight: 600;
  color: var(--label);
}

.chat-empty-state .empty-desc {
  margin: 0;
  font-size: 12.5px;
  color: var(--label-secondary);
}

.chat-list-retry {
  color: var(--blue, #007aff);
  font-weight: 600;
  cursor: pointer;
}

.message-list::-webkit-scrollbar {
  display: none;
  width: 0;
}

/* 消息项 · iOS 列表行 */
.message-item {
  display: flex;
  align-items: center;
  padding: 11px 12px;
  margin-bottom: 2px;
  border-radius: 14px;
  cursor: pointer;
  transition: background-color 0.35s cubic-bezier(0.32, 0.72, 0, 1);
}

.message-item:hover {
  background: var(--fill);
}

.message-item.active {
  background: rgba(0, 122, 255, 0.12);
}

.message-avatar-container {
  position: relative;
  flex-shrink: 0;
}

/* iOS 圆形头像 */
.avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: linear-gradient(145deg, #4da3ff, #007aff);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 12px;
  font-size: 18px;
  font-weight: 600;
  overflow: hidden;
  box-shadow: 0 2px 6px rgba(0, 122, 255, 0.22);
}

.avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar span {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar-fallback-icon {
  width: 23px;
  height: 23px;
  opacity: 0.92;
}

.info {
  flex: 1;
  overflow: hidden;
  min-width: 0;
}

.name {
  display: flex;
  align-items: center;
  font-size: 15.5px;
  font-weight: 600;
  margin-bottom: 3px;
  text-align: left;
  color: var(--label);
  letter-spacing: -0.2px;
  white-space: nowrap;
  overflow: hidden;
}

.name-text {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 未查看的「@我」标记 */
.at-me-tag {
  display: inline-block;
  flex: 0 0 auto;
  margin-left: 6px;
  padding: 0 5px;
  border-radius: 7px;
  background: #ff3b30;
  color: #fff;
  font-size: 10.5px;
  font-weight: 700;
  line-height: 15px;
  vertical-align: middle;
}

/* 已置顶会话：淡蓝底 + 名字前的置顶标签，和「@我」红标区分 */
.message-item.pinned {
  background: rgba(0, 122, 255, 0.06);
}

.message-item.pinned.active {
  background: rgba(0, 122, 255, 0.16);
}

.pin-tag {
  display: inline-block;
  flex: 0 0 auto;
  margin-left: 6px;
  padding: 0 5px;
  border-radius: 7px;
  background: rgba(0, 122, 255, 0.14);
  color: #007aff;
  font-size: 10.5px;
  font-weight: 600;
  line-height: 15px;
  vertical-align: middle;
}

/* 免打扰：铃铛斜线，低调灰，不抢未读红点 */
.mute-tag {
  flex: 0 0 auto;
  margin-left: 5px;
  color: var(--secondary-label, #8e8e93);
  vertical-align: middle;
}

/* 会话预览前的「有人@我」前缀，对齐微信的会话列表提示 */
.at-me-prefix {
  flex: 0 0 auto;
  white-space: nowrap;
  color: #ff3b30;
  font-weight: 600;
  margin-right: 2px;
}

.draft-prefix {
  flex: 0 0 auto;
  white-space: nowrap;
  color: #8a8f99;
  font-weight: 600;
  margin-right: 2px;
}

.last-msg {
  display: flex;
  align-items: baseline;
  min-width: 0;
  font-size: 13px;
  color: var(--label-secondary);
  text-align: left;
  line-height: 1.3;
}

/* 前缀不参与省略，正文单独省略：保证「[有人@我]」永远完整可读 */
.last-msg-text {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 会话列表里的「对方正在输入」：占用同一行位置，5 秒后回落到最后一条消息 */
.typing-preview {
  display: flex;
  align-items: center;
  gap: 4px;
  min-width: 0;
  color: var(--label-tertiary);
  font-style: italic;
}

.typing-preview-text {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.typing-dots {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  flex: 0 0 auto;
}

.typing-dots i {
  width: 3px;
  height: 3px;
  border-radius: 50%;
  background: var(--label-tertiary);
  animation: typingBounce 1.2s infinite ease-in-out;
}

.typing-dots i:nth-child(2) { animation-delay: 0.2s; }
.typing-dots i:nth-child(3) { animation-delay: 0.4s; }

@keyframes typingBounce {
  0%, 60%, 100% { opacity: 0.3; transform: translateY(0); }
  30% { opacity: 1; transform: translateY(-2px); }
}

.status-time {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  min-width: 58px;
  gap: 4px;
  flex-shrink: 0;
  margin-left: 8px;
}

.time {
  font-size: 11.5px;
  color: var(--label-tertiary);
  font-weight: 500;
}

/* 状态徽标 */
.friend-status {
  display: flex;
  align-items: center;
  gap: 4px;
}

.online-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: #c7c7cc;
}

.online-dot.online {
  background-color: #34c759;
  box-shadow: 0 0 4px rgba(52, 199, 89, 0.5);
}

.status-label {
  font-size: 11.5px;
  color: var(--label-secondary);
}

.group-status,
.bot-status {
  display: flex;
  align-items: center;
  margin-top: 2px;
}

/* 群聊 / 机器人标签 · iOS 胶囊 */
.group-tag {
  display: inline-block;
  font-size: 10.5px;
  padding: 2px 8px;
  border-radius: 999px;
  font-weight: 600;
  line-height: 1.3;
  color: #7239ea;
  background-color: rgba(114, 57, 234, 0.12);
}

.bot-tag {
  display: inline-block;
  font-size: 10.5px;
  padding: 2px 8px;
  border-radius: 999px;
  font-weight: 600;
  line-height: 1.3;
  color: #0095ff;
  background-color: rgba(0, 149, 255, 0.12);
}

/* iOS 红未读徽标 */
.friend-unread-badge {
  position: absolute;
  top: -4px;
  right: 2px;
  background-color: #ff3b30;
  color: white;
  border-radius: 999px;
  min-width: 18px;
  height: 18px;
  font-size: 11px;
  font-weight: 600;
  line-height: 18px;
  text-align: center;
  padding: 0 5px;
  box-sizing: border-box;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1.5px solid var(--separator);
  box-shadow: 0 1px 3px rgba(255, 59, 48, 0.4);
}

.friend-unread-badge[data-count="99+"] {
  padding: 0 4px;
  min-width: 22px;
}

/* 加载提示 */
.loading-more,
.no-more-data {
  padding: 15px;
  text-align: center;
  color: var(--label-tertiary);
  font-size: 13px;
}

.loading-more {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 8px;
}

.loading-more::before {
  content: "";
  width: 14px;
  height: 14px;
  border: 2px solid rgba(0, 122, 255, 0.2);
  border-top: 2px solid #007aff;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

/* 基础值保持可见：动画时钟冻结时会停在首帧，首帧必须是不透明的。
   消失交给 2 秒定时器，动画只做入场位移——淡出会让文字在消失前先变得看不清 */
.no-more-data {
  animation: fadeInOut 2s ease-in-out forwards;
  opacity: 1;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* 入场只做位移，opacity 全程为 1；消失由 2 秒定时器负责 */
@keyframes fadeInOut {
  0% { transform: translateY(-6px); opacity: 1; }
  20% { transform: translateY(0); opacity: 1; }
  100% { transform: translateY(0); opacity: 1; }
}

/* 右键菜单 · iOS 菜单 */
.right-click-menu {
  position: fixed;
  z-index: 9999;
  background: rgba(255, 255, 255, 0.94);
  -webkit-backdrop-filter: saturate(180%) blur(24px);
  backdrop-filter: saturate(180%) blur(24px);
  border-radius: 14px;
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.2);
  padding: 5px;
  min-width: 170px;
  animation: menu-fade 0.2s cubic-bezier(0.32, 0.72, 0, 1);
  border: 1px solid rgba(0, 0, 0, 0.04);
}

/* 入场只做位移与缩放，不碰 opacity：动画时钟冻结时菜单依然完整可读 */
@keyframes menu-fade {
  from {
    transform: scale(0.96) translateY(4px);
  }
  to {
    transform: scale(1) translateY(0);
  }
}

.right-click-menu .menu-item {
  padding: 10px 14px;
  font-size: 14px;
  color: var(--label);
  display: flex;
  align-items: center;
  border-radius: 10px;
  transition: background-color 0.25s cubic-bezier(0.32, 0.72, 0, 1);
}

.right-click-menu .menu-item:hover {
  background: rgba(0, 122, 255, 0.1);
}

.menu-divider {
  height: 1px;
  background: var(--separator-strong);
  margin: 5px 6px;
}

/* 高亮目标 */
@keyframes target-highlight {
  0%, 100% {
    background-color: transparent;
  }
  15%, 85% {
    background-color: rgba(0, 122, 255, 0.12);
  }
  50% {
    background-color: rgba(0, 122, 255, 0.06);
  }
}

.message-item.highlight-target {
  animation: target-highlight 3s ease-in-out;
  border-radius: 14px;
  position: relative;
  z-index: 1;
}

/* 兼容遗留的聊天窗口样式（保留定义避免回归） */
.input-row textarea {
  width: 100%;
  height: 100%;
  padding: 8px 12px;
  border: 1px solid rgba(60, 60, 67, 0.15);
  border-radius: 18px;
  resize: none;
  box-sizing: border-box;
  line-height: 1.5;
  overflow-y: auto;
  min-height: 40px;
  max-height: 150px;
  outline: none;
  font-family: inherit;
  transition: border-color 0.3s;
}

.input-row textarea.error {
  border-color: #ff3b30;
}

.message.sending .content {
  opacity: 0.7;
  background-color: rgba(0, 122, 255, 0.08);
}

/* ===== 窄屏自适应 =====
   窗口宽度不足时：左栏从 260px 最小宽降为 180px，会话项收紧。
   这里不整块砍掉信息，而是做有损但体面的降级：
   时间缩成 10px 小字贴到预览行右侧，在线状态/群聊标签收起（群名本身已可辨认）。 */
@media (max-width: 640px) {
  .message-item { padding: 9px 8px; position: relative; }
  .avatar { width: 36px; height: 36px; margin-right: 8px; font-size: 15px; }
  .status-time {
    position: absolute;
    right: 8px;
    bottom: 9px;
    display: block;
    min-width: 0;
    margin-left: 0;
    pointer-events: none;
  }
  .status-time .time { font-size: 10px; line-height: 1; color: var(--label-tertiary); }
  .status-time .friend-status,
  .status-time .group-status,
  .status-time .bot-status { display: none; }
  .last-msg { padding-right: 38px; }
  /* 181px 的会话栏里放不下「[有人@我] + 预览」，同一信息已由名字旁的红色「@我」承载，
     这里收起前缀，避免整行被压成三行；宽屏仍保留微信式前缀。 */
  .at-me-prefix { display: none; }
}
/* 手机窄屏：会话列表与聊天二选一，不再把 180px 列表和聊天区挤在 375px 里 */
@media (max-width: 520px) {
  .left-container {
    display: none;
  }

  .main-content.mobile-list-visible .left-container {
    display: flex;
    width: 100%;
    flex: 1 1 auto;
    min-width: 0;
    border-right: 0;
  }

  .main-content.mobile-list-visible .right-container {
    display: none;
  }

  .main-content:not(.mobile-list-visible) .right-container {
    width: 100%;
    flex: 1 1 auto;
  }
}
</style>