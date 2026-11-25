import { core } from '@/core/index.js'
import config from '@/config/index.js'

export function createSession(data) {
	return core.ai.createSession(data)
}

export function getHistoryMessages(data) {
	return core.ai.getHistoryMessages(data)
}

export function pageSessions(botId, data) {
	return core.ai.pageSessions(botId, data)
}

export function renameSession(sessionId, title) {
	return core.ai.renameSession(sessionId, title)
}

export function updateSessionModel(sessionId, model) {
	return core.ai.updateSessionModel(sessionId, model)
}

export function generateImage(data) {
	return core.ai.generateImage(data)
}

export function smartReply(data) {
	return core.ai.smartReply(data)
}

export function translateMessage(data) {
	return core.ai.translateMessage(data)
}

export function deleteSession(sessionId) {
	return core.ai.deleteSession(sessionId)
}

/**
 * 流式对话（H5 使用 fetch 读取 SSE）
 * @param {Object} data { userId, botId, sessionId, input }
 * @param {Object} handlers { onDelta, onStatus, signal }
 */
export async function streamChat(data, handlers) {
	const token = uni.getStorageSync(config.tokenKey) || ''
	const options = {
		method: 'POST',
		headers: {
			'Content-Type': 'application/json',
			...(token && { [config.tokenKey]: token })
		},
		body: JSON.stringify(data)
	}
	if (handlers && handlers.signal) options.signal = handlers.signal

	const response = await fetch(config.baseURL + '/ai/chat/session/messages/stream', options)
	if (!response.ok) {
		throw new Error('AI 服务连接失败（' + response.status + '）')
	}
	if (!response.body) {
		throw new Error('当前浏览器不支持流式回复')
	}

	const reader = response.body.getReader()
	const decoder = new TextDecoder()
	let buffer = ''
	let currentEvent = ''

	while (true) {
		const result = await reader.read()
		if (result.done) break
		buffer += decoder.decode(result.value, { stream: true })
		const lines = buffer.split(/\r?\n/)
		buffer = lines.pop() || ''

		for (const rawLine of lines) {
			const line = rawLine.trim()
			if (line.startsWith('event:')) {
				currentEvent = line.slice(6).trim()
				continue
			}
			if (!line.startsWith('data:')) continue
			const content = line.slice(5)
			if (content === '[DONE]') continue
			if (currentEvent === 'status') {
				if (handlers && handlers.onStatus) handlers.onStatus(content)
			} else if (handlers && handlers.onDelta) {
				handlers.onDelta(content)
			}
		}
	}
}
