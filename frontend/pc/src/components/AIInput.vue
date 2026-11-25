<template>
  <div class="ai-input-container" :class="{ 'voice-mode': voiceMode, 'is-muted': muted }">
    <button
      class="voice-toggle-btn"
      @click="$emit('toggleVoice')"
      :title="voiceMode ? '切换为键盘输入' : '语音输入'"
      :aria-label="voiceMode ? '切换为键盘输入' : '语音输入'"
      :disabled="muted"
    >
      <svg v-if="!voiceMode" viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
        <rect x="9" y="2.5" width="6" height="12" rx="3" />
        <path d="M5.5 11.5a6.5 6.5 0 0 0 13 0" />
        <path d="M12 18v3.5" />
      </svg>
      <svg v-else viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
        <rect x="2.5" y="5" width="15" height="14" rx="2.5" />
        <path d="M21 8.5l-6 3.5 6 3.5z" />
      </svg>
    </button>
    <textarea
      v-if="!voiceMode"
      type="text"
      v-model="localMessage"
      @keyup.enter="handleKeyDown"
      :placeholder="muted ? '全员禁言中，仅群主和管理员可发言' : (chatType === 3 ? '有问题，尽管问' : '发送消息')"
      :disabled="muted"
      rows="1"
      maxlength="20000"
      @input="handleInput"
      @paste="handlePaste"
      ref="messageInput"
      :class="{ 'error': showEmptyWarn }"
      class="ai-textarea"
      aria-label="消息输入框"
      aria-describedby="message-input-help"
    ></textarea>
    <!-- 语音模式：按住说话 -->
    <div v-else class="voice-record-area">
      <div class="voice-record-hint" v-if="voiceRecording">
        <span class="voice-record-dot"></span>
        <span class="voice-record-time">{{ voiceDuration }}″</span>
        <span class="voice-record-text">松开发送 · 上滑取消</span>
      </div>
      <button
        class="voice-record-btn"
        :class="{ recording: voiceRecording }"
        @mousedown.prevent="onVoiceDown"
        @mouseup.prevent="onVoiceUp"
        @mouseleave.prevent="onVoiceLeave"
        @touchstart.prevent="onVoiceDown"
        @touchend.prevent="onVoiceUp"
        @touchcancel.prevent="onVoiceCancel"
      >
        <svg viewBox="0 0 24 24" width="26" height="26" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
          <rect x="9" y="2.5" width="6" height="12" rx="3" />
          <path d="M5.5 11.5a6.5 6.5 0 0 0 13 0" />
          <path d="M12 18v3.5" />
        </svg>
        <span>{{ voiceRecording ? '松开 结束' : '按住 说话' }}</span>
      </button>
    </div>
    <div class="ai-send-btn-wrapper">
      <transition name="warn-fade">
        <div v-if="showEmptyWarn" class="message-empty-warn" role="alert">
          <span class="error-text">不能发送空白信息</span>
          <span class="error-arrow"></span>
        </div>
      </transition>
      <button
        v-if="!voiceMode"
        :class="['ai-send-btn', { 'is-empty': isEmptySend, 'is-muted': muted }]"
        @click="handleSendOrStop"
        :disabled="muted"
        :aria-label="muted ? '全员禁言中，无法发送' : (isBotTyping ? '停止机器人' : '发送消息')"
      >
        <img v-if="isBotTyping" src="@/static/icons/stop.png" alt="停止" class="send-icon-img" />
        <img v-else src="@/static/icons/send.png" alt="发送" class="send-icon-img" />
      </button>
    </div>
  </div>
</template>

<script>
export default {
  name: 'AIInput',
  props: {
    modelValue: {
      type: String,
      default: ''
    },
    showEmptyWarn: {
      type: Boolean,
      default: false
    },
    chatType: {
      type: Number,
      default: 0
    },
    isBotTyping: {
      type: Boolean,
      default: false
    },
    hasImages: {
      type: Boolean,
      default: false
    },
    voiceMode: {
      type: Boolean,
      default: false
    },
    voiceRecording: {
      type: Boolean,
      default: false
    },
    voiceDuration: {
      type: Number,
      default: 0
    },
    muted: {
      type: Boolean,
      default: false
    }
  },
  emits: ['update:modelValue', 'send', 'keyDown', 'stop', 'pasteImage', 'backspace', 'toggleVoice', 'voiceStart', 'voiceStop', 'voiceCancel', 'atTrigger', 'caretChange', 'typing'],
  computed: {
    // 空内容且无图片、机器人也没在回复时，发送按钮呈灰色“空态”。
    // 注意：只灰不禁用 —— 禁用会让点击彻底静默，用户得不到“为什么发不出去”的反馈。
    isEmptySend() {
      return (!this.localMessage.trim() && !this.hasImages) && !this.isBotTyping
    }
  },
  data() {
    return {
      localMessage: this.modelValue
    }
  },
  watch: {
    modelValue: {
      handler(newValue) {
        this.localMessage = newValue
      },
      immediate: true
    },
    localMessage: {
      handler(newValue) {
        this.$emit('update:modelValue', newValue)
      }
    }
  },
  methods: {
    handleInput(event) {
      const el = (event && event.target) || this.$refs.messageInput
      if (!el) return
      const pos = typeof el.selectionStart === 'number' ? el.selectionStart : this.localMessage.length
      const ch = pos > 0 ? el.value.charAt(pos - 1) : ''
      // 手动敲入 @ 时通知父组件（群聊据此弹出成员面板，并按光标位置插入）
      if (ch === '@') {
        this.$emit('atTrigger', { position: pos })
      }
      this.$emit('caretChange', pos)
      // 输入内容变化时通知父组件发「正在输入」事件（节流与发送时机由父组件决定）
      this.$emit('typing')
    },
    // 供父组件在插入 @昵称 后恢复光标位置
    focusAt(position) {
      this.$nextTick(() => {
        const el = this.$refs.messageInput
        if (!el) return
        el.focus()
        const len = el.value.length
        const pos = Math.max(0, Math.min(typeof position === 'number' ? position : len, len))
        el.setSelectionRange(pos, pos)
      })
    },
    handleSendOrStop() {
      if (this.isBotTyping) {
        this.$emit('stop')
      } else {
        this.$emit('send')
      }
    },
    handleKeyDown(event) {
      // 处理backspace键
      if (event.key === 'Backspace' && this.localMessage === '') {
        this.$emit('backspace')
      }
      // 处理回车键发送消息
      else if (event.key === 'Enter' && !event.shiftKey) {
        // 有文字内容或有图片时可以发送
        if (this.localMessage.trim() || this.hasImages) {
          event.preventDefault()
          this.$emit('send')
        }
      }
      this.$emit('keyDown', event)
    },
    handlePaste(event) {
      const clipboardData = event.clipboardData || window.clipboardData
      const items = clipboardData.items
      
      if (items) {
        for (let i = 0; i < items.length; i++) {
          if (items[i].type.indexOf('image') !== -1) {
            const file = items[i].getAsFile()
            if (file) {
              event.preventDefault()
              this.$emit('pasteImage', file)
              return
            }
          }
        }
      }
    },
    onVoiceDown() {
      this.$emit('voiceStart')
    },
    onVoiceUp() {
      this.$emit('voiceStop')
    },
    onVoiceLeave() {
      // 按住期间滑出按钮区域视为取消
      if (this.voiceRecording) {
        this.$emit('voiceCancel')
      }
    },
    onVoiceCancel() {
      this.$emit('voiceCancel')
    }
  }
}
</script>

<style scoped>
.ai-input-container {
  position: relative;
  width: 100%;
  max-width: 800px;
  margin: 0 auto;
  /* 不能裁切：发送按钮上方的「不能发送空白信息」提示是绝对定位在容器外的，
     overflow:hidden 会把它裁成一条红边，用户在宽屏下根本读不到提示。 */
  overflow: visible;
  border-radius: 18px;
  background: var(--card);
  box-shadow: 0 2px 12px var(--separator);
  transition: all 0.3s ease;
}

.ai-input-container:focus-within {
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.12);
  transform: translateY(-1px);
}

.ai-textarea {
  width: 100%;
  height: 100%;
  padding: 14px 60px 14px 56px;
  border: none;
  border-radius: 18px;
  resize: none;
  box-sizing: border-box;
  line-height: 1.5;
  overflow-y: auto;
  outline: none;
  min-height: 56px;
  max-height: 180px; /* 限制最大高度 */
  font-family: inherit;
  font-size: 15px;
  color: #333;
  transition: all 0.3s ease;
  background: transparent;
  animation: inputFocus 0.3s ease-out;
}

.voice-toggle-btn {
  position: absolute;
  left: 8px;
  top: 50%;
  transform: translateY(-50%);
  width: 36px;
  height: 36px;
  border: none;
  border-radius: 50%;
  background: transparent;
  color: #8a8a8f;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.25s ease;
  outline: none;
  z-index: 2;
}

.voice-toggle-btn:hover {
  background: rgba(0, 0, 0, 0.06);
  color: #64B5F6;
}

.voice-mode .voice-toggle-btn {
  color: #64B5F6;
}

.voice-record-area {
  width: 100%;
  min-height: 56px;
  padding: 8px 52px 8px 52px;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
}

.voice-record-hint {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #8a8a8f;
}

.voice-record-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #f56c6c;
  animation: voiceBlink 1s ease-in-out infinite;
}

.voice-record-time {
  font-variant-numeric: tabular-nums;
  color: #f56c6c;
  font-weight: 600;
}

.voice-record-btn {
  width: 100%;
  height: 42px;
  border: none;
  border-radius: 21px;
  background: rgba(100, 181, 246, 0.12);
  color: #4a90d9;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: all 0.25s ease;
  outline: none;
  user-select: none;
  -webkit-user-select: none;
}

.voice-record-btn:hover {
  background: rgba(100, 181, 246, 0.2);
}

.voice-record-btn.recording {
  background: #f56c6c;
  color: #fff;
  transform: scale(1.02);
  box-shadow: 0 4px 16px rgba(245, 108, 108, 0.35);
}

@keyframes voiceBlink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.35; }
}

.voice-mode .ai-textarea {
  display: none;
}

.ai-textarea::-webkit-scrollbar {
  display: none;
}

.ai-textarea.error {
  color: #f56c6c;
}

.ai-textarea:focus {
  outline: none;
}

/* 全员禁言：输入框和发送键都不可用，但文案要让人一眼知道原因 */
.ai-input-container.is-muted {
  background: var(--fill, #f2f2f7);
  box-shadow: none;
  cursor: not-allowed;
}
.ai-input-container.is-muted .send-icon-img {
  opacity: .35;
}
.ai-textarea:disabled {
  cursor: not-allowed;
  color: #8a8a8f;
  -webkit-text-fill-color: #8a8a8f;
}

.ai-send-btn.is-muted {
  background: var(--fill-strong, #e0e0e0);
  cursor: not-allowed;
  box-shadow: none;
}
.ai-send-btn.is-muted:hover {
  background: var(--fill-strong, #e0e0e0);
  transform: none;
  box-shadow: none;
}

.ai-send-btn-wrapper {
  position: absolute;
  right: 8px;
  top: 50%;
  transform: translateY(-50%);
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
}

.ai-send-btn {
  width: 40px;
  height: 40px;
  border: none;
  border-radius: 50%;
  background: #e0e0e0;
  color: white;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
  box-shadow: none;
  outline: none;
}

.ai-send-btn:not(.is-empty) {
  background: #64B5F6;
  box-shadow: 0 2px 8px rgba(100, 181, 246, 0.3);
}

.ai-send-btn:hover:not(.is-empty) {
  background: #42A5F5;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(100, 181, 246, 0.4);
}

/* 空态只做视觉降级，按钮仍可点击：点击后由父组件置 showEmptyWarn 给出红字提示 */
.ai-send-btn.is-empty {
  background: #e0e0e0;
  cursor: pointer;
  box-shadow: none;
}

.ai-send-btn:focus {
  outline: none;
}

.ai-send-btn:active:not(.is-empty) {
  transform: translateY(-50%) scale(0.9);
}

.send-icon {
  font-size: 18px;
  font-weight: bold;
  transform: rotate(45deg);
}

.send-icon-img {
  width: 20px;
  height: 20px;
  object-fit: contain;
}

.ai-textarea::placeholder {
  color: #999;
  font-size: 15px;
  font-style: normal;
  /* 窄屏下输入区可用宽度有限，占位文案折成两行会把输入框撑成双倍高度 */
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 窄屏：左右两个圆形按钮只占 36/40px，56/60px 的对称内边距会把文字挤进 60px，
   占位文案折行、真实输入也只能看到三四个字。按实际按钮宽度收紧。 */
@media (max-width: 768px) {
  .ai-textarea {
    padding: 14px 50px 14px 46px;
  }
}

.message-empty-warn {
  padding: 4px 8px;
  background: #fef0f0;
  color: #f56c6c;
  border-radius: 3px;
  font-size: 12px;
  white-space: nowrap;
  animation: fadeIn 0.3s;
  position: absolute;
  /* 抬到输入框上方的留白区：贴着按钮放会压住「文件/语音通话/视频通话」这排图标 */
  bottom: calc(100% + 34px);
  z-index: 10;
  box-shadow: 0 2px 8px rgba(245, 108, 108, 0.18);
}

.error-arrow {
  position: absolute;
  bottom: -6px;
  right: 12px;
  width: 0;
  height: 0;
  border-left: 6px solid transparent;
  border-right: 6px solid transparent;
  border-top: 6px solid #ffccc7;
}

.error-arrow::after {
  content: '';
  position: absolute;
  bottom: 1px;
  left: -5px;
  width: 0;
  height: 0;
  border-left: 5px solid transparent;
  border-right: 5px solid transparent;
  border-top: 5px solid #fff2f2;
}

/* 入场只做位移，不碰 opacity：动画时钟冻结时“不能发送”的红字提示依然可读 */
.warn-fade-enter-active {
  transition: transform 0.18s ease-out;
}
.warn-fade-enter-from {
  transform: translateY(4px);
}
.warn-fade-leave-active {
  transition: none;
}
@keyframes fadeIn {
  from { transform: translateY(5px); }
  to { transform: translateY(0); }
}

/* 输入框入场：只上滑，不动 opacity —— 冻结在首帧时用户仍能看到自己打的字 */
@keyframes inputFocus {
  from {
    transform: translateY(10px);
  }
  to {
    transform: translateY(0);
  }
}
</style>
