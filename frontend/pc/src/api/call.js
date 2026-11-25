import request from '@/utils/request'

// 创建呼叫id
export function createCallId() {
    return request({
        url: '/client/call/createCallId',
        method: 'post'
    })
}
