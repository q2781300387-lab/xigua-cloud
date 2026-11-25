import request from '@/utils/request'
import { core } from '@/core'

// 获取最后几条聊天记录
export function getLastMes(data) {
    return core.chat.getLastMes(data)
}

// 分页获取好友历史消息
export function getHistoryMes(data, options) {
    return core.chat.getHistoryMes(data, options)
}

// 分页查询视频通话记录
export function getCallRecords(data) {
    return core.chat.getCallRecords(data)
}

// 未接来电角标数量（PC / 手机共用同一已读水位）
export function getCallUnread() {
    return core.chat.getCallUnread()
}

// 清空通话记录未接角标
export function markCallRead() {
    return core.chat.markCallRead()
}

// 删除单条通话记录（仅当前用户）
export function deleteCallRecord(data) {
    return core.chat.deleteCallRecord(data)
}

// 清空全部通话记录（仅当前用户）
export function clearCallRecords() {
    return core.chat.clearCallRecords()
}

// 批量查询消息表情回应与当前用户收藏状态
export function getMessageInteractions(data) {
    return core.chat.getMessageInteractions(data)
}

// 切换消息表情回应
export function toggleMessageReaction(data) {
    return core.chat.toggleMessageReaction(data)
}

// 收藏 / 取消收藏消息
export function toggleMessageFavorite(data) {
    return core.chat.toggleMessageFavorite(data)
}

// 收藏夹分页列表
export function getMessageFavorites(data) {
    return core.chat.getMessageFavorites(data)
}

// 删除收藏
export function removeMessageFavorite(data) {
    return core.chat.removeMessageFavorite(data)
}

// 搜索历史聊天记录（PC 独有）
export function searchHistoryMes(data) {
    return request({
        url: '/search/chat/message/search',
        method: 'post',
        data: data
    })
}

// 全局搜索（PC 独有）
export function searchGlobalMessages(data) {
    return request({
        url: '/search/chat/message/global',
        method: 'post',
        data: data
    })
}

// 查询未读且@我的会话（PC 独有）
export function getAtMeUnread(data) {
    return request({
        url: '/center/chat/message/atMeUnread',
        method: 'post',
        data: data
    })
}

// 编辑文本消息
export function editMessage(data) {
    return request({
        url: '/center/chat/message/edit',
        method: 'post',
        data: data
    })
}

// 保存会话置顶 / 草稿
export function saveConversationSetting(data) {
    return request({
        url: '/center/chat/message/conversation/setting',
        method: 'post',
        data: data
    })
}

// 查询会话置顶 / 草稿
export function getConversationSetting(data) {
    return request({
        url: '/center/chat/message/conversation/setting/get',
        method: 'post',
        data: data
    })
}
