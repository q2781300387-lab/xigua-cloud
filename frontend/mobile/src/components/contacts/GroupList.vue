<template>
	<view class="groups-container">
		<!-- 群聊卡片列表 -->
		<view v-if="groupList.length > 0" class="group-list">
			<view v-for="(group, index) in groupList" :key="index" class="group-card" @click="handleGroupClick(group)">
				<!-- 头像 -->
				<view class="group-avatar-box">
					<image class="group-icon" src="/static/icons/user/group-outline.svg" mode="aspectFit"></image>
					<image
						v-if="showAvatar(group)"
						class="group-avatar"
						:src="group.avatar"
						mode="aspectFill"
						@error="handleAvatarError(group)"
					></image>
				</view>
				<!-- 群信息 -->
				<view class="group-info">
					<text class="group-name">{{ group.name }}</text>
					<text class="group-members">{{ group.memberCount }}人</text>
				</view>
				<!-- 右箭头 -->
				<image class="arrow-icon" src="/static/icons/arrow/right.svg" mode="aspectFit"></image>
			</view>
		</view>

		<!-- 空状态提示（只在群列表为空时显示） -->
		<view v-else class="empty-state-card">
			<view class="empty-icon-wrapper">
				<image class="empty-icon" src="/static/icons/user/group-outline.svg" mode="aspectFit"></image>
			</view>
			<text class="empty-title">发现有新群正在聊天？</text>
			<text class="empty-subtitle">创建一个群组，或者打招呼加入一起聊聊吧～</text>
			<view class="create-group-btn" @click="handleCreateGroup">
				<text class="create-btn-text">创建新群</text>
			</view>
		</view>
	</view>
</template>

<script>
export default {
	name: 'GroupList',
	props: {
		groupList: {
			type: Array,
			default: () => []
		}
	},
	methods: {
		handleGroupClick(group) {
			this.$emit('group-click', group);
		},
		handleCreateGroup() {
			this.$emit('create-group');
		},
		handleAvatarError(group) {
			group.avatar = '';
		},
		showAvatar(group) {
			const url = group && group.avatar;
			return !!url && !String(url).includes('default-avatar');
		}
	}
};
</script>

<style lang="scss" scoped>
.groups-container {
	display: flex;
	flex-direction: column;
	gap: 0;
}

/* 群聊卡片列表 */
.group-list {
	display: flex;
	flex-direction: column;
	gap: 0;
	background-color: #ffffff;
	border-radius: 18rpx;
	border: 1rpx solid rgba(60, 60, 67, 0.12);
	overflow: hidden;
}

/* 群聊卡片 */
.group-card {
	display: flex;
	align-items: center;
	padding: 20rpx 24rpx;
	background-color: #ffffff;
	border-radius: 0;
	box-shadow: none;
	border-bottom: 1rpx solid rgba(60, 60, 67, 0.12);
	transition: background-color 0.15s ease;
}

.group-card:last-child {
	border-bottom: 0;
}

.group-card:active {
	background-color: #f2f2f7;
	transform: none;
	opacity: 1;
}

/* 群头像 */
.group-avatar-box {
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

.group-avatar {
	position: absolute;
	left: 0;
	top: 0;
	width: 88rpx;
	height: 88rpx;
	border-radius: 12rpx;
	flex-shrink: 0;
	object-fit: cover;
	overflow: hidden;
	background: #f2f2f7;
}

.group-avatar-placeholder {
	width: 88rpx;
	height: 88rpx;
	border-radius: 12rpx;
	margin-right: 24rpx;
	background: #f2f2f7;
	display: flex;
	align-items: center;
	justify-content: center;
	flex-shrink: 0;
}

.group-icon {
	width: 44rpx;
	height: 44rpx;
	color: #1c1c1e;
}

/* 群信息 */
.group-info {
	flex: 1;
	display: flex;
	flex-direction: column;
	gap: 8rpx;
	min-width: 0;
}

.group-name {
	font-family: -apple-system, BlinkMacSystemFont, 'PingFang SC', sans-serif;
	font-size: 30rpx;
	font-weight: 500;
	line-height: 1.35;
	color: #1c1c1e;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
}

.group-members {
	font-family: -apple-system, BlinkMacSystemFont, 'PingFang SC', sans-serif;
	font-size: 22rpx;
	font-weight: 400;
	line-height: 1.4;
	color: rgba(60, 60, 67, 0.6);
}

/* 右箭头 */
.arrow-icon {
	width: 32rpx;
	height: 32rpx;
	color: #d0d0d0;
	flex-shrink: 0;
	margin-left: 16rpx;
}

/* ========== 空状态卡片 ========== */
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

/* 创建群组按钮 */
.create-group-btn {
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

.create-group-btn:active {
	transform: scale(0.96);
	box-shadow: 0 2rpx 8rpx rgba(17, 17, 17, 0.4);
}

.create-btn-text {
	font-family: 'Plus Jakarta Sans', -apple-system, BlinkMacSystemFont, sans-serif;
	font-size: 28rpx;
	font-weight: 700;
	color: #ffffff;
	letter-spacing: 0.02em;
}
</style>
