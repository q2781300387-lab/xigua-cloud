<template>
	<view class="todo-page">
		<view class="top-bar safe-top-bar">
			<view class="top-bar-content">
				<view class="back-btn" @click="goBack">
					<image class="back-icon" src="/static/icons/arrow/left.svg" mode="aspectFit"></image>
				</view>
				<text class="page-title">待办事项</text>
				<view class="top-action" @click="openAdd">
					<text class="top-action-text">新建</text>
				</view>
			</view>
		</view>

		<scroll-view class="main-content" scroll-y>
			<view class="content-wrapper">
				<view class="summary-card">
					<text class="summary-title">今天要做什么</text>
					<text class="summary-desc">{{ pendingList.length ? `还有 ${pendingList.length} 件待办没有完成` : '今天的待办都完成了' }}</text>
					<text class="summary-tip">待办保存在本机，不上传服务器</text>
				</view>

				<view v-if="loadFailed" class="state-card" @click="loadTodos">
					<text class="state-title">读取待办失败</text>
					<text class="state-desc">点击重试</text>
				</view>

				<view v-else>
					<view class="section-head">
						<text class="section-title">未完成</text>
						<text class="section-count">{{ pendingList.length }}</text>
					</view>

					<view v-if="pendingList.length === 0" class="empty-card">
						<text class="empty-title">暂无待办</text>
						<text class="empty-desc">点右上角“新建”，记下一件要做的事</text>
					</view>

					<view v-for="item in pendingList" :key="item.id" class="todo-card">
						<view class="check-circle" :class="{ 'check-overdue': isOverdue(item) }" @click="toggleTodo(item)"></view>
						<view class="todo-body" @click="openEdit(item)">
							<text class="todo-title">{{ item.title }}</text>
							<text v-if="item.note" class="todo-note">{{ item.note }}</text>
							<view v-if="item.remindAt" class="todo-time-row" :class="{ 'time-overdue': isOverdue(item) }">
								<text class="todo-time">{{ formatRemindAt(item.remindAt) }}</text>
								<text v-if="isOverdue(item)" class="todo-overdue">已到期</text>
							</view>
						</view>
						<text class="todo-delete" @click="removeTodo(item)">×</text>
					</view>

					<view v-if="doneList.length" class="section-head section-head-done">
						<text class="section-title">已完成</text>
						<text class="section-count">{{ doneList.length }}</text>
					</view>

					<view v-for="item in doneList" :key="item.id" class="todo-card todo-card-done">
						<view class="check-circle check-done" @click="toggleTodo(item)">
							<text class="check-mark">✓</text>
						</view>
						<view class="todo-body">
							<text class="todo-title todo-title-done">{{ item.title }}</text>
							<text v-if="item.remindAt" class="todo-time">{{ formatRemindAt(item.remindAt) }}</text>
						</view>
						<text class="todo-delete" @click="removeTodo(item)">×</text>
					</view>
				</view>
			</view>
		</scroll-view>

		<view v-if="sheetOpen" class="sheet-mask" @click="closeSheet">
			<view class="sheet-panel" @click.stop>
				<view class="sheet-handle"></view>
				<view class="sheet-head">
					<text class="sheet-cancel" @click="closeSheet">取消</text>
					<text class="sheet-title">{{ editingId ? '编辑待办' : '新建待办' }}</text>
					<text class="sheet-submit" @click="saveTodo">保存</text>
				</view>

				<view class="form-row">
					<text class="form-label">标题</text>
					<input class="form-input" v-model="form.title" placeholder="要做什么？" maxlength="50" />
				</view>

				<view class="form-block">
					<text class="form-label">备注</text>
					<textarea
						class="form-textarea"
						v-model="form.note"
						placeholder="补充说明（可不填）"
						maxlength="200"
						:adjust-position="true"
					></textarea>
				</view>

				<view class="form-row">
					<text class="form-label">设置提醒</text>
					<switch class="form-switch" :checked="form.remindEnabled" color="#111111" @change="onRemindChange" />
				</view>

				<view v-if="form.remindEnabled" class="remind-row">
					<picker mode="date" :value="form.remindDate" @change="onDateChange">
						<view class="picker-box">
							<text class="picker-value">{{ form.remindDate || '选择日期' }}</text>
						</view>
					</picker>
					<picker mode="time" :value="form.remindTime" @change="onTimeChange">
						<view class="picker-box picker-box-time">
							<text class="picker-value">{{ form.remindTime || '选择时间' }}</text>
						</view>
					</picker>
				</view>
			</view>
		</view>
	</view>
</template>

<script>
const STORAGE_KEY = 'xigua_todos'

export default {
	data() {
		return {
			todos: [],
			loadFailed: false,
			sheetOpen: false,
			editingId: null,
			form: {
				title: '',
				note: '',
				remindEnabled: false,
				remindDate: '',
				remindTime: ''
			}
		}
	},
	computed: {
		pendingList() {
			return this.todos
				.filter(item => !item.done)
				.sort((a, b) => (a.remindAt || 0) - (b.remindAt || 0) || b.createdAt - a.createdAt)
		},
		doneList() {
			return this.todos.filter(item => item.done)
		}
	},
	onLoad() {
		this.loadTodos()
	},
	onShow() {
		this.loadTodos()
		this.checkReminders()
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
		todayString() {
			const date = new Date()
			const pad = n => String(n).padStart(2, '0')
			return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
		},
		nowTimeString() {
			const date = new Date()
			const pad = n => String(n).padStart(2, '0')
			return `${pad(date.getHours())}:${pad(date.getMinutes())}`
		},
		loadTodos() {
			this.loadFailed = false
			try {
				const raw = uni.getStorageSync(STORAGE_KEY)
				const list = raw ? JSON.parse(raw) : []
				this.todos = Array.isArray(list) ? list : []
			} catch (e) {
				console.error('读取待办失败:', e)
				this.loadFailed = true
			}
		},
		saveTodos() {
			try {
				uni.setStorageSync(STORAGE_KEY, JSON.stringify(this.todos))
			} catch (e) {
				console.error('保存待办失败:', e)
				uni.showToast({ title: '保存失败', icon: 'none' })
			}
		},
		openAdd() {
			this.editingId = null
			this.form = {
				title: '',
				note: '',
				remindEnabled: false,
				remindDate: this.todayString(),
				remindTime: this.nowTimeString()
			}
			this.sheetOpen = true
		},
		openEdit(item) {
			this.editingId = item.id
			const date = item.remindAt ? new Date(item.remindAt) : null
			const pad = n => String(n).padStart(2, '0')
			this.form = {
				title: item.title || '',
				note: item.note || '',
				remindEnabled: !!item.remindAt,
				remindDate: date ? `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}` : this.todayString(),
				remindTime: date ? `${pad(date.getHours())}:${pad(date.getMinutes())}` : this.nowTimeString()
			}
			this.sheetOpen = true
		},
		closeSheet() {
			this.sheetOpen = false
		},
		onRemindChange(event) {
			this.form.remindEnabled = !!event.detail.value
		},
		onDateChange(event) {
			this.form.remindDate = event.detail.value
		},
		onTimeChange(event) {
			this.form.remindTime = event.detail.value
		},
		buildRemindAt() {
			if (!this.form.remindEnabled || !this.form.remindDate || !this.form.remindTime) return 0
			const time = new Date(`${this.form.remindDate.replace(/-/g, '/')} ${this.form.remindTime}:00`).getTime()
			return isNaN(time) ? 0 : time
		},
		saveTodo() {
			const title = this.form.title.trim()
			if (!title) {
				uni.showToast({ title: '先写一下要做什么', icon: 'none' })
				return
			}
			const remindAt = this.buildRemindAt()
			if (this.editingId) {
				const index = this.todos.findIndex(item => item.id === this.editingId)
				if (index !== -1) {
					this.todos[index] = Object.assign({}, this.todos[index], {
						title,
						note: this.form.note.trim(),
						remindAt,
						reminded: false
					})
				}
			} else {
				this.todos.unshift({
					id: Date.now() + Math.random().toString(36).slice(2, 8),
					title,
					note: this.form.note.trim(),
					remindAt,
					done: false,
					reminded: false,
					createdAt: Date.now()
				})
			}
			this.saveTodos()
			this.sheetOpen = false
			uni.showToast({ title: '已保存', icon: 'success' })
		},
		toggleTodo(item) {
			item.done = !item.done
			if (item.done) item.reminded = true
			this.saveTodos()
		},
		removeTodo(item) {
			uni.showModal({
				title: '删除待办',
				content: `确定删除“${item.title}”吗？`,
				confirmText: '删除',
				confirmColor: '#d92d20',
				success: (result) => {
					if (!result.confirm) return
					this.todos = this.todos.filter(todo => todo.id !== item.id)
					this.saveTodos()
					uni.showToast({ title: '已删除', icon: 'success' })
				}
			})
		},
		isOverdue(item) {
			return !item.done && item.remindAt > 0 && item.remindAt <= Date.now()
		},
		formatRemindAt(timestamp) {
			if (!timestamp) return ''
			const date = new Date(timestamp)
			const pad = n => String(n).padStart(2, '0')
			return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
		},
		checkReminders() {
			const now = Date.now()
			const due = []
			let changed = false
			this.todos.forEach(item => {
				if (!item.done && !item.reminded && item.remindAt > 0 && item.remindAt <= now) {
					due.push(item)
					item.reminded = true
					changed = true
				}
			})
			if (changed) this.saveTodos()
			if (due.length) {
				const first = due[0]
				uni.showToast({
					title: due.length > 1 ? `${due.length} 件待办已到期` : `待办提醒：${first.title}`,
					icon: 'none',
					duration: 2600
				})
			}
		}
	}
}
</script>

<style lang="scss" scoped>
.todo-page {
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

.summary-card {
	background: transparent;
	border-radius: 0;
	padding: 8rpx 4rpx 18rpx;
	margin-bottom: 8rpx;
}

.summary-title {
	display: block;
	font-size: 38rpx;
	font-weight: 700;
	color: #1c1c1e;
	letter-spacing: -0.02em;
}

.summary-desc {
	display: block;
	font-size: 24rpx;
	color: rgba(60, 60, 67, 0.62);
	margin-top: 8rpx;
}

.summary-tip {
	display: block;
	font-size: 21rpx;
	color: rgba(60, 60, 67, 0.4);
	margin-top: 10rpx;
}

.state-card,
.empty-card {
	background: #ffffff;
	border: 1rpx solid rgba(60, 60, 67, 0.12);
	border-radius: 20rpx;
	padding: 56rpx 32rpx;
	display: flex;
	flex-direction: column;
	align-items: center;
}

.state-title,
.empty-title {
	font-size: 28rpx;
	font-weight: 600;
	color: #1c1c1e;
}

.state-desc,
.empty-desc {
	font-size: 24rpx;
	color: rgba(60, 60, 67, 0.6);
	margin-top: 12rpx;
	text-align: center;
	line-height: 1.5;
}

.section-head {
	display: flex;
	align-items: center;
	padding: 16rpx 4rpx 14rpx;
}

.section-head-done {
	margin-top: 12rpx;
}

.section-title {
	font-size: 23rpx;
	font-weight: 600;
	color: rgba(60, 60, 67, 0.55);
	letter-spacing: 0.04em;
}

.section-count {
	font-size: 22rpx;
	color: rgba(60, 60, 67, 0.35);
	margin-left: 10rpx;
}

.todo-card {
	display: flex;
	align-items: center;
	background: #ffffff;
	border: 1rpx solid rgba(60, 60, 67, 0.12);
	border-radius: 18rpx;
	padding: 24rpx 22rpx;
	margin-bottom: 14rpx;
}

.todo-card-done {
	opacity: 0.68;
}

.check-circle {
	width: 42rpx;
	height: 42rpx;
	border-radius: 50%;
	border: 2rpx solid rgba(60, 60, 67, 0.25);
	flex-shrink: 0;
	margin-right: 20rpx;
}

.check-overdue {
	border-color: #d92d20;
}

.check-done {
	background: #111111;
	border-color: #111111;
	display: flex;
	align-items: center;
	justify-content: center;
}

.check-mark {
	font-size: 24rpx;
	color: #ffffff;
	line-height: 1;
}

.todo-body {
	flex: 1;
	min-width: 0;
}

.todo-title {
	display: block;
	font-size: 28rpx;
	font-weight: 500;
	color: #1c1c1e;
	line-height: 1.4;
}

.todo-title-done {
	text-decoration: line-through;
	color: rgba(60, 60, 67, 0.6);
}

.todo-note {
	display: block;
	font-size: 23rpx;
	color: rgba(60, 60, 67, 0.6);
	line-height: 1.45;
	margin-top: 6rpx;
}

.todo-time-row {
	display: flex;
	align-items: center;
	margin-top: 8rpx;
}

.todo-time {
	font-size: 23rpx;
	color: rgba(60, 60, 67, 0.55);
	margin-top: 6rpx;
}

.time-overdue .todo-time {
	color: #d92d20;
}

.todo-overdue {
	font-size: 21rpx;
	color: #d92d20;
	margin-left: 10rpx;
	margin-top: 6rpx;
}

.todo-delete {
	width: 52rpx;
	height: 52rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	font-size: 26rpx;
	color: rgba(60, 60, 67, 0.35);
	flex-shrink: 0;
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

.form-row {
	display: flex;
	align-items: center;
	padding: 24rpx 0;
	border-bottom: 1rpx solid rgba(60, 60, 67, 0.1);
}

.form-label {
	width: 160rpx;
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
	padding: 24rpx 0;
	border-bottom: 1rpx solid rgba(60, 60, 67, 0.1);
}

.form-textarea {
	width: 100%;
	min-height: 130rpx;
	background: #f7f7fa;
	border-radius: 12rpx;
	padding: 18rpx;
	box-sizing: border-box;
	font-size: 26rpx;
	color: #1c1c1e;
	line-height: 1.6;
	margin-top: 14rpx;
}

.form-switch {
	transform: scale(0.82);
	transform-origin: left center;
}

.remind-row {
	display: flex;
	padding: 20rpx 0 6rpx;
}

.picker-box {
	height: 72rpx;
	min-width: 210rpx;
	padding: 0 20rpx;
	background: #f7f7fa;
	border-radius: 12rpx;
	display: flex;
	align-items: center;
}

.picker-box-time {
	margin-left: 16rpx;
	min-width: 160rpx;
}

.picker-value {
	font-size: 26rpx;
	color: #1c1c1e;
}
</style>
