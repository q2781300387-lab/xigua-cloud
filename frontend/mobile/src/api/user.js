import { core } from '@/core/index.js'

export function getUserInfo() {
	return core.user.getUserInfo()
}

export function resetPassword(data) {
	return core.user.resetPassword(data)
}

export function updateUserInfo(data) {
	return core.user.updateUserInfo(data)
}

export function uploadAvatar(avatar) {
	return core.user.uploadAvatar(avatar)
}

export function getListByName(username) {
	return core.user.getListByName(username)
}
