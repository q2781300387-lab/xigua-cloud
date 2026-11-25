<template>
	<view class="call-page">
		<view id="remoteVideoHost" class="remote-surface"></view>
		<view class="video-vignette"></view>

		<view v-if="!incomingPending" class="call-header">
			<text class="peer-name">{{ peerName }}</text>
			<text class="call-status">{{ statusText }}</text>
		</view>

		<view v-if="showWaiting" class="waiting-layer">
			<view class="avatar-ring" :class="{ 'is-ringing': incomingPending }">
				<image v-if="peerAvatar && !avatarFailed" :src="peerAvatar" class="peer-avatar" mode="aspectFill" @error="avatarFailed = true" />
				<view v-else class="peer-avatar avatar-fallback">{{ avatarLetter }}</view>
			</view>
			<text v-if="incomingPending" class="waiting-name">{{ peerName }}</text>
			<text class="waiting-title">{{ waitTitle }}</text>
			<text class="waiting-desc">{{ waitDesc }}</text>
		</view>

		<view id="localVideoHost" class="local-surface" :class="{ 'is-hidden': !localReady }">
			<view v-if="localReady && !cameraOn" class="local-off">摄像头已关闭</view>
		</view>

		<view v-if="autoplayBlocked && !incomingPending && !endNotice && !fatalError" class="resume-layer" @click="resumePlayback">
			<text class="resume-text">轻触恢复通话画面</text>
		</view>

		<view v-if="audioBlocked && !autoplayBlocked && !incomingPending && !endNotice && !fatalError" class="audio-badge" @click="resumePlayback">
			<text class="audio-badge-text">轻触开启声音</text>
		</view>

		<view v-if="!speakerOn && !incomingPending && !endNotice" class="speaker-badge">对方声音已静音</view>

		<view v-if="!incomingPending && !endNotice && !fatalError" class="control-dock">
			<view class="control-row">
				<view class="control-item" hover-class="control-hover" @click="toggleMic">
					<view class="control-circle" :class="{ 'is-off': !micOn }">
						<image src="/static/icons/call/mic.svg" class="control-icon" mode="aspectFit" />
					</view>
					<text class="control-label">{{ micOn ? '麦克风' : '已静音' }}</text>
				</view>

				<view class="control-item" hover-class="control-hover" @click="toggleCamera">
					<view class="control-circle" :class="{ 'is-off': !cameraOn }">
						<image src="/static/icons/call/camera.svg" class="control-icon" mode="aspectFit" />
					</view>
					<text class="control-label">{{ cameraOn ? '摄像头' : '已关闭' }}</text>
				</view>

				<view class="control-item" hover-class="control-hover" @click="toggleSpeaker">
					<view class="control-circle" :class="{ 'speaker-off-state': !speakerOn }">
						<image :src="speakerOn ? '/static/icons/call/speaker.svg' : '/static/icons/call/speaker-off.svg'" class="control-icon" mode="aspectFit" />
					</view>
					<text class="control-label">{{ speakerOn ? '声音' : '已静音' }}</text>
				</view>

				<view v-if="cameraOn" class="control-item" hover-class="control-hover" @click="switchCamera">
					<view class="control-circle">
						<image src="/static/icons/call/switch-camera.svg" class="control-icon" mode="aspectFit" />
					</view>
					<text class="control-label">切换</text>
				</view>
			</view>

			<view class="hangup-wrap">
				<view class="hangup-btn" hover-class="hangup-hover" @click="hangup(false)">
					<image src="/static/icons/call/hangup.svg" class="hangup-icon" mode="aspectFit" />
				</view>
				<text class="hangup-label">挂断</text>
			</view>
		</view>

		<view v-if="incomingPending && !fatalError && !endNotice" class="incoming-actions">
			<view class="incoming-action">
				<view class="call-action-circle reject" hover-class="action-hover" @click="rejectCall">
					<image src="/static/icons/call/hangup.svg" class="action-icon rotate-hangup" mode="aspectFit" />
				</view>
				<text class="action-label">拒绝</text>
			</view>
			<view class="incoming-action">
				<view class="call-action-circle accept" hover-class="action-hover" @click="acceptCall">
					<image src="/static/icons/call/phone.svg" class="action-icon" mode="aspectFit" />
				</view>
				<text class="action-label">接听</text>
			</view>
		</view>

		<view v-if="endNotice" class="end-layer">
			<text class="end-title">{{ endNotice }}</text>
			<text class="end-desc">正在返回聊天...</text>
		</view>

		<view v-if="fatalError" class="error-layer">
			<view class="error-card">
				<view class="error-icon">!</view>
				<text class="error-title">无法继续通话</text>
				<text class="error-desc">{{ fatalError }}</text>
				<view class="error-btn" hover-class="error-btn-hover" @click="closePage">返回聊天</view>
			</view>
		</view>
	</view>
</template>

<script>
import { getWebSocketClient } from '@/utils/websocket.js'
import { normalizeMediaUrl } from '@/utils/media.js'
import {
	parseWsPayload,
	getCurrentUserId,
	createCallId,
	sendVideoSignal,
	setActiveCall,
	clearActiveCall,
	consumeCallIntent
} from '@/utils/videoCallBridge.js'
import {
	createLocalStream,
	createVideoOnlyStream,
	takePendingStream,
	releasePendingStream,
	stopStream,
	describeMediaError
} from '@/utils/videoCallMedia.js'
import {
	startIncomingRing,
	stopIncomingRing,
	startRingback,
	stopRingback,
	releaseCallAudio
} from '@/utils/callAudio.js'

const ICE_SERVERS = [
	{ urls: 'stun:stun.l.google.com:19302' },
	{ urls: 'stun:stun1.l.google.com:19302' },
	{
		// 自建 coturn，经 80 端口 TCP 复用接入（阿里云安全组仅放行 80/443，UDP 不可用）。
		urls: [
			'turn:watermelonim.eu.cc:80?transport=tcp',
			'turn:47.94.184.61:80?transport=tcp'
		],
		username: 'xigua',
		credential: 'xiguaTurn2026K7p9Q2m4Z8'
	}
]

const OUTGOING_TIMEOUT = 30000
const INCOMING_TIMEOUT = 30000
// 已接听但 ICE 迟迟没有成功时，不能一直停在“建立连接”状态。
const CONNECT_TIMEOUT = 20000

function decodeOption(value, fallback) {
	if (value == null || value === '') return fallback
	try {
		return decodeURIComponent(value)
	} catch (err) {
		return value
	}
}

function normalizeSdp(message) {
	if (!message) return null
	let value = message
	if (typeof value === 'string') {
		try {
			value = JSON.parse(value)
		} catch (err) {
			if (value.indexOf('v=0') === 0) return { sdp: value }
			return null
		}
	}
	if (typeof value === 'string') {
		return value.indexOf('v=0') === 0 ? { sdp: value } : null
	}
	if (value && value.sdp) return value
	return null
}

function normalizeCandidate(message) {
	if (!message) return null
	let value = message
	if (typeof value === 'string') {
		try {
			value = JSON.parse(value)
		} catch (err) {
			return null
		}
	}
	if (!value || typeof value !== 'object') return null
	if (!value.candidate && value.candidate !== '') return null
	return value
}

// 页面卸载/刷新时 this 上的部分方法已不可用，异常退出通知必须走模块级函数。
function signalCallExit(vm) {
	if (!vm || vm.finished || vm.hangupSignaled) return
	if (!vm.senderId || !vm.peerId) return
	vm.hangupSignaled = true
	sendVideoSignal(vm.peerAccepted || vm.everConnected ? 'video_call_ended' : 'video_call_canceled', {
		senderId: vm.senderId,
		receiverId: vm.peerId,
		callId: vm.callId,
		message: ''
	})
}

// 通话记录只由主叫写入一次，被叫通过正常聊天消息收到同一条记录，避免双方各写一条。
function sendCallRecord(vm, outcome) {
	if (!vm || vm.recordSent) return
	if (vm.mode !== 'outgoing' || !vm.senderId || !vm.peerId) return
	vm.recordSent = true
	const payload = JSON.stringify({
		kind: 'video',
		outcome,
		duration: outcome === 'completed' ? (Number(vm.duration) || 0) : 0
	})
	const message = {
		chatMessageId: '',
		senderId: vm.senderId,
		receiverId: vm.peerId,
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
	}
	try {
		const client = getWebSocketClient()
		if (client.isConnected && !client.isConnected()) client.connect()
		client.sendMessage(JSON.stringify(message))
	} catch (err) {
		console.warn('写入通话记录失败:', err)
	}
}

export default {
	data() {
		return {
			mode: 'outgoing',
			callId: '',
			peerId: '',
			peerName: '好友',
			peerAvatar: '',
			avatarFailed: false,
			senderId: '',
			phase: 'incoming',
			incomingPending: true,
			localReady: false,
			remoteReady: false,
			autoplayBlocked: false,
			audioBlocked: false,
			remoteAttachSeq: 0,
			speakerSeq: 0,
			micOn: true,
			cameraOn: true,
			speakerOn: true,
			frontCamera: true,
			duration: 0,
			fatalError: '',
			endNotice: '',
			localStream: null,
			remoteStream: null,
			pc: null,
			iceCandidates: [],
			remoteDescriptionReady: false,
			peerAccepted: false,
			everConnected: false,
			finished: false,
			requestSent: false,
			recordSent: false,
			wsListener: null,
			connectionListener: null,
			outgoingTimer: null,
			incomingTimer: null,
			connectTimer: null,
			durationTimer: null,
			returnTimer: null,
			restartTimer: null,
			vibrateTimer: null,
			iceRestartAttempts: 0,
			restartingIce: false,
			hangupSignaled: false,
			pageHideHandler: null,
			visibilityHandler: null,
			wakeLock: null
		}
	},
	computed: {
		showWaiting() {
			return !this.remoteReady && !this.endNotice && !this.fatalError
		},
		avatarLetter() {
			const name = String(this.peerName || '友').trim()
			return name ? name.slice(0, 1) : '友'
		},
		statusText() {
			if (this.endNotice) return this.endNotice
			if (this.fatalError) return '通话未建立'
			if (this.incomingPending) return '邀请你视频通话'
			if (this.phase === 'calling') return '正在呼叫...'
			if (this.phase === 'connecting') return '正在建立连接...'
			if (this.phase === 'connected') return '通话中 ' + this.formatDuration(this.duration)
			return '视频通话'
		},
		waitTitle() {
			if (this.incomingPending) return '视频通话邀请'
			if (this.phase === 'connected') return '等待对方画面'
			if (this.phase === 'connecting') return '正在连接'
			return '正在呼叫'
		},
		waitDesc() {
			if (this.incomingPending) return '对方邀请你进行视频通话'
			if (this.phase === 'connected') return '已接通，正在等待视频画面'
			if (this.phase === 'connecting') return '正在协商加密媒体连接'
			return '已发起通话请求，等待对方接听'
		}
	},
	onLoad(options) {
		const opts = options || {}
		this.mode = opts.mode === 'incoming' ? 'incoming' : 'outgoing'
		this.callId = opts.callId ? String(opts.callId) : ''
		this.peerId = opts.userId ? String(opts.userId) : ''
		this.peerName = decodeOption(opts.name, '好友')
		this.peerAvatar = normalizeMediaUrl(decodeOption(opts.avatar, ''))
		this.senderId = getCurrentUserId()
		this.incomingPending = this.mode === 'incoming'
		this.phase = this.incomingPending ? 'incoming' : 'calling'

		if (!this.senderId || !this.peerId) {
			this.failCall('登录状态或对方信息不完整，无法发起通话')
			return
		}

		// 只接受由聊天页或全局来电监听刚刚登记过的入口。
		// 刷新后模块内的待处理意图已丢失，就不会再按 URL 参数自动重拨。
		const intent = consumeCallIntent(this.callId, this.mode)
		if (!intent) {
			this.finished = true
			this.phase = 'ended'
			this.endNotice = this.mode === 'incoming' ? '来电已失效' : '通话已中断，请重新发起'
			try {
				this.stopIncomingFeedback()
			} catch (err) {}
			this.returnTimer = setTimeout(() => {
				try {
					this.closePage()
				} catch (err) {
					uni.redirectTo({ url: '/pages/chat/chat-list' })
				}
			}, 1600)
			return
		}

		if (typeof window !== 'undefined') {
			this.pageHideHandler = () => signalCallExit(this)
			window.addEventListener('pagehide', this.pageHideHandler)
		}

		if (typeof document !== 'undefined') {
			this.visibilityHandler = () => {
				if (document.visibilityState === 'visible' && !this.finished) {
					this.acquireWakeLock()
				}
			}
			document.addEventListener('visibilitychange', this.visibilityHandler)
		}

		setActiveCall({
			callId: this.callId,
			mode: this.mode,
			peerId: this.peerId
		})

		this.subscribeSignals()
		this.bindConnectionState()

		if (this.incomingPending) {
			this.startIncomingTimer()
			this.startIncomingFeedback()
			return
		}

		this.ensureLocalStream().then(() => {
			this.startOutgoingCall()
		}).catch((err) => {
			console.warn('获取本地媒体失败:', err)
			this.failCall(describeMediaError(err))
		})
	},
	guardStaleEntry() {
		this.finished = true
		this.phase = 'ended'
		this.endNotice = this.mode === 'incoming' ? '来电已失效' : '通话已中断，请重新发起'
		this.stopIncomingFeedback()
		this.returnTimer = setTimeout(() => {
			this.closePage()
		}, 1600)
	},
	signalAbnormalExit() {
		if (this.finished || this.hangupSignaled) return
		if (!this.senderId || !this.peerId) return
		this.hangupSignaled = true
		sendVideoSignal(this.peerAccepted || this.everConnected ? 'video_call_ended' : 'video_call_canceled', {
			senderId: this.senderId,
			receiverId: this.peerId,
			callId: this.callId,
			message: ''
		})
	},
	onReady() {
		this.initVideoElements()
		this.acquireWakeLock()
		if (this.localStream) this.attachLocalVideo()
		if (this.remoteStream) this.attachRemoteVideo()
	},
	onBackPress() {
		// 页面返回等同于挂断，避免返回键留下一个没有页面的幽灵通话。
		if (this.finished) return false
		this.hangup()
		return true
	},
	onUnload() {
		signalCallExit(this)
		this.clearAllTimers()
		this.teardown()
		clearActiveCall(this.callId)
	},
	beforeDestroy() {
		this.clearAllTimers()
		this.teardown()
	},
	methods: {
		subscribeSignals() {
			this.wsListener = (raw) => {
				const msg = parseWsPayload(raw)
				if (!msg || msg.messageType !== 'video_call') return
				if (msg.callId && String(msg.callId) !== String(this.callId)) return
				switch (msg.subType) {
					case 'video_call_accepted':
						this.handleAccepted(msg)
						break
					case 'video_call_rejected':
						this.handleRejected(msg)
						break
					case 'video_call_canceled':
						this.handleCanceled(msg)
						break
					case 'video_call_timeout':
						this.handleTimeout(msg)
						break
					case 'video_call_ended':
						this.handleEnded(msg)
						break
					case 'video_call_offer':
						this.handleOffer(msg)
						break
					case 'video_call_answer':
						this.handleAnswer(msg)
						break
					case 'video_call_ice_candidate':
						this.handleIceCandidate(msg)
						break
					default:
						break
				}
			}
			getWebSocketClient().subscribe(this.wsListener)
		},
		bindConnectionState() {
			const client = getWebSocketClient()
			if (!client.subscribeConnectionState) return
			this.connectionListener = (event) => {
				if (event && event.state === 'failed' && !this.finished) {
					this.failCall('信令连接已断开，请检查网络后重试')
				}
			}
			client.subscribeConnectionState(this.connectionListener)
		},
		async waitForSocket(timeout) {
			const client = getWebSocketClient()
			if (client.isConnected && client.isConnected()) return true
			client.connect()
			const limit = timeout || 4000
			const started = Date.now()
			return new Promise((resolve) => {
				const tick = () => {
					if (client.isConnected && client.isConnected()) {
						resolve(true)
						return
					}
					if (Date.now() - started >= limit) {
						resolve(false)
						return
					}
					setTimeout(tick, 150)
				}
				tick()
			})
		},
		async sendSignal(subType, message) {
			if (!this.senderId || !this.peerId) return false
			const ready = await this.waitForSocket(4000)
			if (!ready) return false
			return this.sendSignalNow(subType, message)
		},
		sendSignalNow(subType, message) {
			if (!this.senderId || !this.peerId) return false
			return sendVideoSignal(subType, {
				senderId: this.senderId,
				receiverId: this.peerId,
				callId: this.callId,
				message: message == null ? '' : message
			})
		},
		sendTerminalSignal(subType, message) {
			// 结束类信令一旦被异步等待拖住，页面可能先卸载，对端就收不到挂断。
			// 这里同步发出并记录，pagehide 不再重复发送。
			if (this.hangupSignaled) return false
			this.hangupSignaled = true
			return this.sendSignalNow(subType, message)
		},
		async acquireWakeLock() {
			if (this.finished || this.wakeLock) return
			if (typeof navigator === 'undefined' || !navigator.wakeLock || !navigator.wakeLock.request) return
			try {
				const sentinel = await navigator.wakeLock.request('screen')
				if (this.finished) {
					try { sentinel.release() } catch (err) {}
					return
				}
				this.wakeLock = sentinel
				sentinel.addEventListener('release', () => {
					if (this.wakeLock === sentinel) this.wakeLock = null
				})
			} catch (err) {}
		},
		releaseWakeLock() {
			if (!this.wakeLock) return
			try { this.wakeLock.release() } catch (err) {}
			this.wakeLock = null
		},
		async ensureLocalStream() {
			if (this.localStream) return
			const prepared = takePendingStream()
			if (prepared) {
				this.localStream = prepared
			} else {
				this.localStream = await createLocalStream('user')
			}
			this.localReady = true
			this.$nextTick(() => {
				this.attachLocalVideo()
			})
		},
		async startOutgoingCall() {
			this.phase = 'calling'
			const ok = await this.sendSignal('video_call_request', '')
			if (!ok) {
				this.failCall('信令服务未连接，视频通话请求没有发出')
				return
			}
			this.requestSent = true
			this.startRingbackTone()
			this.startOutgoingTimer()
			this.acquireWakeLock()
		},
		createPeerConnection() {
			if (this.pc) return this.pc
			if (typeof RTCPeerConnection === 'undefined') {
				this.failCall('当前浏览器不支持 WebRTC 视频通话')
				return null
			}
			const pc = new RTCPeerConnection({ iceServers: ICE_SERVERS })
			this.pc = pc

			if (this.localStream) {
				this.localStream.getTracks().forEach((track) => {
					pc.addTrack(track, this.localStream)
				})
			}

			pc.ontrack = (event) => {
				const stream = event.streams && event.streams[0]
					? event.streams[0]
					: new MediaStream([event.track])
				this.remoteStream = stream
				this.remoteReady = !!(stream && stream.getVideoTracks && stream.getVideoTracks().length)
				this.$nextTick(() => this.attachRemoteVideo())
			}

			pc.onicecandidate = (event) => {
				if (!event.candidate) return
				this.sendSignal('video_call_ice_candidate', JSON.stringify({
					candidate: event.candidate.candidate,
					sdpMid: event.candidate.sdpMid,
					sdpMLineIndex: event.candidate.sdpMLineIndex,
					usernameFragment: event.candidate.usernameFragment
				}))
			}

			pc.onconnectionstatechange = () => {
				const state = pc.connectionState
				if (state === 'connected') {
					this.markConnected()
					this.resetIceRestart()
				} else if (state === 'disconnected') {
					this.scheduleIceRestart()
				} else if (state === 'failed') {
					this.attemptIceRestart(true)
				}
			}

			pc.oniceconnectionstatechange = () => {
				const state = pc.iceConnectionState
				if (state === 'connected' || state === 'completed') {
					this.markConnected()
					this.resetIceRestart()
				} else if (state === 'disconnected') {
					this.scheduleIceRestart()
				} else if (state === 'failed') {
					this.attemptIceRestart(true)
				}
			}
			return pc
		},
		markConnected() {
			this.clearOutgoingTimer()
			this.clearIncomingTimer()
			this.clearConnectTimer()
			this.stopRingbackTone()
			if (!this.everConnected) {
				this.everConnected = true
				this.phase = 'connected'
				this.startDurationTimer()
			}
		},
		scheduleIceRestart() {
			if (this.finished || this.mode !== 'outgoing' || !this.peerAccepted) return
			if (this.restartTimer) return
			// 网络切换时先给 ICE 3 秒自愈窗口，仍不恢复再主动重启。
			this.restartTimer = setTimeout(() => {
				this.restartTimer = null
				const state = this.pc ? (this.pc.iceConnectionState || this.pc.connectionState) : ''
				if (state === 'disconnected' || state === 'failed') {
					this.attemptIceRestart(false)
				}
			}, 3000)
		},
		resetIceRestart() {
			if (this.restartTimer) {
				clearTimeout(this.restartTimer)
				this.restartTimer = null
			}
			this.iceRestartAttempts = 0
			this.restartingIce = false
		},
		attemptIceRestart(immediate) {
			if (this.finished || this.mode !== 'outgoing' || !this.peerAccepted) return
			if (!this.pc || this.restartingIce) return
			if (this.iceRestartAttempts >= 2) {
				this.failCall('网络连接已中断，请挂断后重新发起通话', true)
				return
			}
			if (!immediate) {
				this.scheduleIceRestart()
				return
			}
			this.iceRestartAttempts += 1
			this.restartingIce = true
			const pc = this.pc
			pc.createOffer({ iceRestart: true }).then(async (offer) => {
				if (this.finished || pc !== this.pc) return
				await pc.setLocalDescription(offer)
				await this.sendSignal('video_call_offer', JSON.stringify({
					type: offer.type,
					sdp: offer.sdp,
					iceRestart: true
				}))
				this.phase = this.everConnected ? 'connected' : 'connecting'
			}).catch((err) => {
				console.warn('ICE 重启失败:', err)
				this.restartingIce = false
				this.attemptIceRestart(true)
			})
		},
		async acceptCall() {
			if (!this.incomingPending || this.finished) return
			this.incomingPending = false
			this.phase = 'connecting'
			this.clearIncomingTimer()
			this.stopIncomingFeedback()
			this.acquireWakeLock()

			try {
				if (!this.localStream) {
					this.localStream = takePendingStream() || await createLocalStream('user')
					this.localReady = true
					this.$nextTick(() => this.attachLocalVideo())
				}
			} catch (err) {
				console.warn('接听时获取媒体失败:', err)
				this.sendTerminalSignal('video_call_rejected', '')
				this.failCall(describeMediaError(err))
				return
			}

			const pc = this.createPeerConnection()
			if (!pc) return
			this.peerAccepted = true
			const ok = await this.sendSignal('video_call_accepted', '')
			if (!ok) {
				this.failCall('信令服务未连接，无法接听通话')
				return
			}
			this.startConnectTimer()
		},
		rejectCall() {
			if (this.finished) return
			this.stopIncomingFeedback()
			this.sendTerminalSignal('video_call_rejected', '')
			this.finishWithNotice('已拒绝通话')
		},
			hangup() {
			if (this.finished) return
			sendCallRecord(this, this.peerAccepted || this.everConnected ? 'completed' : 'canceled')
			this.sendTerminalSignal(this.peerAccepted || this.everConnected ? 'video_call_ended' : 'video_call_canceled', '')
			this.finishWithNotice('通话已结束')
		},
		handleAccepted() {
			if (this.mode !== 'outgoing' || this.peerAccepted || this.finished) return
			this.peerAccepted = true
			this.stopRingbackTone()
			this.clearOutgoingTimer()
			this.phase = 'connecting'
			this.ensureLocalStream().then(async () => {
				const pc = this.createPeerConnection()
				if (!pc) return
				const offer = await pc.createOffer()
				await pc.setLocalDescription(offer)
				await this.sendSignal('video_call_offer', JSON.stringify({
					type: offer.type,
					sdp: offer.sdp
				}))
				this.startConnectTimer()
			}).catch((err) => {
				console.warn('生成Offer失败:', err)
				this.failCall('建立通话连接失败，请稍后重试')
			})
		},
		handleOffer(msg) {
			if (this.mode !== 'incoming' || this.incomingPending || this.finished) return
			const offer = normalizeSdp(msg.message)
			if (!offer || !this.pc) return
			this.pc.setRemoteDescription(new RTCSessionDescription({ type: 'offer', sdp: offer.sdp }))
				.then(async () => {
					this.remoteDescriptionReady = true
					await this.drainIceCandidates()
					const answer = await this.pc.createAnswer()
					await this.pc.setLocalDescription(answer)
					await this.sendSignal('video_call_answer', JSON.stringify({
						type: answer.type,
						sdp: answer.sdp
					}))
					if (!this.everConnected) this.phase = 'connecting'
				})
				.catch((err) => {
					console.warn('处理Offer失败:', err)
					this.failCall('建立通话连接失败，请稍后重试')
				})
		},
		handleAnswer(msg) {
			const answer = normalizeSdp(msg.message)
			if (!answer || !this.pc || this.pc.signalingState === 'stable') return
			const apply = async () => {
				await this.pc.setRemoteDescription(new RTCSessionDescription({ type: 'answer', sdp: answer.sdp }))
				this.remoteDescriptionReady = true
				await this.drainIceCandidates()
				this.restartingIce = false
				if (this.everConnected) this.phase = 'connected'
			}
			apply().catch((err) => {
				console.warn('处理Answer失败:', err)
				this.failCall('建立通话连接失败，请稍后重试')
			})
		},
		handleIceCandidate(msg) {
			const candidate = normalizeCandidate(msg.message)
			if (!candidate) return
			if (this.pc && this.remoteDescriptionReady) {
				this.pc.addIceCandidate(new RTCIceCandidate(candidate)).catch((err) => {
					console.warn('添加ICE候选失败:', err)
				})
			} else {
				this.iceCandidates.push(candidate)
			}
		},
		async drainIceCandidates() {
			if (!this.pc) return
			while (this.iceCandidates.length) {
				const candidate = this.iceCandidates.shift()
				try {
					await this.pc.addIceCandidate(new RTCIceCandidate(candidate))
				} catch (err) {
					console.warn('添加缓存ICE失败:', err)
				}
			}
		},
		handleRejected(msg) {
			if (this.finished) return
			this.clearOutgoingTimer()
			const isBusy = !!(msg && msg.message === 'busy')
			sendCallRecord(this, isBusy ? 'busy' : 'rejected')
			this.finishWithNotice(isBusy ? '对方忙线中' : '对方已拒绝通话')
		},
		handleCanceled() {
			if (this.finished) return
			sendCallRecord(this, this.everConnected ? 'completed' : 'canceled')
			this.finishWithNotice(this.incomingPending ? '对方已取消通话' : '通话已取消')
		},
		handleTimeout() {
			if (this.finished) return
			// 对方（被叫方）先到达等待上限时主动广播 timeout。
			// 如果已经接通就忽略，避免迟到帧把正常通话挂断。
			if (this.everConnected) return
			this.stopIncomingFeedback()
			this.stopRingbackTone()
			sendCallRecord(this, 'timeout')
			this.finishWithNotice(this.incomingPending ? '未接来电' : '对方无应答')
		},
		handleEnded() {
			if (this.finished) return
			sendCallRecord(this, this.everConnected ? 'completed' : 'canceled')
			this.finishWithNotice('通话已结束')
		},
		initVideoElements() {
			if (typeof document === 'undefined') return
			const remoteHost = document.getElementById('remoteVideoHost')
			if (remoteHost && !remoteHost.querySelector('video')) {
				const video = document.createElement('video')
				video.setAttribute('autoplay', 'autoplay')
				video.setAttribute('playsinline', 'playsinline')
				video.setAttribute('webkit-playsinline', 'webkit-playsinline')
				video.style.position = 'absolute'
				video.style.left = '0'
				video.style.top = '0'
				video.style.width = '100%'
				video.style.height = '100%'
				video.style.objectFit = 'cover'
				video.style.background = '#0b0b0f'
				video.style.transform = 'translateZ(0)'
				remoteHost.appendChild(video)
			}

			const localHost = document.getElementById('localVideoHost')
			if (localHost && !localHost.querySelector('video')) {
				const video = document.createElement('video')
				video.setAttribute('autoplay', 'autoplay')
				video.setAttribute('playsinline', 'playsinline')
				video.setAttribute('webkit-playsinline', 'webkit-playsinline')
				video.muted = true
				video.style.position = 'absolute'
				video.style.left = '0'
				video.style.top = '0'
				video.style.width = '100%'
				video.style.height = '100%'
				video.style.objectFit = 'cover'
				video.style.transform = 'scaleX(-1)'
				video.style.zIndex = '1'
				localHost.appendChild(video)
			}
		},
		attachLocalVideo() {
			if (typeof document === 'undefined' || !this.localStream) return
			const host = document.getElementById('localVideoHost')
			if (!host) return
			let video = host.querySelector('video')
			if (!video) {
				this.initVideoElements()
				video = host.querySelector('video')
			}
			if (!video) return
			video.srcObject = this.localStream
			video.play().catch(() => {})
		},
		async attachRemoteVideo() {
			if (typeof document === 'undefined' || !this.remoteStream) return
			const host = document.getElementById('remoteVideoHost')
			if (!host) return
			let video = host.querySelector('video')
			if (!video) {
				this.initVideoElements()
				video = host.querySelector('video')
			}
			if (!video) return
			// 多条 track 会回调多次 attach；只让最后一次起播结果生效，
			// 避免前一次 play() 被新一次加载打断后误判成“自动播放被拦截”。
			const seq = ++this.remoteAttachSeq
			const isCurrent = () => seq === this.remoteAttachSeq
			// 先静音起播，绕开浏览器自动播放限制，保证接通后先看到画面；
			// 画面起播后再尝试打开声音，失败时只提示“轻触开启声音”，不遮挡画面。
			const wantAudio = this.speakerOn
			video.muted = true
			video.srcObject = this.remoteStream
			try {
				await video.play()
				if (!isCurrent()) return
				this.autoplayBlocked = false
				if (wantAudio) {
					video.muted = false
					try {
						await video.play()
						if (!isCurrent()) return
						this.audioBlocked = false
					} catch (err) {
						if (!isCurrent()) return
						video.muted = true
						this.audioBlocked = true
					}
				}
			} catch (err) {
				if (!isCurrent()) return
				video.muted = true
				this.autoplayBlocked = true
			}
		},
		async resumePlayback() {
			this.speakerOn = true
			const video = this.getRemoteVideoEl()
			if (!video) return
			video.muted = false
			try {
				await video.play()
				this.autoplayBlocked = false
				this.audioBlocked = false
			} catch (err) {
				video.muted = true
				this.autoplayBlocked = false
				this.audioBlocked = true
				video.play().catch(() => {})
			}
		},
		toggleMic() {
			if (!this.localStream) return
			const enabled = !this.micOn
			this.localStream.getAudioTracks().forEach((track) => {
				track.enabled = enabled
			})
			this.micOn = enabled
		},
		toggleCamera() {
			if (!this.localStream) return
			const enabled = !this.cameraOn
			this.localStream.getVideoTracks().forEach((track) => {
				track.enabled = enabled
			})
			this.cameraOn = enabled
		},
		async toggleSpeaker() {
			const next = !this.speakerOn
			const seq = ++this.speakerSeq
			const video = this.getRemoteVideoEl()
			// 立即切换按钮状态与静音状态，避免 play() 在无帧流上长期 pending 时
			// 按钮看起来“点了没反应”；只有真正的权限拒绝才回退。
			this.speakerOn = next
			this.audioBlocked = false
			if (video) video.muted = !next
			if (!next || !video) return
			try {
				await Promise.race([
					video.play(),
					new Promise((resolve, reject) => {
						setTimeout(() => {
							const err = new Error('play-timeout')
							err.name = 'PlayTimeout'
							reject(err)
						}, 800)
					})
				])
			} catch (err) {
				if (seq !== this.speakerSeq) return
				if (err && err.name === 'PlayTimeout') return
				video.muted = true
				this.speakerOn = false
				this.audioBlocked = true
			}
		},
		getRemoteVideoEl() {
			if (typeof document === 'undefined') return null
			const host = document.getElementById('remoteVideoHost')
			return host ? host.querySelector('video') : null
		},
		async switchCamera() {
			if (!this.pc || !this.localStream) return
			const nextFacing = this.frontCamera ? 'environment' : 'user'
			try {
				const nextStream = await createVideoOnlyStream(nextFacing)
				const nextTrack = nextStream.getVideoTracks()[0]
				if (!nextTrack) return
				const sender = this.pc.getSenders().find((item) => item.track && item.track.kind === 'video')
				if (sender) await sender.replaceTrack(nextTrack)
				const oldTrack = this.localStream.getVideoTracks()[0]
				if (oldTrack) {
					this.localStream.removeTrack(oldTrack)
					oldTrack.stop()
				}
				this.localStream.addTrack(nextTrack)
				this.frontCamera = !this.frontCamera
				const localVideo = typeof document !== 'undefined'
					? document.getElementById('localVideoHost')
					: null
				const video = localVideo ? localVideo.querySelector('video') : null
				if (video) {
					video.srcObject = this.localStream
					video.style.transform = this.frontCamera ? 'scaleX(-1)' : 'none'
					video.play().catch(() => {})
				}
			} catch (err) {
				console.warn('切换摄像头失败:', err)
				uni.showToast({ title: describeMediaError(err), icon: 'none' })
			}
		},
		startRingbackTone() {
			try {
				startRingback()
			} catch (err) {}
		},
		stopRingbackTone() {
			try {
				stopRingback()
			} catch (err) {}
		},
		startOutgoingTimer() {
			this.clearOutgoingTimer()
			this.outgoingTimer = setTimeout(() => {
				if (this.finished || this.peerAccepted) return
				this.stopRingbackTone()
				sendCallRecord(this, 'timeout')
				this.sendTerminalSignal('video_call_timeout', '')
				this.finishWithNotice('对方无应答')
			}, OUTGOING_TIMEOUT)
		},
		startIncomingTimer() {
			this.clearIncomingTimer()
			this.incomingTimer = setTimeout(() => {
				if (this.finished || !this.incomingPending) return
				this.sendTerminalSignal('video_call_timeout', '')
				this.finishWithNotice('来电已超时')
			}, INCOMING_TIMEOUT)
		},
		startConnectTimer() {
			this.clearConnectTimer()
			this.connectTimer = setTimeout(() => {
				if (this.finished || this.everConnected || !this.peerAccepted) return
				this.failCall('视频连接超时，可能是网络或防火墙限制', true)
			}, CONNECT_TIMEOUT)
		},
		startDurationTimer() {
			if (this.durationTimer) clearInterval(this.durationTimer)
			const started = Date.now()
			this.durationTimer = setInterval(() => {
				this.duration = Math.floor((Date.now() - started) / 1000)
			}, 1000)
		},
		clearOutgoingTimer() {
			if (this.outgoingTimer) {
				clearTimeout(this.outgoingTimer)
				this.outgoingTimer = null
			}
		},
		clearIncomingTimer() {
			if (this.incomingTimer) {
				clearTimeout(this.incomingTimer)
				this.incomingTimer = null
			}
		},
		clearConnectTimer() {
			if (this.connectTimer) {
				clearTimeout(this.connectTimer)
				this.connectTimer = null
			}
		},
		clearAllTimers() {
			this.stopIncomingFeedback()
			this.stopRingbackTone()
			this.clearOutgoingTimer()
			this.clearIncomingTimer()
			this.clearConnectTimer()
			if (this.restartTimer) {
				clearTimeout(this.restartTimer)
				this.restartTimer = null
			}
			if (this.durationTimer) {
				clearInterval(this.durationTimer)
				this.durationTimer = null
			}
			if (this.returnTimer) {
				clearTimeout(this.returnTimer)
				this.returnTimer = null
			}
		},
		formatDuration(seconds) {
			const total = Math.max(0, seconds || 0)
			const mins = Math.floor(total / 60)
			const secs = total % 60
			return String(mins).padStart(2, '0') + ':' + String(secs).padStart(2, '0')
		},
		vibrateIncoming() {
			try {
				if (typeof navigator !== 'undefined' && navigator.vibrate) {
					navigator.vibrate([500, 250, 500])
				}
			} catch (err) {}
		},
		startIncomingFeedback() {
			this.vibrateIncoming()
			try {
				startIncomingRing()
			} catch (err) {}
			if (this.vibrateTimer) clearInterval(this.vibrateTimer)
			this.vibrateTimer = setInterval(() => {
				if (!this.incomingPending || this.finished) return
				this.vibrateIncoming()
			}, 2400)
		},
		stopIncomingFeedback() {
			if (this.vibrateTimer) {
				clearInterval(this.vibrateTimer)
				this.vibrateTimer = null
			}
			try {
				stopIncomingRing()
			} catch (err) {}
			try {
				if (typeof navigator !== 'undefined' && navigator.vibrate) navigator.vibrate(0)
			} catch (err) {}
		},
		finishWithNotice(text) {
			if (this.finished) return
			this.finished = true
			this.phase = 'ended'
			this.endNotice = text
			this.stopIncomingFeedback()
			this.clearAllTimers()
			this.teardown()
			this.returnTimer = setTimeout(() => {
				this.closePage()
			}, 1200)
		},
		failCall(message, notifyPeer) {
			if (this.finished) return
			if (this.mode === 'outgoing' && this.requestSent) {
				sendCallRecord(this, this.everConnected ? 'completed' : 'failed')
			}
			// 已经接通过的连接异常终止时，要主动通知对端，否则对方会一直停在通话页。
			if (notifyPeer && this.peerAccepted && !this.hangupSignaled) {
				this.sendTerminalSignal('video_call_ended', 'network_failed')
			}
			this.finished = true
			this.phase = 'error'
			this.fatalError = message || '视频通话启动失败'
			this.stopIncomingFeedback()
			this.clearAllTimers()
			this.teardown()
		},
		teardown() {
			const client = getWebSocketClient()
			if (this.wsListener && client.unsubscribe) {
				client.unsubscribe(this.wsListener)
				this.wsListener = null
			}
			if (this.connectionListener && client.unsubscribeConnectionState) {
				client.unsubscribeConnectionState(this.connectionListener)
				this.connectionListener = null
			}
			if (this.pc) {
				try {
					this.pc.ontrack = null
					this.pc.onicecandidate = null
					this.pc.close()
				} catch (err) {}
				this.pc = null
			}
			if (typeof window !== 'undefined' && this.pageHideHandler) {
				window.removeEventListener('pagehide', this.pageHideHandler)
				this.pageHideHandler = null
			}
			if (typeof document !== 'undefined' && this.visibilityHandler) {
				document.removeEventListener('visibilitychange', this.visibilityHandler)
				this.visibilityHandler = null
			}
			this.releaseWakeLock()
			if (this.localStream) {
				stopStream(this.localStream)
				this.localStream = null
			}
			releasePendingStream()
			try {
				releaseCallAudio()
			} catch (err) {}
			this.remoteStream = null
			this.remoteReady = false
			this.localReady = false
		},
		closePage() {
			const pages = typeof getCurrentPages === 'function' ? getCurrentPages() : []
			if (pages.length > 1) {
				uni.navigateBack({ delta: 1 })
			} else {
				uni.redirectTo({ url: '/pages/chat/chat-list' })
			}
		}
	}
}
</script>

<style lang="scss" scoped>
.call-page {
	position: fixed;
	inset: 0;
	background: #0b0b0f;
	overflow: hidden;
	color: #fff;
	font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', 'Manrope', sans-serif;
}

.remote-surface {
	position: absolute;
	inset: 0;
	background: radial-gradient(circle at 50% 30%, #26314a 0%, #14151c 45%, #09090c 100%);
	z-index: 1;
}

.video-vignette {
	position: absolute;
	inset: 0;
	z-index: 2;
	pointer-events: none;
	background: linear-gradient(180deg, rgba(5, 5, 8, 0.66) 0%, rgba(5, 5, 8, 0) 22%, rgba(5, 5, 8, 0) 58%, rgba(5, 5, 8, 0.82) 100%);
}

.call-header {
	position: absolute;
	top: 0;
	left: 0;
	right: 0;
	z-index: 6;
	display: flex;
	flex-direction: column;
	align-items: center;
	gap: 8rpx;
	padding-top: calc(28rpx + var(--status-bar-height, 0px) + env(safe-area-inset-top));
	padding-bottom: 22rpx;
	pointer-events: none;
}

.peer-name {
	font-size: 44rpx;
	font-weight: 700;
	letter-spacing: 0.5rpx;
	text-shadow: 0 4rpx 24rpx rgba(0, 0, 0, 0.55);
	max-width: 70%;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
}

.call-status {
	font-size: 26rpx;
	color: rgba(255, 255, 255, 0.72);
	letter-spacing: 0.6rpx;
	font-variant-numeric: tabular-nums;
}

.waiting-layer {
	position: absolute;
	left: 0;
	right: 0;
	top: 21%;
	z-index: 4;
	display: flex;
	flex-direction: column;
	align-items: center;
	padding: 0 56rpx;
	pointer-events: none;
}

.avatar-ring {
	width: 196rpx;
	height: 196rpx;
	border-radius: 50%;
	padding: 8rpx;
	background: rgba(255, 255, 255, 0.12);
	border: 1rpx solid rgba(255, 255, 255, 0.3);
	box-shadow: 0 24rpx 80rpx rgba(0, 0, 0, 0.45);
	margin-bottom: 44rpx;
	position: relative;
}

.avatar-ring.is-ringing::before,
.avatar-ring.is-ringing::after {
	content: '';
	position: absolute;
	inset: -18rpx;
	border-radius: 50%;
	border: 2rpx solid rgba(102, 210, 255, 0.45);
	animation: callPulse 1.9s ease-out infinite;
}

.avatar-ring.is-ringing::after {
	animation-delay: 0.9s;
}

@keyframes callPulse {
	0% { transform: scale(0.94); opacity: 0.8; }
	100% { transform: scale(1.28); opacity: 0; }
}

.peer-avatar {
	width: 100%;
	height: 100%;
	border-radius: 50%;
	display: block;
	background: #26314a;
}

.avatar-fallback {
	display: flex;
	align-items: center;
	justify-content: center;
	font-size: 76rpx;
	font-weight: 700;
	color: #fff;
	background: linear-gradient(135deg, #3a6df0, #6a4df0);
}

.waiting-name {
	max-width: 72%;
	font-size: 42rpx;
	font-weight: 700;
	letter-spacing: 0.5rpx;
	margin-bottom: 10rpx;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
	text-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.45);
}

.waiting-title {
	font-size: 34rpx;
	font-weight: 600;
	letter-spacing: 1rpx;
	margin-bottom: 14rpx;
	text-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.4);
}

.waiting-desc {
	font-size: 26rpx;
	color: rgba(255, 255, 255, 0.55);
	text-align: center;
	line-height: 1.6;
}

.local-surface {
	position: absolute;
	top: 150rpx;
	right: 30rpx;
	width: 196rpx;
	height: 268rpx;
	border-radius: 24rpx;
	overflow: hidden;
	z-index: 5;
	background: #191b22;
	box-shadow: 0 18rpx 48rpx rgba(0, 0, 0, 0.42);
	border: 1rpx solid rgba(255, 255, 255, 0.16);
	transition: opacity 0.2s ease;
}

.local-surface.is-hidden {
	opacity: 0;
	pointer-events: none;
}

.local-off {
	position: absolute;
	left: 0;
	right: 0;
	bottom: 18rpx;
	z-index: 2;
	text-align: center;
	font-size: 20rpx;
	color: rgba(255, 255, 255, 0.6);
	pointer-events: none;
}

.speaker-badge {
	position: absolute;
	left: 50%;
	bottom: 330rpx;
	transform: translateX(-50%);
	z-index: 5;
	padding: 8rpx 20rpx;
	border-radius: 999rpx;
	background: rgba(255, 255, 255, 0.12);
	font-size: 22rpx;
	color: rgba(255, 255, 255, 0.7);
}

.resume-layer {
	position: absolute;
	left: 50%;
	top: 50%;
	transform: translate(-50%, -50%);
	z-index: 8;
	padding: 20rpx 36rpx;
	border-radius: 999rpx;
	background: rgba(0, 0, 0, 0.58);
	border: 1rpx solid rgba(255, 255, 255, 0.22);
	backdrop-filter: blur(8px);
}

.resume-text {
	font-size: 26rpx;
	color: #fff;
	white-space: nowrap;
}

.audio-badge {
	position: absolute;
	left: 50%;
	top: calc(150rpx + var(--status-bar-height) + env(safe-area-inset-top));
	transform: translateX(-50%);
	z-index: 9;
	padding: 14rpx 28rpx;
	border-radius: 999rpx;
	background: rgba(0, 0, 0, 0.62);
	border: 1rpx solid rgba(255, 255, 255, 0.16);
}

.audio-badge-text {
	font-size: 26rpx;
	color: #fff;
	white-space: nowrap;
}

.control-dock {
	position: absolute;
	left: 0;
	right: 0;
	bottom: 0;
	z-index: 7;
	padding: 24rpx 28rpx calc(36rpx + env(safe-area-inset-bottom));
	display: flex;
	flex-direction: column;
	align-items: center;
	gap: 30rpx;
	background: linear-gradient(180deg, rgba(7, 7, 10, 0) 0%, rgba(7, 7, 10, 0.55) 38%, rgba(7, 7, 10, 0.92) 100%);
}

.control-row {
	width: 100%;
	display: flex;
	align-items: center;
	justify-content: space-around;
}

.control-item {
	display: flex;
	flex-direction: column;
	align-items: center;
	gap: 14rpx;
	min-width: 112rpx;
}

.control-hover {
	opacity: 0.72;
}

.control-circle {
	width: 108rpx;
	height: 108rpx;
	border-radius: 50%;
	background: rgba(255, 255, 255, 0.16);
	border: 1rpx solid rgba(255, 255, 255, 0.16);
	display: flex;
	align-items: center;
	justify-content: center;
	position: relative;
	overflow: hidden;
}

.control-circle.is-off {
	background: rgba(255, 255, 255, 0.94);
}

.control-circle.is-off::after {
	content: '';
	position: absolute;
	left: 12%;
	right: 12%;
	top: 50%;
	height: 3rpx;
	background: #ff453a;
	transform: rotate(-45deg);
	box-shadow: 0 0 0 1rpx rgba(255, 255, 255, 0.9);
}

.control-icon {
	width: 50rpx;
	height: 50rpx;
}

.control-circle.is-off .control-icon {
	filter: invert(1);
	opacity: 1;
}

.speaker-off-state {
	background: rgba(255, 255, 255, 0.32);
}

.control-label {
	font-size: 22rpx;
	color: rgba(255, 255, 255, 0.7);
	letter-spacing: 0.5rpx;
}

.hangup-wrap {
	display: flex;
	flex-direction: column;
	align-items: center;
	gap: 14rpx;
}

.hangup-btn {
	width: 132rpx;
	height: 132rpx;
	border-radius: 50%;
	background: linear-gradient(180deg, #ff5b50, #e23a31);
	display: flex;
	align-items: center;
	justify-content: center;
	box-shadow: 0 16rpx 44rpx rgba(226, 58, 49, 0.42);
}

.hangup-hover {
	transform: scale(0.94);
	opacity: 0.9;
}

.hangup-icon {
	width: 60rpx;
	height: 60rpx;
	transform: rotate(135deg);
}

.hangup-label {
	font-size: 22rpx;
	color: rgba(255, 255, 255, 0.6);
}

.incoming-actions {
	position: absolute;
	left: 0;
	right: 0;
	bottom: calc(64rpx + env(safe-area-inset-bottom));
	z-index: 7;
	display: flex;
	align-items: center;
	justify-content: center;
	gap: 190rpx;
}

.incoming-action {
	display: flex;
	flex-direction: column;
	align-items: center;
	gap: 18rpx;
}

.call-action-circle {
	width: 142rpx;
	height: 142rpx;
	border-radius: 50%;
	display: flex;
	align-items: center;
	justify-content: center;
}

.call-action-circle.reject {
	background: linear-gradient(180deg, #ff5b50, #dc352c);
	box-shadow: 0 18rpx 48rpx rgba(220, 53, 44, 0.4);
}

.call-action-circle.accept {
	background: linear-gradient(180deg, #4ee08c, #19b865);
	box-shadow: 0 18rpx 48rpx rgba(25, 184, 101, 0.36);
}

.action-hover {
	transform: scale(0.94);
	opacity: 0.9;
}

.action-icon {
	width: 66rpx;
	height: 66rpx;
}

.rotate-hangup {
	transform: rotate(135deg);
}

.action-label {
	font-size: 26rpx;
	color: rgba(255, 255, 255, 0.82);
	font-weight: 600;
}

.end-layer {
	position: absolute;
	inset: 0;
	z-index: 9;
	background: rgba(5, 5, 8, 0.82);
	display: flex;
	flex-direction: column;
	align-items: center;
	justify-content: center;
	gap: 18rpx;
	backdrop-filter: blur(10px);
}

.end-title {
	font-size: 36rpx;
	font-weight: 700;
}

.end-desc {
	font-size: 24rpx;
	color: rgba(255, 255, 255, 0.55);
}

.error-layer {
	position: absolute;
	inset: 0;
	z-index: 10;
	background: rgba(5, 5, 8, 0.88);
	display: flex;
	align-items: center;
	justify-content: center;
	padding: 48rpx;
	backdrop-filter: blur(12px);
}

.error-card {
	width: 100%;
	max-width: 580rpx;
	border-radius: 32rpx;
	padding: 52rpx 40rpx 40rpx;
	background: rgba(255, 255, 255, 0.08);
	border: 1rpx solid rgba(255, 255, 255, 0.12);
	display: flex;
	flex-direction: column;
	align-items: center;
	gap: 20rpx;
}

.error-icon {
	width: 88rpx;
	height: 88rpx;
	border-radius: 50%;
	background: rgba(255, 69, 58, 0.18);
	color: #ff6961;
	font-size: 46rpx;
	font-weight: 700;
	display: flex;
	align-items: center;
	justify-content: center;
}

.error-title {
	font-size: 32rpx;
	font-weight: 700;
}

.error-desc {
	font-size: 25rpx;
	line-height: 1.65;
	color: rgba(255, 255, 255, 0.62);
	text-align: center;
}

.error-btn {
	margin-top: 22rpx;
	width: 100%;
	height: 92rpx;
	border-radius: 18rpx;
	background: rgba(255, 255, 255, 0.14);
	display: flex;
	align-items: center;
	justify-content: center;
	font-size: 28rpx;
	font-weight: 600;
	color: #fff;
}

.error-btn-hover {
	background: rgba(255, 255, 255, 0.22);
}

@media screen and (max-height: 700px) {
	.waiting-layer {
		top: 17%;
	}

	.avatar-ring {
		width: 164rpx;
		height: 164rpx;
		margin-bottom: 30rpx;
	}

	.local-surface {
		width: 168rpx;
		height: 226rpx;
	}

	.control-circle {
		width: 94rpx;
		height: 94rpx;
	}

	.hangup-btn {
		width: 116rpx;
		height: 116rpx;
	}

	.incoming-actions {
		gap: 150rpx;
	}

	.call-action-circle {
		width: 124rpx;
		height: 124rpx;
	}
}
</style>
