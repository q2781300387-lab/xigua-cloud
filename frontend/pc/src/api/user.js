import { core } from '@/core'

// 获取当前登录用户信息
export function getUserInfo() {
    return core.user.getUserInfo()
}

// 上传头像
export function uploadAvatar(data) {
    return core.user.uploadAvatar(data)
}

// 更新用户信息
export function updateUserInfo(data) {
    return core.user.updateUserInfo(data)
}

// 根据用户名查询用户列表
export function getListByName(username) {
    return core.user.getListByName(username)
}
