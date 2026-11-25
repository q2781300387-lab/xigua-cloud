<template>
  <transition name="slide-up">
    <div v-if="visible && caller" class="call-toast">
      <div class="avatar">
        <img v-if="caller.avatar" :src="caller.avatar" />
        <span v-else>{{ caller.name ? caller.name.charAt(0).toUpperCase() : '📹' }}</span>
      </div>
      <div class="text">
        <div class="name">{{ caller.name || '好友' }}</div>
        <div class="desc">邀请你视频通话</div>
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
import { useVideoCallStore } from '@/store/videoCallStore'
import { getWebSocketClient } from '@/ws/WebSocketClient'
import {getObject} from "@/utils/localStorage.js";
import {useVideoWindowStore} from "@/store/videoWindow.js";

const store = useVideoCallStore()
const { visible, caller } = storeToRefs(store)
const videoStore = useVideoWindowStore()

let autoHangupTimer = null

function startAutoHangup() {
  clearTimeout(autoHangupTimer)
  autoHangupTimer = setTimeout(() => {
    const callerInfo = caller.value
    if (callerInfo) {
      // 被叫方先超时也要通知呼叫方，呼叫方不用再等满自己的 30 秒计时器。
      wsClient.sendMessage(JSON.stringify({
        messageType: 'video_call',
        subType: 'video_call_timeout',
        senderId: getObject('userInfo').id,
        receiverId: callerInfo.userId,
        callId: callerInfo.callId,
        timestamp: Date.now()
      }))
    }
    store.hide()
  }, 30 * 1000)
}

function clearAutoHangup() {
  clearTimeout(autoHangupTimer)
  autoHangupTimer = null
}

function handleVisibilityChange() {
  if (document.hidden && visible.value) {
    console.log('页面进入后台，保持视频来电状态')
  }
}

document.addEventListener('visibilitychange', handleVisibilityChange)

watch(visible, (val) => {
  if (val) {
    startAutoHangup()
  } else {
    clearAutoHangup()
  }
})

const wsClient = getWebSocketClient()

const wsListener = (raw) => {
  try {
    const msg = JSON.parse(raw)

    if (msg.messageType === 'video_call' && msg.subType === 'video_call_incoming') {
      // 已有通话窗口时不能再弹第二个来电，否则接听会顶掉正在进行的通话。
      if (videoStore.showVideoWindow && videoStore.caller && String(videoStore.caller.callId) !== String(msg.callId)) {
        wsClient.sendMessage(JSON.stringify({
          messageType: 'video_call',
          subType: 'video_call_rejected',
          senderId: getObject('userInfo').id,
          receiverId: msg.senderId,
          callId: msg.callId,
          message: 'busy',
          timestamp: Date.now()
        }))
        return
      }
      const caller = {
        callId: msg.callId,
        userId: msg.senderId,
        name: msg.sender?.username || '好友',
        avatar: msg.sender?.avatar || '',
      };
      store.showIncomingCall(caller);
    }
    if (msg.messageType === 'video_call' && msg.subType === 'video_call_canceled') {
      store.hide()
    }
    if (msg.messageType === 'video_call' && msg.subType === 'video_call_timeout') {
      if (!caller.value || String(caller.value.callId) === String(msg.callId)) {
        store.hide()
      }
    }
  } catch (e) {
    console.error('WebSocket 消息解析失败', raw, e)
  }
}

wsClient.subscribe(wsListener)

function reject() {
  const callerInfo = caller.value
  if (!callerInfo) return
  const message = {
    messageType: 'video_call',
    subType: 'video_call_rejected',
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

  videoStore.openVideoWindow('incoming', callerInfo);

  const message = {
    messageType: 'video_call',
    subType: 'video_call_accepted',
    senderId: getObject('userInfo').id,
    receiverId: callerInfo.userId,
    callId: callerInfo.callId,
    timestamp: Date.now()
  };
  wsClient.sendMessage(JSON.stringify(message));

  store.hide()
}

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
  background: linear-gradient(135deg, #f48fb1 0%, #ec407a 100%);
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
  background: #ec407a;
}

.btn-icon {
  width: 16px;
  height: 16px;
  transition: transform 0.2s ease;
  filter: invert(1);
}

@media (max-width: 480px) {
  .call-toast {
    left: 16px;
    right: 16px;
    transform: none !important;
    width: auto;
    min-width: calc(100% - 32px);
    box-sizing: border-box;
    opacity: 1 !important;
  }

  .slide-up-enter-from,
  .slide-up-leave-to {
    transform: none !important;
    opacity: 1 !important;
  }
}

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
