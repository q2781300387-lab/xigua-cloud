<template>
  <div class="bot-container">
    <!-- 左侧机器人列表 -->
    <div class="bot-sidebar">
      <h2 class="sidebar-title">我的机器人</h2>
      
      <div class="robot-list">
        <!-- 加载失败：页面内可见的失败态 + 重试，而不是空列表 -->
        <div v-if="loadFailed" class="robot-load-failed">
          <p class="robot-load-failed-title">机器人列表加载失败</p>
          <p class="robot-load-failed-desc">
            网络或服务异常，
            <span class="robot-retry" @click="loadBots">点击重试</span>
          </p>
        </div>
        <div 
          v-for="bot in bots" 
          :key="bot.id" 
          class="robot-item"
          :class="{ active: selectedBotId === bot.id }"
          @click="selectBot(bot)"
        >
          <img :src="bot.avatar" alt="机器人头像" class="robot-avatar" @error="handleAvatarError">
          <span class="robot-name">{{ bot.name || '未命名机器人' }}</span>
        </div>
      </div>
      
      <button class="add-robot-btn" @click="addBot">
        <i class="icon-plus">+</i> 新建机器人
      </button>
    </div>

    <!-- 右侧机器人详情 -->
    <div class="bot-content">
      <!-- 顶部操作栏 -->
      <div class="content-header">
        <div class="header-actions">
          <button class="action-btn edit-btn" @click="editBot" v-if="selectedBot">
            <i class="icon-edit">✏️</i>
          </button>
          <button class="action-btn delete-btn" @click="deleteBot" v-if="selectedBot">
            <i class="icon-delete">🗑️</i>
          </button>
        </div>
      </div>

      <!-- 机器人详情 -->
      <div class="robot-detail" v-if="selectedBot">
        <div class="detail-avatar-container">
          <img :src="selectedBot.avatar" alt="机器人头像" class="detail-avatar" @error="handleAvatarError">
        </div>
        <h1 class="detail-name">{{ selectedBot.name || '未命名机器人' }}</h1>
        <p class="detail-description">{{ selectedBot.description || '一个专为创意头脑风暴和快速信息处理设计的AI助手。形式简约，功能强大。' }}</p>
        <button class="enter-chat-btn" @click="startChat(selectedBot.id)">
          进入聊天 →
        </button>
      </div>

      <!-- 空状态 -->
      <div class="empty-state" v-else>
        <div class="empty-icon">
          <svg class="empty-art" viewBox="0 0 72 72" fill="none" aria-hidden="true">
            <defs>
              <linearGradient id="botGrad" x1="0" y1="0" x2="1" y2="1">
                <stop offset="0" stop-color="#007aff" stop-opacity="0.16"/>
                <stop offset="1" stop-color="#af52de" stop-opacity="0.08"/>
              </linearGradient>
            </defs>
            <circle cx="36" cy="36" r="30" fill="url(#botGrad)"/>
            <path d="M36 26v-9" stroke="currentColor" stroke-width="3" stroke-linecap="round"/>
            <circle cx="36" cy="14" r="4" fill="#ff9500"/>
            <rect x="15" y="26" width="42" height="28" rx="10" style="fill: var(--card)" stroke="currentColor" stroke-width="2.4"/>
            <rect x="10" y="30" width="6" height="10" rx="3" fill="currentColor" opacity="0.5"/>
            <rect x="56" y="30" width="6" height="10" rx="3" fill="currentColor" opacity="0.5"/>
            <circle cx="27" cy="39" r="4" fill="currentColor"/>
            <circle cx="45" cy="39" r="4" fill="currentColor"/>
            <circle cx="28.2" cy="38.2" r="1.6" fill="#fff"/>
            <circle cx="46.2" cy="38.2" r="1.6" fill="#fff"/>
            <path d="M29 50h14" stroke="currentColor" stroke-width="2.4" stroke-linecap="round"/>
            <circle cx="56" cy="50" r="6" fill="#ff9500" opacity="0.9"/>
            <path d="M56 47v6M53 50h6" stroke="#fff" stroke-width="1.6" stroke-linecap="round"/>
          </svg>
        </div>
        <h3 class="empty-title">选择一个机器人</h3>
        <p class="empty-desc">从左侧边栏选择一个机器人查看详情或开始聊天</p>
      </div>
    </div>
  </div>
</template>

<script>
import { getBots, deleteBot } from '@/api/bot.js'
import { set } from '@/utils/localStorage.js'

export default {
  name: 'BotPage',
  data() {
    return {
      bots: [],
      // 列表请求失败要落在页面上，不能只靠一闪而过的 toast
      loadFailed: false,
      selectedBotId: '',
      selectedBot: null,
      defaultAvatar: '/default-bot-avatar.png'
    }
  },
  created() {
    this.loadBots()
  },
  methods: {
    // 加载机器人列表
    loadBots() {
      this.loadFailed = false
      getBots().then(response => {
        if (response.code === 200) {
          this.bots = response.data || []
          // 默认选择第一个机器人
          if (this.bots.length > 0 && !this.selectedBotId) {
            this.selectBot(this.bots[0])
          }
        } else {
          this.loadFailed = true
          this.$message.error('获取机器人列表失败')
        }
      }).catch(error => {
        this.loadFailed = true
        console.error('获取机器人列表失败:', error)
        this.$message.error('获取机器人列表失败')
      })
    },

    // 头像错误处理
    handleAvatarError(event) {
      event.target.src = this.defaultAvatar
    },

    // 选择机器人
    selectBot(bot) {
      this.selectedBotId = bot.id
      this.selectedBot = bot
    },

    // 开始对话
    startChat(botId) {
      // 获取选中的机器人信息
      const selectedBot = this.bots.find(bot => bot.id === botId)
      
      // 跳转到AI聊天页面并开始与机器人对话
      // 使用localStorage存储机器人聊天参数
      const openChat = {
        chatId: botId,
        rank: 0,
        botInfo: selectedBot
      }
      set('openChat', JSON.stringify(openChat))
      
      // 跳转到AI聊天页面（携带botId）
      this.$router.push(`/ai-chat/${botId}`)
    },

    // 编辑机器人
    editBot() {
      if (this.selectedBot) {
        this.$message.info('编辑机器人功能开发中')
      }
    },

    // 删除机器人
    deleteBot() {
      if (!this.selectedBot) return
      
      this.$confirm(`确定要删除机器人 "${this.selectedBot.name}" 吗？此操作不可恢复。`, '警告', {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning',
        confirmButtonClass: 'el-button--danger'
      }).then(() => {
        deleteBot(this.selectedBot.id).then(response => {
          if (response.code === 200) {
            this.$message.success('机器人删除成功')
            this.loadBots() // 刷新列表
            this.selectedBotId = ''
            this.selectedBot = null
          } else {
            this.$message.error('删除失败：' + response.message)
          }
        })
      }).catch(() => {
        // 取消操作
      })
    },

    // 添加机器人
    addBot() {
      this.$router.push('/bot/create')
    }
  }
}
</script>

<style scoped>
/* ============ 苹果设计语言 · 机器人页 ============ */
.bot-container {
  width: 100%;
  height: 100%;
  display: flex;
  background: var(--bg);
  font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', 'SF Pro Text', 'PingFang SC', 'Helvetica Neue', 'Microsoft YaHei', sans-serif;
  -webkit-font-smoothing: antialiased;
  overflow: hidden;
}

/* ---------- 左侧边栏：毛玻璃 + inset grouped 列表 ---------- */
.bot-sidebar {
  width: 264px;
  min-width: 264px;
  display: flex;
  flex-direction: column;
  padding: 28px 20px 20px;
  background: rgba(242, 242, 247, 0.72);
  -webkit-backdrop-filter: saturate(180%) blur(30px);
  backdrop-filter: saturate(180%) blur(30px);
  border-right: 1px solid var(--separator);
}

.sidebar-title {
  font-size: 28px;
  font-weight: 700;
  letter-spacing: -0.6px;
  color: rgba(0, 0, 0, 0.92);
  margin: 0 0 22px 6px;
}

.robot-list {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 0;
}

.robot-list::-webkit-scrollbar { width: 4px; }
.robot-list::-webkit-scrollbar-thumb { background: rgba(120, 120, 128, 0.28); border-radius: 2px; }

.robot-load-failed {
  padding: 18px 12px;
  text-align: center;
}

.robot-load-failed-title {
  margin: 0 0 6px;
  font-size: 14px;
  font-weight: 600;
  color: #b45309;
}

.robot-load-failed-desc {
  margin: 0;
  font-size: 12.5px;
  color: rgba(60, 60, 67, 0.6);
}

.robot-retry {
  color: #007aff;
  font-weight: 600;
  cursor: pointer;
}
.robot-list::-webkit-scrollbar-track { background: transparent; }

.robot-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 12px;
  cursor: pointer;
  transition: background-color 0.35s cubic-bezier(0.32, 0.72, 0, 1);
}

.robot-item:hover { background: var(--fill); }

.robot-item.active {
  background: rgba(0, 122, 255, 0.12);
}

.robot-avatar {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  object-fit: cover;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.12), 0 0 0 0.5px rgba(0, 0, 0, 0.04);
  background: var(--accent-gradient);
}

.robot-name {
  font-size: 14.5px;
  font-weight: 500;
  color: rgba(0, 0, 0, 0.86);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.robot-item.active .robot-name { color: #007aff; font-weight: 600; }

/* 新建按钮：iOS 系统蓝胶囊 */
.add-robot-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-top: 18px;
  padding: 12px 14px;
  background: #007aff;
  color: #ffffff;
  border: none;
  border-radius: 999px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(0, 122, 255, 0.35);
  transition: transform 0.35s cubic-bezier(0.32, 0.72, 0, 1), box-shadow 0.35s cubic-bezier(0.32, 0.72, 0, 1), opacity 0.35s cubic-bezier(0.32, 0.72, 0, 1);
}

.add-robot-btn:hover { box-shadow: 0 4px 14px rgba(0, 122, 255, 0.45); }
.add-robot-btn:active { transform: scale(0.96); opacity: 0.9; }

.add-robot-btn i { font-size: 17px; font-style: normal; font-weight: 600; }

/* ---------- 右侧内容区 ---------- */
.bot-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 32px 40px;
  overflow-y: auto;
  background: transparent;
}

.content-header {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  margin-bottom: 24px;
}

.header-actions { display: flex; gap: 10px; }

/* iOS 图标按钮（SF 风格） */
.action-btn {
  width: 38px;
  height: 38px;
  border: none;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  font-size: 17px;
  background: var(--fill);
  color: var(--label);
  transition: background-color 0.35s cubic-bezier(0.32, 0.72, 0, 1), transform 0.35s cubic-bezier(0.32, 0.72, 0, 1), color 0.35s cubic-bezier(0.32, 0.72, 0, 1);
}

.action-btn:hover { background: rgba(120, 120, 128, 0.2); }

.delete-btn:hover { background: rgba(255, 59, 48, 0.14); color: #ff3b30; }

.action-btn:active { transform: scale(0.88); }

/* 机器人详情：iOS 大圆角卡片 */
.robot-detail {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  flex: 1;
  padding: 48px 32px;
  background: var(--card);
  border-radius: 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06), 0 8px 32px rgba(0, 0, 0, 0.04);
  min-height: 420px;
}

/* App 图标风格的圆角方形头像 */
.detail-avatar-container {
  margin-bottom: 28px;
  position: relative;
}

.detail-avatar-container::before {
  content: '';
  position: absolute;
  inset: -16px;
  background: linear-gradient(145deg, rgba(0, 122, 255, 0.16), rgba(88, 86, 214, 0.12));
  border-radius: 40px;
  filter: blur(18px);
  z-index: -1;
}

.detail-avatar {
  width: 120px;
  height: 120px;
  border-radius: 26px;
  object-fit: cover;
  background: var(--accent-gradient);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.14), 0 2px 4px rgba(0, 0, 0, 0.06), inset 0 0 0 1px rgba(255, 255, 255, 0.6);
}

.detail-name {
  font-size: 30px;
  font-weight: 700;
  letter-spacing: -0.5px;
  color: rgba(0, 0, 0, 0.92);
  margin: 0 0 10px 0;
}

.detail-description {
  font-size: 15.5px;
  line-height: 1.6;
  color: rgba(0, 0, 0, 0.58);
  max-width: 480px;
  margin: 0 0 36px 0;
}

/* iOS 系统蓝胶囊主按钮 */
.enter-chat-btn {
  padding: 14px 36px;
  background: #007aff;
  color: #ffffff;
  border: none;
  border-radius: 999px;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 0.1px;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  box-shadow: 0 4px 16px rgba(0, 122, 255, 0.38);
  transition: transform 0.35s cubic-bezier(0.32, 0.72, 0, 1), box-shadow 0.35s cubic-bezier(0.32, 0.72, 0, 1), opacity 0.35s cubic-bezier(0.32, 0.72, 0, 1);
}

.enter-chat-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 8px 24px rgba(0, 122, 255, 0.5);
}

.enter-chat-btn:active { transform: scale(0.96); opacity: 0.92; }

/* 空状态：iOS 精致空态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  flex: 1;
  min-height: 420px;
  background: var(--card);
  border-radius: 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06), 0 8px 32px rgba(0, 0, 0, 0.04);
  padding: 48px 32px;
}

.empty-icon {
  width: 96px;
  height: 96px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 20px;
  background: var(--accent-gradient);
  border-radius: 28px;
  box-shadow: inset 0 0 0 1px var(--separator);
}

.empty-art {
  width: 72px;
  height: 72px;
  color: var(--accent);
  filter: drop-shadow(0 10px 22px rgba(0, 122, 255, 0.22));
}

.empty-title {
  font-size: 20px;
  font-weight: 600;
  letter-spacing: -0.3px;
  color: var(--label);
  margin: 0 0 8px 0;
}

.empty-desc {
  font-size: 14.5px;
  line-height: 1.5;
  color: var(--label-secondary);
  margin: 0;
  max-width: 300px;
}

/* 响应式 */
@media (max-width: 768px) {
  .bot-container { flex-direction: column; }
  .bot-sidebar {
    width: 100%;
    min-width: 0;
    border-right: none;
    border-bottom: 1px solid var(--separator);
    padding: 16px;
  }
  .sidebar-title { font-size: 22px; margin-bottom: 14px; }
  .robot-list { flex-direction: row; overflow-x: auto; gap: 8px; }
  .robot-item { flex-direction: column; min-width: 72px; text-align: center; padding: 12px 6px; }
  .robot-name { white-space: normal; font-size: 12px; margin-top: 6px; }
  .bot-content { padding: 16px; }
  .robot-detail, .empty-state { padding: 32px 20px; min-height: 320px; }
  .detail-avatar { width: 96px; height: 96px; border-radius: 21px; }
  .detail-name { font-size: 24px; }
  .detail-description { font-size: 14px; }
}
</style>