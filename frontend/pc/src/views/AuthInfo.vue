<template>
  <div class="info-page">
    <div class="bg-grid" aria-hidden="true"></div>
    <div class="bg-blob blob-left" aria-hidden="true"></div>
    <div class="bg-blob blob-right" aria-hidden="true"></div>

    <header class="info-header">
      <router-link class="brand" to="/login" :aria-label="copy.info.backToLogin">
        <img
          :src="assets.brandLockup"
          alt="西瓜IM"
          class="brand-lockup"
          draggable="false"
        />
      </router-link>

      <nav class="nav" :aria-label="copy.common.navLabel">
        <button
          v-for="item in navItems"
          :key="item.key"
          type="button"
          class="nav-key"
          :class="{ 'is-active': item.key === pageKey }"
          :aria-current="item.key === pageKey ? 'page' : undefined"
          @click="go(item.key)"
        >
          {{ item.label }}
        </button>
      </nav>

      <AuthLanguageSwitch @change="applyPageTitle" />
    </header>

    <main class="info-main">
      <article class="info-card">
        <p class="info-eyebrow">{{ page.eyebrow }}</p>
        <h1 class="info-title">{{ page.title }}</h1>
        <p class="info-lead">{{ page.lead }}</p>
        <div class="info-rule" aria-hidden="true"></div>

        <div v-if="pageKey === 'about'" class="about-sections">
          <section
            v-for="section in page.sections"
            :key="section.heading"
            class="about-row"
          >
            <h2>{{ section.heading }}</h2>
            <p>{{ section.body }}</p>
          </section>
        </div>

        <div v-else-if="pageKey === 'features'" class="feature-grid">
          <section
            v-for="(card, index) in page.cards"
            :key="card.title"
            class="feature-card"
          >
            <span class="feature-number" aria-hidden="true">
              {{ String(index + 1).padStart(2, "0") }}
            </span>
            <h2>{{ card.title }}</h2>
            <p>{{ card.body }}</p>
          </section>
        </div>

        <div v-else class="contact-list">
          <section
            v-for="channel in page.channels"
            :key="channel.title"
            class="contact-row"
          >
            <span class="contact-dot" aria-hidden="true"></span>
            <div class="contact-copy">
              <h2>{{ channel.title }}</h2>
              <p>{{ channel.body }}</p>
            </div>
          </section>
        </div>

        <p v-if="page.note" class="info-note">{{ page.note }}</p>

        <div class="info-actions">
          <router-link class="info-action primary" to="/login">
            {{ copy.info.backToLogin }}
          </router-link>
          <router-link class="info-action secondary" to="/register">
            {{ copy.info.createAccount }}
          </router-link>
        </div>
      </article>
    </main>

    <footer class="info-footer">
      <div>{{ copy.common.secure }}</div>
      <div>{{ copy.common.copyright }}</div>
    </footer>
  </div>
</template>

<script>
import brandLockup from "@/static/brand-lockup.png";
import AuthLanguageSwitch from "@/components/AuthLanguageSwitch.vue";
import { authLocale, getAuthCopy, syncAuthLanguageTag } from "@/locales/auth.js";

const PAGE_KEYS = ["about", "features", "contact"];

export default {
  name: "AuthInfo",
  components: { AuthLanguageSwitch },
  data() {
    return {
      assets: { brandLockup },
    };
  },
  computed: {
    copy() {
      return getAuthCopy(authLocale.value);
    },
    pageKey() {
      const key = this.$route.meta && this.$route.meta.infoPage;
      return PAGE_KEYS.includes(key) ? key : "about";
    },
    page() {
      return this.copy.info[this.pageKey];
    },
    navItems() {
      return PAGE_KEYS.map((key) => ({
        key,
        label: this.copy.common.nav[key],
      }));
    },
    pageTitle() {
      return this.page.pageTitle;
    },
  },
  watch: {
    pageTitle: {
      immediate: true,
      handler(title) {
        if (title) document.title = title;
      },
    },
  },
  mounted() {
    syncAuthLanguageTag();
  },
  methods: {
    go(key) {
      if (key === this.pageKey) return;
      this.$router.push(`/${key}`);
    },
    applyPageTitle() {
      this.$nextTick(() => {
        document.title = this.pageTitle;
      });
    },
  },
};
</script>

<style scoped>
.info-page,
.info-page *,
.info-page *::before,
.info-page *::after {
  box-sizing: border-box;
}

.info-page {
  --info-card-bg: rgba(255, 255, 255, 0.97);
  --info-card-border: rgba(60, 60, 67, 0.12);
  --info-inner-bg: #f7f7fa;
  --info-inner-border: rgba(60, 60, 67, 0.08);
  position: relative;
  height: 100vh;
  min-height: 100vh;
  width: 100%;
  display: flex;
  flex-direction: column;
  overflow-x: hidden;
  overflow-y: auto;
  background: var(--bg-gradient);
  font-family: -apple-system, BlinkMacSystemFont, "SF Pro Text", "PingFang SC",
    "Microsoft YaHei", Arial, sans-serif;
}

.bg-grid {
  position: fixed;
  inset: 0;
  background-image: linear-gradient(var(--separator) 1px, transparent 1px),
    linear-gradient(90deg, var(--separator) 1px, transparent 1px);
  background-size: 40px 40px;
  opacity: 0.3;
  pointer-events: none;
  z-index: 0;
}

.bg-blob {
  position: fixed;
  border-radius: 9999px;
  filter: blur(80px);
  pointer-events: none;
  z-index: 0;
}

.blob-left {
  left: -120px;
  top: 14%;
  width: 400px;
  height: 400px;
  background: rgba(0, 122, 255, 0.045);
}

.blob-right {
  right: -130px;
  bottom: 2%;
  width: 420px;
  height: 420px;
  background: rgba(52, 199, 89, 0.028);
}

/* 顶栏与登录 / 注册页保持一致 */
.info-header {
  position: relative;
  z-index: 40;
  width: 100%;
  max-width: 944px;
  height: 80px;
  margin: 0 auto;
  padding: 0 24px;
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  align-items: center;
  flex: 0 0 80px;
}

.brand {
  display: flex;
  align-items: center;
  justify-self: start;
  grid-column: 1;
  text-decoration: none;
}

.brand-lockup {
  display: block;
  width: auto;
  height: 42px;
  user-select: none;
  -webkit-user-drag: none;
}

.nav {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--label-secondary);
  justify-self: center;
  grid-column: 2;
}

.nav-key {
  appearance: none;
  -webkit-appearance: none;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 34px;
  padding: 0 15px;
  border-radius: 10px;
  background: #ffffff;
  border: 1px solid rgba(60, 60, 67, 0.12);
  color: var(--label-secondary);
  font: inherit;
  font-weight: 500;
  line-height: 1;
  white-space: nowrap;
  cursor: pointer;
  box-shadow: 0 1px 2px rgba(16, 24, 40, 0.05), inset 0 1px 0 rgba(255, 255, 255, 0.9);
  transition: transform 0.16s ease, background-color 0.16s ease, box-shadow 0.16s ease,
    border-color 0.16s ease, color 0.16s ease;
}

.nav-key:hover {
  background: #ffffff;
  border-color: rgba(60, 60, 67, 0.2);
  color: var(--label);
  transform: translateY(-1px);
  box-shadow: 0 3px 8px rgba(16, 24, 40, 0.08), inset 0 1px 0 #ffffff;
}

.nav-key:active {
  transform: translateY(1px);
  background: #f7f7fa;
  box-shadow: inset 0 1px 3px rgba(16, 24, 40, 0.08);
}

.nav-key:focus-visible {
  outline: 3px solid rgba(0, 122, 255, 0.28);
  outline-offset: 2px;
}

.nav-key.is-active {
  background: #1c1c1e;
  border-color: #1c1c1e;
  color: #ffffff;
  box-shadow: 0 2px 6px rgba(16, 24, 40, 0.16), inset 0 1px 0 rgba(255, 255, 255, 0.12);
}

/* 内容区 */
.info-main {
  position: relative;
  z-index: 2;
  width: 100%;
  max-width: 948px;
  margin: 0 auto;
  padding: 36px 24px 26px;
  flex: 1 0 auto;
}

.info-card {
  width: 100%;
  padding: 54px 54px 38px;
  border-radius: 28px;
  background: var(--info-card-bg);
  border: 1px solid var(--info-card-border);
  box-shadow: 0 18px 46px rgba(16, 24, 40, 0.08), 0 2px 6px rgba(16, 24, 40, 0.03),
    inset 0 1px 0 rgba(255, 255, 255, 0.7);
}

.info-eyebrow {
  margin: 0 0 14px;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 1.6px;
  text-transform: uppercase;
  color: var(--label-tertiary);
}

.info-title {
  margin: 0;
  font-size: 46px;
  line-height: 1.12;
  letter-spacing: -1.3px;
  color: var(--label);
}

.info-lead {
  max-width: 700px;
  margin: 18px 0 0;
  font-size: 16px;
  line-height: 1.85;
  color: var(--label-secondary);
}

.info-rule {
  height: 1px;
  margin: 30px 0 8px;
  background: var(--separator);
}

/* 关于：编辑式双栏 */
.about-row {
  display: grid;
  grid-template-columns: 180px minmax(0, 1fr);
  gap: 24px;
  padding: 24px 0;
  border-bottom: 1px solid var(--separator);
}

.about-row h2 {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  line-height: 1.7;
  color: var(--label);
}

.about-row p {
  margin: 0;
  font-size: 15px;
  line-height: 1.9;
  color: var(--label-secondary);
}

/* 功能：卡片网格 */
.feature-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  padding: 24px 0 6px;
}

.feature-card {
  position: relative;
  min-height: 150px;
  padding: 20px 20px 22px;
  border-radius: 16px;
  background: var(--info-inner-bg);
  border: 1px solid var(--info-inner-border);
}

.feature-number {
  display: block;
  margin-bottom: 14px;
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.8px;
  color: var(--label-tertiary);
  font-variant-numeric: tabular-nums;
}

.feature-card h2 {
  margin: 0 0 8px;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: -0.2px;
  color: var(--label);
}

.feature-card p {
  margin: 0;
  font-size: 13.5px;
  line-height: 1.8;
  color: var(--label-secondary);
}

/* 联系：条目式列表 */
.contact-list {
  padding: 14px 0 0;
}

.contact-row {
  display: grid;
  grid-template-columns: 10px minmax(0, 1fr);
  gap: 16px;
  padding: 20px 0;
  border-bottom: 1px solid var(--separator);
}

.contact-dot {
  width: 8px;
  height: 8px;
  margin-top: 7px;
  border-radius: 50%;
  background: var(--label);
}

.contact-row h2 {
  margin: 0 0 8px;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: -0.2px;
  color: var(--label);
}

.contact-row p {
  margin: 0;
  font-size: 14px;
  line-height: 1.85;
  color: var(--label-secondary);
}

.info-note {
  margin: 26px 0 0;
  padding-left: 14px;
  border-left: 2px solid var(--separator);
  font-size: 12.5px;
  line-height: 1.8;
  color: var(--label-tertiary);
}

/* 底部操作 */
.info-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 30px;
}

.info-action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 44px;
  padding: 0 20px;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 600;
  text-decoration: none;
  transition: transform 0.16s ease, box-shadow 0.16s ease, background-color 0.16s ease;
}

.info-action:hover {
  transform: translateY(-1px);
}

.info-action:active {
  transform: translateY(1px);
}

.info-action:focus-visible {
  outline: 3px solid rgba(0, 122, 255, 0.28);
  outline-offset: 2px;
}

.info-action.primary {
  background: #1c1c1e;
  border: 1px solid #1c1c1e;
  color: #ffffff;
  box-shadow: 0 6px 14px rgba(16, 24, 40, 0.16), inset 0 1px 0 rgba(255, 255, 255, 0.12);
}

.info-action.secondary {
  background: #ffffff;
  border: 1px solid rgba(60, 60, 67, 0.14);
  color: var(--label);
  box-shadow: 0 1px 2px rgba(16, 24, 40, 0.04), inset 0 1px 0 rgba(255, 255, 255, 0.9);
}

/* 页脚 */
.info-footer {
  position: relative;
  z-index: 2;
  margin: 0 auto 20px;
  padding: 0 24px;
  text-align: center;
  font-size: 10px;
  color: var(--label-tertiary);
  letter-spacing: 1px;
}

.info-footer div:last-child {
  margin-top: 8px;
  letter-spacing: normal;
}

@media (prefers-reduced-motion: reduce) {
  .nav-key,
  .info-action {
    transition: none;
  }
}

@media (max-width: 760px) {
  .info-header {
    height: auto;
    padding: 12px 16px 10px;
    row-gap: 10px;
    grid-template-rows: auto auto;
    flex: 0 0 auto;
  }

  .brand {
    grid-row: 1;
  }

  .brand-lockup {
    height: 36px;
  }

  :deep(.lang-switch) {
    grid-row: 1;
  }

  .nav {
    grid-row: 2;
    grid-column: 1 / -1;
    justify-self: start;
    gap: 6px;
    width: 100%;
  }

  .nav-key {
    height: 32px;
    padding: 0 12px;
    font-size: 12px;
  }

  .info-main {
    padding: 18px 16px 40px;
  }

  .info-card {
    padding: 32px 22px 28px;
    border-radius: 22px;
  }

  .info-title {
    font-size: 32px;
    letter-spacing: -0.8px;
  }

  .info-lead {
    margin-top: 14px;
    font-size: 14.5px;
    line-height: 1.8;
  }

  .info-rule {
    margin: 26px 0 6px;
  }

  .about-row {
    grid-template-columns: 1fr;
    gap: 8px;
    padding: 20px 0;
  }

  .feature-grid {
    grid-template-columns: 1fr;
    gap: 10px;
  }

  .feature-card {
    min-height: 0;
  }

  .contact-row {
    padding: 20px 0;
  }

  .info-actions {
    margin-top: 28px;
  }

  .info-footer {
    margin-bottom: 20px;
    padding: 0 16px;
  }
}
</style>

<style>
html[data-theme="dark"] .info-page {
  --info-card-bg: rgba(28, 28, 30, 0.94);
  --info-card-border: rgba(255, 255, 255, 0.1);
  --info-inner-bg: #2c2c2e;
  --info-inner-border: rgba(255, 255, 255, 0.08);
}

html[data-theme="dark"] .brand-lockup {
  filter: invert(1);
}

html[data-theme="dark"] .nav-key {
  background: #303033;
  border-color: rgba(255, 255, 255, 0.1);
  color: #aeaeb2;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.35), inset 0 1px 0 rgba(255, 255, 255, 0.06);
}

html[data-theme="dark"] .nav-key:hover {
  background: #3a3a3d;
  border-color: rgba(255, 255, 255, 0.16);
  color: #f2f2f7;
}

html[data-theme="dark"] .nav-key.is-active {
  background: #f2f2f7;
  border-color: #f2f2f7;
  color: #1c1c1e;
}

html[data-theme="dark"] .info-card {
  box-shadow: 0 18px 46px rgba(0, 0, 0, 0.32), inset 0 1px 0 rgba(255, 255, 255, 0.06);
}

html[data-theme="dark"] .info-action.primary {
  background: #f2f2f7;
  border-color: #f2f2f7;
  color: #1c1c1e;
}

html[data-theme="dark"] .info-action.secondary {
  background: #2c2c2e;
  border-color: rgba(255, 255, 255, 0.12);
  color: #f2f2f7;
}
</style>
