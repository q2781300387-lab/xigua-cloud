<template>
  <div class="moments-wrap">
  <div
    class="moments-page"
    ref="page"
    @scroll="onScroll"
    @click="closeMenu"
    @touchstart="onTouchStart"
    @touchmove="onTouchMove"
    @touchend="onTouchEnd"
  >
    <!-- iOS 大标题导航 -->
    <header class="nav-bar">
      <div class="nav-title">朋友圈<span v-if="newCount" class="nav-new">新 {{ newCount }} 条</span></div>
      <div class="nav-actions">
        <button class="nav-camera" :class="{ spinning: refreshing }" title="刷新" @click.stop="refresh">
          <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round">
            <path d="M21 12a9 9 0 1 1-2.6-6.4" />
            <path d="M21 3v6h-6" />
          </svg>
        </button>
        <button class="nav-camera" title="发表动态" @click.stop="openComposer">
          <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round">
            <path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z" />
            <circle cx="12" cy="13" r="4" />
          </svg>
        </button>
      </div>
    </header>

    <!-- 封面区 -->
    <section class="cover">
      <div class="cover-photo" :style="{ backgroundImage: 'url(' + coverImg + ')' }"></div>
      <div class="cover-overlay"></div>
      <div class="cover-user" @click="onCoverTap">
        <span class="cover-name">{{ userInfo.username || userInfo.nickname || '我' }}</span>
        <div class="cover-avatar">
          <img :src="userInfo.avatar || defaultAvatar" alt="avatar" />
        </div>
      </div>
    </section>

    <!-- 下拉刷新提示 -->
    <div class="pull-hint" :style="{ height: pullDist + 'px', opacity: pullDist > 0 ? 1 : 0 }">
      <span>{{ pullDist >= 60 ? '松手刷新' : '下拉刷新' }}</span>
    </div>

    <!-- 动态列表 -->
    <main class="feed" :class="{ 'feed-empty': moments.length === 0 }">
      <div v-if="moments.length === 0" class="empty-state">
        <template v-if="loadError">
          <svg viewBox="0 0 24 24" width="64" height="64" fill="none" stroke="currentColor" stroke-width="1" stroke-linecap="round" stroke-linejoin="round" class="empty-icon">
            <circle cx="12" cy="12" r="9" />
            <path d="M12 7.5v6" />
            <circle cx="12" cy="16.8" r="1" fill="currentColor" stroke="none" />
          </svg>
          <p class="empty-title">加载失败</p>
          <p class="empty-sub">网络或服务异常，请稍后重试</p>
          <button class="empty-retry" @click="refresh">重新加载</button>
        </template>
        <template v-else>
          <svg viewBox="0 0 24 24" width="64" height="64" fill="none" stroke="currentColor" stroke-width="1" stroke-linecap="round" stroke-linejoin="round" class="empty-icon">
            <path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z" />
            <circle cx="12" cy="13" r="4" />
          </svg>
          <p class="empty-title">还没有动态</p>
          <p class="empty-sub">点击右上角相机，分享此刻</p>
        </template>
      </div>

      <article v-for="m in moments" :key="m.id" class="moment-card" :class="{ 'moment-new': m.isNew }">
        <img class="moment-avatar" :src="m.avatar" alt="" />
        <div class="moment-body">
          <div class="moment-head">
            <span v-if="m.isNew" class="new-dot" title="新动态"></span>
            <span class="moment-name" :style="{ color: m.nameColor }">{{ m.nickname }}</span>
            <span class="moment-time">{{ m.time }}</span>
            <span v-if="m.location" class="moment-loc">📍 {{ m.location }}</span>
            <button v-if="m.canDelete" class="moment-more" title="更多" @click.stop="openMenu(m)">•••</button>
            <div v-if="menuId === m.id" class="more-menu" @click.stop>
              <button class="more-danger" @click="removeMoment(m)">删除动态</button>
            </div>
          </div>
          <p class="moment-text moment-link" @click="goDetail(m)">{{ m.text }}</p>

          <!-- 图片九宫格 -->
          <div v-if="m.images && m.images.length" class="moment-grid" :class="'grid-' + Math.min(m.images.length, 3)">
            <div v-for="(img, i) in m.images.slice(0, 9)" :key="i" class="grid-cell" @click="previewImages(m.images, i)">
              <img :src="img" alt="" loading="lazy" />
            </div>
          </div>

          <!-- 点赞 & 评论 -->
          <div v-if="m.likes.length || m.comments.length" class="reaction-panel">
            <div v-if="m.likes.length" class="like-row" @click.stop="openLikes(m)">
              <span class="like-icon">❤️</span>
              <span class="like-list">{{ m.likes.join('，') }}</span>
              <span class="like-more">共 {{ m.likes.length }} 人 ›</span>
            </div>
            <div v-if="m.comments.length" class="comment-list">
              <div v-for="(c, ci) in m.comments" :key="c.id || ci" class="comment-row">
                <span class="comment-name" :style="{ color: c.color }">{{ c.name }}</span>
                <span class="comment-text">{{ c.text }}</span>
                <button v-if="c.mine" class="comment-del" title="删除评论" @click.stop="removeComment(m, c)">删除</button>
              </div>
            </div>
          </div>

          <!-- 操作栏 -->
          <div class="moment-actions">
            <button class="action-btn" :class="{ liked: m.liked }" @click="toggleLike(m)">
              <svg v-if="m.liked" viewBox="0 0 24 24" width="15" height="15" fill="#ff3b30">
                <path d="M20.8 4.6a5.5 5.5 0 0 0-7.8 0L12 5.6l-1-1a5.5 5.5 0 0 0-7.8 7.8l1 1L12 21l7.8-7.6 1-1a5.5 5.5 0 0 0 0-7.8z" />
              </svg>
              <svg v-else viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                <path d="M20.8 4.6a5.5 5.5 0 0 0-7.8 0L12 5.6l-1-1a5.5 5.5 0 0 0-7.8 7.8l1 1L12 21l7.8-7.6 1-1a5.5 5.5 0 0 0 0-7.8z" />
              </svg>
              <span>{{ m.liked ? '已赞' : '赞' }}</span>
            </button>
            <button class="action-btn" @click="toggleComment(m)">
              <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                <path d="M21 11.5a8.4 8.4 0 0 1-8.5 8.3 8.6 8.6 0 0 1-3.9-.9L3 20l1.2-5.4A8.2 8.2 0 0 1 3 11.5 8.4 8.4 0 0 1 11.5 3.2a8.4 8.4 0 0 1 9.5 8.3z" />
              </svg>
              <span>评论</span>
            </button>
          </div>

          <!-- 评论输入 -->
          <div v-if="m.commenting" class="comment-box">
            <input
              v-model="m.commentDraft"
              class="comment-input"
              placeholder="评论…"
              @keyup.enter="submitComment(m)"
            />
            <button class="comment-send" @click="submitComment(m)">发送</button>
          </div>
        </div>
      </article>

      <div v-if="loadingMore" class="feed-end">加载中…</div>
      <div v-else-if="!hasMore && moments.length" class="feed-end">— 已经到底啦 —</div>
    </main>

    <!-- 发布弹窗 -->
    <transition name="sheet">
      <div v-if="composerOpen" class="composer-mask" @click.self="closeComposer">
        <div class="composer-sheet">
          <div class="sheet-handle"></div>
          <div class="sheet-head">
            <button class="sheet-cancel" @click="closeComposer">取消</button>
            <span class="sheet-title">发表动态</span>
            <button class="sheet-publish" :disabled="!draft.trim() && draftImages.length === 0" @click="publish">发表</button>
          </div>
          <textarea
            v-model="draft"
            class="sheet-textarea"
            placeholder="这一刻的想法…"
            maxlength="500"
          ></textarea>
          <div class="sheet-tools">
            <div class="tool-btn" title="添加图片" @click="triggerFilePicker">
              <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                <rect x="3" y="3" width="18" height="18" rx="2" ry="2" />
                <circle cx="8.5" cy="8.5" r="1.5" />
                <path d="M21 15l-5-5L5 21" />
              </svg>
            </div>
            <input
              ref="fileInput"
              type="file"
              accept="image/*"
              multiple
              class="hidden-input"
              @change="onFilesChosen"
            />
            <span class="sheet-count">{{ draft.length }}/500</span>
          </div>

          <!-- 所在位置 -->
          <div class="sheet-loc">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <path d="M21 10c0 6-4.5 11-9 11s-9-5-9-11a9 9 0 0 1 18 0z" />
              <circle cx="12" cy="10" r="3" />
            </svg>
            <input
              v-model="draftLocation"
              class="loc-input"
              placeholder="所在位置（可选）"
              maxlength="40"
              @keyup.enter="$refs.locInput && $refs.locInput.blur()"
            />
            <button v-if="draftLocation" class="loc-clear" title="清除位置" @click="draftLocation = ''">✕</button>
          </div>

          <!-- 待发布图片预览 -->
          <div v-if="draftImages.length" class="draft-images">
            <div v-for="(img, i) in draftImages" :key="i" class="draft-img">
              <img :src="img.preview" alt="" />
              <button class="draft-remove" @click="removeDraftImage(i)">✕</button>
              <span v-if="img.uploading" class="draft-uploading">上传中…</span>
            </div>
          </div>
        </div>
      </div>
    </transition>

    <!-- 点赞人列表 -->
    <transition name="fade">
      <div v-if="likesPanel.show" class="likes-mask" @click.self="closeLikes">
        <div class="likes-sheet">
          <div class="likes-handle"></div>
          <div class="likes-head">点赞 · {{ likesPanel.names.length }} 人</div>
          <div class="likes-list">
            <div v-for="(n, i) in likesPanel.names" :key="i" class="likes-item">
              <span class="likes-heart">❤</span>
              <span>{{ n }}</span>
            </div>
          </div>
          <button class="likes-close" @click="closeLikes">关闭</button>
        </div>
      </div>
    </transition>

  </div>

    <!-- 图片预览（在滚动容器之外，避免随内容滚动） -->
    <transition name="fade">
      <div v-if="preview.show" class="preview-mask" @click.self="closePreview">
        <div class="preview-counter">{{ preview.index + 1 }} / {{ preview.list.length }}</div>
        <button v-if="preview.list.length > 1" class="preview-nav preview-prev" @click.stop="prevImage">‹</button>
        <img
          :src="preview.list[preview.index]"
          class="preview-img"
          alt=""
          @touchstart="onPreviewTouchStart"
          @touchend="onPreviewTouchEnd"
        />
        <button v-if="preview.list.length > 1" class="preview-nav preview-next" @click.stop="nextImage">›</button>
      </div>
    </transition>
  </div>
</template>

<script>
import { uploadFile } from '@/api/file'
import { showToast } from '@/plugins/message'
import { momentList, momentPublish, momentLike, momentComment, momentDelete, momentCommentDelete } from '@/api/moment'
import coverImg from '@/static/images/moments-cover.jpg'

// 本地内联图：替代 picsum 外链，断网 / 外网慢时也能立刻出图
const _svg = (s) => 'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(s)

const av = (c1, c2, ch) =>
  _svg(`<svg xmlns="http://www.w3.org/2000/svg" width="120" height="120" viewBox="0 0 120 120">
<defs><linearGradient id="a" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="${c1}"/><stop offset="1" stop-color="${c2}"/></linearGradient></defs>
<rect width="120" height="120" rx="60" fill="url(#a)"/>
<text x="60" y="77" text-anchor="middle" font-family="sans-serif" font-size="42" fill="#fff">${ch}</text>
</svg>`)

export default {
  name: 'Moments',
  data() {
    return {
      defaultAvatar: av('#8ec5ff', '#3a7bfd', '西'),
      userInfo: {},
      composerOpen: false,
      draft: '',
      draftLocation: '',
      draftImages: [],
      preview: { show: false, list: [], index: 0 },
      previewTouchX: null,
      coverImg,
      moments: [],
      loading: false,
      loadError: false,
      publishing: false,
      pageNum: 1,
      pageSize: 20,
      hasMore: true,
      loadingMore: false,
      refreshing: false,
      newCount: 0,
      menuId: null,
      likesPanel: { show: false, names: [] },
      pullStart: null,
      pullDist: 0
    }
  },
  created() {
    this.initUser()
    this.loadMoments()
  },
  methods: {
    initUser() {
      try {
        const raw = localStorage.getItem('userInfo')
        if (raw) this.userInfo = JSON.parse(raw)
      } catch (e) { /* ignore */ }
    },
    async loadMoments(reset = true) {
      if (!this.userInfo.id) return
      if (reset) {
        this.loading = true
        this.loadError = false
        this.pageNum = 1
        this.hasMore = true
      }
      try {
        const res = await momentList({ userId: this.userInfo.id, pageNum: this.pageNum, pageSize: this.pageSize })
        const rows = ((res && res.data) || []).map(v => this.mapMoment(v))
        this.moments = reset ? rows : this.moments.concat(rows)
        this.hasMore = rows.length === this.pageSize
        if (reset) {
          // 与上次已读时间比较，标出新动态并写回已读时间
          const lastRead = Number(localStorage.getItem('momentsLastRead') || 0)
          rows.forEach(r => { r.isNew = lastRead > 0 && r.createTimeRaw > lastRead })
          this.newCount = rows.filter(r => r.isNew).length
          localStorage.setItem('momentsLastRead', String(Date.now()))
          // 互动已读基线：进入朋友圈即把此刻记为已读，之后的点赞/评论才计入红点
          const now = new Date()
          const pad = n => ('0' + n).slice(-2)
          localStorage.setItem('momentsInteractSeen',
            `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())} ` +
            `${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(now.getSeconds())}`)
        }
      } catch (e) {
        if (reset) {
          this.moments = []
          this.loadError = true
        } else {
          this.pageNum = Math.max(1, this.pageNum - 1)
        }
      } finally {
        this.loading = false
        this.loadingMore = false
        this.refreshing = false
      }
    },
    async loadMore() {
      if (this.loading || this.loadingMore || !this.hasMore) return
      this.loadingMore = true
      this.pageNum += 1
      await this.loadMoments(false)
    },
    onScroll(e) {
      const el = e.target
      if (!el || this.loading || this.loadingMore || !this.hasMore) return
      if (el.scrollTop + el.clientHeight >= el.scrollHeight - 160) this.loadMore()
    },
    async refresh() {
      if (this.refreshing || this.loading) return
      this.refreshing = true
      await this.loadMoments(true)
    },
    onTouchStart(e) {
      const el = this.$refs.page
      if (!el || el.scrollTop > 0) { this.pullStart = null; return }
      this.pullStart = e.touches[0].clientY
    },
    onTouchMove(e) {
      if (this.pullStart == null) return
      const d = e.touches[0].clientY - this.pullStart
      this.pullDist = d > 0 ? Math.min(d * 0.45, 90) : 0
    },
    onTouchEnd() {
      if (this.pullDist >= 60) this.refresh()
      this.pullStart = null
      this.pullDist = 0
    },
    mapComments(list) {
      return (list || []).map(c => ({
        id: c.id,
        userId: c.userId,
        name: c.nickname,
        color: '#576b95',
        text: c.text,
        mine: !!this.userInfo.id && c.userId === this.userInfo.id
      }))
    },
    openMenu(m) {
      if (!m.canDelete) return
      this.menuId = this.menuId === m.id ? null : m.id
    },
    closeMenu() { this.menuId = null },
    async removeMoment(m) {
      if (!this.userInfo.id || !m.canDelete) return
      this.menuId = null
      if (!window.confirm('删除这条动态？删除后不可恢复')) return
      try {
        await momentDelete({ userId: this.userInfo.id, momentId: m.id })
        this.moments = this.moments.filter(x => x.id !== m.id)
      } catch (e) { /* 拦截器已提示 */ }
    },
    openLikes(m) {
      if (!m.likes.length) return
      this.likesPanel = { show: true, names: m.likes }
    },
    closeLikes() { this.likesPanel.show = false },
    async removeComment(m, c) {
      if (!this.userInfo.id || !c.mine) return
      try {
        const res = await momentCommentDelete({ userId: this.userInfo.id, commentId: c.id })
        const v = res && res.data
        if (v) m.comments = this.mapComments(v.comments)
      } catch (e) { /* 拦截器已提示 */ }
    },
    mapMoment(v) {
      return {
        id: v.id,
        nickname: v.nickname || '用户',
        avatar: v.avatar || this.defaultAvatar,
        nameColor: '#576b95',
        time: this.relTime(v.createTime),
        location: v.location || '',
        text: v.content || '',
        images: v.images || [],
        likes: v.likes || [],
        likeCount: v.likeCount || (v.likes || []).length,
        comments: this.mapComments(v.comments),
        liked: !!v.liked,
        canDelete: !!this.userInfo.id && v.userId === this.userInfo.id,
        commenting: false,
        commentDraft: '',
        createTimeRaw: this.tsOf(v.createTime),
        isNew: false
      }
    },
    tsOf(text) {
      if (!text) return 0
      const t = new Date(String(text).replace(/-/g, '/')).getTime()
      return isNaN(t) ? 0 : t
    },
    relTime(text) {
      if (!text) return ''
      const t = new Date(String(text).replace(/-/g, '/')).getTime()
      if (isNaN(t)) return text
      const diff = Date.now() - t
      if (diff < 60 * 1000) return '刚刚'
      if (diff < 3600 * 1000) return Math.floor(diff / 60000) + ' 分钟前'
      if (diff < 24 * 3600 * 1000) return Math.floor(diff / 3600000) + ' 小时前'
      if (diff < 7 * 24 * 3600 * 1000) return Math.floor(diff / 86400000) + ' 天前'
      return String(text).slice(0, 10)
    },
    openComposer() { this.composerOpen = true },
    closeComposer() {
      this.composerOpen = false
      this.draft = ''
      this.draftLocation = ''
      this.draftImages = []
    },
    triggerFilePicker() {
      if (this.$refs.fileInput) this.$refs.fileInput.click()
    },
    onFilesChosen(e) {
      const files = Array.from(e.target.files || [])
      const left = 9 - this.draftImages.length
      files.slice(0, left).forEach(file => {
        if (!file.type.startsWith('image/')) return
        this.draftImages.push({
          file,
          preview: URL.createObjectURL(file),
          url: '',
          uploading: false
        })
      })
      // 允许重复选择同一文件
      e.target.value = ''
    },
    removeDraftImage(i) {
      const img = this.draftImages[i]
      if (img && img.preview && img.preview.startsWith('blob:')) URL.revokeObjectURL(img.preview)
      this.draftImages.splice(i, 1)
    },
    uploadImage(file) {
      return uploadFile(file).then(res => {
        // 兼容 R 结构：{ code, data: url } 或 { code, url }
        return (res && res.data && typeof res.data === 'string') ? res.data : (res.data && res.data.url) || ''
      })
    },
    async publish() {
      const text = this.draft.trim()
      if (!text && this.draftImages.length === 0) return
      if (this.publishing || !this.userInfo.id) return
      this.publishing = true
      try {
        const draft = this.draftImages
        const results = new Array(draft.length).fill('')
        let failed = 0
        // 并发上传：按原顺序回填，单张失败计数而不是静默吞掉
        await Promise.all(draft.map((img, i) => {
          img.uploading = true
          return this.uploadImage(img.file)
            .then(url => { if (url) results[i] = url; else failed += 1 })
            .catch(() => { failed += 1 })
            .finally(() => { img.uploading = false })
        }))
        const images = results.filter(Boolean)
        if (failed > 0) {
          if (!text && images.length === 0) {
            showToast('图片上传失败，请重试', 'error')
            return
          }
          showToast(`${failed} 张图片上传失败，已跳过`, 'warning')
        }
        const res = await momentPublish({
          userId: this.userInfo.id,
          content: text,
          images,
          location: this.draftLocation.trim()
        })
        const v = res && res.data
        if (!v) throw new Error('发布失败')
        const mapped = this.mapMoment(v)
        mapped.isNew = true
        this.moments.unshift(mapped)
        this.draft = ''
        this.draftLocation = ''
        this.draftImages = []
        this.composerOpen = false
        setTimeout(() => { if (this.moments[0]) this.moments[0].isNew = false }, 1500)
      } catch (e) {
        // 错误提示由 request 拦截器统一给出
      } finally {
        this.publishing = false
      }
    },
    async toggleLike(m) {
      if (!this.userInfo.id) return
      try {
        const res = await momentLike({ userId: this.userInfo.id, momentId: m.id })
        const v = res && res.data
        if (v) {
          m.liked = !!v.liked
          m.likes = v.likes || []
        }
      } catch (e) { /* 拦截器已提示 */ }
    },
    toggleComment(m) { m.commenting = !m.commenting; if (m.commenting) m.commentDraft = '' },
    async submitComment(m) {
      const text = (m.commentDraft || '').trim()
      if (!text || !this.userInfo.id) return
      try {
        const res = await momentComment({ userId: this.userInfo.id, momentId: m.id, content: text })
        const v = res && res.data
        if (v) {
          m.comments = this.mapComments(v.comments)
        }
        m.commentDraft = ''
        m.commenting = false
      } catch (e) { /* 拦截器已提示 */ }
    },
    previewImages(list, index) {
      this.preview = { show: true, list, index }
    },
    closePreview() { this.preview.show = false },
    prevImage() {
      const n = this.preview.list.length
      if (!n) return
      this.preview.index = (this.preview.index - 1 + n) % n
    },
    nextImage() {
      const n = this.preview.list.length
      if (!n) return
      this.preview.index = (this.preview.index + 1) % n
    },
    onPreviewTouchStart(e) {
      this.previewTouchX = e.changedTouches[0].clientX
    },
    onPreviewTouchEnd(e) {
      if (this.previewTouchX == null) return
      const dx = e.changedTouches[0].clientX - this.previewTouchX
      if (Math.abs(dx) > 40) {
        if (dx < 0) this.nextImage()
        else this.prevImage()
      }
      this.previewTouchX = null
    },
    goDetail(m) {
      if (!m || !m.id) return
      this.$router.push('/moments/' + m.id)
    },
    onCoverTap() { /* 预留：点击封面查看个人主页 */ }
  }
}
</script>

<style scoped>
.moments-wrap {
  position: relative;
  height: 100%;
  overflow: hidden;
}
.moments-page {
  height: 100%;
  overflow-y: auto;
  background: var(--bg);
  color: var(--label);
  -webkit-font-smoothing: antialiased;
  scroll-behavior: smooth;
}

/* —— iOS 大标题导航 —— */
.nav-bar {
  position: sticky;
  top: 0;
  z-index: 20;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 40px 20px 12px;
  background: var(--bg-blur-strong);
  backdrop-filter: saturate(180%) blur(20px);
  -webkit-backdrop-filter: saturate(180%) blur(20px);
  border-bottom: 1px solid var(--separator);
}
.nav-title {
  font-size: 32px;
  font-weight: 700;
  letter-spacing: -0.5px;
}
.nav-camera {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border: none;
  border-radius: 50%;
  background: var(--fill);
  color: var(--label);
  cursor: pointer;
  transition: transform 0.18s ease, background 0.18s ease;
}
.nav-camera:hover {
  background: var(--fill-strong);
  transform: scale(1.08);
}
.nav-camera:active { transform: scale(0.92); }
.nav-new {
  display: inline-block;
  margin-left: 8px;
  padding: 2px 8px;
  border-radius: 999px;
  background: var(--accent-red, #ff3b30);
  color: #fff;
  font-size: 11px;
  font-weight: 500;
  vertical-align: middle;
}
.new-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--accent-red, #ff3b30);
  flex-shrink: 0;
  align-self: center;
}
.nav-actions { display: flex; align-items: center; gap: 8px; }
.nav-camera.spinning svg { animation: spin 0.8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

/* 下拉刷新 */
.pull-hint {
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  font-size: 12px;
  color: var(--label-tertiary);
  transition: height 0.16s ease, opacity 0.16s ease;
}

/* —— 封面 —— */
.cover {
  position: relative;
  height: 240px;
  overflow: hidden;
  border-bottom: 1px solid var(--separator);
}
.cover-photo {
  position: absolute;
  inset: 0;
  background-size: cover;
  background-position: center;
  filter: saturate(1.05);
}
.cover-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, transparent 55%, rgba(0, 0, 0, 0.28));
}
.cover-user {
  position: absolute;
  right: 16px;
  bottom: 14px;
  display: flex;
  align-items: flex-end;
  gap: 12px;
  cursor: pointer;
}
.cover-name {
  color: #fff;
  font-size: 17px;
  font-weight: 600;
  text-shadow: 0 1px 4px rgba(0, 0, 0, 0.35);
  margin-bottom: 8px;
}
.cover-avatar {
  width: 72px;
  height: 72px;
  border-radius: 14px;
  overflow: hidden;
  border: 2px solid rgba(255, 255, 255, 0.92);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.25);
  background: #fff;
  transition: transform 0.2s ease;
}
.cover-avatar:hover { transform: scale(1.04); }
.cover-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

/* —— 动态流 —— */
.feed {
  padding: 10px 0 30px;
  max-width: 720px;
  margin: 0 auto;
  width: 100%;
  box-sizing: border-box;
}
.moment-card {
  display: flex;
  gap: 12px;
  padding: 16px 18px;
  background: var(--card);
  border-bottom: 1px solid var(--separator);
  transition: background 0.2s ease;
}
.moment-card:hover { background: var(--card-highlight); }
.moment-new { animation: moment-in 0.45s cubic-bezier(0.2, 0.9, 0.3, 1.2); }
@keyframes moment-in {
  from { transform: translateY(14px) scale(0.98); }
  to { transform: none; }
}
.moment-avatar {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  object-fit: cover;
  flex-shrink: 0;
  box-shadow: var(--shadow-sm);
}
.moment-body { flex: 1; min-width: 0; }
.moment-head {
  display: flex;
  align-items: baseline;
  gap: 8px;
  flex-wrap: wrap;
  position: relative;
}
.moment-more {
  margin-left: auto;
  border: none;
  background: none;
  color: var(--label-tertiary);
  font-size: 15px;
  letter-spacing: 1px;
  line-height: 1;
  padding: 4px 6px;
  border-radius: 6px;
  cursor: pointer;
  opacity: 0.7;
}
.moment-more:hover { background: var(--fill); opacity: 1; }
.more-menu {
  position: absolute;
  right: 0;
  top: 26px;
  z-index: 10;
  min-width: 110px;
  padding: 4px;
  border-radius: 10px;
  background: var(--card-solid, #fff);
  box-shadow: var(--shadow-lg);
  border: 1px solid var(--separator);
}
.more-danger {
  width: 100%;
  border: none;
  background: none;
  color: var(--accent-red, #ff3b30);
  font-size: 14px;
  text-align: center;
  padding: 9px 12px;
  border-radius: 8px;
  cursor: pointer;
}
.more-danger:hover { background: var(--fill); }
.moment-name { font-size: 15px; font-weight: 600; cursor: pointer; }
.moment-time { font-size: 12px; color: var(--label-tertiary); }
.moment-loc { font-size: 12px; color: var(--label-tertiary); }
.moment-text {
  margin: 8px 0;
  font-size: 15px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
  color: var(--label);
}
.moment-link { cursor: pointer; }
.moment-link:hover { opacity: 0.82; }

/* 九宫格 */
.moment-grid {
  display: grid;
  gap: 4px;
  margin: 4px 0 8px;
  grid-template-columns: repeat(3, 1fr);
  max-width: 320px;
}
.moment-grid.grid-1 { grid-template-columns: 1fr; max-width: 240px; }
.moment-grid.grid-1 .grid-cell { aspect-ratio: 1.6; }
.grid-cell {
  aspect-ratio: 1;
  border-radius: 8px;
  overflow: hidden;
  cursor: zoom-in;
  background: var(--fill);
}
.grid-cell img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  transition: transform 0.25s ease;
}
.grid-cell:hover img { transform: scale(1.06); }

/* 点赞评论面板 */
.reaction-panel {
  margin-top: 6px;
  border-radius: 10px;
  background: var(--fill);
  padding: 8px 12px;
  max-width: 320px;
}
.like-row {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  font-size: 13px;
  padding-bottom: 6px;
  border-bottom: 1px solid var(--separator);
}
.like-row:only-child { border-bottom: none; padding-bottom: 0; }
.like-icon { flex-shrink: 0; font-size: 12px; line-height: 1.6; }
.like-list { color: var(--label-secondary); line-height: 1.6; }
.like-more { margin-left: auto; flex-shrink: 0; font-size: 12px; color: var(--label-tertiary); padding-left: 8px; }
.like-row { cursor: pointer; border-radius: 6px; }
.like-row:hover .like-more { color: var(--accent-blue, #007aff); }
.comment-list { padding-top: 6px; }
.comment-list:first-child { padding-top: 0; }
.comment-row {
  font-size: 14px;
  line-height: 1.6;
  word-break: break-word;
}
.comment-name { font-weight: 600; cursor: pointer; margin-right: 6px; }
.comment-text { color: var(--label); }
.comment-del {
  margin-left: 8px;
  border: none;
  background: none;
  color: var(--label-tertiary);
  font-size: 12px;
  padding: 2px 4px;
  border-radius: 4px;
  cursor: pointer;
}
.comment-del:hover { color: var(--accent-red, #ff3b30); background: var(--fill); }

/* 操作栏 */
.moment-actions {
  display: flex;
  gap: 4px;
  margin-top: 6px;
  justify-content: flex-end;
}
.action-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  border: none;
  background: none;
  color: var(--label-secondary);
  font-size: 13px;
  padding: 6px 10px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.18s ease, color 0.18s ease, transform 0.12s ease;
}
.action-btn:hover { background: var(--fill); }
.action-btn:active { transform: scale(0.94); }
.action-btn.liked { color: var(--accent-red); }

/* 评论输入 */
.comment-box {
  display: flex;
  gap: 6px;
  margin-top: 8px;
}
.comment-input {
  flex: 1;
  min-width: 0;
  border: none;
  outline: none;
  background: var(--card);
  border: 1px solid var(--separator);
  border-radius: 8px;
  padding: 6px 10px;
  font-size: 13px;
  color: var(--label);
  font-family: inherit;
}
.comment-input:focus { border-color: var(--accent); }
.comment-send {
  border: none;
  background: var(--accent);
  color: #fff;
  border-radius: 8px;
  padding: 0 14px;
  font-size: 13px;
  cursor: pointer;
}

/* 空态 */
.feed-empty { display: flex; flex-direction: column; align-items: center; padding-top: 80px; }
.empty-icon { color: var(--label-tertiary); margin-bottom: 16px; }
.empty-title { font-size: 17px; font-weight: 600; color: var(--label-secondary); margin: 0 0 6px; }
.empty-sub { font-size: 14px; color: var(--label-tertiary); margin: 0; }
.empty-retry {
  margin-top: 16px;
  padding: 9px 22px;
  border: none;
  border-radius: 999px;
  background: var(--accent, #007aff);
  color: #fff;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: transform 0.18s cubic-bezier(0.34, 1.56, 0.64, 1), filter 0.18s ease;
}
.empty-retry:hover { filter: brightness(1.08); }
.empty-retry:active { transform: scale(0.94); }
.feed-end {
  text-align: center;
  font-size: 12px;
  color: var(--label-tertiary);
  padding: 18px 0 8px;
}

/* —— 发布弹窗（iOS Sheet） —— */
.composer-mask {
  position: fixed;
  inset: 0;
  z-index: 50;
  background: var(--overlay);
  display: flex;
  align-items: flex-end;
  justify-content: center;
}
.composer-sheet {
  width: 100%;
  max-width: 520px;
  background: rgba(255, 255, 255, 0.96);
  -webkit-backdrop-filter: blur(28px) saturate(1.4);
  backdrop-filter: blur(28px) saturate(1.4);
  border-radius: 18px 18px 0 0;
  padding: 12px 16px 20px;
  box-shadow: var(--shadow-lg);
  max-height: 86vh;
  overflow-y: auto;
}
:root[data-theme="dark"] .composer-sheet { background: rgba(28, 28, 30, 0.96); }
.sheet-handle {
  width: 40px;
  height: 5px;
  border-radius: 3px;
  background: var(--separator-strong);
  margin: 0 auto 10px;
}
.sheet-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}
.sheet-cancel {
  border: none;
  background: none;
  color: var(--label-secondary);
  font-size: 15px;
  cursor: pointer;
  padding: 6px 0;
}
.sheet-title { font-size: 17px; font-weight: 600; }
.sheet-publish {
  border: none;
  background: var(--accent);
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  border-radius: 18px;
  padding: 6px 18px;
  cursor: pointer;
  transition: opacity 0.18s ease;
}
.sheet-publish:disabled { opacity: 0.4; cursor: not-allowed; }
.sheet-textarea {
  width: 100%;
  box-sizing: border-box;
  height: 120px;
  border: none;
  outline: none;
  background: var(--fill);
  border-radius: 12px;
  padding: 12px 14px;
  font-size: 16px;
  line-height: 1.6;
  color: var(--label);
  resize: none;
  font-family: inherit;
}
.sheet-tools {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 10px;
}
.tool-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  border-radius: 50%;
  background: var(--fill);
  color: var(--label);
  cursor: pointer;
  transition: background 0.18s ease, transform 0.12s ease;
}
.tool-btn:hover { background: var(--fill-strong); }
.tool-btn:active { transform: scale(0.9); }
.sheet-count { font-size: 12px; color: var(--label-tertiary); }

/* 所在位置 */
.sheet-loc {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 10px;
  padding: 9px 12px;
  border-radius: 10px;
  background: var(--fill);
  color: var(--label-secondary);
}
.loc-input {
  flex: 1;
  min-width: 0;
  border: none;
  background: transparent;
  outline: none;
  font-size: 14px;
  color: var(--label);
}
.loc-input::placeholder { color: var(--label-tertiary); }
.loc-clear {
  border: none;
  background: none;
  color: var(--label-tertiary);
  font-size: 13px;
  padding: 2px 4px;
  cursor: pointer;
}
.loc-clear:hover { color: var(--label-secondary); }
.hidden-input { display: none; }

/* 待发布图片 */
.draft-images {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 10px;
}
.draft-img {
  position: relative;
  width: 72px;
  height: 72px;
  border-radius: 10px;
  overflow: hidden;
  border: 1px solid var(--separator);
  background: var(--fill);
}
.draft-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.draft-remove {
  position: absolute;
  top: 4px;
  right: 4px;
  width: 20px;
  height: 20px;
  border: none;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.55);
  color: #fff;
  font-size: 11px;
  line-height: 1;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}
.draft-uploading {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.75);
  color: var(--label-secondary);
  font-size: 11px;
}

.sheet-enter-active, .sheet-leave-active { transition: transform 0.32s cubic-bezier(0.22, 0.9, 0.32, 1), opacity 0.25s ease; }
.sheet-enter-from, .sheet-leave-to { transform: translateY(100%); opacity: 0; }

/* —— 图片预览 —— */
.preview-mask {
  position: absolute;
  inset: 0;
  z-index: 1000000;
  background: rgba(0, 0, 0, 0.94);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: zoom-out;
}
.preview-img {
  max-width: 92vw;
  max-height: 88vh;
  border-radius: 10px;
  box-shadow: 0 20px 80px rgba(0, 0, 0, 0.6);
  user-select: none;
}
.preview-counter {
  position: absolute;
  top: 44px;
  left: 50%;
  transform: translateX(-50%);
  padding: 5px 12px;
  border-radius: 999px;
  background: rgba(0, 0, 0, 0.5);
  color: #fff;
  font-size: 13px;
  letter-spacing: 0.5px;
  z-index: 2;
}
.preview-nav {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 40px;
  height: 40px;
  border: none;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.45);
  color: #fff;
  font-size: 24px;
  line-height: 1;
  cursor: pointer;
  z-index: 2;
  display: flex;
  align-items: center;
  justify-content: center;
}
.preview-nav:hover { background: rgba(0, 0, 0, 0.65); }
.preview-prev { left: 14px; }
.preview-next { right: 14px; }
.fade-enter-active, .fade-leave-active { transition: opacity 0.22s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }

/* —— 点赞人弹层 —— */
.likes-mask {
  position: fixed;
  inset: 0;
  z-index: 55;
  background: var(--overlay);
  display: flex;
  align-items: flex-end;
  justify-content: center;
}
.likes-sheet {
  width: 100%;
  max-width: 460px;
  background: rgba(255, 255, 255, 0.96);
  -webkit-backdrop-filter: blur(28px) saturate(1.4);
  backdrop-filter: blur(28px) saturate(1.4);
  border-radius: 18px 18px 0 0;
  padding: 12px 16px 18px;
  box-shadow: var(--shadow-lg);
  max-height: 70vh;
  overflow-y: auto;
}
:root[data-theme="dark"] .likes-sheet { background: rgba(28, 28, 30, 0.96); }
.likes-handle {
  width: 40px;
  height: 5px;
  border-radius: 3px;
  background: var(--separator-strong);
  margin: 0 auto 12px;
}
.likes-head { font-size: 15px; font-weight: 600; margin-bottom: 10px; color: var(--label); }
.likes-list { display: flex; flex-direction: column; gap: 2px; }
.likes-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: var(--label-secondary);
  padding: 9px 4px;
  border-bottom: 1px solid var(--separator);
}
.likes-item:last-child { border-bottom: none; }
.likes-heart { color: var(--accent-red); font-size: 13px; }
.likes-close {
  width: 100%;
  margin-top: 12px;
  border: none;
  background: var(--fill);
  color: var(--label);
  font-size: 15px;
  padding: 11px 0;
  border-radius: 10px;
  cursor: pointer;
}
.likes-close:hover { background: var(--fill-strong); }

/* 滚动条 */
.moments-page::-webkit-scrollbar { width: 8px; }
.moments-page::-webkit-scrollbar-thumb {
  background: var(--scrollbar-thumb);
  border-radius: 4px;
}
</style>
