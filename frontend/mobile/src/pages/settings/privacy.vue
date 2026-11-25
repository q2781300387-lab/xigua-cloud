<template>
	<view class="privacy-page">
		<view class="header safe-top-bar">
			<view class="header-back" hover-class="header-back-hover" @click="handleBack">
				<image class="back-icon" src="/static/icons/arrow/left.svg" mode="aspectFit"></image>
			</view>
			<text class="header-title">隐私与安全</text>
		</view>

		<scroll-view class="main-content" scroll-y>
			<view class="content">
				<view class="group">
					<view class="group-title">消息通知</view>
					<view class="row">
						<view class="row-main">
							<text class="row-title">通知显示消息内容</text>
							<text class="row-desc">关闭后锁屏通知只提示“收到一条新消息”</text>
						</view>
						<switch :checked="settings.notificationPreview === 1" color="#111111" @change="onToggle('notificationPreview', $event)" />
					</view>
					<view class="row row-border">
						<view class="row-main">
							<text class="row-title">新消息提示音</text>
							<text class="row-desc">收到消息时播放提示音</text>
						</view>
						<switch :checked="settings.notificationSound === 1" color="#111111" @change="onToggle('notificationSound', $event)" />
					</view>
				</view>

				<view class="group">
					<view class="group-title">隐私</view>
					<view class="row">
						<view class="row-main">
							<text class="row-title">展示在线状态</text>
							<text class="row-desc">关闭后好友看不到你的在线/离线状态</text>
						</view>
						<switch :checked="settings.showOnlineStatus === 1" color="#111111" @change="onToggle('showOnlineStatus', $event)" />
					</view>
					<view class="row row-border">
						<view class="row-main">
							<text class="row-title">发送已读回执</text>
							<text class="row-desc">关闭后对方不会看到“已读”</text>
						</view>
						<switch :checked="settings.showReadReceipt === 1" color="#111111" @change="onToggle('showReadReceipt', $event)" />
					</view>
					<view class="row row-border">
						<view class="row-main">
							<text class="row-title">允许通过手机号找到我</text>
							<text class="row-desc">关闭后不能通过手机号搜索添加</text>
						</view>
						<switch :checked="settings.allowPhoneSearch === 1" color="#111111" @change="onToggle('allowPhoneSearch', $event)" />
					</view>
					<view class="row">
						<view class="row-main">
							<text class="row-title">加好友需要验证</text>
							<text class="row-desc">关闭后任何人可直接添加你为好友</text>
						</view>
						<switch :checked="settings.friendVerify === 1" color="#111111" @change="onToggle('friendVerify', $event)" />
					</view>
				</view>

				<view class="group">
					<view class="group-title">黑名单</view>
					<view v-if="blocks.length === 0" class="empty">暂无被屏蔽的用户</view>
					<view v-for="item in blocks" :key="item.id" class="row row-border block-row">
						<view class="block-avatar">{{ (item.name || '?').slice(0, 1) }}</view>
						<view class="row-main">
							<text class="row-title">{{ item.name }}</text>
							<text class="row-desc">已屏蔽消息</text>
						</view>
						<view class="mini-btn" hover-class="mini-btn-hover" @click="removeBlock(item)">移出</view>
					</view>
				</view>

				<view class="group">
					<view class="group-title">登录设备</view>
					<view v-for="(device, index) in devices" :key="device.id" class="device-row" :class="{ 'row-border': index < devices.length - 1 }">
						<view class="device-icon">{{ device.platform === 'App' ? '📱' : '💻' }}</view>
						<view class="row-main">
							<view class="device-head">
								<text class="row-title">{{ device.region || device.ip || '未知位置' }}</text>
								<text v-if="device.current" class="current-tag">当前设备</text>
							</view>
							<text class="row-desc">{{ friendlyUa(device.userAgent) }} · {{ device.lastLoginTime }}</text>
						</view>
					</view>
					<button class="secondary-btn" hover-class="secondary-hover" @click="handleKickOthers">下线其它所有设备</button>
				</view>

				<view class="group">
					<view class="group-title">安全与数据</view>
					<view class="row row-border" @click="handleReport">
						<view class="row-main">
							<text class="row-title">举报违规用户</text>
							<text class="row-desc">提交后由管理员审核处理</text>
						</view>
						<text class="chevron">›</text>
					</view>
					<view class="row row-border" @click="handleExport">
						<view class="row-main">
							<text class="row-title">导出个人数据</text>
							<text class="row-desc">下载账号资料、联系人、群与最近消息</text>
						</view>
						<text class="chevron">›</text>
					</view>
					<view class="row danger-row" @click="openDeactivate">
						<view class="row-main">
							<text class="row-title danger-text">注销账号</text>
							<text class="row-desc">注销后所有设备下线，且无法登录</text>
						</view>
						<text class="chevron">›</text>
					</view>
				</view>

				<text class="footer-note">设置会实时保存到服务器，PC 与手机共用同一套偏好。</text>
			</view>
		</scroll-view>

		<view v-if="deactivateVisible" class="modal-mask" @click="closeDeactivate">
			<view class="modal-card" @click.stop>
				<text class="modal-title">注销账号</text>
				<text class="modal-desc">此操作会立即下线所有设备并停用账号。请输入登录密码，并输入“注销账号”确认。</text>
				<input v-model="deactivateForm.password" class="modal-input" :password="true" placeholder="登录密码" placeholder-class="input-ph" />
				<input v-model="deactivateForm.confirmText" class="modal-input" placeholder="输入：注销账号" placeholder-class="input-ph" />
				<button class="danger-btn" :disabled="deactivateBusy" @click="handleDeactivate">{{ deactivateBusy ? '处理中…' : '确认注销' }}</button>
				<button class="cancel-btn" @click="closeDeactivate">取消</button>
			</view>
		</view>
	</view>
</template>

<script>
import swipeBack from '@/mixins/swipeBack.js';
import { firstLetterFriends } from '@/api/contact.js';
import {
	getPrivacySettings,
	savePrivacySettings,
	getBlockList,
	unblockUser,
	getLoginDevices,
	kickOtherDevices,
	submitReport,
	exportPersonalData,
	deactivateAccount
} from '@/api/privacy.js';
import { closeWebSocket } from '@/utils/websocket.js';
import config from '@/config/index.js';

export default {
	mixins: [swipeBack],
	data() {
		return {
			settings: {
				notificationPreview: 1,
				notificationSound: 1,
				showOnlineStatus: 1,
				showReadReceipt: 1,
				allowPhoneSearch: 1,
				friendVerify: 1
			},
			blocks: [],
			devices: [],
			friends: [],
			deactivateVisible: false,
			deactivateBusy: false,
			deactivateForm: { password: '', confirmText: '' }
		};
	},
	onLoad() {
		this.loadAll();
	},
	methods: {
		async loadAll() {
			await Promise.all([this.loadSettings(), this.loadBlocks(), this.loadDevices(), this.loadFriends()]);
		},
		async loadSettings() {
			try {
				const res = await getPrivacySettings();
				if (res && res.data) this.settings = Object.assign(this.settings, res.data);
			} catch (e) {}
		},
		async loadBlocks() {
			try {
				const res = await getBlockList();
				this.blocks = (res && res.data) || [];
			} catch (e) {
				this.blocks = [];
			}
		},
		async loadDevices() {
			try {
				const res = await getLoginDevices();
				this.devices = (res && res.data) || [];
			} catch (e) {
				this.devices = [];
			}
		},
		async loadFriends() {
			try {
				const res = await firstLetterFriends();
				const groups = (res && res.data) || [];
				this.friends = groups.reduce((all, group) => all.concat(group.list || []), []);
			} catch (e) {
				this.friends = [];
			}
		},
		async onToggle(key, event) {
			const value = event && event.detail && event.detail.value ? 1 : 0;
			const previous = this.settings[key];
			this.settings[key] = value;
			try {
				const res = await savePrivacySettings(this.settings);
				if (res && res.data) this.settings = Object.assign(this.settings, res.data);
				uni.showToast({ title: '已保存', icon: 'none', duration: 1200 });
			} catch (e) {
				this.settings[key] = previous;
				uni.showToast({ title: '保存失败，请重试', icon: 'none' });
			}
		},
		async removeBlock(item) {
			uni.showModal({
				title: '移出黑名单',
				content: `确定不再屏蔽「${item.name}」吗？`,
				success: async (res) => {
					if (!res.confirm) return;
					try {
						await unblockUser(item.id);
						this.blocks = this.blocks.filter((b) => b.id !== item.id);
						uni.showToast({ title: '已移出黑名单', icon: 'none' });
					} catch (e) {}
				}
			});
		},
		handleKickOthers() {
			uni.showModal({
				title: '下线其它设备',
				content: '其它设备上的登录会立即失效，当前设备不受影响。',
				success: async (res) => {
					if (!res.confirm) return;
					try {
						const result = await kickOtherDevices();
						const count = result && typeof result.data === 'number' ? result.data : 0;
						uni.showToast({ title: count > 0 ? `已下线 ${count} 台设备` : '没有其它在线设备', icon: 'none' });
						this.loadDevices();
					} catch (e) {}
				}
			});
		},
		handleReport() {
			if (!this.friends.length) {
				uni.showToast({ title: '暂无可举报的好友', icon: 'none' });
				return;
			}
			uni.showActionSheet({
				itemList: this.friends.slice(0, 6).map((f) => f.username || f.name || '用户'),
				success: (res) => {
					const friend = this.friends[res.tapIndex];
					if (friend) this.chooseReportReason(friend.id || friend.friendId || friend.userId, friend.username);
				}
			});
		},
		chooseReportReason(targetId, name) {
			const reasons = ['骚扰辱骂', '虚假身份', '垃圾广告', '色情低俗', '诈骗欺诈'];
			uni.showActionSheet({
				itemList: reasons,
				success: async (res) => {
					try {
						await submitReport({ targetId, targetType: 'user', reason: reasons[res.tapIndex], detail: `举报用户：${name || targetId}` });
						uni.showToast({ title: '举报已提交', icon: 'success' });
					} catch (e) {}
				}
			});
		},
		async handleExport() {
			try {
				const res = await exportPersonalData();
				const payload = JSON.stringify(res.data || res, null, 2);
				const fileName = `xigua-data-${Date.now()}.json`;
				// #ifdef H5
				const blob = new Blob([payload], { type: 'application/json' });
				const url = URL.createObjectURL(blob);
				const a = document.createElement('a');
				a.href = url;
				a.download = fileName;
				document.body.appendChild(a);
				a.click();
				document.body.removeChild(a);
				setTimeout(() => URL.revokeObjectURL(url), 3000);
				uni.showToast({ title: '已开始下载', icon: 'none' });
				// #endif
				// #ifndef H5
				uni.setClipboardData({ data: payload, success: () => uni.showToast({ title: '数据已复制', icon: 'none' }) });
				// #endif
			} catch (e) {}
		},
		friendlyUa(ua) {
			if (!ua) return '未知设备';
			if (/iPhone|iPad|iOS/i.test(ua)) return 'iOS 设备';
			if (/Android/i.test(ua)) return 'Android 设备';
			if (/Macintosh/i.test(ua)) return 'Mac';
			if (/Windows/i.test(ua)) return 'Windows';
			return ua.slice(0, 28);
		},
		openDeactivate() {
			this.deactivateForm = { password: '', confirmText: '' };
			this.deactivateVisible = true;
		},
		closeDeactivate() {
			this.deactivateVisible = false;
		},
		async handleDeactivate() {
			if (!this.deactivateForm.password) {
				uni.showToast({ title: '请输入登录密码', icon: 'none' });
				return;
			}
			if (this.deactivateForm.confirmText.trim() !== '注销账号') {
				uni.showToast({ title: '请输入“注销账号”确认', icon: 'none' });
				return;
			}
			this.deactivateBusy = true;
			try {
				await deactivateAccount(this.deactivateForm);
				closeWebSocket();
				uni.removeStorageSync(config.tokenKey);
				uni.removeStorageSync(config.userInfoKey);
				uni.removeStorageSync('userInfo');
				this.deactivateVisible = false;
				uni.showToast({ title: '账号已注销', icon: 'none' });
				setTimeout(() => uni.reLaunch({ url: '/pages/login/login' }), 1200);
			} catch (e) {
			} finally {
				this.deactivateBusy = false;
			}
		},
		handleBack() {
			uni.navigateBack({
				fail: () => uni.reLaunch({ url: '/pages/profile/profile' })
			});
		}
	}
};
</script>

<style lang="scss" scoped>
.privacy-page {
	width: 100%;
	height: 100vh;
	background: #f2f2f7;
	display: flex;
	flex-direction: column;
	overflow: hidden;
}
.header {
	position: relative;
	height: 88rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	background: rgba(255, 255, 255, 0.82);
	backdrop-filter: saturate(180%) blur(20px);
	-webkit-backdrop-filter: saturate(180%) blur(20px);
	border-bottom: 1rpx solid rgba(60, 60, 67, 0.12);
	z-index: 10;
	flex-shrink: 0;
}
.header-back {
	position: absolute;
	left: 16rpx;
	bottom: 0;
	width: 72rpx;
	height: 88rpx;
	display: flex;
	align-items: center;
	justify-content: center;
}
.header-back-hover { background: rgba(120, 120, 128, 0.12); }
.back-icon { width: 36rpx; height: 36rpx; opacity: 0.6; }
.header-title { font-size: 32rpx; font-weight: 600; color: #1c1c1e; }
.main-content { flex: 1; overflow: hidden; }
.content { padding: 24rpx 24rpx calc(60rpx + env(safe-area-inset-bottom)); }
.group {
	background: #ffffff;
	border-radius: 20rpx;
	border: 1rpx solid rgba(60, 60, 67, 0.12);
	margin-bottom: 24rpx;
	overflow: hidden;
}
.group-title {
	font-size: 23rpx;
	font-weight: 600;
	color: rgba(60, 60, 67, 0.5);
	padding: 22rpx 24rpx 6rpx;
	letter-spacing: 0.02em;
}
.row {
	display: flex;
	align-items: center;
	padding: 22rpx 24rpx;
	min-height: 72rpx;
}
.row-border { border-bottom: 1rpx solid rgba(60, 60, 67, 0.1); }
.row-main { flex: 1; display: flex; flex-direction: column; gap: 5rpx; }
.row-title { font-size: 27rpx; font-weight: 500; color: #1c1c1e; }
.row-desc { font-size: 21rpx; line-height: 30rpx; color: rgba(60, 60, 67, 0.48); }
.chevron { font-size: 34rpx; color: rgba(60, 60, 67, 0.28); }
.empty { padding: 30rpx 24rpx 34rpx; font-size: 23rpx; color: rgba(60, 60, 67, 0.45); }
.block-row { gap: 18rpx; }
.block-avatar {
	width: 64rpx;
	height: 64rpx;
	border-radius: 50%;
	background: #f2f2f7;
	display: flex;
	align-items: center;
	justify-content: center;
	font-size: 24rpx;
	font-weight: 600;
	color: #1c1c1e;
}
.mini-btn {
	padding: 10rpx 22rpx;
	border-radius: 999rpx;
	background: #111111;
	color: #ffffff;
	font-size: 22rpx;
}
.mini-btn-hover { opacity: 0.82; }
.device-row { display: flex; padding: 22rpx 24rpx; gap: 16rpx; align-items: center; }
.device-icon { font-size: 34rpx; width: 48rpx; text-align: center; }
.device-head { display: flex; align-items: center; gap: 10rpx; }
.current-tag {
	font-size: 19rpx;
	color: #111111;
	padding: 2rpx 10rpx;
	border: 1rpx solid rgba(17, 17, 17, 0.22);
	border-radius: 999rpx;
}
.secondary-btn {
	margin: 18rpx 24rpx 24rpx;
	height: 76rpx;
	line-height: 76rpx;
	border-radius: 14rpx;
	background: #f2f2f7;
	color: #1c1c1e;
	font-size: 25rpx;
	border: 0;
}
.secondary-btn::after, .danger-btn::after, .cancel-btn::after { border: 0; }
.secondary-hover { background: #e9e9ee; }
.danger-text { color: #d92d20; }
.footer-note {
	display: block;
	text-align: center;
	font-size: 21rpx;
	color: rgba(60, 60, 67, 0.38);
	padding: 6rpx 24rpx 20rpx;
}
.modal-mask {
	position: fixed;
	top: 0; left: 0; right: 0; bottom: 0;
	background: rgba(0, 0, 0, 0.38);
	z-index: 200;
	display: flex;
	align-items: center;
	justify-content: center;
	padding: 40rpx;
}
.modal-card {
	width: 100%;
	background: #ffffff;
	border-radius: 24rpx;
	padding: 32rpx 28rpx calc(28rpx + env(safe-area-inset-bottom));
}
.modal-title { font-size: 30rpx; font-weight: 600; color: #1c1c1e; }
.modal-desc { display: block; margin-top: 12rpx; font-size: 22rpx; line-height: 34rpx; color: rgba(60, 60, 67, 0.55); }
.modal-input {
	margin-top: 18rpx;
	height: 84rpx;
	padding: 0 22rpx;
	background: #f2f2f7;
	border-radius: 12rpx;
	font-size: 25rpx;
}
.input-ph { color: rgba(60, 60, 67, 0.35); }
.danger-btn {
	margin-top: 24rpx;
	height: 84rpx;
	line-height: 84rpx;
	background: #d92d20;
	color: #ffffff;
	font-size: 26rpx;
	font-weight: 600;
	border-radius: 14rpx;
}
.cancel-btn {
	margin-top: 14rpx;
	height: 80rpx;
	line-height: 80rpx;
	background: #f2f2f7;
	color: #1c1c1e;
	font-size: 25rpx;
	border-radius: 14rpx;
}
</style>
