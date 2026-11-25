<template>
  <div
    class="mascot"
    role="button"
    tabindex="0"
    aria-label="摸摸西瓜大象吉祥物"
    :class="{
      'is-happy': happy,
      'is-shy': passwordVisible,
      'is-listening': activeField,
      'is-hovered': hovered,
    }"
    @mouseenter="hovered = true"
    @mouseleave="hovered = false"
    @click="cheer"
    @keydown.enter.prevent="cheer"
    @keydown.space.prevent="cheer"
  >
    <svg
      class="mascot-svg"
      viewBox="0 0 280 280"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      aria-hidden="true"
    >
      <g class="mascot-aura">
        <circle cx="140" cy="140" r="112" fill="#F0FFF4" opacity="0.6" />
        <circle cx="140" cy="140" r="93.3" fill="#ECFDF5" opacity="0.5" />
      </g>

      <g class="mascot-body" :style="bodyStyle">
        <g class="mascot-ear ear-left" :style="leftEarStyle">
          <circle cx="88.67" cy="135.33" r="28" fill="#F1F5F9" stroke="#CBD5E1" stroke-width="1.87" />
          <circle cx="88.67" cy="135.33" r="18.8" fill="#FFE4E6" opacity="0.5" />
        </g>
        <g class="mascot-ear ear-right" :style="rightEarStyle">
          <circle cx="191.33" cy="135.33" r="28" fill="#F1F5F9" stroke="#CBD5E1" stroke-width="1.87" />
          <circle cx="191.33" cy="135.33" r="18.8" fill="#FFE4E6" opacity="0.5" />
        </g>

        <ellipse
          cx="140"
          cy="144.67"
          rx="46.67"
          ry="46.67"
          fill="url(#mascot-face)"
          stroke="#CBD5E1"
          stroke-width="1.87"
        />

        <path
          v-if="happy && !passwordVisible"
          class="mascot-happy-eye eye-left-happy"
          d="M116.8 140.4C118.4 136.6 119.8 135.4 121.33 135.4C122.86 135.4 124.26 136.6 125.86 140.4"
          stroke="#334155"
          stroke-width="2.4"
          stroke-linecap="round"
          fill="none"
        />
        <ellipse
          v-else
          class="mascot-eye eye-left"
          :cx="121.33 + eyeX"
          :cy="140 + eyeY"
          rx="3.2"
          :ry="eyeRy"
          fill="#334155"
        />
        <path
          v-if="happy && !passwordVisible"
          class="mascot-happy-eye eye-right-happy"
          d="M154.14 140.4C155.74 136.6 157.14 135.4 158.67 135.4C160.2 135.4 161.6 136.6 163.2 140.4"
          stroke="#334155"
          stroke-width="2.4"
          stroke-linecap="round"
          fill="none"
        />
        <ellipse
          v-else
          class="mascot-eye eye-right"
          :cx="158.67 + eyeX"
          :cy="140 + eyeY"
          rx="3.2"
          :ry="eyeRy"
          fill="#334155"
        />
        <circle v-if="!happy" :cx="120.4 + eyeX" :cy="139 + eyeY" r="1" fill="#FFFFFF" opacity="0.75" />
        <circle v-if="!happy" :cx="157.7 + eyeX" :cy="139 + eyeY" r="1" fill="#FFFFFF" opacity="0.75" />

        <ellipse cx="112" cy="149.33" rx="5.6" ry="3.73" fill="#FF9A9A" opacity="0.4" />
        <ellipse cx="168" cy="149.33" rx="5.6" ry="3.73" fill="#FF9A9A" opacity="0.4" />

        <path
          v-if="!passwordVisible"
          class="mascot-mouth"
          d="M140 149.33C140 164.89 144.67 169.56 154 163.33"
          stroke="#CBD5E1"
          stroke-width="4.67"
          stroke-linecap="round"
        />
        <ellipse
          v-else
          class="mascot-shy-mouth"
          cx="146"
          cy="159"
          rx="4.6"
          ry="5.6"
          fill="#FFB3B3"
        />

        <g class="mascot-melon" :style="melonStyle">
          <path
            d="M56 182C112 244.22 168 244.22 224 182"
            stroke="#68D391"
            stroke-width="11.2"
            stroke-linecap="round"
          />
          <path d="M61.6 182C113.87 234.89 166.13 234.89 218.4 182H61.6Z" fill="url(#mascot-melon)" />
          <circle cx="102.67" cy="200.67" r="2.8" fill="#333333" opacity="0.2" />
          <circle cx="140" cy="219.33" r="2.8" fill="#333333" opacity="0.2" />
          <circle cx="177.33" cy="200.67" r="2.8" fill="#333333" opacity="0.2" />
          <circle cx="121.33" cy="210" r="1.9" fill="#333333" opacity="0.2" />
          <circle cx="158.67" cy="210" r="1.9" fill="#333333" opacity="0.2" />
        </g>

        <g class="mascot-paws" :style="pawStyle">
          <circle cx="107.33" cy="182" r="11.2" fill="#F1F5F9" stroke="#CBD5E1" stroke-width="1.87" />
          <circle cx="172.67" cy="182" r="11.2" fill="#F1F5F9" stroke="#CBD5E1" stroke-width="1.87" />
        </g>

        <g class="mascot-sparkles" :style="sparkleStyle">
          <path d="M49.4 76.76L53.58 56.32L67.43 71.93L49.4 76.76Z" fill="#FFD700" opacity="0.2" />
          <circle cx="224" cy="84" r="9.33" fill="#FF5252" opacity="0.1" />
          <path
            d="M189.18 166.64L181.07 164.47C179.58 164.07 178.04 164.95 177.64 166.45L175.47 174.56C175.07 176.06 175.95 177.59 177.45 177.99L185.56 180.17C187.05 180.57 188.59 179.68 188.99 178.19L191.16 170.07C191.56 168.58 190.68 167.04 189.18 166.64Z"
            fill="#9AE6B4"
            opacity="0.2"
          />
        </g>
      </g>

      <defs>
        <linearGradient id="mascot-face" x1="93.33" y1="98" x2="93.33" y2="191.33" gradientUnits="userSpaceOnUse">
          <stop stop-color="#F8FAFC" />
          <stop offset="1" stop-color="#E2E8F0" />
        </linearGradient>
        <linearGradient id="mascot-melon" x1="61.6" y1="182" x2="61.6" y2="221.67" gradientUnits="userSpaceOnUse">
          <stop stop-color="#FF9A9A" />
          <stop offset="1" stop-color="#FF7EB3" />
        </linearGradient>
      </defs>
    </svg>
  </div>
</template>

<script>
const clamp = (value, min, max) => Math.min(max, Math.max(min, value));

export default {
  name: "InteractiveMascot",
  props: {
    activeField: {
      type: String,
      default: "",
    },
    passwordVisible: {
      type: Boolean,
      default: false,
    },
  },
  data() {
    return {
      hovered: false,
      happy: false,
      blink: false,
      pointerX: 0,
      pointerY: 0,
      bobPhase: 0,
      bobTimer: null,
      blinkTimer: null,
      happyTimer: null,
    };
  },
  computed: {
    eyeX() {
      if (this.passwordVisible) return 0;
      if (this.happy) return 0;
      let x = clamp(this.pointerX / 34, -3.4, 3.4);
      if (this.activeField && !this.hovered) x = Math.max(x, 2.2);
      return x;
    },
    eyeY() {
      if (this.passwordVisible) return 0;
      if (this.happy) return -0.5;
      let y = clamp(this.pointerY / 40, -2.4, 2.4);
      if (this.activeField && !this.hovered) y = Math.max(y, 0.8);
      return y;
    },
    eyeRy() {
      if (this.passwordVisible) return 0.7;
      return this.blink ? 0.7 : 3.2;
    },
    tilt() {
      if (this.passwordVisible) return 0;
      if (this.happy) return 0;
      let angle = clamp(this.pointerX / 26, -6, 6);
      if (this.hovered) angle *= 1.25;
      return angle;
    },
    bodyStyle() {
      return {
        transform: `rotate(${this.tilt}deg) translateY(${this.bobOffset}px)`,
      };
    },
    bobOffset() {
      if (this.passwordVisible) return 0;
      return Math.sin(this.bobPhase) * 2.2;
    },
    melonStyle() {
      const offset = this.passwordVisible ? 0 : Math.sin(this.bobPhase + Math.PI) * 1.4;
      return {
        transform: `translateY(${offset}px)`,
      };
    },
    pawStyle() {
      const offset = this.passwordVisible ? 0 : Math.sin(this.bobPhase + Math.PI) * 1.4;
      return {
        transform: `translateY(${offset}px)`,
      };
    },
    leftEarStyle() {
      let angle = this.hovered ? -3 : -1.2;
      if (this.activeField && !this.passwordVisible) angle -= 1.5;
      if (this.happy) angle -= 4;
      return { transform: `rotate(${angle}deg)` };
    },
    rightEarStyle() {
      let angle = this.hovered ? 3 : 1.2;
      if (this.activeField && !this.passwordVisible) angle += 1.5;
      if (this.happy) angle += 4;
      return { transform: `rotate(${angle}deg)` };
    },
    sparkleStyle() {
      const rotate = this.happy ? 18 : Math.sin(this.bobPhase * 0.7) * 4;
      return { transform: `rotate(${rotate}deg) scale(${this.happy ? 1.06 : 1})` };
    },
  },
  watch: {
    passwordVisible(visible) {
      if (visible) this.blink = false;
    },
  },
  mounted() {
    window.addEventListener("mousemove", this.onPointerMove, { passive: true });
    this.bobTimer = window.setInterval(() => {
      this.bobPhase += 0.075;
    }, 60);
    this.scheduleBlink();
  },
  beforeUnmount() {
    window.removeEventListener("mousemove", this.onPointerMove);
    if (this.bobTimer) clearInterval(this.bobTimer);
    if (this.blinkTimer) clearTimeout(this.blinkTimer);
    if (this.happyTimer) clearTimeout(this.happyTimer);
  },
  methods: {
    onPointerMove(event) {
      if (!this.$el) return;
      const rect = this.$el.getBoundingClientRect();
      if (!rect.width || !rect.height) return;
      const centerX = rect.left + rect.width / 2;
      const centerY = rect.top + rect.height / 2;
      const dx = clamp((event.clientX - centerX) / rect.width, -1.6, 1.6);
      const dy = clamp((event.clientY - centerY) / rect.height, -1.6, 1.6);
      this.pointerX = dx * 100;
      this.pointerY = dy * 100;
    },
    scheduleBlink() {
      if (this.blinkTimer) clearTimeout(this.blinkTimer);
      const delay = 3600 + Math.random() * 3600;
      this.blinkTimer = setTimeout(() => {
        if (!this.passwordVisible) {
          this.blink = true;
          setTimeout(() => {
            this.blink = false;
          }, 150);
        }
        this.scheduleBlink();
      }, delay);
    },
    cheer() {
      if (this.happy) return;
      this.happy = true;
      if (this.happyTimer) clearTimeout(this.happyTimer);
      this.happyTimer = setTimeout(() => {
        this.happy = false;
      }, 1100);
    },
  },
};
</script>

<style scoped>
.mascot {
  width: 100%;
  height: 100%;
  cursor: pointer;
  border-radius: 50%;
  -webkit-tap-highlight-color: transparent;
  user-select: none;
}

.mascot:focus-visible {
  outline: 2px solid rgba(0, 122, 255, 0.55);
  outline-offset: 6px;
}

.mascot-svg {
  width: 100%;
  height: 100%;
  display: block;
  overflow: visible;
  transform-origin: 50% 62%;
  transition: transform 0.35s cubic-bezier(0.22, 0.61, 0.36, 1);
}

.mascot.is-hovered .mascot-svg {
  transform: scale(1.035);
}

.mascot-body,
.mascot-ear,
.mascot-melon,
.mascot-paws,
.mascot-sparkles {
  transform-box: fill-box;
  transform-origin: center;
}

.mascot-body {
  transform-origin: 50% 58%;
  transition: transform 0.5s cubic-bezier(0.22, 0.61, 0.36, 1);
}

.mascot-ear {
  transform-origin: 50% 82%;
  transition: transform 0.45s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.mascot-eye {
  transition: cx 0.18s ease-out, cy 0.18s ease-out, ry 0.12s ease-out;
}

.mascot-mouth,
.mascot-shy-mouth {
  transition: opacity 0.16s ease;
}

.mascot-happy-eye {
  transition: opacity 0.16s ease;
}

.mascot.is-happy .mascot-sparkles path,
.mascot.is-happy .mascot-sparkles circle {
  opacity: 0.55;
}

.mascot-melon,
.mascot-paws {
  transition: transform 0.6s cubic-bezier(0.22, 0.61, 0.36, 1);
}

.mascot-sparkles {
  transform-origin: 140px 140px;
  transition: transform 0.7s cubic-bezier(0.22, 0.61, 0.36, 1);
}

.mascot.is-happy .mascot-body {
  animation: mascotCheer 0.55s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.mascot.is-happy .mascot-melon,
.mascot.is-happy .mascot-paws {
  animation: melonCheer 0.55s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.mascot.is-shy .mascot-ear {
  filter: saturate(0.9);
}

@keyframes mascotCheer {
  0%,
  100% {
    transform: rotate(0) translateY(0);
  }
  32% {
    transform: rotate(-3.5deg) translateY(-9px) scale(1.04);
  }
  68% {
    transform: rotate(3.5deg) translateY(-3px) scale(1.02);
  }
}

@keyframes melonCheer {
  0%,
  100% {
    transform: translateY(0) rotate(0);
  }
  45% {
    transform: translateY(5px) rotate(-3deg);
  }
  75% {
    transform: translateY(-1px) rotate(2deg);
  }
}

@media (prefers-reduced-motion: reduce) {
  .mascot-body,
  .mascot-svg,
  .mascot-ear,
  .mascot-melon,
  .mascot-paws,
  .mascot-sparkles,
  .mascot-eye,
  .mascot-mouth,
  .mascot-shy-mouth {
    transition: none;
    animation: none !important;
  }
}
</style>
