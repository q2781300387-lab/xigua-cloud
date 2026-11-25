/**
 * 手机 H5 Web Push 订阅管理
 * 复用后端 /client/push/publicKey、/client/push/subscribe、/client/push/unsubscribe
 */
import { get, post } from '@/utils/request'

const SW_URL = '/static/sw.js'

function urlBase64ToUint8Array(base64String) {
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

async function ensureServiceWorker() {
	if (!('serviceWorker' in navigator)) return null
	let reg = await navigator.serviceWorker.getRegistration('/')
	if (!reg) {
		reg = await navigator.serviceWorker.register(SW_URL, { scope: '/' })
	}
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
			setTimeout(resolve, 5000)
		})
	}
	return reg
}

async function fetchVapidPublicKey() {
	try {
		const res = await get('/client/push/publicKey')
		return (res && (res.data || res)) || null
	} catch (e) {
		return null
	}
}

/**
 * 完成订阅并上报后端。返回 { ok, reason }，不抛异常。
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
		await post('/client/push/subscribe', {
			endpoint: json.endpoint,
			p256dh: keys.p256dh,
			auth: keys.auth
		})
	} catch (e) {
		return { ok: false, reason: 'report-failed' }
	}
	return { ok: true }
}

export async function unsubscribeWebPush() {
	if (!('serviceWorker' in navigator)) return
	try {
		const reg = await navigator.serviceWorker.getRegistration('/')
		if (!reg) return
		const sub = await reg.pushManager.getSubscription()
		if (!sub) return
		const endpoint = sub.endpoint
		await sub.unsubscribe()
		await post('/client/push/unsubscribe', { endpoint })
	} catch (e) {
		/* 注销失败不阻塞退出 */
	}
}
