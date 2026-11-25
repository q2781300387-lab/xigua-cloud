<template>
  <div class="glass-panel">
    <div class="gp-title">
      <span class="gp-title-main">玻璃主题</span>
      <span class="gp-title-sub">Aqua 风格 · 一键开关</span>
    </div>

    <div class="gp-group">
      <div class="gp-row" @click="toggleEnabled">
        <span class="gp-label">启用玻璃主题</span>
        <span class="ios-switch" :class="{ on: s.enabled }"><span class="switch-knob"></span></span>
      </div>
    </div>

    <template v-if="s.enabled">
      <div class="gp-group">
        <div class="gp-row">
          <span class="gp-label">背景模糊</span>
          <span class="gp-value">{{ s.blur }}px</span>
        </div>
        <input
          class="gp-range"
          type="range"
          min="0"
          max="40"
          :value="s.blur"
          @input="setNum('blur', $event)"
        />
        <div class="gp-row">
          <span class="gp-label">磨砂浓度</span>
          <span class="gp-value">{{ s.frost }}</span>
        </div>
        <input
          class="gp-range"
          type="range"
          min="0"
          max="100"
          :value="s.frost"
          @input="setNum('frost', $event)"
        />
      </div>

      <div class="gp-group">
        <div class="gp-row">
          <span class="gp-label">背景</span>
        </div>
        <div class="gp-seg">
          <button
            class="gp-seg-btn"
            :class="{ on: s.background === 'fluid' }"
            @click="setBg('fluid')"
          >
            流体光斑
          </button>
          <button
            class="gp-seg-btn"
            :class="{ on: s.background === 'wallpaper' }"
            @click="setBg('wallpaper')"
          >
            壁纸
          </button>
        </div>
        <div v-if="s.background === 'fluid'" class="gp-row">
          <span class="gp-label">色调</span>
          <span class="gp-value">{{ s.hue }}°</span>
        </div>
        <input
          v-if="s.background === 'fluid'"
          class="gp-range"
          type="range"
          min="0"
          max="360"
          :value="s.hue"
          @input="setNum('hue', $event)"
        />
        <div class="gp-row">
          <span class="gp-label">深浅</span>
          <span class="gp-value">{{ s.depth }}</span>
        </div>
        <input
          class="gp-range"
          type="range"
          min="0"
          max="100"
          :value="s.depth"
          @input="setNum('depth', $event)"
        />
        <div class="gp-row">
          <span class="gp-label">背景亮度</span>
          <span class="gp-value">{{ s.brightness }}</span>
        </div>
        <input
          class="gp-range"
          type="range"
          min="0"
          max="100"
          :value="s.brightness"
          @input="setNum('brightness', $event)"
        />
        <div v-if="s.background === 'wallpaper'" class="gp-row">
          <span class="gp-label">壁纸图片</span>
        </div>
        <div v-if="s.background === 'wallpaper'" class="gp-actions">
          <label class="gp-file">
            <input type="file" accept="image/*" @change="onPickWallpaper" />
            选择壁纸
          </label>
          <button v-if="s.wallpaper" class="gp-clear" @click="clearWallpaper">移除</button>
        </div>
      </div>

      <div class="gp-group">
        <div class="gp-row" @click="setBool('particles')">
          <span class="gp-label">漂浮光点</span>
          <span class="ios-switch" :class="{ on: s.particles }"><span class="switch-knob"></span></span>
        </div>
        <div class="gp-row" @click="setBool('edges')">
          <span class="gp-label">边缘渐隐</span>
          <span class="ios-switch" :class="{ on: s.edges }"><span class="switch-knob"></span></span>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup name="glass-settings">
import { reactive } from 'vue'

const api = window.XG_GLASS

const s = reactive(Object.assign({}, api ? api.get() : {}))

function toggleEnabled() {
  if (!api) return
  s.enabled = api.toggle()
}

function setNum(key, e) {
  const v = Number(e.target.value)
  s[key] = v
  if (api) api.set(key, v)
}

function setBool(key) {
  if (!api) return
  s[key] = !s[key]
  api.set(key, s[key])
}

function setBg(mode) {
  s.background = mode
  if (api) api.set('background', mode)
}

function onPickWallpaper(e) {
  const file = e.target.files?.[0]
  if (!file || !api) return
  const reader = new FileReader()
  reader.onload = () => {
    s.wallpaper = String(reader.result)
    api.set('wallpaper', s.wallpaper)
  }
  reader.readAsDataURL(file)
}

function clearWallpaper() {
  s.wallpaper = ''
  if (api) api.set('wallpaper', '')
}
</script>

<style scoped>
.glass-panel {
  margin-top: 4px;
  font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', 'SF Pro Text', 'PingFang SC',
    'Helvetica Neue', 'Microsoft YaHei', sans-serif;
}

.gp-title {
  display: flex;
  align-items: baseline;
  gap: 10px;
  padding: 14px 4px 10px;
}

.gp-title-main {
  font-size: 17px;
  font-weight: 700;
  color: var(--label);
}

.gp-title-sub {
  font-size: 12px;
  color: rgba(60, 60, 67, 0.55);
}

.gp-group {
  padding: 6px 12px;
  margin-bottom: 12px;
  background: var(--card);
  border-radius: 14px;
  box-shadow: 0 1px 3px var(--separator);
}

.gp-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 40px;
  cursor: pointer;
}

.gp-label {
  font-size: 15px;
  color: var(--label);
}

.gp-value {
  font-size: 13px;
  color: rgba(60, 60, 67, 0.7);
  font-variant-numeric: tabular-nums;
  min-width: 44px;
  text-align: right;
}

.gp-range {
  width: 100%;
  margin: 2px 0 10px;
  accent-color: var(--accent);
  -webkit-appearance: none;
  appearance: none;
  height: 4px;
  border-radius: 2px;
  background: rgba(120, 120, 128, 0.28);
  outline: none;
  cursor: pointer;
}

.gp-range::-webkit-slider-thumb {
  -webkit-appearance: none;
  appearance: none;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: #fff;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.25), 0 0 0 0.5px rgba(0, 0, 0, 0.08);
}

.gp-range::-moz-range-thumb {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: #fff;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.25);
  border: none;
}

.gp-seg {
  display: flex;
  gap: 8px;
  margin: 6px 0 10px;
}

.gp-seg-btn {
  flex: 1;
  height: 34px;
  font-size: 13px;
  font-weight: 600;
  color: rgba(60, 60, 67, 0.7);
  background: var(--fill);
  border: none;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.25s cubic-bezier(0.32, 0.72, 0, 1);
}

.gp-seg-btn.on {
  color: #fff;
  background: var(--accent);
  box-shadow: 0 4px 10px rgba(0, 122, 255, 0.28);
}

.gp-actions {
  display: flex;
  gap: 8px;
  padding-bottom: 10px;
}

.gp-file {
  display: inline-flex;
  align-items: center;
  height: 32px;
  padding: 0 14px;
  font-size: 13px;
  font-weight: 600;
  color: var(--accent);
  background: rgba(0, 122, 255, 0.1);
  border-radius: 10px;
  cursor: pointer;
}

.gp-file input {
  display: none;
}

.gp-clear {
  height: 32px;
  padding: 0 12px;
  font-size: 13px;
  color: rgba(60, 60, 67, 0.7);
  background: var(--fill);
  border: none;
  border-radius: 10px;
  cursor: pointer;
}

/* iOS 开关 */
.ios-switch {
  position: relative;
  width: 38px;
  height: 22px;
  border-radius: 11px;
  background: rgba(120, 120, 128, 0.28);
  transition: background 0.28s cubic-bezier(0.32, 0.72, 0, 1);
  flex-shrink: 0;
}

.ios-switch.on {
  background: #34c759;
}

.switch-knob {
  position: absolute;
  top: 2px;
  left: 2px;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: #fff;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.25);
  transition: transform 0.28s cubic-bezier(0.32, 0.72, 0, 1);
}

.ios-switch.on .switch-knob {
  transform: translateX(16px);
}
</style>
