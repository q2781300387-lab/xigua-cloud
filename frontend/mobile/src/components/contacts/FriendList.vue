<template>
	<view class="friend-list-container">
		<!-- 加载中 -->
		<view v-if="loading" class="empty-tip">
			<text class="empty-text">加载中...</text>
		</view>

		<!-- 空状态 -->
		<view v-else-if="contactGroups.length === 0" class="empty-state-card">
			<view class="empty-icon-wrapper">
				<image class="empty-icon" src="/static/icons/user/group-outline.svg" mode="aspectFit"></image>
			</view>
			<text class="empty-title">还没有添加好友？</text>
			<text class="empty-subtitle">添加好友，开始愉快的聊天吧～</text>
			<view class="action-btn" @click="handleAddFriend">
				<text class="action-btn-text">添加好友</text>
			</view>
		</view>

		<!-- 循环渲染分组 -->
		<view v-else>
			<view v-for="(group, index) in contactGroups" :key="index" :id="'group-' + group.letter" class="contact-group">
				<view class="group-header">
					<text class="group-letter">{{ group.letter }}</text>
				</view>
				<view class="group-contacts">
					<view v-for="(contact, idx) in group.contacts" :key="idx" class="contact-card" @click="handleContactClick(contact)">
						<!-- 头像 -->
						<view class="contact-avatar-box">
							<text class="avatar-text">{{ (contact.name || '?').charAt(0).toUpperCase() }}</text>
							<image
								v-if="showAvatar(contact)"
								class="contact-avatar"
								:src="contact.avatar"
								mode="aspectFill"
								@error="handleAvatarError(contact)"
							></image>
						</view>
						<!-- 信息 -->
						<view class="contact-info">
							<text class="contact-name">{{ contact.name }}</text>
							<text v-if="contact.status === '在线'" class="contact-status">在线</text>
						</view>
						<image class="chevron" src="/static/icons/arrow/right.svg" mode="aspectFit"></image>
					</view>
				</view>
			</view>
		</view>
	</view>
</template>

<script>
export default {
	name: 'FriendList',
	props: {
		contactGroups: {
			type: Array,
			default: () => []
		},
		loading: {
			type: Boolean,
			default: false
		}
	},
	methods: {
		handleContactClick(contact) {
			this.$emit('contact-click', contact);
		},
		handleAddFriend() {
			this.$emit('add-friend');
		},
		handleAvatarError(contact) {
			contact.avatar = '';
		},
		showAvatar(contact) {
			const url = contact && contact.avatar;
			return !!url && !String(url).includes('default-avatar');
		}
	}
};
</script>

<style lang="scss" scoped>
.friend-list-container {
	width: 100%;
}

.contact-group {
	margin-bottom: 28rpx;
}

.group-header {
	padding: 0 24rpx;
	margin-bottom: 10rpx;
}

.group-letter {
	font-size: 22rpx;
	font-weight: 600;
	text-transform: uppercase;
	letter-spacing: 0.06em;
	color: rgba(60, 60, 67, 0.6);
}

.group-contacts {
	display: flex;
	flex-direction: column;
	gap: 0;
	background-color: #ffffff;
	border-radius: 18rpx;
	border: 1rpx solid rgba(60, 60, 67, 0.12);
	overflow: hidden;
}

.contact-card {
	display: flex;
	align-items: center;
	padding: 20rpx 24rpx;
	background-color: #ffffff;
	border-radius: 0;
	box-shadow: none;
	border: 0;
	border-bottom: 1rpx solid rgba(60, 60, 67, 0.12);
	transition: background-color 0.15s;
}

.contact-card:last-child {
	border-bottom: 0;
}

.contact-card:active {
	background-color: #f2f2f7;
	transform: none;
}

.contact-avatar-box {
	position: relative;
	width: 88rpx;
	height: 88rpx;
	border-radius: 12rpx;
	margin-right: 24rpx;
	background: #f2f2f7;
	display: flex;
	align-items: center;
	justify-content: center;
	flex-shrink: 0;
	overflow: hidden;
}

.contact-avatar {
	position: absolute;
	left: 0;
	top: 0;
	width: 88rpx;
	height: 88rpx;
	border-radius: 12rpx;
	display: block;
	overflow: hidden;
	background: #f2f2f7;
}

.avatar-text {
	font-size: 34rpx;
	font-weight: 600;
	color: #1c1c1e;
}

.contact-info {
	flex: 1;
	display: flex;
	flex-direction: column;
	gap: 2rpx;
	min-width: 0;
}

.contact-name {
	font-size: 30rpx;
	font-weight: 500;
	color: #1c1c1e;
	line-height: 1.35;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
}

.contact-status {
	font-size: 22rpx;
	color: #10b981;
	line-height: 1.4;
}

.chevron {
	width: 28rpx;
	height: 28rpx;
	opacity: 0.25;
	flex-shrink: 0;
	margin-left: 12rpx;
}

/* 空状态提示 */
.empty-tip {
	display: flex;
	flex-direction: column;
	align-items: center;
	padding: 120rpx 48rpx;
}

/* 空状态卡片 */
.empty-state-card {
	display: flex;
	flex-direction: column;
	align-items: center;
	padding: 80rpx 48rpx;
	background: #ffffff;
	border-radius: 24rpx;
	margin: 32rpx 0;
	box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.06);
}

.empty-icon-wrapper {
	width: 128rpx;
	height: 128rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	background: linear-gradient(135deg, #F2F2F7 0%, #f0f7ff 100%);
	border-radius: 50%;
	margin-bottom: 32rpx;
}

.empty-icon {
	width: 64rpx;
	height: 64rpx;
	color: #111111;
	stroke-width: 2.5;
}

.empty-title {
	font-family: -apple-system, BlinkMacSystemFont, 'PingFang SC', sans-serif;
	font-size: 30rpx;
	font-weight: 600;
	line-height: 1.4;
	color: #1a1a1a;
	text-align: center;
	margin-bottom: 12rpx;
}

.empty-subtitle {
	font-family: -apple-system, BlinkMacSystemFont, 'PingFang SC', sans-serif;
	font-size: 24rpx;
	font-weight: 400;
	line-height: 1.6;
	color: #999999;
	text-align: center;
	margin-bottom: 40rpx;
	padding: 0 24rpx;
}

/* 操作按钮 */
.action-btn {
	display: flex;
	align-items: center;
	justify-content: center;
	height: 80rpx;
	padding: 0 56rpx;
	background: #111111;
	border-radius: 40rpx;
	box-shadow: 0 4rpx 16rpx rgba(17, 17, 17, 0.3);
	transition: all 0.3s ease;
}

.action-btn:active {
	transform: scale(0.96);
	box-shadow: 0 2rpx 8rpx rgba(17, 17, 17, 0.4);
}

.action-btn-text {
	font-family: 'Plus Jakarta Sans', -apple-system, BlinkMacSystemFont, sans-serif;
	font-size: 28rpx;
	font-weight: 700;
	color: #ffffff;
	letter-spacing: 0.02em;
}
</style>
