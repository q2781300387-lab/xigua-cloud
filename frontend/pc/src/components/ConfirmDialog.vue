<template>
  <teleport to="body">
    <div v-if="visible" class="confirm-mask" @click.self="cancel">
      <div class="confirm-box" :class="{ mobile: isMobile }">
        <div class="confirm-title">{{ title }}</div>
        <div class="confirm-content">{{ content }}</div>

        <div class="confirm-actions">
          <button class="btn cancel" @click="cancel">{{ cancelText }}</button>
          <button class="btn confirm" @click="confirm">{{ confirmText }}</button>
        </div>
      </div>
    </div>
  </teleport>
</template>

<script>
export default {
  name: 'ConfirmDialog',
  props: {
    visible: Boolean,
    title: String,
    content: String,
    confirmText: { type: String, default: '确定' },
    cancelText: { type: String, default: '取消' }
  },
  emits: ['confirm', 'cancel'],
  computed: {
    isMobile() {
      return window.innerWidth < 768
    }
  },
  methods: {
    confirm() {
      this.$emit('confirm')
    },
    cancel() {
      this.$emit('cancel')
    }
  }
}
</script>

<style scoped>
.confirm-mask {
  position: fixed;
  inset: 0;
  /* 不依赖主题变量：confirm 组件挂在 body 上的独立 app 里，玻璃主题会把 --overlay 改成近透明 */
  background: rgba(0, 0, 0, 0.45);
  /* 要高于页面内的业务弹层（如群详情 .sheet-mask 2100），否则确认框会被遮住 */
  z-index: 2600;
  display: flex;
  justify-content: center;
  align-items: center;
}

.confirm-box {
  width: 320px;
  box-sizing: border-box;
  /* 固定实底 + 强模糊：既保证可读，又保留 iOS 毛玻璃观感 */
  background: rgba(255, 255, 255, 0.97);
  color: #1c1c1e;
  -webkit-backdrop-filter: blur(24px) saturate(180%);
  backdrop-filter: blur(24px) saturate(180%);
  border-radius: 14px;
  padding: 20px;
  animation: fadeIn .2s ease;
  box-shadow: 0 12px 40px rgba(19, 45, 83, 0.24);
  border: 1px solid rgba(60, 60, 67, 0.12);
}

html[data-theme="dark"] .confirm-box {
  background: rgba(28, 28, 30, 0.97);
  color: #f2f2f7;
  border-color: rgba(84, 84, 88, 0.6);
}

.confirm-box.mobile {
  width: 100%;
  border-radius: 16px 16px 0 0;
  position: absolute;
  bottom: 0;
}

.confirm-title {
  font-size: 17px;
  font-weight: 600;
  margin-bottom: 8px;
}

.confirm-content {
  font-size: 14px;
  color: rgba(60, 60, 67, 0.62);
  margin-bottom: 20px;
}

html[data-theme="dark"] .confirm-content {
  color: rgba(235, 235, 245, 0.66);
}

.confirm-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.btn {
  border: none;
  padding: 8px 18px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.32, 0.72, 0, 1);
}

.cancel {
  background: rgba(120, 120, 128, 0.14);
  color: inherit;
}

.cancel:hover {
  background: rgba(120, 120, 128, 0.22);
}

.confirm {
  background: var(--accent);
  color: #fff;
}

.confirm:hover {
  filter: brightness(1.08);
}

/* 入场只做位移，不碰 opacity：动画时钟被冻结时确认框仍完整可读，
   避免出现“遮罩出来了、框却看不见”导致无法点确定/取消 */
@keyframes fadeIn {
  from { transform: translateY(10px) }
  to { transform: translateY(0) }
}
</style>
