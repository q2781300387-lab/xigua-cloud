<template>
  <view class="page">
			<WebSocketKeeper />
		<!-- TopAppBar with glassmorphism -->
		<view class="top-bar safe-top-bar">
			<view class="top-bar-content">
				<view class="top-bar-back" hover-class="top-bar-back-hover" @click="goBack">
					<image class="back-icon" src="/static/icons/arrow/left.svg" mode="aspectFit"></image>
				</view>
				<text class="page-title">新的朋友</text>
				<view class="top-bar-placeholder"></view>
			</view>
		</view>

    <view class="main-content">
      <!-- 收到的申请 Section -->
      <view class="section">
        <view class="section-header" @click="toggleReceived">
          <text class="section-title">收到的申请</text>
          <image class="expand-icon" :class="{ 'icon-collapsed': !showReceived }" src="/static/icons/arrow/down.svg" mode="aspectFit"></image>
        </view>

        <view v-if="showReceived" class="requests-list">
          <!-- Request Items from API -->
          <view
            v-for="(item, index) in receiveList"
            :key="index"
            class="request-item"
            :class="getRequestItemClass(item.flowStatus)"
          >
            <image
              class="avatar"
              :class="{ 'grayscale': item.flowStatus !== 0 }"
              :src="item.avatar || '/static/logo.png'"
              mode="aspectFill"
            />
            <view class="info">
              <view class="name-row">
                <text class="name" :class="{ 'added-name': item.flowStatus === 1 }">{{ item.username }}</text>
                <text class="time">{{ formatTime(item.createTime) }}</text>
              </view>
              <text class="message" :class="{ 'declined-text': item.flowStatus !== 0 }">
                {{ item.applyMsg ? '验证信息: ' + item.applyMsg : '无验证信息' }}
              </text>
            </view>
            <!-- 待处理状态：显示操作按钮 -->
            <view v-if="item.flowStatus === 0" class="actions-vertical">
              <button class="btn-reject" hover-class="btn-hover" @click.stop="handleReject(item)">拒绝</button>
              <button class="btn-accept" hover-class="btn-hover" @click.stop="handleAccept(item)">接受</button>
            </view>
            <!-- 其他状态：显示状态文本 -->
            <view v-else class="status">
              <text class="status-text" :class="{ 'primary': item.flowStatus === 1 }">
                {{ getStatusText(item.flowStatus) }}
              </text>
            </view>
          </view>

          <!-- 空状态 -->
          <view v-if="receiveList.length === 0" class="empty-state">
            <text class="empty-text">暂无收到的申请</text>
          </view>
        </view>
      </view>

      <!-- 发出的申请 Section -->
      <view class="section">
        <view class="section-header" @click="toggleSent">
          <text class="section-title">发出的申请</text>
          <image class="expand-icon" :class="{ 'icon-collapsed': !showSent }" src="/static/icons/arrow/down.svg" mode="aspectFit"></image>
        </view>

        <view v-if="showSent" class="requests-list">
          <!-- Sent Items from API -->
          <view
            v-for="(item, index) in sendList"
            :key="index"
            class="request-item active"
          >
            <image
              class="avatar"
              :src="item.avatar || '/static/logo.png'"
              mode="aspectFill"
            />
            <view class="info">
              <view class="name-row">
                <text class="name">{{ item.username }}</text>
                <text class="time">{{ formatTime(item.createTime) }}</text>
              </view>
              <text class="message">{{ item.applyMsg || '无验证信息' }}</text>
            </view>
            <view class="status">
              <text class="status-text" :class="{ 'primary': item.flowStatus === 0 }">
                {{ getSentStatusText(item.flowStatus) }}
              </text>
            </view>
          </view>

          <!-- 空状态 -->
          <view v-if="sendList.length === 0" class="empty-state">
            <text class="empty-text">暂无发出的申请</text>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { getSendFriendRequestList, getReceiveFriendRequestList } from '@/api/contact.js'
import { friendVerify } from '@/api/friend.js'
import WebSocketKeeper from '@/components/WebSocketKeeper.vue'

export default {
	components: {
		WebSocketKeeper
	},
  data() {
    return {
      showReceived: true,
      showSent: true,
      receiveList: [],
      sendList: []
    }
  },
  onLoad() {
    this.loadData()
  },
  methods: {
    goBack() {
      uni.navigateBack({
        fail: () => {
          uni.reLaunch({ url: '/pages/notice/notice-list' })
        }
      })
    },
    /**
     * 加载数据
     */
    async loadData() {
      try {
        // 并行请求两个接口
        const [sendRes, receiveRes] = await Promise.all([
          getSendFriendRequestList(),
          getReceiveFriendRequestList()
        ])

        // 处理发送列表
        if (sendRes.code === 200 && sendRes.data) {
          this.sendList = Array.isArray(sendRes.data) ? sendRes.data : (sendRes.data.list || [])
        }

        // 处理接收列表
        if (receiveRes.code === 200 && receiveRes.data) {
          this.receiveList = Array.isArray(receiveRes.data) ? receiveRes.data : (receiveRes.data.list || [])
        }
      } catch (error) {
        console.error('加载好友申请列表失败:', error)
        uni.showToast({
          title: '加载失败',
          icon: 'none'
        })
      }
    },

    toggleReceived() {
      this.showReceived = !this.showReceived
    },
    toggleSent() {
      this.showSent = !this.showSent
    },

    /**
     * 根据flowStatus获取请求项的样式类
     * 0: 待处理, 1: 已同意, 2: 已拒绝
     */
    getRequestItemClass(flowStatus) {
      if (flowStatus === 0) return 'active'
      if (flowStatus === 1) return 'added'
      if (flowStatus === 2) return 'declined'
      return 'active'
    },

    /**
     * 获取状态文本（收到的申请）
     */
    getStatusText(flowStatus) {
      const statusMap = {
        0: '待处理',
        1: '已添加',
        2: '已拒绝'
      }
      return statusMap[flowStatus] || '未知状态'
    },

    /**
     * 获取状态文本（发出的申请）
     */
    getSentStatusText(flowStatus) {
      const statusMap = {
        0: '等待验证',
        1: '对方已同意',
        2: '已拒绝'
      }
      return statusMap[flowStatus] || '未知状态'
    },

    /**
     * 格式化时间
     */
    formatTime(timeStr) {
      if (!timeStr) return ''

      try {
        const date = new Date(timeStr)
        const now = new Date()
        const diff = now - date

        // 小于1小时
        if (diff < 3600000) {
          const minutes = Math.floor(diff / 60000)
          return minutes < 1 ? '刚刚' : `${minutes}分钟前`
        }

        // 小于24小时
        if (diff < 86400000) {
          const hours = Math.floor(diff / 3600000)
          return `${hours}小时前`
        }

        // 小于7天
        if (diff < 604800000) {
          const days = Math.floor(diff / 86400000)
          return `${days}天前`
        }

        // 其他情况显示日期
        return `${date.getMonth() + 1}/${date.getDate()}`
      } catch (e) {
        return timeStr
      }
    },

    /**
     * 接受好友请求
     */
    async handleAccept(item) {
      uni.showModal({
        title: '接受好友请求',
        content: `确定接受 ${item.username} 的好友请求吗？`,
        success: async (res) => {
          if (res.confirm) {
            try {
              // 调用好友验证接口（flowStatus=2 表示接受）
              const result = await friendVerify({
                requestId: item.requestId,
                friendId: item.userId,
                flowStatus: 1
              })

              // 成功响应
              if (result.code === 200) {
                uni.showToast({
                  title: '已接受好友请求',
                  icon: 'success',
                  duration: 2000
                })
                // 刷新列表
                await this.loadData()
              } else {
                // 业务错误（如500、400等），显示接口返回的错误信息
                uni.showToast({
                  title: result.msg || '操作失败',
                  icon: 'none',
                  duration: 2000
                })
              }
            } catch (error) {
              console.error('接受好友请求失败:', error)

              // 尝试从error对象中获取错误信息
              let errorMsg = '操作失败，请重试'
              if (error && error.msg) {
                errorMsg = error.msg
              } else if (error && error.message) {
                errorMsg = error.message
              } else if (error && error.data && error.data.msg) {
                errorMsg = error.data.msg
              }

              uni.showToast({
                title: errorMsg,
                icon: 'none',
                duration: 2000
              })
            }
          }
        }
      })
    },

    /**
     * 拒绝好友请求
     */
    async handleReject(item) {
      uni.showModal({
        title: '拒绝好友请求',
        content: `确定拒绝 ${item.username} 的好友请求吗？`,
        success: async (res) => {
          if (res.confirm) {
            try {
              // 调用好友验证接口（flowStatus=2 表示拒绝）
              const result = await friendVerify({
                requestId: item.requestId,
                friendId: item.userId,
                flowStatus: 2
              })

              // 成功响应
              if (result.code === 200) {
                uni.showToast({
                  title: '已拒绝好友请求',
                  icon: 'success',
                  duration: 2000
                })
                // 刷新列表
                await this.loadData()
              } else {
                // 业务错误（如500、400等），显示接口返回的错误信息
                uni.showToast({
                  title: result.msg || '操作失败',
                  icon: 'none',
                  duration: 2000
                })
              }
            } catch (error) {
              console.error('拒绝好友请求失败:', error)

              // 尝试从error对象中获取错误信息
              let errorMsg = '操作失败，请重试'
              if (error && error.msg) {
                errorMsg = error.msg
              } else if (error && error.message) {
                errorMsg = error.message
              } else if (error && error.data && error.data.msg) {
                errorMsg = error.data.msg
              }

              uni.showToast({
                title: errorMsg,
                icon: 'none',
                duration: 2000
              })
            }
          }
        }
      })
    }
  }
}
</script>

<style scoped>
.page {
	display: block;
	min-height: 100vh;
	background-color: #f2f2f7;
}

/* TopAppBar */
.top-bar {
	position: fixed;
	top: 0;
	left: 0;
	right: 0;
	background: rgba(255, 255, 255, 0.82);
	backdrop-filter: saturate(180%) blur(20px);
	-webkit-backdrop-filter: saturate(180%) blur(20px);
	z-index: 999;
	border-bottom: 1rpx solid rgba(60, 60, 67, 0.12);
}

.top-bar-content {
	position: relative;
	height: 88rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	padding: 0 24rpx;
}

.top-bar-back {
	position: absolute;
	left: 14rpx;
	top: 0;
	width: 72rpx;
	height: 88rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	border-radius: 12rpx;
}

.top-bar-back-hover {
	background: rgba(120, 120, 128, 0.12);
}

.back-icon {
	width: 36rpx;
	height: 36rpx;
	opacity: 0.6;
}

.top-bar-placeholder {
	width: 72rpx;
	height: 88rpx;
}

.page-title {
	font-family: -apple-system, BlinkMacSystemFont, "Manrope", sans-serif;
	font-size: 32rpx;
	font-weight: 600;
	color: #1c1c1e;
	letter-spacing: -0.5rpx;
}

/* Main Content */
.main-content {
	display: block;
	padding-top: calc(var(--status-bar-height) + 112rpx);
	padding-bottom: calc(40rpx + env(safe-area-inset-bottom));
}

/* Section */
.section {
	display: block;
	background: #ffffff;
	margin: 0 24rpx 24rpx;
	border-radius: 20rpx;
	border: 1rpx solid rgba(60, 60, 67, 0.12);
	overflow: hidden;
}

.section-header {
	display: flex;
	align-items: center;
	justify-content: space-between;
	padding: 24rpx;
	cursor: pointer;
	user-select: none;
}

.section-header:active {
	opacity: 0.7;
}

.section-title {
	font-family: -apple-system, BlinkMacSystemFont, "Manrope", sans-serif;
	font-size: 26rpx;
	font-weight: 600;
	color: rgba(60, 60, 67, 0.6);
	letter-spacing: 0;
}

.expand-icon {
	width: 32rpx;
	height: 32rpx;
	opacity: 0.6;
	flex-shrink: 0;
	transition: transform 0.3s ease;
}

.expand-icon.icon-collapsed {
	transform: rotate(-90deg);
}

/* Requests List */
.requests-list {
	display: flex;
	flex-direction: column;
}

/* Request Item */
.request-item {
	padding: 24rpx;
	display: flex;
	align-items: center;
	gap: 24rpx;
	border-top: 1rpx solid rgba(60, 60, 67, 0.12);
	transition: background-color 0.15s;
}

.request-item.active {
	background-color: #ffffff;
}

.request-item.declined {
	background-color: #ffffff;
	opacity: 0.55;
}

.request-item.added {
	background-color: #ffffff;
	opacity: 0.72;
}

.request-item.active:active {
	background-color: #f2f2f7;
}

/* Avatar */
.avatar {
	width: 96rpx;
	height: 96rpx;
	border-radius: 50%;
	flex-shrink: 0;
	background: #f2f2f7;
}

.avatar.grayscale {
	filter: grayscale(100%);
}

/* Info */
.info {
	flex: 1;
	min-width: 0;
	display: flex;
	flex-direction: column;
	gap: 6rpx;
}

.name-row {
	display: flex;
	align-items: center;
	justify-content: space-between;
}

.name {
	font-family: -apple-system, BlinkMacSystemFont, "Manrope", sans-serif;
	font-size: 29rpx;
	font-weight: 600;
	color: #1c1c1e;
	flex: 1;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
	letter-spacing: -0.2rpx;
}

.name.added-name {
	color: #1c1c1e;
}

.time {
	font-family: -apple-system, BlinkMacSystemFont, sans-serif;
	font-size: 21rpx;
	color: rgba(60, 60, 67, 0.36);
	margin-left: 16rpx;
	flex-shrink: 0;
	font-weight: 400;
	text-transform: none;
}

.message {
	font-family: -apple-system, BlinkMacSystemFont, sans-serif;
	font-size: 23rpx;
	color: rgba(60, 60, 67, 0.6);
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
	line-height: 1.45;
}

.message.declined-text {
	color: rgba(60, 60, 67, 0.4);
}

/* Actions - Vertical Layout */
.actions-vertical {
	display: flex;
	flex-direction: column;
	gap: 10rpx;
	flex-shrink: 0;
}

.btn-reject,
.btn-accept {
	height: 52rpx;
	line-height: 52rpx;
	padding: 0 26rpx;
	border-radius: 9999rpx;
	font-family: -apple-system, BlinkMacSystemFont, sans-serif;
	font-size: 22rpx;
	font-weight: 500;
	border: none;
	white-space: nowrap;
	margin: 0;
}

.btn-reject {
	border: 1rpx solid rgba(60, 60, 67, 0.18);
	color: rgba(60, 60, 67, 0.6);
	background-color: #ffffff;
}

.btn-reject.btn-hover {
	background-color: #f2f2f7;
}

.btn-accept {
	background-color: #111111;
	color: #ffffff;
}

.btn-accept::after,
.btn-reject::after {
	border: 0;
}

.btn-accept.btn-hover {
	opacity: 0.86;
}

/* Status */
.status {
	flex-shrink: 0;
	padding: 0 8rpx;
}

.status-text {
	font-family: -apple-system, BlinkMacSystemFont, sans-serif;
	font-size: 23rpx;
	font-weight: 500;
	color: rgba(60, 60, 67, 0.48);
}

.status-text.primary {
	color: #111111;
}

/* Empty State */
.empty-state {
	padding: 56rpx 32rpx;
	text-align: center;
	border-top: 1rpx solid rgba(60, 60, 67, 0.12);
}

.empty-text {
	font-family: -apple-system, BlinkMacSystemFont, sans-serif;
	font-size: 24rpx;
	color: rgba(60, 60, 67, 0.4);
}
</style>
