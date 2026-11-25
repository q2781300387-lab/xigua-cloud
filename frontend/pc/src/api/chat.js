import request from '@/utils/request'

// 打开聊天
export function openChat(data) {
    return request({
        url: '/center/chat/openChat',
        method: 'post',
        data: data
    })
}

// 关闭聊天
export function closeChat(chatId) {
    return request({
        url: '/center/chat/closeChat?chatId=' + chatId,
        method: 'post'
    })
}

// 隐藏聊天
export function hideChat(chatId) {
    return request({
        url: '/center/chat/hideChat?chatId=' + chatId,
        method: 'post'
    })
}
