import request from '@/utils/request'
import { core } from '@/core'
import { getToken } from '@/utils/auth'

// 创建会话
export function createSession(data) {
    return core.ai.createSession(data)
}

// 兼容旧调用
export const 创建会话 = createSession

// 发送流式消息（PC 独有，使用 fetch 处理 SSE），支持外部 signal 取消
export function streamChatSSE(data, signal) {
    return new Promise((resolve, reject) => {
        const baseURL = import.meta.env.VITE_APP_BASE_API;
        const url = `${baseURL}/ai/chat/session/messages/stream`;

        fetch(url, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'xigua-token': getToken()
            },
            body: JSON.stringify(data),
            signal: signal
        })
        .then(response => {
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            resolve(response.body.getReader());
        })
        .catch(error => {
            reject(error);
        });
    });
}

// 查询历史消息（分页）
export function getHistoryMessages(data) {
    return core.ai.getHistoryMessages(data)
}

// 删除会话
export function deleteSession(sessionId) {
    return core.ai.deleteSession(sessionId)
}

// 重命名会话
export function renameSession(sessionId, title) {
    return core.ai.renameSession(sessionId, title)
}

// 切换会话模型（供应商）
export function updateSessionModel(sessionId, model) {
    return core.ai.updateSessionModel(sessionId, model)
}

// 分页获取会话列表
export function pageSessions(botId, data) {
    return core.ai.pageSessions(botId, data)
}

// AI 智能回复（PC 独有）
export function smartReply(data) {
    return request({
        url: '/ai/chat/smart-reply',
        method: 'post',
        data: data
    })
}

// 消息翻译（PC 独有）
export function translateMessage(data) {
    return request({
        url: '/ai/chat/translate',
        method: 'post',
        data: data
    })
}

// AI 绘画
export function generateImage(data) {
    return core.ai.generateImage(data)
}
