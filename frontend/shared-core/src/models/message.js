export const CONTENT_TYPES = {
	TEXT: 1,
	IMAGE: 2,
	VOICE: 3,
	VIDEO: 4,
	FILE: 5,
	CALL: 9
}

export const MESSAGE_TYPES = {
	CHAT: 'chat',
	NOTIFY: 'notify',
	HEART_BEAT: 'heart_beat'
}

export const SUB_TYPES = {
	MES_SEND: 'mes_send',
	MES_RECEIVE: 'mes_receive',
	MES_SEND_ACK: 'mes_send_ack',
	MES_READ: 'mes_read',
	SWITCH_CHAT_WINDOW: 'switch_chat_window',
	SUBMIT_UNREAD: 'submit_unread'
}

const CONTENT_TYPE_MAP = {
	1: 'text',
	2: 'image',
	3: 'voice',
	4: 'video',
	5: 'file'
}

const DEFAULT_FILE_NAME = {
	voice: 'audio.mp3',
	video: 'video.mp4',
	file: 'document.pdf',
	image: 'image.jpg'
}

export function getMessageType(contentType) {
	const type = parseInt(contentType, 10)
	return CONTENT_TYPE_MAP[type] || 'text'
}

export function parseMessageContent(message, type) {
	if (type === 'text') return message
	try {
		const parsed = JSON.parse(message)
		return parsed.url || parsed.path || message
	} catch (err) {
		return message
	}
}

export function getDefaultFileName(type) {
	return DEFAULT_FILE_NAME[type] || 'file'
}

export function optimizeFileName(fileName) {
	if (!fileName) return ''
	// 上传服务会把「雪花ID_」或「32位UUID_」拼进存储名，展示时去掉这段前缀
	const cleanName = String(fileName)
		.replace(/^\d{15,}_/, '')
		.replace(/^[0-9a-f]{32}_/i, '')
	const maxLength = 30
	if (cleanName.length <= maxLength) return cleanName
	const lastDotIndex = cleanName.lastIndexOf('.')
	if (lastDotIndex === -1) return cleanName.substring(0, maxLength - 3) + '...'
	const name = cleanName.substring(0, lastDotIndex)
	const ext = cleanName.substring(lastDotIndex)
	const maxNameLength = maxLength - ext.length - 3
	if (name.length <= maxNameLength) return cleanName
	return name.substring(0, maxNameLength) + '...' + ext
}

export function getFileNameFromUrl(url, type) {
	if (!url) return getDefaultFileName(type)
	try {
		const parts = url.split('/')
		const fileName = parts[parts.length - 1]
		return optimizeFileName(fileName || getDefaultFileName(type))
	} catch (err) {
		return getDefaultFileName(type)
	}
}

export function formatFileSize(bytes) {
	if (!bytes || bytes === 0) return '--'
	const k = 1024
	const sizes = ['B', 'KB', 'MB', 'GB']
	const i = Math.floor(Math.log(bytes) / Math.log(k))
	return (bytes / Math.pow(k, i)).toFixed(1) + ' ' + sizes[i]
}

export function formatDuration(seconds) {
	if (!seconds || seconds === 0) return '01秒'
	const sec = parseInt(seconds, 10)
	if (sec < 60) return sec.toString().padStart(2, '0') + '秒'
	const min = Math.floor(sec / 60)
	const remainSec = sec % 60
	return min.toString().padStart(2, '0') + '分' + remainSec.toString().padStart(2, '0') + '秒'
}

export function parseWsMessage(data) {
	if (typeof data !== 'string') return data
	return JSON.parse(data)
}
