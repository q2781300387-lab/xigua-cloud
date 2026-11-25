const VIDEO_CONSTRAINTS = {
	audio: {
		echoCancellation: true,
		noiseSuppression: true,
		autoGainControl: true
	},
	video: {
		width: { ideal: 1280 },
		height: { ideal: 720 },
		facingMode: { ideal: 'user' }
	}
}

let pendingStream = null

export function isMediaSupported() {
	return typeof navigator !== 'undefined' &&
		navigator.mediaDevices &&
		typeof navigator.mediaDevices.getUserMedia === 'function'
}

export function stopStream(stream) {
	if (!stream) return
	try {
		stream.getTracks().forEach(function (track) {
			track.stop()
		})
	} catch (err) {
		console.warn('停止媒体轨道失败', err)
	}
}

export async function createLocalStream(facingMode) {
	if (!isMediaSupported()) {
		const err = new Error('当前浏览器不支持摄像头或麦克风')
		err.name = 'NotSupportedError'
		throw err
	}
	const constraints = {
		audio: VIDEO_CONSTRAINTS.audio,
		video: Object.assign({}, VIDEO_CONSTRAINTS.video, {
			facingMode: { ideal: facingMode || 'user' }
		})
	}
	return navigator.mediaDevices.getUserMedia(constraints)
}

export async function createVideoOnlyStream(facingMode) {
	if (!isMediaSupported()) {
		const err = new Error('当前浏览器不支持摄像头')
		err.name = 'NotSupportedError'
		throw err
	}
	return navigator.mediaDevices.getUserMedia({
		audio: false,
		video: {
			width: { ideal: 1280 },
			height: { ideal: 720 },
			facingMode: { ideal: facingMode || 'environment' }
		}
	})
}

export async function prepareVideoStream(facingMode) {
	releasePendingStream()
	const stream = await createLocalStream(facingMode || 'user')
	pendingStream = stream
	return stream
}

export function takePendingStream() {
	const stream = pendingStream
	pendingStream = null
	return stream
}

export function releasePendingStream() {
	if (pendingStream) {
		stopStream(pendingStream)
		pendingStream = null
	}
}

export function describeMediaError(err) {
	const name = err && err.name ? err.name : ''
	if (name === 'NotAllowedError' || name === 'PermissionDeniedError' || name === 'SecurityError') {
		return '摄像头或麦克风权限被拒绝，请在浏览器设置中允许后重试'
	}
	if (name === 'NotFoundError' || name === 'DevicesNotFoundError') {
		return '未找到可用的摄像头或麦克风'
	}
	if (name === 'NotReadableError' || name === 'TrackStartError') {
		return '摄像头或麦克风被其他应用占用，请关闭后重试'
	}
	if (name === 'NotSupportedError') {
		return '当前浏览器不支持视频通话，请使用最新版 Chrome 或 Safari'
	}
	if (name === 'AbortError') {
		return '摄像头启动超时，请重试'
	}
	return '无法启动摄像头或麦克风，请检查设备与权限'
}
