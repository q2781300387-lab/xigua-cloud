import { core } from '@/core/index.js'

export function firstLetterFriends() {
	return core.contact.firstLetterFriends()
}

export function getSendFriendRequestList() {
	return core.contact.getSendFriendRequestList()
}

export function getReceiveFriendRequestList() {
	return core.contact.getReceiveFriendRequestList()
}

export function getGroupList() {
	return core.contact.getGroupList()
}

export function getContactCount() {
	return core.contact.getContactCount()
}
