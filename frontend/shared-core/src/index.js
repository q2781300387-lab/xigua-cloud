import { createAuthApi } from './api/auth.js'
import { createUserApi } from './api/user.js'
import { createContactApi } from './api/contact.js'
import { createFriendApi } from './api/friend.js'
import { createGroupApi } from './api/group.js'
import { createChatApi } from './api/chat.js'
import { createMomentApi } from './api/moment.js'
import { createBotApi } from './api/bot.js'
import { createAiApi } from './api/ai.js'
import { createTreeholeApi } from './api/treehole.js'
import { createEmailApi } from './api/email.js'
import { createPrivacyApi } from './api/privacy.js'

export { createHttpClient } from './http.js'
export { createWebSocketClient } from './ws.js'
export { getFileType } from './utils/file.js'
export {
	CONTENT_TYPES,
	MESSAGE_TYPES,
	SUB_TYPES,
	getMessageType,
	parseMessageContent,
	getDefaultFileName,
	optimizeFileName,
	getFileNameFromUrl,
	formatFileSize,
	formatDuration,
	parseWsMessage
} from './models/message.js'

export {
	createAuthApi,
	createUserApi,
	createContactApi,
	createFriendApi,
	createGroupApi,
	createChatApi,
	createMomentApi,
	createBotApi,
	createAiApi,
	createTreeholeApi,
	createEmailApi,
	createPrivacyApi
}

/**
 * 创建一套业务 API。http 由各端按自己的平台能力注入。
 */
export function createCore(http) {
	return {
		auth: createAuthApi(http),
		user: createUserApi(http),
		contact: createContactApi(http),
		friend: createFriendApi(http),
		group: createGroupApi(http),
		chat: createChatApi(http),
		moment: createMomentApi(http),
		bot: createBotApi(http),
		ai: createAiApi(http),
		treehole: createTreeholeApi(http),
		email: createEmailApi(http),
		privacy: createPrivacyApi(http)
	}
}
