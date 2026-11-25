<template>
	<view class="group-call-page">
		<view class="gc-top safe-top-bar">
			<view class="gc-back" @click="hangup">
				<text class="gc-back-icon">‹</text>
			</view>
			<view class="gc-heading">
				<text class="gc-title">{{ groupName || '群通话' }}</text>
				<text class="gc-sub">{{ video ? '群视频' : '群语音' }} · {{ peerList.length + 1 }}/4 人 · {{ timerText }}</text>
			</view>
		</view>

		<view class="gc-body">
			<view class="gc-grid" :class="'gc-grid-' + Math.min(4, peerList.length + 1)">
				<view class="gc-tile gc-local">
					<video
						id="gc-local-video"
						class="gc-video"
						:class="{ 'gc-hidden': !video || !cameraOn }"
						:controls="false"
						autoplay
						playsinline
						muted
						object-fit="cover"
					></video>
					<view v-if="!video || !cameraOn" class="gc-avatar">{{ initial(myName) }}</view>
					<view class="gc-name">{{ myName }}（我）</view>
					<view v-if="muted" class="gc-muted-tag">静音</view>
				</view>

				<view v-for="p in peerList" :key="p.userId" class="gc-tile">
					<video
						:id="'gc-remote-' + p.userId"
						class="gc-video"
						:class="{ 'gc-hidden': !video || !p.hasVideo }"
						:controls="false"
						autoplay
						playsinline
						object-fit="cover"
					></video>
					<view v-if="!video || !p.hasVideo" class="gc-avatar">{{ initial(p.username) }}</view>
					<view class="gc-name">{{ p.username }}</view>
				</view>
			</view>

			<text v-if="statusText" class="gc-status">{{ statusText }}</text>
		</view>

		<view class="gc-controls safe-bottom-bar">
			<view class="gc-control" :class="{ active: !muted }" @click="toggleMute">
				<image class="gc-control-icon" :src="muted ? '/static/icons/call/mic.svg' : '/static/icons/call/mic.svg'" mode="aspectFit"></image>
				<text class="gc-control-label">{{ muted ? '已静音' : '麦克风' }}</text>
			</view>
			<view v-if="video" class="gc-control" :class="{ active: cameraOn }" @click="toggleCamera">
				<image class="gc-control-icon" src="/static/icons/call/camera.svg" mode="aspectFit"></image>
				<text class="gc-control-label">{{ cameraOn ? '摄像头' : '已关闭' }}</text>
			</view>
			<view class="gc-control gc-hangup" @click="hangup">
				<image class="gc-control-icon" src="/static/icons/call/hangup.svg" mode="aspectFit"></image>
				<text class="gc-control-label">挂断</text>
			</view>
		</view>
	</view>
</template>

<script>
import {
	startGroupCall, joinGroupCall, getGroupCallState,
	leaveGroupCall, sendGroupCallSignal, pollGroupCallSignals
} from '@/api/group.js'

const ICE_CONFIG = { iceServers: [{ urls: 'stun:stun.l.google.com:19302' }] }

export default {
	data() {
		return {
			groupId: '',
			groupName: '',
			video: false,
			myId: '',
			myName: '我',
			callId: '',
			myJoinedAt: 0,
			peerList: [],
			pcMap: {},
			pendingCandidates: {},
			remoteStreamMap: {},
			localStream: null,
			cameraTrack: null,
			muted: false,
			cameraOn: false,
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
		timerText() {
			const s = Math.max(0, this.elapsed)
			const m = String(Math.floor(s / 60)).padStart(2, '0')
			const sec = String(s % 60).padStart(2, '0')
			return `${m}:${sec}`
		}
	},
	onLoad(options) {
		this.groupId = options.groupId || ''
		this.groupName = decodeURIComponent(options.name || '')
		this.video = String(options.video || '0') === '1'
		this.cameraOn = this.video
		const storedUser = uni.getStorageSync('userInfo') || {}
		let me = storedUser
		if (typeof me === 'string') {
			try { me = JSON.parse(me) } catch (e) { me = {} }
		}
		this.myId = String(me.id || me.userId || '')
		this.myName = me.username || me.nickname || '我'
		if (typeof window !== 'undefined') {
			this._pagehideHandler = () => this.leaveOnPageHide()
			window.addEventListener('pagehide', this._pagehideHandler)
		}
		this.init()
	},
	onUnload() {
		if (typeof window !== 'undefined' && this._pagehideHandler) {
			window.removeEventListener('pagehide', this._pagehideHandler)
		}
		if (!this.closed && this.callId) {
			try { leaveGroupCall(this.groupId, this.callId) } catch (e) {}
		}
		this.cleanup()
	},
	methods: {
		initial(name) {
			return String(name || '?').trim().slice(0, 1).toUpperCase()
		},
		unwrap(res) {
			if (!res) throw new Error('接口无响应')
			if (res.code !== 200) throw new Error(res.msg || res.message || '接口返回失败')
			return res.data
		},
		toast(title) {
			uni.showToast({ title, icon: 'none' })
		},
		async init() {
			if (!this.groupId) {
				this.toast('群信息不完整')
				return
			}
			try {
				await this.initMedia()
				await this.enterRoom()
				this.statusText = ''
				this.startedAt = Date.now()
				this.timerId = setInterval(() => {
					this.elapsed = Math.floor((Date.now() - this.startedAt) / 1000)
				}, 1000)
				this.stateTimer = setInterval(() => this.refreshState(), 2000)
				this.signalTimer = setInterval(() => this.pollSignals(), 700)
				this.$nextTick(() => this.bindAllStreams())
			} catch (e) {
				this.statusText = (e && (e.message || e.msg)) || '无法开始群通话'
				this.toast(this.statusText)
			}
		},
		async initMedia() {
			if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
				throw new Error('当前环境不支持音视频通话')
			}
			const constraints = this.video
				? { audio: true, video: { facingMode: 'user', width: { ideal: 640 }, height: { ideal: 480 } } }
				: { audio: true }
			this.localStream = await navigator.mediaDevices.getUserMedia(constraints)
			this.cameraTrack = this.localStream.getVideoTracks()[0] || null
		},
		findVideoEl(id) {
			// uni-app H5 的 video 组件会包一层容器，真正播放的 video 在内部
			if (typeof document === 'undefined') return null
			const host = document.getElementById(id)
			if (!host) return null
			if (host.tagName === 'VIDEO') return host
			return host.querySelector('video') || host
		},
		attachStream(id, stream) {
			const el = this.findVideoEl(id)
			if (!el || !stream) return
			try {
				if (el.srcObject !== stream) el.srcObject = stream
				el.autoplay = true
				el.playsInline = true
				el.muted = id === 'gc-local-video'
				const p = el.play()
				if (p && p.catch) p.catch(() => {})
			} catch (e) {}
		},
		bindAllStreams() {
			if (this.localStream) this.attachStream('gc-local-video', this.localStream)
			this.peerList.forEach(p => {
				if (p.stream) this.attachStream('gc-remote-' + p.userId, p.stream)
			})
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
			if (me) this.myJoinedAt = me.joinedAt || 0
			const list = (room.participants || []).filter(p => String(p.userId) !== this.myId)
			this.peerList = list.map(p => {
				const id = String(p.userId)
				const old = this.peerList.find(x => String(x.userId) === id)
				const stream = this.remoteStreamMap[id] || (old ? old.stream : null)
				return {
					userId: id,
					username: p.username || '群成员',
					joinedAt: p.joinedAt || 0,
					stream,
					hasVideo: stream ? stream.getVideoTracks().length > 0 : (old ? old.hasVideo : true)
				}
			})
			this.$nextTick(this.bindAllStreams)
		},
		async refreshState() {
			if (this.closed || !this.callId) return
			try {
				const room = this.unwrap(await getGroupCallState(this.groupId))
				if (!room || room.callId !== this.callId) {
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
			} catch (e) {}
		},
		createPeer(userId) {
			if (this.pcMap[userId]) return this.pcMap[userId]
			const pc = new RTCPeerConnection(ICE_CONFIG)
			this.pcMap[userId] = pc
			if (this.localStream) {
				this.localStream.getTracks().forEach(t => pc.addTrack(t, this.localStream))
			}
			pc.onicecandidate = (ev) => {
				if (ev.candidate) this.sendSignal(userId, 'ice', ev.candidate.toJSON())
			}
			pc.ontrack = (ev) => {
				const stream = ev.streams && ev.streams[0]
				if (!stream) return
				this.remoteStreamMap[String(userId)] = stream
				const peer = this.peerList.find(p => String(p.userId) === String(userId))
				if (peer) {
					peer.stream = stream
					peer.hasVideo = stream.getVideoTracks().length > 0
					this.peerList = [...this.peerList]
					this.$nextTick(() => this.bindAllStreams())
				}
			}
			return pc
		},
		async ensureConnections() {
			if (this.closed) return
			for (const peer of this.peerList) {
				if (this.pcMap[peer.userId]) continue
				// 后加入者主动向先加入者发 offer，避免双向 glare
				if (this.myJoinedAt > (peer.joinedAt || 0)) {
					await this.offer(peer.userId)
				}
			}
		},
		async offer(userId) {
			if (this.closed) return
			const pc = this.createPeer(userId)
			const offer = await pc.createOffer()
			await pc.setLocalDescription(offer)
			await this.sendSignal(userId, 'offer', offer)
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
			} catch (e) {}
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
				try { await this.handleSignal(sig) } catch (e) {}
			}
		},
		async handleSignal(sig) {
			const from = String(sig.fromUserId || '')
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
			this.peerList = this.peerList.filter(p => String(p.userId) !== key)
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
		leaveOnPageHide() {
			if (this.closed || !this.callId) return
			const token = uni.getStorageSync('xigua-token')
			try {
				fetch(`/client/group/call/leave?groupId=${encodeURIComponent(this.groupId)}&callId=${encodeURIComponent(this.callId)}`, {
					method: 'POST',
					headers: token ? { 'xigua-token': token } : {},
					keepalive: true,
					credentials: 'include'
				}).catch(() => {})
			} catch (e) {}
		},
		async hangup() {
			await this.close(false)
			uni.navigateBack({ delta: 1 })
		},
		async close(remoteClosed) {
			if (this.closed) return
			const wasRemote = remoteClosed === true
			this.closed = true
			this.cleanup()
			if (!wasRemote && this.callId) {
				try { await leaveGroupCall(this.groupId, this.callId) } catch (e) {}
			}
			if (wasRemote) {
				this.toast(this.statusText || '通话已结束')
				setTimeout(() => uni.navigateBack({ delta: 1 }), 800)
			}
		},
		cleanup() {
			this.closed = true
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
		}
	}
}
</script>

<style scoped>
.group-call-page {
	position: fixed;
	inset: 0;
	display: flex;
	flex-direction: column;
	background: radial-gradient(120% 80% at 50% 0%, #2a2d35 0%, #121417 58%, #08090b 100%);
	color: #fff;
	overflow: hidden;
}
.gc-top {
	display: flex;
	align-items: center;
	padding: 12px 16px 6px;
	flex-shrink: 0;
}
.gc-back {
	width: 40px;
	height: 40px;
	display: flex;
	align-items: center;
	justify-content: center;
	font-size: 26px;
	color: rgba(255,255,255,.8);
}
.gc-back-icon { line-height: 1; }
.gc-heading { margin-left: 4px; display: flex; flex-direction: column; }
.gc-title { font-size: 16px; font-weight: 600; }
.gc-sub { margin-top: 4px; font-size: 11px; color: rgba(255,255,255,.5); }
.gc-body { flex: 1; min-height: 0; padding: 10px 12px 0; display: flex; flex-direction: column; }
.gc-grid {
	flex: 1;
	min-height: 0;
	display: grid;
	gap: 8px;
	grid-template-columns: 1fr 1fr;
	align-content: center;
}
.gc-grid-1 { grid-template-columns: 1fr; }
.gc-tile {
	position: relative;
	border-radius: 16px;
	overflow: hidden;
	background: linear-gradient(145deg, #30343c, #1b1e22);
	border: 1px solid rgba(255,255,255,.08);
	min-height: 150px;
}
.gc-video { width: 100%; height: 100%; display: block; }
.gc-hidden { visibility: hidden; }
.gc-avatar {
	position: absolute;
	inset: 0;
	display: flex;
	align-items: center;
	justify-content: center;
	font-size: 34px;
	font-weight: 600;
	color: rgba(255,255,255,.9);
	background: linear-gradient(145deg, #3a3f47, #23262b);
}
.gc-name {
	position: absolute;
	left: 10px;
	bottom: 9px;
	max-width: calc(100% - 20px);
	padding: 4px 9px;
	border-radius: 999px;
	background: rgba(0,0,0,.45);
	font-size: 11px;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
}
.gc-muted-tag {
	position: absolute;
	top: 9px;
	right: 9px;
	padding: 3px 8px;
	border-radius: 999px;
	background: rgba(0,0,0,.5);
	font-size: 10px;
	color: rgba(255,255,255,.8);
}
.gc-local { outline: 1px solid rgba(255,255,255,.16); }
.gc-status { text-align: center; font-size: 12px; color: rgba(255,255,255,.6); padding: 7px 0 2px; }
.gc-controls {
	display: flex;
	justify-content: center;
	gap: 22px;
	padding: 16px 0 20px;
	flex-shrink: 0;
}
.gc-control {
	width: 62px;
	display: flex;
	flex-direction: column;
	align-items: center;
	gap: 6px;
}
.gc-control.active .gc-control-icon { opacity: 1; }
.gc-control-icon {
	width: 52px;
	height: 52px;
	border-radius: 50%;
	background: rgba(255,255,255,.1);
	padding: 13px;
	box-sizing: border-box;
	opacity: .78;
}
.gc-control-label { font-size: 11px; color: rgba(255,255,255,.6); }
.gc-hangup .gc-control-icon { background: #e5484d; opacity: 1; }
.gc-hangup .gc-control-label { color: rgba(255,255,255,.8); }
</style>
