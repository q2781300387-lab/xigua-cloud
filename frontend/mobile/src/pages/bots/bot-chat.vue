<template>
	<view class="bot-chat-page">
		<view class="top-bar safe-top-bar">
			<view class="top-bar-content">
				<view class="back-btn" @click="goBack">
					<image class="back-icon" src="/static/icons/arrow/left.svg" mode="aspectFit"></image>
				</view>
				<view class="title-wrap">
					<text class="page-title">{{ botName }}</text>
					<text class="page-subtitle">{{ currentTitle || 'AI 对话' }}</text>
				</view>
				<view class="top-actions">
					<view class="top-action" @click="drawerOpen = true">
						<text class="top-action-text">会话</text>
					</view>
					<view class="top-action" @click="newChat">
						<text class="top-action-text">新对话</text>
					</view>
				</view>
			</view>
		</view>

		<scroll-view class="message-scroll" scroll-y :scroll-into-view="scrollInto">
			<view class="message-list">
				<view v-if="historyLoading" class="history-state">正在加载会话…</view>

				<view v-else-if="messages.length === 0" class="welcome-card">
					<view class="welcome-icon">
						<image class="welcome-icon-img" :src="botAvatar || defaultAvatar" mode="aspectFill"></image>
					</view>
					<text class="welcome-title">{{ botName }}</text>
					<text class="welcome-desc">{{ botDescription || '随时开始对话，它会按照设定回复你。' }}</text>
					<view class="welcome-chips">
						<view class="welcome-chip" @click="usePrompt('你好，介绍一下你自己')">
							<text class="welcome-chip-text">介绍一下你自己</text>
						</view>
						<view class="welcome-chip" @click="usePrompt('帮我整理一下今天的待办')">
							<text class="welcome-chip-text">整理今天的待办</text>
						</view>
						<view class="welcome-chip" @click="usePrompt('画一只戴墨镜的西瓜')">
							<text class="welcome-chip-text">画一只西瓜</text>
						</view>
					</view>
					<view v-if="sessions.length" class="welcome-history" @click="drawerOpen = true">
						<text class="welcome-history-text">有 {{ sessions.length }} 个历史会话 ›</text>
					</view>
				</view>

				<view v-for="msg in messages" :key="msg.id" class="message-row" :class="msg.role === 'user' ? 'row-user' : 'row-ai'">
					<view v-if="msg.role === 'ai'" class="ai-avatar">
						<image class="ai-avatar-img" :src="botAvatar || defaultAvatar" mode="aspectFill"></image>
					</view>
					<view class="bubble" :class="msg.role === 'user' ? 'bubble-user' : 'bubble-ai'">
						<image
							v-if="isImageMessage(msg)"
							class="bubble-image"
							:src="imageUrlOf(msg)"
							mode="aspectFit"
							@click="previewImage(msg)"
						></image>
						<text v-else-if="msg.status && !msg.content" class="bubble-status">{{ msg.status }}</text>
						<text v-else-if="msg.content" class="bubble-text">{{ msg.content }}</text>
						<text v-else class="bubble-text bubble-placeholder">正在思考…</text>
					</view>
				</view>

				<view id="msg-bottom" class="message-bottom"></view>
			</view>
		</scroll-view>

		<view class="input-area">
			<view class="model-segment">
				<view
					v-for="p in providerOptions"
					:key="p.value"
					class="segment-item"
					:class="{ active: currentProvider === p.value }"
					@click="switchProvider(p.value)"
				>
					<text class="segment-text">{{ p.label }}</text>
				</view>
			</view>
			<view class="input-bar">
				<textarea
					class="chat-input"
					v-model="inputText"
					placeholder="发消息，或描述想画的画面…"
					maxlength="1000"
					auto-height
					:adjust-position="true"
				></textarea>
				<view class="paint-btn" :class="{ disabled: isGeneratingImage }" @click="generateImage">
					<image class="paint-icon" src="/static/icons/feature/paint.svg" mode="aspectFit"></image>
				</view>
				<view v-if="isStreaming" class="send-btn stop-btn" @click="stopStreaming">
					<text class="send-text">停止</text>
				</view>
				<view v-else class="send-btn" :class="{ 'send-disabled': !inputText.trim() || isGeneratingImage }" @click="sendMessage">
					<text class="send-text">发送</text>
				</view>
			</view>
			<text class="input-tip">生成的内容可能包含不准确信息，请验证重要信息。</text>
		</view>

		<view v-if="drawerOpen" class="drawer-mask" @click="drawerOpen = false">
			<view class="drawer" @click.stop>
				<view class="drawer-head safe-top-bar">
					<text class="drawer-title">会话</text>
					<view class="drawer-close" @click="drawerOpen = false">
						<text class="drawer-close-text">关闭</text>
					</view>
				</view>

				<view class="drawer-new" @click="newChat">
					<text class="drawer-new-plus">＋</text>
					<text class="drawer-new-text">开启新对话</text>
				</view>

				<scroll-view class="session-scroll" scroll-y>
					<view v-if="sessions.length === 0" class="session-empty">
						<text class="session-empty-title">暂无会话记录</text>
						<text class="session-empty-desc">点击「开启新对话」开始聊天</text>
					</view>
					<view v-for="group in sessionGroups" :key="group.label" class="session-group">
						<text class="session-group-label">{{ group.label }}</text>
						<view v-for="chat in group.items" :key="chat.id" class="session-item-wrap">
							<view class="session-item" :class="{ active: isActiveSession(chat) }" @click="selectSession(chat)">
								<text class="session-title">{{ chat.title || '未命名会话' }}</text>
								<text v-if="chat.model" class="session-model" :class="modelTagClass(chat.model)">{{ modelLabel(chat.model) }}</text>
								<view class="session-more" @click.stop="toggleSessionMenu(chat.id)">
									<text class="session-more-text">⋯</text>
								</view>
							</view>
							<view v-if="menuSessionId === chat.id" class="session-menu">
								<view class="session-menu-item" @click.stop="startRename(chat)">
									<text class="session-menu-text">重命名</text>
								</view>
								<view class="session-menu-item danger" @click.stop="removeSession(chat)">
									<text class="session-menu-text danger-text">删除</text>
								</view>
							</view>
						</view>
					</view>
				</scroll-view>

				<view class="drawer-footer">
					<image class="drawer-bot-avatar" :src="botAvatar || defaultAvatar" mode="aspectFill"></image>
					<view class="drawer-bot-info">
						<text class="drawer-bot-name">{{ botName }}</text>
						<text class="drawer-bot-desc">当前机器人</text>
					</view>
				</view>
			</view>
		</view>

		<view v-if="renameOpen" class="dialog-mask" @click.self="closeRename">
			<view class="dialog">
				<text class="dialog-title">重命名会话</text>
				<input class="dialog-input" v-model="renameInput" maxlength="15" placeholder="输入新标题" :focus="renameOpen" />
				<view class="dialog-actions">
					<view class="dialog-btn" @click="closeRename">
						<text class="dialog-btn-text">取消</text>
					</view>
					<view class="dialog-btn primary" @click="submitRename">
						<text class="dialog-btn-text primary-text">保存</text>
					</view>
				</view>
			</view>
		</view>
	</view>
</template>

<script>
import config from '@/config/index.js'
import {
	createSession,
	pageSessions,
	getHistoryMessages,
	deleteSession,
	renameSession,
	updateSessionModel,
	generateImage,
	streamChat
} from '@/api/ai.js'
import { getBotDetail } from '@/api/bot.js'

export default {
	data() {
		return {
			userInfo: {},
			botId: '',
			botName: '机器人',
			botDescription: '',
			botAvatar: '',
			messages: [],
			inputText: '',
			sessionId: '',
			currentTitle: '',
			sessions: [],
			historyLoading: false,
			isStreaming: false,
			isGeneratingImage: false,
			streamController: null,
			activeAiMessage: null,
			requestSeq: 0,
			scrollInto: '',
			messageSeed: 0,
			drawerOpen: false,
			menuSessionId: '',
			renameOpen: false,
			renameTarget: null,
			renameInput: '',
			currentProvider: 'zhipu',
			providerOptions: [
				{ value: 'zhipu', label: '智谱 GLM', model: 'glm-4-flash' },
				{ value: 'deepseek', label: 'DeepSeek', model: 'deepseek-chat' }
			],
			defaultAvatar: '/static/icons/feature/robot.svg'
		}
	},
	computed: {
		sessionGroups() {
			const groups = []
			const map = {}
			this.sessions.forEach((chat) => {
				const label = this.groupLabel(chat.createTime)
				if (!map[label]) {
					map[label] = []
					groups.push(label)
				}
				map[label].push(chat)
			})
			return groups.map(label => ({ label, items: map[label] }))
		}
	},
	onLoad(options) {
		try {
			this.userInfo = JSON.parse(uni.getStorageSync(config.userInfoKey) || '{}')
		} catch (e) {
			this.userInfo = {}
		}
		this.botId = options.botId || ''
		this.botName = options.name ? decodeURIComponent(options.name) : '机器人'
		this.botAvatar = options.avatar ? decodeURIComponent(options.avatar) : ''
		this.currentProvider = uni.getStorageSync('aiProvider') || 'zhipu'
		if (this.botId) {
			uni.setStorageSync('currentBot', {
				id: this.botId,
				name: this.botName,
				avatar: this.botAvatar
			})
		}
		uni.setNavigationBarTitle({ title: this.botName })
		this.loadBotDetail()
		this.loadSessions()
	},
	methods: {
		goBack() {
			const pages = getCurrentPages()
			if (pages.length > 1) {
				uni.navigateBack()
			} else {
				uni.redirectTo({ url: '/pages/bots/bots' })
			}
		},
		nextId() {
			this.messageSeed += 1
			return 'msg-' + Date.now() + '-' + this.messageSeed
		},
		usePrompt(text) {
			this.inputText = text
		},
		scrollToBottom() {
			this.scrollInto = ''
			this.$nextTick(() => {
				this.scrollInto = 'msg-bottom'
			})
		},
		isImageMessage(msg) {
			return msg && msg.role === 'ai' && typeof msg.content === 'string' && msg.content.indexOf('img:') === 0
		},
		imageUrlOf(msg) {
			return (msg.content || '').slice(4)
		},
		previewImage(msg) {
			const url = this.imageUrlOf(msg)
			if (url) uni.previewImage({ urls: [url], current: url })
		},
		isActiveSession(chat) {
			return this.sessionId && String(this.sessionId) === String(chat.id)
		},
		parseTime(value) {
			if (!value) return 0
			if (typeof value === 'number') return value
			const t = new Date(String(value).replace(/-/g, '/')).getTime()
			return isNaN(t) ? 0 : t
		},
		groupLabel(value) {
			const t = this.parseTime(value)
			if (!t) return '更早'
			const now = new Date()
			const today = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime()
			const day = new Date(new Date(t).getFullYear(), new Date(t).getMonth(), new Date(t).getDate()).getTime()
			if (day === today) return '今天'
			if (day === today - 86400000) return '昨天'
			if (day > today - 7 * 86400000) return '最近 7 天'
			const d = new Date(t)
			return d.getFullYear() + ' 年 ' + (d.getMonth() + 1) + ' 月'
		},
		modelLabel(model) {
			if (!model) return ''
			if (model.indexOf('glm') !== -1 || model.indexOf('chatglm') !== -1) return 'GLM'
			if (model.indexOf('deepseek') !== -1) return 'DS'
			return 'AI'
		},
		modelTagClass(model) {
			if (!model) return ''
			if (model.indexOf('glm') !== -1 || model.indexOf('chatglm') !== -1) return 'tag-glm'
			if (model.indexOf('deepseek') !== -1) return 'tag-ds'
			return ''
		},
		providerModel() {
			const p = this.providerOptions.find(o => o.value === this.currentProvider)
			return p ? p.model : 'glm-4-flash'
		},
		providerLabel(value) {
			const p = this.providerOptions.find(o => o.value === value)
			return p ? p.label : value
		},
		providerFromModel(model) {
			return model && String(model).indexOf('deepseek') !== -1 ? 'deepseek' : 'zhipu'
		},
		syncProviderToSession(chat) {
			const provider = this.providerFromModel(chat && chat.model)
			this.currentProvider = provider
			uni.setStorageSync('aiProvider', provider)
		},
		async loadBotDetail() {
			if (!this.botId) return
			try {
				const res = await getBotDetail(this.botId)
				const data = res && res.data
				if (!data) return
				if (data.description) this.botDescription = data.description
				if (data.name && (!this.botName || this.botName === '机器人')) this.botName = data.name
				if (data.avatar && !this.botAvatar) this.botAvatar = data.avatar
			} catch (e) {
				console.error('加载机器人详情失败:', e)
			}
		},
		async loadSessions() {
			if (!this.botId) return
			try {
				const res = await pageSessions(this.botId, { pageNum: 1, pageSize: 50 })
				if (res && res.code === 200 && res.data) {
					const rows = res.data.rows || []
					this.sessions = rows.map(item => ({
						id: String(item.id),
						title: item.title || '未命名会话',
						createTime: item.createTime,
						model: item.model || ''
					})).sort((a, b) => this.parseTime(b.createTime) - this.parseTime(a.createTime))
				}
			} catch (e) {
				console.error('加载会话失败:', e)
			}
		},
		cancelActiveRequest() {
			this.requestSeq += 1
			if (this.streamController) {
				try {
					this.streamController.abort()
				} catch (e) {
					// ignore
				}
			}
			this.streamController = null
			this.isStreaming = false
			this.activeAiMessage = null
		},
		stopStreaming() {
			const aiMessage = this.activeAiMessage
			this.cancelActiveRequest()
			if (aiMessage && !aiMessage.content) {
				aiMessage.status = ''
				aiMessage.content = '已停止生成。'
			}
			this.scrollToBottom()
		},
		newChat() {
			this.cancelActiveRequest()
			this.sessionId = ''
			this.currentTitle = ''
			this.messages = []
			this.inputText = ''
			this.drawerOpen = false
			this.menuSessionId = ''
			uni.showToast({ title: '已开始新对话', icon: 'none' })
		},
		toggleSessionMenu(chatId) {
			this.menuSessionId = this.menuSessionId === chatId ? '' : chatId
		},
		async selectSession(chat) {
			this.menuSessionId = ''
			this.drawerOpen = false
			if (this.isActiveSession(chat)) return
			this.cancelActiveRequest()
			this.sessionId = String(chat.id)
			this.currentTitle = chat.title || '未命名会话'
			this.syncProviderToSession(chat)
			this.messages = []
			await this.loadHistory(String(chat.id))
		},
		async loadHistory(targetId) {
			this.historyLoading = true
			try {
				const res = await getHistoryMessages({ sessionId: targetId, pageNum: 1, pageSize: 100 })
				if (this.sessionId !== targetId) return
				const data = res && res.data
				const rows = (data && data.rows) || []
				this.messages = rows.map((item, index) => {
					const senderIsUser = String(item.senderId) === String(this.userInfo.id)
					const role = item.role === 'assistant' ? 'ai' : item.role === 'user' ? 'user' : (senderIsUser ? 'user' : 'ai')
					return {
						id: item.messageId || ('history-' + index),
						role,
						content: item.content || '',
						time: this.parseTime(item.createTime)
					}
				}).filter(m => m.content).sort((a, b) => (a.time || 0) - (b.time || 0))
				this.scrollToBottom()
			} catch (e) {
				if (this.sessionId === targetId) {
					this.messages = []
					uni.showToast({ title: '会话加载失败', icon: 'none' })
				}
			} finally {
				if (this.sessionId === targetId) this.historyLoading = false
			}
		},
		async switchProvider(value) {
			if (this.currentProvider === value) return
			const previous = this.currentProvider
			this.currentProvider = value
			uni.setStorageSync('aiProvider', value)
			if (!this.sessionId) return
			try {
				const res = await updateSessionModel(this.sessionId, this.providerModel())
				if (!res || res.code !== 200) throw new Error('切换失败')
				const item = this.sessions.find(s => String(s.id) === String(this.sessionId))
				if (item) item.model = this.providerModel()
				uni.showToast({ title: '已切换为' + this.providerLabel(value) + '，下条生效', icon: 'none' })
			} catch (e) {
				this.currentProvider = previous
				uni.setStorageSync('aiProvider', previous)
				uni.showToast({ title: '切换失败，请重试', icon: 'none' })
			}
		},
		async sendMessage() {
			const text = this.inputText.trim()
			if (!text) return
			if (this.isGeneratingImage) {
				uni.showToast({ title: '正在绘画，请稍候', icon: 'none' })
				return
			}
			if (this.isStreaming) {
				uni.showToast({ title: '正在生成中，请等待回复完成', icon: 'none' })
				return
			}
			if (!this.botId) return
			this.inputText = ''
			const seq = ++this.requestSeq
			const userId = this.userInfo.id || '1'

			try {
				let sessionId = this.sessionId
				if (!sessionId) {
					const res = await createSession({
						userId,
						botId: this.botId,
						input: text,
						model: this.providerModel()
					})
					if (seq !== this.requestSeq) return
					if (!res || !res.data || !res.data.id) throw new Error('会话创建失败')
					sessionId = String(res.data.id)
					this.sessionId = sessionId
					this.currentTitle = res.data.title || text
					this.sessions.unshift({
						id: sessionId,
						title: this.currentTitle,
						createTime: res.data.createTime || new Date().toISOString(),
						model: this.providerModel()
					})
				}

				this.messages.push({ id: this.nextId(), role: 'user', content: text })
				const aiMessage = { id: this.nextId(), role: 'ai', content: '', status: '正在思考…' }
				this.messages.push(aiMessage)
				this.activeAiMessage = aiMessage
				this.isStreaming = true
				this.scrollToBottom()

				this.streamController = new AbortController()
				try {
					await streamChat(
						{ userId, botId: this.botId, sessionId, input: text },
						{
							signal: this.streamController.signal,
							onStatus: (status) => {
								if (seq !== this.requestSeq || this.sessionId !== sessionId) return
								aiMessage.status = status === '正在识别意图...' ? '正在思考…' : status
							},
							onDelta: (chunk) => {
								if (this.sessionId !== sessionId) return
								aiMessage.status = ''
								aiMessage.content += chunk
								this.scrollToBottom()
							}
						}
					)
					if (!aiMessage.content) aiMessage.content = '这次没有收到回复，请再试一次。'
				} finally {
					if (seq === this.requestSeq) {
						this.isStreaming = false
						this.streamController = null
						this.activeAiMessage = null
						aiMessage.status = ''
						this.scrollToBottom()
					}
				}
			} catch (e) {
				if (e && (e.name === 'AbortError' || seq !== this.requestSeq)) return
				console.error('AI 对话失败:', e)
				const last = this.messages[this.messages.length - 1]
				if (last && last.role === 'ai') {
					last.status = ''
					last.content = e.message || '回复失败，请稍后重试。'
				}
				this.isStreaming = false
				this.activeAiMessage = null
				uni.showToast({ title: '回复失败，请稍后重试', icon: 'none' })
			}
		},
		async generateImage() {
			if (this.isGeneratingImage || this.isStreaming) {
				if (this.isStreaming) uni.showToast({ title: '正在生成中，请稍候', icon: 'none' })
				return
			}
			const prompt = this.inputText.trim()
			if (!prompt) {
				uni.showToast({ title: '请输入想画的画面描述', icon: 'none' })
				return
			}
			if (!this.botId) return
			this.isGeneratingImage = true
			try {
				const res = await generateImage({
					userId: this.userInfo.id || '1',
					botId: this.botId,
					sessionId: this.sessionId || undefined,
					prompt
				})
				if (!res || res.code !== 200 || !res.data || !res.data.url) throw new Error('绘画失败')
				const data = res.data
				if (!this.sessionId) {
					if (!data.sessionId) throw new Error('绘画会话创建失败')
					this.sessionId = String(data.sessionId)
					this.currentTitle = prompt
					this.syncProviderToSession({ model: 'glm-4-flash' })
					this.sessions.unshift({
						id: this.sessionId,
						title: prompt,
						createTime: new Date().toISOString(),
						model: 'glm-4-flash'
					})
				}
				this.inputText = ''
				this.messages.push({ id: this.nextId(), role: 'user', content: prompt })
				this.messages.push({ id: this.nextId(), role: 'ai', content: 'img:' + data.url })
				this.scrollToBottom()
			} catch (e) {
				console.error('AI 绘画失败:', e)
				uni.showToast({ title: '绘画失败，请稍后重试', icon: 'none' })
			} finally {
				this.isGeneratingImage = false
			}
		},
		startRename(chat) {
			this.menuSessionId = ''
			this.renameTarget = chat
			this.renameInput = chat.title || ''
			this.renameOpen = true
		},
		closeRename() {
			this.renameOpen = false
			this.renameTarget = null
			this.renameInput = ''
		},
		async submitRename() {
			const title = this.renameInput.trim()
			if (!title || !this.renameTarget) return
			try {
				const res = await renameSession(this.renameTarget.id, title.slice(0, 15))
				if (!res || res.code !== 200) throw new Error('重命名失败')
				this.renameTarget.title = title.slice(0, 15)
				if (String(this.sessionId) === String(this.renameTarget.id)) this.currentTitle = this.renameTarget.title
				this.closeRename()
				uni.showToast({ title: '已重命名', icon: 'success' })
			} catch (e) {
				uni.showToast({ title: '重命名失败，请重试', icon: 'none' })
			}
		},
		removeSession(chat) {
			this.menuSessionId = ''
			uni.showModal({
				title: '删除会话',
				content: '删除后这段对话记录将不可恢复。',
				confirmText: '删除',
				confirmColor: '#ff3b30',
				success: async (r) => {
					if (!r.confirm) return
					try {
						const res = await deleteSession(chat.id)
						if (!res || res.code !== 200) throw new Error('删除失败')
						this.sessions = this.sessions.filter(s => String(s.id) !== String(chat.id))
						if (String(this.sessionId) === String(chat.id)) this.newChat()
						uni.showToast({ title: '已删除', icon: 'success' })
					} catch (e) {
						uni.showToast({ title: '删除失败，请重试', icon: 'none' })
					}
				}
			})
		}
	}
}
</script>

<style lang="scss" scoped>
.bot-chat-page {
	width: 100%;
	height: 100vh;
	background: #f2f2f7;
	display: flex;
	flex-direction: column;
	overflow: hidden;
}

.top-bar {
	position: relative;
	background: rgba(255, 255, 255, 0.9);
	backdrop-filter: saturate(180%) blur(20px);
	-webkit-backdrop-filter: saturate(180%) blur(20px);
	border-bottom: 1rpx solid rgba(60, 60, 67, 0.12);
	flex-shrink: 0;
}

.top-bar-content {
	height: 96rpx;
	display: flex;
	align-items: center;
	padding: 0 20rpx;
}

.back-btn {
	width: 64rpx;
	height: 68rpx;
	display: flex;
	align-items: center;
	justify-content: center;
}

.back-icon {
	width: 34rpx;
	height: 34rpx;
	opacity: 0.6;
}

.title-wrap {
	flex: 1;
	display: flex;
	flex-direction: column;
	align-items: center;
	min-width: 0;
}

.page-title {
	font-size: 30rpx;
	font-weight: 700;
	color: #1c1c1e;
	max-width: 100%;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
}

.page-subtitle {
	font-size: 20rpx;
	color: rgba(60, 60, 67, 0.45);
	margin-top: 2rpx;
	max-width: 100%;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
}

.top-actions {
	display: flex;
	align-items: center;
	flex-shrink: 0;
}

.top-action {
	height: 64rpx;
	padding: 0 10rpx;
	display: flex;
	align-items: center;
	justify-content: center;
}

.top-action-text {
	font-size: 24rpx;
	color: rgba(60, 60, 67, 0.62);
}

.message-scroll {
	flex: 1;
	min-height: 0;
	overflow: hidden;
}

.message-scroll ::v-deep .uni-scroll-view {
	height: 100% !important;
}

.message-list {
	padding: 28rpx 24rpx 20rpx;
}

.history-state {
	padding: 60rpx 0;
	text-align: center;
	font-size: 24rpx;
	color: #8e8e93;
}

.welcome-card {
	background: #ffffff;
	border: 1rpx solid rgba(60, 60, 67, 0.1);
	border-radius: 24rpx;
	padding: 44rpx 30rpx 34rpx;
	display: flex;
	flex-direction: column;
	align-items: center;
	margin-top: 20rpx;
}

.welcome-icon {
	width: 112rpx;
	height: 112rpx;
	border-radius: 24rpx;
	background: #f2f2f7;
	overflow: hidden;
	display: flex;
	align-items: center;
	justify-content: center;
}

.welcome-icon-img {
	width: 100%;
	height: 100%;
}

.welcome-title {
	font-size: 32rpx;
	font-weight: 700;
	color: #1c1c1e;
	margin-top: 22rpx;
}

.welcome-desc {
	font-size: 24rpx;
	color: rgba(60, 60, 67, 0.6);
	line-height: 1.6;
	text-align: center;
	margin-top: 12rpx;
}

.welcome-chips {
	display: flex;
	flex-wrap: wrap;
	justify-content: center;
	margin-top: 24rpx;
}

.welcome-chip {
	border: 1rpx solid rgba(60, 60, 67, 0.16);
	border-radius: 999rpx;
	padding: 12rpx 22rpx;
	margin: 0 8rpx 10rpx;
}

.welcome-chip-text {
	font-size: 23rpx;
	color: rgba(60, 60, 67, 0.7);
}

.welcome-history {
	margin-top: 12rpx;
	padding: 10rpx 0;
}

.welcome-history-text {
	font-size: 22rpx;
	color: rgba(60, 60, 67, 0.45);
}

.message-row {
	display: flex;
	align-items: flex-start;
	margin-bottom: 22rpx;
}

.row-user {
	justify-content: flex-end;
}

.row-ai {
	justify-content: flex-start;
}

.ai-avatar {
	width: 64rpx;
	height: 64rpx;
	border-radius: 14rpx;
	background: #ffffff;
	border: 1rpx solid rgba(60, 60, 67, 0.1);
	overflow: hidden;
	flex-shrink: 0;
	margin-right: 12rpx;
}

.ai-avatar-img {
	width: 100%;
	height: 100%;
}

.bubble {
	max-width: 72%;
	padding: 18rpx 22rpx;
	border-radius: 18rpx;
	box-sizing: border-box;
}

.bubble-user {
	background: #111111;
	border-bottom-right-radius: 6rpx;
}

.bubble-ai {
	background: #ffffff;
	border: 1rpx solid rgba(60, 60, 67, 0.1);
	border-bottom-left-radius: 6rpx;
}

.bubble-text {
	display: block;
	font-size: 27rpx;
	line-height: 1.6;
	white-space: pre-wrap;
	word-break: break-word;
}

.bubble-user .bubble-text {
	color: #ffffff;
}

.bubble-ai .bubble-text {
	color: #1c1c1e;
}

.bubble-status {
	display: block;
	font-size: 23rpx;
	color: rgba(60, 60, 67, 0.45);
}

.bubble-placeholder {
	color: rgba(60, 60, 67, 0.4);
}

.bubble-image {
	display: block;
	width: 400rpx;
	height: 400rpx;
	max-width: 100%;
	border-radius: 12rpx;
	background: #f2f2f7;
	overflow: hidden;
}

.bubble-image ::v-deep div {
	width: 100% !important;
	height: 100% !important;
}

.bubble-image ::v-deep img {
	width: 100% !important;
	height: 100% !important;
	object-fit: contain;
}

.message-bottom {
	height: 8rpx;
}

.input-area {
	flex-shrink: 0;
	background: rgba(255, 255, 255, 0.94);
	border-top: 1rpx solid rgba(60, 60, 67, 0.1);
	padding: 14rpx 20rpx calc(12rpx + env(safe-area-inset-bottom));
}

.model-segment {
	display: flex;
	background: #ececf1;
	border-radius: 14rpx;
	padding: 4rpx;
	margin-bottom: 14rpx;
}

.segment-item {
	flex: 1;
	height: 52rpx;
	border-radius: 11rpx;
	display: flex;
	align-items: center;
	justify-content: center;
}

.segment-item.active {
	background: #ffffff;
	box-shadow: 0 1rpx 4rpx rgba(0, 0, 0, 0.08);
}

.segment-text {
	font-size: 22rpx;
	color: rgba(60, 60, 67, 0.6);
}

.segment-item.active .segment-text {
	color: #1c1c1e;
	font-weight: 600;
}

.input-bar {
	display: flex;
	align-items: flex-end;
}

.chat-input {
	flex: 1;
	min-height: 76rpx;
	max-height: 220rpx;
	background: #f2f2f7;
	border-radius: 16rpx;
	padding: 18rpx 20rpx;
	box-sizing: border-box;
	font-size: 27rpx;
	color: #1c1c1e;
	line-height: 1.4;
}

.paint-btn {
	width: 76rpx;
	height: 76rpx;
	margin-left: 12rpx;
	border-radius: 16rpx;
	background: #ffffff;
	border: 1rpx solid rgba(60, 60, 67, 0.14);
	display: flex;
	align-items: center;
	justify-content: center;
	flex-shrink: 0;
}

.paint-btn.disabled {
	opacity: 0.4;
}

.paint-icon {
	width: 42rpx;
	height: 42rpx;
}

.send-btn {
	width: 112rpx;
	height: 76rpx;
	margin-left: 12rpx;
	border-radius: 16rpx;
	background: #111111;
	display: flex;
	align-items: center;
	justify-content: center;
	flex-shrink: 0;
}

.stop-btn {
	background: #3a3a3c;
}

.send-disabled {
	opacity: 0.35;
}

.send-text {
	font-size: 25rpx;
	font-weight: 600;
	color: #ffffff;
}

.input-tip {
	display: block;
	margin-top: 10rpx;
	text-align: center;
	font-size: 19rpx;
	color: rgba(60, 60, 67, 0.34);
}

.drawer-mask {
	position: fixed;
	inset: 0;
	z-index: 900;
	background: rgba(0, 0, 0, 0.35);
	display: flex;
	justify-content: flex-start;
}

.drawer {
	width: 82%;
	height: 100%;
	background: #ffffff;
	display: flex;
	flex-direction: column;
	border-radius: 0 26rpx 26rpx 0;
	overflow: hidden;
}

.drawer-head {
	display: flex;
	align-items: center;
	justify-content: space-between;
	padding: 16rpx 26rpx 8rpx;
	flex-shrink: 0;
}

.drawer-title {
	font-size: 36rpx;
	font-weight: 700;
	color: #1c1c1e;
	letter-spacing: -0.02em;
}

.drawer-close {
	padding: 12rpx 0 12rpx 16rpx;
}

.drawer-close-text {
	font-size: 24rpx;
	color: rgba(60, 60, 67, 0.55);
}

.drawer-new {
	margin: 16rpx 24rpx 18rpx;
	height: 84rpx;
	border-radius: 18rpx;
	background: #111111;
	display: flex;
	align-items: center;
	justify-content: center;
	flex-shrink: 0;
}

.drawer-new-plus {
	font-size: 32rpx;
	color: #ffffff;
	margin-right: 8rpx;
}

.drawer-new-text {
	font-size: 27rpx;
	font-weight: 600;
	color: #ffffff;
}

.session-scroll {
	flex: 1;
	min-height: 0;
	padding: 0 16rpx;
	overflow: hidden;
}

.session-scroll ::v-deep .uni-scroll-view {
	height: 100% !important;
}

.session-empty {
	padding: 60rpx 20rpx;
	display: flex;
	flex-direction: column;
	align-items: center;
}

.session-empty-title {
	font-size: 26rpx;
	color: #3a3a3c;
}

.session-empty-desc {
	font-size: 22rpx;
	color: #8e8e93;
	margin-top: 10rpx;
}

.session-group {
	margin-bottom: 16rpx;
}

.session-group-label {
	display: block;
	font-size: 21rpx;
	color: rgba(60, 60, 67, 0.42);
	padding: 14rpx 10rpx 8rpx;
}

.session-item-wrap {
	position: relative;
}

.session-item {
	display: flex;
	align-items: center;
	padding: 18rpx 12rpx;
	border-radius: 14rpx;
	gap: 8rpx;
}

.session-item.active {
	background: #f2f2f7;
}

.session-title {
	flex: 1;
	min-width: 0;
	font-size: 25rpx;
	color: #1c1c1e;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
}

.session-model {
	flex-shrink: 0;
	font-size: 18rpx;
	padding: 3rpx 8rpx;
	border-radius: 6rpx;
	background: #f2f2f7;
	color: #8e8e93;
}

.session-model.tag-glm {
	background: rgba(52, 199, 89, 0.12);
	color: #248a3d;
}

.session-model.tag-ds {
	background: rgba(0, 122, 255, 0.1);
	color: #007aff;
}

.session-more {
	width: 44rpx;
	height: 44rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	flex-shrink: 0;
}

.session-more-text {
	font-size: 26rpx;
	color: rgba(60, 60, 67, 0.5);
	letter-spacing: 2rpx;
}

.session-menu {
	position: absolute;
	right: 8rpx;
	top: 64rpx;
	z-index: 10;
	background: #ffffff;
	border-radius: 12rpx;
	box-shadow: 0 8rpx 30rpx rgba(0, 0, 0, 0.14);
	padding: 6rpx 0;
	min-width: 170rpx;
}

.session-menu-item {
	padding: 16rpx 24rpx;
}

.session-menu-text {
	font-size: 24rpx;
	color: #1c1c1e;
}

.danger-text {
	color: #ff3b30;
}

.drawer-footer {
	flex-shrink: 0;
	display: flex;
	align-items: center;
	gap: 14rpx;
	padding: 18rpx 26rpx calc(22rpx + env(safe-area-inset-bottom));
	border-top: 1rpx solid rgba(60, 60, 67, 0.1);
}

.drawer-bot-avatar {
	width: 56rpx;
	height: 56rpx;
	border-radius: 12rpx;
	background: #f2f2f7;
}

.drawer-bot-info {
	display: flex;
	flex-direction: column;
	gap: 2rpx;
}

.drawer-bot-name {
	font-size: 24rpx;
	font-weight: 600;
	color: #1c1c1e;
}

.drawer-bot-desc {
	font-size: 19rpx;
	color: #8e8e93;
}

.dialog-mask {
	position: fixed;
	inset: 0;
	z-index: 1000;
	background: rgba(0, 0, 0, 0.35);
	display: flex;
	align-items: center;
	justify-content: center;
	padding: 0 48rpx;
}

.dialog {
	width: 100%;
	background: #ffffff;
	border-radius: 26rpx;
	padding: 32rpx 28rpx 24rpx;
	box-sizing: border-box;
}

.dialog-title {
	display: block;
	text-align: center;
	font-size: 30rpx;
	font-weight: 700;
	color: #1c1c1e;
}

.dialog-input {
	margin-top: 22rpx;
	height: 84rpx;
	border-radius: 14rpx;
	background: #f2f2f7;
	padding: 0 20rpx;
	font-size: 26rpx;
	color: #1c1c1e;
	box-sizing: border-box;
}

.dialog-actions {
	margin-top: 24rpx;
	display: flex;
	gap: 16rpx;
}

.dialog-btn {
	flex: 1;
	height: 80rpx;
	border-radius: 16rpx;
	background: #f2f2f7;
	display: flex;
	align-items: center;
	justify-content: center;
}

.dialog-btn.primary {
	background: #111111;
}

.dialog-btn-text {
	font-size: 26rpx;
	color: #3a3a3c;
}

.primary-text {
	color: #ffffff;
	font-weight: 600;
}
</style>
