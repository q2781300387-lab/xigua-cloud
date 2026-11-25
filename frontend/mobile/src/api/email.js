import { core } from '@/core/index.js'

export function sendEmailCode(data) {
	return core.email.sendEmailCode(data)
}

export function verifyResetCode(data) {
	return core.email.verifyResetCode(data)
}
