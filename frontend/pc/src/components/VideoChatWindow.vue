<template>
  <div 
    class="video-chat-window" 
    v-if="showVideoWindow" 
    :class="{ 'minimized': isMinimized }"
    :style="!isMinimized ? windowPos : ''"
    role="dialog"
    aria-modal="true"
    aria-label="视频通话"
  >
    <!-- 视频通话窗口 -->
    <div class="video-container" v-show="!isMinimized">
      <!-- 远程视频 -->
      <div class="remote-video-container" @mousedown.prevent="startDrag">
        <video 
          ref="remoteVideo" 
          autoplay 
          playsinline
          class="remote-video"
          :class="{ 'show-placeholder': !remoteStreamReady }"
        ></video>
        <div v-if="!remoteStreamReady" class="video-placeholder">
          <div class="placeholder-avatar">
            <img v-if="caller.avatar" :src="caller.avatar" />
            <span v-else>{{ caller.name ? caller.name.charAt(0).toUpperCase() : 'U' }}</span>
          </div>
        </div>
      </div>

      <!-- 本地视频（画中画） -->
      <div class="local-video-container">
        <video 
          ref="localVideo" 
          autoplay 
          playsinline 
          muted
          class="local-video"
        ></video>
      </div>

      <!-- 顶部信息栏 -->
      <div class="video-header" @mousedown.prevent="startDrag">
        <div class="video-user-info">
          <div class="video-username">{{ caller.name || '未知用户' }}</div>
          <div class="video-status-message" :class="displayStatusClass" aria-live="polite">
            <span class="status-dot" :class="statusDotState"></span>
            {{ displayStatusText }}
          </div>
        </div>
        <div class="video-call-duration" v-if="connectionStatus === 'connected'">
          {{ formatDuration(currentDuration) }}
        </div>
      </div>

      <div
        v-if="signalBanner"
        class="signal-banner"
        :class="signalBanner.tone"
        role="status"
        aria-live="assertive"
      >
        <span class="signal-banner-dot" aria-hidden="true"></span>
        <div class="signal-banner-copy">
          <strong>{{ signalBanner.title }}</strong>
          <span>{{ signalBanner.detail }}</span>
        </div>
      </div>

      <!-- 底部控制按钮区域 -->
      <div class="video-controls" @mousedown.stop>
        <div class="control-bar">
          <!-- 麦克风开关 -->
          <button
              class="control-btn mic-btn"
              :class="{ 'muted': isMicMuted }"
              @click="toggleMicrophone"
              :disabled="isProcessing"
              :title="isMicMuted ? '开启麦克风' : '静音'"
              :aria-label="isMicMuted ? '开启麦克风' : '静音'"
              :aria-pressed="isMicMuted"
          >
            <img
                :src="isMicMuted ? closeMic : openMic"
                :alt="isMicMuted ? '关闭麦克风' : '开启麦克风'"
                class="btn-icon"
            />
          </button>

          <!-- 摄像头开关 -->
          <button
              class="control-btn camera-btn"
              :class="{ 'muted': isCameraMuted }"
              @click="toggleCamera"
              :disabled="isProcessing"
              :title="isCameraMuted ? '开启摄像头' : '关闭摄像头'"
              :aria-label="isCameraMuted ? '开启摄像头' : '关闭摄像头'"
              :aria-pressed="isCameraMuted"
          >
            <img
                :src="isCameraMuted ? closeCamera : openCamera"
                :alt="isCameraMuted ? '关闭摄像头' : '开启摄像头'"
                class="btn-icon"
            />
          </button>

          <!-- 挂断 / 取消按钮 -->
          <button
              class="control-btn hangup-btn"
              @click="onHangupAction"
              :disabled="isProcessing"
              :title="hasConnected ? '挂断通话' : '取消呼叫'"
              :aria-label="hasConnected ? '挂断通话' : '取消呼叫'"
          >
            <img 
              src="/src/static/icons/tel.png" 
              :alt="hasConnected ? '挂断通话' : '取消呼叫'" 
              class="btn-icon hangup-icon"
            >
          </button>

          <!-- 远端音频静音 -->
          <button
              class="control-btn speaker-btn"
              :class="{ 'muted': isSpeakerMuted }"
              @click="toggleSpeaker"
              :disabled="isProcessing"
              :title="isSpeakerMuted ? '恢复对方声音' : '静音对方声音'"
              :aria-label="isSpeakerMuted ? '恢复对方声音' : '静音对方声音'"
              :aria-pressed="isSpeakerMuted"
          >
            <img
                :src="isSpeakerMuted ? closeSpeaker : openSpeaker"
                :alt="isSpeakerMuted ? '恢复对方声音' : '静音对方声音'"
                class="btn-icon"
            >
          </button>

          <!-- 最小化按钮 -->
          <button
              class="control-btn minimize-btn"
              @click="minimizeWindow"
              :disabled="isProcessing"
              title="最小化"
              aria-label="最小化通话窗口"
          >
            <img src="/src/static/icons/minimize.png" alt="最小化" class="btn-icon">
          </button>
        </div>
      </div>
    </div>

    <!-- 最小化悬浮气泡 -->
    <div
        class="video-minimized"
        v-if="isMinimized"
        :style="{ left: minimizedPos.x + 'px', top: minimizedPos.y + 'px' }"
        @mousedown.prevent="startDrag"
        @click="handleBubbleClick"
        role="button"
        tabindex="0"
        :aria-label="`恢复与 ${caller.name || '对方'} 的视频通话`"
        @keydown.enter.prevent="handleBubbleClick"
        @keydown.space.prevent="handleBubbleClick"
    >
      <div class="minimized-avatar">
        <img v-if="caller.avatar" :src="caller.avatar" />
        <span v-else>{{ caller.name ? caller.name.charAt(0).toUpperCase() : 'V' }}</span>
      </div>
      <div class="minimized-status">
        <div class="minimized-name">{{ caller.name || '视频通话' }}</div>
        <div class="minimized-duration" v-if="connectionStatus === 'connected'">
          {{ formatDuration(currentDuration) }}
        </div>
      </div>
    </div>
  </div>
  
  <!-- 确认对话框 -->
  <ConfirmDialog
    :visible="showConfirmDialog"
    title="权限提示"
    content="需要摄像头和麦克风权限才能进行视频通话，请在浏览器设置中允许访问。"
    @confirm="handleConfirmDialog"
    @cancel="handleCancelDialog"
  />
</template>

<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { storeToRefs } from 'pinia'
import { useVideoWindowStore } from '@/store/videoWindow.js'
import { getWebSocketClient } from '@/ws/WebSocketClient'
import { getObject } from '@/utils/localStorage.js'
import ConfirmDialog from '@/components/ConfirmDialog.vue'
import openMic from '@/static/icons/open_mic.png'
import closeMic from '@/static/icons/close_mic.png'
import openSpeaker from '@/static/icons/open_speaker.png'
import closeSpeaker from '@/static/icons/close_speaker.png'
import openCamera from '@/static/icons/video.png'

const store = useVideoWindowStore()
const { showVideoWindow, callType, caller } = storeToRefs(store)

const CONNECT_TIMEOUT = 20000

const localVideo = ref(null)
const remoteVideo = ref(null)

const isMinimized = ref(false)
const isProcessing = ref(false)
const isMicMuted = ref(false)
const isSpeakerMuted = ref(false)
const isCameraMuted = ref(false)
const showConfirmDialog = ref(false)
const hasConnected = ref(false)
const callRecordSent = ref(false)
const previousFocus = ref(null)

const connectionStatus = ref('calling')
const currentDuration = ref(0)
const statusMessage = ref('正在连接...')
const remoteStreamReady = ref(false)

const currentUser = ref({})
const callId = ref(null)
const peerConnection = ref(null)
const mediaStream = ref(null)
const remoteStream = ref(null)
const iceCandidates = ref([])

const durationTimer = ref(null)
const callTimeoutTimer = ref(null)
const connectTimeoutTimer = ref(null)
const iceRestartTimer = ref(null)
const iceRestartAttempts = ref(0)
const restartingIce = ref(false)
const wsListener = ref(null)
let connectionListener = null

const signalState = ref('open')
const signalAttempt = ref(0)
const signalMaxRetries = ref(5)

const windowPos = ref({
  position: 'fixed',
  left: '50%',
  top: '50%',
  transform: 'translate(-50%, -50%)',
  width: '480px',
  height: '640px',
  zIndex: 9998
})

const minimizedPos = ref({
  x: Math.max(8, window.innerWidth - 168),
  y: Math.max(8, window.innerHeight - 76)
})

const iceServers = [
  { urls: 'stun:stun.l.google.com:19302' },
  { urls: 'stun:stun1.l.google.com:19302' },
  {
    // 自建 coturn，经 80 端口 TCP 复用接入（阿里云安全组仅放行 80/443，UDP 不可用）。
    urls: [
      'turn:xiguaim.eu.cc:80?transport=tcp',
      'turn:47.94.184.61:80?transport=tcp'
    ],
    username: 'xigua',
    credential: 'xiguaTurn2026K7p9Q2m4Z8'
  }
]

const closeCamera = openCamera

const statusMessageClass = computed(() => {
  const statusMap = {
    'calling': 'status-calling',
    'connected': 'status-connected',
    'disconnected': 'status-disconnected'
  }
  return statusMap[connectionStatus.value] || 'status-connecting'
})

const signalBanner = computed(() => {
  if (signalState.value === 'failed') {
    return {
      tone: 'danger',
      title: '通话信号已断开',
      detail: '请挂断后重新发起通话'
    }
  }
  if (signalState.value === 'reconnecting') {
    return {
      tone: 'warning',
      title: '通话信号中断',
      detail: `正在重连（${signalAttempt.value}/${signalMaxRetries.value}）…`
    }
  }
  if (signalState.value === 'closed') {
    return {
      tone: 'warning',
      title: '通话信号中断',
      detail: '正在尝试恢复连接…'
    }
  }
  return null
})

const displayStatusText = computed(() => {
  if (signalState.value === 'failed') return '信号已断开'
  if (signalState.value === 'reconnecting' || signalState.value === 'closed') return '信号重连中'
  return statusMessage.value
})

const displayStatusClass = computed(() => {
  if (signalState.value === 'failed') return 'status-disconnected'
  if (signalState.value === 'reconnecting' || signalState.value === 'closed') return 'status-calling'
  return statusMessageClass.value
})

const statusDotState = computed(() => {
  if (signalState.value === 'failed') return 'disconnected'
  if (signalState.value === 'reconnecting' || signalState.value === 'closed') return 'connecting'
  return connectionStatus.value
})

function focusFirstControl() {
  document.querySelector('.video-chat-window .control-btn')?.focus()
}

function ensureCallFocus(attempt = 0) {
  if (!showVideoWindow.value || isMinimized.value) return
  const win = document.querySelector('.video-chat-window')
  if (win && !win.contains(document.activeElement)) {
    focusFirstControl()
  }
  if (attempt < 10) {
    setTimeout(() => ensureCallFocus(attempt + 1), 100)
  }
}

function restorePreviousFocus() {
  const previous = previousFocus.value
  const target = previous && document.contains(previous)
    ? previous
    : document.querySelector('i.icon-tool[title="视频通话"]')
  target?.focus?.()
}

function handleWindowFocus() {
  if (!showVideoWindow.value || isMinimized.value) return
  const win = document.querySelector('.video-chat-window')
  if (win && !win.contains(document.activeElement)) {
    focusFirstControl()
  }
}

function handleFocusTrap(event) {
  if (event.key !== 'Tab' || !showVideoWindow.value) return

  const scope = isMinimized.value
    ? document.querySelector('.video-minimized')
    : document.querySelector('.video-chat-window')
  if (!scope) return

  const focusables = isMinimized.value
    ? [scope]
    : Array.from(scope.querySelectorAll('button:not([disabled])'))
  if (!focusables.length) return

  const first = focusables[0]
  const last = focusables[focusables.length - 1]
  const active = document.activeElement

  if (!scope.contains(active)) {
    event.preventDefault()
    ;(event.shiftKey ? last : first).focus()
    return
  }

  if (event.shiftKey && active === first) {
    event.preventDefault()
    last.focus()
  } else if (!event.shiftKey && active === last) {
    event.preventDefault()
    first.focus()
  }
}

watch(isProcessing, (value) => {
  if (!value && showVideoWindow.value && !isMinimized.value) {
    nextTick(() => {
      ensureCallFocus()
    })
  }
})

watch(showVideoWindow, (newVal) => {
  if (newVal) {
    previousFocus.value = document.activeElement instanceof HTMLElement ? document.activeElement : null
    setupWebSocketListener()
    initializeVideoChat()
    document.addEventListener('keydown', handleFocusTrap, true)
    window.addEventListener('focus', handleWindowFocus)
    nextTick(() => {
      ensureCallFocus()
    })
  } else {
    cleanup()
  }
})

onMounted(() => {
  // 只有通话窗口真正打开时才监听信令，避免未接听时被 offer 自动接通
  if (showVideoWindow.value) {
    setupWebSocketListener()
  }
})

onBeforeUnmount(() => {
  cleanup()
})

async function initializeVideoChat() {
  try {
    currentUser.value = getObject('userInfo') || {}
    callRecordSent.value = false
    
    if (callType.value === 'outgoing') {
      await initiateOutgoingCall()
    } else if (callType.value === 'incoming') {
      await handleIncomingCall()
    }
  } catch (error) {
    console.error('初始化视频聊天失败:', error)
  }
}

async function initiateOutgoingCall() {
  try {
    clearCallTimeoutTimer()
    
    statusMessage.value = '正在呼叫...'
    isProcessing.value = true

    if (!callId.value) {
      callId.value = 'call_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9)
      caller.value.callId = callId.value
    }

    await getUserMedia()
    await createPeerConnection()

    sendWebSocketMessage({
      messageType: 'video_call',
      subType: 'video_call_request',
      senderId: currentUser.value.id,
      receiverId: caller.value.userId,
      callId: callId.value,
      timestamp: Date.now()
    })

    isProcessing.value = false
    statusMessage.value = '等待对方接听...'
    
    callTimeoutTimer.value = setTimeout(() => {
      if (connectionStatus.value !== 'connected') {
        sendWebSocketMessage({
          messageType: 'video_call',
          subType: 'video_call_timeout',
          senderId: currentUser.value.id,
          receiverId: caller.value.userId,
          callId: callId.value,
          timestamp: Date.now()
        })
        statusMessage.value = '对方未接听'
        connectionStatus.value = 'disconnected'
        // 主叫自己的超时计时器到点也要落记录；被叫的 timeout 帧可能同时到达并被此分支抢先关窗
        recordCallOutcome('timeout')
        closeVideoWindow()
      }
    }, 30000)

  } catch (error) {
    console.error('呼叫失败:', error)
    statusMessage.value = '呼叫失败'
    connectionStatus.value = 'disconnected'
    isProcessing.value = false
    clearCallTimeoutTimer()
  }
}

async function handleIncomingCall() {
  try {
    statusMessage.value = '正在建立连接...'
    isProcessing.value = true

    await getUserMedia()
    await createPeerConnection()

    isProcessing.value = false
    statusMessage.value = '等待对方建立连接...'
    startConnectTimeout()

  } catch (error) {
    console.error('建立连接失败:', error)
    statusMessage.value = '连接失败'
    isProcessing.value = false
  }
}

async function getUserMedia() {
  try {
    const constraints = {
      audio: {
        echoCancellation: true,
        noiseSuppression: true
      },
      video: {
        width: { ideal: 1280 },
        height: { ideal: 720 },
        facingMode: 'user'
      }
    }

    mediaStream.value = await navigator.mediaDevices.getUserMedia(constraints)
    
    await nextTick()
    
    if (localVideo.value) {
      localVideo.value.srcObject = mediaStream.value
      localVideo.value.onloadedmetadata = () => {
        localVideo.value.play().catch(err => {
          console.error('播放本地视频失败:', err)
        })
      }
    }
  } catch (error) {
    console.error('获取媒体流失败:', error)
    
    if (error.name === 'NotAllowedError' || error.name === 'PermissionDeniedError') {
      showConfirmDialog.value = true
      closeVideoWindow()
    } else if (error.name === 'AbortError') {
      console.error('摄像头启动超时，可能是设备被其他应用占用')
      statusMessage.value = '摄像头启动超时'
      connectionStatus.value = 'disconnected'
      closeVideoWindow()
    } else if (error.name === 'NotFoundError') {
      console.error('未找到摄像头或麦克风设备')
      statusMessage.value = '未找到摄像头或麦克风'
      connectionStatus.value = 'disconnected'
      closeVideoWindow()
    } else if (error.name === 'NotReadableError') {
      console.error('摄像头或麦克风被其他应用占用')
      statusMessage.value = '设备被其他应用占用'
      connectionStatus.value = 'disconnected'
      closeVideoWindow()
    }
    
    throw error
  }
}

function handleConfirmDialog() {
  showConfirmDialog.value = false
}

function handleCancelDialog() {
  showConfirmDialog.value = false
}

async function createPeerConnection() {
  try {
    peerConnection.value = new RTCPeerConnection({ iceServers })

    mediaStream.value.getTracks().forEach(track => {
      peerConnection.value.addTrack(track, mediaStream.value)
    })

    peerConnection.value.ontrack = (event) => {
      const newStream = event.streams[0]
      
      if (remoteStream.value !== newStream) {
        remoteStream.value = newStream
        remoteStreamReady.value = true
        
        nextTick(() => {
          if (remoteVideo.value && remoteStream.value) {
            remoteVideo.value.srcObject = remoteStream.value
            remoteVideo.value.onloadedmetadata = () => {
              remoteVideo.value.play().catch(err => {
                console.error('播放远程视频失败:', err)
              })
            }
          }
        })
      }
    }

    peerConnection.value.onicecandidate = (event) => {
      if (event.candidate) {
        sendWebSocketMessage({
          messageType: 'video_call',
          subType: 'video_call_ice_candidate',
          senderId: currentUser.value.id,
          receiverId: caller.value.userId,
          message: event.candidate,
          callId: callId.value,
          timestamp: Date.now()
        })
      }
    }

    peerConnection.value.oniceconnectionstatechange = () => {
      const state = peerConnection.value.iceConnectionState

      if (state === 'connected' || state === 'completed') {
        const firstConnection = !hasConnected.value
        hasConnected.value = true
        connectionStatus.value = 'connected'
        statusMessage.value = '通话中'
        clearConnectTimeout()
        resetIceRestart()
        startCallTimer(firstConnection)
      } else if (state === 'disconnected') {
        connectionStatus.value = 'disconnected'
        statusMessage.value = '连接不稳定，正在尝试恢复...'
        scheduleIceRestart()
      } else if (state === 'failed') {
        if (callType.value === 'outgoing') {
          attemptIceRestart()
        } else {
          connectionStatus.value = 'disconnected'
          statusMessage.value = '连接已断开，请挂断后重试'
          stopCallTimer()
        }
      }
    }
  } catch (error) {
    console.error('创建PeerConnection失败:', error)
    throw error
  }
}

function scheduleIceRestart() {
  if (callType.value !== 'outgoing' || !peerConnection.value || iceRestartTimer.value) return
  // 网络切换时先给 ICE 3 秒自愈窗口，仍未恢复再重新协商。
  iceRestartTimer.value = setTimeout(() => {
    iceRestartTimer.value = null
    const pc = peerConnection.value
    if (!pc) return
    const state = pc.iceConnectionState || pc.connectionState
    if (state === 'disconnected' || state === 'failed') {
      attemptIceRestart()
    }
  }, 3000)
}

function resetIceRestart() {
  if (iceRestartTimer.value) {
    clearTimeout(iceRestartTimer.value)
    iceRestartTimer.value = null
  }
  iceRestartAttempts.value = 0
  restartingIce.value = false
}

async function attemptIceRestart() {
  const pc = peerConnection.value
  if (!pc || callType.value !== 'outgoing' || restartingIce.value) return
  if (!hasConnected.value) {
    connectionStatus.value = 'disconnected'
    statusMessage.value = '连接已断开，请挂断后重试'
    stopCallTimer()
    return
  }
  if (iceRestartAttempts.value >= 2) {
    connectionStatus.value = 'disconnected'
    statusMessage.value = '网络连接已中断，请挂断后重新发起'
    stopCallTimer()
    return
  }
  iceRestartAttempts.value += 1
  restartingIce.value = true
  try {
    const offer = await pc.createOffer({ iceRestart: true })
    await pc.setLocalDescription(offer)
    sendWebSocketMessage({
      messageType: 'video_call',
      subType: 'video_call_offer',
      senderId: currentUser.value.id,
      receiverId: caller.value.userId,
      message: offer,
      callId: callId.value,
      timestamp: Date.now()
    })
    statusMessage.value = '网络切换，正在恢复通话...'
  } catch (error) {
    console.error('ICE 重启失败:', error)
    restartingIce.value = false
    attemptIceRestart()
  }
}

function setupSignalStateListener() {
  const wsClient = getWebSocketClient()
  const max = wsClient.getMaxRetries ? wsClient.getMaxRetries() : 5
  signalMaxRetries.value = max

  if (connectionListener) {
    wsClient.unsubscribeConnectionState(connectionListener)
  }

  connectionListener = (event) => {
    if (!showVideoWindow.value) return
    if (event.state === 'open') {
      signalState.value = 'open'
      signalAttempt.value = 0
      return
    }
    signalState.value = event.state
    signalAttempt.value = event.attempt || 0
    if (event.max) signalMaxRetries.value = event.max
  }

  wsClient.subscribeConnectionState(connectionListener)

  // 订阅时可能已经处于断线/重连中，主动同步一次当前状态
  const ready = wsClient.getReadyState()
  const attempts = wsClient.getReconnectAttempts()
  if (ready === WebSocket.OPEN) {
    signalState.value = 'open'
    signalAttempt.value = 0
  } else if (attempts >= max) {
    signalState.value = 'failed'
    signalAttempt.value = attempts
  } else if (wsClient.isReconnecting()) {
    signalState.value = 'reconnecting'
    signalAttempt.value = attempts
  } else if (ready === WebSocket.CLOSED) {
    signalState.value = 'closed'
    signalAttempt.value = attempts
  }
}

function setupWebSocketListener() {
  const wsClient = getWebSocketClient()
  setupSignalStateListener()
  
  if (wsListener.value) {
    wsClient.unsubscribe(wsListener.value)
  }

  wsListener.value = (raw) => {
    try {
      const data = JSON.parse(raw)
      
      if (data.messageType !== 'video_call') return

      switch (data.subType) {
        case 'video_call_offer':
          handleOffer(data)
          break
        case 'video_call_answer':
          handleAnswer(data)
          break
        case 'video_call_accepted':
          handleCallAccepted(data)
          break
        case 'video_call_ice_candidate':
          handleIceCandidate(data)
          break
        case 'video_call_ended':
          handleCallEnded(data)
          break
        case 'video_call_rejected':
          handleCallRejected(data)
          break
        case 'video_call_canceled':
          handleCallCanceled(data)
          break
        case 'video_call_timeout':
          handleCallTimeout(data)
          break
      }
    } catch (error) {
      console.error('处理WebSocket消息失败:', error)
    }
  }

  wsClient.subscribe(wsListener.value)
}

async function handleOffer(data) {
  try {
    // 未打开通话窗口（尚未接听）时不得自动应答
    if (!showVideoWindow.value || callType.value !== 'incoming') {
      return
    }

    if (data.callId) {
      callId.value = data.callId
    }
    
    if (data.senderId && !caller.value.userId) {
      caller.value.userId = data.senderId
    }

    if (!peerConnection.value) {
      await getUserMedia()
      await createPeerConnection()
    }

    let offer
    if (typeof data.message === 'string') {
      offer = JSON.parse(data.message)
    } else if (data.message && data.message.sdp) {
      offer = data.message
    } else {
      offer = data
    }

    const rtcOffer = new RTCSessionDescription({
      type: 'offer',
      sdp: offer.sdp || offer
    })

    await peerConnection.value.setRemoteDescription(rtcOffer)
    await addCachedIceCandidates()
    const answer = await peerConnection.value.createAnswer()
    await peerConnection.value.setLocalDescription(answer)

    sendWebSocketMessage({
      messageType: 'video_call',
      subType: 'video_call_answer',
      senderId: currentUser.value.id,
      receiverId: caller.value.userId,
      message: answer,
      callId: callId.value,
      timestamp: Date.now()
    })

    if (!hasConnected.value) {
      statusMessage.value = '正在建立连接...'
    }

  } catch (error) {
    console.error('处理Offer失败:', error)
    statusMessage.value = '建立连接失败'
    connectionStatus.value = 'disconnected'
  }
}

async function handleAnswer(data) {
  try {
    if (!peerConnection.value) return

    if (data.callId) {
      callId.value = data.callId
    }

    let answer
    if (typeof data.message === 'string') {
      answer = JSON.parse(data.message)
    } else if (data.message && data.message.sdp) {
      answer = data.message
    } else {
      answer = data
    }

    const rtcAnswer = new RTCSessionDescription({
      type: 'answer',
      sdp: answer.sdp || answer
    })

    await peerConnection.value.setRemoteDescription(rtcAnswer)
    await addCachedIceCandidates()
    restartingIce.value = false
    if (!hasConnected.value) {
      statusMessage.value = '正在建立连接...'
    }

  } catch (error) {
    console.error('处理Answer失败:', error)
    statusMessage.value = '建立连接失败'
    connectionStatus.value = 'disconnected'
  }
}

async function handleIceCandidate(data) {
  try {
    if (!peerConnection.value) return

    let candidate
    if (typeof data.message === 'string') {
      candidate = JSON.parse(data.message)
    } else {
      candidate = data.message
    }

    if (peerConnection.value.remoteDescription) {
      await peerConnection.value.addIceCandidate(new RTCIceCandidate(candidate))
    } else {
      iceCandidates.value.push(candidate)
      console.warn('远程描述未设置，缓存ICE候选')
    }
  } catch (error) {
    console.error('添加ICE候选失败:', error)
  }
}

async function addCachedIceCandidates() {
  if (!peerConnection.value) return

  while (iceCandidates.value.length > 0) {
    try {
      const candidate = iceCandidates.value.shift()
      await peerConnection.value.addIceCandidate(new RTCIceCandidate(candidate))
    } catch (error) {
      console.error('添加缓存的ICE候选失败:', error)
    }
  }
}

async function handleCallAccepted(data) {
  try {
    clearCallTimeoutTimer()
    statusMessage.value = '对方已接听'
    
    if (!peerConnection.value) {
      await getUserMedia()
      await createPeerConnection()
    }

    const offer = await peerConnection.value.createOffer()
    await peerConnection.value.setLocalDescription(offer)

    const offerMessage = {
      messageType: 'video_call',
      subType: 'video_call_offer',
      senderId: currentUser.value.id,
      receiverId: caller.value.userId,
      message: offer,
      callId: callId.value,
      timestamp: Date.now()
    }
    
    sendWebSocketMessage(offerMessage)

    isProcessing.value = false
    statusMessage.value = '正在建立连接...'
    startConnectTimeout()

  } catch (error) {
    console.error('发送Offer失败:', error)
    statusMessage.value = '建立连接失败'
    connectionStatus.value = 'disconnected'
    isProcessing.value = false
    clearCallTimeoutTimer()
  }
}

function handleCallRejected(data) {
  clearCallTimeoutTimer()
  // 对端正在通话时自动回 busy，与用户主动拒绝区分开
  const isBusy = !!(data && data.message === 'busy')
  recordCallOutcome(isBusy ? 'busy' : 'rejected')
  
  statusMessage.value = isBusy ? '对方忙线中' : '对方已拒绝'
  connectionStatus.value = 'disconnected'

  setTimeout(() => {
    closeVideoWindow()
  }, 1000)
}

function handleCallEnded(data) {
  clearCallTimeoutTimer()
  recordCallOutcome(hasConnected.value ? 'completed' : 'canceled')
  
  statusMessage.value = '对方已挂断'
  connectionStatus.value = 'disconnected'

  setTimeout(() => {
    closeVideoWindow()
  }, 2000)
}

function handleCallCanceled(data) {
  clearCallTimeoutTimer()
  recordCallOutcome(hasConnected.value ? 'completed' : 'canceled')
  
  statusMessage.value = '对方已取消'
  connectionStatus.value = 'disconnected'

  setTimeout(() => {
    closeVideoWindow()
  }, 2000)
}

function handleCallTimeout(data) {
  // 已经接通后的迟到超时帧不能把正常通话挂断
  if (hasConnected.value) return
  clearCallTimeoutTimer()
  clearConnectTimeout()
  recordCallOutcome('timeout')

  statusMessage.value = callType.value === 'incoming' ? '未接来电' : '对方无应答'
  connectionStatus.value = 'disconnected'

  setTimeout(() => {
    closeVideoWindow()
  }, 2000)
}

function clearCallTimeoutTimer() {
  if (callTimeoutTimer.value) {
    clearTimeout(callTimeoutTimer.value)
    callTimeoutTimer.value = null
  }
}

function startConnectTimeout() {
  clearConnectTimeout()
  connectTimeoutTimer.value = setTimeout(() => {
    if (!showVideoWindow.value || connectionStatus.value === 'connected') return
    recordCallOutcome(hasConnected.value ? 'completed' : 'failed')
    sendWebSocketMessage({
      messageType: 'video_call',
      subType: 'video_call_ended',
      senderId: currentUser.value.id,
      receiverId: caller.value.userId,
      callId: callId.value,
      timestamp: Date.now(),
      reason: 'connect_timeout'
    })
    statusMessage.value = '连接超时，可能是网络或防火墙限制'
    connectionStatus.value = 'disconnected'
    stopCallTimer()
    setTimeout(() => {
      if (connectTimeoutTimer.value === null) closeVideoWindow()
    }, 1500)
  }, CONNECT_TIMEOUT)
}

function clearConnectTimeout() {
  if (connectTimeoutTimer.value) {
    clearTimeout(connectTimeoutTimer.value)
    connectTimeoutTimer.value = null
  }
}

function recordCallOutcome(outcome) {
  // 只在主叫侧落一条记录，被叫通过收到的聊天消息看到同一通电话，避免重复。
  if (callRecordSent.value) return
  if (callType.value !== 'outgoing') return
  if (!currentUser.value.id || !caller.value.userId) return
  callRecordSent.value = true
  const payload = JSON.stringify({
    kind: 'video',
    outcome,
    duration: outcome === 'completed' ? (currentDuration.value || 0) : 0
  })
  sendWebSocketMessage({
    chatMessageId: '',
    senderId: currentUser.value.id,
    receiverId: caller.value.userId,
    message: payload,
    chatType: 1,
    messageType: 'chat',
    contentType: 9,
    subType: 'mes_send',
    isRead: false,
    sendStatus: 'sending',
    replyToId: '',
    replyContent: '',
    atUsers: '[]',
    createTime: Date.now()
  })
}

function sendWebSocketMessage(message) {
  try {
    const wsClient = getWebSocketClient()
    const socketInstance = wsClient.getInstance()
    
    if (socketInstance && socketInstance.readyState === WebSocket.OPEN) {
      wsClient.sendMessage(JSON.stringify(message))
    } else {
      wsClient.connect()
      setTimeout(() => {
        wsClient.sendMessage(JSON.stringify(message))
      }, 1000)
    }
  } catch (error) {
    console.error('发送WebSocket消息失败:', error)
  }
}

function startCallTimer(reset = false) {
  if (reset) {
    currentDuration.value = 0
  }
  if (durationTimer.value) {
    return
  }
  durationTimer.value = setInterval(() => {
    currentDuration.value++
  }, 1000)
}

function stopCallTimer() {
  if (durationTimer.value) {
    clearInterval(durationTimer.value)
    durationTimer.value = null
  }
}

function toggleMicrophone() {
  isMicMuted.value = !isMicMuted.value
  if (mediaStream.value) {
    const audioTrack = mediaStream.value.getAudioTracks()[0]
    if (audioTrack) {
      audioTrack.enabled = !isMicMuted.value
    }
  }
}

function toggleCamera() {
  isCameraMuted.value = !isCameraMuted.value
  if (mediaStream.value) {
    const videoTrack = mediaStream.value.getVideoTracks()[0]
    if (videoTrack) {
      videoTrack.enabled = !isCameraMuted.value
    }
  }
}

function toggleSpeaker() {
  isSpeakerMuted.value = !isSpeakerMuted.value
  if (remoteVideo.value) {
    remoteVideo.value.muted = isSpeakerMuted.value
  }
}

function onHangupAction() {
  // 确保停止拖拽，防止窗口漂移
  if (isDragging.value) {
    isDragging.value = false
    document.removeEventListener('mousemove', onDrag)
    document.removeEventListener('mouseup', stopDrag)
    document.removeEventListener('touchmove', onDrag)
    document.removeEventListener('touchend', stopDrag)
  }
  
  if (hasConnected.value) {
    hangUp()
  } else {
    cancelCall()
  }
}

async function hangUp() {
  try {
    isProcessing.value = true
    statusMessage.value = '正在挂断...'
    recordCallOutcome(hasConnected.value ? 'completed' : 'canceled')

    sendWebSocketMessage({
      messageType: 'video_call',
      subType: 'video_call_ended',
      senderId: currentUser.value.id,
      receiverId: caller.value.userId,
      callId: callId.value,
      timestamp: Date.now(),
      reason: 'user_hangup'
    })

    closeVideoWindow()
    cleanup()

  } catch (error) {
    console.error('挂断失败:', error)
    isProcessing.value = false
  }
}

function cancelCall() {
  try {
    isProcessing.value = true
    statusMessage.value = '已取消呼叫'
    recordCallOutcome('canceled')

    sendWebSocketMessage({
      messageType: 'video_call',
      subType: 'video_call_canceled',
      senderId: currentUser.value.id,
      receiverId: caller.value.userId,
      callId: callId.value,
      timestamp: Date.now()
    })

    closeVideoWindow()
    cleanup()
  } catch (e) {
    console.error('取消呼叫失败:', e)
  }
}

function minimizeWindow() {
  isMinimized.value = true
  nextTick(() => {
    document.querySelector('.video-minimized')?.focus()
  })
}

function restoreWindow() {
  isMinimized.value = false
  nextTick(() => {
    focusFirstControl()
  })
}

function handleBubbleClick() {
  if (isDragging.value) {
    isDragging.value = false
    return
  }
  restoreWindow()
}

function closeVideoWindow() {
  store.closeVideoWindow()
}

function cleanup() {
  stopCallTimer()
  clearCallTimeoutTimer()
  clearConnectTimeout()
  resetIceRestart()
  document.removeEventListener('keydown', handleFocusTrap, true)
  window.removeEventListener('focus', handleWindowFocus)

  if (isMinimized.value) {
    isMinimized.value = false
  }

  nextTick(() => {
    restorePreviousFocus()
  })
  previousFocus.value = null

  // 先清空视频元素的srcObject，避免视频元素保持对媒体流的引用
  if (localVideo.value) {
    localVideo.value.srcObject = null
  }

  if (remoteVideo.value) {
    remoteVideo.value.srcObject = null
  }

  // 停止所有媒体流的tracks
  if (mediaStream.value) {
    mediaStream.value.getTracks().forEach(track => {
      track.stop()
    })
    mediaStream.value = null
  }

  // 停止远程媒体流的tracks（如果有）
  if (remoteStream.value) {
    remoteStream.value.getTracks().forEach(track => {
      track.stop()
    })
    remoteStream.value = null
  }

  // 关闭peerConnection
  if (peerConnection.value) {
    peerConnection.value.close()
    peerConnection.value = null
  }

  // 取消WebSocket监听器订阅
  if (wsListener.value) {
    const wsClient = getWebSocketClient()
    wsClient.unsubscribe(wsListener.value)
    wsListener.value = null
  }

  if (connectionListener) {
    getWebSocketClient().unsubscribeConnectionState(connectionListener)
    connectionListener = null
  }

  // 重置其他状态
  iceCandidates.value = []
  remoteStreamReady.value = false
  connectionStatus.value = 'calling'
  isProcessing.value = false
  isMicMuted.value = false
  isSpeakerMuted.value = false
  isCameraMuted.value = false
  currentDuration.value = 0
  hasConnected.value = false
  callId.value = null
  signalState.value = 'open'
  signalAttempt.value = 0
}

function formatDuration(seconds) {
  const hours = Math.floor(seconds / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  const secs = seconds % 60
  
  if (hours > 0) {
    return `${hours}:${String(minutes).padStart(2, '0')}:${String(secs).padStart(2, '0')}`
  }
  return `${minutes}:${String(secs).padStart(2, '0')}`
}

const isDragging = ref(false)
const dragOffset = ref({ x: 0, y: 0 })

function startDrag(event) {
  if (event.button !== 0) return
  
  isDragging.value = true
  const clientX = event.clientX || event.touches?.[0]?.clientX
  const clientY = event.clientY || event.touches?.[0]?.clientY
  
  if (isMinimized.value) {
    dragOffset.value = {
      x: clientX - minimizedPos.value.x,
      y: clientY - minimizedPos.value.y
    }
  } else {
    const rect = event.currentTarget.getBoundingClientRect()
    dragOffset.value = {
      x: clientX - rect.left,
      y: clientY - rect.top
    }
  }

  document.addEventListener('mousemove', onDrag)
  document.addEventListener('mouseup', stopDrag)
  document.addEventListener('touchmove', onDrag)
  document.addEventListener('touchend', stopDrag)
}

function onDrag(event) {
  if (!isDragging.value) return
  
  const clientX = event.clientX || event.touches?.[0]?.clientX
  const clientY = event.clientY || event.touches?.[0]?.clientY
  
  if (isMinimized.value) {
    const bubble = document.querySelector('.video-minimized')
    const bubbleWidth = bubble?.offsetWidth || 156
    const bubbleHeight = bubble?.offsetHeight || 56
    minimizedPos.value = {
      x: Math.min(Math.max(8, clientX - dragOffset.value.x), window.innerWidth - bubbleWidth - 8),
      y: Math.min(Math.max(8, clientY - dragOffset.value.y), window.innerHeight - bubbleHeight - 8)
    }
  } else {
    windowPos.value = {
      ...windowPos.value,
      left: (clientX - dragOffset.value.x) + 'px',
      top: (clientY - dragOffset.value.y) + 'px',
      right: 'auto'
    }
  }
}

function stopDrag() {
  isDragging.value = false
  document.removeEventListener('mousemove', onDrag)
  document.removeEventListener('mouseup', stopDrag)
  document.removeEventListener('touchmove', onDrag)
  document.removeEventListener('touchend', stopDrag)
}
</script>

<style scoped>
.video-chat-window {
  position: fixed;
  z-index: 9998;
}

/* 最小化时收起整个通话窗，避免透明全屏层继续拦截底层页面点击；
   视频容器用 v-show 保留在 DOM 中，恢复后本地/远端流不会黑屏 */
.video-chat-window.minimized {
  left: 0 !important;
  top: 0 !important;
  right: auto !important;
  bottom: auto !important;
  width: 0 !important;
  height: 0 !important;
  transform: none !important;
  pointer-events: none;
}

.video-chat-window.minimized .video-minimized {
  pointer-events: auto;
}

.video-container {
  width: 100%;
  height: 100%;
  background: #050505;
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 24px 80px rgba(0, 0, 0, 0.45), 0 0 0 1px rgba(255, 255, 255, 0.08);
  display: flex;
  flex-direction: column;
  position: relative;
  isolation: isolate;
}

.remote-video-container {
  flex: 1;
  position: relative;
  background: #050505;
}

.remote-video {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.remote-video.show-placeholder {
  opacity: 0;
}

.video-placeholder {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: radial-gradient(circle at 50% 35%, #2a2a2e 0%, #111113 70%);
}

.placeholder-avatar {
  width: 112px;
  height: 112px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.14);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  font-size: 42px;
  font-weight: 600;
  color: #fff;
  box-shadow: 0 20px 50px rgba(0, 0, 0, 0.35);
}

.placeholder-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.local-video-container {
  position: absolute;
  bottom: 110px;
  right: 16px;
  width: 98px;
  height: 132px;
  border-radius: 16px;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.28);
  background: #111;
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.45), inset 0 1px 0 rgba(255, 255, 255, 0.16);
  z-index: 3;
}

.local-video {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transform: scaleX(-1);
  display: block;
}

.video-header {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  padding: calc(16px + env(safe-area-inset-top)) 16px 44px;
  background: linear-gradient(180deg, rgba(0, 0, 0, 0.64) 0%, rgba(0, 0, 0, 0.28) 55%, rgba(0, 0, 0, 0) 100%);
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  z-index: 4;
}

.video-user-info {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  min-width: 0;
}

.video-username {
  max-width: 220px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 17px;
  font-weight: 600;
  letter-spacing: 0.2px;
  color: #fff;
  text-shadow: 0 2px 12px rgba(0, 0, 0, 0.45);
}

.video-status-message {
  margin-top: 7px;
  padding: 4px 9px 4px 8px;
  border-radius: 999px;
  background: rgba(0, 0, 0, 0.34);
  border: 1px solid rgba(255, 255, 255, 0.12);
  color: rgba(255, 255, 255, 0.9);
  font-size: 12px;
  line-height: 1.2;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
}

.status-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  flex: 0 0 auto;
  animation: pulse 1.5s ease-in-out infinite;
}

.status-dot.calling {
  background: #ffd60a;
  box-shadow: 0 0 0 3px rgba(255, 214, 10, 0.16);
}

.status-dot.connected {
  background: #30d158;
  box-shadow: 0 0 0 3px rgba(48, 209, 88, 0.18);
  animation: none;
}

.status-dot.disconnected {
  background: #ff453a;
  box-shadow: 0 0 0 3px rgba(255, 69, 58, 0.18);
  animation: none;
}

.video-call-duration {
  flex: 0 0 auto;
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(0, 0, 0, 0.34);
  border: 1px solid rgba(255, 255, 255, 0.12);
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  line-height: 1.2;
  font-variant-numeric: tabular-nums;
  font-family: 'SF Mono', 'SF Pro Display', Monaco, monospace;
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
}

.signal-banner {
  position: absolute;
  top: calc(90px + env(safe-area-inset-top));
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  align-items: center;
  gap: 9px;
  max-width: calc(100% - 32px);
  padding: 8px 13px 8px 12px;
  border-radius: 14px;
  background: rgba(28, 28, 32, 0.82);
  border: 1px solid rgba(255, 255, 255, 0.14);
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.38), inset 0 1px 0 rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(18px) saturate(150%);
  -webkit-backdrop-filter: blur(18px) saturate(150%);
  z-index: 6;
  pointer-events: none;
}

.signal-banner-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex: 0 0 auto;
}

.signal-banner.warning .signal-banner-dot {
  background: #ffd60a;
  box-shadow: 0 0 0 3px rgba(255, 214, 10, 0.16);
}

.signal-banner.danger .signal-banner-dot {
  background: #ff453a;
  box-shadow: 0 0 0 3px rgba(255, 69, 58, 0.2);
}

.signal-banner-copy {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.signal-banner-copy strong {
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  line-height: 1.25;
  white-space: nowrap;
}

.signal-banner-copy span {
  color: rgba(255, 255, 255, 0.72);
  font-size: 11px;
  line-height: 1.3;
  white-space: nowrap;
}

.video-controls {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 44px 16px calc(18px + env(safe-area-inset-bottom));
  background: linear-gradient(to top, rgba(0, 0, 0, 0.62) 0%, rgba(0, 0, 0, 0.16) 55%, transparent 100%);
  display: flex;
  justify-content: center;
  z-index: 5;
}

.control-bar {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 14px;
  padding: 10px;
  border-radius: 999px;
  background: rgba(26, 26, 30, 0.56);
  border: 1px solid rgba(255, 255, 255, 0.14);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.35), inset 0 1px 0 rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(20px) saturate(140%);
  -webkit-backdrop-filter: blur(20px) saturate(140%);
}

.control-btn {
  width: 46px;
  height: 46px;
  padding: 0;
  border: 1px solid rgba(255, 255, 255, 0.14);
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.16);
  color: #fff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: transform 0.18s ease, background 0.18s ease, opacity 0.18s ease;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.12);
  -webkit-tap-highlight-color: transparent;
}

.control-btn:hover:not(:disabled) {
  background: rgba(255, 255, 255, 0.24);
  transform: scale(1.04);
}

.control-btn:active:not(:disabled) {
  transform: scale(0.94);
}

.control-btn:focus-visible {
  outline: 2px solid rgba(255, 255, 255, 0.85);
  outline-offset: 3px;
}

.control-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.control-btn.muted {
  background: #f2f2f7;
  border-color: rgba(255, 255, 255, 0.6);
}

.control-btn.muted .btn-icon {
  filter: brightness(0);
}

.hangup-btn {
  width: 58px;
  height: 58px;
  background: #ff453a;
  border: none;
  box-shadow: 0 10px 24px rgba(255, 69, 58, 0.38), inset 0 1px 0 rgba(255, 255, 255, 0.22);
}

.hangup-btn:hover:not(:disabled) {
  background: #ff3b30;
  transform: scale(1.04);
}

.btn-icon {
  width: 22px;
  height: 22px;
  display: block;
  pointer-events: none;
  filter: brightness(0) invert(1);
}

.hangup-icon {
  width: 24px;
  height: 24px;
  transform: rotate(135deg);
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.video-minimized {
  position: fixed;
  box-sizing: border-box;
  width: auto;
  min-width: 154px;
  height: 56px;
  background: rgba(28, 28, 32, 0.82);
  border: 1px solid rgba(255, 255, 255, 0.16);
  border-radius: 999px;
  box-shadow: 0 10px 28px rgba(0, 0, 0, 0.4), inset 0 1px 0 rgba(255, 255, 255, 0.1);
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 9px;
  padding: 4px 14px 4px 4px;
  cursor: grab;
  transition: transform 0.18s ease, box-shadow 0.18s ease;
  z-index: 9999;
  -webkit-backdrop-filter: blur(20px) saturate(140%);
  backdrop-filter: blur(20px) saturate(140%);
  -webkit-tap-highlight-color: transparent;
}

.video-minimized:active {
  cursor: grabbing;
}

.video-minimized:focus-visible {
  outline: 2px solid rgba(255, 255, 255, 0.85);
  outline-offset: 3px;
}

.video-minimized:hover {
  transform: scale(1.03);
  box-shadow: 0 14px 34px rgba(0, 0, 0, 0.46), inset 0 1px 0 rgba(255, 255, 255, 0.12);
}

.minimized-avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: var(--card);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  font-size: 18px;
  font-weight: 600;
  color: #333;
  flex-shrink: 0;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.minimized-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.minimized-status {
  display: flex;
  flex-direction: column;
  gap: 1px;
  min-width: 0;
}

.minimized-name {
  max-width: 88px;
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  line-height: 1.3;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.minimized-duration {
  color: rgba(255, 255, 255, 0.68);
  font-size: 11px;
  line-height: 1.2;
  font-variant-numeric: tabular-nums;
}

@media (max-width: 480px) {
  .video-chat-window {
    right: 0 !important;
    left: 0 !important;
    top: 0 !important;
    width: 100% !important;
    height: 100% !important;
    /* 桌面端用 left/top 50% + translate(-50%,-50%) 居中；
       移动端改成 inset 0 后必须清掉百分比位移，否则整块会被平移出屏幕一半 */
    transform: none !important;
    border-radius: 0 !important;
  }

  .video-container {
    border-radius: 0;
  }

  .video-header {
    padding: calc(14px + env(safe-area-inset-top)) 14px 34px;
  }

  .video-username {
    max-width: 190px;
    font-size: 16px;
  }

  .video-call-duration {
    padding: 5px 9px;
    font-size: 12px;
  }

  .signal-banner {
    top: calc(76px + env(safe-area-inset-top));
    max-width: calc(100% - 24px);
  }

  .signal-banner-copy strong {
    font-size: 12px;
  }

  .signal-banner-copy span {
    font-size: 10.5px;
  }

  /* 移动端关闭额外毛玻璃，避免小尺寸圆角被 backdrop-filter 裁成方角 */
  .video-status-message,
  .video-call-duration {
    background: rgba(0, 0, 0, 0.5);
    backdrop-filter: none;
    -webkit-backdrop-filter: none;
  }

  .control-bar {
    gap: 10px;
    padding: 8px;
    width: auto;
    background: rgba(18, 18, 22, 0.74);
    backdrop-filter: none;
    -webkit-backdrop-filter: none;
  }

  .local-video-container {
    width: 92px;
    height: 124px;
    bottom: 104px;
    right: 12px;
    border-radius: 14px;
  }

  .video-controls {
    padding: 38px 14px calc(14px + env(safe-area-inset-bottom));
  }

  .control-btn {
    width: 44px;
    height: 44px;
  }

  .hangup-btn {
    width: 56px;
    height: 56px;
  }

  .btn-icon {
    width: 21px;
    height: 21px;
  }

  .hangup-icon {
    width: 23px;
    height: 23px;
  }
}

@media (max-height: 480px) and (orientation: landscape) {
  .video-chat-window {
    right: 0 !important;
    left: 0 !important;
    top: 0 !important;
    width: 100% !important;
    height: 100% !important;
    transform: none !important;
    border-radius: 0 !important;
  }

  .video-container {
    border-radius: 0;
  }

  .video-header {
    padding: calc(10px + env(safe-area-inset-top)) 18px 22px;
  }

  .video-username {
    max-width: 46vw;
    font-size: 16px;
  }

  .video-status-message {
    margin-top: 5px;
  }

  .video-call-duration {
    padding: 5px 9px;
    font-size: 12px;
  }

  .signal-banner {
    top: calc(60px + env(safe-area-inset-top));
    padding: 7px 12px 7px 11px;
    border-radius: 12px;
  }

  .video-status-message,
  .video-call-duration {
    background: rgba(0, 0, 0, 0.5);
    backdrop-filter: none;
    -webkit-backdrop-filter: none;
  }

  .local-video-container {
    width: 104px;
    height: 140px;
    bottom: 92px;
    right: 18px;
    border-radius: 14px;
  }

  .video-controls {
    padding: 24px 18px calc(10px + env(safe-area-inset-bottom));
  }

  .control-bar {
    gap: 12px;
    padding: 9px;
    background: rgba(18, 18, 22, 0.74);
    backdrop-filter: none;
    -webkit-backdrop-filter: none;
  }

  .control-btn {
    width: 42px;
    height: 42px;
  }

  .hangup-btn {
    width: 54px;
    height: 54px;
  }

  .btn-icon {
    width: 20px;
    height: 20px;
  }

  .hangup-icon {
    width: 22px;
    height: 22px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .control-btn,
  .video-minimized {
    transition: none;
  }

  .status-dot {
    animation: none;
  }
}
</style>
