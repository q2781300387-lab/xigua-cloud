<template>
  <div class="home-page" :class="phaseClass">
    <!-- 深色模式星光粒子背景 -->
    <div class="star-field" aria-hidden="true">
      <span v-for="i in 24" :key="i" :style="starStyle(i)"></span>
    </div>

    <!-- 顶部用户信息 -->
    <div class="user-header">
      <div class="greeting">
        <div class="analog-clock">
          <div class="clock-face">
            <div class="hand hour-hand" :style="hourStyle"></div>
            <div class="hand minute-hand" :style="minuteStyle"></div>
            <div class="hand second-hand" :style="secondStyle"></div>
          </div>
        </div>
        <div class="user-info">
          <h2>
            <span class="typed-greeting">{{ typedGreeting }}</span><span class="caret" v-if="typing">|</span>，{{ this.currentUser?.username }}
          </h2>
          <p class="meta-info">
            <span class="meta-chip">{{ fullDateInfo }}</span>
            <span class="meta-chip">{{ location }} · {{ weather.condition }} {{ weather.emoji }} {{ weather.temp }}℃</span>
            <span class="user-agent-info">{{ userAgentInfo }}</span>
          </p>
        </div>
      </div>
    </div>

    <!-- 数据概览卡片 -->
    <div class="stats-card">
      <!-- 加载骨架 -->
      <div v-if="homeLoading" class="stats-skeleton" aria-hidden="true">
        <div v-for="i in 3" :key="i" class="sk-stat">
          <div class="sk-icon"></div>
          <div class="sk-num"></div>
          <div class="sk-label"></div>
        </div>
      </div>

      <template v-else>
      <div class="stat-item">
        <div class="stat-icon-mini" style="color: #007aff;">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>
        </div>
        <div class="stat-value">{{ statValue(homeCount.friendCount) }}</div>
        <div class="stat-label">好友</div>
      </div>
      <div class="stat-item">
        <div class="stat-icon-mini" style="color: #34c759;">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5z"/></svg>
        </div>
        <div class="stat-value">{{ statValue(homeCount.unreadCount) }}</div>
        <div class="stat-label">未读消息</div>
      </div>
      <div class="stat-item">
        <div class="stat-icon-mini" style="color: #ff9500;">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="8.5" cy="8" r="3.5"/><path d="M1.5 20.5v-1.6a4.4 4.4 0 0 1 4.4-4.4h5.2a4.4 4.4 0 0 1 4.4 4.4v1.6"/><circle cx="16.5" cy="9.5" r="2.6"/><path d="M22.5 20.5v-1.3a3.4 3.4 0 0 0-2.7-3.3"/></svg>
        </div>
        <div class="stat-value">{{ statValue(homeCount.groupCount) }}</div>
        <div class="stat-label">群聊</div>
      </div>
      </template>
    </div>

    <!-- 快捷功能区 -->
    <div class="quick-actions">
      <h3 class="section-title">快捷入口</h3>
      <div class="action-grid">
        <div class="action-item" role="button" tabindex="0" @click="goTo('/chat', 'chat')" @keydown.enter="goTo('/chat', 'chat')" @keydown.space.prevent="goTo('/chat', 'chat')">
          <div class="action-icon" style="color: #34c759;">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5z"/></svg>
          </div>
          <div class="action-text">发起聊天</div>
        </div>
        <!-- AI聊天：跳机器人列表（/bot），不是普通聊天页 -->
        <div class="action-item" role="button" tabindex="0" @click="goTo('/bot', 'bot')" @keydown.enter="goTo('/bot', 'bot')" @keydown.space.prevent="goTo('/bot', 'bot')">
          <div class="action-icon" style="color: #af52de;">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M12 3l1.9 5.7 5.7 1.9-5.7 1.9L12 18.4l-1.9-5.9-5.7-1.9 5.7-1.9L12 3z"/><path d="M19 17.5l.9 2.6 2.6.9-2.6.9-.9 2.6-.9-2.6-2.6-.9 2.6-.9.9-2.6z"/></svg>
          </div>
          <div class="action-text">AI聊天</div>
        </div>
        <div class="action-item" role="button" tabindex="0" @click="goTo('/friend', 'friend')" @keydown.enter="goTo('/friend', 'friend')" @keydown.space.prevent="goTo('/friend', 'friend')">
          <div class="action-icon" style="color: #007aff;">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>
          </div>
          <div class="action-text">联系人</div>
        </div>
        <div class="action-item" role="button" tabindex="0" @click="goTo('/profile', 'profile')" @keydown.enter="goTo('/profile', 'profile')" @keydown.space.prevent="goTo('/profile', 'profile')">
          <div class="action-icon" style="color: #ff9500;">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
          </div>
          <div class="action-text">我的资料</div>
        </div>
      </div>
    </div>

    <div class="dashboard-section">
      <!-- 左侧：更新日志 (50%) -->
      <div class="update-log-panel">
        <div class="panel-header">
          <h3 class="panel-title">更新日志</h3>
          <div class="filter-tabs">
            <span
              v-for="t in updateTabs"
              :key="t.key"
              class="tab"
              :class="{ active: activeUpdateTab === t.key }"
              role="button"
              tabindex="0"
              @click="activeUpdateTab = t.key"
              @keydown.enter="activeUpdateTab = t.key"
            >{{ t.label }}</span>
          </div>
        </div>

        <div class="scrollable-content">
          <div v-if="filteredUpdates.length === 0" class="update-empty">
            <span class="update-empty-icon">🗂</span>
            <span>这个分类下暂时还没有更新</span>
          </div>

          <div v-for="(u, idx) in filteredUpdates" :key="idx" class="update-item">
            <div v-if="u.monthLabel" class="month-divider">{{ u.monthLabel }}</div>

            <div
              class="update-card"
              :class="{ featured: u.featured }"
            >
              <div class="card-header">
                <span class="version">{{ u.version }}</span>
                <span class="date">{{ u.date }}</span>
                <span class="tag" :class="u.tagClass">{{ u.tagText }}</span>
              </div>
              <h4 class="card-title">{{ u.title }}</h4>
              <p class="card-desc">{{ u.desc }}</p>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧：本周活跃 (50%) -->
      <div class="activity-panel">
        <div class="panel-header">
          <h3 class="panel-title">本周活跃</h3>
          <span class="time-range">{{ weekRange || getWeekRange() }}</span>
        </div>

        <div class="stats-grid">
          <div class="stat-card">
            <div class="stat-icon">💬</div>
            <div class="stat-content">
              <div class="stat-value">{{ val(0) }}</div>
              <div class="stat-label">消息发送(条)</div>
              <div class="trend" :class="trendClass(trends[0])">{{ trendText(trends[0]) }}</div>
            </div>
          </div>

          <div class="stat-card">
            <div class="stat-icon">👥</div>
            <div class="stat-content">
              <div class="stat-value">{{ val(1) }}</div>
              <div class="stat-label">新增好友(人)</div>
              <div class="trend" :class="trendClass(trends[1])">{{ trendText(trends[1]) }}</div>
            </div>
          </div>

          <div class="stat-card highlight">
            <div class="stat-icon">⏱️</div>
            <div class="stat-content">
              <div class="stat-value">{{ fmtAvg(activity[2]) }}</div>
              <div class="stat-label">日均消息(条)</div>
              <div class="trend" :class="trendClass(trends[2])">{{ trendText(trends[2]) }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>


  </div>
</template>

<script>
import { getLocation, getWeather } from "@/api/thirdParty.js";
import {getObject, set} from "@/utils/localStorage.js";
import {getHomeCount} from "@/api/home.js";
import { UAParser } from 'ua-parser-js';
import {getWebSocketClient} from "@/ws/WebSocketClient.js";
export default {
  name: 'Home',
  mounted() {
    this.timer = setInterval(() => {
      this.time = new Date()
    }, 1000) // 每秒更新
    this.getLocation().then(() => {
      this.getWeather()
    })

    this.parseUserAgent();

    // 订阅websocket消息
    getWebSocketClient().subscribe(this.wsListener);
  },
  beforeDestroy() {
    clearInterval(this.timer)
    clearInterval(this.typeTimer)
    this.typeTimer = null
    clearTimeout(this.activityTimer)
    this.activityTimer = null

    if (this.wsListener) {
      getWebSocketClient().unsubscribe(this.wsListener);
      this.wsListener = null;
    }
  },

  data() {
    return {
      time: new Date(),
      timer: null,
      location: '',
      weather: {
        condition: '',
        emoji: '',
        temp: ''
      },
      userAgentInfo: '', // 用户代理信息
      currentUser: {},
      homeCount: {
        friendCount: 0,
        unreadCount: 0,
        groupCount: 0
      },
      wsListener: null,
      dayPhase: 'noon',
      // 打字机问候
      typedGreeting: '',
      typing: false,
      typeTimer: null,
      // 数字滚动
      activity: [0, 0, 0],
      activityTargets: [0, 0, 0],
      // 环比百分比，null 表示上周同期没有基准，不能硬编一个数
      trends: [null, null, null],
      // 统计周期文字，优先后端返回的周一到周日
      weekRange: '',
      // 真数据是否已到位；未到位时卡片显示占位而不是 0
      statsReady: false,
      activityTimer: null,
      // 星光粒子
      stars: [],
      reduceMotion: false,
      // 统计卡加载骨架
      homeLoading: true,
      // 更新日志筛选（真实可点，不是装饰）
      activeUpdateTab: 'all',
      updateTabs: [
        { key: 'all', label: '全部' },
        { key: 'new', label: '新功能' },
        { key: 'optimized', label: '优化' },
        { key: 'fixed', label: '修复' }
      ],
      updates: [
        { version: '体验版', date: '2025年6月1日', tagText: '里程碑', tagClass: 'milestone', title: '体验环境上线', desc: '全新体验环境开放测试，欢迎反馈使用体验', kind: 'milestone', featured: true },
        { monthLabel: '2025年8月', version: 'v1.0.0', date: '12月06日', tagText: '新功能', tagClass: 'new', title: '打开/关闭/隐藏聊天窗口', desc: '新增聊天窗口打开/关闭/隐藏功能', kind: 'new' },
        { version: 'v1.0.0', date: '10月02日', tagText: '新功能', tagClass: 'new', title: '支持ai聊天', desc: '新增ai聊天功能，与ai助手互动', kind: 'new' },
        { version: 'v1.0.0', date: '08月24日', tagText: '新功能', tagClass: 'new', title: '支持群聊', desc: '创建群组，多人群组聊天功能', kind: 'new' },
        { version: 'v1.0.0', date: '08月12日', tagText: '新功能', tagClass: 'new', title: '消息失败重发功能上线', desc: '现在当消息发送失败时，系统会显示重试按钮', kind: 'new' },
        { version: 'v1.0.0', date: '08月12日', tagText: '优化', tagClass: 'optimized', title: '消息抖动问题修复', desc: '优化了消息发送时的动画效果', kind: 'optimized' },
        { version: 'v1.0.0', date: '08月03日', tagText: '优化', tagClass: 'optimized', title: '联系人页面改版', desc: '重新设计了联系人列表界面', kind: 'optimized' }
      ]
    }
  },
  created() {
    this.currentUser = getObject('userInfo');
    this.reduceMotion = window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches
    this.updateDayPhase()
    this.getHomeCount()
    this.buildStars()
    this.animateActivity()
  },
  computed: {
    filteredUpdates() {
      if (this.activeUpdateTab === 'all') return this.updates
      return this.updates.filter(u => u.kind === this.activeUpdateTab)
    },
    phaseClass() {
      return 'phase-' + this.dayPhase
    },
    greetingText() {
      const hour = this.time.getHours()
      if (hour < 5) return '夜深了，早点休息'
      if (hour < 8) return '清晨微光，早安'
      if (hour < 11) return '上午好，元气满满'
      if (hour < 14) return '中午好'
      if (hour < 17) return '午后好，小憩一下'
      if (hour < 19) return '黄昏温柔，傍晚好'
      return '晚上好'
    },
    greetingFull() {
      return `${this.greetingText}，${this.currentUser?.username || ''}`
    },
    fullDateInfo() {
      const weekdays = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六']
      return `${this.time.getFullYear()}年${this.time.getMonth()+1}月${this.time.getDate()}日 ${weekdays[this.time.getDay()]}`
    },
    hourStyle() {
      return {
        transform: `rotate(${(this.time.getHours() % 12) * 30 + this.time.getMinutes() * 0.5}deg)`
      }
    },
    minuteStyle() {
      return {
        transform: `rotate(${this.time.getMinutes() * 6}deg)`
      }
    },
    secondStyle() {
      return {
        transform: `rotate(${this.time.getSeconds() * 6}deg)`
      }
    }
  },
  watch: {
    greetingText: {
      immediate: true,
      handler(text) {
        if (!text) return
        if (this.reduceMotion) {
          this.typedGreeting = text
          this.typing = false
          return
        }
        this.typedGreeting = ''
        this.typing = true
        clearInterval(this.typeTimer)
        let i = 0
        this.typeTimer = setInterval(() => {
          i++
          this.typedGreeting = text.slice(0, i)
          if (i >= text.length) {
            clearInterval(this.typeTimer)
            this.typeTimer = null
            this.typing = false
          }
        }, 90)
      }
    }
  },
  methods: {
    // 生成星光粒子（确定性伪随机，避免 SSR/刷新闪烁）
    buildStars() {
      const stars = []
      for (let i = 0; i < 24; i++) {
        stars.push({
          left: ((i * 37 + 13) % 92) + 4 + '%',
          top: ((i * 53 + 7) % 82) + 6 + '%',
          size: 2 + (i % 4),
          delay: ((i * 31) % 60) / 10 + 's',
          duration: 3.2 + (i % 5) * 0.7 + 's',
          opacity: 0.25 + ((i * 17) % 40) / 100
        })
      }
      this.stars = stars
    },
    starStyle(i) {
      const s = this.stars[i - 1] || { left: '10%', top: '10%', size: 2, delay: '0s', duration: '4s', opacity: 0.4 }
      return {
        left: s.left,
        top: s.top,
        width: s.size + 'px',
        height: s.size + 'px',
        animationDelay: s.delay,
        animationDuration: s.duration,
        '--star-o': s.opacity
      }
    },
    // 本周活跃数字滚动
    animateActivity() {
      if (this.reduceMotion) {
        this.activity = [...this.activityTargets]
        return
      }
      const dur = 1300
      const t0 = performance.now()
      const targets = this.activityTargets
      const step = (now) => {
        const p = Math.min((now - t0) / dur, 1)
        const ease = 1 - Math.pow(1 - p, 3)
        this.activity = [
          Math.round(targets[0] * ease),
          Math.round(targets[1] * ease),
          Math.round(targets[2] * ease * 10) / 10
        ]
        if (p < 1) requestAnimationFrame(step)
      }
      requestAnimationFrame(step)
      // 页面在后台时 rAF 会被浏览器暂停，动画永远停在起始帧；补一个兜底把终值落下去
      clearTimeout(this.activityTimer)
      this.activityTimer = setTimeout(() => {
        this.activity = [...targets]
      }, dur + 150)
    },

    // 卡片数值：真数据没到位时显示占位，而不是把 0 当真
    val(i) {
      return this.statsReady ? this.activity[i] : '—'
    },

    // 日均保留一位小数
    fmtAvg(v) {
      if (!this.statsReady) return '—'
      return (Math.round(Number(v) * 10) / 10).toFixed(1)
    },

    // 上涨绿、下跌红、没有对比基准灰
    trendClass(v) {
      if (v === null || v === undefined) return 'flat'
      if (v > 0) return 'up'
      if (v < 0) return 'down'
      return 'flat'
    },

    trendText(v) {
      if (v === null || v === undefined) return '— 无对比'
      if (v === 0) return '持平'
      const arrow = v > 0 ? '↑' : '↓'
      return `${arrow}${Math.abs(v).toFixed(1)}%`
    },

    // 按时段计算问候背景光晕
    updateDayPhase() {
      const h = new Date().getHours()
      if (h >= 5 && h < 8) this.dayPhase = 'dawn'
      else if (h >= 8 && h < 11) this.dayPhase = 'morning'
      else if (h >= 11 && h < 14) this.dayPhase = 'noon'
      else if (h >= 14 && h < 17) this.dayPhase = 'afternoon'
      else if (h >= 17 && h < 19) this.dayPhase = 'dusk'
      else if (h >= 19 && h < 23) this.dayPhase = 'night'
      else this.dayPhase = 'midnight'
    },
    showComingSoon() {
      alert('功能即将上线，敬请期待！')
    },
    async getLocation() {
      const res = await getLocation()
      if(res.data.city != ''){
        this.location = res.data.city
      }else{
        this.location = '北京'
      }
    },
    async getWeather() {
      // 需要先获取城市名称（接上面的定位）
      const res = await getWeather( this.location)

      this.weather = {
        condition: res.data.info,
        emoji: this.getWeatherEmoji(res.data.info),
        temp: res.data.temperature || 26
      }
    },

    //根据天气情况返回对应的emoji
    getWeatherEmoji(weather) {
      // 简单的映射关系
      const map = {
        '晴': '☀️',
        '多云': '⛅',
        '阴': '☁️',
        '雨': '🌧️',
        '雪': '❄️',
        '雾': '🌫️',
        // 可以继续添加更多映射
      }
      return map[weather] || '🌤️'
    },

    // 获取首页统计信息
    async getHomeCount() {
      this.homeLoading = true
      try {
        const res = await getHomeCount()
        if (res.code == 200 && res.data) {
          const d = res.data
          this.homeCount = d
          // 三张卡改成后端真值：本周发送消息数 / 本周新增好友数 / 本周日均发送消息数
          this.activityTargets = [
            Number(d.weekMessageCount || 0),
            Number(d.weekFriendCount || 0),
            Number(d.weekDailyAvg || 0)
          ]
          // 环比由后端算（本周 vs 上周同期），拿不到基准就是 null，前端显示占位
          this.trends = [
            d.messageTrend == null ? null : Number(d.messageTrend),
            d.friendTrend == null ? null : Number(d.friendTrend),
            d.dailyTrend == null ? null : Number(d.dailyTrend)
          ]
          if (d.weekStart && d.weekEnd) {
            const fmt = (s) => {
              const [, mm, dd] = s.split('-')
              return `${Number(mm)}.${Number(dd)}`
            }
            this.weekRange = `${fmt(d.weekStart)} - ${fmt(d.weekEnd)}`
          }
          this.statsReady = true
          this.animateActivity()
        }
      } catch (e) {
        // 接口失败时不伪装成 0，卡片保留占位
        this.statsReady = false
      } finally {
        this.homeLoading = false
      }
    },

    // 统计数值展示：未拿到真实数据时用「—」，不要显示成 0
    statValue(n) {
      if (!this.statsReady || n === undefined || n === null) return '—'
      return n
    },

    // 跳转页面
    goTo(path, activeMenu) {
      if(path == null || activeMenu == null){
        return
      }
      this.$router.push(path)
      set('activeMenu', activeMenu)
    },

    // 获取本周范围
    getWeekRange() {
      const now = new Date()
      const start = new Date(now.getFullYear(), now.getMonth(), now.getDate() - now.getDay() + 1)
      const end = new Date(now.getFullYear(), now.getMonth(), now.getDate() - now.getDay() + 7)
      return `${start.getMonth() + 1}.${start.getDate()} - ${end.getMonth() + 1}.${end.getDate()}`
    },

    // 解析userAgent
    parseUserAgent() {
      try {
        const parser = new UAParser(navigator.userAgent);
        const result = parser.getResult();

        // 获取浏览器信息和版本
        const browserName = result.browser.name || '未知浏览器';
        const browserVersion = result.browser.version ? result.browser.version.split('.')[0] : '';

        // 获取操作系统信息和版本
        const osName = result.os.name || '未知系统';
        const osVersion = result.os.version ? result.os.version.split('.')[0] : '';

        // 格式化显示
        const browserDisplay = browserVersion ? `${browserName} ${browserVersion}` : browserName;
        const osDisplay = osVersion ? `${osName} ${osVersion}` : osName;

        this.userAgentInfo = `${browserDisplay} | ${osDisplay}`;

      } catch (error) {
        console.error('解析 UserAgent 失败:', error);

        // 降级方案：使用简单检测
        const ua = navigator.userAgent;
        let browser = '未知浏览器';
        let os = '未知系统';

        if (ua.includes('Chrome') && !ua.includes('Edg')) browser = 'Chrome';
        else if (ua.includes('Firefox')) browser = 'Firefox';
        else if (ua.includes('Safari') && !ua.includes('Chrome')) browser = 'Safari';
        else if (ua.includes('Edg')) browser = 'Edge';

        if (ua.includes('Windows')) os = 'Windows';
        else if (ua.includes('Mac')) os = 'macOS';
        else if (ua.includes('Linux')) os = 'Linux';
        else if (ua.includes('Android')) os = 'Android';
        else if (ua.includes('like Mac')) os = 'iOS';

        this.userAgentInfo = `${browser} | ${os}`;
      }
    },
  },
}
</script>

<style scoped>
.home-page {
  padding: 0;
  width: 100%;
  min-height: 100%;
  position: relative;
  font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', 'SF Pro Text', 'PingFang SC', 'Helvetica Neue', 'Microsoft YaHei', Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  /* 动态时段光晕背景：两团柔光 + 主题底色 */
  background:
    radial-gradient(640px 360px at 82% -8%, var(--glow-1, rgba(0, 122, 255, 0.10)), transparent 62%),
    radial-gradient(520px 320px at 6% 16%, var(--glow-2, rgba(255, 149, 0, 0.08)), transparent 60%),
    var(--bg);
  transition: background 1.4s cubic-bezier(0.32, 0.72, 0, 1);
}

/* —— 时段光晕（浅色值） —— */
.home-page.phase-midnight { --glow-1: rgba(175, 82, 222, 0.10); --glow-2: rgba(0, 122, 255, 0.08); }
.home-page.phase-dawn     { --glow-1: rgba(255, 149, 0, 0.16); --glow-2: rgba(94, 92, 230, 0.12); }
.home-page.phase-morning  { --glow-1: rgba(0, 122, 255, 0.14); --glow-2: rgba(52, 199, 89, 0.08); }
.home-page.phase-noon     { --glow-1: rgba(255, 204, 0, 0.14); --glow-2: rgba(0, 122, 255, 0.10); }
.home-page.phase-afternoon{ --glow-1: rgba(255, 149, 0, 0.12); --glow-2: rgba(0, 122, 255, 0.12); }
.home-page.phase-dusk     { --glow-1: rgba(255, 94, 58, 0.18); --glow-2: rgba(255, 149, 0, 0.12); }
.home-page.phase-night    { --glow-1: rgba(88, 86, 214, 0.14); --glow-2: rgba(0, 122, 255, 0.10); }

/* 入场动效：卡片依次上滑（不再动 opacity，动画时钟冻结时内容依然可见） */
@keyframes homeFadeUp {
  from {
    transform: translateY(16px);
  }
  to {
    /* 结束态必须是 none：配合 fill-mode both，动画后不再残留 transform，
       否则卡片会常驻独立合成层，在非整数缩放下出现横向拉伸的模糊条 */
    transform: none;
  }
}

/* 星光粒子层：默认隐藏，深色模式下显现 */
.star-field {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
  z-index: 0;
  opacity: 0;
  transition: opacity 1.4s ease;
}

.star-field span {
  position: absolute;
  border-radius: 50%;
  background: #fff;
  box-shadow: 0 0 6px 1px rgba(255, 255, 255, 0.45);
  animation-name: starFloat;
  animation-timing-function: ease-in-out;
  animation-iteration-count: infinite;
}

@keyframes starFloat {
  0%, 100% {
    transform: translateY(0) scale(1);
    opacity: var(--star-o, 0.4);
  }
  50% {
    transform: translateY(-14px) scale(1.25);
    opacity: calc(var(--star-o, 0.4) + 0.35);
  }
}

:root[data-theme="dark"] .star-field {
  opacity: 1;
}

/* 午夜时段：粒子更亮更密 */
:root[data-theme="dark"] .home-page.phase-midnight .star-field {
  opacity: 1;
}
:root[data-theme="dark"] .home-page.phase-midnight .star-field span {
  box-shadow: 0 0 8px 2px rgba(255, 255, 255, 0.55);
}

/* 打字机光标 */
.caret {
  display: inline-block;
  width: 2px;
  margin-left: 1px;
  height: 1.05em;
  vertical-align: -0.15em;
  background: var(--accent);
  animation: caretBlink 0.9s steps(2, start) infinite;
}

@keyframes caretBlink {
  to { visibility: hidden; }
}

/* 顶部统计卡、快捷入口、仪表板渐进入场（快速响应） */
.stats-card {
  animation: homeFadeUp 0.4s cubic-bezier(0.32, 0.72, 0, 1) both;
  animation-delay: 0.04s;
}

/* 统计卡加载骨架 */
.stats-skeleton {
  display: flex;
  gap: 14px;
  width: 100%;
}

.sk-stat {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 16px 10px;
}

.sk-icon {
  width: 30px;
  height: 30px;
  border-radius: 9px;
  background: linear-gradient(100deg, var(--fill) 40%, color-mix(in srgb, var(--fill) 40%, #fff 60%) 50%, var(--fill) 60%);
  background-size: 200% 100%;
  animation: skeletonShimmer 1.4s ease-in-out infinite;
}

.sk-num {
  width: 34px;
  height: 18px;
  border-radius: 6px;
  background: linear-gradient(100deg, var(--fill) 40%, color-mix(in srgb, var(--fill) 40%, #fff 60%) 50%, var(--fill) 60%);
  background-size: 200% 100%;
  animation: skeletonShimmer 1.4s ease-in-out infinite;
}

.sk-label {
  width: 46px;
  height: 10px;
  border-radius: 5px;
  background: linear-gradient(100deg, var(--fill) 40%, color-mix(in srgb, var(--fill) 40%, #fff 60%) 50%, var(--fill) 60%);
  background-size: 200% 100%;
  animation: skeletonShimmer 1.4s ease-in-out infinite;
}

@keyframes skeletonShimmer {
  0% { background-position: 160% 0; }
  100% { background-position: -40% 0; }
}

@media (prefers-reduced-motion: reduce) {
  .sk-icon,
  .sk-num,
  .sk-label {
    animation: none;
  }
}

.quick-actions {
  animation: homeFadeUp 0.4s cubic-bezier(0.32, 0.72, 0, 1) both;
  animation-delay: 0.08s;
}

.dashboard-section {
  animation: homeFadeUp 0.4s cubic-bezier(0.32, 0.72, 0, 1) both;
  animation-delay: 0.12s;
}

/* 本周活跃卡片逐张浮现 */
.stat-card {
  animation: homeFadeUp 0.4s cubic-bezier(0.32, 0.72, 0, 1) both;
}

.stats-grid .stat-card:nth-child(1) { animation-delay: 0.14s; }
.stats-grid .stat-card:nth-child(2) { animation-delay: 0.20s; }
.stats-grid .stat-card:nth-child(3) { animation-delay: 0.26s; }

/* 更新日志卡片渐进浮现 */
.update-card {
  animation: homeFadeUp 0.4s cubic-bezier(0.32, 0.72, 0, 1) both;
}

.update-log-panel .update-card:nth-child(2) { animation-delay: 0.06s; }
.update-log-panel .update-card:nth-child(3) { animation-delay: 0.10s; }
.update-log-panel .update-card:nth-child(4) { animation-delay: 0.14s; }
.update-log-panel .update-card:nth-child(5) { animation-delay: 0.18s; }
.update-log-panel .update-card:nth-child(6) { animation-delay: 0.22s; }

@media (prefers-reduced-motion: reduce) {
  .star-field span,
  .caret,
  .user-header,
  .stats-card,
  .quick-actions,
  .dashboard-section,
  .stat-card,
  .update-card {
    animation: none !important;
  }
  .star-field {
    display: none;
  }
}

.user-header {
  animation: homeFadeUp 0.4s cubic-bezier(0.32, 0.72, 0, 1) both;
}

.stats-card {
  animation: homeFadeUp 0.4s cubic-bezier(0.32, 0.72, 0, 1) 0.04s both;
}

.quick-actions {
  animation: homeFadeUp 0.4s cubic-bezier(0.32, 0.72, 0, 1) 0.08s both;
}

.dashboard-section {
  animation: homeFadeUp 0.4s cubic-bezier(0.32, 0.72, 0, 1) 0.12s both;
}

@media (prefers-reduced-motion: reduce) {
  .user-header,
  .stats-card,
  .quick-actions,
  .dashboard-section {
    animation: none;
  }
}

/* 顶部问候：iOS 毛玻璃大标题卡片 */
.user-header {
  padding: 20px;
  margin: 12px auto 0;
  max-width: 1024px;
  background:
    linear-gradient(105deg, rgba(0, 122, 255, 0.10) 0%, rgba(175, 82, 222, 0.075) 55%, rgba(255, 149, 0, 0.065) 100%),
    var(--card);
  border: 1px solid var(--separator);
  border-radius: 20px;
  box-shadow: 0 4px 12px var(--separator), 0 2px 4px rgba(0, 0, 0, 0.04);
}

/* 暗色下叠层太亮，单独压一版 */
html[data-theme="dark"] .user-header {
  background:
    linear-gradient(105deg, rgba(0, 122, 255, 0.14) 0%, rgba(175, 82, 222, 0.12) 55%, rgba(255, 149, 0, 0.08) 100%),
    var(--card);
}

.greeting {
  display: flex;
  align-items: center;
  gap: 16px;
  max-width: 1000px;
  margin: 0 auto;
}

.user-info {
  text-align: left;
  flex: 1;
}

.user-info h2 {
  margin: 0 0 4px 0;
  font-size: 26px;
  font-weight: 700;
  letter-spacing: -0.5px;
  color: var(--label);
}

.meta-info {
  margin: 0;
  font-size: 12.5px;
  color: var(--label-secondary);
  position: relative;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.meta-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 3px 10px;
  background: var(--fill);
  border-radius: 999px;
  color: var(--label-secondary);
  font-weight: 500;
}

/* iOS 精细表盘 */
.analog-clock {
  width: 52px;
  height: 52px;
  min-width: 52px;
  position: relative;
}

.clock-face {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  background: var(--card-gradient);
  border: 1px solid rgba(120, 120, 128, 0.25);
  position: relative;
  box-shadow: inset 0 1px 2px rgba(255, 255, 255, 0.9), 0 4px 12px rgba(0, 0, 0, 0.1);
}

.clock-face::after {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  width: 4px;
  height: 4px;
  margin: -2px 0 0 -2px;
  background: #ff9500;
  border-radius: 50%;
  box-shadow: 0 0 0 1px rgba(255, 149, 0, 0.25);
  z-index: 4;
}

.hand {
  position: absolute;
  left: 50%;
  bottom: 50%;
  transform-origin: 50% 100%;
}

.hour-hand {
  width: 3px;
  height: 13px;
  margin-left: -1.5px;
  background: var(--label);
  border-radius: 3px 3px 1.5px 1.5px;
  z-index: 3;
}

.minute-hand {
  width: 2px;
  height: 19px;
  margin-left: -1px;
  background: var(--label);
  border-radius: 2px 2px 1px 1px;
  z-index: 2;
}

.second-hand {
  width: 1px;
  height: 21px;
  margin-left: -0.5px;
  background: #ff9500;
  border-radius: 1px;
  z-index: 1;
}

/* 数据概览：iOS inset-grouped 卡片 */
.stats-card,
.quick-actions {
  margin: 12px auto 0;
  max-width: 1024px;
  background: var(--card);
  border-radius: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06), 0 1px 2px rgba(0, 0, 0, 0.04);
}

.stats-card {
  display: flex;
  overflow: hidden;
}

.stat-item {
  position: relative;
  text-align: center;
  flex: 1;
  padding: 16px 12px;
  transition: background-color 0.18s cubic-bezier(0.32, 0.72, 0, 1);
}

.stat-icon-mini {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  margin: 0 auto 8px;
  border-radius: 10px;
  background: rgba(120, 120, 128, 0.09);
}

.stat-icon-mini svg {
  width: 18px;
  height: 18px;
}

.stat-item + .stat-item::before {
  content: '';
  position: absolute;
  top: 50%;
  left: 0;
  width: 1px;
  height: 60%;
  transform: translateY(-50%);
  background: var(--separator-strong);
}

.stat-item:active {
  background: var(--fill);
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  line-height: 1.1;
  color: #007aff;
  font-variant-numeric: tabular-nums;
  letter-spacing: -0.5px;
}

.stat-label {
  font-size: 12.5px;
  color: var(--label-secondary);
  margin-top: 3px;
}

/* 快捷入口：iOS App 图标网格 */
.quick-actions {
  padding: 16px 16px 20px;
}

.section-title {
  margin: 0 0 12px 0;
  font-size: 12.5px;
  font-weight: 600;
  color: var(--label-secondary);
  letter-spacing: 0.1px;
}

.action-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  padding: 8px;
}

.action-item {
  text-align: center;
  padding: 18px 10px 14px;
  border-radius: 16px;
  background: var(--fill);
  border: 1px solid var(--separator);
  cursor: pointer;
  outline: none;
  transition: background 0.18s cubic-bezier(0.32, 0.72, 0, 1),
              transform 0.18s cubic-bezier(0.32, 0.72, 0, 1),
              box-shadow 0.18s cubic-bezier(0.32, 0.72, 0, 1),
              border-color 0.18s cubic-bezier(0.32, 0.72, 0, 1);
}

.action-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  margin: 0 auto 9px;
  border-radius: 13px;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.1), 0 1px 2px rgba(0, 0, 0, 0.06);
}

.action-icon svg {
  width: 24px;
  height: 24px;
}

.action-grid .action-item:nth-child(1) .action-icon {
  background: linear-gradient(145deg, rgba(52, 199, 89, 0.22), rgba(52, 199, 89, 0.12));
}

.action-grid .action-item:nth-child(2) .action-icon {
  background: linear-gradient(145deg, rgba(175, 82, 222, 0.2), rgba(175, 82, 222, 0.1));
}

.action-grid .action-item:nth-child(3) .action-icon {
  background: linear-gradient(145deg, rgba(0, 122, 255, 0.2), rgba(0, 122, 255, 0.1));
}

.action-grid .action-item:nth-child(4) .action-icon {
  background: linear-gradient(145deg, rgba(255, 149, 0, 0.22), rgba(255, 149, 0, 0.12));
}

.action-item:hover {
  background: var(--card);
  border-color: rgba(0, 122, 255, 0.28);
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(19, 45, 83, 0.10), 0 2px 6px rgba(19, 45, 83, 0.06);
}

.action-item:active {
  transform: scale(0.96);
  box-shadow: 0 2px 6px rgba(19, 45, 83, 0.08);
}

.action-item:focus-visible {
  border-color: rgba(0, 122, 255, 0.4);
  box-shadow: 0 0 0 3px rgba(0, 122, 255, 0.25);
}

.action-icon:hover {
  cursor: pointer;
}

.action-text {
  font-size: 12.5px;
  font-weight: 500;
  color: var(--label);
}

/* 看板双卡 */
.dashboard-section {
  display: flex;
  gap: 12px;
  margin: 12px 12px 16px;
  max-width: 1024px;
  margin-left: auto;
  margin-right: auto;
}

.update-log-panel,
.activity-panel {
  flex: 1;
  background: var(--card);
  border-radius: 20px;
  height: 322px;
  display: flex;
  flex-direction: column;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06), 0 1px 2px rgba(0, 0, 0, 0.04);
}

.panel-header {
  padding: 14px 18px 10px;
  border-bottom: 1px solid var(--separator);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.panel-title {
  margin: 0;
  font-size: 17px;
  font-weight: 700;
  letter-spacing: -0.3px;
  color: var(--label);
}

.time-range {
  font-size: 12px;
  font-weight: 500;
  color: var(--label-tertiary);
}

/* 筛选：iOS 分段控件 */
.filter-tabs {
  display: flex;
  gap: 4px;
  padding: 3px;
  background: var(--fill);
  border-radius: 999px;
}

.tab {
  padding: 4px 12px;
  font-size: 12px;
  font-weight: 600;
  color: var(--label-secondary);
  cursor: pointer;
  border-radius: 999px;
  transition: all 0.18s cubic-bezier(0.32, 0.72, 0, 1);
}

.tab:hover {
  color: #007aff;
  background: var(--fill);
}

.tab.active {
  background: var(--card);
  color: #007aff;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

/* 可滚动内容区域 */
.scrollable-content {
  flex: 1;
  overflow-y: auto;
  padding: 0 16px 16px;
}

.scrollable-content::-webkit-scrollbar {
  width: 5px;
}

.scrollable-content::-webkit-scrollbar-track {
  background: transparent;
}

.scrollable-content::-webkit-scrollbar-thumb {
  background-color: rgba(120, 120, 128, 0.32);
  border-radius: 999px;
}

.scrollable-content::-webkit-scrollbar-thumb:hover {
  background-color: rgba(120, 120, 128, 0.5);
}

/* 更新卡片：iOS 分组行 */
.update-card {
  background: var(--card);
  border: 1px solid var(--separator-strong);
  border-radius: 12px;
  padding: 13px 15px;
  margin: 9px 0;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.03);
  transition: all 0.18s cubic-bezier(0.32, 0.72, 0, 1);
}

.update-card:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px var(--separator);
}

.update-card.featured {
  background: rgba(0, 122, 255, 0.06);
  border-color: rgba(0, 122, 255, 0.18);
  border-left-width: 3px;
}

.card-header {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 6px;
}

.version {
  font-size: 12px;
  color: #007aff;
  font-weight: 600;
}

.date {
  font-size: 12px;
  color: var(--label-tertiary);
}

.tag {
  font-size: 11px;
  padding: 2px 9px;
  border-radius: 999px;
  font-weight: 600;
  letter-spacing: 0.1px;
}

.tag.milestone {
  background: rgba(0, 122, 255, 0.14);
  color: #007aff;
}

.tag.new {
  background: rgba(52, 199, 89, 0.14);
  color: #34c759;
}

.tag.optimized {
  background: rgba(255, 149, 0, 0.14);
  color: #ff9500;
}

.card-title {
  margin: 0 0 4px 0;
  font-size: 14.5px;
  font-weight: 600;
  letter-spacing: -0.2px;
  color: var(--label);
}

.card-desc {
  margin: 0;
  font-size: 12px;
  color: var(--label-secondary);
  line-height: 1.5;
}

/* 月份分隔线 */
.month-divider {
  font-size: 12px;
  font-weight: 600;
  color: var(--label-secondary);
  margin: 14px 0 6px 0;
  padding-left: 8px;
  position: relative;
}

/* 分类下无内容时的空态 */
.update-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 44px 12px;
  font-size: 13px;
  color: var(--label-tertiary);
}

.update-empty-icon {
  font-size: 22px;
  opacity: 0.65;
}

/* 活跃数据统计 */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 10px;
  padding: 14px;
}

.stat-card {
  background: var(--card);
  border: 1px solid var(--separator-strong);
  border-radius: 12px;
  padding: 13px 14px;
  display: flex;
  align-items: center;
  gap: 12px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.03);
  transition: all 0.18s cubic-bezier(0.32, 0.72, 0, 1);
}

.stat-card:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px var(--separator);
}

.stat-card.highlight {
  background: linear-gradient(145deg, rgba(0, 122, 255, 0.08), rgba(0, 122, 255, 0.03));
  border-color: rgba(0, 122, 255, 0.16);
}

.stat-icon {
  font-size: 21px;
  width: 40px;
  height: 40px;
  background: var(--fill);
  border-radius: 11px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-card.highlight .stat-icon {
  background: rgba(0, 122, 255, 0.12);
}

.stat-content {
  flex: 1;
}

.stat-value {
  font-size: 20px;
  font-weight: 700;
  letter-spacing: -0.3px;
  color: var(--label);
  margin-bottom: 2px;
  font-variant-numeric: tabular-nums;
}

.stat-label {
  font-size: 12px;
  color: var(--label-secondary);
}

.trend {
  font-size: 11px;
  font-weight: 600;
  margin-top: 2px;
}

.trend.up {
  color: #34c759;
}

.trend.down {
  color: #ff3b30;
}

.trend.flat {
  color: var(--label-tertiary, #8e8e93);
}

/* 底部摘要 */
.activity-summary {
  display: flex;
  border-top: 1px solid var(--separator);
  margin-top: auto;
}

.summary-item {
  flex: 1;
  text-align: center;
  padding: 12px;
}

.summary-value {
  font-size: 20px;
  font-weight: 700;
  color: #007aff;
}

.summary-label {
  font-size: 12px;
  color: var(--label-secondary);
  margin-top: 4px;
}

.user-agent-info {
  margin-left: auto;
  padding-left: 12px;
  font-size: 12px;
}

/* 窄屏适配 */
@media (max-width: 768px) {
  .dashboard-section {
    flex-direction: column;
  }

  .update-log-panel,
  .activity-panel {
    height: auto;
    min-height: 280px;
  }

  .action-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .user-info h2 {
    font-size: 20px;
  }

  .user-agent-info {
    display: none;
  }

  .stats-grid {
    grid-template-columns: 1fr;
  }

  .stat-card.highlight {
    grid-column: span 1;
  }
}
</style>