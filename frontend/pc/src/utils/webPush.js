/**
 * Web Push 订阅管理
 * 后端：POST /client/push/subscribe、POST /client/push/unsubscribe、GET /client/push/publicKey
 */
import request from '@/utils/request'

/** base64url → Uint8Array（PushManager 要 ArrayBuffer 视图） */
export function urlBase64ToUint8Array(base64String) {
  const padding = '='.repeat((4 - (base64String.length % 4)) % 4)
  const base64 = (base64String + padding).replace(/-/g, '+').replace(/_/g, '/')
  const raw = window.atob(base64)
  const output = new Uint8Array(raw.length)
  for (let i = 0; i < raw.length; i++) output[i] = raw.charCodeAt(i)
  return output
}

export function pushSupported() {
  return 'serviceWorker' in navigator && 'PushManager' in window && 'Notification' in window
}

/** 注册 SW 并等待其激活（不用 ServiceWorkerContainer.ready，避免非页面上下文的 NotSupportedError） */
export async function ensureServiceWorker() {
  if (!('serviceWorker' in navigator)) return null
  const reg = await navigator.serviceWorker.register('/sw.js')
  if (reg.active) return reg
  const pending = reg.installing || reg.waiting
  if (pending) {
    await new Promise((resolve) => {
      const onState = () => {
        if (pending.state === 'activated') {
          pending.removeEventListener('statechange', onState)
          resolve()
        }
      }
      pending.addEventListener('statechange', onState)
      // 兜底：5 秒未激活也继续，期间 getSubscription 会返回 null 再由调用方兜底
      setTimeout(resolve, 5000)
    })
  }
  return reg
}

function arrayBufferToBase64Url(buffer) {
  const bytes = new Uint8Array(buffer)
  let binary = ''
  for (let i = 0; i < bytes.length; i++) binary += String.fromCharCode(bytes[i])
  return window.btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '')
}

/** 拿后端 VAPID 公钥（未配置时返回 null，前端降级为仅站内横幅） */
export async function fetchVapidPublicKey() {
  try {
    const res = await request({ url: '/client/push/publicKey', method: 'get' })
    return (res && (res.data || res)) || null
  } catch (e) {
    return null
  }
}

/**
 * 完成订阅并把订阅参数上报后端。
 * 返回 { ok, reason }，不抛异常，调用方据此给用户反馈。
 */
export async function subscribeWebPush() {
  if (!pushSupported()) return { ok: false, reason: 'unsupported' }

  if (Notification.permission !== 'granted') {
    const perm = await Notification.requestPermission()
    if (perm !== 'granted') return { ok: false, reason: 'denied' }
  }

  const reg = await ensureServiceWorker()
  if (!reg) return { ok: false, reason: 'no-sw' }

  const publicKey = await fetchVapidPublicKey()
  if (!publicKey) return { ok: false, reason: 'no-vapid' }

  let sub = await reg.pushManager.getSubscription()
  if (!sub) {
    // 部分网络环境到推送服务不可达时 subscribe 会长时间挂起而不报错，这里加超时兜底
    const subscribing = reg.pushManager.subscribe({
      userVisibleOnly: true,
      applicationServerKey: urlBase64ToUint8Array(publicKey)
    })
    sub = await Promise.race([
      subscribing,
      new Promise((resolve) => setTimeout(() => resolve(null), 8000))
    ])
    if (!sub) return { ok: false, reason: 'unreachable' }
  }

  const json = sub.toJSON()
  const keys = json.keys || {}
  try {
    await request({
      url: '/client/push/subscribe',
      method: 'post',
      data: { endpoint: json.endpoint, p256dh: keys.p256dh, auth: keys.auth }
    })
  } catch (e) {
    return { ok: false, reason: 'report-failed' }
  }
  return { ok: true, endpoint: json.endpoint }
}

/** 退出时注销当前浏览器订阅 */
export async function unsubscribeWebPush() {
  if (!('serviceWorker' in navigator)) return
  try {
    const reg = await navigator.serviceWorker.getRegistration('/sw.js')
    if (!reg) return
    const sub = await reg.pushManager.getSubscription()
    if (!sub) return
    const endpoint = sub.endpoint
    await sub.unsubscribe()
    await request({ url: '/client/push/unsubscribe', method: 'post', data: { endpoint } })
  } catch (e) {
    /* 注销失败不阻塞退出 */
  }
}
