<template>
  <div class="profile-container">
    <header class="profile-header">
      <button class="profile-back" type="button" aria-label="返回" @click="handleBack">
        <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M15 4L7 12l8 8"/>
        </svg>
      </button>
      <div class="profile-title">个人信息</div>
      <div class="profile-header-side"></div>
    </header>

    <div class="profile-scroll">
      <div class="profile-column">
        <section class="profile-hero" aria-label="个人名片">
          <button class="profile-avatar" type="button" aria-label="更换头像" @click="editAvatar">
            <img
              v-if="userInfo.avatar && !avatarError"
              :src="userInfo.avatar"
              class="profile-avatar-image"
              :alt="formData.username || '头像'"
              @error="avatarError = true"
            >
            <span v-else class="profile-avatar-fallback">{{ avatarFallback }}</span>
            <span class="profile-avatar-badge" aria-hidden="true">
              <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z"/>
                <circle cx="12" cy="13" r="4"/>
              </svg>
            </span>
          </button>

          <div class="profile-identity">
            <h1 class="profile-name">{{ formData.username || '未命名用户' }}</h1>
            <p class="profile-id">微信号 xgid_{{ formData.id || '—' }}</p>
            <p class="profile-signature">{{ formData.signature || defaultSignature }}</p>
          </div>

          <button class="profile-avatar-action" type="button" @click="editAvatar">更换头像</button>
        </section>

        <section class="profile-section">
          <div class="profile-section-title">账号资料</div>
          <div class="profile-group">
            <div class="ios-row">
              <span class="row-label">名字</span>
              <div class="row-value">
                <input
                  v-model="formData.username"
                  class="form-input"
                  type="text"
                  maxlength="20"
                  placeholder="请输入名字"
                  aria-label="名字"
                >
              </div>
            </div>

            <div class="ios-row">
              <span class="row-label">微信号</span>
              <div class="row-value">
                <div class="id-input-container">
                  <span class="id-prefix">xgid_</span>
                  <input
                    v-model="formData.id"
                    class="form-input with-prefix"
                    type="text"
                    :size="idInputSize"
                    placeholder="请输入微信号"
                    aria-label="微信号"
                  >
                </div>
              </div>
            </div>
          </div>
          <p class="profile-hint">微信号是账号的唯一标识，修改后请使用新号码登录。</p>
        </section>

        <section class="profile-section">
          <div class="profile-section-title">更多信息</div>
          <div class="profile-group">
            <div class="ios-row">
              <span class="row-label">性别</span>
              <div class="row-value">
                <div class="sex-select-wrapper">
                  <select v-model="formData.sex" class="sex-select" aria-label="性别">
                    <option :value="null">未设置</option>
                    <option :value="1">男</option>
                    <option :value="2">女</option>
                  </select>
                  <span class="select-arrow" aria-hidden="true">
                    <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                      <path d="M8 10l4 4 4-4"/>
                    </svg>
                  </span>
                </div>
              </div>
            </div>

            <div class="ios-row">
              <span class="row-label">地区</span>
              <div class="row-value">
                <input
                  v-model="formData.region"
                  class="form-input"
                  type="text"
                  maxlength="30"
                  placeholder="例如：河北 廊坊"
                  aria-label="地区"
                >
              </div>
            </div>

            <div class="ios-row">
              <span class="row-label">个性签名</span>
              <div class="row-value">
                <input
                  v-model="formData.signature"
                  class="form-input"
                  type="text"
                  maxlength="60"
                  placeholder="写一句介绍自己的话"
                  aria-label="个性签名"
                >
              </div>
            </div>
          </div>
        </section>

        <form class="profile-save-bar" @submit.prevent="handleSubmit">
          <button class="profile-save" type="submit" :disabled="saving">
            {{ saving ? '保存中…' : '保存修改' }}
          </button>
        </form>
      </div>
    </div>

    <transition name="profile-fade">
      <div v-if="showAvatarEditor" class="profile-avatar-overlay">
        <AvatarEditorView
          :initialImage="avatarImage"
          @confirm="handleAvatarConfirm"
          @back="handleCloseEditor"
        />
      </div>
    </transition>
  </div>
</template>

<script>
import { getUserInfo, updateUserInfo } from "@/api/user.js";
import AvatarEditorView from "./AvatarEditor.vue";

export default {
  components: {
    AvatarEditorView
  },
  data() {
    return {
      formData: {
        username: '',
        id: '',
        sex: null,
        region: '',
        signature: ''
      },
      defaultSignature: '这个人很懒，什么都没留下',
      userInfo: {
        avatar: ""
      },
      avatarError: false,
      showAvatarEditor: false,
      avatarImage: '',
      saving: false
    }
  },
  computed: {
    avatarFallback() {
      const name = (this.formData.username || '').trim();
      return name ? name.slice(0, 1) : '西';
    },
    idInputSize() {
      return Math.max(10, (this.formData.id || '').length + 2);
    }
  },
  created() {
    this.getUserInfo();
  },
  methods: {
    editAvatar() {
      this.showAvatarEditor = true;
      this.avatarImage = this.userInfo.avatar;
    },
    async getUserInfo() {
      try {
        const response = await getUserInfo();
        const data = response.data || {};

        this.formData = {
          username: data.username || '',
          id: data.id || '',
          sex: data.sex === 1 || data.sex === 2 ? data.sex : null,
          region: data.region || '',
          signature: data.signature || this.defaultSignature
        };

        this.userInfo.avatar = data.avatar || '';
        this.avatarError = false;
      } catch (error) {
        this.$message.error('个人信息加载失败，请稍后重试');
      }
    },
    async handleSubmit() {
      const username = (this.formData.username || '').trim();
      const id = (this.formData.id || '').trim();
      if (!username) {
        this.$message.warning('请填写名字');
        return;
      }
      if (!id) {
        this.$message.warning('请填写微信号');
        return;
      }
      if (this.saving) return;

      this.saving = true;
      try {
        const response = await updateUserInfo({
          ...this.formData,
          username,
          id,
          region: (this.formData.region || '').trim(),
          signature: (this.formData.signature || '').trim()
        });
        if (response.code === 200) {
          this.formData.username = username;
          this.formData.id = id;
          this.$message.success('个人信息已保存');
        } else {
          this.$message.error(response.msg || '保存失败，请稍后重试');
        }
      } catch (error) {
        this.$message.error('保存失败，请稍后重试');
      } finally {
        this.saving = false;
      }
    },
    handleAvatarConfirm(newAvatar) {
      if (newAvatar) {
        this.userInfo.avatar = newAvatar;
        this.avatarError = false;
      }
      this.closeAvatarEditor();
    },
    closeAvatarEditor() {
      this.showAvatarEditor = false;
    },
    handleCloseEditor() {
      this.showAvatarEditor = false;
    },
    handleBack() {
      this.$router.go(-1);
    }
  }
}
</script>

<style scoped>
/* ===== 个人信息 · iOS 设计语言 ===== */
.profile-container {
  position: relative;
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: var(--bg);
  color: var(--label);
  font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', 'SF Pro Text', 'PingFang SC', 'Helvetica Neue', 'Microsoft YaHei', sans-serif;
  -webkit-font-smoothing: antialiased;
}

/* 顶部导航：与聊天页同高，返回键和内容左边缘对齐 */
.profile-header {
  height: 52px;
  flex: 0 0 52px;
  display: flex;
  align-items: center;
  padding: 0 max(8px, calc((100% - 680px) / 2 + 8px));
  box-sizing: border-box;
  background: var(--bg-blur);
  -webkit-backdrop-filter: saturate(180%) blur(28px);
  backdrop-filter: saturate(180%) blur(28px);
  border-bottom: 1px solid var(--separator);
  position: sticky;
  top: 0;
  z-index: 10;
}

.profile-back {
  width: 40px;
  height: 40px;
  flex: none;
  border: 0;
  border-radius: 10px;
  background: transparent;
  color: var(--accent, #007aff);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: background .16s ease, transform .16s ease;
}
.profile-back:active {
  background: var(--fill);
  transform: scale(.94);
}
.profile-back:focus-visible {
  outline: 2px solid rgba(0, 122, 255, .35);
  outline-offset: 2px;
}

.profile-title {
  flex: 1;
  text-align: center;
  font-size: 17px;
  font-weight: 700;
  letter-spacing: -.3px;
  color: var(--label);
}
.profile-header-side {
  width: 40px;
  flex: 0 0 40px;
}

.profile-scroll {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  overscroll-behavior: contain;
  scrollbar-width: thin;
}

.profile-column {
  width: 100%;
  max-width: 680px;
  margin: 0 auto;
  padding: 20px 20px calc(12px + env(safe-area-inset-bottom));
  box-sizing: border-box;
}

/* ===== 个人名片 ===== */
.profile-hero {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  border-radius: 18px;
  background:
    radial-gradient(circle at 0 0, rgba(0, 122, 255, .10), rgba(0, 122, 255, 0) 58%),
    var(--card);
  border: 1px solid var(--separator);
  box-shadow: var(--shadow-sm);
  box-sizing: border-box;
}

.profile-avatar {
  position: relative;
  width: 70px;
  height: 70px;
  flex: none;
  padding: 0;
  border: 0;
  border-radius: 50%;
  background: var(--fill);
  cursor: pointer;
  overflow: visible;
}
.profile-avatar:focus-visible {
  outline: 2px solid rgba(0, 122, 255, .4);
  outline-offset: 3px;
}
.profile-avatar-image {
  width: 70px;
  height: 70px;
  display: block;
  border-radius: 50%;
  object-fit: cover;
  box-shadow: 0 6px 16px rgba(15, 23, 42, .14);
}
.profile-avatar-fallback {
  width: 70px;
  height: 70px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: linear-gradient(135deg, rgba(0, 122, 255, .16), rgba(0, 122, 255, .06));
  color: var(--accent, #007aff);
  font-size: 24px;
  font-weight: 700;
}
.profile-avatar-badge {
  position: absolute;
  right: -3px;
  bottom: -3px;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--accent, #007aff);
  color: #fff;
  border: 2px solid var(--card);
  box-shadow: 0 2px 8px rgba(0, 122, 255, .35);
}

.profile-identity {
  flex: 1;
  min-width: 0;
}
.profile-name {
  margin: 0;
  font-size: 21px;
  line-height: 1.25;
  font-weight: 700;
  letter-spacing: -.4px;
  color: var(--label);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.profile-id {
  margin: 5px 0 0;
  font-size: 12.5px;
  line-height: 1.4;
  color: var(--label-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.profile-signature {
  margin: 7px 0 0;
  font-size: 12.5px;
  line-height: 1.5;
  color: var(--label-tertiary);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  word-break: break-word;
}
.profile-avatar-action {
  flex: none;
  align-self: flex-start;
  margin-top: 2px;
  padding: 7px 12px;
  border: 0;
  border-radius: 999px;
  background: rgba(0, 122, 255, .10);
  color: var(--accent, #007aff);
  font-size: 12px;
  font-weight: 600;
  line-height: 1.2;
  cursor: pointer;
}
.profile-avatar-action:active { opacity: .62; }

/* ===== 分组卡片 ===== */
.profile-section {
  margin-top: 20px;
}
.profile-section-title {
  padding: 0 4px 8px;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: .3px;
  color: var(--label-secondary);
}
.profile-group {
  background: var(--card);
  border-radius: 16px;
  border: 1px solid var(--separator);
  box-shadow: var(--shadow-sm);
  overflow: hidden;
}
.profile-hint {
  margin: 8px 4px 0;
  font-size: 11.5px;
  line-height: 1.5;
  color: var(--label-tertiary);
}

.ios-row {
  display: flex;
  align-items: center;
  min-height: 54px;
  padding: 8px 16px;
  border-bottom: .5px solid var(--separator);
  transition: background-color .16s ease;
}
.ios-row:last-child {
  border-bottom: 0;
}
.ios-row:focus-within {
  background: var(--fill);
}

.row-label {
  width: 78px;
  flex: none;
  font-size: 14px;
  font-weight: 500;
  color: var(--label);
}
.row-value {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  justify-content: flex-end;
}

.form-input {
  width: 100%;
  border: 0;
  outline: 0;
  padding: 6px 0;
  background: transparent;
  color: var(--label);
  font-size: 14px;
  text-align: right;
  font-family: inherit;
}
.form-input::placeholder {
  color: var(--label-tertiary);
}
.form-input.with-prefix {
  width: auto;
  max-width: calc(100% - 38px);
  flex: 0 1 auto;
  padding-left: 4px;
  text-align: left;
  box-sizing: border-box;
}

.id-input-container {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: flex-end;
}
.id-prefix {
  flex: none;
  color: var(--label-secondary);
  font-size: 14px;
  pointer-events: none;
}

/* 性别：与其它行同一套右对齐文字，不再画成独立输入框 */
.sex-select-wrapper {
  position: relative;
  min-width: 86px;
}
.sex-select {
  width: 100%;
  border: 0;
  outline: 0;
  padding: 6px 24px 6px 0;
  background: transparent;
  color: var(--label);
  font-size: 14px;
  text-align-last: right;
  appearance: none;
  cursor: pointer;
  font-family: inherit;
}
.sex-select option {
  background: var(--card);
  color: var(--label);
}
.select-arrow {
  position: absolute;
  right: 0;
  top: 50%;
  transform: translateY(-50%);
  display: flex;
  color: var(--label-tertiary);
  pointer-events: none;
}

/* ===== 保存 ===== */
.profile-save-bar {
  position: sticky;
  bottom: 0;
  margin: 24px -20px 0;
  padding: 14px 20px calc(16px + env(safe-area-inset-bottom));
  background: linear-gradient(180deg, rgba(0, 0, 0, 0), var(--bg) 28%);
}
.profile-save {
  width: 100%;
  height: 48px;
  border: 0;
  border-radius: 999px;
  background: var(--accent, #007aff);
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: .2px;
  cursor: pointer;
  box-shadow: 0 6px 18px rgba(0, 122, 255, .28);
  transition: transform .18s cubic-bezier(.32, .72, 0, 1), opacity .18s ease;
}
.profile-save:active:not(:disabled) {
  transform: scale(.985);
  opacity: .9;
}
.profile-save:disabled {
  opacity: .55;
  cursor: default;
  box-shadow: none;
}
.profile-save:focus-visible {
  outline: 2px solid rgba(0, 122, 255, .35);
  outline-offset: 3px;
}

/* ===== 头像编辑器浮层 ===== */
.profile-avatar-overlay {
  position: absolute;
  inset: 0;
  z-index: 30;
  background: var(--bg);
}
.profile-fade-enter-active,
.profile-fade-leave-active {
  transition: opacity .18s ease;
}
.profile-fade-enter-from,
.profile-fade-leave-to {
  opacity: 0;
}

@media (max-width: 640px) {
  .profile-header {
    padding: 0 8px;
  }
  .profile-column {
    padding: 14px 14px calc(10px + env(safe-area-inset-bottom));
  }
  .profile-hero {
    gap: 13px;
    padding: 16px;
  }
  .profile-avatar,
  .profile-avatar-image,
  .profile-avatar-fallback {
    width: 60px;
    height: 60px;
  }
  .profile-avatar-fallback { font-size: 21px; }
  .profile-name { font-size: 19px; }
  .profile-avatar-action {
    padding: 6px 10px;
    font-size: 11px;
  }
  .profile-section { margin-top: 16px; }
  .row-label { width: 70px; }
  .profile-save-bar {
    margin-left: -14px;
    margin-right: -14px;
    padding-left: 14px;
    padding-right: 14px;
  }
}

@media (max-width: 420px) {
  .profile-avatar-action { display: none; }
  .profile-hero { align-items: flex-start; }
}
</style>
