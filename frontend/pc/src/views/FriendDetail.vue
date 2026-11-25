<template>
  <div class="friend-right">
    <div class="friend-detail">
      <div class="detail-close" @click="closeDetail">
        <i class="el-icon-close"> × </i>
      </div>
      <div class="detail-header">
        <img :src="friendDetail.avatar" alt="头像" class="detail-avatar">
        <div class="detail-name">
          {{ friendDetail.username }}
          <span class="detail-status" :class="{ online: friendDetail.isOnline }">
          {{ friendDetail.isOnline ? '在线' : '离线' }}
        </span>
        </div>
        <div class="detail-signature">
          {{ friendDetail.signature || '这个人很懒，什么都没留' }}
        </div>
      </div>

      <div class="detail-section">
        <!-- 可以继续添加更多信息项，会自动每行两个排列 -->
        <div class="section-title">基本信息</div>
        <div class="detail-rows">
          <div class="detail-row">
            <span class="row-label">性别：</span>
            <span class="row-value">{{ getSexCN(friendDetail.sex) }}</span>
          </div>
          <div class="detail-row">
            <span class="row-label">地区：</span>
            <span class="row-value">{{ friendDetail.region || '未知' }}</span>
          </div>
          <div class="detail-row">
            <span class="row-label">年龄：</span>
            <span class="row-value">{{ friendDetail.age || '未知' }}</span>
          </div>
          <div class="detail-row">
            <span class="row-label">邮箱：</span>
            <span class="row-value">{{ friendDetail.email || '未知' }}</span>
          </div>
          <div class="detail-row">
            <span class="row-label">手机：</span>
            <span class="row-value">{{ friendDetail.phone || '未知' }}</span>
          </div>
        </div>
      </div>

      <div class="detail-section">
        <div class="section-title">社交信息</div>
        <div class="detail-row">
          <span class="row-label">注册时间：</span>
          <span class="row-value">{{ formatDetailTime(friendDetail.registerTime) }}</span>
        </div>
        <div class="detail-row">
          <span class="row-label">成为好友：</span>
          <span class="row-value">{{ formatDetailTime(friendDetail.createFriendTime) }}</span>
        </div>
      </div>

      <div class="detail-actions">
        <div class="action-btn-container">
          <button class="action-btn chat-btn" @click="startChat(friendDetail.userId)">
            <i class="icon-message"></i> 发消息
          </button>
        </div>
        <div class="action-btn-container">
          <button class="action-btn delete-btn" @click="deleteFriend()">
            <i class="icon-delete"></i> 删除好友
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import {delFriend, getFriendDetail} from '@/api/friendRelation.js'
import {set} from "@/utils/localStorage.js";

export default {
  data() {
    return {
      activeTab: 'friends',
      unreadRequests: 0,
      friends: [],
      friendRequests: [],
      receivedRequests: [],
      sentRequests: [],
      contextMenu: {
        visible: false,
        top: 0,
        left: 0,
        friendId:null
      },
      showFriendDetailFlag: false,
      friendDetail: {},
    }
  },
  created() {

  },
  computed: {

  },
  methods: {
    startChat(friendId) {
      set('activeMenu', 'chat');
      this.$router.push({
        path: 'Chat',
        query: {
          "friendId": friendId
        }
      })
    },

    getFriendDetail(friendId) {
      getFriendDetail(friendId).then(response => {
        if (response.code === 200) {
          this.friendDetail = response.data
        }else{
          this.friendDetail = null;
          this.$message.error('获取好友详情失败')
        }
      })
    },

    // 删除好友
    deleteFriend() {
      const friend = this.friendDetail;
      if (!friend) {
        this.$message.error('请先获取好友详情');
        return;
      }

      // 删除好友逻辑
      this.$confirm(`确定要删除好友 ${friend.username} 吗?`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        delFriend(friend.userId).then(response => {
          if (response.code === 200) {
            this.$message.success('删除好友成功');
            // 刷新好友列表
            this.$parent.getContactCount();
            this.$parent.getFriendList();
            // 关闭详情页
            this.closeDetail();
          }else{
            this.$message.error('删除好友失败')
          }
        })
        // 成功后刷新好友列表
        this.fetchFriendList()
      }).catch(() => {
        // 取消操作
      })
    },
    getSexCN(sex) {
      const sexMap = {
        1: '男',
        2: '女'
      };
      return sexMap[sex] || '未知';
    },

    formatDetailTime(timestamp) {
      if (!timestamp) return '未知';
      const date = new Date(timestamp);
      return `${date.getFullYear()}年${date.getMonth()+1}月${date.getDate()}日`;
    },

    closeDetail() {
      this.$emit('close'); // 触发close事件通知父组件
    },
  },
}
</script>

<style scoped>
/* ===== 详情页 · iOS 设计语言 ===== */
.friend-right,
.group-right {
  width: 100%;
  height: 100%;
  background: var(--bg);
  font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', 'SF Pro Text', 'PingFang SC', 'Helvetica Neue', 'Microsoft YaHei', sans-serif;
  -webkit-font-smoothing: antialiased;
}

.friend-detail,
.group-detail {
  padding: 20px;
  height: 100%;
  overflow-y: auto;
  position: relative;
  box-sizing: border-box;
}

/* iOS 分组标题 */
.section-title {
  padding: 0 8px 8px;
  font-size: 13px;
  font-weight: 600;
  color: var(--label-secondary);
  letter-spacing: 0.2px;
}

/* 头部白卡 */
.detail-header {
  text-align: center;
  padding: 28px 20px 24px;
  margin-bottom: 18px;
  background: var(--card);
  border-radius: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06), 0 8px 24px rgba(0, 0, 0, 0.04);
}

.detail-avatar {
  width: 88px;
  height: 88px;
  border-radius: 50%;
  border: 3px solid #ffffff;
  margin-bottom: 12px;
  object-fit: cover;
  box-shadow: 0 6px 18px rgba(0, 0, 0, 0.16);
}

.detail-name {
  font-size: 20px;
  font-weight: 700;
  letter-spacing: -0.3px;
  margin-bottom: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--label);
}

.detail-status {
  font-size: 12px;
  font-weight: 500;
  color: var(--label-secondary);
  margin-left: 8px;
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.detail-status::before {
  content: '';
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #c7c7cc;
}

.detail-status.online {
  color: #34c759;
}

.detail-status.online::before {
  background: #34c759;
  box-shadow: 0 0 5px rgba(52, 199, 89, 0.5);
}

.detail-signature {
  font-size: 14px;
  color: var(--label-secondary);
}

/* 信息区白卡 */
.detail-section {
  margin-bottom: 18px;
}

.detail-rows {
  display: flex;
  flex-wrap: wrap;
  gap: 0;
  background: var(--card);
  border-radius: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  padding: 6px 18px;
}

.detail-row {
  flex: 0 0 100%;
  display: flex;
  align-items: center;
  padding: 13px 0;
  font-size: 14.5px;
  border-bottom: 1px solid var(--separator);
}

.detail-row:last-child {
  border-bottom: none;
}

.row-label {
  color: var(--label-secondary);
  min-width: 64px;
  margin-right: 12px;
  font-size: 14.5px;
}

.row-value {
  color: var(--label);
  font-size: 14.5px;
  flex: 1;
  word-break: break-all;
}

/* 操作区 · iOS 胶囊按钮 */
.detail-actions {
  display: flex;
  justify-content: center;
  gap: 14px;
  margin-top: 24px;
}

.action-btn-container {
  display: flex;
  justify-content: center;
  flex: 1;
  max-width: 160px;
}

.action-btn {
  padding: 12px 20px;
  min-width: 120px;
  border-radius: 999px;
  font-size: 14.5px;
  font-weight: 600;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s cubic-bezier(0.32, 0.72, 0, 1);
  border: none;
}

.action-btn i {
  margin-right: 6px;
  font-size: 14px;
}

.chat-btn {
  background-color: #007aff;
  color: white;
  box-shadow: 0 4px 14px rgba(0, 122, 255, 0.35);
}

.chat-btn:hover {
  background-color: #3395ff;
  transform: translateY(-1px);
  box-shadow: 0 8px 20px rgba(0, 122, 255, 0.45);
}

.delete-btn,
.quit-btn {
  background-color: var(--card);
  color: #ff3b30;
  border: 1px solid rgba(255, 59, 48, 0.3);
}

.delete-btn:hover,
.quit-btn:hover {
  background-color: rgba(255, 59, 48, 0.08);
  transform: translateY(-1px);
}

/* 关闭按钮 */
.detail-close {
  position: absolute;
  top: 28px;
  right: 24px;
  width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: rgba(120, 120, 128, 0.14);
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.32, 0.72, 0, 1);
  z-index: 10;
}

.detail-close:hover {
  background: var(--fill-strong);
  transform: rotate(90deg);
}

.detail-close i {
  font-size: 15px;
  font-style: normal;
  color: var(--label-secondary);
}

/* 空态 */
.empty-tip {
  text-align: center;
  color: var(--label-secondary);
  padding: 60px 20px;
}

.empty-tip i {
  font-size: 48px;
  margin-bottom: 10px;
  opacity: 0.5;
}

.empty-tip p {
  margin: 0;
  font-size: 14px;
}
</style>