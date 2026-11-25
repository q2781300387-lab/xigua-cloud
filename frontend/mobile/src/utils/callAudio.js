/**
 * 视频通话提示音。
 *
 * 浏览器要求音频上下文必须由用户手势创建/恢复，因此在聊天页点击“视频通话”
 * 时先调用 primeCallAudio() 预热；通话页复用同一个 AudioContext，避免导航后
 * 新建上下文被自动播放策略挂起，导致来电铃声或回铃音无声。
 */
let audioContext = null
let incomingTimer = null
let ringbackTimer = null
const liveNodes = new Set()

function getAudioContext() {
	if (typeof window === 'undefined') return null
	const Ctx = window.AudioContext || window.webkitAudioContext
	if (!Ctx) return null
	if (!audioContext || audioContext.state === 'closed') {
		try {
			audioContext = new Ctx()
		} catch (err) {
			audioContext = null
		}
	}
	return audioContext
}

function resumeAudioContext(ctx) {
	if (!ctx) return
	if (ctx.state === 'suspended') {
		ctx.resume().catch(() => {})
	}
}

export function primeCallAudio() {
	const ctx = getAudioContext()
	resumeAudioContext(ctx)
	return ctx
}

function scheduleTone(options) {
	const ctx = audioContext
	if (!ctx || ctx.state !== 'running') return
	const start = ctx.currentTime + (options.delay || 0)
	const duration = options.duration || 0.4
	try {
		const osc = ctx.createOscillator()
		const gain = ctx.createGain()
		osc.type = 'sine'
		osc.frequency.value = options.frequency
		gain.gain.setValueAtTime(0, start)
		gain.gain.linearRampToValueAtTime(options.volume || 0.08, start + 0.03)
		gain.gain.setValueAtTime(options.volume || 0.08, start + duration - 0.06)
		gain.gain.linearRampToValueAtTime(0, start + duration)
		osc.connect(gain)
		gain.connect(ctx.destination)
		osc.start(start)
		osc.stop(start + duration)
		osc.onended = () => {
			liveNodes.delete(osc)
			try {
				osc.disconnect()
				gain.disconnect()
			} catch (err) {}
		}
		liveNodes.add(osc)
	} catch (err) {}
}

function stopNodes() {
	liveNodes.forEach((node) => {
		try {
			node.stop()
		} catch (err) {}
	})
	liveNodes.clear()
}

export function playIncomingBurst() {
	const ctx = primeCallAudio()
	if (!ctx) return
	;[0, 0.62].forEach((offset) => {
		;[440, 480].forEach((frequency) => {
			scheduleTone({
				frequency,
				duration: 0.4,
				volume: 0.09,
				delay: offset
			})
		})
	})
}

export function startIncomingRing() {
	stopIncomingRing()
	primeCallAudio()
	playIncomingBurst()
	incomingTimer = setInterval(() => {
		playIncomingBurst()
	}, 2400)
}

export function stopIncomingRing() {
	if (incomingTimer) {
		clearInterval(incomingTimer)
		incomingTimer = null
	}
}

export function startRingback() {
	stopRingback()
	const ctx = primeCallAudio()
	if (!ctx) return
	const tick = () => {
		scheduleTone({
			frequency: 450,
			duration: 1,
			volume: 0.06
		})
	}
	tick()
	ringbackTimer = setInterval(tick, 4000)
}

export function stopRingback() {
	if (ringbackTimer) {
		clearInterval(ringbackTimer)
		ringbackTimer = null
	}
}

export function releaseCallAudio() {
	stopIncomingRing()
	stopRingback()
	stopNodes()
	if (audioContext) {
		try {
			audioContext.close()
		} catch (err) {}
		audioContext = null
	}
}
