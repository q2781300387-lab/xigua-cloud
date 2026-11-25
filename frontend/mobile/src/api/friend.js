import { core } from '@/core/index.js'

export function getFriendDetail(friendId) {
	return core.friend.getFriendDetail(friendId)
}

export function delFriend(friendId) {
	return core.friend.delFriend(friendId)
}

export function sendFriendRequest(data) {
	return core.friend.sendFriendRequest(data)
}

export function friendVerify(data) {
	return core.friend.friendVerify(data)
}
