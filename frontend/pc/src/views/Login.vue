<template>
  <div class="login-page">
    <div class="bg-grid" aria-hidden="true"></div>
    <div class="bg-blob blob-left" aria-hidden="true"></div>
    <div class="bg-blob blob-right" aria-hidden="true"></div>

    <header class="login-header">
      <div class="brand">
        <img
          :src="assets.brandLockup"
          alt="西瓜IM"
          class="brand-lockup"
          draggable="false"
        />
      </div>

      <nav class="nav" :aria-label="copy.common.navLabel">
        <button type="button" class="nav-key" @click="goInfo('about')">{{ copy.common.nav.about }}</button>
        <button type="button" class="nav-key" @click="goInfo('features')">{{ copy.common.nav.features }}</button>
        <button type="button" class="nav-key" @click="goInfo('contact')">{{ copy.common.nav.contact }}</button>
      </nav>

      <AuthLanguageSwitch @change="onLocaleChange" />
    </header>

    <main class="login-main">
      <section class="hero" aria-hidden="true">
        <div class="hero-card">
          <div class="hero-grid"></div>
          <div class="hero-glow"></div>
          <div class="hero-characters">
            <AnimatedLoginCharacters
              :is-typing="Boolean(activeField)"
              :show-password="showPassword"
              :password-length="form.password.length"
            />
          </div>
        </div>
      </section>

      <section class="auth-card" aria-labelledby="login-heading">
        <div class="auth-title">
          <h1 id="login-heading" class="auth-heading">{{ copy.login.heading }}</h1>
          <p class="auth-subtitle">{{ copy.login.subtitle }}</p>
        </div>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          class="login-form"
          @submit.prevent="login"
        >
          <div class="field">
            <label class="field-label" for="login-username">{{ copy.login.accountLabel }}</label>
            <el-form-item prop="username" class="form-item">
              <el-input
                id="login-username"
                ref="usernameInput"
                v-model="form.username"
                :placeholder="copy.login.accountPlaceholder"
                autocomplete="username"
                :aria-label="copy.login.accountLabel"
                @focus="handleFieldFocus('username')"
                @blur="handleFieldBlur"
              />
              <span class="input-icon" aria-hidden="true">
                <img :src="assets.userIcon" alt="" />
              </span>
            </el-form-item>
          </div>

          <div class="field">
            <label class="field-label" for="login-password">{{ copy.login.passwordLabel }}</label>
            <div class="password-wrap">
              <el-form-item prop="password" class="form-item">
                <el-input
                  id="login-password"
                  ref="pwdInput"
                  v-model="form.password"
                  :type="showPassword ? 'text' : 'password'"
                  :placeholder="copy.login.passwordPlaceholder"
                  autocomplete="current-password"
                  :aria-label="copy.login.passwordLabel"
                  @focus="handleFieldFocus('password')"
                  @blur="handleFieldBlur"
                />
              </el-form-item>
              <button
                type="button"
                class="pwd-toggle"
                :class="{ on: showPassword }"
                :aria-pressed="showPassword ? 'true' : 'false'"
                :aria-label="showPassword ? copy.login.hidePassword : copy.login.showPassword"
                :title="showPassword ? copy.login.hidePassword : copy.login.showPassword"
                @mousedown.prevent
                @click="togglePassword"
              >
                <svg
                  v-if="showPassword"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="1.6"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  aria-hidden="true"
                >
                  <path d="M3.2 12s3.6-6 8.8-6c2 0 3.8.7 5.3 1.8M20.8 12s-3.6 6-8.8 6c-2 0-3.8-.7-5.3-1.8" />
                  <path d="M4 4l16 16" />
                </svg>
                <svg
                  v-else
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="1.6"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  aria-hidden="true"
                >
                  <path d="M2.6 12S6.2 5.6 12 5.6 21.4 12 21.4 12 17.8 18.4 12 18.4 2.6 12 2.6 12Z" />
                  <circle cx="12" cy="12" r="2.6" />
                </svg>
              </button>
            </div>
          </div>

          <div class="form-row">
            <label class="remember">
              <input type="checkbox" v-model="remember" />
              <span class="checkbox" :class="{ checked: remember }">
                <svg v-if="remember" viewBox="0 0 24 24" aria-hidden="true">
                  <path
                    d="M5 12.5l4.2 4.2L19 7"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="2.2"
                    stroke-linecap="round"
                    stroke-linejoin="round"
                  />
                </svg>
              </span>
              <span>{{ copy.login.remember }}</span>
            </label>
            <button type="button" class="forgot-link" @click="handleForgotPassword">
              {{ copy.login.forgot }}
            </button>
          </div>

          <button
            type="submit"
            class="submit-btn"
            :disabled="loading"
            :aria-busy="loading ? 'true' : 'false'"
          >
            <span v-if="loading" class="spinner" aria-hidden="true"></span>
            <span>{{ loading ? copy.login.submitting : copy.login.submit }}</span>
            <img v-if="!loading" :src="assets.arrowIcon" alt="" class="btn-arrow" />
          </button>
        </el-form>

        <div class="alt-login">
          <div class="alt-divider"><span>{{ copy.login.otherMethods }}</span></div>
          <div class="alt-actions">
            <button
              type="button"
              class="alt-btn"
              :aria-label="copy.login.messageMethodLabel"
              :title="copy.login.messageMethodTitle"
              @click="handleAltLogin(copy.login.messageMethod)"
            >
              <svg viewBox="0 0 24 24" aria-hidden="true">
                <path
                  d="M4.8 5.2h14.4a1.6 1.6 0 0 1 1.6 1.6v8.4a1.6 1.6 0 0 1-1.6 1.6H9.2l-4.4 3v-3a1.6 1.6 0 0 1-1.6-1.6V6.8a1.6 1.6 0 0 1 1.6-1.6Z"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="1.5"
                  stroke-linejoin="round"
                />
              </svg>
            </button>
            <button
              type="button"
              class="alt-btn"
              :aria-label="copy.login.fingerprintMethodLabel"
              :title="copy.login.fingerprintMethodTitle"
              @click="handleAltLogin(copy.login.fingerprintMethod)"
            >
              <svg viewBox="0 0 24 24" aria-hidden="true">
                <path
                  d="M8.2 8.6a4 4 0 0 1 6.4 3.2c0 3.2-.7 6.2-2 8.6M6.3 6.2A6.7 6.7 0 0 1 18 11.8c0 1.5-.2 2.9-.5 4.2M4.8 11.6a7 7 0 0 1 .3-2.1M12 11.8c0 2.9-.6 5.8-1.9 8.3M9.4 11.8a2.5 2.5 0 0 1 5 0c0 2.7-.6 5.4-1.7 7.8M3.6 14.2c.2 1.6.7 3.1 1.5 4.5M15.4 11.8c0 2.7-.5 5.4-1.6 7.9"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="1.5"
                  stroke-linecap="round"
                />
              </svg>
            </button>
            <a
              class="alt-btn"
              href="https://watermelonim.eu.cc"
              target="_blank"
              rel="noopener noreferrer"
              :aria-label="copy.login.mobileMethodLabel"
              :title="copy.login.mobileMethodTitle"
            >
              <svg viewBox="0 0 24 24" aria-hidden="true">
                <rect
                  x="7"
                  y="2.6"
                  width="10"
                  height="18.8"
                  rx="2.3"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="1.5"
                />
                <path
                  d="M10.2 18.6h3.6"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="1.5"
                  stroke-linecap="round"
                />
              </svg>
            </a>
          </div>
        </div>

        <div class="switch-link">
          {{ copy.login.noAccount }}
          <router-link to="/register">{{ copy.login.join }}</router-link>
        </div>
      </section>
    </main>

    <footer class="login-footer">
      <div>{{ copy.common.secure }}</div>
      <div>{{ copy.common.copyright }}</div>
    </footer>
  </div>
</template>


<script>
import brandLockup from "@/static/brand-lockup.png";
import userIcon from "@/static/user.svg";
import arrowIcon from "@/static/arrow.svg";
import AnimatedLoginCharacters from "@/components/AnimatedLoginCharacters.vue";
import AuthLanguageSwitch from "@/components/AuthLanguageSwitch.vue";
import { authLocale, getAuthCopy, localizeServerMessage, syncAuthLanguageTag } from "@/locales/auth.js";
import { getUserInfo } from "@/api/user.js";
import { login } from "@/api/auth.js";
import { setToken } from "@/utils/auth.js";
import { set, setObject } from "@/utils/localStorage.js";
import { getWebSocketClient } from "@/ws/WebSocketClient.js";

const REMEMBER_KEY = "xigua-login-username";

export default {
  name: "Login",
  components: { AnimatedLoginCharacters, AuthLanguageSwitch },
  data() {
    return {
      form: {
        username: "",
        password: "",
      },
      remember: false,
      loading: false,
      showPassword: false,
      activeField: "",
      assets: { brandLockup, userIcon, arrowIcon },
    };
  },
  computed: {
    copy() {
      return getAuthCopy(authLocale.value);
    },
    rules() {
      const validation = this.copy.login.validation;
      return {
        username: [{ required: true, message: validation.usernameRequired, trigger: "blur" }],
        password: [
          { required: true, message: validation.passwordRequired, trigger: "blur" },
          {
            min: 6,
            max: 20,
            message: validation.passwordLength,
            trigger: ["blur", "change"],
          },
        ],
      };
    },
  },
  watch: {
    copy() {
      this.applyPageTitle();
      this.$nextTick(() => {
        const form = this.$refs.formRef;
        if (form && typeof form.clearValidate === "function") form.clearValidate();
      });
    },
  },
  mounted() {
    syncAuthLanguageTag();
    this.applyPageTitle();
    try {
      const savedName = localStorage.getItem(REMEMBER_KEY);
      if (savedName) {
        this.form.username = savedName;
        this.remember = true;
      }
    } catch (e) {
      /* 隐私模式下 localStorage 可能不可用，忽略即可 */
    }
  },
  methods: {
    goInfo(page) {
      this.$router.push(`/${page}`);
    },
    handleFieldFocus(field) {
      this.activeField = field;
    },
    handleFieldBlur() {
      this.activeField = "";
    },
    // 切换密码可见性：切换后保持输入焦点与光标在末尾，避免用户重新点击输入框
    togglePassword() {
      this.showPassword = !this.showPassword;
      this.$nextTick(() => {
        const wrap = this.$refs.pwdInput && this.$refs.pwdInput.$el;
        const input = wrap && wrap.querySelector("input");
        if (!input) return;
        const len = input.value ? input.value.length : 0;
        input.focus();
        try {
          input.setSelectionRange(len, len);
        } catch (e) {
          /* 某些浏览器对 password 类型暂不支持 setSelectionRange，忽略即可 */
        }
      });
    },
    persistRememberedName() {
      try {
        if (this.remember) {
          localStorage.setItem(REMEMBER_KEY, this.form.username);
        } else {
          localStorage.removeItem(REMEMBER_KEY);
        }
      } catch (e) {
        /* 忽略存储失败，不影响登录 */
      }
    },
    applyPageTitle() {
      document.title = this.copy.login.pageTitle;
    },
    onLocaleChange() {
      this.applyPageTitle();
    },
    handleForgotPassword() {
      this.$message({
        type: "info",
        message: this.copy.login.toast.contactAdmin,
        duration: 3000,
      });
    },
    handleAltLogin(method) {
      this.$message({
        type: "info",
        message: this.copy.login.toast.altUnavailable.replace("{method}", method),
        duration: 3000,
      });
    },
    async login() {
      if (this.loading) return;
      this.loading = true;
      try {
        let valid = false;
        try {
          valid = await this.$refs.formRef.validate();
        } catch (e) {
          valid = false;
        }
        if (!valid) {
          this.$message.error(this.copy.login.toast.checkInput);
          this.$nextTick(() => {
            const target = this.form.username
              ? this.$refs.pwdInput
              : this.$refs.usernameInput;
            if (target && typeof target.focus === "function") target.focus();
          });
          return;
        }

        const loginRes = await login({
          username: this.form.username,
          password: this.form.password,
        }, { silent: true });
        if (loginRes.code != 200) {
          const detail =
            (loginRes.data && loginRes.data.msg) ||
            loginRes.msg ||
            "";
          this.$message.error(
            this.copy.login.toast.loginFailed +
              localizeServerMessage(detail, this.copy.login.toast.tryLater)
          );
          return;
        }

        setToken(loginRes.data.token);
        this.persistRememberedName();
        // 换账号登录后不能沿用上一个账号的会话窗口，否则会先打开别人的最近会话
        localStorage.removeItem("currentChatWindow");

        const userInfoRes = await getUserInfo();
        if (userInfoRes.code != 200) {
          const detail =
            (userInfoRes.data && userInfoRes.data.msg) ||
            userInfoRes.msg ||
            "";
          this.$message.error(
            this.copy.login.toast.userInfoFailed +
              localizeServerMessage(detail, this.copy.login.toast.tryLater)
          );
          return;
        }

        setObject("userInfo", userInfoRes.data);
        set("activeMenu", "home");

        getWebSocketClient().connect();
        this.$router.push("/home");
      } catch (error) {
        const detail = error && error.message ? error.message : "";
        this.$message.error(
          this.copy.login.toast.loginFailed +
            localizeServerMessage(detail, this.copy.login.toast.tryLater)
        );
      } finally {
        this.loading = false;
      }
    },
  },
};
</script>

<style scoped>
.login-page,
.login-page *,
.login-page *::before,
.login-page *::after {
  box-sizing: border-box;
}

.login-page {
  --login-card-bg: rgba(255, 255, 255, 0.97);
  position: relative;
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

/* 背景网格 */
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

/* 背景柔光斑 */
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

/* 顶部导航 */
.login-header {
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

/* 关于 / 功能 / 联系：三枚独立实体小按键，和语言按钮同一套材质语言 */
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
  user-select: none;
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

/* 主区 */
.login-main {
  position: relative;
  z-index: 2;
  width: 100%;
  max-width: 1040px;
  margin: 0 auto;
  padding: 64px 24px 12px;
  display: flex;
  gap: 48px;
  align-items: center;
  justify-content: center;
  flex: 1 0 auto;
}

/* 左侧深灰角色图卡 */
.hero {
  flex: 0 0 400px;
  display: flex;
  justify-content: center;
}

.hero-card {
  position: relative;
  width: 400px;
  height: 548px;
  border-radius: 24px;
  overflow: hidden;
  background: linear-gradient(180deg, #747f88 0%, #66717a 46%, #4c5761 100%);
  box-shadow: 0 22px 54px rgba(47, 58, 69, 0.16), 0 4px 14px rgba(47, 58, 69, 0.06);
  isolation: isolate;
}

.hero-grid {
  position: absolute;
  inset: 0;
  background-image: linear-gradient(rgba(255, 255, 255, 0.055) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.055) 1px, transparent 1px);
  background-size: 44px 44px;
}

.hero-glow {
  position: absolute;
  left: -20%;
  right: -20%;
  bottom: -38%;
  height: 62%;
  background: radial-gradient(ellipse at 50% 100%, rgba(255, 255, 255, 0.14), transparent 68%);
  pointer-events: none;
}

.hero-characters {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  height: 330px;
  overflow: hidden;
}

.hero-characters :deep(.characters) {
  position: absolute;
  left: 60px;
  bottom: 0;
  transform: scale(0.62);
  transform-origin: bottom left;
}

/* 右侧表单卡 */
.auth-card {
  flex: 0 0 448px;
  min-height: 580px;
  display: flex;
  flex-direction: column;
  background: var(--login-card-bg);
  border: 1px solid rgba(15, 23, 42, 0.045);
  border-radius: 24px;
  box-shadow: 0 22px 56px rgba(15, 23, 42, 0.09), 0 2px 8px rgba(15, 23, 42, 0.04);
  -webkit-backdrop-filter: saturate(180%) blur(30px);
  backdrop-filter: saturate(180%) blur(30px);
  padding: 40px 40px 32px;
  position: relative;
  overflow: hidden;
}

.auth-title {
  margin-bottom: 24px;
}

.auth-heading {
  margin: 0;
  font-size: 27px;
  font-weight: 700;
  color: var(--label);
  letter-spacing: -0.5px;
}

.auth-subtitle {
  margin: 6px 0 0;
  font-size: 14px;
  color: var(--label-secondary);
}

.field {
  margin-bottom: 18px;
}

.field-label {
  display: block;
  font-size: 12px;
  font-weight: 600;
  color: var(--label-secondary);
  letter-spacing: 0.4px;
  margin-bottom: 8px;
  text-align: left;
}

.form-item {
  margin-bottom: 0;
  position: relative;
}

.input-icon {
  position: absolute;
  right: 14px;
  top: 50%;
  transform: translateY(-50%);
  width: 16px;
  height: 16px;
  pointer-events: none;
  opacity: 0.4;
}

.input-icon img {
  width: 100%;
  height: 100%;
}

/* 密码可见性切换 */
.password-wrap {
  position: relative;
}

.pwd-toggle {
  position: absolute;
  right: 7px;
  top: 50%;
  transform: translateY(-50%);
  width: 34px;
  height: 34px;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 0;
  border-radius: 10px;
  background: transparent;
  color: var(--label-tertiary);
  cursor: pointer;
  -webkit-tap-highlight-color: transparent;
  transition: color 0.18s ease, background 0.18s ease;
}

.pwd-toggle:hover {
  color: var(--label);
  background: rgba(120, 120, 128, 0.1);
}

.pwd-toggle:active {
  background: rgba(120, 120, 128, 0.16);
}

.pwd-toggle:focus-visible {
  outline: 2px solid rgba(0, 122, 255, 0.5);
  outline-offset: 1px;
}

.pwd-toggle.on {
  color: var(--label);
}

.pwd-toggle svg {
  width: 18px;
  height: 18px;
  display: block;
}

.form-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 4px;
  margin: 8px 0 24px;
  font-size: 12px;
  color: var(--label-secondary);
}

.remember {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  user-select: none;
}

.remember input {
  position: absolute;
  opacity: 0;
  pointer-events: none;
}

.checkbox {
  width: 18px;
  height: 18px;
  border: 1.5px solid var(--separator-strong);
  border-radius: 6px;
  background: var(--card);
  box-shadow: 0 1px 2px var(--separator);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s cubic-bezier(0.32, 0.72, 0, 1);
  position: relative;
}

.remember input:focus-visible + .checkbox {
  outline: 2px solid rgba(0, 122, 255, 0.5);
  outline-offset: 2px;
}

.remember input:checked + .checkbox {
  background: #007aff;
  border-color: #007aff;
}

.checkbox.checked {
  background: #007aff;
  border-color: #007aff;
}

.checkbox.checked svg {
  width: 12px;
  height: 12px;
  color: #ffffff;
}

.submit-btn {
  width: 100%;
  height: 50px;
  border-radius: 999px;
  background: linear-gradient(180deg, #3395ff 0%, #007aff 100%);
  border: none;
  color: white;
  font-size: 14.5px;
  font-weight: 600;
  letter-spacing: 0.3px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  cursor: pointer;
  transition: background 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
  box-shadow: 0 4px 16px rgba(0, 122, 255, 0.35);
}

.submit-btn:hover:not(:disabled) {
  background: #3395ff;
  box-shadow: 0 8px 24px rgba(0, 122, 255, 0.45);
  transform: translateY(-1px);
}

.submit-btn:active:not(:disabled) {
  transform: translateY(0);
  background: #0066d6;
}

.submit-btn:disabled {
  opacity: 0.68;
  cursor: default;
}

.btn-arrow {
  width: 12px;
  height: 12px;
}

.spinner {
  width: 14px;
  height: 14px;
  border-radius: 50%;
  border: 2px solid rgba(255, 255, 255, 0.35);
  border-top-color: #ffffff;
  animation: spin 0.8s linear infinite;
}

.forgot-link {
  border: 0;
  padding: 0;
  background: transparent;
  color: #007aff;
  font-family: inherit;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  -webkit-tap-highlight-color: transparent;
}

.forgot-link:hover {
  text-decoration: underline;
  text-underline-offset: 2px;
}

.forgot-link:focus-visible {
  outline: 2px solid rgba(0, 122, 255, 0.4);
  outline-offset: 2px;
  border-radius: 4px;
}

.alt-login {
  margin-top: 18px;
}

.alt-divider {
  display: flex;
  align-items: center;
  gap: 12px;
  color: var(--label-tertiary);
  font-size: 11px;
  white-space: nowrap;
}

.alt-divider::before,
.alt-divider::after {
  content: "";
  height: 1px;
  flex: 1;
  background: var(--separator);
}

.alt-actions {
  display: flex;
  justify-content: center;
  gap: 16px;
  margin-top: 14px;
}

.alt-btn {
  width: 38px;
  height: 38px;
  padding: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--separator);
  border-radius: 50%;
  background: var(--card);
  color: var(--label-secondary);
  cursor: pointer;
  -webkit-tap-highlight-color: transparent;
  transition: border-color 0.18s ease, background 0.18s ease, color 0.18s ease;
}

.alt-btn:hover {
  color: var(--label);
  border-color: var(--separator-strong);
  background: rgba(120, 120, 128, 0.08);
}

.alt-btn:focus-visible {
  outline: 2px solid rgba(0, 122, 255, 0.45);
  outline-offset: 2px;
}

.alt-btn svg {
  width: 18px;
  height: 18px;
}

.switch-link {
  text-align: center;
  font-size: 12px;
  color: var(--label-secondary);
  margin-top: auto;
  padding-top: 14px;
}

.switch-link a {
  color: #007aff;
  font-weight: 600;
  text-decoration: none;
}

/* Element Plus 输入框 · iOS */
:deep(.el-form-item__content) {
  width: 100%;
}

:deep(.el-input__wrapper) {
  box-shadow: none !important;
  background: rgba(118, 118, 128, 0.1);
  border-radius: 14px;
  transition: all 0.25s cubic-bezier(0.32, 0.72, 0, 1);
  padding: 0 16px;
}

:deep(.el-input__wrapper.is-focus) {
  background: var(--card);
  box-shadow: 0 0 0 2px rgba(0, 122, 255, 0.25) !important;
}

:deep(.el-input__inner) {
  height: 50px;
  border-radius: 14px;
  border: none;
  background: transparent;
  padding: 0 42px 0 16px;
  font-size: 14px;
  color: var(--label);
  font-family: inherit;
}

:deep(.el-input__inner::placeholder) {
  color: var(--label-tertiary);
  font-weight: 500;
}

/* 校验错误文案：Element 的入场过渡在本项目里会卡在初始帧，直接取消过渡并常显 */
:deep(.el-form-item__error) {
  margin-top: 6px;
  color: #ff3b30;
  opacity: 1 !important;
  transform: none !important;
  transition: none !important;
}

:deep(.el-form-item.is-error) {
  margin-bottom: 32px;
}

/* 页脚 */
.login-footer {
  position: relative;
  z-index: 2;
  margin: 0 auto 30px;
  padding-top: 8px;
  text-align: center;
  font-size: 10px;
  color: var(--label-tertiary);
  letter-spacing: 1px;
}

.login-footer div:last-child {
  margin-top: 8px;
  letter-spacing: normal;
  color: var(--label-tertiary);
}

@keyframes ping {
  0% {
    transform: scale(1);
    opacity: 0.6;
  }
  70% {
    transform: scale(1.8);
    opacity: 0;
  }
  100% {
    opacity: 0;
  }
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

@media (prefers-reduced-motion: reduce) {
  .badge-ping::after,
  .spinner {
    animation: none;
  }

  .submit-btn,
  .nav-key {
    transition: none;
  }
}

@media (max-width: 1200px) {
  .login-main {
    gap: 40px;
  }
}

@media (max-width: 940px) {
  .hero {
    display: none;
  }

  .login-main {
    padding-top: 28px;
  }

  .auth-card {
    flex: 0 1 auto;
    width: 100%;
    max-width: 448px;
    min-height: 0;
  }
}

@media (max-height: 820px) {
  .login-main {
    padding-top: 32px;
    padding-bottom: 8px;
  }

  .auth-card {
    min-height: 0;
  }

  .login-footer {
    margin-bottom: 8px;
    padding-top: 4px;
  }
}

@media (max-width: 768px) {
  .login-header {
    padding: 0 16px;
  }

  .nav {
    display: none;
  }

  .login-main {
    padding: 20px 16px 0;
  }

  .auth-card {
    padding: 28px 20px 26px;
    border-radius: 20px;
  }

  .login-footer {
    margin-bottom: 20px;
    padding: 0 16px;
  }
}
</style>

<style>
/* 玻璃模式会全局清空登录页底色并替换卡片表面，这里用更高优先级还原旧版视觉 */
html[data-glass] .login-page.login-page,
html[data-glass][data-theme="dark"] .login-page.login-page {
  background: var(--bg-gradient);
}

html[data-glass] .login-page .auth-card {
  background: var(--login-card-bg);
  border-color: var(--separator);
}

html[data-glass] .login-page .auth-card {
  box-shadow: var(--shadow-lg);
}

html[data-theme="dark"] .login-page.login-page {
  --login-card-bg: rgba(28, 28, 30, 0.94);
}

html[data-theme="dark"] .login-page .auth-card {
  border-color: rgba(255, 255, 255, 0.1);
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
  box-shadow: 0 3px 8px rgba(0, 0, 0, 0.4), inset 0 1px 0 rgba(255, 255, 255, 0.08);
}

html[data-theme="dark"] .nav-key:active {
  background: #262629;
  box-shadow: inset 0 1px 3px rgba(0, 0, 0, 0.5);
}
</style>


