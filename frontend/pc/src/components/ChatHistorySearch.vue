<template>
  <div class="history-search-panel" @keydown.esc="handleClose">
    <div class="hs-header">
      <span class="hs-back" title="返回" @click="handleClose">‹</span>
      <div class="hs-input-wrap">
        <input
          ref="searchInput"
          v-model="keyword"
          type="text"
          class="hs-input"
          placeholder="搜索聊天记录"
          @keyup.enter="handleSearch"
        />
        <span v-if="keyword" class="hs-clear" @click="clearKeyword">×</span>
      </div>
      <span class="hs-search-btn" :class="{ disabled: !keyword.trim() }" @click="handleSearch">搜索</span>
    </div>

    <div class="hs-body" ref="resultBody" @scroll="handleScroll">
      <div v-if="!hasSearched" class="hs-tip">
        输入关键词，搜索与「{{ chatName }}」的聊天记录
      </div>

      <div v-else-if="loading && results.length === 0" class="hs-tip">
        <span class="hs-loading-spinner"></span> 搜索中...
      </div>

      <!-- 请求失败要和「真的没有结果」区分开：服务不可用时不能骗用户说没搜到 -->
      <div v-else-if="errorMessage" class="hs-tip hs-error" role="alert">
        <p class="hs-error-title">搜索失败</p>
        <p class="hs-error-detail">{{ errorMessage }}</p>
        <span class="hs-retry" @click="handleSearch">重试</span>
      </div>

      <div v-else-if="!loading && results.length === 0" class="hs-tip">
        未找到与“{{ searchedKeyword }}”相关的聊天记录
      </div>

      <template v-else>
        <div class="hs-result-count">共找到 {{ total }} 条相关记录</div>

        <div
          v-for="item in results"
          :key="item.chatMessageId"
          class="hs-result-item"
          @click="handleLocate(item)"
        >
          <div class="hs-avatar">
            <img v-if="resolveAvatar(item)" :src="resolveAvatar(item)" :alt="resolveName(item)" />
            <span v-else>{{ resolveName(item).charAt(0) }}</span>
          </div>
          <div class="hs-content">
            <div class="hs-row-top">
              <span class="hs-name">{{ resolveName(item) }}</span>
              <span class="hs-time">{{ formatFullTime(item.createTime) }}</span>
            </div>
            <div v-if="item.contentType === 1" class="hs-snippet" v-html="resolveSnippet(item)"></div>
            <div v-else class="hs-snippet hs-snippet-plain">{{ contentTypeLabel(item.contentType) }}</div>
          </div>
        </div>

        <div v-if="loadingMore" class="hs-loading-more">加载中...</div>
        <div v-else-if="errorMessage" class="hs-inline-error" role="alert">
          <span>{{ errorMessage }}</span>
          <span class="hs-retry" @click="handleSearch">重试</span>
        </div>
        <div v-else-if="!hasMore" class="hs-no-more">没有更多了</div>
      </template>
    </div>
  </div>
</template>

<script>
import { searchHistoryMes } from '@/api/chatMessage.js'

const CONTENT_TYPE_LABELS = {
  1: '',
  2: '[图片]',
  3: '[语音]',
  4: '[视频]',
  5: '[文件]'
}

export default {
  name: 'ChatHistorySearch',
  props: {
    currentUser: {
      type: Object,
      default: () => ({})
    },
    currentChatWindow: {
      type: Object,
      default: () => ({})
    }
  },
  emits: ['close', 'locate-message'],
  data() {
    return {
      keyword: '',
      searchedKeyword: '',
      hasSearched: false,
      loading: false,
      loadingMore: false,
      results: [],
      pageNum: 1,
      pageSize: 20,
      total: 0,
      hasMore: false,
      // 搜索服务失败（如 503）要和「没有结果」区分开，否则会误导用户
      errorMessage: '',
      // 请求序号：新搜索/清空后，旧请求的迟到结果不得覆盖当前内容
      searchSeq: 0
    }
  },
  computed: {
    chatName() {
      return this.currentChatWindow?.chatName || ''
    }
  },
  mounted() {
    this.$nextTick(() => this.$refs.searchInput?.focus())
  },
  methods: {
    handleClose() {
      this.$emit('close')
    },

    clearKeyword() {
      this.searchSeq++
      this.keyword = ''
      this.results = []
      this.hasSearched = false
      this.errorMessage = ''
      this.loading = false
      this.loadingMore = false
      this.$refs.searchInput?.focus()
    },

    handleSearch() {
      const kw = this.keyword.trim()
      if (!kw) return

      this.searchedKeyword = kw
      this.hasSearched = true
      this.pageNum = 1
      this.results = []
      this.hasMore = false
      this.errorMessage = ''
      this.fetchResults()
    },

    handleScroll() {
      const el = this.$refs.resultBody
      if (!el || this.loading || this.loadingMore || !this.hasMore || this.errorMessage) return

      if (el.scrollHeight - el.scrollTop - el.clientHeight < 80) {
        this.pageNum++
        this.fetchResults()
      }
    },

    // 把 axios/网关错误翻译成用户能懂的文案；保留服务端明确返回的 message
    describeSearchError(error) {
      const status = error?.response?.status
      const serverMessage = error?.response?.data?.msg || error?.response?.data?.message
      if (status === 503) {
        return '搜索服务暂时不可用，请稍后重试'
      }
      if (status === 401 || status === 403) {
        return '登录状态已失效，请重新登录'
      }
      if (serverMessage) {
        return String(serverMessage)
      }
      if (status) {
        return `搜索请求失败（HTTP ${status}）`
      }
      return '网络异常，未能连接到搜索服务'
    },

    // 分页查询搜索结果
    async fetchResults() {
      const seq = ++this.searchSeq
      this.loading = this.pageNum === 1
      this.loadingMore = this.pageNum > 1

      const req = {
        keyword: this.searchedKeyword,
        chatType: this.currentChatWindow.chatType,
        receiverId: this.currentChatWindow.chatId,
        pageNum: this.pageNum,
        pageSize: this.pageSize
      }

      try {
        const res = await searchHistoryMes(req)
        if (seq !== this.searchSeq) return
        const data = res?.data || {}
        const rows = (data.rows || []).map(row => ({
          ...row,
          // 后端返回字段为 id / messageType，这里映射成聊天窗口通用的 chatMessageId / contentType
          chatMessageId: row.id,
          contentType: row.messageType
        }))
        this.results = this.pageNum === 1 ? rows : [...this.results, ...rows]
        this.total = Number(data.totalCount) || 0
        this.hasMore = this.pageNum < (Number(data.totalPage) || 0)
        this.errorMessage = ''
      } catch (error) {
        if (seq !== this.searchSeq) return
        this.errorMessage = this.describeSearchError(error)
        this.hasMore = false
        console.error('聊天记录搜索失败', error)
      } finally {
        if (seq === this.searchSeq) {
          this.loading = false
          this.loadingMore = false
        }
      }
    },

    contentTypeLabel(contentType) {
      return CONTENT_TYPE_LABELS[contentType] || ''
    },

    handleLocate(item) {
      this.$emit('locate-message', item)
    },

    resolveName(item) {
      if (item.senderId === this.currentUser.id) return '我'
      return item.senderName || this.currentChatWindow?.chatName || '对方'
    },

    resolveAvatar(item) {
      if (item.senderId === this.currentUser.id) return this.currentUser.avatar
      return item.senderAvatar || this.currentChatWindow?.avatar
    },

    // 优先使用后端返回的高亮片段（已用 <em> 标记命中关键词），没有则降级为纯文本转义展示
    resolveSnippet(item) {
      if (item.highlightMessage) return this.sanitizeHighlight(item.highlightMessage)
      return this.escapeHtml(item.message)
    },

    // 高亮片段来自消息正文，不能直接 v-html：只放行 <em>/</em>，其余 HTML 全部转义
    sanitizeHighlight(raw) {
      return String(raw)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/&lt;em&gt;/g, '<em>')
        .replace(/&lt;\/em&gt;/g, '</em>')
    },

    escapeHtml(str) {
      const div = document.createElement('div')
      div.textContent = str
      return div.innerHTML
    },

    formatFullTime(value) {
      const date = new Date(value)
      const pad = n => String(n).padStart(2, '0')
      return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
    }
  }
}
</script>

<style scoped>
.history-search-panel {
  position: absolute;
  inset: 0;
  z-index: 20;
  /* 玻璃主题里 --card 只有 3% 不透明度，搜索页必须自带不透明底，否则文字和消息叠在一起 */
  background: rgba(255, 255, 255, 0.96);
  -webkit-backdrop-filter: saturate(180%) blur(24px);
  backdrop-filter: saturate(180%) blur(24px);
  display: flex;
  flex-direction: column;
  box-shadow: 0 0 0 1px var(--separator);
}

.hs-header {
  display: flex;
  align-items: center;
  padding: 10px 14px;
  background: #f5f5f5;
  border-bottom: 1px solid #e0e0e0;
  flex-shrink: 0;
}

.hs-back {
  font-size: 26px;
  line-height: 1;
  color: #333;
  cursor: pointer;
  padding: 0 8px 0 0;
  user-select: none;
}

.hs-input-wrap {
  flex: 1;
  position: relative;
  display: flex;
  align-items: center;
}

.hs-input {
  width: 100%;
  padding: 8px 28px 8px 10px;
  border: none;
  border-radius: 4px;
  background: var(--card);
  outline: none;
  box-sizing: border-box;
  font-size: 14px;
}

.hs-clear {
  position: absolute;
  right: 8px;
  color: #999;
  cursor: pointer;
  font-size: 16px;
  line-height: 1;
}

.hs-search-btn {
  margin-left: 10px;
  color: #07C160;
  font-size: 14px;
  cursor: pointer;
  flex-shrink: 0;
  user-select: none;
}

.hs-search-btn.disabled {
  color: #bbb;
  cursor: not-allowed;
}

.hs-body {
  flex: 1;
  overflow-y: auto;
  padding: 0 0 12px 0;
}

.hs-tip {
  padding: 40px 20px;
  text-align: center;
  color: #999;
  font-size: 14px;
}

.hs-loading-spinner {
  display: inline-block;
  width: 14px;
  height: 14px;
  margin-right: 6px;
  border: 2px solid #eee;
  border-top: 2px solid #07C160;
  border-radius: 50%;
  vertical-align: -2px;
  animation: hs-spin 1s linear infinite;
}

@keyframes hs-spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.hs-result-count {
  padding: 10px 14px;
  font-size: 12px;
  color: #999;
}

.hs-result-item {
  display: flex;
  padding: 10px 14px;
  cursor: pointer;
  border-bottom: 1px solid var(--separator);
}

.hs-result-item:hover {
  background: #f5f5f5;
}

.hs-avatar {
  width: 36px;
  height: 36px;
  border-radius: 4px;
  background: #07C160;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 10px;
  font-size: 16px;
  flex-shrink: 0;
  overflow: hidden;
}

.hs-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.hs-content {
  flex: 1;
  min-width: 0;
}

.hs-row-top {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: 4px;
}

.hs-name {
  font-size: 14px;
  color: #333;
}

.hs-time {
  font-size: 12px;
  color: #999;
  flex-shrink: 0;
  margin-left: 8px;
}

.hs-snippet {
  font-size: 13px;
  color: #666;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  text-align: left;
}

.hs-snippet-plain {
  color: #999;
  font-style: italic;
}

.hs-snippet :deep(em) {
  background: #ffe58f;
  color: #333;
  font-style: normal;
  border-radius: 2px;
  padding: 0 1px;
}

.hs-loading-more,
.hs-no-more {
  padding: 14px;
  text-align: center;
  color: #999;
  font-size: 13px;
}

/* 失败态：明确说明是服务问题，并给出重试入口，不能伪装成「没有结果」 */
.hs-error {
  color: #b25e00;
}

.hs-error-title {
  margin: 0 0 6px;
  font-size: 15px;
  font-weight: 600;
}

.hs-error-detail {
  margin: 0 0 14px;
  font-size: 13px;
  line-height: 1.5;
  color: #9a6a2f;
  word-break: break-word;
}

.hs-retry {
  display: inline-block;
  padding: 6px 16px;
  border-radius: 14px;
  background: rgba(0, 122, 255, 0.1);
  color: #007aff;
  font-size: 13px;
  cursor: pointer;
  user-select: none;
}

.hs-retry:hover {
  background: rgba(0, 122, 255, 0.18);
}

.hs-inline-error {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 12px 14px;
  color: #b25e00;
  font-size: 13px;
  text-align: center;
}

/* 深色模式的覆盖放在非 scoped 块里：本项目 Vue 2 + vite 不支持 :global 选择器 */
</style>

<style>
html[data-theme="dark"] .history-search-panel {
  background: rgba(28, 28, 30, 0.96);
}

html[data-theme="dark"] .history-search-panel .hs-header {
  background: #1c1c1e;
  border-bottom-color: #2c2c2e;
}

html[data-theme="dark"] .history-search-panel .hs-back {
  color: #f2f2f7;
}

html[data-theme="dark"] .history-search-panel .hs-input {
  background: #2c2c2e;
  color: #f2f2f7;
}

html[data-theme="dark"] .history-search-panel .hs-clear,
html[data-theme="dark"] .history-search-panel .hs-tip,
html[data-theme="dark"] .history-search-panel .hs-result-count,
html[data-theme="dark"] .history-search-panel .hs-time,
html[data-theme="dark"] .history-search-panel .hs-no-more,
html[data-theme="dark"] .history-search-panel .hs-loading-more {
  color: #8e8e93;
}

html[data-theme="dark"] .history-search-panel .hs-result-item:hover {
  background: #2c2c2e;
}

html[data-theme="dark"] .history-search-panel .hs-name {
  color: #f2f2f7;
}

html[data-theme="dark"] .history-search-panel .hs-snippet {
  color: #aeaeb2;
}

html[data-theme="dark"] .history-search-panel .hs-snippet em {
  background: rgba(255, 214, 10, 0.28);
  color: #ffd60a;
  font-style: normal;
  border-radius: 2px;
  padding: 0 1px;
}

html[data-theme="dark"] .history-search-panel .hs-error-detail {
  color: #d9a15a;
}
</style>
