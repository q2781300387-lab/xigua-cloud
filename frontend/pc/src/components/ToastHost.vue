<template>
  <div v-if="list.length" class="toast-layer">
    <div
      v-for="t in list"
      :key="t.id"
      class="toast-item"
      :class="t.type"
      role="alert"
    >
      <span class="toast-icon">
        <svg v-if="t.type === 'success'" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round">
          <path d="M5 12.5l4.2 4.2L19 6.8" />
        </svg>
        <svg v-else-if="t.type === 'error'" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round">
          <path d="M7 7l10 10M17 7L7 17" />
        </svg>
        <svg v-else-if="t.type === 'warning'" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round">
          <path d="M12 6v7" />
          <circle cx="12" cy="17.6" r="1.05" fill="currentColor" stroke="none" />
        </svg>
        <svg v-else viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round">
          <path d="M12 7v6.5" />
          <circle cx="12" cy="17.2" r="1.05" fill="currentColor" stroke="none" />
        </svg>
      </span>
      <span class="toast-text">{{ t.message }}</span>
    </div>
  </div>
</template>

<script>
export default {
  name: 'ToastHost',
  props: {
    list: {
      type: Array,
      default: () => []
    }
  }
}
</script>

<style scoped>
.toast-layer {
  position: absolute;
  bottom: 92px;
  left: 0;
  right: 0;
  z-index: 1000100;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  pointer-events: none;
  padding: 0 16px;
  box-sizing: border-box;
}

.toast-item {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  max-width: min(560px, 92vw);
  padding: 11px 18px 11px 14px;
  border-radius: 14px;
  background: rgba(28, 28, 30, 0.94);
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.28), 0 2px 8px rgba(0, 0, 0, 0.18);
  border: 1px solid rgba(255, 255, 255, 0.08);
  color: #fff;
  /* 入场只做位移与缩放，不碰 opacity：即使动画时钟被冻结（后台窗口、
     动画降级、远程桌面等），提示也会以完全可读的状态停在首帧。 */
  animation: toast-in 0.26s cubic-bezier(0.32, 0.72, 0, 1);
  opacity: 1;
}

.toast-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  flex-shrink: 0;
}

.toast-icon.success { color: #30d158; }
.toast-icon.error { color: #ff453a; }
.toast-icon.warning { color: #ff9f0a; }
.toast-icon.info { color: #64d2ff; }

.toast-text {
  font-size: 14px;
  line-height: 1.45;
  letter-spacing: 0.2px;
  word-break: break-word;
}

@keyframes toast-in {
  from {
    transform: translateY(-14px) scale(0.96);
  }
  to {
    transform: translateY(0) scale(1);
  }
}

/* 系统要求减少动态效果时直接呈现终态，不做任何入场动画 */
@media (prefers-reduced-motion: reduce) {
  .toast-item {
    animation: none;
  }
}
</style>
