<template>
  <div ref="stage" class="characters" aria-hidden="true">
    <!-- 紫色高个子：最后排 -->
    <div ref="purple" class="character purple" :style="purpleStyle">
      <div class="eyes purple-eyes" :style="purpleEyesStyle">
        <div
          v-for="eye in 2"
          :key="'purple-' + eye"
          class="eye white-eye purple-eye"
          :class="{ 'is-blinking': purpleBlinking }"
        >
          <span
            v-if="!purpleBlinking"
            class="pupil purple-pupil"
            :style="pupilTransform(purpleLook)"
          ></span>
        </div>
      </div>
    </div>

    <!-- 黑色高个子：中间层 -->
    <div ref="black" class="character black" :style="blackStyle">
      <div class="eyes black-eyes" :style="blackEyesStyle">
        <div
          v-for="eye in 2"
          :key="'black-' + eye"
          class="eye white-eye black-eye"
          :class="{ 'is-blinking': blackBlinking }"
        >
          <span
            v-if="!blackBlinking"
            class="pupil black-pupil"
            :style="pupilTransform(blackLook)"
          ></span>
        </div>
      </div>
    </div>

    <!-- 橙色半圆：左前方 -->
    <div ref="orange" class="character orange" :style="orangeStyle">
      <div class="eyes orange-eyes" :style="orangeEyesStyle">
        <span
          v-for="eye in 2"
          :key="'orange-' + eye"
          class="pupil bare-pupil"
          :style="pupilTransform(orangeLook)"
        ></span>
      </div>
    </div>

    <!-- 黄色高个子：右前方 -->
    <div ref="yellow" class="character yellow" :style="yellowStyle">
      <div class="eyes yellow-eyes" :style="yellowEyesStyle">
        <span
          v-for="eye in 2"
          :key="'yellow-' + eye"
          class="pupil bare-pupil"
          :style="pupilTransform(yellowLook)"
        ></span>
      </div>
      <div class="mouth" :style="yellowMouthStyle"></div>
    </div>
  </div>
</template>

<script>
const clamp = (value, min, max) => Math.min(max, Math.max(min, value));

export default {
  name: "AnimatedLoginCharacters",
  props: {
    isTyping: { type: Boolean, default: false },
    showPassword: { type: Boolean, default: false },
    passwordLength: { type: Number, default: 0 },
  },
  data() {
    return {
      mouseX: 0,
      mouseY: 0,
      resizeTick: 0,
      purpleBlinking: false,
      blackBlinking: false,
      isLookingAtEachOther: false,
      isPurplePeeking: false,
      reduceMotion: false,
      purpleBlinkTimer: null,
      purpleBlinkOffTimer: null,
      blackBlinkTimer: null,
      blackBlinkOffTimer: null,
      lookTimer: null,
      peekTimer: null,
      peekOffTimer: null,
    };
  },
  computed: {
    passwordVisible() {
      return this.showPassword && this.passwordLength > 0;
    },
    isHidingPassword() {
      return !this.showPassword && this.passwordLength > 0;
    },
    purpleMetrics() {
      return this.metrics("purple");
    },
    blackMetrics() {
      return this.metrics("black");
    },
    orangeMetrics() {
      return this.metrics("orange");
    },
    yellowMetrics() {
      return this.metrics("yellow");
    },
    purpleLook() {
      if (this.passwordVisible) {
        return this.isPurplePeeking ? { x: 4, y: 5 } : { x: -4, y: -4 };
      }
      if (this.isLookingAtEachOther) return { x: 3, y: 4 };
      return this.defaultLook(this.purpleMetrics);
    },
    blackLook() {
      if (this.passwordVisible) return { x: -4, y: -4 };
      if (this.isLookingAtEachOther) return { x: 0, y: -4 };
      return this.defaultLook(this.blackMetrics);
    },
    orangeLook() {
      if (this.passwordVisible) return { x: -5, y: -4 };
      return this.defaultLook(this.orangeMetrics);
    },
    yellowLook() {
      if (this.passwordVisible) return { x: -5, y: -4 };
      return this.defaultLook(this.yellowMetrics);
    },
    purpleStyle() {
      const m = this.purpleMetrics;
      let transform = `skewX(${m.bodySkew}deg)`;
      if (this.passwordVisible) {
        transform = "skewX(0deg)";
      } else if (this.isTyping || this.isHidingPassword) {
        transform = `skewX(${m.bodySkew - 8}deg) translateX(28px)`;
      }
      return {
        height: (this.isTyping || this.isHidingPassword) ? "440px" : "400px",
        transform,
      };
    },
    blackStyle() {
      const m = this.blackMetrics;
      let transform = `skewX(${m.bodySkew}deg)`;
      if (this.passwordVisible) {
        transform = "skewX(0deg)";
      } else if (this.isLookingAtEachOther) {
        transform = `skewX(${m.bodySkew * 1.5 + 6}deg) translateX(14px)`;
      } else if (this.isTyping || this.isHidingPassword) {
        transform = `skewX(${m.bodySkew * 1.5}deg)`;
      }
      return { transform };
    },
    orangeStyle() {
      return {
        transform: this.passwordVisible ? "skewX(0deg)" : `skewX(${this.orangeMetrics.bodySkew}deg)`,
      };
    },
    yellowStyle() {
      return {
        transform: this.passwordVisible ? "skewX(0deg)" : `skewX(${this.yellowMetrics.bodySkew}deg)`,
      };
    },
    purpleEyesStyle() {
      let left = 45 + this.purpleMetrics.faceX;
      let top = 40 + this.purpleMetrics.faceY;
      if (this.passwordVisible) {
        left = 20;
        top = 35;
      } else if (this.isLookingAtEachOther) {
        left = 55;
        top = 65;
      }
      return { left: `${left}px`, top: `${top}px` };
    },
    blackEyesStyle() {
      let left = 26 + this.blackMetrics.faceX;
      let top = 32 + this.blackMetrics.faceY;
      if (this.passwordVisible) {
        left = 10;
        top = 28;
      } else if (this.isLookingAtEachOther) {
        left = 32;
        top = 12;
      }
      return { left: `${left}px`, top: `${top}px` };
    },
    orangeEyesStyle() {
      const left = this.passwordVisible ? 50 : 82 + this.orangeMetrics.faceX;
      const top = this.passwordVisible ? 128 : 130 + this.orangeMetrics.faceY;
      return { left: `${left}px`, top: `${top}px` };
    },
    yellowEyesStyle() {
      const left = this.passwordVisible ? 20 : 52 + this.yellowMetrics.faceX;
      const top = this.passwordVisible ? 35 : 40 + this.yellowMetrics.faceY;
      return { left: `${left}px`, top: `${top}px` };
    },
    yellowMouthStyle() {
      const left = this.passwordVisible ? 10 : 40 + this.yellowMetrics.faceX;
      const top = this.passwordVisible ? 88 : 88 + this.yellowMetrics.faceY;
      return { left: `${left}px`, top: `${top}px` };
    },
  },
  watch: {
    isTyping(value) {
      clearTimeout(this.lookTimer);
      if (!value) {
        this.isLookingAtEachOther = false;
        return;
      }
      this.isLookingAtEachOther = true;
      this.lookTimer = setTimeout(() => {
        this.isLookingAtEachOther = false;
      }, 800);
    },
    passwordVisible() {
      this.syncPeeking();
    },
    passwordLength() {
      this.syncPeeking();
    },
  },
  mounted() {
    this.mouseX = window.innerWidth / 2;
    this.mouseY = window.innerHeight / 2;
    this.resizeTick += 1;
    this.reduceMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;

    window.addEventListener("mousemove", this.onMouseMove, { passive: true });
    window.addEventListener("resize", this.onResize, { passive: true });

    if (!this.reduceMotion) {
      this.scheduleBlink("purple");
      this.scheduleBlink("black");
    }
    this.syncPeeking();
  },
  beforeUnmount() {
    window.removeEventListener("mousemove", this.onMouseMove);
    window.removeEventListener("resize", this.onResize);
    clearTimeout(this.purpleBlinkTimer);
    clearTimeout(this.purpleBlinkOffTimer);
    clearTimeout(this.blackBlinkTimer);
    clearTimeout(this.blackBlinkOffTimer);
    clearTimeout(this.lookTimer);
    clearTimeout(this.peekTimer);
    clearTimeout(this.peekOffTimer);
  },
  methods: {
    onMouseMove(event) {
      this.mouseX = event.clientX;
      this.mouseY = event.clientY;
    },
    onResize() {
      // 触发一次重算，让角色在窗口尺寸变化后重新对准鼠标
      this.resizeTick += 1;
    },
    metrics(name) {
      void this.resizeTick;
      const stage = this.$el;
      if (!stage) return { faceX: 0, faceY: 0, bodySkew: 0 };
      const layout = {
        purple: { left: 70, width: 180, height: 400 },
        black: { left: 240, width: 120, height: 310 },
        orange: { left: 0, width: 240, height: 240 },
        yellow: { left: 310, width: 140, height: 230 },
      }[name];
      if (!layout) return { faceX: 0, faceY: 0, bodySkew: 0 };
      // 用未参与角色形变的舞台几何计算中心，避免角色自身 transform 造成反馈抖动
      const rect = stage.getBoundingClientRect();
      const scaleX = rect.width / 550 || 1;
      const scaleY = rect.height / 400 || 1;
      const centerX = rect.left + (layout.left + layout.width / 2) * scaleX;
      const top = 400 - layout.height;
      const centerY = rect.top + (top + layout.height / 3) * scaleY;
      const deltaX = this.mouseX - centerX;
      const deltaY = this.mouseY - centerY;
      return {
        faceX: clamp(deltaX / 20, -15, 15),
        faceY: clamp(deltaY / 30, -10, 10),
        bodySkew: clamp(-deltaX / 120, -4.5, 4.5),
      };
    },
    defaultLook(metrics) {
      return {
        x: clamp(metrics.faceX / 3, -5, 5),
        y: clamp(metrics.faceY / 3, -4, 4),
      };
    },
    pupilTransform(look) {
      return {
        transform: `translate(${look.x}px, ${look.y}px)`,
      };
    },
    scheduleBlink(name) {
      const timerKey = `${name}BlinkTimer`;
      const offKey = `${name}BlinkOffTimer`;
      const blinkingKey = `${name}Blinking`;
      const wait = 3000 + Math.random() * 4000;
      this[timerKey] = setTimeout(() => {
        this[blinkingKey] = true;
        this[offKey] = setTimeout(() => {
          this[blinkingKey] = false;
          if (!this.reduceMotion) this.scheduleBlink(name);
        }, 150);
      }, wait);
    },
    syncPeeking() {
      clearTimeout(this.peekTimer);
      clearTimeout(this.peekOffTimer);
      this.isPurplePeeking = false;
      if (!this.passwordVisible || this.reduceMotion) return;
      this.schedulePeek();
    },
    schedulePeek() {
      this.peekTimer = setTimeout(() => {
        if (!this.passwordVisible) return;
        this.isPurplePeeking = true;
        this.peekOffTimer = setTimeout(() => {
          this.isPurplePeeking = false;
          if (this.passwordVisible && !this.reduceMotion) this.schedulePeek();
        }, 800);
      }, 2000 + Math.random() * 3000);
    },
  },
};
</script>

<style scoped>
.characters {
  position: relative;
  width: 550px;
  height: 400px;
  flex: 0 0 auto;
}

.character {
  position: absolute;
  bottom: 0;
  transform-origin: bottom center;
  transition: transform 0.7s cubic-bezier(0.4, 0, 0.2, 1),
    height 0.7s cubic-bezier(0.4, 0, 0.2, 1);
  will-change: transform;
}

.purple {
  left: 70px;
  width: 180px;
  height: 400px;
  z-index: 1;
  background: #6c3ff5;
  border-radius: 10px 10px 0 0;
}

.black {
  left: 240px;
  width: 120px;
  height: 310px;
  z-index: 2;
  background: #2d2d2d;
  border-radius: 8px 8px 0 0;
}

.orange {
  left: 0;
  width: 240px;
  height: 240px;
  z-index: 3;
  background: #ff9b6b;
  border-radius: 120px 120px 0 0;
}

.yellow {
  left: 310px;
  width: 140px;
  height: 230px;
  z-index: 4;
  background: #e8d754;
  border-radius: 70px 70px 0 0;
}

.eyes {
  position: absolute;
  display: flex;
  transition: left 0.7s cubic-bezier(0.4, 0, 0.2, 1),
    top 0.7s cubic-bezier(0.4, 0, 0.2, 1);
}

.purple-eyes {
  gap: 32px;
}

.black-eyes {
  gap: 24px;
}

.orange-eyes,
.yellow-eyes {
  gap: 32px;
  transition: left 0.2s ease-out, top 0.2s ease-out;
}

.yellow-eyes {
  gap: 24px;
}

.eye {
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border-radius: 999px;
  background: #fff;
  transition: height 0.15s ease;
}

.purple-eye {
  width: 18px;
  height: 18px;
}

.black-eye {
  width: 16px;
  height: 16px;
}

.eye.is-blinking {
  height: 2px;
  border-radius: 2px;
}

.pupil {
  display: block;
  border-radius: 999px;
  background: #2d2d2d;
  transition: transform 0.1s ease-out;
  will-change: transform;
}

.purple-pupil {
  width: 7px;
  height: 7px;
}

.black-pupil {
  width: 6px;
  height: 6px;
}

.bare-pupil {
  width: 12px;
  height: 12px;
  transition: transform 0.1s ease-out;
}

.mouth {
  position: absolute;
  width: 80px;
  height: 4px;
  border-radius: 999px;
  background: #2d2d2d;
  transition: left 0.2s ease-out, top 0.2s ease-out;
}

@media (prefers-reduced-motion: reduce) {
  .character,
  .eyes,
  .eye,
  .pupil,
  .mouth {
    transition: none !important;
  }
}
</style>
