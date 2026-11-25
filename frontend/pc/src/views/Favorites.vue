<template>
  <div class="favorites-page">
    <div class="page-head">
      <div class="head-text">
        <h1>收藏夹</h1>
        <p>消息以快照保存，原消息撤回后仍可在这里查看</p>
      </div>
      <button class="refresh-btn" :disabled="loading" @click="load(true)">
        <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
          <path d="M20 11a8 8 0 1 0-2.3 6.4" />
          <path d="M20 5v6h-6" />
        </svg>
        <span>刷新</span>
      </button>
    </div>

    <div class="filter-bar">
      <button
          v-for="tab in tabs"
          :key="tab.key"
          class="filter-chip"
          :class="{ active: currentType === tab.key }"
          @click="switchType(tab.key)"
      >{{ tab.label }}</button>

      <div class="search-box">
        <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
          <circle cx="11" cy="11" r="7" />
          <path d="M17 17l4 4" />
        </svg>
        <input v-model="keyword" type="text" placeholder="搜索内容、会话或发送人" @input="onSearchInput" @keyup.enter="load(true)" />
        <button v-if="keyword" class="search-clear" @click="clearKeyword">×</button>
      </div>

      <span class="total-hint">共 {{ totalCount }} 条</span>
    </div>

    <div v-if="loading && !items.length" class="state-box">
      <span class="spinner"></span>
      <p>正在加载收藏…</p>
    </div>

    <div v-else-if="errorText" class="state-box">
      <p>{{ errorText }}</p>
      <button class="retry-btn" @click="load(true)">重试</button>
    </div>

    <div v-else-if="!items.length" class="state-box empty">
      <svg viewBox="0 0 24 24" width="40" height="40" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round">
        <path d="M12 3.2l2.7 5.5 6 .9-4.3 4.2 1 6-5.4-2.8-5.4 2.8 1-6L4.3 9.6l6-.9z" />
      </svg>
      <p>{{ keyword ? '没有匹配的收藏' : '还没有收藏消息' }}</p>
      <span>在聊天里右键消息，选择「收藏」即可加入这里</span>
    </div>

    <ul v-else class="favorite-list">
      <li
          v-for="item in items"
          :key="item.id"
          class="favorite-row"
          :class="{ active: activeItem && activeItem.id === item.id }"
          @click="openItem(item)"
      >
        <div class="type-badge" :class="'type-' + (item.messageType || 1)">
          <span>{{ typeLabel(item.messageType).slice(0, 1) }}</span>
        </div>

        <div class="favorite-main">
          <div class="favorite-title">
            <span class="chat-name">{{ item.chatName || '聊天' }}</span>
            <span class="type-label">{{ typeLabel(item.messageType) }}</span>
            <span class="favorite-time">{{ formatTime(item.createTime) }}</span>
          </div>

          <div class="favorite-body">
            <img v-if="Number(item.messageType) === 2 && isImageContent(item.content)" :src="item.content" class="favorite-thumb" alt="" />
            <div class="favorite-text">
              <span class="sender">{{ item.senderName || '对方' }}：</span>{{ previewText(item) }}
            </div>
          </div>
        </div>

        <button class="row-delete" title="移除收藏" @click.stop="removeItem(item)">
          <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round">
            <path d="M4 7h16" />
            <path d="M9 7V5h6v2" />
            <path d="M7 7l1 13h8l1-13" />
          </svg>
        </button>
      </li>
    </ul>

    <div v-if="items.length && hasMore" class="load-more">
      <button :disabled="loadingMore" @click="loadMore">{{ loadingMore ? '加载中…' : '加载更多' }}</button>
    </div>

    <!-- 收藏详情：点开卡片后在右侧展开，不打断列表浏览 -->
    <div v-if="activeItem" class="detail-mask" @click.self="activeItem = null">
      <aside class="detail-panel">
        <div class="detail-head">
          <div>
            <span class="detail-chat">{{ activeItem.chatName || '聊天' }}</span>
            <span class="detail-time">{{ activeItem.originalTime || activeItem.createTime }}</span>
          </div>
          <button class="detail-close" @click="activeItem = null">×</button>
        </div>

        <div class="detail-meta">
          <span>{{ activeItem.senderName || '对方' }}</span>
          <span class="dot">·</span>
          <span>{{ typeLabel(activeItem.messageType) }}</span>
        </div>

        <div class="detail-content">
          <img
              v-if="Number(activeItem.messageType) === 2 && isImageContent(activeItem.content)"
              :src="activeItem.content"
              class="detail-image"
              alt=""
          />
          <div v-else-if="Number(activeItem.messageType) === 7" class="merge-detail">
            <div class="merge-detail-head">聊天记录 · 共 {{ parseMerged(activeItem.content).length }} 条</div>
            <div v-for="(it, i) in parseMerged(activeItem.content)" :key="i" class="merge-item">
              <div class="merge-item-head">
                <span class="merge-sender">{{ it.s || it.senderName || '成员' }}</span>
                <span class="merge-time">{{ it.t || it.time || '' }}</span>
              </div>
              <div class="merge-item-text">{{ it.c || it.content || '' }}</div>
            </div>
            <div v-if="!parseMerged(activeItem.content).length" class="merge-empty">聊天记录内容不可解析，已保留原始快照</div>
          </div>
          <pre v-else class="detail-text">{{ detailText(activeItem) }}</pre>
        </div>

        <div class="detail-actions">
          <button class="detail-btn" @click="copyContent">复制内容</button>
          <button class="detail-btn danger" @click="removeItem(activeItem)">移除收藏</button>
        </div>
      </aside>
    </div>
  </div>
</template>

<script>
import { getMessageFavorites, removeMessageFavorite } from '@/api/chatMessage.js'
import { ElMessage } from '@/plugins/message'

export default {
  name: 'Favorites',
  data() {
    return {
      tabs: [
        { key: 'all', label: '全部' },
        { key: '1', label: '文字' },
        { key: '2', label: '图片' },
        { key: '5', label: '文件' },
        { key: '7', label: '聊天记录' }
      ],
      currentType: 'all',
      keyword: '',
      items: [],
      totalCount: 0,
      pageNum: 1,
      pageSize: 20,
      totalPage: 1,
      loading: false,
      loadingMore: false,
      errorText: '',
      searchTimer: null,
      activeItem: null
    }
  },
  computed: {
    hasMore() {
      return this.pageNum < this.totalPage
    }
  },
  mounted() {
    this.load(true)
  },
  beforeUnmount() {
    if (this.searchTimer) clearTimeout(this.searchTimer)
  },
  methods: {
    async load(reset = false) {
      if (reset) {
        this.pageNum = 1
        this.items = []
        this.totalCount = 0
        this.totalPage = 1
        this.loading = true
        this.errorText = ''
        this.activeItem = null
      } else {
        this.loadingMore = true
      }
      try {
        const res = await getMessageFavorites({
          pageNum: this.pageNum,
          pageSize: this.pageSize,
          keyword: this.keyword.trim(),
          messageType: this.currentType === 'all' ? null : Number(this.currentType)
        })
        const data = res.data || {}
        this.items = reset ? (data.rows || []) : [...this.items, ...(data.rows || [])]
        this.totalCount = Number(data.totalCount || 0)
        this.totalPage = Number(data.totalPage || 1)
      } catch (error) {
        this.errorText = (error && error.message) || '收藏列表加载失败，请稍后重试'
      } finally {
        this.loading = false
        this.loadingMore = false
      }
    },

    loadMore() {
      if (this.loadingMore || !this.hasMore) return
      this.pageNum += 1
      this.load(false)
    },

    switchType(key) {
      if (this.currentType === key) return
      this.currentType = key
      this.load(true)
    },

    onSearchInput() {
      if (this.searchTimer) clearTimeout(this.searchTimer)
      this.searchTimer = setTimeout(() => this.load(true), 350)
    },

    clearKeyword() {
      this.keyword = ''
      this.load(true)
    },

    openItem(item) {
      this.activeItem = item
    },

    async removeItem(item) {
      if (!item || !item.messageId) return
      if (!window.confirm('从收藏夹移除这条消息？仅影响你自己的收藏。')) return
      try {
        await removeMessageFavorite({ messageId: item.messageId })
        this.items = this.items.filter(row => row.messageId !== item.messageId)
        this.totalCount = Math.max(0, this.totalCount - 1)
        if (this.activeItem && this.activeItem.messageId === item.messageId) this.activeItem = null
        ElMessage.success('已移除收藏')
      } catch (error) {
        ElMessage.error((error && error.message) || '移除失败，请重试')
      }
    },

    async copyContent() {
      const text = this.activeItem ? this.detailText(this.activeItem) : ''
      if (!text) return
      try {
        await navigator.clipboard.writeText(text)
        ElMessage.success('已复制')
      } catch (e) {
        ElMessage.error('复制失败，请手动选择文本')
      }
    },

    typeLabel(type) {
      const map = { 1: '文字', 2: '图片', 3: '语音', 4: '视频', 5: '文件', 6: '系统', 7: '聊天记录', 8: '投票', 9: '通话' }
      return map[Number(type)] || '消息'
    },

    isImageContent(content) {
      return /^(https?:\/\/|\/media\/|data:image\/)/.test(String(content || ''))
    },

    previewText(item) {
      const type = Number(item.messageType)
      const raw = String(item.content || '')
      if (type === 2) return '[图片]'
      if (type === 3) return '[语音]'
      if (type === 4) return '[视频]'
      if (type === 5) {
        const name = this.fileNameFromUrl(raw)
        return name ? '[文件] ' + name : '[文件]'
      }
      if (type === 7) return `[聊天记录] ${this.parseMerged(raw).length} 条`
      if (type === 8) return '[投票]'
      if (type === 9) return '[通话记录]'
      return raw.replace(/\s+/g, ' ').slice(0, 90) || '[空消息]'
    },

    detailText(item) {
      const type = Number(item.messageType)
      if (type === 2) return '[图片] ' + item.content
      if (type === 3) return '[语音] ' + item.content
      if (type === 4) return '[视频] ' + item.content
      if (type === 5) return '[文件] ' + (this.fileNameFromUrl(item.content) || item.content)
      return String(item.content || '')
    },

    fileNameFromUrl(raw) {
      try {
        const url = String(raw || '')
        const last = url.split('?')[0].split('/').pop() || ''
        return decodeURIComponent(last)
      } catch (e) {
        return ''
      }
    },

    parseMerged(content) {
      try {
        let raw = String(content || '[]')
        if (raw.indexOf('【聊天记录】') === 0) raw = raw.slice('【聊天记录】'.length)
        const arr = JSON.parse(raw)
        return Array.isArray(arr) ? arr : []
      } catch (e) {
        return []
      }
    },

    formatTime(value) {
      if (!value) return ''
      const text = String(value).replace('T', ' ')
      return text.length >= 16 ? text.slice(0, 16) : text
    }
  }
}
</script>

<style scoped>
.favorites-page {
  position: relative;
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
  padding: 22px 26px 26px;
  gap: 14px;
  box-sizing: border-box;
  color: var(--label, #111);
}

.page-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.head-text h1 {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
  letter-spacing: -0.3px;
}

.head-text p {
  margin: 5px 0 0;
  font-size: 12px;
  color: var(--secondary-label, #6e6e73);
}

.refresh-btn,
.retry-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  height: 32px;
  padding: 0 12px;
  border-radius: 9px;
  border: 1px solid var(--separator, rgba(60, 60, 67, 0.16));
  background: var(--card, rgba(255, 255, 255, 0.75));
  color: var(--label, #111);
  font-size: 12px;
  cursor: pointer;
}

.refresh-btn:disabled {
  opacity: 0.55;
}

.filter-bar {
  display: flex;
  align-items: center;
  gap: 7px;
  flex-wrap: wrap;
}

.filter-chip {
  height: 28px;
  padding: 0 12px;
  border-radius: 14px;
  border: 1px solid var(--separator, rgba(60, 60, 67, 0.14));
  background: transparent;
  color: var(--secondary-label, #6e6e73);
  font-size: 12px;
  cursor: pointer;
  transition: all 0.15s ease;
}

.filter-chip.active {
  background: var(--label, #111);
  border-color: var(--label, #111);
  color: var(--card, #fff);
}

.search-box {
  display: flex;
  align-items: center;
  gap: 6px;
  height: 30px;
  padding: 0 9px;
  border-radius: 9px;
  border: 1px solid var(--separator, rgba(60, 60, 67, 0.16));
  background: var(--card, rgba(255, 255, 255, 0.65));
  min-width: 230px;
  color: var(--secondary-label, #6e6e73);
}

.search-box input {
  flex: 1;
  min-width: 0;
  border: 0;
  outline: none;
  background: transparent;
  color: var(--label, #111);
  font-size: 12px;
}

.search-clear {
  width: 17px;
  height: 17px;
  border: 0;
  border-radius: 50%;
  background: var(--fill, rgba(118, 118, 128, 0.14));
  color: var(--secondary-label, #6e6e73);
  font-size: 12px;
  cursor: pointer;
}

.total-hint {
  margin-left: auto;
  font-size: 12px;
  color: var(--secondary-label, #6e6e73);
}

.state-box {
  flex: 1;
  min-height: 240px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: var(--secondary-label, #6e6e73);
  font-size: 13px;
  text-align: center;
}

.state-box.empty svg {
  color: var(--secondary-label, #8e8e93);
  opacity: 0.62;
}

.state-box p {
  margin: 0;
  font-weight: 600;
  color: var(--label, #111);
}

.state-box span {
  font-size: 12px;
}

.spinner {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  border: 2px solid var(--separator, rgba(60, 60, 67, 0.16));
  border-top-color: var(--accent, #007aff);
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.favorite-list {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  list-style: none;
  margin: 0;
  padding: 0 2px 10px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.favorite-row {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 13px 13px 13px 12px;
  border-radius: 14px;
  border: 1px solid var(--separator, rgba(60, 60, 67, 0.1));
  background: var(--card, rgba(255, 255, 255, 0.66));
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.03);
  cursor: pointer;
  transition: background 0.16s ease, border-color 0.16s ease, transform 0.16s ease;
}

.favorite-row:hover {
  background: var(--card, rgba(255, 255, 255, 0.9));
  transform: translateY(-1px);
}

.favorite-row.active {
  border-color: color-mix(in srgb, var(--accent, #007aff) 40%, transparent);
  background: color-mix(in srgb, var(--accent, #007aff) 7%, var(--card, #fff));
}

.type-badge {
  flex: 0 0 34px;
  width: 34px;
  height: 34px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  color: #fff;
  background: #8e8e93;
}

.type-badge.type-1 { background: #3a7afe; }
.type-badge.type-2 { background: #2aa87f; }
.type-badge.type-3 { background: #7b61d1; }
.type-badge.type-4 { background: #d15f61; }
.type-badge.type-5 { background: #b07a2f; }
.type-badge.type-7 { background: #5a6a85; }
.type-badge.type-8 { background: #a05a9d; }
.type-badge.type-9 { background: #4c8e9c; }

.favorite-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.favorite-title {
  display: flex;
  align-items: center;
  gap: 7px;
  min-width: 0;
}

.chat-name {
  max-width: 45%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
  font-weight: 650;
}

.type-label {
  padding: 1px 6px;
  border-radius: 5px;
  background: var(--fill, rgba(118, 118, 128, 0.1));
  color: var(--secondary-label, #6e6e73);
  font-size: 10.5px;
  flex: 0 0 auto;
}

.favorite-time {
  margin-left: auto;
  flex: 0 0 auto;
  font-size: 11px;
  color: var(--secondary-label, #8e8e93);
}

.favorite-body {
  display: flex;
  align-items: flex-start;
  gap: 9px;
  min-width: 0;
}

.favorite-thumb {
  width: 54px;
  height: 54px;
  border-radius: 8px;
  object-fit: cover;
  flex: 0 0 54px;
  background: var(--fill, rgba(118, 118, 128, 0.1));
}

.favorite-text {
  flex: 1;
  min-width: 0;
  font-size: 12.5px;
  line-height: 1.55;
  color: var(--secondary-label, #48484a);
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  word-break: break-word;
}

.sender {
  color: var(--label, #111);
  font-weight: 600;
}

.row-delete {
  flex: 0 0 28px;
  width: 28px;
  height: 28px;
  margin-top: 2px;
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: var(--secondary-label, #8e8e93);
  cursor: pointer;
  opacity: 0;
  transition: opacity 0.14s ease, background 0.14s ease, color 0.14s ease;
}

.favorite-row:hover .row-delete {
  opacity: 1;
}

.row-delete:hover {
  background: rgba(209, 60, 60, 0.1);
  color: #d13c3c;
}

.load-more {
  display: flex;
  justify-content: center;
  padding: 4px 0 0;
}

.load-more button {
  height: 30px;
  padding: 0 18px;
  border-radius: 15px;
  border: 1px solid var(--separator, rgba(60, 60, 67, 0.16));
  background: var(--card, rgba(255, 255, 255, 0.7));
  color: var(--secondary-label, #6e6e73);
  font-size: 12px;
  cursor: pointer;
}

.detail-mask {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.2);
  backdrop-filter: blur(2px);
  display: flex;
  justify-content: flex-end;
  z-index: 20;
}

.detail-panel {
  width: min(430px, 92%);
  height: 100%;
  background: var(--card, #fff);
  border-left: 1px solid var(--separator, rgba(60, 60, 67, 0.14));
  box-shadow: -14px 0 40px rgba(0, 0, 0, 0.12);
  display: flex;
  flex-direction: column;
  animation: slideIn 0.24s cubic-bezier(0.32, 0.72, 0, 1);
  box-sizing: border-box;
}

@keyframes slideIn {
  from { transform: translateX(28px); opacity: 0; }
  to { transform: translateX(0); opacity: 1; }
}

.detail-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding: 20px 20px 8px;
}

.detail-head > div {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.detail-chat {
  font-size: 17px;
  font-weight: 700;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.detail-time {
  font-size: 11.5px;
  color: var(--secondary-label, #8e8e93);
}

.detail-close {
  width: 28px;
  height: 28px;
  border: 0;
  border-radius: 50%;
  background: var(--fill, rgba(118, 118, 128, 0.12));
  color: var(--secondary-label, #6e6e73);
  font-size: 19px;
  line-height: 1;
  cursor: pointer;
}

.detail-meta {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 0 20px 13px;
  font-size: 11.5px;
  color: var(--secondary-label, #6e6e73);
}

.dot {
  opacity: 0.5;
}

.detail-content {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  margin: 0 16px;
  padding: 16px;
  border-radius: 14px;
  background: var(--fill, rgba(118, 118, 128, 0.07));
}

.detail-image {
  display: block;
  max-width: 100%;
  max-height: 62vh;
  margin: 0 auto;
  border-radius: 10px;
  object-fit: contain;
}

.detail-text {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: inherit;
  font-size: 14px;
  line-height: 1.7;
  color: var(--label, #111);
}

.merge-detail {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.merge-detail-head {
  font-size: 12px;
  font-weight: 600;
  color: var(--secondary-label, #6e6e73);
  padding-bottom: 6px;
  border-bottom: 1px solid var(--separator, rgba(60, 60, 67, 0.12));
}

.merge-item {
  padding: 9px 10px;
  border-radius: 9px;
  background: var(--card, rgba(255, 255, 255, 0.72));
}

.merge-item-head {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 4px;
  font-size: 11px;
  color: var(--secondary-label, #8e8e93);
}

.merge-sender {
  font-weight: 600;
  color: var(--secondary-label, #6e6e73);
}

.merge-item-text {
  font-size: 13px;
  line-height: 1.6;
  word-break: break-word;
}

.merge-empty {
  font-size: 12px;
  color: var(--secondary-label, #8e8e93);
}

.detail-actions {
  display: flex;
  gap: 8px;
  padding: 14px 18px 18px;
}

.detail-btn {
  flex: 1;
  height: 36px;
  border-radius: 10px;
  border: 1px solid var(--separator, rgba(60, 60, 67, 0.16));
  background: var(--card, #fff);
  color: var(--label, #111);
  font-size: 12.5px;
  font-weight: 550;
  cursor: pointer;
}

.detail-btn.danger {
  color: #d13c3c;
  border-color: rgba(209, 60, 60, 0.22);
  background: rgba(209, 60, 60, 0.06);
}

@media (max-width: 760px) {
  .favorites-page { padding: 16px 14px 18px; }
  .search-box { min-width: 100%; order: 5; }
  .total-hint { display: none; }
  .detail-panel { width: 100%; }
  .row-delete { opacity: 1; }
}
</style>
