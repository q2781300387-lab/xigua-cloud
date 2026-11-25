<template>
  <div class="gc-page" @keydown.esc="close">
    <div class="gc-head">
      <div>
        <div class="gc-title">{{ groupName || '群通话' }}</div>
        <div class="gc-sub">{{ video ? '群视频' : '群语音' }} · {{ peers.length + 1 }}/4 人</div>
      </div>
      <div class="gc-head-right">
        <span class="gc-dot"></span>
        <span class="gc-timer">{{ callTimer }}</span>
      </div>
    </div>

    <div class="gc-grid" :class="'gc-grid-' + Math.min(4, peers.length + 1)">
      <div class="gc-tile gc-local">
        <video ref="localVideo" autoplay playsinline muted :class="{ 'gc-mirror': !sharing, 'gc-hidden': !video || !cameraOn }"></video>
        <div v-if="!video || !cameraOn" class="gc-avatar">{{ initial(myName) }}</div>
        <div class="gc-name">{{ myName }}（我）</div>
      </div>
      <div v-for="p in peers" :key="p.userId" class="gc-tile">
        <video :id="'gc-remote-' + p.userId" autoplay playsinline :class="{ 'gc-hidden': !video || !p.hasVideo }"></video>
        <div v-if="!video || !p.hasVideo" class="gc-avatar">{{ initial(p.username) }}</div>
        <div class="gc-name">{{ p.username }}</div>
      </div>
    </div>

    <div v-if="statusText" class="gc-status">{{ statusText }}</div>

    <div class="gc-controls">
      <button class="gc-btn" :class="{ active: !muted }" @click="toggleMute" :title="muted ? '取消静音' : '静音'">
        <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
          <rect x="9" y="2.5" width="6" height="12" rx="3" />
          <path d="M5.5 11.5a6.5 6.5 0 0 0 13 0" />
          <path d="M12 18v3.5" />
          <path v-if="muted" d="M3 3l18 18" />
        </svg>
        <span>{{ muted ? '已静音' : '麦克风' }}</span>
      </button>
      <button v-if="video" class="gc-btn" :class="{ active: cameraOn }" @click="toggleCamera" :title="cameraOn ? '关闭摄像头' : '打开摄像头'">
        <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
          <rect x="2" y="6" width="13" height="12" rx="2.5" />
          <path d="M22 8.5l-6 3.5 6 3.5z" />
          <path v-if="!cameraOn" d="M2 20l20-16" />
        </svg>
        <span>{{ cameraOn ? '摄像头' : '已关闭' }}</span>
      </button>
      <button v-if="video" class="gc-btn" :class="{ active: sharing }" @click="toggleScreenShare" :title="sharing ? '停止共享' : '共享屏幕'">
        <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
          <rect x="2" y="4" width="20" height="13" rx="2" />
          <path d="M8 20h8M12 17v3" />
        </svg>
        <span>{{ sharing ? '停止共享' : '共享屏幕' }}</span>
      </button>
      <button class="gc-btn gc-hangup" @click="close" title="离开通话">
        <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
          <path d="M4 14c4.5-4 11.5-4 16 0" />
          <path d="M6.5 12.5l-1.5 3 3 1.5" />
          <path d="M17.5 12.5l1.5 3-3 1.5" />
        </svg>
        <span>离开</span>
      </button>
    </div>
  </div>
</template>

<script>
import {getObject} from '@/utils/localStorage.js'
import {getToken} from '@/utils/auth'
import {
  startGroupCall, joinGroupCall, getGroupCallState,
  leaveGroupCall, sendGroupCallSignal, pollGroupCallSignals
} from '@/api/group.js'
import { ElMessage } from '@/plugins/message'

export default {
  name: 'GroupCallWindow',
  props: {
    groupId: { type: [String, Number], required: true },
    groupName: { type: String, default: '' },
    video: { type: Boolean, default: false }
  },
  emits: ['close'],
  data() {
    return {
      myId: '',
      myName: '我',
      callId: '',
      myJoinedAtValue: 0,
      peers: [],
      localStream: null,
      cameraTrack: null,
      screenStream: null,
      sharing: false,
      muted: false,
      cameraOn: this.video,
      statusText: '正在连接…',
      startedAt: 0,
      elapsed: 0,
      timerId: null,
      stateTimer: null,
      signalTimer: null,
      closed: false
    }
  },
  computed: {
    callTimer() {
      const s = Math.max(0, this.elapsed)
      const m = String(Math.floor(s / 60)).padStart(2, '0')
      const sec = String(s % 60).padStart(2, '0')
      return `${m}:${sec}`
    }
  },
  created() {
    this.pcMap = {}
    this.pendingCandidates = {}
    this.remoteStreamMap = {}
  },
  async mounted() {
    window.addEventListener('keydown', this.onKeydown)
    window.addEventListener('beforeunload', this.leaveOnUnload)
    const me = getObject('userInfo') || {}
    this.myId = String(me.id || me.userId || '')
    this.myName = me.username || me.nickname || '我'
    try {
      await this.initMedia()
      await this.enterRoom()
      this.statusText = ''
      this.startedAt = Date.now()
      this.timerId = setInterval(() => { this.elapsed = Math.floor((Date.now() - this.startedAt) / 1000) }, 1000)
      this.stateTimer = setInterval(() => this.refreshState(), 2000)
      this.signalTimer = setInterval(() => this.pollSignals(), 700)
    } catch (e) {
      this.statusText = e.message || '无法开始群通话'
      ElMessage.error(this.statusText)
      this.close(true)
    }
  },
  beforeUnmount() {
    this.cleanup()
  },
  methods: {
    initial(name) {
      return String(name || '?').trim().slice(0, 1).toUpperCase()
    },
    onKeydown(e) {
      if (e.key === 'Escape') this.close()
    },
    leaveOnUnload() {
      if (!this.callId || this.closed) return
      const token = getToken()
      try {
        fetch(`/client/group/call/leave?groupId=${encodeURIComponent(this.groupId)}&callId=${encodeURIComponent(this.callId)}`, {
          method: 'POST',
          headers: token ? { 'xigua-token': token } : {},
          keepalive: true,
          credentials: 'include'
        }).catch(() => {})
      } catch (e) {}
    },
    unwrap(res) {
      if (!res) throw new Error('接口无响应')
      if (res.code !== 200) throw new Error(res.msg || res.message || '接口返回失败')
      return res.data
    },
    async initMedia() {
      if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
        throw new Error('当前浏览器不支持音视频通话')
      }
      const constraints = this.video
        ? { audio: true, video: { width: { ideal: 640 }, height: { ideal: 480 }, facingMode: 'user' } }
        : { audio: true }
      this.localStream = await navigator.mediaDevices.getUserMedia(constraints)
      this.cameraTrack = this.localStream.getVideoTracks()[0] || null
      this.$nextTick(this.bindLocal)
    },
    bindLocal() {
      if (this.$refs.localVideo && this.localStream) {
        this.$refs.localVideo.srcObject = this.localStream
      }
    },
    async enterRoom() {
      let room = this.unwrap(await startGroupCall(this.groupId, this.video))
      const inRoom = (room.participants || []).some(p => String(p.userId) === this.myId)
      if (!inRoom) {
        room = this.unwrap(await joinGroupCall(this.groupId, room.callId))
      }
      this.callId = room.callId
      this.applyRoom(room)
      await this.ensureConnections()
    },
    applyRoom(room) {
      if (!room) return
      const me = (room.participants || []).find(p => String(p.userId) === this.myId)
      if (me) this.myJoinedAtValue = me.joinedAt || 0
      const list = (room.participants || []).filter(p => String(p.userId) !== this.myId)
      this.peers = list.map(p => {
        const id = String(p.userId)
        const old = this.peers.find(x => String(x.userId) === id)
        const stream = this.remoteStreamMap[id] || (old ? old.stream : null)
        return {
          userId: id,
          username: p.username || '群成员',
          avatar: p.avatar || '',
          joinedAt: p.joinedAt || 0,
          stream,
          hasVideo: stream ? stream.getVideoTracks().length > 0 : (old ? old.hasVideo : true)
        }
      })
      this.$nextTick(this.bindRemoteStreams)
    },
    bindRemoteStreams() {
      this.peers.forEach(p => {
        const el = document.getElementById('gc-remote-' + p.userId)
        if (el && p.stream && el.srcObject !== p.stream) {
          el.srcObject = p.stream
        }
      })
    },
    async refreshState() {
      if (this.closed || !this.callId) return
      try {
        const room = this.unwrap(await getGroupCallState(this.groupId))
        if (!room || !room.callId || room.callId !== this.callId) {
          this.statusText = '通话已结束'
          this.close(true)
          return
        }
        const stillIn = (room.participants || []).some(p => String(p.userId) === this.myId)
        if (!stillIn) {
          this.statusText = '你已离开通话'
          this.close(true)
          return
        }
        this.applyRoom(room)
        await this.ensureConnections()
      } catch (e) {
        // 网络抖动时下一轮继续
      }
    },
    createPeer(userId) {
      if (this.pcMap[userId]) return this.pcMap[userId]
      const pc = new RTCPeerConnection({ iceServers: [{ urls: 'stun:stun.l.google.com:19302' }] })
      this.pcMap[userId] = pc
      if (this.localStream) {
        this.localStream.getTracks().forEach(t => pc.addTrack(t, this.localStream))
      }
      pc.onicecandidate = (ev) => {
        if (ev.candidate) {
          this.sendSignal(userId, 'ice', ev.candidate.toJSON())
        }
      }
      pc.ontrack = (ev) => {
        const stream = ev.streams && ev.streams[0]
        if (!stream) return
        this.remoteStreamMap[String(userId)] = stream
        const peer = this.peers.find(p => String(p.userId) === String(userId))
        if (peer) {
          peer.stream = stream
          peer.hasVideo = stream.getVideoTracks().length > 0
          this.peers = [...this.peers]
          this.$nextTick(this.bindRemoteStreams)
        }
      }
      pc.onconnectionstatechange = () => {
        if (pc.connectionState === 'failed') {
          try { pc.restartIce() } catch (e) {}
        }
      }
      return pc
    },
    async ensureConnections() {
      if (this.closed) return
      const myJoinedAt = this.myJoinedAt()
      for (const peer of this.peers) {
        if (this.pcMap[peer.userId]) continue
        // 新加入者主动向老成员发 offer，老成员等待，避免双向 glare
        if (myJoinedAt > (peer.joinedAt || 0)) {
          await this.offer(peer.userId)
        }
      }
    },
    myJoinedAt() {
      return this.myJoinedAtValue || 0
    },
    async offer(userId) {
      if (this.closed) return
      const pc = this.createPeer(userId)
      const offer = await pc.createOffer()
      await pc.setLocalDescription(offer)
      this.sendSignal(userId, 'offer', offer)
    },
    async sendSignal(targetUserId, type, payload) {
      try {
        await sendGroupCallSignal({
          groupId: this.groupId,
          callId: this.callId,
          targetUserId: String(targetUserId),
          type,
          payload
        })
      } catch (e) {
        // 对方可能已离会
      }
    },
    async pollSignals() {
      if (this.closed || !this.callId) return
      let list = []
      try {
        list = this.unwrap(await pollGroupCallSignals(this.groupId, this.callId)) || []
      } catch (e) {
        return
      }
      for (const sig of list) {
        try {
          await this.handleSignal(sig)
        } catch (e) {
          // 单个信令失败不影响其他成员
        }
      }
    },
    async handleSignal(sig) {
      const from = String(sig.fromUserId || sig.userId || '')
      if (!from || from === this.myId) return
      if (sig.type === 'leave') {
        this.removePeer(from)
        return
      }
      const pc = this.createPeer(from)
      if (sig.type === 'offer') {
        if (pc.signalingState !== 'stable') {
          try { await pc.setLocalDescription({ type: 'rollback' }) } catch (e) {}
        }
        await pc.setRemoteDescription(new RTCSessionDescription(sig.payload))
        await this.flushCandidates(from)
        const answer = await pc.createAnswer()
        await pc.setLocalDescription(answer)
        await this.sendSignal(from, 'answer', answer)
      } else if (sig.type === 'answer') {
        if (pc.signalingState === 'have-local-offer') {
          await pc.setRemoteDescription(new RTCSessionDescription(sig.payload))
          await this.flushCandidates(from)
        }
      } else if (sig.type === 'ice') {
        if (!pc.remoteDescription || !pc.remoteDescription.type) {
          this.pendingCandidates[from] = this.pendingCandidates[from] || []
          this.pendingCandidates[from].push(sig.payload)
        } else {
          await pc.addIceCandidate(new RTCIceCandidate(sig.payload))
        }
      }
    },
    async flushCandidates(userId) {
      const pc = this.pcMap[userId]
      const list = this.pendingCandidates[userId] || []
      this.pendingCandidates[userId] = []
      for (const c of list) {
        try { await pc.addIceCandidate(new RTCIceCandidate(c)) } catch (e) {}
      }
    },
    removePeer(userId) {
      const key = String(userId)
      if (this.pcMap[key]) {
        try { this.pcMap[key].close() } catch (e) {}
        delete this.pcMap[key]
      }
      this.peers = this.peers.filter(p => String(p.userId) !== key)
    },
    toggleMute() {
      if (!this.localStream) return
      this.muted = !this.muted
      this.localStream.getAudioTracks().forEach(t => { t.enabled = !this.muted })
    },
    toggleCamera() {
      if (!this.localStream) return
      this.cameraOn = !this.cameraOn
      this.localStream.getVideoTracks().forEach(t => { t.enabled = this.cameraOn })
    },
    async toggleScreenShare() {
      if (!this.video) return
      if (this.sharing) {
        this.stopScreenShare()
        return
      }
      try {
        this.screenStream = await navigator.mediaDevices.getDisplayMedia({ video: { frameRate: 15 }, audio: false })
        const screenTrack = this.screenStream.getVideoTracks()[0]
        for (const id of Object.keys(this.pcMap)) {
          const sender = this.pcMap[id].getSenders().find(s => s.track && s.track.kind === 'video')
          if (sender) await sender.replaceTrack(screenTrack)
        }
        this.sharing = true
        this.statusText = '正在共享屏幕'
        screenTrack.onended = () => this.stopScreenShare()
        if (this.$refs.localVideo) this.$refs.localVideo.srcObject = this.screenStream
      } catch (e) {
        if (e && e.name === 'NotAllowedError') return
        ElMessage.error('无法共享屏幕')
      }
    },
    stopScreenShare() {
      this.sharing = false
      if (this.screenStream) {
        this.screenStream.getTracks().forEach(t => t.stop())
        this.screenStream = null
      }
      const cam = this.cameraTrack
      for (const id of Object.keys(this.pcMap)) {
        const sender = this.pcMap[id].getSenders().find(s => s.track && s.track.kind === 'video')
        if (sender && cam) {
          sender.replaceTrack(cam).catch(() => {})
          cam.enabled = this.cameraOn
        }
      }
      this.statusText = ''
      this.$nextTick(this.bindLocal)
    },
    async close(silent = false) {
      if (this.closed) return
      const wasClosedByRemote = silent === true
      this.closed = true
      this.cleanup()
      if (!wasClosedByRemote && this.callId) {
        try { await leaveGroupCall(this.groupId, this.callId) } catch (e) {}
      }
      this.$emit('close')
    },
    cleanup() {
      this.closed = true
      window.removeEventListener('keydown', this.onKeydown)
      window.removeEventListener('beforeunload', this.leaveOnUnload)
      if (this.timerId) clearInterval(this.timerId)
      if (this.stateTimer) clearInterval(this.stateTimer)
      if (this.signalTimer) clearInterval(this.signalTimer)
      this.timerId = this.stateTimer = this.signalTimer = null
      Object.keys(this.pcMap || {}).forEach(id => {
        try { this.pcMap[id].close() } catch (e) {}
      })
      this.pcMap = {}
      if (this.localStream) {
        this.localStream.getTracks().forEach(t => t.stop())
        this.localStream = null
      }
      if (this.screenStream) {
        this.screenStream.getTracks().forEach(t => t.stop())
        this.screenStream = null
      }
    }
  }
}
</script>

<style scoped>
.gc-page {
  position: fixed;
  inset: 0;
  z-index: 10000;
  display: flex;
  flex-direction: column;
  background: radial-gradient(1200px 700px at 50% 0%, #23262d 0%, #0e1013 62%, #08090b 100%);
  color: #fff;
  padding: 22px 26px 20px;
  box-sizing: border-box;
}
.gc-head { display: flex; justify-content: space-between; align-items: center; }
.gc-title { font-size: 19px; font-weight: 650; letter-spacing: .2px; }
.gc-sub { font-size: 12px; color: rgba(255,255,255,.52); margin-top: 4px; }
.gc-head-right { display: flex; align-items: center; gap: 8px; font-variant-numeric: tabular-nums; color: rgba(255,255,255,.72); font-size: 13px; }
.gc-dot { width: 7px; height: 7px; border-radius: 50%; background: #34c759; box-shadow: 0 0 0 4px rgba(52,199,89,.12); }
.gc-grid {
  flex: 1;
  min-height: 0;
  margin-top: 18px;
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  align-content: center;
}
.gc-grid-1 { grid-template-columns: minmax(0, 1fr); max-width: 760px; width: 100%; margin-left: auto; margin-right: auto; }
.gc-grid-2 { grid-template-columns: repeat(2, minmax(0, 1fr)); max-width: 1100px; width: 100%; margin-left: auto; margin-right: auto; }
.gc-tile {
  position: relative;
  border-radius: 18px;
  overflow: hidden;
  background: linear-gradient(145deg, #2a2d34, #17191d);
  border: 1px solid rgba(255,255,255,.08);
  box-shadow: 0 18px 45px rgba(0,0,0,.28);
  min-height: 180px;
}
.gc-tile video { width: 100%; height: 100%; object-fit: cover; display: block; background: #101216; }
.gc-tile video.gc-mirror { transform: scaleX(-1); }
.gc-tile video.gc-hidden { visibility: hidden; }
.gc-avatar {
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
  font-size: 40px;
  font-weight: 600;
  color: rgba(255,255,255,.9);
  background: linear-gradient(145deg, #343941, #202329);
}
.gc-name {
  position: absolute;
  left: 14px;
  bottom: 12px;
  font-size: 12.5px;
  padding: 5px 10px;
  border-radius: 999px;
  background: rgba(0,0,0,.42);
  backdrop-filter: blur(8px);
  max-width: calc(100% - 28px);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.gc-local { outline: 1px solid rgba(255,255,255,.16); }
.gc-status {
  text-align: center;
  color: rgba(255,255,255,.6);
  font-size: 12.5px;
  padding: 7px 0 0;
}
.gc-controls {
  display: flex;
  justify-content: center;
  gap: 14px;
  padding: 18px 0 4px;
}
.gc-btn {
  width: 74px;
  height: 64px;
  border: 0;
  border-radius: 16px;
  background: rgba(255,255,255,.1);
  color: rgba(255,255,255,.82);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 5px;
  font-size: 11px;
  cursor: pointer;
  transition: background .16s, color .16s, transform .16s;
  backdrop-filter: blur(10px);
}
.gc-btn:hover { background: rgba(255,255,255,.16); transform: translateY(-1px); }
.gc-btn.active { color: #fff; }
.gc-btn.active svg { opacity: 1; }
.gc-btn svg { opacity: .9; }
.gc-hangup { background: #e5484d; color: #fff; }
.gc-hangup:hover { background: #f2555a; }
@media (max-width: 720px) {
  .gc-page { padding: 16px 14px 12px; }
  .gc-grid { grid-template-columns: 1fr; gap: 8px; }
  .gc-grid-1, .gc-grid-2 { grid-template-columns: 1fr; }
  .gc-tile { min-height: 120px; }
  .gc-btn { width: 64px; height: 58px; }
}
</style>
