<template>
	<view class="group-join-page">
		<view class="header safe-top-bar">
			<view class="header-back" hover-class="header-back-hover" @click="handleBack">
				<image class="back-icon" src="/static/icons/arrow/left.svg" mode="aspectFit"></image>
			</view>
			<text class="header-title">加入群聊</text>
		</view>

		<scroll-view class="main-content" scroll-y>
			<view class="content-wrapper">
				<view v-if="loading" class="state-card">
					<text class="state-text">正在读取群资料…</text>
				</view>

				<view v-else-if="errorText" class="state-card">
					<text class="state-title">无法打开该群二维码</text>
					<text class="state-text">{{ errorText }}</text>
					<button class="primary-btn" @click="loadGroup">重新加载</button>
				</view>

				<view v-else-if="group" class="group-card">
					<view class="group-head">
						<image v-if="group.groupAvatar" class="group-avatar" :src="group.groupAvatar" mode="aspectFill"></image>
						<view v-else class="group-avatar group-avatar-fallback">
							<text>{{ (group.groupName || '群').slice(0, 1) }}</text>
						</view>
						<view class="group-meta">
							<text class="group-name">{{ group.groupName }}</text>
							<text class="group-no">群号 {{ group.groupId }} · {{ group.currentMember || 0 }} 人</text>
						</view>
					</view>

					<text v-if="group.groupDescription" class="group-desc">{{ group.groupDescription }}</text>

					<view class="divider"></view>

					<view v-if="group.joined" class="joined-block">
						<text class="joined-title">你已在这个群里</text>
						<button class="primary-btn" @click="openGroup">进入群聊</button>
					</view>

					<view v-else class="join-block">
						<text class="join-hint">向群主或管理员发送入群申请，通过后即可加入。</text>
						<textarea
							v-model="reason"
							class="reason-input"
							maxlength="200"
							placeholder="填写申请理由（选填）"
						/>
						<button class="primary-btn" :disabled="applying" @click="applyJoin">
							{{ applying ? '提交中…' : (applied ? '已提交申请' : '申请加入') }}
						</button>
					</view>
				</view>
			</view>
		</scroll-view>
	</view>
</template>

<script>
import { searchGroups, requestJoinGroup } from '@/api/group.js'

export default {
	data() {
		return {
			groupId: '',
			group: null,
			loading: true,
			errorText: '',
			reason: '',
			applying: false,
			applied: false
		}
	},
	onLoad(options = {}) {
		this.groupId = decodeURIComponent(options.id || options.groupId || '')
		this.loadGroup()
	},
	methods: {
		handleBack() {
			const pages = getCurrentPages()
			if (pages.length > 1) {
				uni.navigateBack()
			} else {
				uni.reLaunch({ url: '/pages/chat/chat-list' })
			}
		},
		async loadGroup() {
			if (!this.groupId) {
				this.loading = false
				this.errorText = '二维码缺少群号，请让群主重新分享'
				return
			}
			this.loading = true
			this.errorText = ''
			try {
				const res = await searchGroups(this.groupId)
				const list = (res && res.data) || []
				const found = list.find(item => String(item.groupId) === String(this.groupId)) || list[0]
				if (!found) {
					this.errorText = '群聊不存在或已解散'
					this.group = null
				} else {
					this.group = found
				}
			} catch (e) {
				this.errorText = (e && (e.msg || e.message)) || '网络异常，请稍后重试'
			} finally {
				this.loading = false
			}
		},
		openGroup() {
			if (!this.group) return
			uni.navigateTo({
				url: `/pages/chat/chat-detail?id=${this.group.groupId}&name=${encodeURIComponent(this.group.groupName || '')}&chatType=2`
			})
		},
		async applyJoin() {
			if (!this.group || this.applying || this.applied) return
			this.applying = true
			try {
				await requestJoinGroup({ groupId: this.group.groupId, reason: this.reason.trim() || '通过群二维码申请加入' })
				this.applied = true
				uni.showToast({ title: '申请已提交，等待审批', icon: 'none' })
			} catch (e) {
				uni.showToast({ title: (e && (e.msg || e.message)) || '提交失败，请重试', icon: 'none' })
			} finally {
				this.applying = false
			}
		}
	}
}
</script>

<style scoped>
.group-join-page {
	height: 100vh;
	background: #f5f6f8;
}

.header {
	position: relative;
	display: flex;
	align-items: center;
	justify-content: center;
	height: 88rpx;
	background: #ffffff;
	border-bottom: 1rpx solid #ececec;
}

.header-back {
	position: absolute;
	left: 12rpx;
	width: 72rpx;
	height: 72rpx;
	display: flex;
	align-items: center;
	justify-content: center;
}

.back-icon {
	width: 36rpx;
	height: 36rpx;
}

.header-title {
	font-size: 32rpx;
	font-weight: 600;
	color: #111111;
}

.main-content {
	height: calc(100vh - 88rpx);
}

.content-wrapper {
	padding: 32rpx 24rpx;
}

.state-card,
.group-card {
	background: #ffffff;
	border-radius: 24rpx;
	padding: 40rpx 32rpx;
	box-shadow: 0 12rpx 36rpx rgba(15, 23, 42, 0.06);
}

.state-card {
	display: flex;
	flex-direction: column;
	align-items: center;
	gap: 20rpx;
}

.state-title {
	font-size: 30rpx;
	font-weight: 600;
	color: #111111;
}

.state-text {
	font-size: 26rpx;
	color: #777777;
	text-align: center;
}

.group-head {
	display: flex;
	align-items: center;
	gap: 24rpx;
}

.group-avatar {
	width: 112rpx;
	height: 112rpx;
	border-radius: 24rpx;
	background: #f0f1f3;
}

.group-avatar-fallback {
	display: flex;
	align-items: center;
	justify-content: center;
	font-size: 40rpx;
	font-weight: 600;
	color: #111111;
}

.group-meta {
	flex: 1;
	display: flex;
	flex-direction: column;
	gap: 10rpx;
}

.group-name {
	font-size: 34rpx;
	font-weight: 600;
	color: #111111;
}

.group-no {
	font-size: 24rpx;
	color: #888888;
}

.group-desc {
	display: block;
	margin-top: 24rpx;
	font-size: 26rpx;
	line-height: 1.7;
	color: #555555;
}

.divider {
	height: 1rpx;
	margin: 32rpx 0;
	background: #eeeeee;
}

.join-block,
.joined-block {
	display: flex;
	flex-direction: column;
	gap: 20rpx;
}

.joined-title {
	font-size: 28rpx;
	font-weight: 600;
	color: #111111;
}

.join-hint {
	font-size: 25rpx;
	line-height: 1.6;
	color: #666666;
}

.reason-input {
	width: 100%;
	min-height: 140rpx;
	padding: 20rpx;
	box-sizing: border-box;
	font-size: 26rpx;
	color: #222222;
	background: #f6f7f9;
	border-radius: 16rpx;
}

.primary-btn {
	width: 100%;
	height: 84rpx;
	line-height: 84rpx;
	border-radius: 16rpx;
	background: #111111;
	color: #ffffff;
	font-size: 28rpx;
	font-weight: 600;
}

.primary-btn[disabled] {
	opacity: 0.65;
}
</style>
