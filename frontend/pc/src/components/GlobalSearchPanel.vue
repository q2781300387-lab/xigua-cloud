<template>
  <div v-if="visible" class="global-search-layer" role="dialog" aria-modal="true" aria-label="全局消息搜索" @keydown.esc="close">
    <div class="gs-backdrop" @click="close"></div>
    <section class="gs-panel">
      <header class="gs-header">
        <button type="button" class="gs-icon-btn" aria-label="关闭全局搜索" title="关闭" @click="close">
          <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
            <path d="M15 5l-7 7 7 7"></path>
          </svg>
        </button>
        <div class="gs-input-wrap">
          <svg class="gs-search-icon" viewBox="0 0 24 24" width="17" height="17" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="11" cy="11" r="7"></circle>
            <path d="M16.5 16.5L21 21" stroke-linecap="round"></path>
          </svg>
          <input
            ref="searchInput"
            v-model="keyword"
            type="search"
            class="gs-input"
            placeholder="搜索所有聊天记录、文件和群"
            aria-label="全局搜索关键词"
            autocomplete="off"
            @input="onInput"
          />
          <button v-if="keyword" type="button" class="gs-clear" aria-label="清空关键词" title="清空" @click="clearKeyword">×</button>
        </div>
        <button type="button" class="gs-cancel" @click="close">取消</button>
      </header>

      <div class="gs-filters" role="tablist" aria-label="搜索范围">
        <button
          v-for="scopeItem in scopes"
          :key="scopeItem.value"
          type="button"
          role="tab"
          class="gs-chip"
          :class="{ active: scope === scopeItem.value }"
          :aria-selected="scope === scopeItem.value ? 'true' : 'false'"
          @click="changeScope(scopeItem.value)"
        >
          {{ scopeItem.label }}
          <span v-if="scopeCount(scopeItem.value)" class="gs-chip-count">{{ scopeCount(scopeItem.value) }}</span>
        </button>
      </div>

      <div v-if="scope === 'messages'" class="gs-filters gs-subfilters" role="tablist" aria-label="消息类型筛选">
        <button
          v-for="filter in filters"
          :key="'type-' + filter.value"
          type="button"
          role="tab"
          class="gs-chip gs-chip-sm"
          :class="{ active: messageType === filter.value }"
          :aria-selected="messageType === filter.value ? 'true' : 'false'"
          @click="changeFilter(filter.value)"
        >{{ filter.label }}</button>
      </div>

      <div ref="resultBody" class="gs-body" @scroll="onScroll">
        <div v-if="!hasSearched" class="gs-state">
          <div class="gs-state-art" aria-hidden="true">
            <svg viewBox="0 0 72 72" width="60" height="60" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round">
              <circle cx="31" cy="31" r="18"></circle>
              <path d="M45 45l13 13"></path>
              <path d="M24 31h14M31 24v14"></path>
            </svg>
          </div>
          <p class="gs-state-title">全局搜索</p>
          <p class="gs-state-desc">一次搜到消息、联系人、群和群文件</p>
        </div>

        <div v-else-if="loading && scope === 'messages' && results.length === 0" class="gs-state">
          <span class="gs-spinner" aria-hidden="true"></span>
          <p class="gs-state-desc">正在搜索…</p>
        </div>

        <div v-else-if="errorMessage && scope === 'messages'" class="gs-state gs-state-error" role="alert">
          <p class="gs-state-title">消息搜索失败</p>
          <p class="gs-state-desc">{{ errorMessage }}</p>
          <button type="button" class="gs-retry" @click="runSearch(true)">重试</button>
        </div>

        <template v-else>
          <div v-if="scope === 'all'" class="gs-all">
            <p v-if="aggregateError" class="gs-inline-error">联系人、群、文件搜索暂不可用：{{ aggregateError }}</p>

            <section v-if="contacts.length" class="gs-section">
              <div class="gs-section-head">
                <h3>联系人</h3>
                <button type="button" class="gs-more-link" @click="changeScope('contacts')">全部 {{ contacts.length }}</button>
              </div>
              <button v-for="item in contacts.slice(0, 3)" :key="'c-' + item.id" type="button" class="gs-result" @click="openContact(item)">
                <span class="gs-avatar"><img v-if="item.avatar" :src="item.avatar" alt="" @error="item.avatar = ''" /><span v-else>{{ initial(item.name) }}</span></span>
                <span class="gs-result-main">
                  <span class="gs-result-top"><span class="gs-conversation">{{ item.name || '联系人' }}</span><span class="gs-badge gs-badge-neutral">联系人</span></span>
                  <span class="gs-snippet">{{ item.signature || (item.online ? '在线' : '好友') }}</span>
                </span>
                <span class="gs-chevron">›</span>
              </button>
            </section>

            <section v-if="groups.length" class="gs-section">
              <div class="gs-section-head">
                <h3>群聊</h3>
                <button type="button" class="gs-more-link" @click="changeScope('groups')">全部 {{ groups.length }}</button>
              </div>
              <button v-for="item in groups.slice(0, 3)" :key="'g-' + item.id" type="button" class="gs-result" @click="openGroup(item)">
                <span class="gs-avatar gs-avatar-group"><img v-if="item.avatar" :src="item.avatar" alt="" @error="item.avatar = ''" /><span v-else>{{ initial(item.name) }}</span></span>
                <span class="gs-result-main">
                  <span class="gs-result-top"><span class="gs-conversation">{{ item.name || '群聊' }}</span><span class="gs-badge">群聊</span></span>
                  <span class="gs-snippet">{{ item.memberCount || 0 }} 位成员</span>
                </span>
                <span class="gs-chevron">›</span>
              </button>
            </section>

            <section v-if="files.length" class="gs-section">
              <div class="gs-section-head">
                <h3>群文件</h3>
                <button type="button" class="gs-more-link" @click="changeScope('files')">全部 {{ files.length }}</button>
              </div>
              <button v-for="item in files.slice(0, 3)" :key="'f-' + item.id" type="button" class="gs-result" @click="openFile(item)">
                <span class="gs-avatar gs-avatar-file">{{ fileInitial(item.name) }}</span>
                <span class="gs-result-main">
                  <span class="gs-result-top"><span class="gs-conversation">{{ item.name }}</span><span class="gs-time">{{ formatSize(item.size) }}</span></span>
                  <span class="gs-snippet">{{ item.groupName || '群文件' }}<span v-if="item.uploaderName"> · {{ item.uploaderName }}</span></span>
                </span>
                <span class="gs-chevron">›</span>
              </button>
            </section>

            <section v-if="results.length" class="gs-section">
              <div class="gs-section-head">
                <h3>消息</h3>
                <button type="button" class="gs-more-link" @click="changeScope('messages')">全部 {{ total }}</button>
              </div>
              <button v-for="item in results.slice(0, 5)" :key="'m-' + item.id" type="button" class="gs-result" @click="handleJump(item)">
                <span class="gs-avatar"><img v-if="item.conversationAvatar" :src="item.conversationAvatar" alt="" @error="markAvatarBroken(item)" /><span v-else>{{ initial(item.conversationName) }}</span></span>
                <span class="gs-result-main">
                  <span class="gs-result-top">
                    <span class="gs-conversation">{{ item.conversationName || '会话' }}</span>
                    <span v-if="Number(item.conversationChatType) === 2" class="gs-badge">群聊</span>
                    <span class="gs-time">{{ formatTime(item.createTime) }}</span>
                  </span>
                  <span class="gs-snippet" v-html="snippetHtml(item)"></span>
                </span>
                <span class="gs-chevron">›</span>
              </button>
            </section>

            <div v-if="!aggregateLoading && !contacts.length && !groups.length && !files.length && !results.length" class="gs-state gs-state-compact">
              <p class="gs-state-title">没有找到“{{ searchedKeyword }}”</p>
              <p class="gs-state-desc">换个关键词试试，或缩小到消息类型再搜</p>
            </div>
          </div>

          <div v-else-if="scope === 'contacts'" class="gs-list">
            <p class="gs-count">找到 {{ contacts.length }} 位联系人</p>
            <button v-for="item in contacts" :key="'cl-' + item.id" type="button" class="gs-result" @click="openContact(item)">
              <span class="gs-avatar"><img v-if="item.avatar" :src="item.avatar" alt="" @error="item.avatar = ''" /><span v-else>{{ initial(item.name) }}</span></span>
              <span class="gs-result-main">
                <span class="gs-result-top"><span class="gs-conversation">{{ item.name || '联系人' }}</span><span class="gs-time">{{ item.online ? '在线' : '' }}</span></span>
                <span class="gs-snippet">{{ item.signature || '我的好友' }}</span>
              </span>
              <span class="gs-chevron">›</span>
            </button>
            <div v-if="!contacts.length && !aggregateLoading" class="gs-state gs-state-compact"><p class="gs-state-title">没有匹配的联系人</p></div>
          </div>

          <div v-else-if="scope === 'groups'" class="gs-list">
            <p class="gs-count">找到 {{ groups.length }} 个群聊</p>
            <button v-for="item in groups" :key="'gl-' + item.id" type="button" class="gs-result" @click="openGroup(item)">
              <span class="gs-avatar gs-avatar-group"><img v-if="item.avatar" :src="item.avatar" alt="" @error="item.avatar = ''" /><span v-else>{{ initial(item.name) }}</span></span>
              <span class="gs-result-main">
                <span class="gs-result-top"><span class="gs-conversation">{{ item.name || '群聊' }}</span><span class="gs-badge">群聊</span></span>
                <span class="gs-snippet">{{ item.memberCount || 0 }} 位成员</span>
              </span>
              <span class="gs-chevron">›</span>
            </button>
            <div v-if="!groups.length && !aggregateLoading" class="gs-state gs-state-compact"><p class="gs-state-title">没有匹配的群聊</p></div>
          </div>

          <div v-else-if="scope === 'files'" class="gs-list">
            <p class="gs-count">找到 {{ files.length }} 个文件</p>
            <button v-for="item in files" :key="'fl-' + item.id" type="button" class="gs-result" @click="openFile(item)">
              <span class="gs-avatar gs-avatar-file">{{ fileInitial(item.name) }}</span>
              <span class="gs-result-main">
                <span class="gs-result-top"><span class="gs-conversation">{{ item.name }}</span><span class="gs-time">{{ formatSize(item.size) }}</span></span>
                <span class="gs-snippet">{{ item.groupName || '群文件' }}<span v-if="item.uploaderName"> · {{ item.uploaderName }}</span></span>
              </span>
              <span class="gs-chevron">›</span>
            </button>
            <div v-if="!files.length && !aggregateLoading" class="gs-state gs-state-compact"><p class="gs-state-title">没有匹配的群文件</p></div>
          </div>

          <div v-else class="gs-list">
            <p class="gs-count">共 {{ total }} 条消息</p>
            <button v-for="item in results" :key="item.id" type="button" class="gs-result" @click="handleJump(item)">
              <span class="gs-avatar"><img v-if="item.conversationAvatar" :src="item.conversationAvatar" alt="" @error="markAvatarBroken(item)" /><span v-else>{{ initial(item.conversationName) }}</span></span>
              <span class="gs-result-main">
                <span class="gs-result-top">
                  <span class="gs-conversation">{{ item.conversationName || '会话' }}</span>
                  <span v-if="Number(item.conversationChatType) === 2" class="gs-badge">群聊</span>
                  <span class="gs-time">{{ formatTime(item.createTime) }}</span>
                </span>
                <span class="gs-snippet" v-html="snippetHtml(item)"></span>
                <span class="gs-result-meta"><span>{{ item.senderName || '对方' }}</span><span v-if="item.messageType !== 1" class="gs-type">{{ typeLabel(item.messageType) }}</span></span>
              </span>
              <span class="gs-chevron">›</span>
            </button>
            <div v-if="!loading && results.length === 0" class="gs-state gs-state-compact">
              <p class="gs-state-title">没有找到“{{ searchedKeyword }}”</p>
              <p class="gs-state-desc">换个关键词，或把筛选切回“全部”再试</p>
            </div>
            <div v-if="loadingMore" class="gs-more"><span class="gs-spinner gs-spinner-sm"></span> 加载中…</div>
            <div v-else-if="results.length && !hasMore" class="gs-more">已经到底了</div>
          </div>
        </template>
      </div>
    </section>
  </div>
</template>

<script>
import { searchGlobalMessages } from '@/api/chatMessage.js'
import { searchContactsGroupsFiles } from '@/api/contact.js'

const FILTERS = [
  { label: '全部', value: null },
  { label: '文本', value: 1 },
  { label: '图片', value: 2 },
  { label: '语音', value: 3 },
  { label: '视频', value: 4 },
  { label: '文件', value: 5 }
]

const SCOPES = [
  { label: '综合', value: 'all' },
  { label: '消息', value: 'messages' },
  { label: '联系人', value: 'contacts' },
  { label: '群', value: 'groups' },
  { label: '文件', value: 'files' }
]

const TYPE_LABELS = { 1: '文本', 2: '图片', 3: '语音', 4: '视频', 5: '文件' }

function escapeHtml(text) {
  return String(text == null ? '' : text)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

function formatClock(value) {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  const now = new Date()
  const sameDay = date.getFullYear() === now.getFullYear() &&
    date.getMonth() === now.getMonth() &&
    date.getDate() === now.getDate()
  const pad = n => String(n).padStart(2, '0')
  if (sameDay) return `${pad(date.getHours())}:${pad(date.getMinutes())}`
  const sameYear = date.getFullYear() === now.getFullYear()
  return sameYear
    ? `${date.getMonth() + 1}月${date.getDate()}日`
    : `${date.getFullYear()}/${date.getMonth() + 1}/${date.getDate()}`
}

export default {
  name: 'GlobalSearchPanel',
  props: {
    visible: { type: Boolean, default: false }
  },
  emits: ['close', 'jump'],
  data() {
    return {
      keyword: '',
      searchedKeyword: '',
      scope: 'all',
      scopes: SCOPES,
      messageType: null,
      filters: FILTERS,
      hasSearched: false,
      loading: false,
      loadingMore: false,
      aggregateLoading: false,
      aggregateError: '',
      contacts: [],
      groups: [],
      files: [],
      results: [],
      total: 0,
      pageNum: 1,
      pageSize: 20,
      hasMore: false,
      errorMessage: '',
      requestSeq: 0,
      aggregateSeq: 0,
      debounceTimer: null
    }
  },
  watch: {
    visible(value) {
      if (value) {
        this.$nextTick(() => {
          if (this.$refs.searchInput) this.$refs.searchInput.focus()
        })
      } else {
        this.reset()
      }
    }
  },
  beforeDestroy() {
    if (this.debounceTimer) clearTimeout(this.debounceTimer)
  },
  methods: {
    close() {
      this.$emit('close')
    },
    reset() {
      if (this.debounceTimer) clearTimeout(this.debounceTimer)
      this.requestSeq++
      this.aggregateSeq++
      this.keyword = ''
      this.searchedKeyword = ''
      this.scope = 'all'
      this.messageType = null
      this.hasSearched = false
      this.loading = false
      this.loadingMore = false
      this.aggregateLoading = false
      this.aggregateError = ''
      this.contacts = []
      this.groups = []
      this.files = []
      this.results = []
      this.total = 0
      this.pageNum = 1
      this.hasMore = false
      this.errorMessage = ''
    },
    onInput() {
      if (this.debounceTimer) clearTimeout(this.debounceTimer)
      const value = this.keyword.trim()
      if (!value) {
        this.requestSeq++
        this.aggregateSeq++
        this.hasSearched = false
        this.results = []
        this.contacts = []
        this.groups = []
        this.files = []
        this.total = 0
        this.errorMessage = ''
        this.aggregateError = ''
        this.loading = false
        this.aggregateLoading = false
        return
      }
      this.debounceTimer = setTimeout(() => this.runAll(true), 380)
    },
    clearKeyword() {
      this.keyword = ''
      this.onInput()
      if (this.$refs.searchInput) this.$refs.searchInput.focus()
    },
    changeFilter(value) {
      if (this.messageType === value && this.scope === 'messages') return
      this.messageType = value
      this.scope = 'messages'
      if (this.keyword.trim()) this.runSearch(true)
    },
    async runAll() {
      const keyword = this.keyword.trim()
      if (!keyword) return
      this.searchedKeyword = keyword
      this.hasSearched = true
      this.runSearch(true)
      this.runAggregate(keyword)
    },
    async runAggregate(keyword) {
      const seq = ++this.aggregateSeq
      this.aggregateLoading = true
      this.aggregateError = ''
      try {
        const res = await searchContactsGroupsFiles(keyword)
        if (seq !== this.aggregateSeq) return
        if (!res || res.code !== 200) {
          throw new Error((res && res.message) || '搜索服务暂时不可用')
        }
        const data = (res && res.data) || {}
        this.contacts = Array.isArray(data.contacts) ? data.contacts : []
        this.groups = Array.isArray(data.groups) ? data.groups : []
        this.files = Array.isArray(data.files) ? data.files : []
      } catch (error) {
        if (seq !== this.aggregateSeq) return
        this.contacts = []
        this.groups = []
        this.files = []
        this.aggregateError = (error && error.message) || '网络异常'
      } finally {
        if (seq === this.aggregateSeq) this.aggregateLoading = false
      }
    },
    changeScope(value) {
      if (this.scope === value) return
      this.scope = value
      if (this.$refs.resultBody) this.$refs.resultBody.scrollTop = 0
      if (value === 'messages' && this.keyword.trim() && !this.hasSearched) {
        this.runAll()
      }
    },
    scopeCount(value) {
      if (!this.hasSearched) return 0
      if (value === 'contacts') return this.contacts.length
      if (value === 'groups') return this.groups.length
      if (value === 'files') return this.files.length
      if (value === 'messages') return this.total
      return this.contacts.length + this.groups.length + this.files.length + this.total
    },
    openContact(item) {
      if (!item || item.id == null) return
      this.$emit('jump', {
        conversationId: String(item.id),
        conversationName: item.name || '联系人',
        conversationAvatar: item.avatar || '',
        conversationChatType: 1,
        messageId: ''
      })
    },
    openGroup(item) {
      if (!item || item.id == null) return
      this.$emit('jump', {
        conversationId: String(item.id),
        conversationName: item.name || '群聊',
        conversationAvatar: item.avatar || '',
        conversationChatType: 2,
        messageId: ''
      })
    },
    openFile(item) {
      if (!item || !item.url) return
      window.open(item.url, '_blank', 'noopener')
    },
    formatSize(size) {
      const value = Number(size)
      if (!value || value < 0) return ''
      if (value < 1024) return `${value} B`
      if (value < 1024 * 1024) return `${(value / 1024).toFixed(1)} KB`
      if (value < 1024 * 1024 * 1024) return `${(value / 1024 / 1024).toFixed(1)} MB`
      return `${(value / 1024 / 1024 / 1024).toFixed(1)} GB`
    },
    fileInitial(name) {
      const ext = String(name || '').split('.').pop()
      return ext && ext !== String(name) ? ext.slice(0, 3).toUpperCase() : 'FILE'
    },
    async runSearch(reset) {
      const keyword = this.keyword.trim()
      if (!keyword) return
      if (reset) {
        this.requestSeq++
        this.pageNum = 1
        this.results = []
        this.total = 0
        this.hasMore = false
      }
      const seq = this.requestSeq
      this.searchedKeyword = keyword
      this.hasSearched = true
      this.errorMessage = ''
      this.loading = reset
      this.loadingMore = !reset
      try {
        const res = await searchGlobalMessages({
          keyword,
          pageNum: this.pageNum,
          pageSize: this.pageSize,
          messageType: this.messageType
        })
        if (seq !== this.requestSeq) return
        if (!res || res.code !== 200 || !res.data) {
          throw new Error((res && res.message) || '搜索服务暂时不可用')
        }
        const page = res.data
        const rows = Array.isArray(page.rows) ? page.rows : []
        this.results = reset ? rows : this.results.concat(rows)
        this.total = Number(page.totalCount) || this.results.length
        this.hasMore = Number(page.pageNum) < Number(page.totalPage)
      } catch (error) {
        if (seq !== this.requestSeq) return
        this.errorMessage = (error && error.message) || '网络异常，请稍后再试'
      } finally {
        if (seq === this.requestSeq) {
          this.loading = false
          this.loadingMore = false
        }
      }
    },
    onScroll(event) {
      if (this.scope !== 'messages') return
      if (!this.hasMore || this.loading || this.loadingMore || this.errorMessage) return
      const box = event.target
      if (box.scrollTop + box.clientHeight >= box.scrollHeight - 80) {
        this.pageNum++
        this.runSearch(false)
      }
    },
    handleJump(item) {
      if (!item || item.conversationId == null) return
      this.$emit('jump', {
        conversationId: String(item.conversationId),
        conversationName: item.conversationName || '会话',
        conversationAvatar: item.conversationAvatar || '',
        conversationChatType: Number(item.conversationChatType) || 1,
        messageId: String(item.id)
      })
    },
    initial(name) {
      const text = String(name || '会').trim()
      return text ? text.charAt(0) : '会'
    },
    markAvatarBroken(item) {
      this.$set(item, 'conversationAvatar', '')
    },
    typeLabel(type) {
      return TYPE_LABELS[type] || '消息'
    },
    formatTime(value) {
      return formatClock(value)
    },
    snippetHtml(item) {
      const raw = item && item.message != null ? String(item.message) : ''
      const safe = escapeHtml(raw)
      const keyword = this.searchedKeyword
      if (!keyword) return safe
      const pattern = keyword.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
      try {
        return safe.replace(new RegExp(pattern, 'gi'), match => `<em style="font-style:normal;color:#007aff;font-weight:600">${match}</em>`)
      } catch (e) {
        return safe
      }
    }
  }
}
</script>

<style scoped>
.global-search-layer {
  position: fixed;
  inset: 0;
  z-index: 4000;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding: max(20px, env(safe-area-inset-top)) 16px 24px;
}
.gs-backdrop {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.32);
  backdrop-filter: blur(3px);
  -webkit-backdrop-filter: blur(3px);
}
.gs-panel {
  position: relative;
  width: min(680px, 100%);
  max-height: min(76vh, 720px);
  display: flex;
  flex-direction: column;
  border-radius: 18px;
  background: var(--bg-elevated, #fff);
  box-shadow: 0 24px 70px rgba(0, 0, 0, 0.24), 0 3px 10px rgba(0, 0, 0, 0.08);
  overflow: hidden;
  animation: gs-pop 220ms cubic-bezier(0.2, 0.9, 0.3, 1);
}
@keyframes gs-pop {
  from { opacity: 0; transform: translateY(-10px) scale(0.98); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}
.gs-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 12px 8px;
  background: var(--bg-elevated, #fff);
}
.gs-icon-btn {
  width: 34px;
  height: 34px;
  flex: none;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 0;
  border-radius: 50%;
  background: transparent;
  color: var(--text-primary, #1c1c1e);
  cursor: pointer;
}
.gs-icon-btn:hover { background: var(--fill, rgba(118, 118, 128, 0.12)); }
.gs-input-wrap {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 6px;
  height: 38px;
  padding: 0 10px;
  border-radius: 10px;
  background: var(--fill, rgba(118, 118, 128, 0.12));
}
.gs-search-icon { color: var(--text-tertiary, #8e8e93); flex: none; }
.gs-input {
  flex: 1;
  min-width: 0;
  height: 100%;
  border: 0;
  outline: 0;
  background: transparent;
  color: var(--text-primary, #1c1c1e);
  font-size: 15px;
}
.gs-input::-webkit-search-cancel-button { display: none; }
.gs-clear {
  width: 22px;
  height: 22px;
  border: 0;
  border-radius: 50%;
  background: rgba(118, 118, 128, 0.22);
  color: var(--text-secondary, #636366);
  font-size: 15px;
  line-height: 20px;
  cursor: pointer;
}
.gs-cancel {
  flex: none;
  border: 0;
  background: transparent;
  color: #007aff;
  font-size: 15px;
  padding: 6px 4px;
  cursor: pointer;
}
.gs-filters {
  display: flex;
  gap: 8px;
  padding: 2px 14px 10px;
  overflow-x: auto;
  scrollbar-width: none;
  border-bottom: 1px solid var(--separator, rgba(60, 60, 67, 0.1));
}
.gs-filters::-webkit-scrollbar { display: none; }
.gs-chip {
  flex: none;
  height: 28px;
  padding: 0 12px;
  border: 0;
  border-radius: 999px;
  background: var(--fill, rgba(118, 118, 128, 0.12));
  color: var(--text-secondary, #3a3a3c);
  font-size: 13px;
  cursor: pointer;
}
.gs-chip.active {
  background: #007aff;
  color: #fff;
}
.gs-chip-count {
  margin-left: 4px;
  font-size: 11px;
  opacity: 0.78;
}
.gs-subfilters {
  padding-top: 8px;
  border-bottom: 1px solid var(--separator, rgba(60, 60, 67, 0.1));
}
.gs-chip-sm {
  height: 26px;
  padding: 0 10px;
  font-size: 12px;
}
.gs-section { padding-bottom: 4px; }
.gs-section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 10px 2px;
}
.gs-section-head h3 {
  margin: 0;
  font-size: 12px;
  font-weight: 600;
  color: var(--text-tertiary, #8e8e93);
  letter-spacing: 0.04em;
}
.gs-more-link {
  border: 0;
  background: transparent;
  color: #007aff;
  font-size: 12px;
  cursor: pointer;
  padding: 4px;
}
.gs-inline-error {
  margin: 8px 10px;
  padding: 8px 10px;
  border-radius: 8px;
  background: rgba(255, 149, 0, 0.1);
  color: #c2410c;
  font-size: 12px;
}
.gs-avatar-group { background: linear-gradient(145deg, #34c759, #0a84ff); }
.gs-avatar-file {
  background: linear-gradient(145deg, #8e8e93, #48484a);
  font-size: 11px;
  letter-spacing: 0.02em;
}
.gs-badge-neutral {
  background: var(--fill, rgba(118, 118, 128, 0.12));
  color: var(--text-secondary, #636366);
}
.gs-state-compact { min-height: 140px; }
.gs-list { padding-bottom: 8px; }
.gs-body {
  flex: 1;
  min-height: 180px;
  overflow-y: auto;
  overscroll-behavior: contain;
  padding: 6px 8px 14px;
}
.gs-state {
  min-height: 220px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 24px;
  text-align: center;
  color: var(--text-tertiary, #8e8e93);
}
.gs-state-art { color: rgba(0, 122, 255, 0.45); }
.gs-state-title { margin: 0; font-size: 15px; font-weight: 600; color: var(--text-secondary, #3a3a3c); }
.gs-state-desc { margin: 0; font-size: 13px; line-height: 1.5; }
.gs-state-error .gs-state-title { color: #c2410c; }
.gs-retry {
  margin-top: 4px;
  border: 0;
  border-radius: 999px;
  padding: 7px 18px;
  background: #007aff;
  color: #fff;
  font-size: 13px;
  cursor: pointer;
}
.gs-spinner {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  border: 2px solid rgba(0, 122, 255, 0.22);
  border-top-color: #007aff;
  animation: gs-spin 760ms linear infinite;
}
.gs-spinner-sm { width: 14px; height: 14px; display: inline-block; vertical-align: -2px; }
@keyframes gs-spin { to { transform: rotate(360deg); } }
.gs-count {
  margin: 6px 8px 8px;
  font-size: 12px;
  color: var(--text-tertiary, #8e8e93);
}
.gs-result {
  width: 100%;
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 10px 8px;
  border: 0;
  border-radius: 12px;
  background: transparent;
  text-align: left;
  cursor: pointer;
  color: inherit;
}
.gs-result:hover,
.gs-result:focus-visible { background: var(--fill, rgba(118, 118, 128, 0.1)); outline: none; }
.gs-avatar {
  width: 38px;
  height: 38px;
  flex: none;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border-radius: 10px;
  background: linear-gradient(145deg, #5ac8fa, #007aff);
  color: #fff;
  font-size: 16px;
  font-weight: 600;
}
.gs-avatar img { width: 100%; height: 100%; object-fit: cover; }
.gs-result-main { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 3px; }
.gs-result-top {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}
.gs-conversation {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary, #1c1c1e);
}
.gs-badge {
  flex: none;
  padding: 1px 5px;
  border-radius: 4px;
  background: rgba(0, 122, 255, 0.1);
  color: #007aff;
  font-size: 10px;
}
.gs-time {
  flex: none;
  margin-left: auto;
  font-size: 11px;
  color: var(--text-tertiary, #8e8e93);
}
.gs-snippet {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  font-size: 13px;
  line-height: 1.45;
  color: var(--text-secondary, #3a3a3c);
  word-break: break-word;
}
.gs-snippet :deep(em) {
  font-style: normal;
  color: #007aff;
  font-weight: 600;
}
.gs-result-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 11px;
  color: var(--text-tertiary, #8e8e93);
}
.gs-type {
  padding: 1px 5px;
  border-radius: 4px;
  background: var(--fill, rgba(118, 118, 128, 0.12));
}
.gs-chevron {
  align-self: center;
  flex: none;
  color: var(--text-tertiary, #c7c7cc);
  font-size: 22px;
  line-height: 1;
}
.gs-more {
  padding: 14px 0 6px;
  text-align: center;
  font-size: 12px;
  color: var(--text-tertiary, #8e8e93);
}
@media (max-width: 600px) {
  .global-search-layer { padding: 0; align-items: stretch; }
  .gs-panel { width: 100%; max-height: 100vh; height: 100%; border-radius: 0; }
  .gs-header { padding-top: calc(10px + env(safe-area-inset-top)); }
}
html[data-theme="dark"] .gs-panel { background: #1c1c1e; }
html[data-theme="dark"] .gs-input { color: #f2f2f7; }
html[data-theme="dark"] .gs-result:hover { background: rgba(255, 255, 255, 0.08); }
@media (prefers-reduced-motion: reduce) {
  .gs-panel, .gs-spinner { animation: none; }
}
</style>
