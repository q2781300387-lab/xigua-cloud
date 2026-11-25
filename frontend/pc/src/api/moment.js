import request from '@/utils/request'
import { core } from '@/core'

// 朋友圈列表（自己 + 好友）
export function momentList(data) {
    return core.moment.momentList(data)
}

// 发布动态
export function momentPublish(data) {
    return core.moment.momentPublish(data)
}

// 点赞 / 取消点赞
export function momentLike(data) {
    return core.moment.momentLike(data)
}

// 评论
export function momentComment(data) {
    return core.moment.momentComment(data)
}

// 删除动态
export function momentDelete(data) {
    return core.moment.momentDelete(data)
}

// 删除评论
export function momentCommentDelete(data) {
    return core.moment.momentCommentDelete(data)
}

// 动态详情
export function momentDetail(data) {
    return core.moment.momentDetail(data)
}

// 未读互动（PC 独有）
export function momentInteractUnread(data) {
    return request({
        url: '/center/moment/interact-unread',
        method: 'post',
        data: data
    })
}
