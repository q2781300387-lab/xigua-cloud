<template>
  <div class="call-records-page">
    <div class="page-head">
      <div class="head-text">
        <h1>通话记录</h1>
      </div>
      <div class="head-actions">
        <button class="clear-btn" :disabled="loading || !records.length" @click="clearAll">
          <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
            <path d="M4 7h16" />
            <path d="M9 7V5h6v2" />
            <path d="M7 7l1 13h8l1-13" />
            <path d="M10 11v5M14 11v5" />
          </svg>
          <span>清空</span>
        </button>
        <button class="refresh-btn" :disabled="loading" @click="reload">
          <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
            <path d="M20 11a8 8 0 1 0-2.3 6.4" />
            <path d="M20 5v6h-6" />
          </svg>
          <span>刷新</span>
        </button>
      </div>
    </div>

    <div class="filter-bar">
      <button
        v-for="tab in tabs"
        :key="tab.key"
        class="filter-chip"
        :class="{ active: filter === tab.key }"
        @click="switchFilter(tab.key)"
      >
        {{ tab.label }}
      </button>

      <div class="search-box">
        <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
          <circle cx="11" cy="11" r="7" />
          <path d="M17 17l4 4" />
        </svg>
        <input v-model="keyword" type="text" placeholder="搜索联系人" @input="onSearchInput" @keyup.enter="applySearch" />
      </div>

      <select v-model="range" class="range-select" @change="fetchRecords(true)">
        <option value="all">全部时间</option>
        <option value="today">今天</option>
        <option value="week">近 7 天</option>
        <option value="month">近 30 天</option>
      </select>

      <span class="total-hint">共 {{ totalCount }} 条</span>
    </div>

    <div v-if="loading && !records.length" class="state-box">
      <span class="spinner"></span>
      <p>正在加载通话记录…</p>
    </div>

    <div v-else-if="errorText" class="state-box">
      <p>{{ errorText }}</p>
      <button class="retry-btn" @click="reload">重试</button>
    </div>

    <div v-else-if="!records.length" class="state-box empty">
      <svg viewBox="0 0 24 24" width="38" height="38" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round">
        <rect x="7" y="2.5" width="10" height="19" rx="2.5" />
        <path d="M10.5 18.5h3" />
      </svg>
      <p>{{ filter === 'missed' ? '没有未接来电' : '还没有通话记录' }}</p>
      <span>视频通话结束后会自动出现在这里</span>
    </div>

    <ul v-else class="record-list">
      <li
        v-for="item in records"
        :key="item.messageId"
        class="record-row"
        :class="{ missed: item.missed }"
        @click="openPeerChat(item)"
      >
        <div class="avatar">
          <img v-if="item.peerAvatar" :src="item.peerAvatar" alt="" @error="onAvatarError($event)" />
          <span v-else>{{ (item.peerName || '?').slice(0, 1) }}</span>
        </div>

        <div class="record-main">
          <div class="record-title">
            <span class="peer-name">{{ item.peerName || '未知用户' }}</span>
            <span class="record-time">{{ formatTime(item.createTime) }}</span>
          </div>
          <div class="record-sub">
            <svg
              class="direction-icon"
              :class="item.direction"
              viewBox="0 0 24 24"
              width="14"
              height="14"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
            >
              <path v-if="item.direction === 'outgoing'" d="M7 17L17 7" />
              <path v-if="item.direction === 'outgoing'" d="M9 7h8v8" />
              <path v-if="item.direction === 'incoming'" d="M17 7L7 17" />
              <path v-if="item.direction === 'incoming'" d="M15 17H7V9" />
            </svg>
            <span class="outcome-text" :class="{ danger: item.missed }">{{ outcomeText(item) }}</span>
            <span v-if="item.outcome === 'completed' && item.duration > 0" class="duration">{{ formatDuration(item.duration) }}</span>
          </div>
        </div>

        <div class="row-actions">
          <button class="row-icon-btn call" title="回拨" @click.stop="redial(item)">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M15 10l4.5-2.5v9L15 14" />
              <rect x="3" y="7" width="12" height="10" rx="2.5" />
            </svg>
          </button>
          <button class="row-icon-btn delete" title="删除记录" @click.stop="deleteOne(item)">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
              <path d="M4 7h16" />
              <path d="M9 7V5h6v2" />
              <path d="M7 7l1 13h8l1-13" />
            </svg>
          </button>
          <button class="row-open-btn" title="打开聊天" @click.stop="openPeerChat(item)">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M9 5l7 7-7 7" />
            </svg>
          </button>
        </div>
      </li>
    </ul>

    <div v-if="records.length && hasMore" class="load-more">
      <button :disabled="loadingMore" @click="loadMore">{{ loadingMore ? '加载中…' : '加载更多' }}</button>
    </div>
    <div v-else-if="records.length" class="list-end">已经到底了</div>
  </div>
</template>

<script>
import { getCallRecords, markCallRead, deleteCallRecord, clearCallRecords } from '@/api/chatMessage.js'
import { set } from '@/utils/localStorage'

const PAGE_SIZE = 20

export default {
  name: 'CallRecords',
  data() {
    return {
      records: [],
      pageNum: 1,
      totalPage: 1,
      totalCount: 0,
      filter: 'all',
      keyword: '',
      range: 'all',
      loading: false,
      loadingMore: false,
      errorText: '',
      requestSeq: 0,
      searchTimer: null,
      tabs: [
        { key: 'all', label: '全部' },
        { key: 'missed', label: '未接' }
      ]
    }
  },
  computed: {
    hasMore() {
      return this.pageNum < this.totalPage
    }
  },
  mounted() {
    this.fetchRecords(true)
  },
  beforeDestroy() {
    if (this.searchTimer) clearTimeout(this.searchTimer)
  },
  methods: {
    async reload() {
      await this.fetchRecords(true)
    },
    switchFilter(key) {
      if (this.filter === key) return
      this.filter = key
      this.fetchRecords(true)
    },
    onSearchInput() {
      if (this.searchTimer) clearTimeout(this.searchTimer)
      this.searchTimer = setTimeout(() => this.fetchRecords(true), 320)
    },
    applySearch() {
      if (this.searchTimer) clearTimeout(this.searchTimer)
      this.fetchRecords(true)
    },
    rangeParams() {
      if (this.range === 'all') return {}
      const now = new Date()
      const start = new Date(now)
      if (this.range === 'today') {
        start.setHours(0, 0, 0, 0)
      } else {
        start.setDate(start.getDate() - (this.range === 'week' ? 6 : 29))
        start.setHours(0, 0, 0, 0)
      }
      return {
        startTime: this.toDateTime(start),
        endTime: this.toDateTime(now)
      }
    },
    toDateTime(date) {
      const pad = (n) => String(n).padStart(2, '0')
      return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ` +
        `${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
    },
    async fetchRecords(reset) {
      const seq = ++this.requestSeq
      if (reset) {
        this.loading = true
        this.errorText = ''
        this.pageNum = 1
      } else {
        this.loadingMore = true
      }
      try {
        const res = await getCallRecords({
          pageNum: reset ? 1 : this.pageNum + 1,
          pageSize: PAGE_SIZE,
          missedOnly: this.filter === 'missed',
          keyword: this.keyword.trim(),
          ...this.rangeParams()
        })
        if (seq !== this.requestSeq) return
        if (!res || res.code !== 200 || !res.data) {
          throw new Error((res && res.msg) || '加载失败')
        }
        const page = res.data
        const rows = Array.isArray(page.rows) ? page.rows : []
        this.records = reset ? rows : this.records.concat(rows)
        this.pageNum = page.pageNum || 1
        this.totalPage = page.totalPage || 1
        this.totalCount = page.totalCount || 0
        if (reset) {
          // 记录页打开即视为已看过，PC / 手机共用同一已读水位，清空未接角标
          markCallRead().catch(() => {})
        }
      } catch (e) {
        if (seq !== this.requestSeq) return
        this.errorText = (e && (e.msg || e.message)) || '加载失败，请稍后重试'
        if (reset) {
          this.records = []
          this.totalCount = 0
        }
      } finally {
        if (seq === this.requestSeq) {
          this.loading = false
          this.loadingMore = false
        }
      }
    },
    loadMore() {
      if (this.loadingMore || !this.hasMore) return
      this.fetchRecords(false)
    },
    outcomeText(item) {
      if (item.direction === 'outgoing') {
        if (item.outcome === 'completed') return '通话时长'
        if (item.outcome === 'busy') return '对方忙线中'
        if (item.outcome === 'canceled') return '已取消'
        if (item.outcome === 'timeout') return '对方无应答'
        if (item.outcome === 'rejected') return '对方已拒绝'
        return '未接通'
      }
      if (item.outcome === 'completed') return '通话时长'
      if (item.outcome === 'busy') return '忙线未接'
      if (item.outcome === 'timeout') return '未接来电'
      if (item.outcome === 'rejected') return '已拒绝'
      if (item.outcome === 'canceled') return '对方已取消'
      return '未接通'
    },
    formatDuration(sec) {
      const s = Number(sec) || 0
      if (s < 60) return `${s} 秒`
      const m = Math.floor(s / 60)
      const r = s % 60
      return r ? `${m} 分 ${r} 秒` : `${m} 分钟`
    },
    formatTime(value) {
      if (!value) return ''
      const date = new Date(value.replace(/-/g, '/'))
      if (Number.isNaN(date.getTime())) return value
      const now = new Date()
      const sameDay = date.toDateString() === now.toDateString()
      const pad = (n) => String(n).padStart(2, '0')
      if (sameDay) return `今天 ${pad(date.getHours())}:${pad(date.getMinutes())}`
      const yesterday = new Date(now.getTime() - 86400000)
      if (date.toDateString() === yesterday.toDateString()) {
        return `昨天 ${pad(date.getHours())}:${pad(date.getMinutes())}`
      }
      if (date.getFullYear() === now.getFullYear()) {
        return `${date.getMonth() + 1}月${date.getDate()}日 ${pad(date.getHours())}:${pad(date.getMinutes())}`
      }
      return `${date.getFullYear()}年${date.getMonth() + 1}月${date.getDate()}日`
    },
    redial(item) {
      if (!item.peerId) return
      set('openChat', JSON.stringify({
        chatId: item.peerId,
        chatType: 1,
        chatName: item.peerName || '',
        avatar: item.peerAvatar || '',
        autoVideoCall: true
      }))
      set('activeMenu', 'chat')
      this.$router.push({ path: '/chat', query: { t: Date.now() } })
    },
    async deleteOne(item) {
      if (!item.messageId) return
      if (!window.confirm('删除这条通话记录？仅你自己不可见，对方记录不受影响。')) return
      try {
        const res = await deleteCallRecord({ messageId: item.messageId })
        if (!res || res.code !== 200) throw new Error((res && res.msg) || '删除失败')
        this.records = this.records.filter((row) => row.messageId !== item.messageId)
        this.totalCount = Math.max(0, this.totalCount - 1)
      } catch (e) {
        window.alert((e && (e.msg || e.message)) || '删除失败，请稍后重试')
      }
    },
    async clearAll() {
      if (!this.records.length) return
      if (!window.confirm('清空全部通话记录？仅你自己不可见，对方记录不受影响。')) return
      try {
        const res = await clearCallRecords()
        if (!res || res.code !== 200) throw new Error((res && res.msg) || '清空失败')
        this.records = []
        this.pageNum = 1
        this.totalPage = 1
        this.totalCount = 0
      } catch (e) {
        window.alert((e && (e.msg || e.message)) || '清空失败，请稍后重试')
      }
    },
    openPeerChat(item) {
      if (!item.peerId) return
      set('openChat', JSON.stringify({
        chatId: item.peerId,
        chatType: 1,
        chatName: item.peerName || '',
        avatar: item.peerAvatar || ''
      }))
      set('activeMenu', 'chat')
      this.$router.push({ path: '/chat', query: { t: Date.now() } })
    },
    onAvatarError(event) {
      if (event && event.target) event.target.style.display = 'none'
    }
  }
}
</script>

<style scoped>
.call-records-page {
  height: 100%;
  box-sizing: border-box;
  padding: 28px 32px 40px;
  overflow-y: auto;
  color: var(--label);
  background: var(--bg-gradient);
}

.page-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}
.page-head h1 {
  margin: 0;
  font-size: 26px;
  font-weight: 700;
  letter-spacing: -0.3px;
}
.page-head p {
  margin: 6px 0 0;
  font-size: 13px;
  color: var(--label-secondary);
}
.head-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}
.clear-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 7px 14px;
  border-radius: 16px;
  border: 0.5px solid var(--separator-strong);
  background: var(--card);
  color: var(--label-secondary);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.18s ease;
}
.clear-btn:hover { color: var(--accent-red); border-color: var(--accent-red); }
.clear-btn:disabled { opacity: 0.45; cursor: default; }
.refresh-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 7px 14px;
  border-radius: 16px;
  border: 0.5px solid var(--separator-strong);
  background: var(--card);
  color: var(--label-secondary);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.18s ease;
}
.refresh-btn:hover { color: var(--accent); border-color: var(--accent); }
.refresh-btn:disabled { opacity: 0.55; cursor: default; }

.filter-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
}
.filter-chip {
  padding: 7px 18px;
  border-radius: 16px;
  border: none;
  background: var(--fill);
  color: var(--label-secondary);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.18s ease;
}
.filter-chip.active {
  background: var(--accent);
  color: #fff;
  box-shadow: 0 4px 14px rgba(0, 122, 255, 0.25);
}
.total-hint {
  margin-left: auto;
  font-size: 12px;
  color: var(--label-tertiary);
}
.search-box {
  display: flex;
  align-items: center;
  gap: 7px;
  padding: 0 12px;
  height: 33px;
  border-radius: 16px;
  background: var(--fill);
  color: var(--label-tertiary);
}
.search-box input {
  width: 140px;
  border: none;
  outline: none;
  background: transparent;
  color: var(--label);
  font-size: 13px;
}
.search-box input::placeholder { color: var(--label-tertiary); }
.range-select {
  height: 33px;
  padding: 0 12px;
  border-radius: 16px;
  border: none;
  outline: none;
  background: var(--fill);
  color: var(--label-secondary);
  font-size: 13px;
  cursor: pointer;
}

.record-list {
  list-style: none;
  margin: 0;
  padding: 0;
  background: var(--card);
  border-radius: 16px;
  overflow: hidden;
  box-shadow: var(--shadow-sm);
}
.record-row {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 15px 18px;
  cursor: pointer;
  transition: background 0.16s ease;
  border-bottom: 0.5px solid var(--separator);
}
.record-row:last-child { border-bottom: none; }
.record-row:hover { background: var(--card-highlight); }
.record-row.missed .peer-name { color: var(--accent-red); }

.avatar {
  flex: none;
  width: 44px;
  height: 44px;
  border-radius: 12px;
  overflow: hidden;
  background: var(--accent-gradient);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 17px;
  font-weight: 600;
  color: var(--accent);
}
.avatar img { width: 100%; height: 100%; object-fit: cover; display: block; }

.record-main { flex: 1; min-width: 0; }
.record-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.peer-name {
  font-size: 15px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.record-time {
  flex: none;
  font-size: 12px;
  color: var(--label-tertiary);
}
.record-sub {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 5px;
  font-size: 13px;
  color: var(--label-secondary);
}
.direction-icon.outgoing { color: var(--accent); }
.direction-icon.incoming { color: var(--label-tertiary); }
.outcome-text.danger { color: var(--accent-red); }
.duration {
  color: var(--label-tertiary);
  font-size: 12px;
}

.row-actions {
  flex: none;
  display: flex;
  align-items: center;
  gap: 8px;
}
.row-icon-btn {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  border: none;
  background: var(--fill);
  color: var(--label-tertiary);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.16s ease;
}
.row-icon-btn.call:hover { background: var(--accent); color: #fff; }
.row-icon-btn.delete:hover { background: var(--accent-red); color: #fff; }
.row-open-btn {
  flex: none;
  width: 30px;
  height: 30px;
  border-radius: 50%;
  border: none;
  background: var(--fill);
  color: var(--label-tertiary);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.16s ease;
}
.row-open-btn:hover { background: var(--accent); color: #fff; }

.state-box {
  padding: 72px 20px;
  text-align: center;
  color: var(--label-secondary);
  background: var(--card);
  border-radius: 16px;
  box-shadow: var(--shadow-sm);
}
.state-box.empty svg { color: var(--label-tertiary); }
.state-box p { margin: 12px 0 4px; font-size: 14px; }
.state-box span { font-size: 12px; color: var(--label-tertiary); }
.spinner {
  display: inline-block;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  border: 2px solid var(--fill-strong);
  border-top-color: var(--accent);
  animation: spin 0.8s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }
.retry-btn {
  margin-top: 14px;
  padding: 7px 20px;
  border: none;
  border-radius: 16px;
  background: var(--accent);
  color: #fff;
  cursor: pointer;
}

.load-more {
  display: flex;
  justify-content: center;
  margin-top: 16px;
}
.load-more button {
  padding: 8px 26px;
  border-radius: 18px;
  border: 0.5px solid var(--separator-strong);
  background: var(--card);
  color: var(--label-secondary);
  font-size: 13px;
  cursor: pointer;
}
.load-more button:disabled { opacity: 0.5; }
.list-end {
  padding: 18px 0 4px;
  text-align: center;
  font-size: 12px;
  color: var(--label-tertiary);
}
</style>
