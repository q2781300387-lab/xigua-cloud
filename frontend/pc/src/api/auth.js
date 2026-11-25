import { core } from '@/core'

// 登录
export function login(data, options = {}) {
    return core.auth.login(data, options)
}

// 登出
export function logout() {
    return core.auth.logout()
}

// 注册
export function register(data, options = {}) {
    return core.auth.register(data, options)
}

// // ticket兑换token
// export function redeemToken(data) {
//     return request({
//         url: '/sso/auth/redeemToken?ticket=' + data,
//         method: 'get'
//     })
// }
