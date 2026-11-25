import request from '@/utils/request'
import { core } from '@/core'

// 全局搜索联系人/群/群文件（消息搜索走 search 服务）
export function searchContactsGroupsFiles(keyword) {
    return request({
        url: '/client/search/all',
        method: 'get',
        params: { keyword }
    })
}

// 获取联系人数量
export function getContactCount() {
    return core.contact.getContactCount()
}

// 获取好友列表（PC 独有，核心暂未抽象）
export function getFriendList() {
    return request({
        url: '/client/contact/getFriendList',
        method: 'get',
    })
}

// 获取群聊列表
export function getGroupList() {
    return core.contact.getGroupList()
}

// 获取机器人列表（PC 独有）
export function getBotList() {
    return request({
        url: '/client/contact/getBotList',
        method: 'get',
    })
}

// 获取发送好友申请列表
export function getSendFriendRequestList() {
    return core.contact.getSendFriendRequestList()
}

// 获取接收好友申请列表
export function getReceiveFriendRequestList() {
    return core.contact.getReceiveFriendRequestList()
}

// 撤回我发送且仍待处理的好友申请
export function withdrawFriendRequest(requestId) {
    return request({
        url: '/client/contact/withdrawFriendRequest',
        method: 'post',
        params: { requestId }
    })
}
