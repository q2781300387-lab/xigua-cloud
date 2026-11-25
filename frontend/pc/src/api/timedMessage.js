import request from '@/utils/request'

// 预约定时消息（服务端落库，到点由服务端调度发送）
export function createTimedMessageApi(data) {
    return request({
        url: '/center/chat/message/timed/create',
        method: 'post',
        data: data
    })
}

// 查询本人待发送的定时消息
export function listTimedMessagesApi(data) {
    return request({
        url: '/center/chat/message/timed/list',
        method: 'post',
        data: data
    })
}

// 取消定时消息
export function cancelTimedMessageApi(data) {
    return request({
        url: '/center/chat/message/timed/cancel',
        method: 'post',
        data: data
    })
}
