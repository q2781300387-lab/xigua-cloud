<template>
	<view class="bots-page">
		<view class="top-bar safe-top-bar">
			<view class="top-bar-content">
				<view class="back-btn" @click="goBack">
					<image class="back-icon" src="/static/icons/arrow/left.svg" mode="aspectFit"></image>
				</view>
				<text class="page-title">机器人</text>
				<view class="top-action" @click="openCreate">
					<text class="top-action-text">新建</text>
				</view>
			</view>
		</view>

		<scroll-view class="main-content" scroll-y>
			<view class="content-wrapper">
				<view class="intro-card">
					<text class="intro-title">创建属于你的 AI 角色</text>
					<text class="intro-desc">设定角色、语气和能力，然后开始对话。每个机器人都会记住自己的设定。</text>
				</view>

				<view v-if="loading" class="state-card">
					<text class="state-title">正在加载机器人…</text>
				</view>
				<view v-else-if="loadFailed" class="state-card" @click="loadBots">
					<text class="state-title">加载失败</text>
					<text class="state-desc">点击重试</text>
				</view>
				<view v-else-if="bots.length === 0" class="state-card">
					<text class="state-title">还没有机器人</text>
					<text class="state-desc">点右上角“新建”，创建第一个 AI 角色</text>
					<view class="empty-create" @click="openCreate">
						<text class="empty-create-text">新建机器人</text>
					</view>
				</view>

				<view v-for="bot in bots" :key="bot.id" class="bot-card" @click="openDetail(bot)" @longpress="removeBot(bot)">
					<image class="bot-avatar" :src="bot.avatar || defaultAvatar" mode="aspectFit" @error="handleAvatarError"></image>
					<view class="bot-info">
						<text class="bot-name">{{ bot.name || '未命名机器人' }}</text>
						<text class="bot-desc">{{ bot.description || '一个可以随时对话的 AI 角色' }}</text>
					</view>
					<image class="bot-arrow" src="/static/icons/arrow/right.svg" mode="aspectFit"></image>
				</view>

				<view v-if="bots.length > 0" class="bot-actions">
					<text class="bot-actions-tip">点任意机器人查看详情，长按可删除</text>
				</view>
			</view>
		</scroll-view>

		<view v-if="createOpen" class="sheet-mask" @click="closeCreate">
			<view class="sheet-panel" @click.stop>
				<view class="sheet-handle"></view>
				<view class="sheet-head">
					<text class="sheet-cancel" @click="closeCreate">取消</text>
					<text class="sheet-title">{{ editingBotId ? '编辑机器人' : '新建机器人' }}</text>
					<text class="sheet-submit" :class="{ 'sheet-submit-disabled': creating }" @click="submitCreate">{{ creating ? (editingBotId ? '保存中' : '创建中') : (editingBotId ? '保存' : '创建') }}</text>
				</view>

				<view class="avatar-row">
					<view class="avatar-picker" @click="chooseAvatar">
						<image class="avatar-picker-img" :src="form.avatarPreview || form.avatar || defaultAvatar" mode="aspectFill" @error="handleAvatarError"></image>
						<view class="avatar-picker-badge">{{ uploadingAvatar ? '…' : '+' }}</view>
					</view>
					<view class="avatar-row-info">
						<text class="avatar-row-title">机器人头像</text>
						<text class="avatar-row-desc">{{ uploadingAvatar ? '上传中…' : '点击上传并裁剪，8MB 内' }}</text>
					</view>
				</view>

				<view class="form-row">
					<text class="form-label">名字</text>
					<input class="form-input" v-model="form.name" placeholder="例如：写作助手" maxlength="20" />
				</view>
				<view class="form-row">
					<text class="form-label">简介</text>
					<input class="form-input" v-model="form.description" placeholder="一句话说明它擅长什么" maxlength="80" />
				</view>
				<view class="form-block">
					<text class="form-label">角色设定</text>
					<textarea
						class="form-textarea"
						v-model="form.prompt"
						placeholder="描述角色、语气、能力和边界，例如：你是一位耐心的高数老师，回答要一步步推导。"
						maxlength="2000"
						:adjust-position="true"
					></textarea>
					<text class="form-count">{{ form.prompt.length }}/2000</text>
				</view>
				<view class="form-tip">
					<text class="form-tip-text">头像与名字会出现在对话页，创建后可直接开始聊天</text>
				</view>
			</view>
		</view>

		<view v-if="detailBot" class="sheet-mask" @click="closeDetail">
			<view class="detail-panel" @click.stop>
				<view class="sheet-handle"></view>
				<image class="detail-avatar" :src="detailBot.avatar || defaultAvatar" mode="aspectFill" @error="handleAvatarError"></image>
				<text class="detail-name">{{ detailBot.name || '未命名机器人' }}</text>
				<text class="detail-desc">{{ detailBot.description || '一个可以随时对话的 AI 角色' }}</text>
				<view class="detail-enter" @click="openChat(detailBot)">
					<text class="detail-enter-text">进入聊天 →</text>
				</view>
				<text class="detail-edit" @click="openEdit(detailBot)">编辑配置</text>
				<text class="detail-delete" @click="removeBot(detailBot)">删除机器人</text>
				<text class="detail-tip">角色设定只用于对话，不会在这里公开显示</text>
			</view>
		</view>

		<AvatarCropper
			:visible="cropOpen"
			:src="cropSrc"
			@cancel="closeAvatarCrop"
			@confirm="confirmAvatarCrop"
			@error="handleCropError"
		/>
	</view>
</template>

<script>
import { getBots, getBotDetail, createBot, updateBot, deleteBot } from '@/api/bot.js'
import { uploadFile } from '@/api/file.js'
import AvatarCropper from '@/components/avatar-cropper/avatar-cropper.vue'

export default {
	components: {
		AvatarCropper
	},
	data() {
		return {
			bots: [],
			loading: true,
			loadFailed: false,
			createOpen: false,
			creating: false,
			uploadingAvatar: false,
			editingBotId: null,
			detailBot: null,
			cropOpen: false,
			cropSrc: '',
			form: {
				name: '',
				description: '',
				prompt: '',
				avatar: '',
				avatarPreview: ''
			},
			defaultAvatar: '/static/icons/feature/robot.svg'
		}
	},
	onLoad() {
		this.loadBots()
	},
	onShow() {
		if (!this.loading && this.bots.length === 0) this.loadBots()
	},
	methods: {
		goBack() {
			const pages = getCurrentPages()
			if (pages.length > 1) {
				uni.navigateBack()
			} else {
				uni.redirectTo({ url: '/pages/discover/discover' })
			}
		},
		handleAvatarError(event) {
			if (event && event.target) event.target.src = this.defaultAvatar
		},
		async loadBots() {
			this.loadFailed = false
			try {
				const res = await getBots()
				if (res && res.code === 200) {
					this.bots = res.data || []
				} else {
					this.loadFailed = true
				}
			} catch (e) {
				console.error('加载机器人失败:', e)
				this.loadFailed = true
			} finally {
				this.loading = false
			}
		},
		openCreate() {
			this.editingBotId = null
			this.form = { name: '', description: '', prompt: '', avatar: '', avatarPreview: '' }
			this.createOpen = true
		},
		closeCreate() {
			if (this.creating || this.uploadingAvatar) return
			this.createOpen = false
			this.releaseCropSrc()
			this.form.avatarPreview = ''
		},
		chooseAvatar() {
			if (this.uploadingAvatar) return
			uni.chooseImage({
				count: 1,
				sizeType: ['compressed'],
				sourceType: ['album', 'camera'],
				success: async (res) => {
					const info = (res.tempFiles && res.tempFiles[0]) || {}
					if (info.size && info.size > 8 * 1024 * 1024) {
						uni.showToast({ title: '图片不能超过 8MB', icon: 'none' })
						return
					}
					// #ifdef H5
					this.cropSrc = res.tempFilePaths[0]
					this.cropOpen = true
					// #endif
					// #ifndef H5
					const fileArg = res.tempFilePaths[0]
					this.form.avatarPreview = fileArg
					this.uploadSelectedAvatar(fileArg)
					// #endif
				}
			})
		},
		closeAvatarCrop() {
			this.cropOpen = false
			this.releaseCropSrc()
		},
		releaseCropSrc() {
			// #ifdef H5
			if (this.cropSrc && this.cropSrc.indexOf('blob:') === 0) {
				try {
					URL.revokeObjectURL(this.cropSrc)
				} catch (e) {
					// ignore
				}
			}
			// #endif
			this.cropSrc = ''
		},
		handleCropError(message) {
			this.closeAvatarCrop()
			uni.showToast({ title: message || '裁剪失败，请重试', icon: 'none' })
		},
		confirmAvatarCrop(blob) {
			this.cropOpen = false
			this.releaseCropSrc()
			this.uploadSelectedAvatar(blob)
		},
		async uploadSelectedAvatar(fileArg) {
			this.uploadingAvatar = true
			try {
				const uploadRes = await uploadFile(fileArg)
				const url = uploadRes && uploadRes.data
				if (!url) throw new Error('上传失败')
				this.form.avatar = url
				this.form.avatarPreview = url
				uni.showToast({ title: '头像已上传', icon: 'success' })
			} catch (e) {
				this.form.avatarPreview = ''
				uni.showToast({ title: e.message || '头像上传失败', icon: 'none' })
			} finally {
				this.uploadingAvatar = false
			}
		},
		async openEdit(bot) {
			uni.showLoading({ title: '读取配置…', mask: true })
			try {
				const res = await getBotDetail(bot.id)
				const detail = (res && res.data) || bot
				this.form = {
					name: detail.name || '',
					description: detail.description || '',
					prompt: detail.prompt || detail.systemPrompt || detail.persona || '',
					avatar: detail.avatar || '',
					avatarPreview: detail.avatar || ''
				}
				this.editingBotId = bot.id
				this.detailBot = null
				this.createOpen = true
			} catch (e) {
				console.error('读取机器人配置失败:', e)
				uni.showToast({ title: e.message || '读取配置失败', icon: 'none' })
			} finally {
				uni.hideLoading()
			}
		},
		async submitCreate() {
			if (this.creating) return
			if (!this.form.name.trim()) {
				uni.showToast({ title: '给机器人起个名字吧', icon: 'none' })
				return
			}
			if (!this.form.avatar) {
				uni.showToast({ title: '请先上传机器人头像', icon: 'none' })
				return
			}
			if (!this.form.prompt.trim()) {
				uni.showToast({ title: '请填写角色设定', icon: 'none' })
				return
			}
			this.creating = true
			const payload = {
				name: this.form.name.trim(),
				description: this.form.description.trim(),
				prompt: this.form.prompt.trim(),
				avatar: this.form.avatar
			}
			try {
				const res = this.editingBotId
					? await updateBot(this.editingBotId, payload)
					: await createBot(payload)
				if (res && res.code === 200) {
					this.createOpen = false
					uni.showToast({ title: this.editingBotId ? '保存成功' : '创建成功', icon: 'success' })
					this.editingBotId = null
					this.loading = true
					await this.loadBots()
				} else {
					throw new Error((res && res.msg) || '保存失败')
				}
			} catch (e) {
				console.error('保存机器人失败:', e)
				uni.showToast({ title: e.message || '保存失败，请重试', icon: 'none' })
			} finally {
				this.creating = false
			}
		},
		openDetail(bot) {
			this.detailBot = bot
		},
		closeDetail() {
			this.detailBot = null
		},
		openChat(bot) {
			const currentBot = {
				id: bot.id,
				name: bot.name || '机器人',
				avatar: bot.avatar || ''
			}
			uni.setStorageSync('currentBot', currentBot)
			uni.navigateTo({
				url: '/pages/bots/bot-chat?botId=' + encodeURIComponent(bot.id)
					+ '&name=' + encodeURIComponent(currentBot.name)
					+ '&avatar=' + encodeURIComponent(currentBot.avatar)
			})
		},
		removeBot(bot) {
			uni.showModal({
				title: '删除机器人',
				content: '删除后这个角色和它的设定将不可恢复。',
				confirmText: '删除',
				confirmColor: '#d92d20',
				success: async (result) => {
					if (!result.confirm) return
					try {
						const res = await deleteBot(bot.id)
						if (res && res.code === 200) {
							this.bots = this.bots.filter(item => item.id !== bot.id)
							if (this.detailBot && this.detailBot.id === bot.id) this.detailBot = null
							uni.showToast({ title: '已删除', icon: 'success' })
						}
					} catch (e) {
						console.error('删除机器人失败:', e)
					}
				}
			})
		}
	}
}
</script>

<style lang="scss" scoped>
.bots-page {
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
	width: 68rpx;
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

.page-title {
	flex: 1;
	text-align: center;
	font-size: 34rpx;
	font-weight: 700;
	color: #1c1c1e;
}

.top-action {
	width: 96rpx;
	height: 68rpx;
	display: flex;
	align-items: center;
	justify-content: flex-end;
}

.top-action-text {
	font-size: 27rpx;
	font-weight: 600;
	color: #111111;
}

.main-content {
	flex: 1;
	min-height: 0;
	overflow: hidden;
}

.main-content ::v-deep .uni-scroll-view {
	height: 100% !important;
}

.content-wrapper {
	padding: 24rpx 24rpx 60rpx;
}

.intro-card {
	background: rgba(255, 255, 255, 0.86);
	border: 1rpx solid rgba(60, 60, 67, 0.1);
	border-radius: 28rpx;
	padding: 32rpx 30rpx;
	margin-bottom: 24rpx;
	box-shadow: 0 12rpx 28rpx rgba(0, 0, 0, 0.04);
}

.intro-title {
	display: block;
	font-size: 32rpx;
	font-weight: 700;
	color: #1c1c1e;
}

.intro-desc {
	display: block;
	font-size: 24rpx;
	line-height: 1.65;
	color: rgba(60, 60, 67, 0.62);
	margin-top: 14rpx;
}

.state-card {
	background: #ffffff;
	border: 1rpx solid rgba(60, 60, 67, 0.12);
	border-radius: 20rpx;
	padding: 64rpx 32rpx;
	display: flex;
	flex-direction: column;
	align-items: center;
}

.state-title {
	font-size: 28rpx;
	font-weight: 600;
	color: #1c1c1e;
}

.state-desc {
	font-size: 24rpx;
	color: rgba(60, 60, 67, 0.6);
	margin-top: 14rpx;
	text-align: center;
	line-height: 1.5;
}

.empty-create {
	margin-top: 28rpx;
	height: 76rpx;
	padding: 0 40rpx;
	border-radius: 16rpx;
	background: #111111;
	display: flex;
	align-items: center;
	justify-content: center;
}

.empty-create-text {
	font-size: 26rpx;
	font-weight: 600;
	color: #ffffff;
}

.bot-card {
	display: flex;
	align-items: center;
	background: #ffffff;
	border: 1rpx solid rgba(60, 60, 67, 0.12);
	border-radius: 20rpx;
	padding: 26rpx 28rpx;
	margin-bottom: 20rpx;
}

.bot-avatar {
	width: 84rpx;
	height: 84rpx;
	border-radius: 18rpx;
	background: #f2f2f7;
	flex-shrink: 0;
}

.bot-info {
	flex: 1;
	margin-left: 22rpx;
	min-width: 0;
}

.bot-name {
	display: block;
	font-size: 29rpx;
	font-weight: 600;
	color: #1c1c1e;
}

.bot-desc {
	display: block;
	font-size: 23rpx;
	color: rgba(60, 60, 67, 0.6);
	margin-top: 6rpx;
	line-height: 1.45;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
}

.bot-arrow {
	width: 26rpx;
	height: 26rpx;
	opacity: 0.35;
	flex-shrink: 0;
	margin-left: 12rpx;
}

.bot-actions {
	padding: 12rpx 0;
	display: flex;
	justify-content: center;
}

.bot-actions-tip {
	font-size: 22rpx;
	color: rgba(60, 60, 67, 0.4);
}

.sheet-mask {
	position: fixed;
	left: 0;
	right: 0;
	top: 0;
	bottom: 0;
	background: rgba(0, 0, 0, 0.35);
	z-index: 800;
	display: flex;
	align-items: flex-end;
}

.sheet-panel {
	width: 100%;
	max-height: 84vh;
	overflow-y: auto;
	-webkit-overflow-scrolling: touch;
	background: #ffffff;
	border-radius: 26rpx 26rpx 0 0;
	padding: 16rpx 28rpx calc(28rpx + env(safe-area-inset-bottom));
	box-sizing: border-box;
	max-height: 88vh;
	overflow-y: auto;
}

.sheet-handle {
	width: 72rpx;
	height: 8rpx;
	border-radius: 4rpx;
	background: rgba(60, 60, 67, 0.2);
	margin: 0 auto 16rpx;
}

.sheet-head {
	display: flex;
	align-items: center;
	height: 76rpx;
	margin-bottom: 8rpx;
}

.sheet-cancel {
	width: 110rpx;
	font-size: 26rpx;
	color: rgba(60, 60, 67, 0.7);
}

.sheet-title {
	flex: 1;
	text-align: center;
	font-size: 30rpx;
	font-weight: 600;
	color: #1c1c1e;
}

.sheet-submit {
	width: 110rpx;
	text-align: right;
	font-size: 26rpx;
	font-weight: 600;
	color: #111111;
}

.sheet-submit-disabled {
	opacity: 0.4;
}

.form-row {
	display: flex;
	align-items: center;
	padding: 22rpx 0;
	border-bottom: 1rpx solid rgba(60, 60, 67, 0.1);
}

.form-label {
	width: 120rpx;
	font-size: 26rpx;
	color: rgba(60, 60, 67, 0.7);
	flex-shrink: 0;
}

.form-input {
	flex: 1;
	font-size: 27rpx;
	color: #1c1c1e;
}

.form-block {
	padding: 22rpx 0 0;
}

.form-textarea {
	width: 100%;
	min-height: 190rpx;
	background: #f7f7fa;
	border-radius: 12rpx;
	padding: 18rpx;
	box-sizing: border-box;
	font-size: 26rpx;
	color: #1c1c1e;
	line-height: 1.6;
	margin-top: 14rpx;
}

.form-count {
	display: block;
	text-align: right;
	font-size: 20rpx;
	color: rgba(60, 60, 67, 0.35);
	margin-top: 8rpx;
}

.form-tip {
	margin-top: 20rpx;
	padding-top: 16rpx;
	border-top: 1rpx solid rgba(60, 60, 67, 0.1);
}

.form-tip-text {
	font-size: 21rpx;
	color: rgba(60, 60, 67, 0.4);
}

.avatar-row {
	display: flex;
	align-items: center;
	gap: 22rpx;
	padding: 22rpx 0;
	border-bottom: 1rpx solid rgba(60, 60, 67, 0.1);
}

.avatar-picker {
	position: relative;
	width: 120rpx;
	height: 120rpx;
	border-radius: 18rpx;
	overflow: hidden;
	background: #f2f2f7;
	flex-shrink: 0;
}

.avatar-picker-img {
	width: 100%;
	height: 100%;
}

.avatar-picker-badge {
	position: absolute;
	right: 4rpx;
	bottom: 4rpx;
	width: 38rpx;
	height: 38rpx;
	border-radius: 19rpx;
	background: rgba(17, 17, 17, 0.82);
	color: #ffffff;
	font-size: 26rpx;
	line-height: 38rpx;
	text-align: center;
}

.avatar-row-info {
	display: flex;
	flex-direction: column;
	gap: 8rpx;
}

.avatar-row-title {
	font-size: 26rpx;
	font-weight: 600;
	color: #1c1c1e;
}

.avatar-row-desc {
	font-size: 21rpx;
	color: rgba(60, 60, 67, 0.45);
}

.detail-panel {
	width: 100%;
	background: #ffffff;
	border-radius: 30rpx 30rpx 0 0;
	padding: 16rpx 36rpx calc(30rpx + env(safe-area-inset-bottom));
	box-sizing: border-box;
	display: flex;
	flex-direction: column;
	align-items: center;
}

.detail-avatar {
	width: 140rpx;
	height: 140rpx;
	border-radius: 22rpx;
	background: #f2f2f7;
	margin-top: 8rpx;
}

.detail-name {
	margin-top: 18rpx;
	font-size: 34rpx;
	font-weight: 700;
	color: #1c1c1e;
	letter-spacing: -0.01em;
}

.detail-desc {
	margin-top: 12rpx;
	font-size: 25rpx;
	line-height: 1.6;
	color: rgba(60, 60, 67, 0.62);
	text-align: center;
}

.detail-enter {
	width: 100%;
	height: 88rpx;
	margin-top: 32rpx;
	border-radius: 20rpx;
	background: #111111;
	display: flex;
	align-items: center;
	justify-content: center;
}

.detail-enter-text {
	font-size: 28rpx;
	font-weight: 600;
	color: #ffffff;
}

.detail-edit {
	width: 100%;
	height: 82rpx;
	margin-top: 16rpx;
	border-radius: 20rpx;
	border: 1rpx solid rgba(60, 60, 67, 0.16);
	background: #ffffff;
	font-size: 26rpx;
	font-weight: 600;
	color: #1c1c1e;
	line-height: 82rpx;
	text-align: center;
	box-sizing: border-box;
}

.detail-delete {
	margin-top: 22rpx;
	font-size: 25rpx;
	color: #ff3b30;
}

.detail-tip {
	margin-top: 18rpx;
	font-size: 20rpx;
	color: rgba(60, 60, 67, 0.35);
	text-align: center;
}
</style>
