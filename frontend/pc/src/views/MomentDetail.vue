<template>
  <div class="detail-wrap">
  <div class="detail-page">
    <header class="detail-nav">
      <button class="back-btn" title="返回" @click="back">‹</button>
      <span class="detail-title">动态详情</span>
      <span class="nav-gap"></span>
    </header>

    <div v-if="loading" class="detail-state">加载中…</div>
    <div v-else-if="!moment" class="detail-state">动态不存在或已删除</div>

    <main v-else class="detail-body">
      <article class="detail-card">
        <div class="author">
          <img class="avatar" :src="moment.avatar" alt="" />
          <div class="author-meta">
            <span class="name">{{ moment.nickname }}</span>
            <span class="time">{{ moment.time }}</span>
          </div>
          <button v-if="moment.canDelete" class="more-btn" title="更多" @click="menuOpen = !menuOpen">•••</button>
          <div v-if="menuOpen" class="more-menu">
            <button class="more-danger" @click="removeMoment">删除动态</button>
          </div>
        </div>

        <p v-if="moment.location" class="loc">📍 {{ moment.location }}</p>
        <p class="text">{{ moment.text }}</p>

        <div v-if="moment.images.length" class="grid" :class="'grid-' + Math.min(moment.images.length, 3)">
          <div v-for="(img, i) in moment.images.slice(0, 9)" :key="i" class="cell" @click="openPreview(i)">
            <img :src="img" alt="" loading="lazy" />
          </div>
        </div>

        <div class="meta-line">
          <span v-if="moment.likes.length" class="meta-like" @click="likesOpen = true">❤ {{ moment.likes.length }} 人赞过</span>
          <span class="meta-comment">评论 {{ moment.comments.length }}</span>
        </div>

        <div v-if="moment.comments.length" class="comment-list">
          <div v-for="(c, ci) in moment.comments" :key="c.id || ci" class="comment-row">
            <span class="comment-name">{{ c.name }}</span>
            <span class="comment-text">{{ c.text }}</span>
            <button v-if="c.mine" class="comment-del" @click="removeComment(c)">删除</button>
          </div>
        </div>

        <div class="actions">
          <button class="action-btn" :class="{ liked: moment.liked }" @click="toggleLike">
            <span>{{ moment.liked ? '❤ 已赞' : '♡ 赞' }}</span>
          </button>
          <button class="action-btn" @click="commenting = !commenting">评论</button>
        </div>

        <div v-if="commenting" class="comment-box">
          <input v-model="draft" class="comment-input" placeholder="说点什么…" @keyup.enter="submitComment" />
          <button class="comment-send" @click="submitComment">发送</button>
        </div>
      </article>
    </main>

  </div>

    <!-- 点赞人 -->
    <transition name="fade">
      <div v-if="likesOpen" class="mask" @click.self="likesOpen = false">
        <div class="sheet">
          <div class="handle"></div>
          <div class="sheet-head">点赞 · {{ moment.likes.length }} 人</div>
          <div class="like-list">
            <div v-for="(n, i) in moment.likes" :key="i" class="like-item">❤ {{ n }}</div>
          </div>
          <button class="sheet-close" @click="likesOpen = false">关闭</button>
        </div>
      </div>
    </transition>

    <!-- 图片预览 -->
    <transition name="fade">
      <div v-if="preview.open" class="preview-mask" @click.self="preview.open = false">
        <div class="preview-counter">{{ preview.index + 1 }} / {{ moment.images.length }}</div>
        <button v-if="moment.images.length > 1" class="preview-nav left" @click.stop="step(-1)">‹</button>
        <img :src="moment.images[preview.index]" class="preview-img" alt="" @touchstart="onTouchStart" @touchend="onTouchEnd" />
        <button v-if="moment.images.length > 1" class="preview-nav right" @click.stop="step(1)">›</button>
      </div>
    </transition>
  </div>
</template>

<script>
import { momentDetail, momentLike, momentComment, momentDelete, momentCommentDelete } from '@/api/moment'

export default {
  name: 'MomentDetail',
  data() {
    return {
      userInfo: {},
      moment: null,
      loading: true,
      commenting: false,
      draft: '',
      menuOpen: false,
      likesOpen: false,
      preview: { open: false, index: 0 },
      touchX: null
    }
  },
  created() {
    try {
      const raw = localStorage.getItem('userInfo')
      if (raw) this.userInfo = JSON.parse(raw)
    } catch (e) { /* ignore */ }
    this.load()
  },
  methods: {
    async load() {
      if (!this.userInfo.id) { this.loading = false; return }
      this.loading = true
      try {
        const res = await momentDetail({ userId: this.userInfo.id, momentId: this.$route.params.id })
        const v = res && res.data
        this.moment = v ? this.mapMoment(v) : null
      } catch (e) {
        this.moment = null
      } finally {
        this.loading = false
      }
    },
    mapMoment(v) {
      return {
        id: v.id,
        nickname: v.nickname || '用户',
        avatar: v.avatar || this.defaultAvatar(),
        time: this.relTime(v.createTime),
        location: v.location || '',
        text: v.content || '',
        images: v.images || [],
        likes: v.likes || [],
        comments: this.mapComments(v.comments),
        liked: !!v.liked,
        canDelete: !!this.userInfo.id && v.userId === this.userInfo.id
      }
    },
    mapComments(list) {
      return (list || []).map(c => ({
        id: c.id,
        userId: c.userId,
        name: c.nickname,
        text: c.text,
        mine: !!this.userInfo.id && c.userId === this.userInfo.id
      }))
    },
    defaultAvatar() {
      const svg = '<svg xmlns="http://www.w3.org/2000/svg" width="120" height="120" viewBox="0 0 120 120">'
        + '<defs><linearGradient id="a" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#8ec5ff"/><stop offset="1" stop-color="#3a7bfd"/></linearGradient></defs>'
        + '<rect width="120" height="120" rx="60" fill="url(#a)"/>'
        + '<text x="60" y="77" text-anchor="middle" font-family="sans-serif" font-size="42" fill="#fff">西</text></svg>'
      return 'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(svg)
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
    back() {
      if (window.history.length > 1) this.$router.back()
      else this.$router.push('/moments')
    },
    openPreview(i) {
      this.preview = { open: true, index: i }
    },
    step(d) {
      const n = this.moment.images.length
      if (!n) return
      this.preview.index = (this.preview.index + d + n) % n
    },
    onTouchStart(e) { this.touchX = e.changedTouches[0].clientX },
    onTouchEnd(e) {
      if (this.touchX == null) return
      const dx = e.changedTouches[0].clientX - this.touchX
      if (Math.abs(dx) > 40) this.step(dx < 0 ? 1 : -1)
      this.touchX = null
    },
    async toggleLike() {
      if (!this.userInfo.id) return
      try {
        const res = await momentLike({ userId: this.userInfo.id, momentId: this.moment.id })
        const v = res && res.data
        if (v) {
          this.moment.liked = !!v.liked
          this.moment.likes = v.likes || []
        }
      } catch (e) { /* 拦截器已提示 */ }
    },
    async submitComment() {
      const text = (this.draft || '').trim()
      if (!text) return
      try {
        const res = await momentComment({ userId: this.userInfo.id, momentId: this.moment.id, content: text })
        const v = res && res.data
        if (v) this.moment.comments = this.mapComments(v.comments)
        this.draft = ''
        this.commenting = false
      } catch (e) { /* 拦截器已提示 */ }
    },
    async removeComment(c) {
      if (!c.mine) return
      try {
        const res = await momentCommentDelete({ userId: this.userInfo.id, commentId: c.id })
        const v = res && res.data
        if (v) this.moment.comments = this.mapComments(v.comments)
      } catch (e) { /* 拦截器已提示 */ }
    },
    async removeMoment() {
      this.menuOpen = false
      if (!window.confirm('删除这条动态？删除后不可恢复')) return
      try {
        await momentDelete({ userId: this.userInfo.id, momentId: this.moment.id })
        this.moment = null
        this.$router.push('/moments')
      } catch (e) { /* 拦截器已提示 */ }
    }
  }
}
</script>

<style scoped>
.detail-wrap {
  position: relative;
  height: 100%;
  overflow: hidden;
}
.detail-page {
  height: 100%;
  overflow-y: auto;
  background: var(--bg);
  color: var(--label);
}
.detail-nav {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 40px 14px 12px;
  background: var(--nav-bg, rgba(255, 255, 255, 0.85));
  -webkit-backdrop-filter: blur(20px) saturate(1.4);
  backdrop-filter: blur(20px) saturate(1.4);
  border-bottom: 1px solid var(--separator);
  position: sticky;
  top: 0;
  z-index: 8;
}
.back-btn {
  width: 34px;
  height: 34px;
  border: none;
  border-radius: 10px;
  background: var(--fill);
  color: var(--label);
  font-size: 19px;
  line-height: 1;
  cursor: pointer;
}
.detail-title { font-size: 16px; font-weight: 600; }
.nav-gap { width: 34px; }
.detail-state { padding: 40px 20px; text-align: center; color: var(--label-tertiary); font-size: 14px; }
.detail-body { padding: 14px; }
.detail-card {
  background: var(--card-solid, #fff);
  border-radius: 16px;
  padding: 16px 15px 12px;
  box-shadow: var(--shadow-sm);
}
.author { display: flex; align-items: center; gap: 10px; position: relative; }
.avatar { width: 40px; height: 40px; border-radius: 8px; object-fit: cover; background: var(--fill); }
.author-meta { display: flex; flex-direction: column; gap: 3px; }
.name { font-size: 14px; font-weight: 600; color: #576b95; }
.time { font-size: 11px; color: var(--label-tertiary); }
.more-btn {
  margin-left: auto;
  border: none;
  background: none;
  color: var(--label-tertiary);
  font-size: 15px;
  letter-spacing: 1px;
  padding: 4px 6px;
  border-radius: 6px;
  cursor: pointer;
}
.more-btn:hover { background: var(--fill); }
.more-menu {
  position: absolute;
  right: 0;
  top: 28px;
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
  padding: 9px 12px;
  border-radius: 8px;
  cursor: pointer;
}
.more-danger:hover { background: var(--fill); }
.loc { font-size: 12px; color: var(--label-tertiary); margin: 10px 0 0; }
.text { margin: 8px 0; font-size: 15px; line-height: 1.7; white-space: pre-wrap; word-break: break-word; }
.grid { display: grid; gap: 4px; margin-top: 6px; }
.grid-1 { grid-template-columns: 1fr; }
.grid-2 { grid-template-columns: repeat(2, 1fr); }
.grid-3 { grid-template-columns: repeat(3, 1fr); }
.cell { position: relative; aspect-ratio: 1; border-radius: 6px; overflow: hidden; background: var(--fill); cursor: pointer; }
.cell img { width: 100%; height: 100%; object-fit: cover; }
.meta-line { display: flex; gap: 16px; margin-top: 12px; font-size: 12px; color: var(--label-tertiary); }
.meta-like { cursor: pointer; }
.meta-like:hover { color: var(--accent-red, #ff3b30); }
.comment-list {
  margin-top: 10px;
  padding: 8px 10px;
  border-radius: 8px;
  background: var(--fill);
}
.comment-row { font-size: 13px; line-height: 1.7; display: flex; flex-wrap: wrap; align-items: baseline; }
.comment-name { color: #576b95; font-weight: 600; margin-right: 6px; }
.comment-text { color: var(--label); flex: 1; white-space: pre-wrap; word-break: break-word; }
.comment-del {
  border: none;
  background: none;
  color: var(--label-tertiary);
  font-size: 12px;
  padding: 2px 4px;
  cursor: pointer;
}
.comment-del:hover { color: var(--accent-red, #ff3b30); }
.actions { display: flex; gap: 8px; margin-top: 12px; padding-top: 10px; border-top: 1px solid var(--separator); }
.action-btn {
  flex: 1;
  border: none;
  background: var(--fill);
  color: var(--label-secondary);
  font-size: 13px;
  padding: 9px 0;
  border-radius: 9px;
  cursor: pointer;
}
.action-btn.liked { color: var(--accent-red, #ff3b30); background: rgba(255, 59, 48, 0.08); }
.comment-box { display: flex; gap: 8px; margin-top: 10px; }
.comment-input {
  flex: 1;
  min-width: 0;
  border: 1px solid var(--separator);
  border-radius: 9px;
  padding: 9px 12px;
  font-size: 14px;
  background: var(--input-bg, #fff);
  color: var(--label);
  outline: none;
}
.comment-input:focus { border-color: var(--accent-blue, #007aff); }
.comment-send {
  border: none;
  background: var(--accent-blue, #007aff);
  color: #fff;
  font-size: 14px;
  padding: 0 16px;
  border-radius: 9px;
  cursor: pointer;
}
.mask {
  position: absolute;
  inset: 0;
  z-index: 1000000;
  background: var(--overlay);
  display: flex;
  align-items: flex-end;
  justify-content: center;
}
.sheet {
  width: 100%;
  max-width: 460px;
  background: var(--card-solid, #fff);
  border-radius: 18px 18px 0 0;
  padding: 12px 16px 18px;
  box-shadow: var(--shadow-lg);
  max-height: 70vh;
  overflow-y: auto;
}
.handle { width: 40px; height: 5px; border-radius: 3px; background: var(--separator-strong); margin: 0 auto 12px; }
.sheet-head { font-size: 15px; font-weight: 600; margin-bottom: 10px; }
.like-list { display: flex; flex-direction: column; }
.like-item { padding: 9px 4px; font-size: 14px; color: var(--label-secondary); border-bottom: 1px solid var(--separator); }
.like-item:last-child { border-bottom: none; }
.sheet-close {
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
.preview-mask {
  position: absolute;
  inset: 0;
  z-index: 1000000;
  background: rgba(0, 0, 0, 0.94);
  display: flex;
  align-items: center;
  justify-content: center;
}
.preview-img { max-width: 92vw; max-height: 88vh; border-radius: 10px; user-select: none; }
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
}
.preview-nav.left { left: 14px; }
.preview-nav.right { right: 14px; }
.fade-enter-active, .fade-leave-active { transition: opacity 0.22s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
