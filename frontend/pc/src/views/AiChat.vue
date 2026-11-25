<template>
  <div class="ai-chat-container">
    <!-- 左侧AI会话列表 -->
    <div class="chat-sidebar">
      <div class="sidebar-header">
        <button class="new-chat-btn" @click="newChat">
          <i class="icon-plus">+</i> 开启新对话
        </button>
      </div>
      
      <!-- 会话列表 -->
      <div class="chat-list" @click="closeContextMenu" @contextmenu="cancelRename">
        <!-- 有会话数据时显示会话列表 -->
        <template v-if="hasChats">
          <!-- 今天的会话 -->
          <div class="chat-section" v-if="todayChats.length > 0">
            <h3 class="section-title">今天</h3>
            <div v-for="chat in todayChats" :key="chat.id" class="chat-item" :class="{ active: selectedChatId === chat.id }" @click="onChatItemClick(chat)">
              <template v-if="renamingChatId === chat.id">
                <input class="rename-input" v-model="renameInput" @keydown.enter="confirmRename(chat)" @keydown.esc="cancelRename" @click.stop maxlength="15" autofocus />
              </template>
              <template v-else>
                <span class="chat-title">{{ chat.title }}</span>
                <span v-if="chat.model" class="chat-model-tag" :class="modelTagClass(chat.model)">{{ modelLabel(chat.model) }}</span>
                <span class="chat-more-btn" :class="{ show: selectedChatId === chat.id }" @click.stop="toggleContextMenu(chat.id)">...</span>
              </template>
              <div v-if="contextMenuChatId === chat.id" class="chat-context-menu" @click.stop>
                <div class="context-menu-item" @click="startRename(chat)">重命名</div>
                <div class="context-menu-item danger" @click="handleDeleteChat(chat)">删除</div>
              </div>
            </div>
          </div>

          <!-- 昨天的会话 -->
          <div class="chat-section" v-if="yesterdayChats.length > 0">
            <h3 class="section-title">昨天</h3>
            <div v-for="chat in yesterdayChats" :key="chat.id" class="chat-item" :class="{ active: selectedChatId === chat.id }" @click="onChatItemClick(chat)">
              <template v-if="renamingChatId === chat.id">
                <input class="rename-input" v-model="renameInput" @keydown.enter="confirmRename(chat)" @keydown.esc="cancelRename" @click.stop maxlength="15" />
              </template>
              <template v-else>
                <span class="chat-title">{{ chat.title }}</span>
                <span v-if="chat.model" class="chat-model-tag" :class="modelTagClass(chat.model)">{{ modelLabel(chat.model) }}</span>
                <span class="chat-more-btn" :class="{ show: selectedChatId === chat.id }" @click.stop="toggleContextMenu(chat.id)">...</span>
              </template>
              <div v-if="contextMenuChatId === chat.id" class="chat-context-menu" @click.stop>
                <div class="context-menu-item" @click="startRename(chat)">重命名</div>
                <div class="context-menu-item danger" @click="handleDeleteChat(chat)">删除</div>
              </div>
            </div>
          </div>

          <!-- 最近7天的会话 -->
          <div class="chat-section" v-if="last7DaysChats.length > 0">
            <h3 class="section-title">最近7天</h3>
            <div v-for="chat in last7DaysChats" :key="chat.id" class="chat-item" :class="{ active: selectedChatId === chat.id }" @click="onChatItemClick(chat)">
              <template v-if="renamingChatId === chat.id">
                <input class="rename-input" v-model="renameInput" @keydown.enter="confirmRename(chat)" @keydown.esc="cancelRename" @click.stop maxlength="15" />
              </template>
              <template v-else>
                <span class="chat-title">{{ chat.title }}</span>
                <span v-if="chat.model" class="chat-model-tag" :class="modelTagClass(chat.model)">{{ modelLabel(chat.model) }}</span>
                <span class="chat-more-btn" :class="{ show: selectedChatId === chat.id }" @click.stop="toggleContextMenu(chat.id)">...</span>
              </template>
              <div v-if="contextMenuChatId === chat.id" class="chat-context-menu" @click.stop>
                <div class="context-menu-item" @click="startRename(chat)">重命名</div>
                <div class="context-menu-item danger" @click="handleDeleteChat(chat)">删除</div>
              </div>
            </div>
          </div>

          <!-- 按月份分组的会话 -->
          <div v-if="monthlyChatGroups.length > 0">
            <div v-for="[month, chats] in monthlyChatGroups" :key="month" class="chat-section">
              <h3 class="section-title">{{ month }}</h3>
              <div v-for="chat in chats" :key="chat.id" class="chat-item" :class="{ active: selectedChatId === chat.id }" @click="onChatItemClick(chat)">
                <template v-if="renamingChatId === chat.id">
                  <input class="rename-input" v-model="renameInput" @keydown.enter="confirmRename(chat)" @keydown.esc="cancelRename" @click.stop maxlength="15" />
                </template>
                <template v-else>
                  <span class="chat-title">{{ chat.title }}</span>
                  <span v-if="chat.model" class="chat-model-tag" :class="modelTagClass(chat.model)">{{ modelLabel(chat.model) }}</span>
                  <span class="chat-more-btn" :class="{ show: selectedChatId === chat.id }" @click.stop="toggleContextMenu(chat.id)">...</span>
                </template>
                <div v-if="contextMenuChatId === chat.id" class="chat-context-menu" @click.stop>
                  <div class="context-menu-item" @click="startRename(chat)">重命名</div>
                  <div class="context-menu-item danger" @click="handleDeleteChat(chat)">删除</div>
                </div>
              </div>
            </div>
          </div>
        </template>
        
        <!-- 无会话数据时显示提示信息 -->
        <div class="empty-chat-list" v-else>
          <div class="empty-chat-art">
            <svg viewBox="0 0 56 56" fill="none" aria-hidden="true">
              <defs>
                <linearGradient id="aiSideGrad" x1="0" y1="0" x2="1" y2="1">
                  <stop offset="0" stop-color="#af52de" stop-opacity="0.14"/>
                  <stop offset="1" stop-color="#007aff" stop-opacity="0.06"/>
                </linearGradient>
              </defs>
              <circle cx="28" cy="28" r="24" fill="url(#aiSideGrad)"/>
              <rect x="9" y="13" width="28" height="20" rx="9" style="fill: var(--card)" stroke="currentColor" stroke-width="2"/>
              <path d="M9 38l6-4" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
              <circle cx="18" cy="23" r="2" fill="currentColor"/>
              <circle cx="26" cy="23" r="2" fill="currentColor"/>
              <path d="M38 40l9 3-2.5-6.5" stroke="#ff9500" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </div>
          <p class="empty-chat-text">暂无会话记录</p>
          <p class="empty-chat-subtext">点击「开启新对话」开始与AI聊天</p>
        </div>
      </div>
      
      <!-- 底部机器人信息 -->
      <div class="sidebar-footer">
        <div class="bot-info" @click="toggleBotDropdown">
          <img :src="selectedBot?.avatar || '@/static/images/user_default.png'" alt="机器人头像" class="bot-avatar">
          <span class="bot-name">{{ selectedBot?.name || 'DeepSeek' }}</span>
          <i class="icon-dropdown">▼</i>
        </div>
        
        <!-- 机器人下拉菜单 -->
        <div v-if="showBotDropdown" class="bot-dropdown">
          <div class="dropdown-header">选择机器人</div>
          <div 
            v-for="bot in bots" 
            :key="bot.id" 
            class="bot-dropdown-item" 
            :class="{ active: selectedBot?.id === bot.id }"
            @click="switchBot(bot)"
          >
            <img :src="bot.avatar || '@/static/images/user_default.png'" alt="机器人头像" class="dropdown-bot-avatar">
            <span class="dropdown-bot-name">{{ bot.name }}</span>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 中间聊天区域 -->
    <div class="chat-main">
      <!-- 顶部标题 -->
      <div class="chat-header" v-if="selectedChatId">
        <h2 class="chat-title">{{ selectedChat?.title || '未命名会话' }}</h2>
      </div>

      <!-- 聊天内容区域 -->
      <div class="chat-content" v-if="selectedChatId" ref="chatContent" @scroll.passive="handleHistoryScroll">
        <div
          class="history-control"
          v-if="historyPageNum < historyTotalPage || historyLoading"
        >
          <button
            class="history-load-more"
            v-if="!historyLoading"
            type="button"
            @click="loadMoreHistory"
          >
            加载更早消息
          </button>
          <div class="history-loading" v-else>正在加载更早消息...</div>
        </div>

        <!-- 聊天消息 -->
        <div
          class="message"
          v-for="msg in selectedChat.messages"
          :key="msg.messageId || msg._localId"
          :data-message-id="msg.messageId || msg._localId"
          :class="msg.type"
        >
          <div v-if="msg.type === 'user'" class="user-message">
            <div class="message-bubble user-bubble">
              <p>{{ msg.content }}</p>
            </div>
          </div>
          <div v-else-if="msg.type === 'ai'" class="ai-message">
            <div class="message-bubble ai-bubble">
              <div v-if="!msg.content && msg.statusText" class="status-indicator">
                <div class="status-dots">
                  <span></span><span></span><span></span>
                </div>
                <span class="status-text">{{ msg.statusText }}</span>
              </div>
              <div v-else-if="!msg.content" class="thinking-dots">
                <span></span><span></span><span></span>
              </div>
              <div v-else-if="isImageMessage(msg)" class="ai-image-bubble">
                <img :src="imageUrlOf(msg)" alt="AI 绘画" class="ai-image" @load="scrollToBottom">
                <p v-if="msg.prompt" class="ai-image-prompt">{{ msg.prompt }}</p>
              </div>
              <div v-else class="ai-markdown" v-html="renderMarkdown(msg.content)"></div>
            </div>
          </div>
          <div v-else-if="msg.type === 'error'" class="error-message">
            <div class="message-bubble error-bubble">
              <p>{{ msg.content }}</p>
            </div>
          </div>
        </div>
      </div>
      
      <!-- 无会话时的空状态 -->
      <div class="empty-state" v-else>
        <div class="empty-content">
          <div class="empty-icon-container">
            <div class="ai-icon">
              <svg class="empty-art" viewBox="0 0 80 80" fill="none" aria-hidden="true">
                <defs>
                  <radialGradient id="aiCoreGrad" cx="0.5" cy="0.5" r="0.5">
                    <stop offset="0" stop-color="#af52de" stop-opacity="0.3"/>
                    <stop offset="1" stop-color="#af52de" stop-opacity="0"/>
                  </radialGradient>
                </defs>
                <circle cx="40" cy="40" r="34" fill="url(#aiCoreGrad)"/>
                <path d="M40 11l4.8 14.2L59 30l-14.2 4.8L40 49l-4.8-14.2L21 30l14.2-4.8L40 11z" fill="#af52de" opacity="0.9"/>
                <path d="M58 50l2.5 7.5L68 60l-7.5 2.5L58 70l-2.5-7.5L48 60l7.5-2.5L58 50z" fill="#ff9500" opacity="0.9"/>
                <path d="M20 54l2 6 6 2-6 2-2 6-2-6-6-2 6-2 2-6z" fill="#007aff" opacity="0.9"/>
              </svg>
            </div>
          </div>
          <h2>今天有什么可以帮到你?</h2>
          <div class="empty-input-wrap">
            <!-- 模型供应商选择（iOS 分段控件） -->
            <div class="model-segment" role="tablist" aria-label="选择模型供应商">
              <button v-for="p in providerOptions" :key="p.value" type="button" class="segment-item" :class="{ active: currentProvider === p.value }" role="tab" :aria-selected="currentProvider === p.value" @click="switchProvider(p.value)">{{ p.label }}</button>
            </div>
            <div class="empty-input-container">
              <input type="text" v-model="messageInput" placeholder="发送消息，或描述想画的画面" class="empty-input-field" @keydown.enter="sendMessage">
              <button class="paint-btn" @click="generateImage" :disabled="isGeneratingImage" title="AI 绘画" aria-label="AI 绘画">
                <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
                  <path d="M18.37 3.63L21 6.26 8.5 18.75 5.5 19.5l.75-3L18.37 3.63z" fill="currentColor"/>
                  <path d="M13 8l3 3" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"/>
                </svg>
              </button>
              <button class="send-btn" @click="sendMessage">
                <img src="@/static/icons/send.png" alt="发送" class="send-icon">
              </button>
            </div>
          </div>
          <div class="empty-footer">
            <p>生成的内容可能包含不准确信息，请验证重要信息。</p>
          </div>
        </div>
      </div>
      
      <!-- 输入区域 -->
      <div class="chat-input-area" v-if="selectedChatId">
        <div class="model-segment" role="tablist" aria-label="选择模型供应商">
          <button v-for="p in providerOptions" :key="p.value" type="button" class="segment-item" :class="{ active: currentProvider === p.value }" role="tab" :aria-selected="currentProvider === p.value" @click="switchProvider(p.value)">{{ p.label }}</button>
        </div>
        <div class="input-container">
          <input type="text" v-model="messageInput" placeholder="发送消息，或描述想画的画面..." class="chat-input" @keydown.enter="sendMessage">
          <button class="paint-btn" @click="generateImage" :disabled="isGeneratingImage" title="AI 绘画" aria-label="AI 绘画">
            <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
              <path d="M18.37 3.63L21 6.26 8.5 18.75 5.5 19.5l.75-3L18.37 3.63z" fill="currentColor"/>
              <path d="M13 8l3 3" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"/>
            </svg>
          </button>
          <button v-if="isStreaming" class="stop-btn" @click="stopStreaming" title="停止生成" aria-label="停止生成">
            <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
              <rect x="7" y="7" width="10" height="10" rx="2.5" fill="currentColor"/>
            </svg>
          </button>
          <button v-else class="send-btn" @click="sendMessage">
            <img src="@/static/icons/send.png" alt="发送" class="send-icon">
          </button>
        </div>
        <div class="input-footer">
          <p>生成的内容可能包含不准确信息，请验证重要信息。</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import {get, set, remove, getObject} from '@/utils/localStorage.js'
import { getBots } from '@/api/bot.js'
import { createSession, pageSessions, streamChatSSE, getHistoryMessages, deleteSession, renameSession, updateSessionModel, generateImage } from '@/api/ai_session.js'
import { ElMessage } from '@/plugins/message'
import { marked } from 'marked'
import DOMPurify from 'dompurify'

// Markdown 渲染配置（一次初始化）
marked.setOptions({
  breaks: true,
  gfm: true
})

export default {
  name: 'AiChat',
  beforeUnmount() {
    if (this.historyScrollThrottleTimer) {
      clearTimeout(this.historyScrollThrottleTimer)
      this.historyScrollThrottleTimer = null
    }
    document.removeEventListener('mousedown', this._renameOutsideClick)
  },
  data() {
    return {
      messageInput: '',
      // 会话数据
      todayChats: [],
      yesterdayChats: [],
      last7DaysChats: [],
      monthlyChats: {},
      // 选中的会话
      selectedChat: null,
      // 选中的机器人信息
      selectedBot: {
        name: 'DeepSeek',
        avatar: '@/static/images/user_default.png'
      },
      // 当前选择的模型供应商
      currentProvider: get('aiProvider') || 'zhipu',
      // 可切换的模型供应商列表（新增供应商：后端 Nacos 配置对应客户端 + 此处加选项）
      providerOptions: [
        { value: 'zhipu', label: '智谱 GLM', model: 'glm-4-flash' },
        { value: 'deepseek', label: 'DeepSeek', model: 'deepseek-chat' }
      ],
      // 机器人列表
      bots: [],
      // 机器人下拉菜单显示状态
      showBotDropdown: false,
      // 避免非法session提示重复弹出
      invalidSessionWarned: false,
      // 历史消息分页状态（用于上拉加载更多）
      historyPageSize: 20,
      historyPageNum: 0,
      historyTotalPage: 1,
      historyLoading: false,
      // 滚动加载节流与滚动位置状态
      historyScrollThrottleTimer: null,
      pendingScrollToBottom: true,
      // 历史锚点定位（减少分页插入时的抖动）
      historyAnchorMessageId: null,
      // 本地消息唯一ID序列
      localMessageSeq: 0,
      // 会话右键菜单
      contextMenuChatId: null,
      // 正在重命名的会话
      renamingChatId: null,
      renameInput: '',
      // 是否正在流式生成
      isStreaming: false,
      // 当前流的取消控制器
      streamController: null,
      // 是否正在生成绘画
      isGeneratingImage: false
    }
  },
  async created() {
    // 检查是否有存储的机器人信息
    this.loadStoredBotInfo()
    // 先加载机器人列表，再根据路由参数初始化当前机器人
    await this.loadBots()

    const routeBotId = this.$route.params.botId
    if (!this.applyRouteBot(routeBotId)) {
      return
    }

    // 清理openChat参数（不自动选中会话）
    this.checkBotChatParams(routeBotId)
    // 进入页面默认不选中会话，展示空状态
    this.selectedChat = null
    // 加载会话列表
    await this.loadSessions()
    await this.openRouteSessionIfNeeded()
  },
  watch: {
    '$route.params.botId': async function (newBotId, oldBotId) {
      if (!newBotId || newBotId === oldBotId) {
        return
      }

      if (!this.bots.length) {
        await this.loadBots()
      }

      if (!this.applyRouteBot(newBotId)) {
        return
      }

      this.selectedChat = null
      this.invalidSessionWarned = false
      await this.loadSessions()
      await this.openRouteSessionIfNeeded()
    },
    '$route.params.sessionId': async function (newSessionId, oldSessionId) {
      if (!newSessionId || newSessionId === oldSessionId) {
        return
      }
      await this.openRouteSessionIfNeeded()
    }
  },
  computed: {
    selectedChatId() {
      return this.selectedChat ? this.selectedChat.id : null
    },
    hasChats() {
      // 检查是否有今天、昨天或最近7天的会话
      const hasRecentChats = this.todayChats.length > 0 || this.yesterdayChats.length > 0 || this.last7DaysChats.length > 0;
      
      // 检查是否有按月份分组的会话
      const hasMonthlyChats = Object.values(this.monthlyChats).some(chats => Array.isArray(chats) && chats.length > 0);
      
      return hasRecentChats || hasMonthlyChats;
    },
    // 计算属性，返回按月份分组的会话，确保模板能够正确更新
    monthlyChatGroups() {
      return Object.entries(this.monthlyChats).filter(([_, chats]) => Array.isArray(chats) && chats.length > 0);
    }
  },
  methods: {
    // 将 AI 回复渲染为安全的 Markdown HTML
    renderMarkdown(text) {
      if (!text) return ''
      try {
        const rawHtml = marked.parse(text)
        return DOMPurify.sanitize(rawHtml, { ADD_ATTR: ['target'] })
      } catch (e) {
        console.warn('Markdown 渲染失败，退回纯文本', e)
        return text
      }
    },

    // 是否为 AI 绘画图片消息（content 以 img: 前缀开头）
    isImageMessage(msg) {
      return Boolean(msg && typeof msg.content === 'string' && msg.content.startsWith('img:'))
    },

    // 提取图片消息中的真实 URL
    imageUrlOf(msg) {
      if (!msg || typeof msg.content !== 'string') return ''
      return msg.content.startsWith('img:') ? msg.content.slice(4) : ''
    },

    // AI 绘画：文生图（后端生成 → MinIO 上传 → img: 消息入库）
    async generateImage() {
      if (this.isGeneratingImage || this.isStreaming) return
      const prompt = this.messageInput.trim()
      if (!prompt) {
        ElMessage.warning('请输入想画的画面描述')
        return
      }
      const userInfo = getObject('userInfo')
      this.isGeneratingImage = true
      try {
        const res = await generateImage({
          userId: userInfo?.id || '1',
          botId: this.selectedBot?.id || '1',
          sessionId: this.selectedChat?.id ? String(this.selectedChat.id) : undefined,
          prompt
        })
        if (res.code !== 200 || !res.data?.url) {
          throw new Error('绘画失败')
        }
        const { sessionId, url } = res.data

        // 无会话时自动接入返回的新会话
        if (!this.selectedChat) {
          const createdChat = {
            id: sessionId,
            title: prompt,
            messages: []
          }
          this.selectedChat = createdChat
          this.todayChats.unshift(createdChat)
          this.syncRouteSession(sessionId)
        }

        // 输入框内容已被绘画使用，清空
        this.messageInput = ''

        // 用户请求 + AI 图片消息
        if (!Array.isArray(this.selectedChat.messages)) {
          this.selectedChat.messages = []
        }
        this.selectedChat.messages.push({
          messageId: this.nextLocalMessageId(),
          type: 'user',
          content: prompt
        })
        this.selectedChat.messages.push({
          messageId: this.nextLocalMessageId(),
          type: 'ai',
          content: 'img:' + url,
          prompt
        })
        // 刚生成的图必须让人看到
        this.scrollToBottom(true)
      } catch (error) {
        console.error('AI 绘画失败:', error)
        ElMessage.error('绘画失败，请稍后重试')
      } finally {
        this.isGeneratingImage = false
      }
    },

    // 新建聊天
    newChat() {
      // 上一轮的流还没结束时，先中断，否则旧流会往新会话里写内容
      this.stopStreaming()
      // 清空当前聊天，显示空状态界面
      this.selectedChat = null
    },
    
    // 选择聊天
    async selectChat(chat) {
      // 同上：切会话前先中断在用流，避免 isStreaming 卡住导致新会话发不出消息
      this.stopStreaming()
      this.selectedChat = {
        ...chat,
        messages: []
      }
      this.pendingScrollToBottom = true
      this.resetHistoryState()
      await this.loadHistoryMessages(chat.id, { reset: true })
      this.syncRouteSession(chat.id)
    },
    
    // 切换模型供应商
    async switchProvider(provider) {
      if (this.currentProvider === provider) return
      this.currentProvider = provider
      set('aiProvider', provider)
      // 已有会话：立即切换当前会话的模型（下次回复生效）
      if (this.selectedChatId) {
        const model = this.providerModel()
        try {
          const res = await updateSessionModel(this.selectedChatId, model)
          if (res.code === 200) {
            if (this.selectedChat) this.selectedChat.model = model
            const item = this.findSessionById(this.selectedChatId)
            if (item) item.model = model
            ElMessage.success('已切换为' + this.providerLabel(provider) + '，下条回复生效')
          } else {
            ElMessage.error('切换失败，请重试')
          }
        } catch (e) {
          ElMessage.error('切换失败，请重试')
        }
      }
    },

    // 供应商显示名
    providerLabel(provider) {
      const p = this.providerOptions.find(o => o.value === provider)
      return p ? p.label : provider
    },

    // 当前供应商对应的默认模型名
    providerModel() {
      const p = this.providerOptions.find(o => o.value === this.currentProvider)
      return p ? p.model : 'glm-4-flash'
    },

    // 会话模型标签文案
    modelLabel(model) {
      if (!model) return ''
      if (model.indexOf('glm') !== -1 || model.indexOf('chatglm') !== -1) return 'GLM'
      if (model.indexOf('deepseek') !== -1) return 'DS'
      return 'AI'
    },

    // 会话模型标签样式类
    modelTagClass(model) {
      if (!model) return ''
      if (model.indexOf('glm') !== -1 || model.indexOf('chatglm') !== -1) return 'tag-glm'
      if (model.indexOf('deepseek') !== -1) return 'tag-ds'
      return ''
    },

    // 停止当前流式生成
    stopStreaming() {
      if (this.streamController) {
        this.streamController.abort()
      }
      // 立刻复位：abort 触发的是异步 catch，期间状态若停在 true，
      // 切换会话后的每一次发送都会被 isStreaming 守卫静默丢弃
      this.isStreaming = false
      this.streamController = null
    },

    // 发送消息
    async sendMessage() {
      if (this.messageInput.trim()) {
        try {
          // 已有流在生成时禁止重复发送（要给反馈，静默 return 会被当成按钮坏了）
          if (this.isStreaming) {
            ElMessage.warning('正在生成中，请等待本次回复完成')
            return
          }
          // 保存输入内容
          const inputContent = this.messageInput.trim();
          
          // 准备接口参数
          const userInfo = getObject("userInfo");
          let sessionId = this.selectedChat?.id ? String(this.selectedChat.id) : undefined;

          // 清空输入框
          this.messageInput = '';

          // 如果是首次对话，先创建会话
          if (!sessionId) {
            const createRes = await createSession({
              userId: userInfo?.id || '1',
              botId: this.selectedBot?.id || '1',
              input: inputContent,
              model: this.providerModel()
            });

            if (createRes.code !== 200 || !createRes.data?.id) {
              throw new Error('创建会话失败');
            }

            sessionId = String(createRes.data.id);
            const createdChat = {
              id: sessionId,
              title: createRes.data.title || inputContent,
              messages: []
            };
            this.selectedChat = createdChat;
            this.todayChats.unshift(createdChat);
            this.syncRouteSession(sessionId);
          }

          if (!Array.isArray(this.selectedChat.messages)) {
            this.selectedChat.messages = [];
          }

          const params = {
            userId: userInfo?.id || '1',
            botId: this.selectedBot?.id || '1',
            sessionId,
            input: inputContent
          };
          
          // 添加用户消息到聊天记录
          this.selectedChat.messages.push({
            messageId: this.nextLocalMessageId(),
            type: 'user',
            content: inputContent
          });
          
          // 添加AI消息占位符
          const aiMessageIndex = this.selectedChat.messages.length;
          this.selectedChat.messages.push({
            messageId: this.nextLocalMessageId(),
            type: 'ai',
            content: ''
          });

          // 立即滚动到底部，展示用户刚发送的消息（自己发的一定要看到）
          this.scrollToBottom(true);

          // 调用streamChatSSE接口（使用fetch API处理SSE）
          try {
            // 创建可取消的流
            this.streamController = new AbortController();
            this.isStreaming = true;
            const reader = await streamChatSSE(params, this.streamController.signal);

            const decoder = new TextDecoder();
            let accumulatedContent = '';
            let buffer = '';
            let currentEvent = '';

            const processSSELine = (line) => {
              // 解析 event: 行，记录当前事件类型
              if (line.startsWith('event:')) {
                currentEvent = line.slice(6).trim();
                return;
              }

              // 只处理 data: 行
              if (!line.startsWith('data:')) {
                return;
              }

              const data = line.slice(5);
              if (data === '[DONE]') {
                return;
              }

              if (currentEvent === 'status') {
                // status 事件：显示为更自然的状态提示
                const statusText = data === '正在识别意图...' ? '正在思考…' : data;
                this.selectedChat.messages[aiMessageIndex].statusText = statusText;
                this.$nextTick(() => {
                  this.scrollToBottom();
                });
              } else {
                // message 事件（及其他）：作为实际回答内容累加
                accumulatedContent += data;
                this.selectedChat.messages[aiMessageIndex].content = accumulatedContent;
                this.$nextTick(() => {
                  this.scrollToBottom();
                });
              }

              // 重置事件类型，防止误用
              currentEvent = '';
            };

            while (true) {
              const { done, value } = await reader.read();

              if (done) {
                // flush解码器中剩余字节
                buffer += decoder.decode();
                break;
              }

              // 使用stream模式，避免多字节字符被截断
              buffer += decoder.decode(value, { stream: true });

              // 按行处理，保留最后一段不完整行到下个chunk
              let lineEndIndex = buffer.indexOf('\n');
              while (lineEndIndex !== -1) {
                const rawLine = buffer.slice(0, lineEndIndex);
                buffer = buffer.slice(lineEndIndex + 1);

                const line = rawLine.endsWith('\r') ? rawLine.slice(0, -1) : rawLine;
                processSSELine(line);

                lineEndIndex = buffer.indexOf('\n');
              }
            }

            // 处理流结束时最后一行（可能没有换行符）
            if (buffer) {
              const lastLine = buffer.endsWith('\r') ? buffer.slice(0, -1) : buffer;
              processSSELine(lastLine);
            }

            // 流结束后清除状态提示
            this.selectedChat.messages[aiMessageIndex].statusText = '';
            this.isStreaming = false;
            this.streamController = null;
          } catch (error) {
            // 用户主动停止 / 切换会话导致的中断：保留已生成内容，不报错
            if (error && error.name === 'AbortError') {
              const abortMsg = this.selectedChat?.messages?.[aiMessageIndex];
              if (abortMsg) abortMsg.statusText = '';
              this.isStreaming = false;
              this.streamController = null;
              return;
            }
            console.error('发送消息失败:', error);
            this.isStreaming = false;
            this.streamController = null;
            // 占位气泡一个字都没生成时，直接改写成错误提示，不再追加第二条，
            // 避免留下「空白 AI 气泡 + 错误气泡」两条并排
            const aiMsg = this.selectedChat?.messages?.[aiMessageIndex];
            if (aiMsg) {
              if (!aiMsg.content) {
                aiMsg.content = '发送消息失败，请稍后重试';
                aiMsg.type = 'error';
              }
              aiMsg.statusText = '';
            } else if (this.selectedChat && this.selectedChat.messages) {
              this.selectedChat.messages.push({
                messageId: this.nextLocalMessageId(),
                type: 'error',
                content: '发送消息失败，请稍后重试'
              });
            }
          }
          
        } catch (error) {
          console.error('发送消息失败:', error);
          // 外层失败同样要把流状态收干净，否则下一次发送会被 isStreaming 静默吞掉
          this.isStreaming = false;
          this.streamController = null;
          // 添加错误消息到聊天记录
          if (this.selectedChat && this.selectedChat.messages) {
            this.selectedChat.messages.push({
              messageId: this.nextLocalMessageId(),
              type: 'error',
              content: '发送消息失败，请稍后重试'
            });
          }
        }
      } else {
        // 空输入也要给反馈，否则点发送/按回车会像“没反应”
        ElMessage.warning('请输入想发送的内容')
      }
    },

    // 检查并清理从Bot页面传递过来的机器人ID参数
    checkBotChatParams(routeBotId) {
      // 从localStorage中读取openChat参数
      const openChat = JSON.parse(get("openChat") || "{}");
      remove("openChat");

      if (!openChat.chatId) {
        return;
      }

      // 仅处理与当前路由机器人一致的openChat参数
      if (String(openChat.chatId) !== String(routeBotId)) {
        return;
      }
    },

    // 从会话列表中查找会话
    findSessionById(sessionId) {
      const allSessions = [
        ...this.todayChats,
        ...this.yesterdayChats,
        ...this.last7DaysChats,
        ...Object.values(this.monthlyChats).flat()
      ]
      return allSessions.find(item => String(item.id) === String(sessionId)) || null
    },

    // 将当前选中会话同步到路由
    syncRouteSession(sessionId) {
      const botId = this.$route.params.botId
      if (!botId || !sessionId) {
        return
      }

      const currentSessionId = this.$route.params.sessionId
      if (String(currentSessionId) === String(sessionId)) {
        return
      }

      this.$router.replace(`/ai-chat/${botId}/session/${sessionId}`)
    },

    // 非法或不存在会话时回退到机器人会话首页
    handleInvalidSessionRoute() {
      if (this.invalidSessionWarned) {
        return
      }
      this.invalidSessionWarned = true

      ElMessage.warning('会话不存在或链接无效，已返回会话列表')
      this.$router.replace(`/ai-chat/${this.$route.params.botId}`)
    },

    // 路由中存在sessionId时自动打开对应会话
    async openRouteSessionIfNeeded() {
      const routeSessionId = this.$route.params.sessionId
      if (!routeSessionId) {
        return
      }

      const isNumericSessionId = /^\d+$/.test(String(routeSessionId))
      if (!isNumericSessionId) {
        this.handleInvalidSessionRoute()
        return
      }

      const targetSession = this.findSessionById(routeSessionId)
      if (!targetSession) {
        this.handleInvalidSessionRoute()
        return
      }

      if (this.selectedChat && String(this.selectedChat.id) === String(routeSessionId)) {
        return
      }

      await this.selectChat(targetSession)
    },

    // 根据路由参数应用当前机器人，不存在则回到机器人列表
    applyRouteBot(routeBotId) {
      if (!routeBotId) {
        this.$router.replace('/bot');
        return false;
      }

      const matchedBot = this.bots.find(bot => String(bot.id) === String(routeBotId));
      if (!matchedBot) {
        this.$router.replace('/bot');
        return false;
      }

      this.selectedBot = matchedBot;
      this.storeBotInfo(matchedBot);
      // DeepSeek 人格卡：选中即自动联动 DeepSeek 官方供应商
      if (matchedBot.name && matchedBot.name.indexOf('DeepSeek') !== -1) {
        this.currentProvider = 'deepseek'
        set('aiProvider', 'deepseek')
      }
      return true;
    },
    
    // 加载存储的机器人信息
    loadStoredBotInfo() {
      const storedBot = get('currentBot');
      if (storedBot) {
        this.selectedBot = JSON.parse(storedBot);
      }
    },
    
    // 存储机器人信息到localStorage
    storeBotInfo(botInfo) {
      set('currentBot', JSON.stringify(botInfo));
    },
    
    // 加载机器人列表
    async loadBots() {
      try {
        const response = await getBots();
        if (response.code === 200 && response.data) {
          this.bots = response.data;
        } else {
          // 如果API返回但数据格式不正确，使用默认机器人
          this.useDefaultBots();
        }
      } catch (error) {
        console.error('加载机器人列表失败:', error);
        // API调用失败，使用默认机器人
        this.useDefaultBots();
      }
    },
    
    // 重置历史消息分页状态
    resetHistoryState() {
      this.historyPageNum = 0
      this.historyTotalPage = 1
      this.historyLoading = false
      this.pendingScrollToBottom = true
      this.historyAnchorMessageId = null
      if (this.historyScrollThrottleTimer) {
        clearTimeout(this.historyScrollThrottleTimer)
        this.historyScrollThrottleTimer = null
      }
    },

    // 生成本地消息唯一ID
    nextLocalMessageId() {
      this.localMessageSeq += 1
      return `local-${Date.now()}-${this.localMessageSeq}`
    },

    // 滚动到消息底部
    // force=false（默认）：只在用户本来就贴着底部时才跟随，避免流式回复把他从历史里拽回来
    // force=true：自己刚发消息 / 刚打开会话这类“必须看到最新”的场景
    scrollToBottom(force = false) {
      this.$nextTick(() => {
        const chatContent = this.$refs.chatContent
        if (!chatContent) return
        const gap = chatContent.scrollHeight - chatContent.scrollTop - chatContent.clientHeight
        if (force || gap < 120) {
          chatContent.scrollTop = chatContent.scrollHeight
        }
      })
    },

    findFirstValidTimestamp(messages) {
      for (let i = 0; i < messages.length; i += 1) {
        const ts = Number(messages[i]?.timestamp || 0)
        if (ts > 0) {
          return ts
        }
      }
      return 0
    },

    findLastValidTimestamp(messages) {
      for (let i = messages.length - 1; i >= 0; i -= 1) {
        const ts = Number(messages[i]?.timestamp || 0)
        if (ts > 0) {
          return ts
        }
      }
      return 0
    },

    // 点击加载更早消息
    async loadMoreHistory() {
      if (!this.selectedChat?.id || this.historyLoading) {
        return
      }

      if (this.historyPageNum >= this.historyTotalPage) {
        return
      }

      await this.loadHistoryMessages(this.selectedChat.id, { keepPosition: true })
    },

    // 聊天区上拉加载更早消息（节流）
    handleHistoryScroll(event) {
      const chatContent = event?.target
      if (!chatContent || !this.selectedChat?.id) {
        return
      }

      if (chatContent.scrollTop > 100) {
        return
      }

      if (this.historyLoading) {
        return
      }

      if (this.historyPageNum >= this.historyTotalPage) {
        return
      }

      if (this.historyScrollThrottleTimer) {
        return
      }

      this.historyScrollThrottleTimer = setTimeout(async () => {
        this.historyScrollThrottleTimer = null

        const latestChatContent = this.$refs.chatContent
        if (!latestChatContent || latestChatContent.scrollTop > 100) {
          return
        }

        await this.loadHistoryMessages(this.selectedChat.id, { keepPosition: true })
      }, 120)
    },

    // 加载会话历史消息（懒加载：每次仅请求下一页）
    async loadHistoryMessages(sessionId, options = {}) {
      const { reset = false, keepPosition = false } = options
      const targetSessionId = String(sessionId)

      if (!this.selectedChat || String(this.selectedChat.id) !== targetSessionId) {
        return
      }

      if (reset) {
        this.resetHistoryState()
      }

      if (this.historyLoading) {
        return
      }

      if (!reset && this.historyPageNum >= this.historyTotalPage) {
        return
      }

      const nextPageNum = this.historyPageNum + 1
      const chatContent = this.$refs.chatContent
      const firstVisibleMessageId = keepPosition && chatContent
        ? (chatContent.querySelector('.message')?.getAttribute('data-message-id') || null)
        : null
      this.historyAnchorMessageId = firstVisibleMessageId

      this.historyLoading = true

      try {
        const response = await getHistoryMessages({
          sessionId: targetSessionId,
          pageNum: nextPageNum,
          pageSize: this.historyPageSize
        })

        const data = response?.data || {}
        const rawMessages = Array.isArray(data?.rows)
          ? data.rows
          : Array.isArray(data)
            ? data
            : []

        const mappedMessages = rawMessages
          .map((item, index) => {
            const senderIsUser = String(item?.senderId) === String(getObject('userInfo')?.id)
            const typeFromRole = item?.role === 'assistant' ? 'ai' : item?.role === 'user' ? 'user' : null
            const type = typeFromRole || (senderIsUser ? 'user' : 'ai')
            const content = item?.content ?? item?.message ?? ''
            if (!content) {
              return null
            }
            return {
              messageId: item?.messageId || this.nextLocalMessageId(),
              type,
              content,
              timestamp: Number(item?.timestamp || item?.createTime || 0)
            }
          })
          .filter(Boolean)

        const responseTotalPage = Number(data?.totalPage || 0)
        this.historyTotalPage = responseTotalPage > 0 ? responseTotalPage : 1
        this.historyPageNum = nextPageNum

        if (!this.selectedChat || String(this.selectedChat.id) !== targetSessionId) {
          return
        }

        const currentMessages = Array.isArray(this.selectedChat.messages) ? this.selectedChat.messages : []
        const firstCurrentTs = this.findFirstValidTimestamp(currentMessages)
        const lastCurrentTs = this.findLastValidTimestamp(currentMessages)
        const firstNewTs = this.findFirstValidTimestamp(mappedMessages)
        const lastNewTs = this.findLastValidTimestamp(mappedMessages)

        let mergedMessages = currentMessages
        let isPrepend = false

        if (!currentMessages.length) {
          mergedMessages = mappedMessages
        } else if (!mappedMessages.length) {
          mergedMessages = currentMessages
        } else if (lastNewTs && firstCurrentTs && lastNewTs <= firstCurrentTs) {
          // 新页是更早消息，前插
          isPrepend = true
          mergedMessages = [...mappedMessages, ...currentMessages]
        } else if (firstNewTs && lastCurrentTs && firstNewTs >= lastCurrentTs) {
          // 新页是更新消息，后插
          mergedMessages = [...currentMessages, ...mappedMessages]
        } else {
          // 时间存在交叉时兜底排序去重
          const dedupMap = new Map()
          ;[...currentMessages, ...mappedMessages].forEach((item, index) => {
            const dedupKey = item.messageId || `${item.type}-${item.content}-${item.timestamp || 0}-${index}`
            if (!dedupMap.has(dedupKey)) {
              dedupMap.set(dedupKey, item)
            }
          })

          mergedMessages = Array.from(dedupMap.values()).sort((a, b) => Number(a.timestamp || 0) - Number(b.timestamp || 0))
          const mergedFirstTs = this.findFirstValidTimestamp(mergedMessages)
          isPrepend = Boolean(firstNewTs && mergedFirstTs && firstNewTs <= mergedFirstTs)
        }

        this.selectedChat.messages = mergedMessages

        if (keepPosition && isPrepend) {
          this.$nextTick(() => {
            const latestChatContent = this.$refs.chatContent
            if (!latestChatContent) {
              this.historyAnchorMessageId = null
              return
            }

            if (!this.historyAnchorMessageId) {
              this.historyAnchorMessageId = null
              return
            }

            const anchorEl = latestChatContent.querySelector(`[data-message-id="${this.historyAnchorMessageId}"]`)
            if (anchorEl) {
              anchorEl.scrollIntoView({ block: 'start' })
            }
            this.historyAnchorMessageId = null
          })
        } else if (reset || this.pendingScrollToBottom) {
          this.pendingScrollToBottom = false
          // 打开/切换会话必须定位到最新一条，这里不是"跟随"而是"初始定位"
          this.scrollToBottom(true)
        }
      } catch (error) {
        console.error('加载会话历史消息失败:', error)
        if (reset && this.selectedChat && String(this.selectedChat.id) === targetSessionId) {
          this.selectedChat.messages = []
        }
      } finally {
        this.historyLoading = false
      }
    },

    // 加载会话列表
    async loadSessions() {
      try {
        // 使用当前选中的机器人ID，如果没有则使用默认值
        const botId = this.selectedBot?.id || '1';
        const response = await pageSessions(botId, {
          pageNum: 1,
          pageSize: 20
        });
        
        if (response.code === 200 && response.data) {
          // 处理返回的会话数据
          const sessions = response.data.rows || [];
          
          // 清空会话列表
          this.todayChats = [];
          this.yesterdayChats = [];
          this.last7DaysChats = [];
          this.monthlyChats = {};
          
          // 获取当前时间
          const now = new Date();
          const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
          const yesterday = new Date(today);
          yesterday.setDate(yesterday.getDate() - 1);
          const sevenDaysAgo = new Date(today);
          sevenDaysAgo.setDate(sevenDaysAgo.getDate() - 7);
          
          // 遍历会话数据，根据时间分类
          sessions.forEach(session => {
            if (session.createTime) {
              const sessionDate = new Date(session.createTime);
              const sessionDay = new Date(sessionDate.getFullYear(), sessionDate.getMonth(), sessionDate.getDate());
              
              // 转换为对象格式
              const chatItem = {
                id: session.id,
                title: session.title || '未命名会话',
                createTime: session.createTime,
                model: session.model || ''
              };
              
              if (sessionDay.getTime() === today.getTime()) {
                // 今天
                this.todayChats.push(chatItem);
              } else if (sessionDay.getTime() === yesterday.getTime()) {
                // 昨天
                this.yesterdayChats.push(chatItem);
              } else if (sessionDay >= sevenDaysAgo) {
                // 最近7天
                this.last7DaysChats.push(chatItem);
              } else {
                // 按月份分组
                const monthKey = `${sessionDate.getFullYear()}-${String(sessionDate.getMonth() + 1).padStart(2, '0')}`;
                if (!this.monthlyChats[monthKey]) {
                  // 在Vue 3中，直接添加属性即可，会自动成为响应式的
                  this.monthlyChats[monthKey] = [];
                }
                this.monthlyChats[monthKey].push(chatItem);
              }
            } else {
              // 没有时间字段，默认放到今天
              this.todayChats.push({
                id: session.id,
                title: session.title || '未命名会话',
                model: session.model || ''
              });
            }
          });
        }
      } catch (error) {
      }
    },
    
    // 点击会话项
    onChatItemClick(chat) {
      if (this.renamingChatId) {
        this.cancelRename()
      }
      this.contextMenuChatId = null
      this.selectChat(chat)
    },

    // 切换会话右键菜单
    toggleContextMenu(chatId) {
      this.contextMenuChatId = this.contextMenuChatId === chatId ? null : chatId
    },

    // 关闭右键菜单
    closeContextMenu() {
      this.contextMenuChatId = null
    },

    // 开始重命名
    startRename(chat) {
      this.contextMenuChatId = null
      this.renamingChatId = chat.id
      this.renameInput = chat.title || ''
      this.$nextTick(() => {
        document.addEventListener('mousedown', this._renameOutsideClick)
      })
    },

    // 确认重命名
    async confirmRename(chat) {
      document.removeEventListener('mousedown', this._renameOutsideClick)
      const newTitle = this.renameInput.trim()
      if (!newTitle) {
        this.cancelRename()
        return
      }
      try {
        await renameSession(chat.id, newTitle)
        chat.title = newTitle
        if (this.selectedChat && this.selectedChat.id === chat.id) {
          this.selectedChat.title = newTitle
        }
      } catch (e) {
        console.error('重命名失败:', e)
        ElMessage.error('重命名失败')
      }
      this.renamingChatId = null
      this.renameInput = ''
    },

    // 取消重命名
    cancelRename() {
      document.removeEventListener('mousedown', this._renameOutsideClick)
      this.renamingChatId = null
      this.renameInput = ''
    },

    // 重命名时点击外部取消
    _renameOutsideClick(e) {
      const input = document.querySelector('.rename-input')
      if (input && !input.contains(e.target)) {
        this.cancelRename()
      }
    },

    // 删除会话
    async handleDeleteChat(chat) {
      this.contextMenuChatId = null
      try {
        await deleteSession(chat.id)
        // 从列表中移除
        this.todayChats = this.todayChats.filter(c => c.id !== chat.id)
        this.yesterdayChats = this.yesterdayChats.filter(c => c.id !== chat.id)
        this.last7DaysChats = this.last7DaysChats.filter(c => c.id !== chat.id)
        for (const key in this.monthlyChats) {
          this.monthlyChats[key] = this.monthlyChats[key].filter(c => c.id !== chat.id)
        }
        // 如果删除的是当前选中的会话，回到空状态
        if (this.selectedChat && this.selectedChat.id === chat.id) {
          this.selectedChat = null
          this.$router.replace(`/ai-chat/${this.$route.params.botId}`)
        }
      } catch (e) {
        console.error('删除会话失败:', e)
        ElMessage.error('删除失败')
      }
    },

    // 使用默认机器人数据
    useDefaultBots() {
      this.bots = [
        {
          id: '1',
          name: 'DeepSeek',
          avatar: '@/static/images/user_default.png'
        },
        {
          id: '2',
          name: '西瓜',
          avatar: '@/static/images/user_default.png'
        },
        {
          id: '3',
          name: 'Java专家',
          avatar: '@/static/images/user_default.png'
        }
      ];
    },
    
    // 切换机器人下拉菜单
    toggleBotDropdown() {
      this.showBotDropdown = !this.showBotDropdown;
    },
    
    // 切换机器人
    switchBot(bot) {
      // 关闭下拉菜单
      this.showBotDropdown = false;

      // DeepSeek 人格卡：选中即自动联动 DeepSeek 官方供应商
      if (bot.name && bot.name.indexOf('DeepSeek') !== -1) {
        this.currentProvider = 'deepseek'
        set('aiProvider', 'deepseek')
      }

      // 路由切换到对应机器人页面，具体状态由路由watch处理
      this.$router.push(`/ai-chat/${bot.id}`);
    },
    
  }
}
</script>

<style scoped>
/* ===== AI 聊天 · iOS 设计语言 ===== */
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

button {
  font-family: inherit;
  font-size: inherit;
  line-height: inherit;
  color: inherit;
}

input {
  font-family: inherit;
  font-size: inherit;
  line-height: inherit;
  color: inherit;
}

.ai-chat-container {
  display: flex;
  width: 100%;
  /* 父级 .app-container 是 height:auto，百分比高度一路失效，
     内容会把容器撑高、把输入区挤出视口；这里直接锚定视口高度 */
  height: calc(100vh - 24px);
  min-height: 0;
  background: var(--bg);
  font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', 'SF Pro Text', 'PingFang SC', 'Helvetica Neue', 'Microsoft YaHei', sans-serif;
  -webkit-font-smoothing: antialiased;
  overflow: hidden;
}

/* ===== 侧栏 · iOS 毛玻璃 ===== */
.chat-sidebar {
  width: 260px;
  min-width: 260px;
  height: 100%;
  background: var(--bg-blur-strong);
  -webkit-backdrop-filter: saturate(180%) blur(28px);
  backdrop-filter: saturate(180%) blur(28px);
  border-right: 1px solid var(--separator);
  display: flex;
  flex-direction: column;
}

.sidebar-header {
  padding: 16px 14px 10px;
}

.new-chat-btn {
  width: 100%;
  padding: 11px 14px;
  background: #007aff;
  color: #ffffff;
  border: none;
  border-radius: 999px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  transition: all 0.25s cubic-bezier(0.32, 0.72, 0, 1);
  box-shadow: 0 4px 14px rgba(0, 122, 255, 0.35);
}

.new-chat-btn:hover {
  background-color: #3395ff;
}

.icon-plus {
  font-size: 16px;
  font-style: normal;
}

/* 会话列表 */
.chat-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px 10px;
}

.chat-list::-webkit-scrollbar {
  width: 0;
  display: none;
}

.chat-section {
  margin-bottom: 16px;
}

.section-title {
  font-size: 12px;
  font-weight: 600;
  color: var(--label-secondary);
  margin: 0 0 6px 10px;
  letter-spacing: 0.2px;
}

.chat-item {
  padding: 10px 12px;
  border-radius: 12px;
  cursor: pointer;
  margin-bottom: 2px;
  transition: all 0.25s cubic-bezier(0.32, 0.72, 0, 1);
  display: flex;
  align-items: center;
  position: relative;
}

.chat-item:hover {
  background: var(--fill);
}

.chat-item.active {
  background: rgba(0, 122, 255, 0.12);
}

.chat-item .chat-title {
  display: block;
  flex: 1;
  min-width: 0;
  font-size: 14px;
  color: var(--label);
  line-height: 1.4;
  margin: 0;
  text-align: left;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chat-item.active .chat-title {
  color: #007aff;
  font-weight: 600;
}

/* 会话模型标签 */
.chat-model-tag {
  flex-shrink: 0;
  margin-left: 6px;
  font-size: 10px;
  font-weight: 600;
  line-height: 1;
  padding: 3px 7px;
  border-radius: 7px;
  letter-spacing: 0.3px;
}

.chat-model-tag.tag-glm {
  color: #3d63dd;
  background: rgba(61, 99, 221, 0.12);
}

.chat-model-tag.tag-ds {
  color: #7c5cd6;
  background: rgba(124, 92, 214, 0.12);
}

.chat-more-btn {
  flex-shrink: 0;
  margin-left: auto;
  padding: 0 6px;
  font-size: 15px;
  font-weight: bold;
  color: var(--label-secondary);
  border-radius: 6px;
  line-height: 1;
  display: none;
  user-select: none;
  position: relative;
}

.chat-item:hover .chat-more-btn,
.chat-more-btn.show {
  display: inline-block;
}

.chat-more-btn:hover {
  color: #007aff;
  background: rgba(0, 122, 255, 0.12);
}

/* 重命名输入 */
.rename-input {
  flex: 1;
  min-width: 0;
  font-size: 14px;
  padding: 4px 8px;
  border: none;
  border-radius: 8px;
  outline: none;
  background: var(--card);
  box-shadow: 0 0 0 2px rgba(0, 122, 255, 0.3);
}

/* 右键菜单 */
.chat-context-menu {
  position: absolute;
  right: 0;
  top: calc(100% + 4px);
  z-index: 100;
  background: rgba(255, 255, 255, 0.94);
  -webkit-backdrop-filter: saturate(180%) blur(24px);
  backdrop-filter: saturate(180%) blur(24px);
  border-radius: 14px;
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.2);
  min-width: 120px;
  padding: 5px;
  border: 1px solid rgba(0, 0, 0, 0.04);
}

.context-menu-item {
  padding: 9px 14px;
  font-size: 13.5px;
  color: var(--label);
  cursor: pointer;
  white-space: nowrap;
  border-radius: 10px;
  transition: background-color 0.25s cubic-bezier(0.32, 0.72, 0, 1);
}

.context-menu-item:hover {
  background: rgba(0, 122, 255, 0.1);
}

.context-menu-item.danger {
  color: #ff3b30;
}

.context-menu-item.danger:hover {
  background: rgba(255, 59, 48, 0.1);
}

/* 空列表 */
.empty-chat-list {
  padding: 40px 20px;
  text-align: center;
}

.empty-chat-art {
  display: flex;
  justify-content: center;
  margin-bottom: 10px;
}

.empty-chat-art svg {
  width: 52px;
  height: 52px;
  color: var(--accent);
  filter: drop-shadow(0 6px 14px rgba(0, 122, 255, 0.16));
}

.empty-chat-text {
  font-size: 14px;
  font-weight: 500;
  margin: 0 0 8px 0;
  color: var(--label-secondary);
}

.empty-chat-subtext {
  font-size: 12px;
  margin: 0;
  color: var(--label-tertiary);
}

/* 侧栏底部 */
.sidebar-footer {
  padding: 12px 14px;
  border-top: 1px solid var(--separator);
  margin-top: auto;
  width: 100%;
  box-sizing: border-box;
  position: relative;
}

.bot-info {
  cursor: pointer;
  transition: background-color 0.25s cubic-bezier(0.32, 0.72, 0, 1);
  padding: 8px 10px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.bot-info:hover {
  background: var(--fill);
}

.icon-dropdown {
  font-size: 10px;
  color: var(--label-tertiary);
  margin-left: auto;
}

.bot-avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  object-fit: cover;
  margin: 0;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.12);
}

.bot-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--label);
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 机器人下拉 */
.bot-dropdown {
  position: absolute;
  bottom: 100%;
  left: 14px;
  right: 14px;
  background: rgba(255, 255, 255, 0.94);
  -webkit-backdrop-filter: saturate(180%) blur(24px);
  backdrop-filter: saturate(180%) blur(24px);
  border-radius: 16px;
  box-shadow: 0 -8px 32px rgba(0, 0, 0, 0.18);
  z-index: 1000;
  margin-bottom: 8px;
  max-height: 200px;
  overflow-y: auto;
  border: 1px solid rgba(0, 0, 0, 0.04);
}

.dropdown-header {
  padding: 10px 14px;
  font-size: 12px;
  font-weight: 600;
  color: var(--label-secondary);
  border-bottom: 1px solid var(--separator);
}

.bot-dropdown-item {
  padding: 10px 14px;
  cursor: pointer;
  transition: background-color 0.2s cubic-bezier(0.32, 0.72, 0, 1);
  display: flex;
  align-items: center;
  gap: 8px;
}

.bot-dropdown-item:hover {
  background: var(--fill);
}

.bot-dropdown-item.active {
  background: rgba(0, 122, 255, 0.12);
}

.dropdown-bot-avatar {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  object-fit: cover;
}

.dropdown-bot-name {
  font-size: 13px;
  font-weight: 500;
  color: var(--label);
}

/* ===== 主聊天区 ===== */
.chat-main {
  flex: 1;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  background-color: var(--bg);
}

.chat-content {
  flex: 1;
  min-height: 0;
  padding: 12px 32px 0;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
}

.chat-content::-webkit-scrollbar {
  width: 0;
  display: none;
}

.chat-header {
  padding: 16px 32px;
  text-align: center;
  border-bottom: 1px solid var(--separator);
  flex-shrink: 0;
  background: rgba(249, 249, 252, 0.7);
  -webkit-backdrop-filter: saturate(180%) blur(20px);
  backdrop-filter: saturate(180%) blur(20px);
}

.chat-header .chat-title {
  font-size: 16px;
  font-weight: 700;
  letter-spacing: -0.3px;
  color: var(--label);
  margin: 0;
}

.history-control {
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.history-load-more {
  height: 24px;
  line-height: 24px;
  padding: 0 10px;
  border: none;
  background: transparent;
  font-size: 12px;
  color: #007aff;
  cursor: pointer;
  user-select: none;
  font-weight: 600;
}

.history-load-more:hover {
  color: #3395ff;
}

.history-loading {
  height: 24px;
  line-height: 24px;
  font-size: 12px;
  color: var(--label-secondary);
  text-align: center;
}

/* 消息列表 */
.message-list {
  flex: 1;
  padding: 24px 0;
}

.message {
  margin-bottom: 20px;
  display: flex;
  flex-direction: column;
}

.user-message {
  align-self: flex-end;
  max-width: 600px;
  width: auto;
}

.ai-message {
  align-self: flex-start;
  max-width: 600px;
  width: auto;
}

.error-message {
  align-self: center;
  max-width: 600px;
  width: auto;
}

/* iOS 气泡 */
.message-bubble {
  overflow-wrap: break-word;
  word-break: break-word;
  white-space: pre-wrap;
  padding: 12px 16px;
  border-radius: 18px;
  margin-bottom: 8px;
  font-size: 14.5px;
  line-height: 1.6;
}

.user-bubble {
  background: #007aff;
  border-bottom-right-radius: 4px;
  text-align: left;
  color: #ffffff;
  box-shadow: 0 2px 10px rgba(0, 122, 255, 0.3);
}

.ai-bubble {
  background: var(--card);
  border-bottom-left-radius: 4px;
  text-align: left;
  color: var(--label);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}

.error-bubble {
  background: rgba(255, 59, 48, 0.08);
  color: #ff3b30;
  border-radius: 14px;
}

.message-bubble p {
  font-size: 14.5px;
  line-height: 1.6;
  color: inherit;
  margin: 0;
}

/* ===== AI 消息 Markdown 渲染 ===== */
.ai-markdown {
  font-size: 14.5px;
  line-height: 1.65;
  color: inherit;
  word-break: break-word;
  max-width: 100%;
}

/* ===== AI 绘画图片消息 ===== */
.ai-image-bubble {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 6px;
}

.ai-image {
  max-width: 260px;
  width: 100%;
  border-radius: 14px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.12);
  display: block;
  background-color: var(--separator);
}

.ai-image-prompt {
  margin: 0;
  font-size: 12px;
  line-height: 1.5;
  color: var(--label-secondary, #8e8e93);
  word-break: break-word;
}

.ai-markdown > :first-child { margin-top: 0; }
.ai-markdown > :last-child { margin-bottom: 0; }

.ai-markdown p {
  margin: 6px 0;
  font-size: 14.5px;
  line-height: 1.65;
  color: inherit;
}

.ai-markdown h1, .ai-markdown h2, .ai-markdown h3,
.ai-markdown h4, .ai-markdown h5, .ai-markdown h6 {
  margin: 12px 0 6px;
  font-weight: 600;
  line-height: 1.35;
  color: var(--label);
}

.ai-markdown h1 { font-size: 20px; }
.ai-markdown h2 { font-size: 18px; }
.ai-markdown h3 { font-size: 16px; }
.ai-markdown h4, .ai-markdown h5, .ai-markdown h6 { font-size: 14.5px; }

.ai-markdown ul, .ai-markdown ol {
  margin: 6px 0;
  padding-left: 22px;
}

.ai-markdown li {
  margin: 3px 0;
  line-height: 1.6;
}

.ai-markdown strong { font-weight: 600; }

.ai-markdown a {
  color: #007aff;
  text-decoration: none;
}

.ai-markdown a:hover { text-decoration: underline; }

.ai-markdown code {
  font-family: "SF Mono", "JetBrains Mono", Consolas, "Courier New", monospace;
  font-size: 13px;
  background: rgba(120, 120, 128, 0.12);
  color: #d63447;
  padding: 1.5px 6px;
  border-radius: 5px;
}

.ai-markdown pre {
  margin: 8px 0;
  padding: 12px 14px;
  background: #1c1c1e;
  border-radius: 10px;
  overflow-x: auto;
  font-size: 13px;
  line-height: 1.55;
}

.ai-markdown pre code {
  font-size: 13px;
  background: transparent;
  color: #e8e8ed;
  padding: 0;
  border-radius: 0;
  display: block;
  white-space: pre;
}

.ai-markdown blockquote {
  margin: 8px 0;
  padding: 6px 12px;
  border-left: 3px solid rgba(0, 122, 255, 0.45);
  background: rgba(0, 122, 255, 0.06);
  border-radius: 0 8px 8px 0;
  color: var(--label-secondary);
}

.ai-markdown table {
  margin: 8px 0;
  border-collapse: collapse;
  font-size: 13.5px;
  width: 100%;
  display: block;
  overflow-x: auto;
}

.ai-markdown th, .ai-markdown td {
  border: 1px solid rgba(120, 120, 128, 0.25);
  padding: 6px 10px;
  text-align: left;
}

.ai-markdown th {
  background: rgba(120, 120, 128, 0.1);
  font-weight: 600;
}

.ai-markdown hr {
  border: none;
  border-top: 1px solid rgba(120, 120, 128, 0.25);
  margin: 10px 0;
}

.ai-markdown img {
  max-width: 100%;
  border-radius: 8px;
}

/* 深色模式下的代码块适配 */
html.dark .ai-markdown pre {
  background: #101014;
  border: 1px solid rgba(255, 255, 255, 0.08);
}

html.dark .ai-markdown code {
  background: rgba(255, 255, 255, 0.1);
  color: #ff9f0a;
}

html.dark .ai-markdown pre code {
  background: transparent;
  color: #e8e8ed;
}

/* 思考中动画 */
.thinking-dots {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 0;
}

.thinking-dots span {
  width: 6px;
  height: 6px;
  background-color: var(--label-secondary);
  border-radius: 50%;
  animation: thinking-bounce 1.2s infinite ease-in-out;
}

.thinking-dots span:nth-child(2) {
  animation-delay: 0.15s;
}

.thinking-dots span:nth-child(3) {
  animation-delay: 0.3s;
}

@keyframes thinking-bounce {
  0%, 60%, 100% { transform: translateY(0); }
  30% { transform: translateY(-6px); }
}

/* 状态提示 */
.status-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
}

.status-dots {
  display: inline-flex;
  align-items: center;
  gap: 3px;
}

.status-dots span {
  width: 5px;
  height: 5px;
  background-color: #007aff;
  border-radius: 50%;
  animation: thinking-bounce 1.2s infinite ease-in-out;
}

.status-dots span:nth-child(2) {
  animation-delay: 0.15s;
}

.status-dots span:nth-child(3) {
  animation-delay: 0.3s;
}

.status-text {
  font-size: 13px;
  color: var(--label-secondary);
  animation: status-pulse 2s infinite ease-in-out;
}

@keyframes status-pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

/* ===== 空态 · iOS ===== */
.empty-state {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
}

.empty-content {
  text-align: center;
  max-width: 600px;
  width: 100%;
}

.empty-icon-container {
  margin: 0 auto 24px;
  display: flex;
  justify-content: center;
}

.ai-icon {
  background: var(--accent-gradient);
  border-radius: 32px;
  width: 96px;
  height: 96px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: inset 0 0 0 1px var(--separator);
}

.ai-icon .empty-art {
  width: 76px;
  height: 76px;
  filter: drop-shadow(0 10px 20px rgba(175, 82, 222, 0.25));
}

.empty-content h2 {
  font-size: 24px;
  font-weight: 700;
  letter-spacing: -0.5px;
  color: var(--label);
  margin: 0 0 24px 0;
}

/* iOS 输入条 */
.empty-input-container {
  background-color: var(--card);
  border-radius: 999px;
  padding: 0;
  text-align: left;
  box-shadow: 0 2px 10px var(--separator);
  width: 100%;
  max-width: 500px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  gap: 0;
  overflow: hidden;
}

.empty-input-field {
  flex: 1;
  padding: 14px 18px;
  border: none;
  border-radius: 0;
  font-size: 14.5px;
  margin: 0;
  outline: none;
  box-sizing: border-box;
  min-height: 48px;
  background-color: transparent;
}

.empty-input-field:focus {
  outline: none;
}

.send-btn {
  padding: 0 18px;
  background-color: #007aff;
  color: #ffffff;
  border: none;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  margin: 0;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 48px;
  transition: all 0.25s cubic-bezier(0.32, 0.72, 0, 1);
}

.send-btn:hover {
  background-color: #3395ff;
}

.send-icon {
  width: 16px;
  height: 16px;
  object-fit: contain;
  filter: brightness(0) invert(1);
}

/* 停止生成按钮（iOS 风格） */
.stop-btn {
  flex-shrink: 0;
  width: 34px;
  height: 34px;
  border: none;
  border-radius: 50%;
  background: rgba(120, 120, 128, 0.16);
  color: var(--label-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  margin-right: 7px;
  transition: background 0.2s ease, transform 0.15s ease;
}

.paint-btn {
  flex-shrink: 0;
  width: 34px;
  height: 34px;
  border: none;
  border-radius: 50%;
  background: rgba(120, 120, 128, 0.16);
  color: var(--label-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  margin-right: 7px;
  transition: background 0.2s ease, transform 0.15s ease;
}

.paint-btn svg {
  width: 18px;
  height: 18px;
}

.paint-btn:hover {
  background: rgba(120, 120, 128, 0.28);
}

.paint-btn:active {
  transform: scale(0.92);
}

.paint-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.stop-btn:hover {
  background: rgba(120, 120, 128, 0.28);
}

.stop-btn:active {
  transform: scale(0.92);
}

.stop-btn svg {
  width: 13px;
  height: 13px;
}

.empty-footer {
  margin-top: 24px;
  text-align: center;
}

.empty-footer p {
  font-size: 10px;
  color: var(--label-tertiary);
  margin: 0;
}

/* ===== iOS 风格模型分段控件 ===== */
.model-segment {
  display: inline-flex;
  padding: 2px;
  border-radius: 10px;
  background: rgba(120, 120, 128, 0.14);
  -webkit-backdrop-filter: blur(20px);
  backdrop-filter: blur(20px);
  gap: 2px;
  margin-bottom: 12px;
}

.chat-input-area .model-segment,
.empty-input-wrap .model-segment {
  margin-left: auto;
  margin-right: auto;
  max-width: 600px;
  width: fit-content;
}

.segment-item {
  padding: 5px 15px;
  font-size: 12px;
  font-weight: 600;
  color: var(--label-secondary);
  background: transparent;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: color 0.25s ease, background 0.25s ease, box-shadow 0.25s ease;
  -webkit-tap-highlight-color: transparent;
}

.segment-item.active {
  color: var(--label-primary);
  background: var(--card);
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
}

.segment-item:hover {
  color: var(--label-primary);
}

/* ===== 输入区 ===== */
.chat-input-area {
  border-top: 1px solid var(--separator);
  padding: 16px 32px;
  background: rgba(249, 249, 252, 0.88);
  -webkit-backdrop-filter: saturate(180%) blur(28px);
  backdrop-filter: saturate(180%) blur(28px);
}

.input-container {
  background-color: var(--card);
  border-radius: 999px;
  padding: 0;
  margin: 0 0 8px 0;
  /* 白底 + 白卡片时输入框几乎看不见，补 1px 描边和一层可见阴影 */
  border: 1px solid var(--separator);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  max-width: 600px;
  width: 100%;
  margin-left: auto;
  margin-right: auto;
  display: flex;
  align-items: center;
  gap: 0;
  overflow: hidden;
}

.chat-input {
  flex: 1;
  padding: 14px 18px;
  border: none;
  border-radius: 0;
  font-size: 14.5px;
  margin: 0;
  outline: none;
  box-sizing: border-box;
  min-height: 48px;
  background-color: transparent;
}

.chat-input:focus {
  outline: none;
}

.input-footer {
  font-size: 10px;
  color: var(--label-tertiary);
  text-align: center;
  margin: 0;
  max-width: 600px;
  width: 100%;
  margin-left: auto;
  margin-right: auto;
}
</style>