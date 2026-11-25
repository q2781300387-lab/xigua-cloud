<template>
  <router-view />
  <ToastHost :list="toastList" />
  <IncomingCallToast />
  <IncomingVideoCallToast />
  <VoiceChatWindow
      :showVoiceWindow="voiceStore.showVoiceWindow"
      :currentParticipant="voiceStore.currentParticipant"
      @close="voiceStore.closeVoiceWindow()"
  />
  <VideoChatWindow />
</template>
<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import {getCookie} from "@/utils/cookie.js";
import { useVoiceCallStore } from '@/store/voiceCallStore'
import { useVoiceWindowStore } from '@/store/voiceWindow.js'
import {getWebSocketClient} from "@/ws/WebSocketClient.js";
import IncomingCallToast from '@/components/IncomingCallToast.vue'
import ToastHost from '@/components/ToastHost.vue'
import { toastList } from '@/plugins/message'
import IncomingVideoCallToast from '@/components/IncomingVideoCallToast.vue'
import VoiceChatWindow from "@/components/VoiceChatWindow.vue"
import VideoChatWindow from "@/components/VideoChatWindow.vue";

let wsListener = null;
const showVoiceWindow = ref(false);
const currentCaller = ref(null);
const voiceCallStore = useVoiceCallStore()
const voiceStore = useVoiceWindowStore()

onMounted(() => {
  const token = getCookie('xigua-token')
  if (!token) return

  const wsClient = getWebSocketClient()
  wsClient.connect()

  // 定义监听函数（一定要有引用，方便取消）
  wsListener = (message) => {
    try {
      // 接收全局消息
      const data = JSON.parse(message)
      handleReceiveMessage(data)
    } catch (e) {
      console.error('消息解析失败', message, e)
    }
  }

  // 订阅
  wsClient.subscribe(wsListener)
})

onUnmounted(() => {
  if (wsListener) {
    getWebSocketClient().unsubscribe(wsListener)
    wsListener = null
  }
})

function handleReceiveMessage(msg) {
  const messageType = msg.messageType;
  const subType = msg.subType;

  if(messageType == null || messageType === ''){
    return;
  }
  
  // 处理聊天消息接收事件
  if(messageType == 'chat' && subType == 'mes_receive') {
    // 只有当页面不可见时才添加小红点
    if(!document.hidden) {
      return;
    }
    
    // 浏览器title小红点效果
    const redDot = '🔸';
    const currentTitle = document.title;
    
    // 检查小红点是否已存在，避免重复添加
    if(!currentTitle.includes(redDot)) {
      document.title = `${redDot} ${currentTitle}`;
    }
  }
}

// 监听页面可见性变化，页面可见时移除小红点
document.addEventListener('visibilitychange', () => {
  if(!document.hidden) {
    removeRedDot();
  }
});

// 页面加载时移除小红点
removeRedDot();

function removeRedDot() {
  const redDot = '🔸';
  const currentTitle = document.title;
  if(currentTitle.includes(redDot)) {
    document.title = currentTitle.replace(`${redDot} `, '');
  }
}

</script>
<style>
html, body {
  height: 100%;
  margin: 0;
  overflow: hidden;
}

#app {
  width: 100%;
  height: 100vh;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
}
</style>

