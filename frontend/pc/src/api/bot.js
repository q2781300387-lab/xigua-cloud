import { core } from '@/core'

// 创建机器人
export function createBot(data) {
    return core.bot.createBot(data)
}

// 获取机器人列表
export function getBots() {
    return core.bot.getBots()
}

// 获取机器人详情
export function getBotDetail(botId) {
    return core.bot.getBotDetail(botId)
}

// 删除机器人
export function deleteBot(botId) {
    return core.bot.deleteBot(botId)
}

// 更新机器人
export function updateBot(botId, data) {
    return core.bot.updateBot(botId, data)
}
