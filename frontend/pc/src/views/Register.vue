<template>
  <div class="register-page">
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

    <main class="register-main">
      <section class="hero" aria-hidden="true">
        <div class="hero-card">
          <div class="hero-grid"></div>
          <div class="hero-glow"></div>
          <div class="hero-characters">
            <AnimatedLoginCharacters
              :is-typing="Boolean(activeField)"
              :show-password="showPassword || showConfirmPassword"
              :password-length="form.password.length"
            />
          </div>
        </div>
      </section>

      <section class="auth-card" aria-labelledby="register-heading">
        <div class="auth-title">
          <h1 id="register-heading" class="auth-heading">{{ copy.register.heading }}</h1>
          <p class="auth-subtitle">{{ copy.register.subtitle }}</p>
        </div>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          class="register-form"
          @submit.prevent="register"
        >
          <div class="field">
            <label class="field-label" for="register-username">{{ copy.register.usernameLabel }}</label>
            <el-form-item prop="username" class="form-item">
              <el-input
                id="register-username"
                v-model="form.username"
                :placeholder="copy.register.usernamePlaceholder"
                autocomplete="username"
                :aria-label="copy.register.usernameLabel"
                @focus="handleFieldFocus('username')"
                @blur="handleFieldBlur"
              />
              <span class="input-icon" aria-hidden="true">
                <img :src="assets.userIcon" alt="" />
              </span>
            </el-form-item>
          </div>

          <div class="field">
            <label class="field-label" for="register-email">{{ copy.register.emailLabel }}</label>
            <el-form-item prop="email" class="form-item">
              <el-input
                id="register-email"
                v-model="form.email"
                :placeholder="copy.register.emailPlaceholder"
                autocomplete="email"
                :aria-label="copy.register.emailLabel"
                @blur="onEmailBlur"
                @focus="handleFieldFocus('email')"
              />
              <span class="input-icon" aria-hidden="true">
                <img :src="assets.mailIcon" alt="" />
              </span>
            </el-form-item>
          </div>

          <div class="field">
            <label class="field-label" for="register-code">{{ copy.register.codeLabel }}</label>
            <div class="code-wrap">
              <el-form-item prop="code" class="form-item">
                <el-input
                  id="register-code"
                  v-model="form.code"
                  maxlength="6"
                  :placeholder="copy.register.codePlaceholder"
                  class="code-input"
                  inputmode="numeric"
                  autocomplete="one-time-code"
                  :aria-label="copy.register.codeLabel"
                  @input="onCodeInput"
                  @focus="handleFieldFocus('code')"
                  @blur="handleFieldBlur"
                />
              </el-form-item>
              <button
                type="button"
                class="code-btn"
                :disabled="isCodeButtonDisabled || sendingCode"
                @mousedown.prevent
                @click="sendEmailCode"
              >
                {{
                  sendingCode
                    ? copy.register.sendingCode
                    : isCodeButtonDisabled
                    ? copy.register.resendCode.replace("{seconds}", timer)
                    : copy.register.getCode
                }}
              </button>
            </div>
          </div>

          <div class="field">
            <label class="field-label" for="register-password">{{ copy.register.passwordLabel }}</label>
            <div class="password-wrap">
              <el-form-item prop="password" class="form-item">
                <el-input
                  id="register-password"
                  ref="pwdInput"
                  v-model="form.password"
                  :type="showPassword ? 'text' : 'password'"
                  :placeholder="copy.register.passwordPlaceholder"
                  autocomplete="new-password"
                  :aria-label="copy.register.passwordLabel"
                  @input="onPasswordInput"
                  @focus="handleFieldFocus('password')"
                  @blur="handleFieldBlur"
                />
              </el-form-item>
              <button
                type="button"
                class="pwd-toggle"
                :class="{ on: showPassword }"
                :aria-pressed="showPassword ? 'true' : 'false'"
                :aria-label="showPassword ? copy.register.hidePassword : copy.register.showPassword"
                :title="showPassword ? copy.register.hidePassword : copy.register.showPassword"
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
            <p class="pwd-hint">{{ copy.register.passwordHint }}</p>
          </div>

          <div class="field">
            <label class="field-label" for="register-confirm-password">{{ copy.register.confirmLabel }}</label>
            <div class="password-wrap">
              <el-form-item prop="confirmPassword" class="form-item">
                <el-input
                  id="register-confirm-password"
                  ref="confirmPwdInput"
                  v-model="form.confirmPassword"
                  :type="showConfirmPassword ? 'text' : 'password'"
                  :placeholder="copy.register.confirmPlaceholder"
                  autocomplete="new-password"
                  :aria-label="copy.register.confirmLabel"
                  @focus="handleFieldFocus('confirmPassword')"
                  @blur="handleFieldBlur"
                />
              </el-form-item>
              <button
                type="button"
                class="pwd-toggle"
                :class="{ on: showConfirmPassword }"
                :aria-pressed="showConfirmPassword ? 'true' : 'false'"
                :aria-label="showConfirmPassword ? copy.register.hideConfirm : copy.register.showConfirm"
                :title="showConfirmPassword ? copy.register.hideConfirm : copy.register.showConfirm"
                @mousedown.prevent
                @click="toggleConfirmPassword"
              >
                <svg
                  v-if="showConfirmPassword"
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

          <button
            type="submit"
            class="submit-btn"
            :disabled="isLoading"
            :aria-busy="isLoading ? 'true' : 'false'"
          >
            <span v-if="isLoading" class="spinner" aria-hidden="true"></span>
            <span>{{ isLoading ? copy.register.submitting : copy.register.submit }}</span>
            <img v-if="!isLoading" :src="assets.arrowIcon" alt="" class="btn-arrow" />
          </button>
        </el-form>

        <div class="switch-link">
          {{ copy.register.haveAccount }}
          <router-link to="/login">{{ copy.register.signIn }}</router-link>
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
import userIcon from "@/static/user.svg";
import mailIcon from "@/static/mail.svg";
import brandLockup from "@/static/brand-lockup.png";
import arrowIcon from "@/static/arrow.svg";
import AnimatedLoginCharacters from "@/components/AnimatedLoginCharacters.vue";
import AuthLanguageSwitch from "@/components/AuthLanguageSwitch.vue";
import { authLocale, getAuthCopy, localizeServerMessage, syncAuthLanguageTag } from "@/locales/auth.js";
import { register } from "@/api/auth.js";
import { sendEmail } from "@/api/email.js";

export default {
  name: "Register",
  components: { AnimatedLoginCharacters, AuthLanguageSwitch },
  data() {
    return {
      isLoading: false,
      showPassword: false,
      showConfirmPassword: false,
      activeField: "",
      form: {
        username: "",
        email: "",
        code: "",
        password: "",
        confirmPassword: "",
      },
      isCodeButtonDisabled: false,
      sendingCode: false,
      timer: 0,
      countdownTimer: null,
      assets: { userIcon, mailIcon, brandLockup, arrowIcon },
    };
  },
  computed: {
    copy() {
      return getAuthCopy(authLocale.value);
    },
    rules() {
      const validation = this.copy.register.validation;
      const validateEmail = (rule, value, callback) => {
        if (!value) {
          callback(new Error(validation.emailRequired));
        } else if (!/^[1-9][0-9]{4,10}@qq\.com$/.test(value)) {
          callback(new Error(validation.emailInvalid));
        } else {
          callback();
        }
      };

      return {
        username: [
          { required: true, message: validation.usernameRequired, trigger: "blur" },
          {
            min: 2,
            max: 20,
            message: validation.usernameLength,
            trigger: ["blur", "change"],
          },
        ],
        email: [{ required: true, validator: validateEmail, trigger: "blur" }],
        code: [
          { required: true, message: validation.codeRequired, trigger: "blur" },
          { len: 6, message: validation.codeLength, trigger: "blur" },
        ],
        password: [
          { required: true, message: validation.passwordRequired, trigger: "blur" },
          {
            validator: (rule, value, callback) => {
              this.checkPassword(value, callback);
            },
            trigger: ["blur", "change"],
          },
        ],
        confirmPassword: [
          { required: true, message: validation.confirmRequired, trigger: "blur" },
          {
            validator: (rule, value, callback) => {
              if (!value) {
                callback(new Error(validation.confirmRequired));
              } else if (value !== this.form.password) {
                callback(new Error(validation.confirmMismatch));
              } else {
                callback();
              }
            },
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
  },
  methods: {
    goInfo(page) {
      this.$router.push(`/${page}`);
    },
    applyPageTitle() {
      document.title = this.copy.register.pageTitle;
    },
    onLocaleChange() {
      this.applyPageTitle();
    },
    handleFieldFocus(field) {
      this.activeField = field;
    },
    handleFieldBlur() {
      this.activeField = "";
    },
    // 密码强度：与后端 PasswordPolicy 保持同一套口径，前端先拦，避免白跑一次请求
    checkPassword(value, callback) {
      const validation = this.copy.register.validation;
      if (!value) return callback(new Error(validation.passwordRequired));
      if (value.length < 8 || value.length > 20) {
        return callback(new Error(validation.passwordLength));
      }
      if (/\s/.test(value)) return callback(new Error(validation.passwordNoSpaces));
      if (!/[A-Za-z]/.test(value)) return callback(new Error(validation.passwordLetter));
      if (!/[0-9]/.test(value)) return callback(new Error(validation.passwordDigit));
      if (this.form.username && value.toLowerCase() === this.form.username.toLowerCase()) {
        return callback(new Error(validation.passwordSame));
      }
      if (/(.)\1{2,}/.test(value)) {
        return callback(new Error(validation.passwordRepeats));
      }
      callback();
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
    // 确认密码的显隐切换：与密码框同一套交互
    toggleConfirmPassword() {
      this.showConfirmPassword = !this.showConfirmPassword;
      this.$nextTick(() => {
        const wrap = this.$refs.confirmPwdInput && this.$refs.confirmPwdInput.$el;
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
    // 改密码时若确认密码已填，立即复检一致性，避免用户改完密码后确认框仍显示通过
    onPasswordInput() {
      if (!this.form.confirmPassword) return;
      this.$nextTick(() => {
        const form = this.$refs.formRef;
        if (!form) return;
        try {
          const p = form.validateField("confirmPassword");
          if (p && p.catch) p.catch(() => {});
        } catch (e) {
          /* 校验未通过时 element 自身会展示错误文案 */
        }
      });
    },
    focusFirstInvalid(fields) {
      const order = ["username", "email", "code", "password", "confirmPassword"];
      const idMap = {
        username: "register-username",
        email: "register-email",
        code: "register-code",
        password: "register-password",
        confirmPassword: "register-confirm-password",
      };
      const key = order.find((k) => fields && fields[k]) || order.find((k) => !this.form[k]);
      const el = key && document.getElementById(idMap[key]);
      if (el) {
        el.focus();
        el.scrollIntoView({ block: "center", behavior: "smooth" });
      }
    },
    async register() {
      if (this.isLoading) return;
      this.isLoading = true;
      try {
        let fields = null;
        try {
          await this.$refs.formRef.validate();
        } catch (err) {
          fields = err || {};
        }
        if (fields) {
          this.$message.error(this.copy.register.toast.checkInput);
          this.$nextTick(() => this.focusFirstInvalid(fields));
          return;
        }

        const response = await register({
          username: this.form.username,
          email: this.form.email,
          code: this.form.code,
          password: this.form.password,
        }, { silent: true });
        // 非 200 的业务错误已由请求拦截器统一提示，这里只处理成功分支
        if (response && response.code === 200) {
          this.$message.success(this.copy.register.toast.success);
          this.$router.push("/login");
        } else if (response) {
          this.$message.error(
            localizeServerMessage(
              response.msg,
              this.copy.register.toast.registerFailed
            )
          );
        }
      } catch (error) {
        // silent 请求不再由拦截器统一提示，这里按当前语言给出可读原因
        const detail = error && error.message ? error.message : "";
        this.$message.error(
          localizeServerMessage(detail, this.copy.register.toast.registerFailed)
        );
        console.warn("[register]", detail || error);
      } finally {
        this.isLoading = false;
      }
    },
    // 验证码只接受数字：过滤粘贴进来的空格、字母与符号
    onCodeInput(val) {
      this.form.code = String(val || "").replace(/\D/g, "").slice(0, 6);
    },
    // 邮箱去首尾空格，避免把 " 123@qq.com" 直接发给后端
    onEmailBlur() {
      if (this.form.email) this.form.email = this.form.email.trim();
      this.handleFieldBlur();
    },
    async sendEmailCode() {
      if (this.sendingCode || this.isCodeButtonDisabled) return;

      const email = (this.form.email || "").trim();
      if (!email) {
        this.$message.error(this.copy.register.validation.emailRequired);
        return;
      }
      // 与后端保持同一套 QQ 邮箱规则
      if (!/^[1-9][0-9]{4,10}@qq\.com$/.test(email)) {
        this.$message.error(this.copy.register.validation.emailInvalid);
        return;
      }
      this.form.email = email;

      this.sendingCode = true;
      try {
        // silent：由本方法统一提示，避免拦截器再弹一条相同消息
        const response = await sendEmail({ email }, { silent: true });
        if (response && response.code === 200) {
          // 只有真正发出去了才开始倒计时，失败可立即重试
          this.startCountdown();
          this.$message.success(this.copy.register.toast.codeSent);
        } else {
          this.$message.error(
            localizeServerMessage(
              response && response.msg,
              this.copy.register.toast.sendFailed
            )
          );
        }
      } catch (error) {
        this.$message.error(
          localizeServerMessage(
            error && error.message,
            this.copy.register.toast.sendFailed
          )
        );
      } finally {
        this.sendingCode = false;
      }
    },
    startCountdown() {
      if (this.countdownTimer) clearInterval(this.countdownTimer);
      this.timer = 60;
      this.isCodeButtonDisabled = true;
      this.countdownTimer = setInterval(() => {
        if (this.timer > 0) {
          this.timer--;
        } else {
          clearInterval(this.countdownTimer);
          this.isCodeButtonDisabled = false;
        }
      }, 1000);
    },
  },
  beforeUnmount() {
    if (this.countdownTimer) clearInterval(this.countdownTimer);
  },
};
</script>

<style scoped>
.register-page,
.register-page *,
.register-page *::before,
.register-page *::after {
  box-sizing: border-box;
}

.register-page {
  min-height: 100vh;
  width: 100%;
  background: var(--bg-gradient);
  position: relative;
  font-family: -apple-system, BlinkMacSystemFont, "SF Pro Display", "SF Pro Text", "PingFang SC",
    "Helvetica Neue", "Microsoft YaHei", sans-serif;
  color: var(--label);
  display: flex;
  flex-direction: column;
  -webkit-font-smoothing: antialiased;
  overflow-x: hidden;
  overflow-y: auto;
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
.register-main {
  position: relative;
  z-index: 2;
  width: 100%;
  max-width: 1040px;
  margin: 0 auto;
  padding: 56px 24px 12px;
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
  display: flex;
  flex-direction: column;
  background: var(--login-card-bg);
  border: 1px solid rgba(15, 23, 42, 0.045);
  border-radius: 24px;
  box-shadow: 0 22px 56px rgba(15, 23, 42, 0.09), 0 2px 8px rgba(15, 23, 42, 0.04);
  -webkit-backdrop-filter: saturate(180%) blur(30px);
  backdrop-filter: saturate(180%) blur(30px);
  padding: 32px 36px 26px;
  position: relative;
  overflow: hidden;
}

.auth-title {
  margin-bottom: 16px;
}

.auth-heading {
  margin: 0;
  font-size: 26px;
  font-weight: 700;
  color: var(--label);
  letter-spacing: -0.5px;
}

.auth-subtitle {
  margin: 4px 0 0;
  font-size: 13px;
  color: var(--label-secondary);
}

.field {
  margin-bottom: 11px;
}

.field-label {
  display: block;
  font-size: 11.5px;
  font-weight: 600;
  color: var(--label-secondary);
  letter-spacing: 0.4px;
  margin-bottom: 6px;
  text-align: left;
}

.pwd-hint {
  margin: 4px 0 0;
  font-size: 10.5px;
  line-height: 1.35;
  color: var(--label-tertiary, #9aa0a6);
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

.password-wrap,
.code-wrap {
  position: relative;
}

/* 密码可见性切换 */
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

/* 验证码发送按钮：胶囊小按钮嵌在输入框右侧 */
.code-btn {
  position: absolute;
  right: 7px;
  top: 22px;
  transform: translateY(-50%);
  height: 30px;
  padding: 0 11px;
  border: 0;
  border-radius: 9px;
  background: rgba(0, 122, 255, 0.1);
  color: #007aff;
  font-family: inherit;
  font-size: 11.5px;
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
  transition: background 0.18s ease, opacity 0.18s ease;
}

.code-btn:hover:not(:disabled) {
  background: rgba(0, 122, 255, 0.16);
}

.code-btn:disabled {
  color: var(--label-tertiary);
  background: rgba(120, 120, 128, 0.1);
  cursor: default;
  opacity: 0.75;
}

.code-btn:focus-visible {
  outline: 2px solid rgba(0, 122, 255, 0.45);
  outline-offset: 1px;
}

.submit-btn {
  width: 100%;
  height: 46px;
  margin-top: 3px;
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

.switch-link {
  text-align: center;
  font-size: 12px;
  color: var(--label-secondary);
  margin-top: 12px;
  padding-top: 0;
}

.switch-link a {
  color: #007aff;
  font-weight: 600;
  text-decoration: none;
}

/* Element Plus 输入框 · 与登录页同一套 iOS 规格 */
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
  height: 44px;
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

/* 验证码输入框给右侧按钮留位置 */
:deep(.code-input .el-input__inner) {
  padding-right: 118px;
}

/* 校验错误文案：Element 的入场过渡在本项目里会卡在初始帧，直接取消过渡并常显 */
:deep(.el-form-item__error) {
  margin-top: 5px;
  color: #ff3b30;
  opacity: 1 !important;
  transform: none !important;
  transition: none !important;
}

:deep(.el-form-item.is-error) {
  margin-bottom: 24px;
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

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

@media (prefers-reduced-motion: reduce) {
  .spinner {
    animation: none;
  }

  .submit-btn,
  .nav-key {
    transition: none;
  }
}

@media (max-width: 1200px) {
  .register-main {
    gap: 40px;
  }
}

@media (max-width: 940px) {
  .hero {
    display: none;
  }

  .register-main {
    padding-top: 28px;
  }

  .auth-card {
    flex: 0 1 auto;
    width: 100%;
    max-width: 448px;
  }
}

@media (max-height: 820px) {
  .register-main {
    padding-top: 12px;
    padding-bottom: 0;
  }

  .auth-card {
    padding: 20px 36px 10px;
  }

  .login-footer {
    margin-bottom: 8px;
    padding-top: 8px;
  }
}

@media (max-width: 768px) {
  .login-header {
    padding: 0 16px;
  }

  .nav {
    display: none;
  }

  .register-main {
    padding: 20px 16px 0;
  }

  .auth-card {
    padding: 26px 20px 22px;
    border-radius: 20px;
  }

  .login-footer {
    margin-bottom: 20px;
    padding: 0 16px;
  }
}
</style>

<style>
html[data-glass] .register-page.register-page,
html[data-glass][data-theme="dark"] .register-page.register-page {
  background: var(--bg-gradient);
}

html[data-glass] .register-page .auth-card {
  background: var(--login-card-bg);
  border-color: var(--separator);
}

html[data-theme="dark"] .register-page .auth-card {
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
