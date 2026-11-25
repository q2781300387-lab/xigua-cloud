<template>
  <div ref="root" class="lang-switch">
    <button
      ref="trigger"
      type="button"
      class="lang-btn"
      :class="{ open }"
      :aria-label="`${copy.common.switchLanguage}: ${current.label}`"
      :aria-expanded="open ? 'true' : 'false'"
      aria-haspopup="menu"
      @click="toggle"
    >
      <img :src="langIcon" alt="" />
      <span>{{ current.label }}</span>
      <svg class="lang-chevron" viewBox="0 0 12 12" aria-hidden="true">
        <path
          d="M3 4.5 6 7.5 9 4.5"
          fill="none"
          stroke="currentColor"
          stroke-width="1.4"
          stroke-linecap="round"
          stroke-linejoin="round"
        />
      </svg>
    </button>

    <Transition name="lang-menu">
      <div v-if="open" ref="menu" class="lang-menu" role="menu">
        <button
          v-for="language in languages"
          :key="language.value"
          type="button"
          class="lang-option"
          :class="{ active: language.value === locale }"
          role="menuitemradio"
          :aria-checked="language.value === locale ? 'true' : 'false'"
          @click="choose(language)"
        >
          <span>{{ language.label }}</span>
          <svg v-if="language.value === locale" viewBox="0 0 16 16" aria-hidden="true">
            <path
              d="M3 8.4 6.3 11.7 13 4.8"
              fill="none"
              stroke="currentColor"
              stroke-width="1.8"
              stroke-linecap="round"
              stroke-linejoin="round"
            />
          </svg>
        </button>
      </div>
    </Transition>
  </div>
</template>

<script>
import { computed, ref, onMounted, onBeforeUnmount, nextTick } from "vue";
import langIcon from "@/static/lang.svg";
import {
  AUTH_LANGUAGES,
  authLocale,
  getAuthCopy,
  setAuthLocale,
  syncAuthLanguageTag,
} from "@/locales/auth.js";

export default {
  name: "AuthLanguageSwitch",
  emits: ["change"],
  setup() {
    const open = ref(false);
    const root = ref(null);
    const trigger = ref(null);
    const menu = ref(null);

    const copy = computed(() => getAuthCopy(authLocale.value));
    const current = computed(
      () =>
        AUTH_LANGUAGES.find((item) => item.value === authLocale.value) ||
        AUTH_LANGUAGES[0]
    );

    function close() {
      open.value = false;
    }

    function toggle() {
      open.value = !open.value;
    }

    function choose(language) {
      setAuthLocale(language.value);
      open.value = false;
      nextTick(() => {
        if (trigger.value) trigger.value.focus();
      });
    }

    function onPointerDown(event) {
      if (!open.value) return;
      if (root.value && !root.value.contains(event.target)) close();
    }

    function onKeydown(event) {
      if (event.key !== "Escape" || !open.value) return;
      close();
      nextTick(() => {
        if (trigger.value) trigger.value.focus();
      });
    }

    onMounted(() => {
      syncAuthLanguageTag();
      document.addEventListener("pointerdown", onPointerDown, true);
      document.addEventListener("keydown", onKeydown);
    });

    onBeforeUnmount(() => {
      document.removeEventListener("pointerdown", onPointerDown, true);
      document.removeEventListener("keydown", onKeydown);
    });

    return {
      open,
      root,
      trigger,
      menu,
      copy,
      current,
      languages: AUTH_LANGUAGES,
      locale: authLocale,
      toggle,
      choose,
    };
  },
};
</script>

<style scoped>
.lang-switch {
  position: relative;
  grid-column: 3;
  justify-self: end;
  z-index: 12;
}

.lang-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  height: 36px;
  padding: 0 12px 0 14px;
  border-radius: 9999px;
  border: 1px solid rgba(15, 23, 42, 0.1);
  background: #ffffff;
  font-family: inherit;
  font-size: 12px;
  font-weight: 500;
  color: var(--label);
  cursor: pointer;
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.96),
    0 1px 2px rgba(15, 23, 42, 0.05),
    0 5px 14px rgba(15, 23, 42, 0.07);
  transition:
    background 0.16s ease,
    border-color 0.16s ease,
    box-shadow 0.16s ease,
    transform 0.16s ease;
}

.lang-btn:hover {
  border-color: rgba(15, 23, 42, 0.16);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.96),
    0 2px 4px rgba(15, 23, 42, 0.06),
    0 8px 20px rgba(15, 23, 42, 0.09);
}

.lang-btn:active,
.lang-btn.open {
  transform: translateY(1px);
  box-shadow:
    inset 0 1px 2px rgba(15, 23, 42, 0.08),
    0 2px 6px rgba(15, 23, 42, 0.06);
}

.lang-btn:focus-visible {
  outline: 2px solid rgba(0, 122, 255, 0.5);
  outline-offset: 2px;
}

.lang-btn img {
  width: 14px;
  height: 14px;
  opacity: 0.8;
  flex: 0 0 auto;
}

.lang-chevron {
  width: 11px;
  height: 11px;
  flex: 0 0 auto;
  opacity: 0.55;
  transition: transform 0.18s ease;
}

.lang-btn.open .lang-chevron {
  transform: rotate(180deg);
}

.lang-menu {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  width: 164px;
  padding: 6px;
  border-radius: 14px;
  border: 1px solid rgba(15, 23, 42, 0.1);
  background: #ffffff;
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.9),
    0 2px 6px rgba(15, 23, 42, 0.06),
    0 14px 36px rgba(15, 23, 42, 0.14);
}

.lang-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  height: 38px;
  padding: 0 11px;
  border: 0;
  border-radius: 9px;
  background: transparent;
  font-family: inherit;
  font-size: 13px;
  color: var(--label);
  text-align: left;
  cursor: pointer;
  transition:
    background 0.14s ease,
    color 0.14s ease;
}

.lang-option:hover {
  background: rgba(0, 122, 255, 0.07);
}

.lang-option.active {
  background: rgba(0, 122, 255, 0.1);
  color: #007aff;
  font-weight: 600;
}

.lang-option:focus-visible {
  outline: 2px solid rgba(0, 122, 255, 0.45);
  outline-offset: -1px;
}

.lang-option svg {
  width: 15px;
  height: 15px;
  flex: 0 0 auto;
}

.lang-menu-enter-active,
.lang-menu-leave-active {
  transition:
    opacity 0.16s ease,
    transform 0.16s ease;
  transform-origin: top right;
}

.lang-menu-enter-from,
.lang-menu-leave-to {
  opacity: 0;
  transform: translateY(-4px) scale(0.98);
}

@media (prefers-reduced-motion: reduce) {
  .lang-btn,
  .lang-chevron,
  .lang-option,
  .lang-menu-enter-active,
  .lang-menu-leave-active {
    transition: none;
  }
}

html[data-theme="dark"] .lang-btn {
  background: #303033;
  border-color: rgba(255, 255, 255, 0.16);
  color: #f5f5f7;
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.08),
    0 1px 2px rgba(0, 0, 0, 0.35),
    0 6px 16px rgba(0, 0, 0, 0.24);
}

html[data-theme="dark"] .lang-btn:hover {
  border-color: rgba(255, 255, 255, 0.26);
}

html[data-theme="dark"] .lang-btn img {
  opacity: 0.72;
  filter: invert(1);
}

html[data-theme="dark"] .lang-menu {
  background: #2c2c2e;
  border-color: rgba(255, 255, 255, 0.14);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.06),
    0 2px 6px rgba(0, 0, 0, 0.35),
    0 16px 40px rgba(0, 0, 0, 0.42);
}

html[data-theme="dark"] .lang-option {
  color: #f5f5f7;
}

html[data-theme="dark"] .lang-option:hover {
  background: rgba(10, 132, 255, 0.16);
}

html[data-theme="dark"] .lang-option.active {
  background: rgba(10, 132, 255, 0.2);
  color: #64b5ff;
}
</style>
