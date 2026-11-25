import request from '@/utils/request'
import { core } from '@/core'

// 树洞列表（匿名）
export function treeholeList(params) {
    return core.treehole.treeholeList(params)
}

// 发布树洞（匿名）
export function treeholeAdd(data) {
    return core.treehole.treeholeAdd(data)
}

// 树洞点赞
export function treeholeLike(id) {
    return core.treehole.treeholeLike(id)
}

// 群聊智能摘要（PC 独有）
export function chatSummary(data) {
    return request({
        url: '/ai/chat/summary',
        method: 'post',
        data: data
    })
}
