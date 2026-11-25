<template>
  <transition name="slide-up">
    <div v-if="visible && caller" class="call-toast">
      <div class="avatar">
        <img v-if="caller.avatar" :src="caller.avatar" />
        <span v-else>{{ caller.name ? caller.name.charAt(0).toUpperCase() : '🎧' }}</span>
      </div>
      <div class="text">
        <div class="name">{{ caller.name || '好友' }}</div>
        <div class="desc">邀请你语音通话</div>
      </div>
      <div class="actions">
        <button class="reject" @click="reject">
          <img src="/src/static/icons/tel.png" alt="拒绝" class="btn-icon" />
        </button>
        <button class="accept" @click="accept">
          <img src="/src/static/icons/tel.png" alt="接听" class="btn-icon" />
        </button>
      </div>
    </div>
  </transition>
</template>

<script setup>
import { storeToRefs } from 'pinia'
import { watch, onUnmounted } from 'vue'
import { useVoiceCallStore } from '@/store/voiceCallStore'
import { getWebSocketClient } from '@/ws/WebSocketClient'
import {getObject} from "@/utils/localStorage.js";
import {useVoiceWindowStore} from "@/store/voiceWindow.js";

/* =========================
   Pinia 全局来电状态
========================= */
const store = useVoiceCallStore()
const { visible, caller } = storeToRefs(store)


/* =========================
   ⏱ 10 秒未接自动挂断
========================= */
let autoHangupTimer = null

function startAutoHangup() {
  clearTimeout(autoHangupTimer)
  autoHangupTimer = setTimeout(() => {
    console.log('来电超时，自动挂断')
    notifyCallTimeout()
    store.hide()
  }, 10 * 1000)
}

// 10 秒未接听：通知服务端，主叫方会收到「对方超时未接听」
function notifyCallTimeout() {
  const callerInfo = caller.value
  if (!callerInfo || !callerInfo.userId) {
    return
  }
  const currentUserId = (getObject('userInfo') || {}).id
  if (!currentUserId) {
    return
  }
  try {
    wsClient.sendMessage(JSON.stringify({
      messageType: 'audio_call',
      subType: 'audio_call_timeout',
      senderId: currentUserId,
      receiverId: callerInfo.userId,
      callId: callerInfo.callId,
      timestamp: Date.now()
    }))
  } catch (e) {
    console.warn('发送未接听通知失败', e)
  }
}

function clearAutoHangup() {
  clearTimeout(autoHangupTimer)
  autoHangupTimer = null
}

/* =========================
   📱 移动端切后台兜底
========================= */
function handleVisibilityChange() {
  if (document.hidden && visible.value) {
    console.log('页面进入后台，保持来电状态')
  }
}

document.addEventListener('visibilitychange', handleVisibilityChange)

/* =========================
   👀 监听来电显示状态
========================= */
watch(visible, (val) => {
  if (val) {
    startAutoHangup()
  } else {
    clearAutoHangup()
  }
})

/* =========================
   🌐 WebSocket 订阅（只接收）
========================= */
const wsClient = getWebSocketClient()

const wsListener = (raw) => {
  try {
    const msg = JSON.parse(raw)

    // 显示来电
    if (msg.messageType === 'audio_call' && msg.subType === 'audio_call_incoming') {
      const caller = {
        callId: msg.callId,
        userId: msg.senderId,
        name: msg.sender.username,
        avatar: msg.sender.avatar,
      };
      store.showIncomingCall(caller);
    }
    // 对方取消来电
    if (msg.messageType === 'audio_call' && msg.subType === 'audio_call_canceled') {
      store.hide()
    }
  } catch (e) {
    console.error('WebSocket 消息解析失败', raw, e)
  }
}

wsClient.subscribe(wsListener)

/* =========================
   按钮事件
========================= */
function reject() {
  const callerInfo = caller.value
  if (!callerInfo) return
  const message = {
    messageType: 'audio_call',
    subType: 'audio_call_rejected',
    senderId: getObject('userInfo').id,
    receiverId: callerInfo.userId,
    callId: callerInfo.callId,
    timestamp: Date.now()
  };
  wsClient.sendMessage(JSON.stringify(message));
  store.hide()
}

function accept() {
  const callerInfo = caller.value
  if (!callerInfo) return

  // 打开 VoiceChatWindow
  const voiceStore = useVoiceWindowStore();
  voiceStore.openVoiceWindow('incoming',callerInfo);

  const message = {
    messageType: 'audio_call',
    subType: 'audio_call_accepted',
    senderId: getObject('userInfo').id,
    receiverId: callerInfo.userId,
    callId: callerInfo.callId,
    timestamp: Date.now()
  };
  wsClient.sendMessage(JSON.stringify(message));

  store.hide()
}

/* =========================
   清理
========================= */
onUnmounted(() => {
  clearAutoHangup()
  wsClient.unsubscribe(wsListener)
  document.removeEventListener('visibilitychange', handleVisibilityChange)
})
</script>

<style scoped>
.call-toast {
  position: fixed;
  left: 50%;
  transform: translateX(-50%);
  top: 20px;
  width: 300px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  padding: 6px 10px;
  z-index: 9999;
  border: 1px solid var(--separator);
  display: flex;
  align-items: center;
  gap: 8px;
}

.avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: linear-gradient(135deg, #90caf9 0%, #64b5f6 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  font-size: 14px;
  font-weight: 600;
  color: white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  flex-shrink: 0;
}

.avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.text {
  display: flex;
  flex-direction: column;
  justify-content: center;
  flex: 1;
}

.text .name {
  font-weight: 600;
  font-size: 14px;
  color: #333;
}

.text .desc {
  font-size: 12px;
  color: #666;
  margin-top: 2px;
}

.actions {
  display: flex;
  gap: 8px;
}

button {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: none;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
}

button:hover {
  transform: scale(1.05);
}

.reject {
  background: #ff8a80;
}

.reject .btn-icon {
  transform: rotate(135deg);
}

.accept {
  background: #64b5f6;
}

.btn-icon {
  width: 16px;
  height: 16px;
  transition: transform 0.2s ease;
  filter: invert(1);
}

/* 📱 移动端适配 */
@media (max-width: 480px) {
  .call-toast {
    left: 16px;
    right: 16px;
    transform: none;
    width: auto;
    min-width: calc(100% - 32px);
  }
}

/* 🎬 动画 */
.slide-up-enter-active,
.slide-up-leave-active {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.slide-up-enter-from {
  transform: translateX(-50%) translateY(-20px);
  opacity: 0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.slide-up-leave-to {
  transform: translateX(-50%) translateY(-20px);
  opacity: 0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}
</style>
