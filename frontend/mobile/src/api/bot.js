import { core } from '@/core/index.js'

export function getBots() {
	return core.bot.getBots()
}

export function getBotDetail(botId) {
	return core.bot.getBotDetail(botId)
}

export function createBot(data) {
	return core.bot.createBot(data)
}

export function updateBot(botId, data) {
	return core.bot.updateBot(botId, data)
}

export function deleteBot(botId) {
	return core.bot.deleteBot(botId)
}
