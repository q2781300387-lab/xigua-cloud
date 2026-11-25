<template>
	<view class="search-page">
		<view class="nav" :style="{ paddingTop: statusBarHeight + 'px' }">
			<view class="nav-row">
				<view class="back" hover-class="back-hover" @tap="goBack">
					<text class="back-arrow">‹</text>
				</view>
				<view class="search-box">
					<image class="search-icon" src="/static/icons/search/search.svg" mode="aspectFit" />
					<input
						class="search-input"
						v-model="keyword"
						:focus="autoFocus"
						confirm-type="search"
						placeholder="搜索聊天记录、联系人、群聊"
						placeholder-class="search-placeholder"
						@confirm="runSearch(true)"
						@input="onInput"
					/>
					<view v-if="keyword" class="clear" @tap="clearKeyword">
						<text class="clear-text">×</text>
					</view>
				</view>
				<text class="cancel" @tap="goBack">取消</text>
			</view>

			<scroll-view class="filters" scroll-x :show-scrollbar="false">
				<view
					v-for="item in scopes"
					:key="'scope-' + item.value"
					class="filter-chip"
					:class="{ active: scope === item.value }"
					@tap="changeScope(item.value)"
				>
					<text class="filter-text">{{ item.label }}<text v-if="scopeCount(item.value)" class="filter-count"> {{ scopeCount(item.value) }}</text></text>
				</view>
			</scroll-view>

			<scroll-view v-if="scope === 'messages'" class="filters type-filters" scroll-x :show-scrollbar="false">
				<view
					v-for="item in filters"
					:key="'type-' + item.label"
					class="filter-chip filter-chip-sm"
					:class="{ active: messageType === item.value }"
					@tap="changeFilter(item.value)"
				>
					<text class="filter-text">{{ item.label }}</text>
				</view>
			</scroll-view>
		</view>

		<scroll-view
			class="results"
			scroll-y
			:style="{ paddingTop: navHeight + 'px' }"
			@scrolltolower="loadMore"
			:lower-threshold="120"
		>
			<view v-if="(loading || aggregateLoading) && !results.length && !contacts.length && !groups.length && !files.length" class="state">
				<view class="spinner"></view>
				<text class="state-text">正在搜索…</text>
			</view>

			<view v-else-if="!hasSearched" class="hints">
				<text class="hint-title">搜索范围</text>
				<view class="hint-row"><text class="hint-dot"></text><text class="hint-text">全部单聊、群聊与机器人会话</text></view>
				<view class="hint-row"><text class="hint-dot"></text><text class="hint-text">好友、已加入群聊与群文件</text></view>
				<view class="hint-row"><text class="hint-dot"></text><text class="hint-text">文本、图片、文件、语音等消息类型</text></view>
			</view>

			<template v-else>
				<view v-if="scope === 'all'" class="result-list">
					<text v-if="aggregateError" class="inline-error">联系人、群、文件搜索暂不可用</text>

					<view v-if="contacts.length" class="section">
						<view class="section-head">
							<text class="section-title">联系人</text>
							<text class="section-more" @tap="changeScope('contacts')">全部 {{ contacts.length }}</text>
						</view>
						<view v-for="item in contacts.slice(0, 3)" :key="'c-' + item.id" class="result-card" hover-class="result-card-hover" @tap="openContact(item)">
							<view class="avatar">
								<image v-if="item.avatar" class="avatar-img" :src="item.avatar" mode="aspectFill" @error="item.avatar = ''" />
								<text v-else class="avatar-letter">{{ initial(item.name) }}</text>
							</view>
							<view class="result-main">
								<view class="result-top"><text class="conversation">{{ item.name }}</text><text class="badge">联系人</text></view>
								<text class="snippet">{{ item.signature || (item.online ? '在线' : '好友') }}</text>
							</view>
							<text class="chevron">›</text>
						</view>
					</view>

					<view v-if="groups.length" class="section">
						<view class="section-head">
							<text class="section-title">群聊</text>
							<text class="section-more" @tap="changeScope('groups')">全部 {{ groups.length }}</text>
						</view>
						<view v-for="item in groups.slice(0, 3)" :key="'g-' + item.id" class="result-card" hover-class="result-card-hover" @tap="openGroup(item)">
							<view class="avatar avatar-group">
								<image v-if="item.avatar" class="avatar-img" :src="item.avatar" mode="aspectFill" @error="item.avatar = ''" />
								<text v-else class="avatar-letter">{{ initial(item.name) }}</text>
							</view>
							<view class="result-main">
								<view class="result-top"><text class="conversation">{{ item.name }}</text><text class="badge">群聊</text></view>
								<text class="snippet">{{ item.memberCount || 0 }} 位成员</text>
							</view>
							<text class="chevron">›</text>
						</view>
					</view>

					<view v-if="files.length" class="section">
						<view class="section-head">
							<text class="section-title">群文件</text>
							<text class="section-more" @tap="changeScope('files')">全部 {{ files.length }}</text>
						</view>
						<view v-for="item in files.slice(0, 3)" :key="'f-' + item.id" class="result-card" hover-class="result-card-hover" @tap="openFile(item)">
							<view class="avatar avatar-file"><text class="avatar-letter">{{ fileInitial(item.name) }}</text></view>
							<view class="result-main">
								<view class="result-top"><text class="conversation">{{ item.name }}</text><text class="time">{{ formatSize(item.size) }}</text></view>
								<text class="snippet">{{ item.groupName || '群文件' }}</text>
							</view>
							<text class="chevron">›</text>
						</view>
					</view>

					<view v-if="results.length" class="section">
						<view class="section-head">
							<text class="section-title">消息</text>
							<text class="section-more" @tap="changeScope('messages')">全部 {{ totalCount }}</text>
						</view>
						<view v-for="item in results.slice(0, 5)" :key="'m-' + item.id" class="result-card" hover-class="result-card-hover" @tap="openResult(item)">
							<view class="avatar">
								<image v-if="item.conversationAvatar" class="avatar-img" :src="item.conversationAvatar" mode="aspectFill" @error="item.conversationAvatar = ''" />
								<text v-else class="avatar-letter">{{ initial(item.conversationName) }}</text>
							</view>
							<view class="result-main">
								<view class="result-top">
									<text class="conversation">{{ item.conversationName || '会话' }}</text>
									<text v-if="Number(item.conversationChatType) === 2" class="badge">群聊</text>
									<text class="time">{{ formatTime(item.createTime) }}</text>
								</view>
								<text class="snippet">{{ plainSnippet(item) }}</text>
							</view>
							<text class="chevron">›</text>
						</view>
					</view>

					<view v-if="!aggregateLoading && !contacts.length && !groups.length && !files.length && !results.length" class="state state-compact">
						<text class="empty-title">没有找到相关内容</text>
						<text class="empty-sub">换个关键词试试</text>
					</view>
				</view>

				<view v-else-if="scope === 'contacts'" class="result-list">
					<text class="result-count">找到 {{ contacts.length }} 位联系人</text>
					<view v-for="item in contacts" :key="'cl-' + item.id" class="result-card" hover-class="result-card-hover" @tap="openContact(item)">
						<view class="avatar">
							<image v-if="item.avatar" class="avatar-img" :src="item.avatar" mode="aspectFill" @error="item.avatar = ''" />
							<text v-else class="avatar-letter">{{ initial(item.name) }}</text>
						</view>
						<view class="result-main">
							<view class="result-top"><text class="conversation">{{ item.name }}</text><text class="time">{{ item.online ? '在线' : '' }}</text></view>
							<text class="snippet">{{ item.signature || '我的好友' }}</text>
						</view>
						<text class="chevron">›</text>
					</view>
					<view v-if="!contacts.length && !aggregateLoading" class="state state-compact"><text class="state-text">没有匹配的联系人</text></view>
				</view>

				<view v-else-if="scope === 'groups'" class="result-list">
					<text class="result-count">找到 {{ groups.length }} 个群聊</text>
					<view v-for="item in groups" :key="'gl-' + item.id" class="result-card" hover-class="result-card-hover" @tap="openGroup(item)">
						<view class="avatar avatar-group">
							<image v-if="item.avatar" class="avatar-img" :src="item.avatar" mode="aspectFill" @error="item.avatar = ''" />
							<text v-else class="avatar-letter">{{ initial(item.name) }}</text>
						</view>
						<view class="result-main">
							<view class="result-top"><text class="conversation">{{ item.name }}</text><text class="badge">群聊</text></view>
							<text class="snippet">{{ item.memberCount || 0 }} 位成员</text>
						</view>
						<text class="chevron">›</text>
					</view>
					<view v-if="!groups.length && !aggregateLoading" class="state state-compact"><text class="state-text">没有匹配的群聊</text></view>
				</view>

				<view v-else-if="scope === 'files'" class="result-list">
					<text class="result-count">找到 {{ files.length }} 个文件</text>
					<view v-for="item in files" :key="'fl-' + item.id" class="result-card" hover-class="result-card-hover" @tap="openFile(item)">
						<view class="avatar avatar-file"><text class="avatar-letter">{{ fileInitial(item.name) }}</text></view>
						<view class="result-main">
							<view class="result-top"><text class="conversation">{{ item.name }}</text><text class="time">{{ formatSize(item.size) }}</text></view>
							<text class="snippet">{{ item.groupName || '群文件' }}<text v-if="item.uploaderName"> · {{ item.uploaderName }}</text></text>
						</view>
						<text class="chevron">›</text>
					</view>
					<view v-if="!files.length && !aggregateLoading" class="state state-compact"><text class="state-text">没有匹配的群文件</text></view>
				</view>

				<view v-else class="result-list">
					<text class="result-count">共 {{ totalCount }} 条消息</text>
					<view v-if="!results.length && !loading" class="state state-compact">
						<text class="empty-title">没有找到相关内容</text>
						<text class="empty-sub">换个关键词，或把类型切回“全部”</text>
					</view>
					<view
						v-for="item in results"
						:key="item.id"
						class="result-card"
						hover-class="result-card-hover"
						@tap="openResult(item)"
					>
						<view class="avatar">
							<image v-if="item.conversationAvatar" class="avatar-img" :src="item.conversationAvatar" mode="aspectFill" @error="item.conversationAvatar = ''" />
							<text v-else class="avatar-letter">{{ initial(item.conversationName) }}</text>
						</view>
						<view class="result-main">
							<view class="result-top">
								<text class="conversation">{{ item.conversationName || '会话' }}</text>
								<text v-if="Number(item.conversationChatType) === 2" class="badge">群聊</text>
								<text class="time">{{ formatTime(item.createTime) }}</text>
							</view>
							<text class="snippet">{{ plainSnippet(item) }}</text>
							<view class="result-meta">
								<text class="sender">{{ item.senderName || '对方' }}</text>
								<text v-if="Number(item.messageType) !== 1" class="type">{{ typeLabel(item.messageType) }}</text>
							</view>
						</view>
						<text class="chevron">›</text>
					</view>

					<view v-if="loadingMore" class="more"><text class="more-text">加载中…</text></view>
					<view v-else-if="!hasMore && results.length" class="more"><text class="more-text">没有更多了</text></view>
				</view>
			</template>
		</scroll-view>
	</view>
</template>

<script>
import { searchGlobalMessages, searchContactsGroupsFiles, getLastMes } from '@/api/chat.js'

const FILTERS = [
	{ label: '全部', value: null },
	{ label: '文本', value: 1 },
	{ label: '图片', value: 2 },
	{ label: '文件', value: 4 }
]

const SCOPES = [
	{ label: '综合', value: 'all' },
	{ label: '消息', value: 'messages' },
	{ label: '联系人', value: 'contacts' },
	{ label: '群', value: 'groups' },
	{ label: '文件', value: 'files' }
]

const TYPE_LABELS = { 1: '文本', 2: '图片', 3: '语音', 4: '文件', 5: '视频', 7: '聊天记录', 8: '群投票', 9: '视频通话' }

export default {
	data() {
		return {
			statusBarHeight: 20,
			navHeight: 178,
			autoFocus: true,
			keyword: '',
			scope: 'all',
			scopes: SCOPES,
			messageType: null,
			filters: FILTERS,
			contacts: [],
			groups: [],
			files: [],
			aggregateLoading: false,
			aggregateError: '',
			aggregateSeq: 0,
			results: [],
			totalCount: 0,
			pageNum: 1,
			pageSize: 20,
			hasMore: true,
			loading: false,
			loadingMore: false,
			hasSearched: false,
			requestSeq: 0,
			inputTimer: null
		}
	},
	onLoad() {
		try {
			const info = uni.getSystemInfoSync()
			this.statusBarHeight = info.statusBarHeight || 20
			this.navHeight = this.statusBarHeight + 170
		} catch (e) {}
	},
	methods: {
		onInput() {
			if (this.inputTimer) clearTimeout(this.inputTimer)
			const value = this.keyword.trim()
			if (!value) {
				this.results = []
				this.contacts = []
				this.groups = []
				this.files = []
				this.totalCount = 0
				this.hasSearched = false
				this.aggregateError = ''
				return
			}
			this.inputTimer = setTimeout(() => this.runAll(), 420)
		},
		changeScope(value) {
			if (this.scope === value) return
			this.scope = value
		},
		scopeCount(value) {
			if (!this.hasSearched) return 0
			if (value === 'contacts') return this.contacts.length
			if (value === 'groups') return this.groups.length
			if (value === 'files') return this.files.length
			if (value === 'messages') return this.totalCount
			return this.contacts.length + this.groups.length + this.files.length + this.totalCount
		},
		changeFilter(value) {
			if (this.messageType === value && this.scope === 'messages') return
			this.messageType = value
			this.scope = 'messages'
			if (this.keyword.trim()) this.runSearch(true)
		},
		clearKeyword() {
			this.keyword = ''
			this.results = []
			this.contacts = []
			this.groups = []
			this.files = []
			this.totalCount = 0
			this.hasSearched = false
			this.aggregateError = ''
			this.autoFocus = true
		},
		async runAll() {
			const keyword = this.keyword.trim()
			if (!keyword) return
			this.hasSearched = true
			this.runSearch(true)
			this.runAggregate(keyword)
		},
		async runAggregate(keyword) {
			const seq = ++this.aggregateSeq
			this.aggregateLoading = true
			this.aggregateError = ''
			try {
				const res = await searchContactsGroupsFiles(keyword)
				if (seq !== this.aggregateSeq) return
				if (!res || res.code !== 200) throw new Error((res && (res.msg || res.message)) || '搜索失败')
				const data = res.data || {}
				this.contacts = Array.isArray(data.contacts) ? data.contacts : []
				this.groups = Array.isArray(data.groups) ? data.groups : []
				this.files = Array.isArray(data.files) ? data.files : []
			} catch (e) {
				if (seq !== this.aggregateSeq) return
				this.contacts = []
				this.groups = []
				this.files = []
				this.aggregateError = e.message || '搜索服务暂时不可用'
			} finally {
				if (seq === this.aggregateSeq) this.aggregateLoading = false
			}
		},
		async runSearch(reset) {
			const keyword = this.keyword.trim()
			if (!keyword) return
			if (reset) {
				this.pageNum = 1
				this.hasMore = true
				this.results = []
			}
			const seq = ++this.requestSeq
			this.loading = true
			try {
				const res = await searchGlobalMessages({
					keyword,
					pageNum: this.pageNum,
					pageSize: this.pageSize,
					messageType: this.messageType
				})
				if (seq !== this.requestSeq) return
				if (!res || res.code !== 200) throw new Error((res && (res.msg || res.message)) || '搜索失败')
				const data = res.data || {}
				const rows = data.rows || []
				this.results = reset ? rows : this.results.concat(rows)
				this.totalCount = Number(data.totalCount || 0)
				this.hasMore = this.results.length < this.totalCount
				this.hasSearched = true
			} catch (e) {
				if (seq !== this.requestSeq) return
				uni.showToast({ title: e.message || '搜索服务暂时不可用', icon: 'none' })
			} finally {
				if (seq === this.requestSeq) this.loading = false
			}
		},
		async loadMore() {
			if (this.scope !== 'messages') return
			if (this.loading || this.loadingMore || !this.hasMore) return
			this.pageNum += 1
			this.loadingMore = true
			try {
				await this.runSearch(false)
			} finally {
				this.loadingMore = false
			}
		},
		plainSnippet(item) {
			const raw = String(item.highlightMessage || item.message || '')
			const text = raw.replace(/<[^>]+>/g, '')
			return text.length > 160 ? text.slice(0, 160) + '…' : text || '[消息]'
		},
		typeLabel(type) {
			return TYPE_LABELS[Number(type)] || '消息'
		},
		initial(name) {
			return String(name || '会').trim().charAt(0) || '会'
		},
		formatTime(value) {
			if (!value) return ''
			const date = new Date(String(value).replace(' ', 'T'))
			if (Number.isNaN(date.getTime())) return ''
			const now = new Date()
			const sameYear = date.getFullYear() === now.getFullYear()
			const mm = String(date.getMonth() + 1).padStart(2, '0')
			const dd = String(date.getDate()).padStart(2, '0')
			if (sameYear) return `${mm}-${dd}`
			return `${date.getFullYear()}-${mm}-${dd}`
		},
		openContact(item) {
			if (!item || !item.id) return
			this.openConversation(item.id, item.name || '联系人', item.avatar || '', 1)
		},
		openGroup(item) {
			if (!item || !item.id) return
			this.openConversation(item.id, item.name || '群聊', item.avatar || '', 2)
		},
		openConversation(id, name, avatar, chatType) {
			const currentChat = {
				chatId: String(id),
				chatType,
				chatName: name,
				avatar,
				lastMessageContent: null,
				unreadCount: 0,
				isOnline: false,
				updateTime: Date.now(),
				highlightMessageId: ''
			}
			try {
				uni.setStorageSync('currentChat', JSON.stringify(currentChat))
			} catch (e) {}
			uni.navigateTo({
				url: `/pages/chat/chat-detail?id=${encodeURIComponent(currentChat.chatId)}&name=${encodeURIComponent(name)}&chatType=${chatType}`
			})
		},
		openFile(item) {
			if (!item || !item.url) return
			// #ifdef H5
			window.open(item.url, '_blank', 'noopener')
			// #endif
			// #ifndef H5
			uni.showLoading({ title: '正在打开…' })
			uni.downloadFile({
				url: item.url,
				success: (res) => {
					uni.hideLoading()
					if (res.statusCode === 200) {
						uni.openDocument({ filePath: res.tempFilePath, showMenu: true })
					}
				},
				fail: () => uni.hideLoading()
			})
			// #endif
		},
		formatSize(size) {
			const value = Number(size)
			if (!value || value < 0) return ''
			if (value < 1024) return `${value} B`
			if (value < 1024 * 1024) return `${(value / 1024).toFixed(1)} KB`
			if (value < 1024 * 1024 * 1024) return `${(value / 1024 / 1024).toFixed(1)} MB`
			return `${(value / 1024 / 1024 / 1024).toFixed(1)} GB`
		},
		fileInitial(name) {
			const text = String(name || '')
			const parts = text.split('.')
			return parts.length > 1 ? parts.pop().slice(0, 3).toUpperCase() : 'FILE'
		},
		openResult(item) {
			if (!item || !item.conversationId) return
			const chatType = Number(item.conversationChatType) || 1
			const currentChat = {
				chatId: String(item.conversationId),
				chatType,
				chatName: item.conversationName || '会话',
				avatar: item.conversationAvatar || '',
				lastMessageContent: null,
				unreadCount: 0,
				isOnline: false,
				updateTime: Date.now(),
				highlightMessageId: String(item.id || '')
			}
			try {
				uni.setStorageSync('currentChat', JSON.stringify(currentChat))
			} catch (e) {}
			uni.navigateTo({
				url: `/pages/chat/chat-detail?id=${encodeURIComponent(currentChat.chatId)}&name=${encodeURIComponent(currentChat.chatName)}&chatType=${chatType}`
			})
		},
		goBack() {
			uni.navigateBack({
				fail: () => uni.switchTab({ url: '/pages/chat/chat-list' })
			})
		}
	}
}
</script>

<style scoped>
.search-page {
	min-height: 100vh;
	background: #f2f2f7;
	display: flex;
	flex-direction: column;
}

.nav {
	position: fixed;
	top: 0;
	left: 0;
	right: 0;
	z-index: 20;
	background: rgba(255, 255, 255, 0.92);
	backdrop-filter: blur(18px);
	-webkit-backdrop-filter: blur(18px);
	border-bottom: 0.5px solid rgba(0, 0, 0, 0.08);
	padding: 0 12px 10px;
	box-sizing: border-box;
}

.nav-row {
	display: flex;
	align-items: center;
	height: 52px;
	gap: 8px;
}

.back {
	width: 30px;
	height: 36px;
	display: flex;
	align-items: center;
	justify-content: center;
	flex-shrink: 0;
}

.back-hover { opacity: 0.55; }

.back-arrow {
	font-size: 30px;
	line-height: 1;
	color: #111;
	margin-top: -4px;
}

.search-box {
	flex: 1;
	height: 38px;
	border-radius: 12px;
	background: #efeff2;
	display: flex;
	align-items: center;
	padding: 0 10px;
	gap: 6px;
	min-width: 0;
}

.search-icon { width: 16px; height: 16px; flex-shrink: 0; opacity: 0.55; }

.search-input {
	flex: 1;
	height: 38px;
	font-size: 14px;
	color: #111;
	min-width: 0;
}

.search-placeholder { color: #9a9aa0; }

.clear {
	width: 18px;
	height: 18px;
	border-radius: 9px;
	background: #c7c7cc;
	display: flex;
	align-items: center;
	justify-content: center;
}

.clear-text { font-size: 12px; color: #fff; line-height: 1; }

.cancel {
	font-size: 14px;
	color: #111;
	padding: 0 4px;
	flex-shrink: 0;
}

.filters {
	white-space: nowrap;
	width: 100%;
	margin-top: 6px;
}

.filter-chip {
	display: inline-flex;
	align-items: center;
	justify-content: center;
	height: 28px;
	padding: 0 14px;
	margin-right: 8px;
	border-radius: 14px;
	background: #efeff2;
}

.filter-chip.active { background: #111; }
.filter-text { font-size: 12px; color: #555; }
.filter-chip.active .filter-text { color: #fff; font-weight: 600; }
.filter-count { font-size: 11px; opacity: 0.72; }

.type-filters { margin-top: 6px; }
.filter-chip-sm { height: 25px; padding: 0 11px; margin-right: 6px; border-radius: 12px; }
.filter-chip-sm .filter-text { font-size: 11px; }

.section { margin-bottom: 10px; }
.section-head { display: flex; align-items: center; justify-content: space-between; padding: 10px 2px 6px; }
.section-title { font-size: 12px; color: #8e8e93; font-weight: 600; }
.section-more { font-size: 12px; color: #111; }
.inline-error {
	display: block;
	margin: 8px 2px;
	padding: 8px 10px;
	border-radius: 10px;
	background: rgba(255, 149, 0, 0.12);
	color: #b45309;
	font-size: 12px;
}
.state-compact { padding-top: 36px; }
.avatar-group { background: #1c7c3e; }
.avatar-file { background: #3a3a3c; }
.avatar-file .avatar-letter { font-size: 11px; letter-spacing: 0.02em; }

.results {
	flex: 1;
	box-sizing: border-box;
	padding-left: 14px;
	padding-right: 14px;
}

.state {
	padding-top: 90px;
	display: flex;
	flex-direction: column;
	align-items: center;
	gap: 10px;
}

.spinner {
	width: 22px;
	height: 22px;
	border-radius: 50%;
	border: 2px solid #ddd;
	border-top-color: #111;
	animation: spin 0.8s linear infinite;
}

@keyframes spin { to { transform: rotate(360deg); } }

.state-text { font-size: 13px; color: #8e8e93; }
.empty-title { font-size: 16px; font-weight: 600; color: #333; }
.empty-sub { font-size: 12px; color: #8e8e93; text-align: center; padding: 0 40px; }

.hints { padding-top: 30px; }
.hint-title { font-size: 12px; color: #8e8e93; margin-bottom: 14px; display: block; }
.hint-row { display: flex; align-items: center; gap: 8px; margin-bottom: 10px; }
.hint-dot { width: 5px; height: 5px; border-radius: 50%; background: #c7c7cc; }
.hint-text { font-size: 13px; color: #666; }

.result-count { display: block; font-size: 12px; color: #8e8e93; padding: 12px 2px 8px; }

.result-card {
	background: #fff;
	border-radius: 14px;
	padding: 12px;
	display: flex;
	align-items: flex-start;
	gap: 10px;
	margin-bottom: 8px;
}

.result-card-hover { background: #f7f7f9; }

.avatar {
	width: 42px;
	height: 42px;
	border-radius: 12px;
	background: #111;
	display: flex;
	align-items: center;
	justify-content: center;
	overflow: hidden;
	flex-shrink: 0;
}

.avatar-img { width: 42px; height: 42px; }
.avatar-letter { color: #fff; font-size: 16px; font-weight: 600; }

.result-main { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 5px; }

.result-top { display: flex; align-items: center; gap: 6px; }
.conversation { font-size: 14px; font-weight: 600; color: #111; max-width: 55%; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.badge { font-size: 10px; color: #666; background: #f0f0f3; border-radius: 6px; padding: 1px 5px; flex-shrink: 0; }
.time { margin-left: auto; font-size: 11px; color: #9a9aa0; flex-shrink: 0; }

.snippet { font-size: 13px; color: #444; line-height: 1.45; word-break: break-all; }
.result-meta { display: flex; align-items: center; gap: 8px; }
.sender { font-size: 11px; color: #8e8e93; }
.type { font-size: 10px; color: #666; background: #f2f2f5; border-radius: 5px; padding: 1px 5px; }
.chevron { font-size: 20px; color: #c7c7cc; line-height: 42px; flex-shrink: 0; }

.more { padding: 16px 0 26px; display: flex; justify-content: center; }
.more-text { font-size: 12px; color: #9a9aa0; }
</style>
