<template>
  <div 
  class="voice-chat-window" 
  v-if="showVoiceWindow" 
  :class="{ 'minimized': isMinimized }"
  @mousedown.prevent="startDrag"
  :style="!isMinimized ? { ...windowPos, width: connectionStatus === 'connected' ? '330px' : '300px' } : ''"
>
  <!-- 横向语音窗口 -->
  <div class="voice-horizontal-container" v-if="!isMinimized">
      <!-- 用户信息部分 -->
      <div class="voice-user-info">
        <div class="voice-avatar">
          <img 
            v-if="caller.avatar" 
            :src="caller.avatar" 
            :alt="caller.name || '未知用户'" 
            class="avatar-img"
          >
          <span v-else>{{ caller.name ? caller.name.charAt(0).toUpperCase() : 'U' }}</span>
        </div>
        <div class="voice-user-details">
          <div class="voice-username">{{ caller.name || '未知用户' }}</div>
          <div class="voice-status-message" :class="statusClass">
            <span class="status-dot" :class="connectionStatus"></span>
            {{ statusMessage }}
          </div>
        </div>
      </div>

      <!-- 通话时长 -->
      <div class="voice-call-duration" v-if="connectionStatus === 'connected'">
        {{ formatDuration(currentDuration) }}
      </div>

      <!-- 控制按钮区域 -->
      <div class="voice-controls-horizontal">
        <!-- 麦克风开关 -->
        <button
            class="control-btn mic-btn"
            :class="{ 'muted': isMicMuted }"
            @click="toggleMicrophone"
            :disabled="isProcessing"
            title="{{ isMicMuted ? '开启麦克风' : '静音' }}"
        >
          <img
              :src="isMicMuted ? closeMic : openMic"
              :alt="isMicMuted ? '关闭麦克风' : '开启麦克风'"
              class="btn-icon"
          />
        </button>

        <!-- 挂断 / 取消按钮 -->
        <button
            class="control-btn hangup-btn"
            @click="onHangupAction"
            :disabled="isProcessing"
            title="{{ connectionStatus === 'connected' ? '挂断通话' : '取消呼叫' }}"
        >
          <img 
            src="/src/static/icons/tel.png" 
            alt="{{ connectionStatus === 'connected' ? '挂断通话' : '取消呼叫' }}" 
            class="btn-icon hangup-icon"
          >
        </button>

        <!-- 扬声器开关 -->
        <button
            class="control-btn speaker-btn"
            :class="{ 'muted': isSpeakerMuted }"
            @click="toggleSpeaker"
            :disabled="isProcessing"
            title="{{ isSpeakerMuted ? '开启扬声器' : '关闭扬声器' }}"
        >
          <img
              :src="isSpeakerMuted ? closeSpeaker : openSpeaker"
              :alt="isSpeakerMuted ? '关闭扬声器' : '开启扬声器'"
              class="btn-icon"
          >
        </button>

        <!-- 最小化按钮 -->
        <button
            class="control-btn minimize-btn"
            @click="minimizeWindow"
            :disabled="isProcessing"
            title="最小化"
        >
          <img src="/src/static/icons/minimize.png" alt="最小化" class="btn-icon">
        </button>
      </div>
    </div>

    <!-- 最小化悬浮气泡 -->
    <div
        class="voice-minimized"
        v-if="isMinimized"
        :style="{ left: minimizedPos.x + 'px', top: minimizedPos.y + 'px' }"
        @mousedown.prevent="startDrag"
        @click="handleBubbleClick"
        :class="{ 'connected': connectionStatus === 'connected', 'dragging': isDragging }"
    >
      <!-- 状态指示器 -->
      <div class="minimized-status" :class="connectionStatus">
        <div class="status-dot"></div>
      </div>

      <!-- 内容区 -->
      <div class="minimized-content">
        <span class="minimized-name">{{ caller.name || '通话' }}</span>
        <span class="minimized-duration">{{ formatDuration(currentDuration) }}</span>
      </div>

      <!-- 控制按钮 -->
      <div class="minimized-actions">
        <button class="minimized-close" @click.stop="onMinimizedAction" title="结束通话">
          ×
        </button>
      </div>
    </div>
  </div>
</template>

<script>
import openSpeaker from '@/static/icons/open_speaker.png'
import closeSpeaker from '@/static/icons/close_speaker.png'
import openMic from '@/static/icons/open_mic.png'
import closeMic from '@/static/icons/close_mic.png'
import { getWebSocketClient } from "@/ws/WebSocketClient.js";
import { getObject } from "@/utils/localStorage.js";
import { storeToRefs } from "pinia";
import { useVoiceWindowStore } from "@/store/voiceWindow.js";

export default {
  name: 'VoiceChatWindow',

  props: {
    showVoiceWindow: {
      type: Boolean,
      default: false
    }
  },

  data() {
    return {
      // 用户信息
      currentUser: {},
      caller: {
        callId: null,
        userId: null,
        name: '',
        avatar: ''
      },

      // 窗口状态
      isMinimized: false,
      isProcessing: false,

      // 通话icon
      openSpeaker,
      closeSpeaker,
      openMic,
      closeMic,

      // 窗口位置和拖动
      windowPos: {
        top: '20px',
        left: '50%',
        transform: 'translateX(-50%)'
      },
      minimizedPos: {
        x: document.documentElement.clientWidth - 150,
        y: document.documentElement.clientHeight - 100
      },
      isDragging: false,
      dragOffset: {
        x: 0,
        y: 0
      },
      lastClickTime: 0,

      // 通话状态
      connectionStatus: 'connecting', // connecting, connected, disconnected
      isMicMuted: false,
      isSpeakerMuted: false,
      callType: 'outgoing',

      // 通话数据
      currentDuration: 0,
      callId: null,

      // WebRTC相关
      peerConnection: null,
      mediaStream: null,
      remoteAudio: null,
      iceCandidates: [],

      // 状态信息
      statusMessage: '正在连接...',
      statusClass: 'status-connecting',

      // 定时器
      durationTimer: null,
      callTimeoutTimer: null,

      // WebSocket监听器
      wsListener: null,

      // STUN服务器
      iceServers: [
        { urls: 'stun:stun.l.google.com:19302' },
        { urls: 'stun:stun1.l.google.com:19302' }
      ]
    }
  },

  watch: {
    showVoiceWindow(newVal) {
      if (newVal) {
        this.initializeVoiceChat();
      } else {
        this.cleanup();
      }
    }
  },

  mounted() {
    this.setupWebSocketListener();
  },

  beforeDestroy() {
    this.cleanup();
  },

  methods: {
    // 初始化语音聊天
    async initializeVoiceChat() {
      try {
        this.currentUser = getObject('userInfo') || {};
        const store = useVoiceWindowStore();
        const { callType, caller } = storeToRefs(store);

        this.callType = callType.value;
        this.caller = caller.value || { userId: '', username: '', avatar: '' };
        this.callId = this.caller.callId || null;

        // 确保WebSocket监听器正确设置
        this.setupWebSocketListener();

        if (this.callType === 'outgoing') {
          await this.initiateOutgoingCall();
        } else if (this.callType === 'incoming') {
          await this.handleIncomingCall();
        }

      } catch (error) {
        console.error('初始化失败:', error);
        this.statusMessage = '初始化失败';
        this.statusClass = 'status-disconnected';
      }
    },

    // 设置WebSocket监听
    setupWebSocketListener() {
      if (this.wsListener) {
        getWebSocketClient().unsubscribe(this.wsListener);
      }

      this.wsListener = (message) => {
        try {
          const data = JSON.parse(message);
          this.handleWebSocketMessage(data);
        } catch (error) {
          console.error('解析消息失败:', error);
        }
      };

      getWebSocketClient().subscribe(this.wsListener);
    },

    // 处理WebSocket消息
    handleWebSocketMessage(data) {
      if (!data.messageType || data.messageType !== 'audio_call') return;

      // 确保callId和caller信息正确设置
      if (data.callId) {
        this.callId = data.callId;
      }
      
      // 如果是呼叫请求，更新caller信息
      if (data.subType === 'audio_call_request') {
        this.caller = {
          ...this.caller,
          userId: data.senderId,
          callId: data.callId
        };
      }

      switch (data.subType) {
        case 'audio_call_accepted':
          this.handleCallAccepted(data);
          break;
        case 'audio_call_rejected':
          this.handleCallRejected(data);
          break;
        case 'audio_call_timeout':
          this.handleCallTimeout(data);
          break;
        case 'audio_call_offer':
          this.handleOffer(data);
          break;
        case 'audio_call_answer':
          this.handleAnswer(data);
          break;
        case 'audio_call_ice_candidate':
          this.handleIceCandidate(data);
          break;
        case 'audio_call_ended':
          this.handleCallEnded(data);
          break;
        case 'audio_call_request':
          // 呼叫请求已在上面处理
          break;
      }
    },

    // 发起通话
    async initiateOutgoingCall() {
      try {
        // 清除之前可能存在的超时计时器
        this.clearCallTimeoutTimer();
        
        this.statusMessage = '正在呼叫...';
        this.isProcessing = true;

        // 确保callId存在
        if (!this.callId) {
          this.callId = 'call_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);
          this.caller.callId = this.callId;
        }

        await this.getUserMedia();
        await this.createPeerConnection();

        this.sendWebSocketMessage({
          messageType: 'audio_call',
          subType: 'audio_call_request',
          senderId: this.currentUser.id,
          receiverId: this.caller.userId,
          callId: this.callId,
          timestamp: Date.now()
        });

        this.isProcessing = false;
        this.statusMessage = '等待对方接听...';
        
        // 设置10秒超时计时器
        this.callTimeoutTimer = setTimeout(() => {
          if (this.connectionStatus !== 'connected') {
            this.statusMessage = '对方未接听';
            this.statusClass = 'status-disconnected';
            this.closeVoiceWindow();
          }
        }, 10000);

      } catch (error) {
        console.error('呼叫失败:', error);
        this.statusMessage = '呼叫失败';
        this.statusClass = 'status-disconnected';
        this.isProcessing = false;
        this.clearCallTimeoutTimer();
      }
    },

    // 处理来电
    async handleIncomingCall() {
      try {
        this.statusMessage = '正在建立连接...';
        this.isProcessing = true;

        await this.getUserMedia();
        await this.createPeerConnection();

        this.isProcessing = false;
        this.statusMessage = '等待对方建立连接...';

      } catch (error) {
        console.error('建立连接失败:', error);
        this.statusMessage = '连接失败';
        this.isProcessing = false;
      }
    },

    // 获取音频流
    async getUserMedia() {
      try {
        const constraints = {
          audio: {
            echoCancellation: true,
            noiseSuppression: true
          }
        };

        this.mediaStream = await navigator.mediaDevices.getUserMedia(constraints);
      } catch (error) {
        console.error('获取音频流失败:', error);
        throw error;
      }
    },

    // 创建PeerConnection
    async createPeerConnection() {
      try {
        const configuration = {
          iceServers: this.iceServers
        };

        this.peerConnection = new RTCPeerConnection(configuration);

        if (this.mediaStream) {
          const audioTrack = this.mediaStream.getAudioTracks()[0];
          if (audioTrack) {
            this.peerConnection.addTrack(audioTrack, this.mediaStream);
          }
        }

        this.setupPeerConnectionListeners();

      } catch (error) {
        console.error('创建PeerConnection失败:', error);
        throw error;
      }
    },

    // 设置PeerConnection监听器
    setupPeerConnectionListeners() {
      this.peerConnection.onicecandidate = (event) => {
        if (event.candidate) {
          this.sendWebSocketMessage({
            messageType: 'audio_call',
            subType: 'audio_call_ice_candidate',
            senderId: this.currentUser.id,
            receiverId: this.caller.userId,
            message: {
              candidate: event.candidate.candidate,
              sdpMid: event.candidate.sdpMid || '0',
              sdpMLineIndex: event.candidate.sdpMLineIndex || 0,
              usernameFragment: event.candidate.usernameFragment
            },
            callId: this.callId,
            timestamp: Date.now()
          });
        }
      };

      this.peerConnection.ontrack = (event) => {
        if (event.streams && event.streams[0]) {
          if (!this.remoteAudio) {
            this.remoteAudio = new Audio();
            this.remoteAudio.autoplay = true;
          }
          this.remoteAudio.srcObject = event.streams[0];
        }
      };

      this.peerConnection.oniceconnectionstatechange = () => {
        const state = this.peerConnection.iceConnectionState;

        if (state === 'connected' || state === 'completed') {
          this.connectionStatus = 'connected';
          this.statusMessage = '通话中';
          this.statusClass = 'status-connected';
          this.startCallTimer();
        } else if (state === 'disconnected' || state === 'failed' || state === 'closed') {
          this.connectionStatus = 'disconnected';
          this.statusMessage = '连接断开';
          this.statusClass = 'status-disconnected';
          this.stopCallTimer();
        }
      };
    },

    // 处理Offer
    async handleOffer(data) {
      try {
        // 确保callId正确设置
        if (data.callId) {
          this.callId = data.callId;
        }
        
        // 确保caller.userId正确设置
        if (data.senderId && !this.caller.userId) {
          this.caller.userId = data.senderId;
        }

        if (!this.peerConnection) {
          await this.getUserMedia();
          await this.createPeerConnection();
        }

        let offer;
        if (typeof data.message === 'string') {
          offer = JSON.parse(data.message);
        } else if (data.message && data.message.sdp) {
          offer = data.message;
        } else {
          offer = data;
        }

        const rtcOffer = new RTCSessionDescription({
          type: 'offer',
          sdp: offer.sdp || offer
        });

        await this.peerConnection.setRemoteDescription(rtcOffer);
        const answer = await this.peerConnection.createAnswer();
        await this.peerConnection.setLocalDescription(answer);

        this.sendWebSocketMessage({
          messageType: 'audio_call',
          subType: 'audio_call_answer',
          senderId: this.currentUser.id,
          receiverId: this.caller.userId,
          message: answer,
          callId: this.callId,
          timestamp: Date.now()
        });

        this.statusMessage = '正在建立连接...';

      } catch (error) {
        console.error('处理Offer失败:', error);
        this.statusMessage = '建立连接失败';
      }
    },

    // 处理Answer
    async handleAnswer(data) {
      try {
        if (!this.peerConnection) return;

        // 确保callId正确设置
        if (data.callId) {
          this.callId = data.callId;
        }

        let answer;
        if (typeof data.message === 'string') {
          answer = JSON.parse(data.message);
        } else if (data.message && data.message.sdp) {
          answer = data.message;
        } else {
          answer = data;
        }

        const rtcAnswer = new RTCSessionDescription({
          type: 'answer',
          sdp: answer.sdp || answer
        });

        await this.peerConnection.setRemoteDescription(rtcAnswer);

        // 连接状态更新
        this.connectionStatus = 'connected';
        this.statusMessage = '通话中';
        this.statusClass = 'status-connected';
        this.startCallTimer();

        for (const candidate of this.iceCandidates) {
          await this.peerConnection.addIceCandidate(new RTCIceCandidate(candidate));
        }
        this.iceCandidates = [];
      } catch (error) {
        console.error('处理Answer失败:', error);
        this.statusMessage = '建立连接失败';
      }
    },

    // 处理ICE候选
    async handleIceCandidate(data) {
      try {
        // 确保callId正确设置
        if (data.callId) {
          this.callId = data.callId;
        }
        
        let candidateData;

        if (typeof data.message === 'string') {
          try {
            candidateData = JSON.parse(data.message);
          } catch (parseError) {
            console.error('解析ICE候选JSON失败:', parseError);
            return;
          }
        } else {
          candidateData = data.message;
        }

        if (!candidateData || !candidateData.candidate) {
          console.warn('无效的ICE候选数据:', candidateData);
          return;
        }

        if (!this.peerConnection) {
          this.iceCandidates.push(candidateData);
          return;
        }

        if (this.peerConnection.signalingState !== 'stable') {
          this.iceCandidates.push(candidateData);
          return;
        }

        try {
          const iceCandidate = new RTCIceCandidate({
            candidate: candidateData.candidate,
            sdpMid: candidateData.sdpMid || '0',
            sdpMLineIndex: candidateData.sdpMLineIndex || 0,
            usernameFragment: candidateData.usernameFragment
          });
          await this.peerConnection.addIceCandidate(iceCandidate);

          if (this.iceCandidates.length > 0) {
            for (const savedCandidate of [...this.iceCandidates]) {
              try {
                const savedIceCandidate = new RTCIceCandidate({
                  candidate: savedCandidate.candidate,
                  sdpMid: savedCandidate.sdpMid || '0',
                  sdpMLineIndex: savedCandidate.sdpMLineIndex || 0,
                  usernameFragment: savedCandidate.usernameFragment
                });
                await this.peerConnection.addIceCandidate(savedIceCandidate);
              } catch (e) {
                console.error('添加保存的ICE候选失败:', e);
              }
            }
            this.iceCandidates = [];
          }

        } catch (error) {
          console.error('添加ICE候选失败:', error);
          if (error.message.includes('already have remote description')) {
            this.iceCandidates.push(candidateData);
          }
        }

      } catch (error) {
        console.error('处理ICE候选失败:', error);
      }
    },

    // 处理通话接受
    async handleCallAccepted(data) {
      try {
        // 清除超时计时器
        this.clearCallTimeoutTimer();
        
        // 确保callId正确设置
        if (data.callId) {
          this.callId = data.callId;
        }
        
        // 确保caller.userId正确设置
        if (data.senderId) {
          this.caller.userId = data.senderId;
        }
        
        console.log('收到通话接受消息:', data);
        console.log('当前callId:', this.callId);
        console.log('当前caller:', this.caller);
        
        this.statusMessage = '对方已接听，正在建立连接...';
        this.isProcessing = true;

        // 确保获取音频流和创建PeerConnection
        if (!this.mediaStream) {
          await this.getUserMedia();
        }
        
        if (!this.peerConnection) {
          await this.createPeerConnection();
        }

        console.log('开始创建offer...');
        const offer = await this.peerConnection.createOffer();
        console.log('创建offer成功:', offer);
        
        await this.peerConnection.setLocalDescription(offer);
        console.log('设置本地描述成功');

        const offerMessage = {
          messageType: 'audio_call',
          subType: 'audio_call_offer',
          senderId: this.currentUser.id,
          receiverId: this.caller.userId,
          message: offer,
          callId: this.callId,
          timestamp: Date.now()
        };
        
        console.log('发送offer消息:', offerMessage);
        this.sendWebSocketMessage(offerMessage);

        this.isProcessing = false;
        this.statusMessage = '正在建立连接...';

      } catch (error) {
        console.error('发送Offer失败:', error);
        this.statusMessage = '建立连接失败';
        this.statusClass = 'status-disconnected';
        this.isProcessing = false;
        this.clearCallTimeoutTimer();
      }
    },

    // 处理通话拒绝
    handleCallRejected(data) {
      // 清除超时计时器
      this.clearCallTimeoutTimer();
      
      this.statusMessage = '对方已拒绝';
      this.statusClass = 'status-disconnected';
      this.connectionStatus = 'disconnected';

      setTimeout(() => {
        this.closeVoiceWindow();
      }, 2000);
    },

    // 被叫方 10 秒未接听
    handleCallTimeout(data) {
      // 清除超时计时器
      this.clearCallTimeoutTimer();

      this.statusMessage = '对方超时未接听';
      this.statusClass = 'status-disconnected';
      this.connectionStatus = 'disconnected';

      setTimeout(() => {
        this.closeVoiceWindow();
      }, 2000);
    },

    // 处理通话结束
    handleCallEnded(data) {
      // 清除超时计时器
      this.clearCallTimeoutTimer();
      
      this.statusMessage = '对方已挂断';
      this.statusClass = 'status-disconnected';
      this.connectionStatus = 'disconnected';

      setTimeout(() => {
        this.closeVoiceWindow();
      }, 2000);
    },
    
    // 清除呼叫超时计时器
    clearCallTimeoutTimer() {
      if (this.callTimeoutTimer) {
        clearTimeout(this.callTimeoutTimer);
        this.callTimeoutTimer = null;
      }
    },

    // 发送WebSocket消息
    sendWebSocketMessage(message) {
      try {
        const wsClient = getWebSocketClient();
        const socketInstance = wsClient.getInstance();
        
        console.log('准备发送WebSocket消息:', message);
        console.log('WebSocket连接状态:', socketInstance ? socketInstance.readyState : '未初始化');
        
        if (socketInstance && socketInstance.readyState === WebSocket.OPEN) {
          wsClient.sendMessage(JSON.stringify(message));
          console.log('WebSocket消息发送成功');
        } else {
          console.error('WebSocket未连接，无法发送消息');
          // 尝试重新连接WebSocket
          wsClient.connect();
          // 延迟发送消息
          setTimeout(() => {
            wsClient.sendMessage(JSON.stringify(message));
            console.log('WebSocket重连后发送消息');
          }, 1000);
        }
      } catch (error) {
        console.error('发送WebSocket消息失败:', error);
      }
    },

    // 开始通话计时
    startCallTimer() {
      this.currentDuration = 0;
      this.durationTimer = setInterval(() => {
        this.currentDuration++;
      }, 1000);
    },

    // 停止通话计时
    stopCallTimer() {
      if (this.durationTimer) {
        clearInterval(this.durationTimer);
        this.durationTimer = null;
      }
    },

    // 切换麦克风
    toggleMicrophone() {
      this.isMicMuted = !this.isMicMuted;
      if (this.mediaStream) {
        const audioTrack = this.mediaStream.getAudioTracks()[0];
        if (audioTrack) {
          audioTrack.enabled = !this.isMicMuted;
        }
      }
    },

    // 切换扬声器
    toggleSpeaker() {
      this.isSpeakerMuted = !this.isSpeakerMuted;
      if (this.remoteAudio) {
        this.remoteAudio.muted = this.isSpeakerMuted;
      }
    },

    onHangupAction() {
      if (this.connectionStatus === 'connected') {
        this.hangUp();
      } else {
        this.cancelCall();
      }
    },

    onMinimizedAction() {
      if (this.connectionStatus === 'connected') {
        this.hangUp();
      } else {
        this.cancelCall();
      }
    },

    // 挂断通话
    async hangUp() {
      try {
        this.isProcessing = true;
        this.statusMessage = '正在挂断...';

        this.sendWebSocketMessage({
          messageType: 'audio_call',
          subType: 'audio_call_ended',
          senderId: this.currentUser.id,
          receiverId: this.caller.userId,
          callId: this.callId,
          timestamp: Date.now(),
          reason: 'user_hangup'
        });

        // 直接关闭窗口，不显示重置后的状态
        this.closeVoiceWindow();
        
        // 清理资源
        this.cleanup();

      } catch (error) {
        console.error('挂断失败:', error);
        this.isProcessing = false;
      }
    },

    // 取消呼叫
    cancelCall() {
      try {
        this.isProcessing = true;
        this.statusMessage = '已取消呼叫';

        this.sendWebSocketMessage({
          messageType: 'audio_call',
          subType: 'audio_call_canceled',
          senderId: this.currentUser.id,
          receiverId: this.caller.userId,
          callId: this.callId,
          timestamp: Date.now()
        });

        // 直接关闭窗口，不显示重置后的状态
        this.closeVoiceWindow();
        
        // 清理资源
        this.cleanup();
      } catch (e) {
        console.error('取消呼叫失败:', e);
      }
    },

    // 最小化窗口
    minimizeWindow() {
      this.isMinimized = true;
    },

    // 恢复窗口
    restoreWindow() {
      this.isMinimized = false;
    },

    // 关闭窗口
    closeVoiceWindow() {
      const store = useVoiceWindowStore();
      store.closeVoiceWindow();
    },

    // 清理资源
    cleanup() {
      this.stopCallTimer();
      this.clearCallTimeoutTimer();

      if (this.mediaStream) {
        this.mediaStream.getTracks().forEach(track => track.stop());
        this.mediaStream = null;
      }

      if (this.peerConnection) {
        this.peerConnection.close();
        this.peerConnection = null;
      }

      if (this.remoteAudio) {
        this.remoteAudio.srcObject = null;
        this.remoteAudio = null;
      }

      this.iceCandidates = [];

      if (this.wsListener) {
        getWebSocketClient().unsubscribe(this.wsListener);
        this.wsListener = null;
      }

      // 重置通话状态
      this.connectionStatus = 'disconnected';
      this.statusMessage = '通话已结束';
      this.statusClass = 'status-disconnected';
      this.currentDuration = 0;
      this.isProcessing = false;

      // 重置音频状态
      this.isMicMuted = false;
      this.isSpeakerMuted = false;

      // 重置最小化相关状态
      this.isMinimized = false;
      this.isDragging = false;
      this.dragOffset = { x: 0, y: 0 };
      this.lastClickTime = 0;

      // 重置最小化位置到默认值
      this.minimizedPos = {
        x: document.documentElement.clientWidth - 150,
        y: document.documentElement.clientHeight - 100
      };

      // 清理残留的拖动事件监听器（安全处理）
      try {
        document.removeEventListener('mousemove', this.onDrag);
        document.removeEventListener('mouseup', this.stopDrag);
        document.body.classList.remove('no-select');
      } catch (error) {
        // 忽略错误，可能监听器不存在
      }

      // 重置呼叫相关状态
      this.callId = null;
      this.caller = {
        userId: '',
        username: '',
        avatar: ''
      };
    },

    // 格式化时长
    formatDuration(seconds) {
      const mins = Math.floor(seconds / 60);
      const secs = seconds % 60;
      return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
    },

    // 气泡点击事件
    handleBubbleClick(e) {
      if (!this.isDragging && this.lastClickTime && Date.now() - this.lastClickTime < 300) {
        this.restoreWindow();
      } else {
        this.lastClickTime = Date.now();
      }
    },

    // 开始拖动
    startDrag(e) {
      if (e.button !== 0) return;

      e.preventDefault();

      // 防止文本选中
      document.body.classList.add('no-select');
      this.isDragging = true;
      
      if (this.isMinimized) {
        this.dragOffset.x = e.clientX - this.minimizedPos.x;
        this.dragOffset.y = e.clientY - this.minimizedPos.y;
      } else {
        const rect = e.target.closest('.voice-chat-window').getBoundingClientRect();
        this.dragOffset.x = e.clientX - rect.left;
        this.dragOffset.y = e.clientY - rect.top;
      }

      document.addEventListener('mousemove', this.onDrag);
      document.addEventListener('mouseup', this.stopDrag);
    },

    // 停止拖动
    stopDrag(e) {
      this.isDragging = false;
      document.body.classList.remove('no-select');
      document.removeEventListener('mousemove', this.onDrag);
      document.removeEventListener('mouseup', this.stopDrag);
    },

    // 拖动处理
    onDrag(e) {
      if (!this.isDragging) return;

      if (this.isMinimized) {
        this.minimizedPos.x = e.clientX - this.dragOffset.x;
        this.minimizedPos.y = e.clientY - this.dragOffset.y;
      } else {
        const x = e.clientX - this.dragOffset.x;
        const y = e.clientY - this.dragOffset.y;
        
        // 确保窗口在屏幕范围内
        const maxX = window.innerWidth - (this.connectionStatus === 'connected' ? 330 : 300); // 动态窗口宽度
        const maxY = window.innerHeight - 60; // 60 是窗口高度
        
        this.windowPos = {
          top: `${Math.max(0, Math.min(y, maxY))}px`,
          left: `${Math.max(0, Math.min(x, maxX))}px`,
          transform: 'none'
        };
      }
    }
  }
}
</script>

<style scoped>
/* 防止文本选中 */
.no-select {
  user-select: none !important;
  -webkit-user-select: none !important;
  -moz-user-select: none !important;
  -ms-user-select: none !important;
}

.voice-chat-window {
  position: fixed;
  top: 20px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 1000;
  overflow: hidden;
  cursor: move;
  border-radius: 24px;
}

/* 横向语音窗口容器 */
.voice-horizontal-container {
  width: 100%;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  display: flex;
  align-items: center;
  padding: 6px 10px;
  gap: 8px;
  border: 1px solid var(--separator);
  overflow: hidden;
  backdrop-filter: blur(10px);
}

/* 用户信息部分 */
.voice-user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.voice-avatar {
  width: 32px;
  height: 32px;
  background: linear-gradient(135deg, #90caf9 0%, #64b5f6 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-weight: 600;
  font-size: 14px;
  flex-shrink: 0;
  overflow: hidden;
}

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 50%;
}

.voice-user-details {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.voice-username {
  font-size: 12px;
  font-weight: 500;
  color: #333;
}

.voice-status-message {
  font-size: 10px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  display: inline-block;
}

.status-dot.connecting {
  background: #ffa500;
  animation: pulse 1.5s infinite;
}

.status-dot.connected {
  background: #07C160;
}

.status-dot.disconnected {
  background: #f56c6c;
}

.status-connecting {
  color: #ffa500;
}

.status-connected {
  color: #07C160;
}

.status-disconnected {
  color: #f56c6c;
}

/* 通话时长 */
.voice-call-duration {
  flex: 1;
  text-align: center;
  font-size: 11px;
  font-weight: 500;
  color: #666;
  font-family: 'SF Mono', Monaco, monospace;
}

/* 横向控制按钮区域 */
.voice-controls-horizontal {
  display: flex;
  align-items: center;
  gap: 4px;
}

.control-btn {
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.7);
  color: #333;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  font-size: 14px;
  user-select: none;
}

.control-btn:hover:not(:disabled) {
  background: var(--bg-blur-strong);
  transform: scale(1.05);
}

.control-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.control-btn.muted {
  background: rgba(255, 236, 179, 0.7);
  color: #ff9800;
}

.hangup-btn {
  background: #ff8a80;
  color: white;
}

.hangup-btn:hover:not(:disabled) {
  background: #ff7961;
  transform: scale(1.05);
}

/* 按钮图标样式 */
.btn-icon {
  width: 16px;
  height: 16px;
  filter: invert(1);
}

/* 挂断图标旋转效果 */
.hangup-icon {
  transform: rotate(135deg);
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

/* 最小化气泡样式 */
.voice-chat-window.minimized {
  width: auto !important;
  height: auto !important;
  background: transparent !important;
  border: none !important;
  box-shadow: none !important;
  transform: none !important;
  top: auto !important;
  left: auto !important;
  pointer-events: none;
}

.voice-minimized {
  position: fixed;
  z-index: 9999;
  background: rgba(255, 255, 255, 0.95);
  color: #333;
  padding: 6px 10px;
  border-radius: 24px;
  display: flex;
  align-items: center;
  gap: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  cursor: move;
  font-size: 12px;
  white-space: nowrap;
  pointer-events: auto;
  transition: all 0.25s ease;
  border: 1px solid var(--separator);
  backdrop-filter: blur(10px);
  min-width: 120px;
  max-width: 180px;
  user-select: none;
}

/* 通话中样式 */
.voice-minimized.connected {
  background: rgba(7, 193, 96, 0.05);
  border-color: rgba(7, 193, 96, 0.2);
}

.voice-minimized:hover {
  box-shadow:
      0 4px 20px rgba(0, 0, 0, 0.15),
      0 0 1px rgba(0, 0, 0, 0.1);
  transform: translateY(-1px);
}

.voice-minimized.dragging {
  opacity: 0.7;
  cursor: grabbing !important;
}

/* 状态指示器 */
.minimized-status {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.minimized-status .status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  transition: all 0.3s ease;
}

/* 连接中 */
.minimized-status.connecting .status-dot {
  background: #ff9800;
  animation: pulse 1.5s infinite;
}

/* 已连接 */
.minimized-status.connected .status-dot {
  background: #07C160;
  box-shadow: 0 0 0 2px rgba(7, 193, 96, 0.2);
}

/* 断开连接 */
.minimized-status.disconnected .status-dot {
  background: #9e9e9e;
}

/* 内容区域 */
.minimized-content {
  display: flex;
  flex-direction: column;
  gap: 1px;
  flex: 1;
  min-width: 0;
  overflow: hidden;
}

.minimized-name {
  font-weight: 500;
  font-size: 12px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  color: #333;
}

.minimized-duration {
  font-size: 11px;
  font-family: 'SF Mono', Monaco, monospace;
  color: #666;
  letter-spacing: -0.2px;
}

/* 控制按钮 */
.minimized-actions {
  margin-left: 4px;
  opacity: 0.8;
  transition: opacity 0.2s ease;
}

.voice-minimized:hover .minimized-actions {
  opacity: 1;
}

.minimized-close {
  background: #f8d7da;
  border: none;
  color: #dc3545;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  font-size: 12px;
  font-weight: 500;
  line-height: 1;
  padding: 0;
  transition: all 0.2s ease;
}

.minimized-close:hover {
  background: #f5c6cb;
  color: #dc3545;
  transform: scale(1.05);
}

/* 响应式 */
@media (max-width: 600px) {
  .voice-horizontal-container {
    width: 90vw;
    padding: 10px 12px;
    gap: 12px;
  }
  
  .voice-avatar {
    width: 36px;
    height: 36px;
    font-size: 14px;
  }
  
  .voice-username {
    font-size: 13px;
  }
  
  .voice-status-message {
    font-size: 11px;
  }
  
  .voice-call-duration {
    font-size: 13px;
  }
  
  .control-btn {
    width: 36px;
    height: 36px;
    font-size: 14px;
  }
}

@media (max-width: 480px) {
  .voice-minimized {
    padding: 6px 10px;
    font-size: 12px;
    min-width: 100px;
  }

  .minimized-name {
    font-size: 11px;
  }

  .minimized-duration {
    font-size: 10px;
  }
}
</style>
