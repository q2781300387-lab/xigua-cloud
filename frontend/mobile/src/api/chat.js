import { core } from '@/core/index.js'

export function getLastMes(data) {
	return core.chat.getLastMes(data)
}

export function hideChat(chatId) {
	return core.chat.hideChat(chatId)
}

export function getHistoryMes(data, options) {
	return core.chat.getHistoryMes(data, options)
}

export function getCallRecords(data) {
	return core.chat.getCallRecords(data)
}

export function getCallUnread() {
	return core.chat.getCallUnread()
}

export function markCallRead() {
	return core.chat.markCallRead()
}

export function deleteCallRecord(data) {
	return core.chat.deleteCallRecord(data)
}

export function clearCallRecords() {
	return core.chat.clearCallRecords()
}

export function getUnreadInfo() {
	return core.chat.getUnreadInfo()
}

export function getMessageInteractions(data) {
	return core.chat.getMessageInteractions(data)
}

export function toggleMessageReaction(data) {
	return core.chat.toggleMessageReaction(data)
}

export function toggleMessageFavorite(data) {
	return core.chat.toggleMessageFavorite(data)
}

export function getMessageFavorites(data) {
	return core.chat.getMessageFavorites(data)
}

export function removeMessageFavorite(data) {
	return core.chat.removeMessageFavorite(data)
}

export function searchGlobalMessages(data) {
	return core.chat.searchGlobalMessages(data)
}

export function searchHistoryMessages(data) {
	return core.chat.searchHistoryMessages(data)
}

export function searchContactsGroupsFiles(keyword) {
	return core.chat.searchContactsGroupsFiles(keyword)
}

export function getAtMeUnread(data) {
	return core.chat.getAtMeUnread(data)
}

export function editMessage(data) {
	return core.chat.editMessage(data)
}

export function saveConversationSetting(data) {
	return core.chat.saveConversationSetting(data)
}

export function getConversationSetting(data) {
	return core.chat.getConversationSetting(data)
}
