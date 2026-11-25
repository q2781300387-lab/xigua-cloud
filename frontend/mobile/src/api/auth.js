import { core } from '@/core/index.js'

export function login(data) {
	return core.auth.login(data)
}

export function logout() {
	return core.auth.logout()
}

export function register(data) {
	return core.auth.register(data)
}

export function loginRisk(username) {
	return core.auth.loginRisk(username)
}

export function loginCaptcha(username) {
	return core.auth.captcha(username)
}
