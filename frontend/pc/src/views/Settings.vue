<template>
  <div class="settings">
    <div class="settings-scroll">
      <div class="setting-item" @click="$router.push('/settings/profile')">
        <i class="icon">👤</i>
        <span>个人资料</span>
        <i class="arrow">→</i>
      </div>

      <GlassSettings />
    </div>
  </div>
</template>

<script setup name="settings">
import GlassSettings from '@/glass/GlassSettings.vue'
</script>

<style scoped>
/* ===== 设置 · iOS 设计语言 ===== */
.settings {
  width: 100%;
  height: 100%;
  overflow-y: auto;
  background: var(--bg);
  box-sizing: border-box;
  font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', 'SF Pro Text', 'PingFang SC', 'Helvetica Neue', 'Microsoft YaHei', sans-serif;
  -webkit-font-smoothing: antialiased;
}

.settings-scroll {
  padding: 20px;
}

.setting-item {
  display: flex;
  align-items: center;
  padding: 16px 18px;
  background: var(--card);
  border-radius: 14px;
  cursor: pointer;
  font-size: 15.5px;
  color: var(--label);
  font-weight: 500;
  box-shadow: 0 1px 3px var(--separator);
  transition: all 0.25s cubic-bezier(0.32, 0.72, 0, 1);
}

.setting-item:hover {
  background: var(--fill);
  transform: scale(1.005);
}

.setting-item .icon {
  font-size: 18px;
  margin-right: 12px;
  font-style: normal;
}

.arrow {
  margin-left: auto;
  color: rgba(60, 60, 67, 0.55);
  font-style: normal;
}
</style>
