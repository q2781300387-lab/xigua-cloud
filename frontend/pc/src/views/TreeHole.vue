<template>
  <div class="treehole-page">
    <!-- iOS 大标题导航 -->
    <header class="nav-bar">
      <div class="nav-title">匿名树洞</div>
      <span class="nav-sub">匿名倾诉 · 真实回声</span>
      <button class="nav-write" title="写树洞" @click="openComposer">
        <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round">
          <path d="M12 20h9" />
          <path d="M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4z" />
        </svg>
      </button>
    </header>

    <!-- 氛围插画区 -->
    <section class="hero">
      <div class="hero-glow"></div>
      <p class="hero-slogan">这里没有名字，只有心事</p>
      <p class="hero-sub">把你的烦恼悄悄扔进树洞，会收到陌生人的温暖回应</p>
    </section>

    <!-- 树洞列表 -->
    <main class="feed" :class="{ 'feed-empty': holes.length === 0 }">
      <div v-if="loading" class="loading-state">
        <div class="loading-ring"></div>
        <p>树洞加载中…</p>
      </div>

      <div v-else-if="loadFailed" class="empty-state">
        <p class="empty-title">加载失败</p>
        <p class="empty-sub">
          网络或服务异常，
          <span style="color:#007aff;font-weight:600;cursor:pointer" @click="loadHoles">点击重试</span>
        </p>
      </div>

      <div v-else-if="holes.length === 0" class="empty-state">
        <svg viewBox="0 0 24 24" width="64" height="64" fill="none" stroke="currentColor" stroke-width="1" stroke-linecap="round" stroke-linejoin="round" class="empty-icon">
          <path d="M8 10h8M8 14h4" />
          <path d="M12 3a9 9 0 1 0 0 18 9 9 0 0 0 0-18z" />
        </svg>
        <p class="empty-title">树洞空空如也</p>
        <p class="empty-sub">第一个把心事丢进来吧</p>
      </div>

      <article v-for="h in holes" :key="h.id" class="hole-card" :class="{ 'hole-new': h.isNew }">
        <div class="hole-head">
          <span class="hole-avatar">🙈</span>
          <div class="hole-meta">
            <span class="hole-name">匿名同学</span>
            <span class="hole-time">{{ formatTime(h.createTime) }}</span>
          </div>
        </div>
        <p class="hole-text">{{ h.content }}</p>
        <div class="hole-foot">
          <button class="like-btn" :class="{ liked: h.liked }" @click="toggleLike(h)">
            <svg v-if="h.liked" viewBox="0 0 24 24" width="15" height="15" fill="#ff9f0a">
              <path d="M20.8 4.6a5.5 5.5 0 0 0-7.8 0L12 5.6l-1-1a5.5 5.5 0 0 0-7.8 7.8l1 1L12 21l7.8-7.6 1-1a5.5 5.5 0 0 0 0-7.8z" />
            </svg>
            <svg v-else viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <path d="M20.8 4.6a5.5 5.5 0 0 0-7.8 0L12 5.6l-1-1a5.5 5.5 0 0 0-7.8 7.8l1 1L12 21l7.8-7.6 1-1a5.5 5.5 0 0 0 0-7.8z" />
            </svg>
            <span>{{ h.liked ? '暖心' : '暖心' }} {{ h.likeCount }}</span>
          </button>
          <span class="hole-safe">发布者信息已隐藏</span>
        </div>
      </article>

      <div v-if="holes.length > 0" class="feed-end">— 树洞到此，愿心事都被听见 —</div>
    </main>

    <!-- 发布弹窗 -->
    <transition name="sheet">
      <div v-if="composerOpen" class="composer-mask" @click.self="closeComposer">
        <div class="composer-sheet">
          <div class="sheet-handle"></div>
          <div class="sheet-head">
            <button class="sheet-cancel" @click="closeComposer">取消</button>
            <span class="sheet-title">扔进树洞</span>
            <button class="sheet-publish" :disabled="!draft.trim()" @click="publish">匿名发布</button>
          </div>
          <textarea
            v-model="draft"
            class="sheet-textarea"
            placeholder="把不敢说出口的话，悄悄写在这里…（不会显示你的身份）"
            maxlength="500"
          ></textarea>
          <div class="sheet-tip">
            <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <rect x="3" y="11" width="18" height="11" rx="2" />
              <path d="M7 11V7a5 5 0 0 1 10 0v4" />
            </svg>
            <span>树洞为全匿名发布，不展示任何账号信息</span>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script>
import { treeholeList, treeholeAdd, treeholeLike } from '@/api/treehole'

export default {
  name: 'TreeHole',
  data() {
    return {
      holes: [],
      loading: true,
      loadFailed: false,
      composerOpen: false,
      draft: '',
      likedIds: new Set()
    }
  },
  created() {
    this.loadHoles()
  },
  methods: {
    async loadHoles() {
      this.loading = true
      try {
        const res = await treeholeList({ page: 1, size: 30 })
        if (res.code === 200 && res.data) {
          this.holes = (res.data.records || []).map(h => ({ ...h, liked: this.likedIds.has(h.id) }))
        } else {
          this.holes = []
        }
      } catch (e) {
        // 请求失败不能退化成「树洞空空如也」
        this.loadFailed = true;
        console.error("树洞列表加载失败:", e);
      } finally {
        this.loading = false
      }
    },
    openComposer() {
      this.draft = ''
      this.composerOpen = true
    },
    closeComposer() {
      this.composerOpen = false
    },
    async publish() {
      const content = this.draft.trim()
      if (!content) return
      try {
        const res = await treeholeAdd({ content })
        if (res.code === 200) {
          this.composerOpen = false
          this.draft = ''
          // 顶部插入并刷新
          this.holes.unshift({ id: 'new-' + Date.now(), content, likeCount: 0, createTime: new Date().toISOString(), isNew: true })
          this.loadHoles()
        }
      } catch (e) {
        // 静默
      }
    },
    async toggleLike(h) {
      if (h.liked) return
      try {
        const res = await treeholeLike(h.id)
        if (res.code === 200) {
          h.likeCount = (h.likeCount || 0) + 1
          h.liked = true
          this.likedIds.add(h.id)
        }
      } catch (e) {
        // 静默
      }
    },
    formatTime(t) {
      if (!t) return ''
      const d = new Date(t)
      const now = new Date()
      const diff = Math.floor((now - d) / 1000)
      if (diff < 60) return '刚刚'
      if (diff < 3600) return Math.floor(diff / 60) + ' 分钟前'
      if (diff < 86400) return Math.floor(diff / 3600) + ' 小时前'
      const mm = String(d.getMonth() + 1).padStart(2, '0')
      const dd = String(d.getDate()).padStart(2, '0')
      return `${mm}-${dd} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
    }
  }
}
</script>

<style scoped>
.treehole-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #f5f6fa;
  overflow: hidden;
}
.nav-bar {
  flex: none;
  display: flex;
  align-items: center;
  height: 64px;
  padding: 0 20px;
  background: rgba(255,255,255,0.82);
  backdrop-filter: blur(18px);
  -webkit-backdrop-filter: blur(18px);
  border-bottom: 0.5px solid rgba(0,0,0,0.06);
  position: relative;
  z-index: 5;
}
.nav-title {
  font-size: 20px;
  font-weight: 700;
  color: #1d1d1f;
  letter-spacing: 1px;
}
.nav-sub {
  margin-left: 12px;
  font-size: 12px;
  color: #8e8e93;
  margin-top: 3px;
}
.nav-write {
  margin-left: auto;
  width: 34px;
  height: 34px;
  border: none;
  border-radius: 10px;
  background: linear-gradient(135deg, #ff9f0a, #ffbf4a);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(255,159,10,0.35);
}
.hero {
  flex: none;
  position: relative;
  padding: 22px 24px 20px;
  text-align: center;
  overflow: hidden;
}
.hero-glow {
  position: absolute;
  left: 50%;
  top: -20px;
  transform: translateX(-50%);
  width: 320px;
  height: 140px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(255,159,10,0.14), rgba(255,159,10,0));
}
.hero-slogan {
  position: relative;
  font-size: 17px;
  font-weight: 600;
  color: #1d1d1f;
  letter-spacing: 2px;
}
.hero-sub {
  position: relative;
  margin-top: 6px;
  font-size: 12px;
  color: #8e8e93;
  letter-spacing: 1px;
}
.feed {
  flex: 1;
  overflow-y: auto;
  padding: 0 16px 16px;
  -webkit-overflow-scrolling: touch;
}
.feed-empty {
  display: flex;
  flex-direction: column;
  justify-content: center;
}
.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 80px;
  color: #8e8e93;
  font-size: 13px;
}
.loading-ring {
  width: 26px;
  height: 26px;
  border: 3px solid rgba(255,159,10,0.25);
  border-top-color: #ff9f0a;
  border-radius: 50%;
  animation: spin 0.9s linear infinite;
  margin-bottom: 10px;
}
@keyframes spin { to { transform: rotate(360deg); } }
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 70px;
  color: #c7c7cc;
}
.empty-icon { margin-bottom: 12px; }
.empty-title { font-size: 16px; font-weight: 600; color: #8e8e93; }
.empty-sub { margin-top: 6px; font-size: 13px; }
.hole-card {
  background: #fff;
  border-radius: 16px;
  padding: 14px 16px 12px;
  margin-bottom: 12px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.04);
  animation: card-in 0.35s ease both;
}
.hole-new { animation: card-pop 0.5s ease both; }
@keyframes card-in { from { transform: translateY(8px); } to { transform: none; } }
@keyframes card-pop { 0% { transform: scale(0.96); } 70% { transform: scale(1.01); } 100% { transform: none; } }
.hole-head { display: flex; align-items: center; }
.hole-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: linear-gradient(135deg, #fff3d6, #ffe3a9);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  margin-right: 10px;
}
.hole-meta { display: flex; flex-direction: column; }
.hole-name { font-size: 14px; font-weight: 600; color: #3a3a3c; }
.hole-time { font-size: 11px; color: #aeaeb2; margin-top: 2px; }
.hole-text {
  margin-top: 10px;
  font-size: 15px;
  line-height: 1.6;
  color: #1d1d1f;
  word-break: break-word;
}
.hole-foot {
  margin-top: 10px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.like-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  border: none;
  background: #f5f6fa;
  color: #6e6e73;
  font-size: 12px;
  padding: 5px 12px;
  border-radius: 14px;
  cursor: pointer;
  transition: all 0.2s;
}
.like-btn.liked {
  background: rgba(255,159,10,0.12);
  color: #ff9f0a;
}
.hole-safe {
  font-size: 11px;
  color: #c7c7cc;
  letter-spacing: 0.5px;
}
.feed-end {
  text-align: center;
  font-size: 12px;
  color: #c7c7cc;
  padding: 16px 0 8px;
  letter-spacing: 1px;
}
.composer-mask {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.35);
  backdrop-filter: blur(4px);
  z-index: 50;
  display: flex;
  align-items: flex-end;
  justify-content: center;
}
.composer-sheet {
  width: 100%;
  max-width: 520px;
  background: #fff;
  border-radius: 18px 18px 0 0;
  padding: 10px 16px 20px;
  animation: sheet-up 0.28s cubic-bezier(0.32,0.72,0,1) both;
}
@keyframes sheet-up { from { transform: translateY(60px); } to { transform: none; } }
.sheet-handle {
  width: 38px;
  height: 5px;
  border-radius: 3px;
  background: #e5e5ea;
  margin: 2px auto 8px;
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
  color: #8e8e93;
  font-size: 14px;
  cursor: pointer;
  padding: 4px;
}
.sheet-title { font-size: 16px; font-weight: 600; color: #1d1d1f; }
.sheet-publish {
  border: none;
  background: linear-gradient(135deg, #ff9f0a, #ffbf4a);
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  padding: 7px 16px;
  border-radius: 18px;
  cursor: pointer;
}
.sheet-publish:disabled { opacity: 0.4; cursor: not-allowed; }
.sheet-textarea {
  width: 100%;
  height: 120px;
  border: none;
  outline: none;
  resize: none;
  font-size: 15px;
  line-height: 1.6;
  color: #1d1d1f;
  background: #f5f6fa;
  border-radius: 12px;
  padding: 12px;
  box-sizing: border-box;
}
.sheet-tip {
  margin-top: 10px;
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 11px;
  color: #aeaeb2;
}
</style>
