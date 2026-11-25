
import { normalizeAssetUrlsDeep } from './imageFallback.js'

export function get(key) {
    return localStorage.getItem(key)
}

export function set(key, value) {
    localStorage.setItem(key, value)
}

export function remove(key) {
    localStorage.removeItem(key)
}

export function clear() {
    localStorage.clear()
}

// 扩展方法：存储对象（自动JSON序列化）
// 用户信息里不能落地密码哈希：旧版本会把登录后接口返回的 password 一起写进 localStorage。
export function setObject(key, value) {
    if (key === 'userInfo' && value && typeof value === 'object') {
        const safe = { ...value }
        delete safe.password
        value = safe
    }
    localStorage.setItem(key, JSON.stringify(value))
}

// 扩展方法：获取对象（自动JSON解析）
// 读出来时顺手清理历史遗留的本地 dev 端口绝对地址（如 http://localhost:5173/bot-default-avatar.png），
// 避免换了前端端口之后缓存里的头像一直 ERR_CONNECTION_REFUSED。
export function getObject(key) {
    const data = localStorage.getItem(key)
    if (data == null || data == 'undefined') {
        return null
    }
    return data ? normalizeAssetUrlsDeep(JSON.parse(data)) : null
}
