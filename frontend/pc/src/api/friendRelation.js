import request from '@/utils/request'
import { core } from '@/core'

// 发送好友请求
export function sendFriendRequest(data) {
    return core.friend.sendFriendRequest(data)
}

// 接收好友请求（PC 独有）
export function friendRequest() {
    return request({
        url: 'client/friend/relation/friendRequest',
        method: 'get'
    })
}

// 获取好友列表（PC 独有）
export function getFriendList() {
    return request({
        url: 'client/friend/relation/getFriendList',
        method: 'get'
    })
}

// 好友验证
export function friendVerify(data) {
    return core.friend.friendVerify(data)
}

// 好友详情
export function getFriendDetail(friendId) {
    return core.friend.getFriendDetail(friendId)
}

// 删除好友
export function delFriend(friendId) {
    return core.friend.delFriend(friendId)
}
