<template>
  <div class="app-container">
    <div class="app-wrapper">
      <!-- 主体区域 -->
      <div class="wechat-app">
      <!-- 左侧菜单 -->
      <aside class="menu-bar" :class="{ 'badge-bounce': banner.show }">
        <div class="user-avatar"
             @mousedown="pressStart"
             @mouseup="pressEnd"
             @mouseleave="pressEnd"
             @touchstart="pressStart"
             @touchend="pressEnd"
             @touchcancel="pressEnd">
          <img :src="userInfo.avatar || defaultAvatar" alt="avatar" />
        </div>

        <div class="menu-item" :class="{ active: activeMenu === 'home' }" @click="goToHome" title="首页">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
            <path d="M3 10.6L12 3.2l9 7.4" />
            <path d="M5.2 9.2V20.8h13.6V9.2" />
            <path d="M10 20.8v-5.4h4v5.4" />
          </svg>
        </div>

        <div class="menu-item" :class="{ active: activeMenu === 'chat' }" @click="goToChat" title="聊天">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
            <path d="M21 11.5a8.5 8.5 0 0 1-8.5 8.3 8.7 8.7 0 0 1-4-.9L3 20l1.3-5.5A8.3 8.3 0 0 1 3 11.5 8.5 8.5 0 0 1 11.5 3a8.5 8.5 0 0 1 9.5 8.5z" />
          </svg>
        </div>

        <div class="menu-item" :class="{ active: activeMenu === 'bot' }" @click="goToBot" title="机器人">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
            <rect x="4" y="7" width="16" height="12" rx="3" />
            <path d="M12 7V3.5" />
            <circle cx="12" cy="3" r="1" />
            <circle cx="9.2" cy="12" r="1" fill="currentColor" stroke="none" />
            <circle cx="14.8" cy="12" r="1" fill="currentColor" stroke="none" />
            <path d="M9.2 15.6h5.6" />
          </svg>
        </div>

        <div class="menu-item" :class="{ active: activeMenu === 'ai-chat' }" @click="goToAiChat" title="AI 聊天">
          <svg viewBox="0 0 24 24" fill="currentColor">
            <path d="M12 2.5l1.9 5.6 5.6 1.9-5.6 1.9-1.9 5.6-1.9-5.6-5.6-1.9 5.6-1.9z" />
            <path d="M19.5 14.5l1 2.6 2.6 1-2.6 1-1 2.6-1-2.6-2.6-1 2.6-1z" opacity="0.75" />
            <path d="M5.5 3.5l.8 2 2 .8-2 .8-.8 2-.8-2-2-.8 2-.8z" opacity="0.75" />
          </svg>
        </div>

        <div class="menu-item" :class="{ active: activeMenu === 'friend' }" @click="goToFriend" title="好友">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="9" cy="8" r="3.6" />
            <path d="M2.8 20.2c0-3.4 2.8-5.7 6.2-5.7s6.2 2.3 6.2 5.7" />
            <circle cx="17.2" cy="9.4" r="2.7" />
            <path d="M17 14.6c2.6.4 4.6 2.4 4.6 5" />
          </svg>
          <span v-if="unreadCount.friend" class="menu-badge">{{ unreadCount.friend }}</span>
        </div>

        <!-- 待办入口（勾选框图标） -->
        <div class="menu-item" :class="{ active: activeMenu === 'todo' }" @click="goToTodo" title="待办">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
            <rect x="3.5" y="3.5" width="17" height="17" rx="4.5" />
            <path d="M8 12.2l2.7 2.7 5.3-5.6" />
          </svg>
        </div>

        <!-- 通话记录入口（摄像机 + 时钟） -->
        <div class="menu-item" :class="{ active: activeMenu === 'calls' }" @click="goToCallRecords" title="通话记录">
          <span v-if="callUnread" class="menu-badge">{{ callUnread > 99 ? '99+' : callUnread }}</span>
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
            <rect x="2.6" y="6.2" width="12" height="11.6" rx="2.4" />
            <path d="M14.6 10.4l5.4-2.7v8.6l-5.4-2.7z" />
            <path d="M6.2 9.4a4 4 0 0 1 3.6 3.6" />
          </svg>
        </div>

        <!-- 收藏夹入口（星形图标） -->
        <div class="menu-item" :class="{ active: activeMenu === 'favorites' }" @click="goToFavorites" title="收藏夹">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
            <path d="M12 3.2l2.7 5.5 6 .9-4.3 4.2 1 6-5.4-2.8-5.4 2.8 1-6L4.3 9.6l6-.9z" />
          </svg>
        </div>

        <!-- 朋友圈入口（照片拼贴图标）+ 手机版入口 -->
        <div class="menu-item" :class="{ active: activeMenu === 'moments' }" @click="goToMoments" title="朋友圈">
          <span v-if="unreadCount.moments" class="menu-badge">{{ unreadCount.moments }}</span>
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
            <rect x="2.5" y="2.5" width="8.2" height="8.2" rx="2" />
            <rect x="13.3" y="2.5" width="8.2" height="8.2" rx="2" />
            <rect x="2.5" y="13.3" width="8.2" height="8.2" rx="2" />
            <rect x="10.8" y="10.8" width="10.7" height="10.7" rx="2" />
            <circle cx="14.8" cy="14.8" r="1.1" fill="currentColor" stroke="none" />
            <circle cx="18.9" cy="18.9" r="1.1" fill="currentColor" stroke="none" />
          </svg>
        </div>
        <div class="menu-item" :class="{ active: activeMenu === 'treehole' }" @click="goToTreeHole" title="匿名树洞">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="12" r="8.5" />
            <path d="M12 7.5v4.5l3 1.8" />
            <path d="M9.5 4.2c-.8 1-.4 2.4.7 3M14.5 4.2c.8 1 .4 2.4-.7 3" />
          </svg>
        </div>
        <div class="menu-item mobile-entry" title="手机版" @click="openMobileApp">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
            <rect x="7" y="2.5" width="10" height="19" rx="2.5" />
            <path d="M10.5 18.5h3" />
          </svg>
        </div>

        <div class="menu-item" :class="{ active: activeMenu === 'profile' }" @click="goToProfile" title="我的">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="8" r="4" />
            <path d="M4.5 20.4c0-4.1 3.3-6.6 7.5-6.6s7.5 2.5 7.5 6.6" />
          </svg>
        </div>

        <!-- 在线状态显示 -->
        <div class="status-indicator">
          <div class="status-dot" :class="getWsStatusClass()"></div>
          <div class="status-text">{{ getWsStatusText() }}</div>
        </div>

        <!-- 连接失败提示对话框 -->
        <ConfirmDialog
          :visible="showConnErrorDialog"
          :title="connErrorDialogTitle"
          :content="connErrorDialogContent"
          @confirm="handleConnErrorConfirm"
          @cancel="handleConnErrorCancel"
        />

        <div class="menu-item settings" @click.stop="openSettingsMenu" title="设置">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
            <path d="M10.33 2.55A9.6 9.6 0 0 0 13.67 2.55L14.21 6.1A6.3 6.3 0 0 0 15.61 6.84L17.51 4.14A9.6 9.6 0 0 0 19.86 6.49L17.73 9.39A6.3 6.3 0 0 0 18.2 10.91L21.45 10.33A9.6 9.6 0 0 0 21.45 13.67L17.9 14.21A6.3 6.3 0 0 0 17.16 15.61L19.86 17.51A9.6 9.6 0 0 0 17.51 19.86L14.61 17.73A6.3 6.3 0 0 0 13.09 18.2L13.67 21.45A9.6 9.6 0 0 0 10.33 21.45L9.79 17.9A6.3 6.3 0 0 0 8.39 17.16L6.49 19.86A9.6 9.6 0 0 0 4.14 17.51L6.27 14.61A6.3 6.3 0 0 0 5.8 13.09L2.55 13.67A9.6 9.6 0 0 0 2.55 10.33L6.1 9.79A6.3 6.3 0 0 0 6.84 8.39L4.14 6.49A9.6 9.6 0 0 0 6.49 4.14L9.39 6.27A6.3 6.3 0 0 0 10.91 5.8L10.33 2.55" />
            <circle cx="12" cy="12" r="2.8" />
          </svg>
          <Teleport to="body">
            <transition name="menu-pop">
              <div v-if="showSettingsMenu" class="settings-menu" :style="settingsMenuStyle">
              <div class="settings-group">
                <div class="setting-row" @click="toggleTheme">
                  <svg v-if="isDarkMode" class="setting-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="4.5"/><path d="M12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4"/></svg>
                  <svg v-else class="setting-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M21 12.8A9 9 0 1 1 11.2 3a7 7 0 0 0 9.8 9.8z"/></svg>
                  <span class="setting-label">{{ isDarkMode ? '浅色模式' : '深色模式' }}</span>
                  <span class="ios-switch" :class="{ on: isDarkMode }"><span class="switch-knob"></span></span>
                </div>
                <div class="setting-row" @click="toggleNotification">
                  <svg class="setting-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M18 8a6 6 0 0 0-12 0c0 7-3 9-3 9h18s-3-2-3-9"/><path d="M13.7 21a2 2 0 0 1-3.4 0"/></svg>
                  <span class="setting-label">消息通知</span>
                  <span class="ios-switch" :class="{ on: notifyEnabled }"><span class="switch-knob"></span></span>
                </div>
              </div>
              <div class="settings-group">
                <div class="setting-row" @click="openGlassSettings">
                  <svg class="setting-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M12 3l6.5 5.5-2.3 10.5H7.8L5.5 8.5 12 3z"/><path d="M9.2 8.5h5.6M12 8.5v10.5"/></svg>
                  <span class="setting-label">玻璃主题</span>
                  <span class="setting-value">Aqua 风格</span>
                </div>
                <div class="setting-row" @click="openPrivacy">
                  <svg class="setting-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M12 3l8 3v6c0 5-3.4 7.8-8 9-4.6-1.2-8-4-8-9V6l8-3z"/><path d="M9 12l2 2 4-4"/></svg>
                  <span class="setting-label">隐私与安全</span>
                  <svg class="setting-link-arrow" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M7 17L17 7M9 7h8v8"/></svg>
                </div>
                <div class="setting-row" @click="openAdmin">
                  <svg class="setting-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M12 3l8 3v6c0 5-3.4 7.8-8 9-4.6-1.2-8-4-8-9V6l8-3z"/><path d="M9 12l2 2 4-4"/></svg>
                  <span class="setting-label">后台管理</span>
                  <svg class="setting-link-arrow" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M7 17L17 7M9 7h8v8"/></svg>
                </div>
              </div>
              <div class="settings-group">
                <div class="setting-row logout" @click="logout">退出登录</div>
              </div>
            </div>
            </transition>
          </Teleport>
        </div>
      </aside>

      <!-- 玻璃主题设置浮层 -->
      <Teleport to="body">
        <transition name="glass-fade">
          <div v-if="glassSettingsVisible" class="xg-glass-pop" @click.self="closeGlassSettings">
            <div class="xg-glass-pop-panel">
              <div class="xg-glass-pop-head">
                <span>玻璃主题</span>
                <button class="xg-glass-pop-close" @click="closeGlassSettings">✕</button>
              </div>
              <GlassSettings />
            </div>
          </div>
        </transition>
      </Teleport>

      <!-- 新消息 iOS 横幅 -->
      <transition name="banner-slide">
        <div v-if="banner.show" class="message-banner" @click="openBannerChat">
          <img v-if="banner.avatar" :src="banner.avatar" class="banner-avatar" alt="" />
          <div v-else class="banner-avatar banner-avatar-text">{{ (banner.senderName || '新')[0] }}</div>
          <div class="banner-body">
            <div class="banner-head">
              <span class="banner-name">{{ banner.senderName }}</span>
              <span class="banner-time">{{ banner.time }}</span>
            </div>
            <div class="banner-content">{{ banner.content }}</div>
          </div>
        </div>
      </transition>

        <!-- 中间内容（重点优化） -->
        <main class="content-area">
          <!-- 路由切换不能经过 Vue 的 transition 组件：out-in 模式要先等旧组件离场，
               离场推进依赖 requestAnimationFrame，页面隐藏/被遮挡时 rAF 冻结，
               组件会永远停在 leave-from，导致地址变了但页面不切。
               这里改成组件自身播放位移入场动画，挂载即渲染，不做阻塞式离场。 -->
          <router-view v-slot="{ Component, route }">
            <component
              :is="Component"
              :key="route.path"
              class="router-content"
              @menu-change="handleMenuChange"
              @clear-unread="clearUnread"
            />
          </router-view>
        </main>
    </div>

    <!-- 作者 & 交流群 -->
    <div class="footer-info">
      <div class="author">
        👨‍💻 作者：大象爱吃西瓜
        <span class="divider">|</span>
        📮 联系方式：
        <a class="email">377792937@qq.com</a>
        <span class="divider">|</span>

        💬 微信交流群：
        <span class="group-hover">
        <span class="group-id">技术群</span>

          <!-- 悬浮展示的图片 -->
          <div class="group-pop">
            <img src="@/static/images/tech_group.jpg" alt="技术群" />
            <div class="pop-tip">扫码加入技术交流群</div>
          </div>
        </span>
        <span class="divider">|</span>
        <span class="group-hover">
        <span class="group-id">用户群</span>

          <!-- 悬浮展示的图片 -->
          <div class="group-pop">
            <img src="@/static/images/user_group.jpg" alt="用户群" />
            <div class="pop-tip">扫码加入用户群</div>
          </div>
        </span>
        <span class="divider">|</span>
        
        <!-- 项目链接 -->
        <a href="https://github.com/w3777/xigua-cloud" class="project-link" target="_blank">
          <img src="@/static/icons/github.png" alt="GitHub" class="project-icon" />
          <span>GitHub</span>
        </a>
        <span class="divider">|</span>
        <a href="https://gitee.com/daxiangxigua/xigua-cloud" class="project-link" target="_blank">
          <img src="@/static/icons/gitee.png" alt="Gitee" class="project-icon" />
          <span>Gitee</span>
        </a>
      </div>
    </div>
  </div>
</div>
</template>

<script>
import defaultAvatar from '@/static/images/user_default.png'
import { get, set, getObject, clear } from '@/utils/localStorage'
import { removeToken } from '@/utils/auth'
import { getWebSocketClient } from '@/ws/WebSocketClient'
import router from '@/router'
import { logout } from '@/api/auth'
import { momentInteractUnread } from '@/api/moment'
import { getCallUnread } from '@/api/chatMessage.js'
import { checkTodoReminders, formatRemindAt } from '@/utils/todo'
import { loadChatPrefs, isMuted } from '@/utils/chatPrefs'
import { subscribeWebPush, pushSupported } from '@/utils/webPush'
import { ElMessage } from '@/plugins/message'
import ConfirmDialog from '@/components/ConfirmDialog.vue'
import GlassSettings from '@/glass/GlassSettings.vue'

export default {
  name: 'WeChatApp',
  components: {
    ConfirmDialog,
    GlassSettings
  },
  data() {
    return {
      userInfo: {},
      defaultAvatar,
      activeMenu: 'home',
      showSettingsMenu: false,
      settingsMenuStyle: { left: '86px', bottom: '14px' },
      glassSettingsVisible: false,
      unreadCount: {
        home: 0,
        chat: 0,
        bot: 0,
        'ai-chat': 0,
        friend: 0,
        discover: 0,
        moments: 0,
        profile: 0
      },
      // 连接失败提示对话框
      showConnErrorDialog: false,
      connErrorDialogTitle: '连接失败',
      connErrorDialogContent: '消息服务连接失败，是否重新尝试连接？',
      // 用户是否取消了连接尝试
      userCancelledReconnect: false,
      // 深色模式
      isDarkMode: false,
      // 是否跟随系统深浅色（用户手动切换后置为 false）
      themeFollowSystem: true,
      // 西瓜雨彩蛋
      longPressTimer: null,
      // 新消息横幅
      banner: {
        show: false,
        chatId: null,
        chatType: 1,
        senderName: '',
        avatar: '',
        content: '',
        time: ''
      },
      bannerTimer: null,
      wsListener: null,
      // 桌面通知
      notifyEnabled: false,
      // 待办提醒调度
      todoReminderTimer: null,
      notifiedTodoIds: {},
      // 未接来电角标（与手机共用服务端已读水位）
      callUnread: 0,
      callUnreadTimer: null
    }
  },
  created() {
    this.userInfo = getObject('userInfo') || {}
    this.activeMenu = get('activeMenu') || 'home'
    // 直接在地址栏打开 /calls 时也要高亮对应侧栏入口
    if (this.$route.path.indexOf('/calls') === 0) this.activeMenu = 'calls'
    // 地址栏直接打开 /favorites 时同样高亮收藏入口
    if (this.$route.path.indexOf('/favorites') === 0) this.activeMenu = 'favorites'
    this.initTheme()
    this.setupWebSocketMonitor()
    this.setupGlobalShortcuts()
    this.notifyEnabled = get('notifyEnabled') === '1'
    // 订阅实时消息（横幅 + 桌面通知）
    this.wsListener = (raw) => this.handleWsMessage(raw)
    getWebSocketClient().subscribe(this.wsListener)
    // 启动待办提醒调度：挂载即查一次 + 30 秒轮询
    this.notifiedTodoIds = {}
    this.fireTodoReminder()
    this.todoReminderTimer = setInterval(() => this.fireTodoReminder(), 30000)
    // 朋友圈未读红点：别人给我的点赞/评论未读数，挂载即查一次 + 60 秒轮询
    this.loadMomentsUnread()
    this.momentsUnreadTimer = setInterval(() => this.loadMomentsUnread(), 60000)
    // 未接来电角标：挂载即查一次 + 30 秒轮询，手机端接听后同一水位也会同步
    this.loadCallUnread()
    this.callUnreadTimer = setInterval(() => this.loadCallUnread(), 30000)
    // 点桌面推送通知后，Service Worker 会发消息让页面跳到对应会话
    this.swMessageListener = (evt) => {
      const d = evt && evt.data
      if (!d || d.type !== 'push-open-chat' || d.chatId == null) return
      set('openChat', JSON.stringify({
        chatId: d.chatId,
        chatType: d.chatType || 1,
        chatName: '新消息',
        avatar: ''
      }))
      set('activeMenu', 'chat')
      this.activeMenu = 'chat'
      this.$router.push({ path: '/chat', query: { t: Date.now() } })
    }
    if ('serviceWorker' in navigator) {
      navigator.serviceWorker.addEventListener('message', this.swMessageListener)
    }
  },
  beforeUnmount() {
    // 清理定时器
    if (this.momentsUnreadTimer) {
      clearInterval(this.momentsUnreadTimer)
      this.momentsUnreadTimer = null
    }
    if (this.callUnreadTimer) {
      clearInterval(this.callUnreadTimer)
      this.callUnreadTimer = null
    }
    if (this.wsCheckInterval) {
      clearInterval(this.wsCheckInterval)
      this.wsCheckInterval = null
    }
    if (this.todoReminderTimer) {
      clearInterval(this.todoReminderTimer)
      this.todoReminderTimer = null
    }
    if (this.bannerTimer) {
      clearTimeout(this.bannerTimer)
      this.bannerTimer = null
    }
    if (this.wsListener) {
      getWebSocketClient().unsubscribe(this.wsListener)
      this.wsListener = null
    }
    if (this.swMessageListener && 'serviceWorker' in navigator) {
      navigator.serviceWorker.removeEventListener('message', this.swMessageListener)
      this.swMessageListener = null
    }
    // 清理全局快捷键监听
    if (this._shortcutHandler) {
      window.removeEventListener('keydown', this._shortcutHandler)
      this._shortcutHandler = null
    }
    // 清理系统主题监听
    if (this._themeMqHandler && window.matchMedia) {
      window.matchMedia('(prefers-color-scheme: dark)').removeEventListener('change', this._themeMqHandler)
      this._themeMqHandler = null
    }
  },
  watch: {
    activeMenu(v) {
      if (v !== 'moments') this.loadMomentsUnread()
    }
  },
  methods: {
    setupWebSocketMonitor() {
      const wsClient = getWebSocketClient()
      if (wsClient && !wsClient.getInstance()) {
        // 确保WebSocket已连接
        wsClient.connect()
      }
      
      // 定时更新WebSocket状态，确保UI实时刷新
      this.wsCheckInterval = setInterval(() => {
        // 通过强制更新组件来重新渲染在线状态
        this.$forceUpdate()
        
        // 检查是否达到最大重试次数
        const reconnectAttempts = wsClient.getReconnectAttempts()
        const maxRetries = wsClient.getMaxRetries()
        
        // 如果达到最大值，停止重连并显示错误消息
        if (reconnectAttempts >= maxRetries && !this.showConnErrorDialog && !this.userCancelledReconnect) {
          // 显示连接失败提示对话框
          this.showConnErrorDialog = true
        }
      }, 1000)
    },
    // 处理连接失败对话框的确认操作
    handleConnErrorConfirm() {
      this.showConnErrorDialog = false
      // 重置取消标记，以便重新连接失败时再次显示对话框
      this.userCancelledReconnect = false
      // 重新尝试连接
      const wsClient = getWebSocketClient()
      wsClient.connect()
    },
    // 处理连接失败对话框的取消操作
    handleConnErrorCancel() {
      this.showConnErrorDialog = false
      // 用户取消了连接尝试，不再显示对话框
      this.userCancelledReconnect = true
    },
    goToHome() { this.goToPage('home') },
    goToChat() { this.goToPage('chat') },
    goToBot() { this.goToPage('bot') },
    goToAiChat() {
      const currentBot = getObject('currentBot')
      if (currentBot?.id) {
        this.activeMenu = 'ai-chat'
        set('activeMenu', 'ai-chat')
        this.$router.push(`/ai-chat/${currentBot.id}`)
      } else {
        // 无机器人时落到 Bot 页，菜单高亮保持一致
        this.activeMenu = 'bot'
        set('activeMenu', 'bot')
        this.$router.push('/bot')
      }
    },
    goToFriend() { this.goToPage('friend') },
    goToMoments() { this.goToPage('moments') },
    goToTreeHole() { this.goToPage('treehole') },
    goToTodo() { this.goToPage('todo') },
    goToFavorites() { this.goToPage('favorites') },
    goToCallRecords() {
      this.callUnread = 0
      this.goToPage('calls')
    },
    // 打开手机版 H5（新标签，避免打断当前会话）
    openMobileApp() {
      window.open('https://watermelonim.eu.cc', '_blank', 'noopener,noreferrer')
    },
    openAdmin() {
      window.open('https://xiguabackend.eu.cc', '_blank', 'noopener,noreferrer')
    },
    goToProfile() { this.goToPage('profile') },
    goToPage(menu) {
      this.activeMenu = menu
      set('activeMenu', menu)
      if (menu === 'moments') this.unreadCount.moments = 0
      this.$router.push('/' + menu)
    },
    // ===== 待办提醒扫描：站内提示 + 系统通知 =====
    fireTodoReminder() {
      const due = checkTodoReminders()
      if (!due.length) return
      const fresh = due.filter((d) => !this.notifiedTodoIds[d.id])
      if (!fresh.length) return
      fresh.forEach((d) => {
        this.notifiedTodoIds[d.id] = true
        ElMessage.warning(`⏰ 待办提醒：${d.title}${d.remindAt ? `（${formatRemindAt(d.remindAt)}）` : ''}`)
      })
      // 系统通知（权限已授权时直接发；未授权时顺带请求一次）
      if ('Notification' in window) {
        const send = () => {
          fresh.forEach((d) => {
            try {
              const n = new Notification('待办提醒', { body: d.title, tag: `todo-${d.id}` })
              n.onclick = () => {
                window.focus()
                this.$router.push('/todo')
                n.close()
              }
            } catch (e) { /* 通知失败静默降级为站内提示 */ }
          })
        }
        if (Notification.permission === 'granted') {
          send()
        } else if (Notification.permission === 'default') {
          Notification.requestPermission().then((perm) => {
            if (perm === 'granted') send()
          })
        }
      }
    },
    openSettingsMenu(e) {
      const opening = !this.showSettingsMenu
      if (opening) {
        const anchor = (e && e.currentTarget) || this.$el.querySelector('.menu-item.settings')
        if (anchor && anchor.getBoundingClientRect) {
          const rect = anchor.getBoundingClientRect()
          this.settingsMenuStyle = {
            left: Math.round(rect.right + 14) + 'px',
            bottom: Math.max(8, Math.round(window.innerHeight - rect.bottom)) + 'px'
          }
        }
      }
      this.showSettingsMenu = opening
    },
    openGlassSettings() {
      this.showSettingsMenu = false
      this.glassSettingsVisible = true
    },
    openPrivacy() {
      this.showSettingsMenu = false
      this.$router.push('/settings/privacy')
    },
    closeGlassSettings() {
      this.glassSettingsVisible = false
    },
    // 初始化主题：优先用户手动选择，否则跟随系统
    initTheme() {
      const saved = get('theme')
      if (saved === 'dark' || saved === 'light') {
        this.isDarkMode = saved === 'dark'
        document.documentElement.setAttribute('data-theme', saved)
        this.themeFollowSystem = false
      } else {
        const prefersDark = window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches
        if (prefersDark) {
          this.isDarkMode = true
          document.documentElement.setAttribute('data-theme', 'dark')
        }
        this.themeFollowSystem = true
      }
      this.watchSystemTheme()
    },
    // 跟随系统深浅色（仅当用户未手动选择时生效）
    watchSystemTheme() {
      if (!window.matchMedia) return
      const mq = window.matchMedia('(prefers-color-scheme: dark)')
      // 移除旧监听避免重复
      if (this._themeMqHandler) {
        mq.removeEventListener('change', this._themeMqHandler)
      }
      this._themeMqHandler = (e) => {
        if (!this.themeFollowSystem) return
        this.isDarkMode = e.matches
        document.documentElement.setAttribute('data-theme', e.matches ? 'dark' : 'light')
      }
      mq.addEventListener('change', this._themeMqHandler)
    },
    // 切换深浅色
    toggleTheme() {
      this.isDarkMode = !this.isDarkMode
      const theme = this.isDarkMode ? 'dark' : 'light'
      document.documentElement.setAttribute('data-theme', theme)
      set('theme', theme)
      this.themeFollowSystem = false
    },
    // ===== 全局快捷键（Ctrl+K 搜索会话）=====
    setupGlobalShortcuts() {
      if (this._shortcutHandler) {
        window.removeEventListener('keydown', this._shortcutHandler)
      }
      this._shortcutHandler = (e) => {
        // Ctrl+K / Cmd+K 聚焦搜索框
        if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'k') {
          e.preventDefault()
          const input = document.querySelector('.chat-list-search input, .search-input input, input[placeholder*="搜索"]')
          if (input) {
            input.focus()
            input.select()
          }
        }
      }
      window.addEventListener('keydown', this._shortcutHandler)
    },
    // ===== 新消息横幅 + 桌面通知 =====
    handleWsMessage(raw) {
      let data
      try {
        data = JSON.parse(raw)
      } catch (e) {
        return
      }
      if (!data || data.messageType !== 'chat' || data.subType !== 'mes_receive') return
      // 后端 MessageResponse 未下发 chatId：单聊会话 id 即对方 userId，群聊为群 id。
      // 缺这一步时 data.chatId 恒为 null，会导致所有实时消息被下面的守卫直接丢弃。
      let chatId = data.chatId
      if (chatId == null) {
        if (data.chatType === 2) chatId = data.receiverId
        else if (data.chatType === 1 || data.chatType === 3) chatId = data.senderId
      }
      if (chatId == null) return
      // 自己发出的消息不提示
      if (data.senderId && this.userInfo && String(data.senderId) === String(this.userInfo.id)) return

      // 免打扰会话：不弹横幅、不发系统通知；未读角标仍然累加，用户点开能看到
      if (isMuted(loadChatPrefs(), chatId)) return

      // 桌面系统通知
      this.tryNotify(data)

      // 正在查看该会话则不弹横幅（避免打扰）
      const cur = getObject('currentChatWindow')
      if (cur && cur.chatId != null && String(cur.chatId) === String(chatId)) return

      const sender = data.sender || {}
      this.banner = {
        show: true,
        chatId: chatId,
        chatType: data.chatType || 1,
        senderName: sender.username || data.senderName || (data.chatType === 2 ? '群聊消息' : '新消息'),
        avatar: sender.avatar || data.avatar || '',
        content: (data.message || '').toString().slice(0, 60),
        time: new Date().toTimeString().slice(0, 5)
      }
      clearTimeout(this.bannerTimer)
      this.bannerTimer = setTimeout(() => {
        this.banner.show = false
      }, 4200)
    },
    // 桌面通知（页面未聚焦时）
    tryNotify(data) {
      if (!this.notifyEnabled) return
      if (!('Notification' in window)) return
      if (Notification.permission !== 'granted') return
      if (document.hasFocus && document.hasFocus()) return
      const sender = data.sender || {}
      const name = sender.username || data.senderName || '新消息'
      const body = (data.message || '').toString().slice(0, 80)
      try {
        new Notification(name, { body, icon: '/icon-192.png', tag: 'xg-chat' })
      } catch (e) { /* 忽略通知异常 */ }
    },
    toggleNotification() {
      if (!('Notification' in window)) {
        alert('当前浏览器不支持系统通知')
        return
      }
      if (Notification.permission === 'denied') {
        alert('系统通知已被浏览器禁用，请在浏览器设置中开启')
        return
      }
      if (Notification.permission !== 'granted') {
        Notification.requestPermission().then((perm) => {
          if (perm !== 'granted') return
          this.notifyEnabled = true
          set('notifyEnabled', '1')
          this.enableWebPush()
        })
        return
      }
      this.notifyEnabled = !this.notifyEnabled
      set('notifyEnabled', this.notifyEnabled ? '1' : '0')
      if (this.notifyEnabled) this.enableWebPush()
    },
    // Web Push：页面关闭或后台时由 Service Worker 收推送，这里完成浏览器订阅与后端登记
    async enableWebPush() {
      if (!pushSupported()) {
        ElMessage.warning('当前浏览器不支持后台推送，仅在页面打开时提醒')
        return
      }
      try {
        const r = await subscribeWebPush()
        if (r.ok) {
          ElMessage.success('后台推送已开启，关闭页面也能收到新消息')
          return
        }
        if (r.reason === 'no-vapid') {
          ElMessage.warning('后台推送未配置 VAPID 公钥，仅在页面打开时提醒')
        } else if (r.reason === 'denied') {
          ElMessage.warning('未获得通知权限，仅在页面打开时提醒')
        } else if (r.reason === 'unreachable') {
          ElMessage.warning('当前网络无法连接推送服务，仅在页面打开时提醒')
        } else if (r.reason === 'report-failed') {
          ElMessage.error('推送订阅登记失败，请稍后重试')
        }
      } catch (e) {
        ElMessage.error('后台推送开启失败：' + (e && e.message ? e.message : '未知错误'))
      }
    },
    // 点击横幅直达会话
    openBannerChat() {
      const b = this.banner
      if (b.chatId == null) return
      set('openChat', JSON.stringify({
        chatId: b.chatId,
        chatType: b.chatType || 1,
        chatName: b.senderName || '新消息',
        avatar: b.avatar || ''
      }))
      this.banner.show = false
      this.activeMenu = 'chat'
      set('activeMenu', 'chat')
      // 携带时间戳 query：已在 /chat 时也强制触发路由变化，让 Chat 页消费 openChat
      this.$router.push({ path: '/chat', query: { t: Date.now() } })
    },
    // 西瓜雨彩蛋：长按头像 500ms 触发
    pressStart() {
      if (this.reduceMotion()) {
        this.startWatermelonRain()
        return
      }
      this.longPressTimer = setTimeout(() => {
        this.startWatermelonRain()
      }, 500)
    },
    pressEnd() {
      clearTimeout(this.longPressTimer)
      this.longPressTimer = null
    },
    reduceMotion() {
      return window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches
    },
    startWatermelonRain() {
      const container = document.createElement('div')
      container.className = 'watermelon-rain'
      const n = 16
      for (let i = 0; i < n; i++) {
        const s = document.createElement('span')
        s.textContent = '🍉'
        s.style.left = (4 + Math.random() * 92) + 'vw'
        s.style.fontSize = (18 + Math.random() * 28) + 'px'
        s.style.animationDelay = (Math.random() * 0.9) + 's'
        container.appendChild(s)
      }
      document.body.appendChild(container)
      setTimeout(() => container.remove(), 3200)
    },
    async logout() {
      getWebSocketClient().close()
      await logout()
      removeToken('xigua-token')
      clear()
      router.push('/login')
    },
    handleMenuChange(menu) {
      this.activeMenu = menu
    },
    async loadMomentsUnread() {
      try {
        const uid = (this.userInfo && this.userInfo.id) || (getObject('userInfo') || {}).id
        if (!uid) return
        // 红点口径：别人对我动态的点赞 / 评论未读数
        const seen = localStorage.getItem('momentsInteractSeen')
        if (!seen) { this.unreadCount.moments = 0; return }
        const res = await momentInteractUnread({ userId: uid, sinceTime: seen })
        const data = (res && res.data) || {}
        const total = Number(data.total || 0)
        this.unreadCount.moments = (this.activeMenu === 'moments' || total <= 0) ? 0 : Math.min(total, 99)
      } catch (e) { /* 忽略：不影响主界面 */ }
    },
    async loadCallUnread() {
      try {
        const res = await getCallUnread()
        if (!res || res.code !== 200) return
        this.callUnread = Math.max(0, Number(res.data) || 0)
      } catch (e) { /* 忽略：接口失败不阻塞侧栏 */ }
    },
    clearUnread(menu) {
      this.unreadCount[menu] = 0
    },
    getWsStatusClass() {
      const wsClient = getWebSocketClient()
      const reconnectAttempts = wsClient.getReconnectAttempts()
      const maxRetries = wsClient.getMaxRetries()
      
      if (reconnectAttempts >= maxRetries) {
        return 'status-error'
      }
      
      const readyState = wsClient.getReadyState()
      if (readyState === WebSocket.OPEN) {
        return 'status-online'
      } else if (readyState === WebSocket.CONNECTING) {
        // 首次连接时显示连接中
        return reconnectAttempts === 0 ? 'status-connecting' : 'status-reconnecting'
      } else if (wsClient.isReconnecting()) {
        // 重连时显示重连中
        return 'status-reconnecting'
      } else {
        return 'status-offline'
      }
    },
    getWsStatusText() {
      const wsClient = getWebSocketClient()
      const reconnectAttempts = wsClient.getReconnectAttempts()
      const maxRetries = wsClient.getMaxRetries()
      
      if (reconnectAttempts >= maxRetries) {
        return '连接失败'
      }
      
      const readyState = wsClient.getReadyState()
      if (readyState === WebSocket.OPEN) {
        return '在线'
      } else if (readyState === WebSocket.CONNECTING) {
        // 首次连接时显示连接中
        return reconnectAttempts === 0 ? '连接中' : '重连中'
      } else if (wsClient.isReconnecting()) {
        // 重连时显示重连中
        return '重连中'
      } else {
        return '离线'
      }
    }
  }
}
</script>

<style scoped>
/* ===== 最外层容器 · iOS 桌面背景 ===== */
.app-container {
  /* 用 min-height + 内边距：窗口比卡片矮时页面滚动，而不是把卡片上下裁掉 */
  min-height: 100vh;
  height: auto;
  width: 100%;
  box-sizing: border-box;
  padding: 12px 0;
  display: flex;
  justify-content: center;
  align-items: center;
  background: var(--bg);
  font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', 'SF Pro Text', 'PingFang SC', 'Helvetica Neue', 'Microsoft YaHei', sans-serif;
  -webkit-font-smoothing: antialiased;
}

/* ===== 应用包装层 ===== */
.app-wrapper {
  height: auto;
  background: transparent;
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 100%;
  max-width: 1200px;
}

/* ===== 主体窗口 · iOS 卡片质感 ===== */
.wechat-app {
  width: 100%;
  max-width: 1200px;
  height: clamp(420px, calc(100vh - 100px), 650px);
  margin-top: 20px;
  background: var(--card);
  display: flex;
  border-radius: 22px;
  overflow: hidden;
  box-shadow: var(--shadow-lg);
  border: 1px solid var(--separator);
}

/* ===== 菜单侧栏 · iOS 毛玻璃 ===== */
.menu-bar {
  width: 72px;
  min-width: 72px;
  background: var(--bg-blur-strong);
  -webkit-backdrop-filter: saturate(180%) blur(28px);
  backdrop-filter: saturate(180%) blur(28px);
  border-right: 1px solid var(--separator);
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 18px 0 12px;
  z-index: 5;
}

.user-avatar {
  width: 40px;
  height: 40px;
  margin-bottom: 20px;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.14), 0 0 0 0.5px rgba(0, 0, 0, 0.04);
}

.user-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.menu-item {
  width: 100%;
  height: 46px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: var(--label-secondary);
  position: relative;
  transition: background-color 0.35s cubic-bezier(0.32, 0.72, 0, 1), transform 0.25s cubic-bezier(0.34, 1.56, 0.64, 1);
  border-radius: 8px;
}

.menu-item:hover {
  background: var(--fill);
}

.menu-item:hover img {
  transform: scale(1.14);
  filter: brightness(0) opacity(0.75);
}

.menu-item img {
  width: 24px;
  height: 24px;
  filter: brightness(0) opacity(0.55);
  transition: filter 0.35s cubic-bezier(0.32, 0.72, 0, 1), transform 0.25s cubic-bezier(0.34, 1.56, 0.64, 1);
}

/* 朋友圈入口（SVG 图标）与菜单图标一致 */
.menu-item svg {
  width: 24px;
  height: 24px;
  color: var(--label-secondary);
  transition: transform 0.25s cubic-bezier(0.34, 1.56, 0.64, 1), color 0.3s ease;
}
.menu-item:hover svg {
  transform: scale(1.14);
  color: var(--label);
}
.menu-item.active svg {
  color: var(--accent);
  transform: scale(1.08);
}

/* 手机版入口（emoji 图标）悬停反馈 */
.menu-item.mobile-entry {
  font-size: 17px;
  line-height: 1;
}

.menu-item.mobile-entry:hover {
  transform: scale(1.15);
}

.menu-item.active {
  background: rgba(0, 122, 255, 0.12);
}

.menu-item.active::before {
  content: '';
  position: absolute;
  left: 0;
  width: 3px;
  height: 20px;
  background: #007aff;
  border-radius: 0 3px 3px 0;
  box-shadow: 0 0 8px rgba(0, 122, 255, 0.4);
}

.menu-item.active img {
  filter: brightness(0) opacity(0.85);
  transform: scale(1.08);
}

.menu-badge {
  position: absolute;
  top: 5px;
  right: 9px;
  background: #ff3b30;
  color: #fff;
  font-size: 10px;
  font-weight: 600;
  line-height: 1.4;
  border-radius: 999px;
  padding: 1px 6px;
  min-width: 16px;
  box-shadow: 0 1px 3px rgba(255, 59, 48, 0.4);
  animation: badgePulse 2.4s ease-in-out infinite;
  transform-origin: center;
}

@keyframes badgePulse {
  0%, 100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.12);
  }
}

/* 收到新消息瞬间：角标弹跳（由横幅出现触发） */
.badge-bounce .menu-badge {
  animation: badgeBounce 0.7s cubic-bezier(0.34, 1.56, 0.64, 1);
}

@keyframes badgeBounce {
  0% {
    transform: scale(0.6);
  }
  55% {
    transform: scale(1.35);
  }
  100% {
    transform: scale(1);
  }
}

@media (prefers-reduced-motion: reduce) {
  .menu-badge,
  .menu-item,
  .menu-item img {
    animation: none !important;
    transform: none !important;
  }
}

.settings {
  position: relative;
}

/* ===== 在线状态 ===== */
.status-indicator {
  margin-top: auto;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 10px 0;
}

.status-dot {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  margin-bottom: 4px;
  transition: all 0.3s ease;
}

.status-online {
  background: #34c759;
  box-shadow: 0 0 10px rgba(52, 199, 89, 0.55);
}

.status-connecting {
  background: #007aff;
  box-shadow: 0 0 10px rgba(0, 122, 255, 0.55);
  animation: pulse 1.5s infinite;
}

.status-reconnecting {
  background: #ff9500;
  box-shadow: 0 0 10px rgba(255, 149, 0, 0.55);
  animation: pulse 1.5s infinite;
}

.status-offline {
  background: #8e8e93;
  box-shadow: 0 0 10px rgba(142, 142, 147, 0.5);
}

.status-error {
  background: #ff3b30;
  box-shadow: 0 0 10px rgba(255, 59, 48, 0.55);
  animation: pulse 1s infinite;
}

.status-text {
  font-size: 10px;
  color: var(--label-secondary);
  text-align: center;
  font-weight: 500;
}

@keyframes pulse {
  0% {
    transform: scale(1);
    opacity: 1;
  }
  50% {
    transform: scale(1.25);
    opacity: 0.75;
  }
  100% {
    transform: scale(1);
    opacity: 1;
  }
}

/* 设置下拉菜单 · iOS 深色菜单
   浮层必须自己"立"起来：原先用 --bg-blur-strong，玻璃主题下只有约 28% 不透明度，
   底层页面文字会直接透上来和菜单文字叠在一起（首页更新日志那几张卡片尤其明显）。
   这里改成高不透明底色，backdrop-filter 只作为增强，blur 失效也不会影响可读性。 */
.settings-menu {
  /* 齿轮菜单通过 Teleport 挂到 body：菜单宽度超过 72px 侧栏，
     留在 .menu-item 内会被主内容区的合成层盖住导致“能点中但看不见”。 */
  position: fixed;
  /* 由 openSettingsMenu 按齿轮按钮位置写入 left/bottom，向上展开。 */
  transform: none;
  width: 204px;
  max-width: calc(100vw - 76px);
  background: rgba(252, 252, 253, 0.97);
  -webkit-backdrop-filter: saturate(180%) blur(24px);
  backdrop-filter: saturate(180%) blur(24px);
  border-radius: 14px;
  padding: 6px;
  z-index: 1000;
  box-shadow:
    0 12px 40px rgba(19, 45, 83, 0.18),
    0 2px 8px rgba(19, 45, 83, 0.08),
    inset 0 1px 0 rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(60, 60, 67, 0.14);
  display: block;
  opacity: 1;
  visibility: visible;
}

html[data-theme="dark"] .settings-menu {
  background: rgba(30, 31, 34, 0.97);
  border-color: rgba(120, 122, 130, 0.42);
  box-shadow:
    0 12px 40px rgba(0, 0, 0, 0.62),
    0 2px 8px rgba(0, 0, 0, 0.36),
    inset 0 1px 0 rgba(255, 255, 255, 0.08);
}

.settings-group {
  border-radius: 10px;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.5);
}

html[data-theme="dark"] .settings-group {
  background: rgba(255, 255, 255, 0.04);
}

.settings-group + .settings-group {
  margin-top: 6px;
}

.setting-row {
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 12px 14px;
  color: var(--label);
  font-size: 14px;
  font-weight: 500;
  text-align: left;
  cursor: pointer;
  transition: background 0.2s ease;
  border-radius: 10px;
}

.setting-row:hover {
  background: rgba(120, 120, 128, 0.12);
}

.setting-icon {
  width: 18px;
  height: 18px;
  flex-shrink: 0;
  color: var(--label-secondary);
}

.setting-label {
  line-height: 1;
  flex: 1;
}

.setting-row.logout {
  justify-content: center;
  color: var(--accent-red);
  font-weight: 600;
}
.setting-row.logout:hover {
  background: var(--fill);
}

.setting-value {
  font-size: 12px;
  color: var(--label-secondary);
}
.setting-link-arrow {
  width: 14px;
  height: 14px;
  flex-shrink: 0;
  color: var(--label-secondary);
}

/* 玻璃主题浮层 */
.xg-glass-pop {
  position: fixed;
  inset: 0;
  z-index: 3000;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.28);
  backdrop-filter: blur(6px);
  -webkit-backdrop-filter: blur(6px);
}

.xg-glass-pop-panel {
  width: 360px;
  max-height: 82vh;
  overflow-y: auto;
  padding: 8px 18px 18px;
  border-radius: 24px;
  background: var(--card);
  backdrop-filter: blur(24px) saturate(1.6);
  -webkit-backdrop-filter: blur(24px) saturate(1.6);
  border: 1px solid var(--separator);
  box-shadow:
    0 24px 64px rgba(19, 45, 83, 0.22),
    inset 0 1px 0 rgba(255, 255, 255, 0.55);
}

.xg-glass-pop-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 4px 4px;
  font-size: 17px;
  font-weight: 700;
  color: var(--label);
}

.xg-glass-pop-close {
  width: 28px;
  height: 28px;
  border: none;
  border-radius: 50%;
  font-size: 14px;
  line-height: 1;
  color: var(--label-secondary);
  background: var(--fill);
  cursor: pointer;
}

.glass-fade-enter-active,
.glass-fade-leave-active {
  transition: opacity 0.25s ease;
}

.glass-fade-enter-from,
.glass-fade-leave-to {
  opacity: 0;
}

/* iOS 开关 */
.ios-switch {
  position: relative;
  width: 38px;
  height: 22px;
  border-radius: 11px;
  background: var(--fill-strong);
  transition: background 0.28s cubic-bezier(0.32, 0.72, 0, 1);
  flex-shrink: 0;
  cursor: pointer;
}
.ios-switch.on {
  background: var(--accent-green);
}
.switch-knob {
  position: absolute;
  top: 2px;
  left: 2px;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: #fff;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.25);
  transition: transform 0.28s cubic-bezier(0.32, 0.72, 0, 1);
}
.ios-switch.on .switch-knob {
  transform: translateX(16px);
}

/* 展开动效：入场只做位移/缩放，opacity 恒为 1。
   从 opacity:0 起步的入场动画在 rAF 冻结时会停在第一帧（整块菜单看不到）。 */
.menu-pop-enter-active,
.menu-pop-leave-active {
  transition: transform 0.22s cubic-bezier(0.34, 1.56, 0.64, 1);
  transform-origin: left center;
}
.menu-pop-enter-from,
.menu-pop-leave-to {
  opacity: 1;
  transform: translateX(10px) scale(0.92);
}

/* ===== 内容区域 ===== */
.content-area {
  flex: 1;
  min-width: 0;
  background: var(--bg);
  display: flex;
  overflow: hidden;
}

.router-content {
  flex: 1;
  width: 100%;
  height: 100%;
  overflow: auto;
}

/* 路由页不做位移入场：transform 会成为 fixed 浮层的包含块，
   并让整页进入独立合成层，群管理这类全屏页会被合成器漏绘。 */

/* ===== 底部作者信息 · iOS 次级文字 ===== */
.footer-info {
  flex-shrink: 0;
  padding: 10px 0 4px;
  font-size: 12.5px;
  color: var(--label-tertiary);
  text-align: center;
  font-weight: 500;
}

.footer-info a {
  color: var(--label-secondary);
  text-decoration: none;
}

.email {
  color: var(--label-secondary);
  text-decoration: none;
  font-weight: 500;
}

.email:hover {
  color: var(--accent);
}

.divider {
  margin: 0 8px;
  color: var(--label-tertiary);
}

.group-id {
  font-weight: 600;
  color: var(--label-secondary);
}

.group-hover {
  position: relative;
  cursor: pointer;
}

/* 悬浮卡片 · iOS 质感 */
.group-pop {
  position: absolute;
  bottom: 28px;
  left: 50%;
  transform: translateX(-50%) translateY(6px);
  width: 148px;
  padding: 10px;
  background: var(--bg-blur-strong);
  -webkit-backdrop-filter: blur(20px);
  backdrop-filter: blur(20px);
  border-radius: 14px;
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.18);
  opacity: 0;
  pointer-events: none;
  transition: all 0.25s cubic-bezier(0.32, 0.72, 0, 1);
  z-index: 10;
}

.group-pop img {
  width: 100%;
  border-radius: 8px;
}

.pop-tip {
  margin-top: 6px;
  font-size: 12px;
  color: var(--label-secondary);
}

.group-hover:hover .group-pop {
  opacity: 1;
  transform: translateX(-50%) translateY(0);
  pointer-events: auto;
}

/* ===== 项目链接 ===== */
.project-link {
  display: inline-flex;
  align-items: center;
  color: var(--label-secondary);
  text-decoration: none;
  font-weight: 500;
  transition: all 0.25s cubic-bezier(0.32, 0.72, 0, 1);
}

.project-link:hover {
  color: var(--accent);
}

.project-icon {
  width: 15px;
  height: 15px;
  margin-right: 4px;
  vertical-align: middle;
}

/* ===== 新消息 iOS 毛玻璃横幅 ===== */
.message-banner {
  position: fixed;
  top: 18px;
  right: 18px;
  z-index: 9999;
  display: flex;
  align-items: center;
  gap: 10px;
  max-width: 320px;
  padding: 10px 14px;
  border-radius: 14px;
  background: color-mix(in srgb, var(--card) 78%, transparent);
  backdrop-filter: blur(20px) saturate(1.6);
  -webkit-backdrop-filter: blur(20px) saturate(1.6);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.14), inset 0 0 0 0.5px rgba(120, 120, 128, 0.2);
  cursor: pointer;
}

.banner-avatar {
  width: 38px;
  height: 38px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
  background: var(--fill);
}

.banner-avatar-text {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 15px;
  font-weight: 600;
  color: #fff;
  background: linear-gradient(135deg, #34c759, #007aff);
}

.banner-body {
  min-width: 0;
}

.banner-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.banner-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--label);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.banner-time {
  font-size: 11px;
  color: var(--label-tertiary);
  flex-shrink: 0;
}

.banner-content {
  margin-top: 2px;
  font-size: 12.5px;
  color: var(--label-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.banner-slide-enter-active,
.banner-slide-leave-active {
  transition: transform 0.5s cubic-bezier(0.34, 1.3, 0.64, 1), opacity 0.4s ease;
}

.banner-slide-enter-from,
.banner-slide-leave-to {
  transform: translateX(120%);
  opacity: 0;
}

/* ===== 移动端 ===== */
@media (max-width: 768px) {
  /* 全屏卡片：容器不再留内外边距，且隐藏卡片外的作者信息，避免 100vh 溢出被 body 裁掉 */
  .app-container {
    padding: 0;
  }

  .wechat-app {
    height: 100vh;
    margin-top: 0;
    border-radius: 0;
  }

  .footer-info {
    display: none;
    padding: 10px;
    font-size: 12px;
  }
}
</style>