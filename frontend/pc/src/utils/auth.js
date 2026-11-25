import Cookies from 'js-cookie'

const TokenKey = 'xigua-token'

const ExpiresInKey = 'Admin-Expires-In'

export function getToken() {
    return Cookies.get(TokenKey)
}

export function setToken(token) {
    return Cookies.set(TokenKey, token)
}

export function removeToken(key) {
    return Cookies.remove(key)
}

// 解码 JWT payload。只用于前端做「界面身份 / 登录态」一致性校验，不验签。
export function getTokenUserId(token) {
    try {
        if (!token) return ''
        const part = token.split('.')[1]
        if (!part) return ''
        const base64 = part.replace(/-/g, '+').replace(/_/g, '/')
        const raw = atob(base64)
        // atob 按 latin1 解码，中文用户名会乱码；userId 是纯数字，不受影响
        const payload = JSON.parse(decodeURIComponent(Array.prototype.map.call(raw, c =>
            '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2)
        ).join('')))
        return payload && payload.userId ? String(payload.userId) : ''
    } catch (e) {
        return ''
    }
}

export function getExpiresIn() {
    return Cookies.get(ExpiresInKey) || -1
}

export function setExpiresIn(time) {
    return Cookies.set(ExpiresInKey, time)
}

export function removeExpiresIn() {
    return Cookies.remove(ExpiresInKey)
}
