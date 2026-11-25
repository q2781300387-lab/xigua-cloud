<template>
  <!-- 弹框遮罩层 -->
  <div class="dialog-overlay">
    <div class="multi-card-dialog">
      <!-- 弹框标题栏 -->
      <div class="dialog-header">
        <div class="dialog-tabs">
          <div
              class="tab-item"
              :class="{active: activeTab === 'friend'}"
              @click="switchTab('friend')"
          >
            添加好友
          </div>
          <div
              class="tab-item"
              :class="{active: activeTab === 'group'}"
              @click="switchTab('group')"
          >
            创建群聊
          </div>
          <div
              class="tab-item"
              :class="{active: activeTab === 'robot'}"
              @click="switchTab('robot')"
          >
            创建机器人
          </div>
        </div>
        <div class="dialog-close" @click="closeDialog">×</div>
      </div>

      <!-- 添加好友卡片 -->
      <div class="card-content" v-show="activeTab === 'friend'">
        <!-- 搜索区域 -->
        <div class="search-section">
          <input
              v-model="searchQuery"
              type="text"
              placeholder="请输入用户名/qq号"
              @input="handleSearchInput"
              @keyup.enter="searchFriends"
              ref="searchInput"
          >
          <button @click="searchFriends" class="search-btn">搜索</button>
        </div>

        <!-- 搜索结果列表 -->
        <div class="search-results" v-if="searchResults.length > 0">
          <div class="result-item" v-for="user in searchResults" :key="user.id">
            <div class="user-avatar">
              <img v-if="user.avatar" :src="user.avatar" alt="头像">
              <span v-else>{{ user.name.charAt(0) }}</span>
            </div>
            <div class="user-info">
              <div class="name-line">
                <span class="user-name">{{ user.name }}</span>
                <span class="user-sex" v-if="user.sex">
                  <img v-if="user.sex === 1" src="../static/icons/man.png" alt="男" class="sex-icon">
                  <img v-if="user.sex === 2" src="../static/icons/woman.png" alt="女" class="sex-icon">
                </span>
                <span class="user-status" :class="{'online': user.isOnline, 'offline': !user.isOnline}">
                  <i class="status-icon"></i>
                  {{ user.isOnline ? '在线' : '离线' }}
                </span>
              </div>
              <div class="user-id">微信号: xg_{{ user.id }}</div>
            </div>
            <button
                class="add-btn"
                @click="sendFriendRequest(user.id)"
                :disabled="user.requestSent"
            >
              {{ user.requestSent ? '已发送' : '添加' }}
            </button>
          </div>
        </div>

        <!-- 无结果提示 -->
        <div class="no-results" v-else-if="searchQuery && !isSearching">
          没有找到相关用户
        </div>

        <!-- 搜索提示 -->
        <div class="search-tips" v-else>
          输入用户名/qq号搜索好友
        </div>
      </div>

      <!-- 创建群聊卡片 -->
      <div class="card-content" v-show="activeTab === 'group'">
        <div class="group-join-search">
          <div class="group-join-title">加入群聊</div>
          <div class="group-join-bar">
            <input
                v-model="joinKeyword"
                class="group-join-input"
                type="text"
                placeholder="输入群号或群名称搜索"
                @keyup.enter="searchJoinGroups"
            />
            <button class="group-join-btn" :disabled="joinLoading" @click="searchJoinGroups">
              {{ joinLoading ? '搜索中…' : '搜索' }}
            </button>
          </div>
          <div v-if="joinResults.length" class="group-join-results">
            <div v-for="g in joinResults" :key="g.groupId" class="group-join-row">
              <div class="group-join-avatar">
                <img v-if="g.groupAvatar" :src="g.groupAvatar" alt="">
                <span v-else>{{ (g.groupName || '群')[0] }}</span>
              </div>
              <div class="group-join-info">
                <div class="group-join-name">{{ g.groupName }}</div>
                <div class="group-join-meta">群号 {{ g.groupId }} · {{ g.currentMember || 0 }} 人</div>
              </div>
              <button v-if="g.joined" class="group-join-btn joined" disabled>已在群内</button>
              <button v-else class="group-join-btn" @click="applyJoinGroup(g)">申请加入</button>
            </div>
          </div>
          <div v-else-if="joinSearched" class="group-join-empty">没有找到匹配的群聊</div>
        </div>
        <div class="group-creation-form">
          <div class="member-selection">
            <label>选择成员</label>
            <div class="member-list">
              <div
                  class="member-item"
                  v-for="friend in friends"
                  :key="friend.userId"
                  @click="toggleMemberSelection(friend.userId)"
              >
                <div class="member-avatar">
                  <img v-if="friend.avatar" :src="friend.avatar" alt="头像">
                  <span v-else>{{ friend.name.charAt(0) }}</span>
                </div>
                <div class="member-name">{{ friend.name }}</div>
                <div class="member-checkbox" :class="{selected: selectedMembers.includes(friend.userId)}">
                  <i class="check-icon"></i>
                </div>
              </div>
            </div>
          </div>

          <button
              class="create-group-btn"
              @click="createGroup"
              :disabled="!canCreateGroup || creatingGroup"
          >
            <span v-if="creatingGroup">创建中...</span>
            <span v-else>创建群聊</span>
          </button>
        </div>
      </div>

      <!-- 创建机器人卡片 -->
      <div class="card-content" v-show="activeTab === 'robot'">
        <el-form
            :model="robotForm"
            :rules="robotRules"
            ref="robotFormRef"
            class="robot-creation-form"
        >
          <!-- 头像上传区域 -->
          <div class="avatar-upload-section">
            <div class="avatar-upload-label">机器人头像</div>
            <div class="avatar-upload-area">
              <div class="avatar-preview" @click="triggerAvatarUpload">
                <img v-if="robotForm.avatar" :src="robotForm.avatar" alt="头像预览" class="avatar-image">
                <div v-else class="avatar-placeholder">
                  <i class="upload-icon">+</i>
                  <span>点击上传头像</span>
                </div>
                <input
                    type="file"
                    ref="avatarInput"
                    @change="handleAvatarUpload"
                    accept="image/*"
                    class="avatar-input"
                >
              </div>
              <div class="avatar-tips">建议尺寸 120×120 px，支持 JPG、PNG 格式</div>
            </div>
          </div>

          <!-- 名称 -->
          <el-form-item prop="name">
            <label>机器人名称 <span class="required">*</span></label>
            <el-input
                v-model="robotForm.name"
                placeholder="请输入机器人名称（2-20字符）"
                maxlength="20"
                show-word-limit
            />
          </el-form-item>

          <!-- 描述 -->
          <el-form-item prop="description">
            <label>机器人描述 <span class="required">*</span></label>
            <el-input
                v-model="robotForm.description"
                placeholder="简要描述机器人的功能和特点（5-50字符）"
                maxlength="50"
                show-word-limit
            />
          </el-form-item>

          <!-- 提示词 -->
          <el-form-item prop="prompt">
            <label>系统提示词 <span class="required">*</span></label>
            <el-input
                v-model="robotForm.prompt"
                type="textarea"
                placeholder="例如：你是一个专业的客服助手，回答要友好、专业且简洁。请用中文回答用户问题..."
                maxlength="500"
                :rows="4"
                show-word-limit
            />
          </el-form-item>

          <el-form-item>
            <el-button
                class="create-robot-btn"
                @click="createRobot"
                :loading="creatingRobot"
                :disabled="creatingRobot"
                type="primary"
            >
              <i class="btn-icon">🤖</i>
              创建机器人
            </el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script>
import {getListByName, uploadAvatar} from "@/api/user.js";
import { sendFriendRequest } from "@/api/friendRelation.js";
import { getFriendList } from "@/api/contact.js";
import { createGroup, searchGroups, requestJoinGroup } from "@/api/group.js";
import { getObject} from "@/utils/localStorage.js";
import {uploadFile} from "@/api/file.js";
import {createBot} from "@/api/bot.js";

export default {
  props: {
    // 从联系人页快捷入口打开时，可以直接落在“创建群聊”卡片
    initialTab: {
      type: String,
      default: 'friend'
    }
  },
  data() {
    return {
      activeTab: 'friend', // 默认显示添加好友卡片
      // 添加好友相关数据
      searchQuery: '',
      searchResults: [],
      isSearching: false,
      // 创建群聊相关数据
      friends: [],
      selectedMembers: [],
      // 加入群聊搜索
      joinKeyword: '',
      joinResults: [],
      joinLoading: false,
      joinSearched: false,
      robotForm: {
        name: '',
        avatar: '',
        description: '',
        prompt: ''
      },
      selectedAvatarFile: null,
      robotFormRef: null,
      creatingGroup: false,
      creatingRobot: false,
      // 表单验证规则
      robotRules: {
        name: [
          { required: true, message: '请输入机器人名称', trigger: 'blur' },
          { min: 2, max: 20, message: '机器人名称长度为 2 到 20 个字符', trigger: 'blur' }
        ],
        description: [
          { required: true, message: '请输入机器人描述', trigger: 'blur' },
          { min: 5, max: 50, message: '机器人描述长度为 5 到 50 个字符', trigger: 'blur' }
        ],
        prompt: [
          { required: true, message: '请输入系统提示词', trigger: 'blur' },
          { min: 10, message: '提示词至少需要 10 个字符', trigger: 'blur' }
        ]
      }
    }
  },
  computed: {
    canCreateGroup() {
      return this.selectedMembers.length >= 2;
    }
  },
  methods: {
    closeDialog() {
      this.$emit('close');
    },

    // 切换卡片
    switchTab(tab) {
      this.activeTab = tab;
      if (tab === 'group' && this.friends.length === 0) {
        // todo 切换到创建群组 可以单独在显示好友列表加个loading
        this.loadFriendsForGroup();
      }
    },

    // 添加好友相关方法
    handleSearchInput() {
      if (!this.searchQuery.trim()) {
        this.searchResults = [];
      }
    },
    async searchFriends() {
      if (!this.searchQuery.trim()) {
        this.searchResults = [];
        return;
      }

      this.isSearching = true;

      try {
        const response = await getListByName(this.searchQuery);
        this.searchResults = response.data.map(user => ({
          id: user.id,
          name: user.username,
          avatar: user.avatar,
          isOnline: user.isOnline,
          sex: user.sex,
          requestSent: false
        }));
      } catch (error) {
        console.error('搜索用户失败:', error);
        this.searchResults = [];
      } finally {
        this.isSearching = false;
      }
    },
    sendFriendRequest(userId) {
      const user = this.searchResults.find(u => u.id === userId);
      const reqData = {
        friendId: userId,
        applyMsg: '申请添加你为好友'
      }
      sendFriendRequest(reqData).then(response => {
        if (response.code === 200) {
          user.requestSent = true;
          this.$message.success('添加好友请求已发送');
        }
      })
    },

    // 创建群聊相关方法
    async loadFriendsForGroup() {
      try {
        const response = await getFriendList();
        this.friends = response.data.map(friend => ({
          userId: friend.userId,
          name: friend.username,
          avatar: friend.avatar
        }));
      } catch (error) {
        console.error('加载好友列表失败:', error);
        this.$message.error('加载好友列表失败');
      }
    },

    // 创建群聊 选中成员
    toggleMemberSelection(memberId) {
      const index = this.selectedMembers.indexOf(memberId);
      if (index === -1) {
        this.selectedMembers.push(memberId);
      } else {
        this.selectedMembers.splice(index, 1);
      }
    },

    // 创建群组
    async searchJoinGroups() {
      const keyword = String(this.joinKeyword || '').trim();
      if (!keyword) {
        this.$message.warning('请输入群号或群名称');
        return;
      }
      this.joinLoading = true;
      this.joinSearched = false;
      try {
        const res = await searchGroups(keyword);
        this.joinResults = (res && res.data) || [];
        this.joinSearched = true;
      } catch (e) {
        this.joinResults = [];
      } finally {
        this.joinLoading = false;
      }
    },
    async applyJoinGroup(group) {
      try {
        const res = await requestJoinGroup({ groupId: group.groupId, reason: '' });
        if (res && res.code === 200) {
          this.$message.success('申请已提交，等待群主审批');
        }
      } catch (e) { /* request 层已提示 */ }
    },

    async createGroup() {
      if (!this.canCreateGroup) return;
      const currentUserId = getObject('userInfo').id;

      const groupData = {
        memberIds: [currentUserId, ...this.selectedMembers]
      };

      this.creatingGroup = true;
      try{
        const response = await createGroup(groupData);
        if (response.code === 200) {
          this.$message.success('群聊创建成功');
          this.closeDialog();
          this.$emit('group-created', response.data);
        } else {
          this.$message.error(response.msg || '创建群聊失败');
        }

        // 延迟500毫秒 （缓存是异步，可能有一点延迟）
        setTimeout(() => {
          // 刷新消息列表，调用父组件getLastMes方法
          if (this.$parent && typeof this.$parent.getLastMes === 'function') {
            this.$parent.getLastMes();
          }
        }, 500);
      }finally {
        this.creatingGroup = false;
      }
    },

    triggerAvatarUpload() {
      // this.$refs.avatarInput?.click();
    },

    async handleAvatarUpload(event) {
      const file = event.target.files[0];
      if (!file) return;

      // 验证文件类型
      if (!file.type.startsWith('image/')) {
        this.$message.error('请选择图片文件');
        return;
      }

      // 验证文件大小
      if (file.size > 2 * 1024 * 1024) {
        this.$message.error('图片大小不能超过 2MB');
        return;
      }

      try {
        // 显示预览
        const reader = new FileReader();
        reader.onload = (e) => {
          this.robotForm.avatar = e.target.result; // 预览用base64
        };
        reader.readAsDataURL(file);

        // 存储文件对象用于后续上传
        this.selectedAvatarFile = file;

      } catch (error) {
        console.error('图片预览失败:', error);
        this.$message.error('图片预览失败');
      }
    },

    async createRobot() {
      // 表单验证
      const valid = await this.$refs.robotFormRef.validate();
      if (!valid) return;

      let avatarUrl = '';

      // 如果有选择新头像，先上传图片
      if (this.selectedAvatarFile) {
        const uploadResponse = await uploadFile(this.selectedAvatarFile);
        if (uploadResponse.code !== 200) {
          this.$message.error('头像上传失败');
          return;
        }
        avatarUrl = uploadResponse.data;
      }

      // 提交机器人创建数据
      const robotData = {
        name: this.robotForm.name.trim(),
        avatar: avatarUrl || this.robotForm.avatar,
        description: this.robotForm.description.trim(),
        prompt: this.robotForm.prompt.trim()
      };

      this.creatingRobot = true;
      try{
        const response = await createBot(robotData);
        if (response.code === 200) {
          this.$message.success('机器人创建成功');
          this.closeDialog();
          this.$emit('robot-created', response.data);
          this.resetRobotForm();
        } else {
          this.$message.error(response.msg || '创建机器人失败');
        }
      }finally {
        this.creatingRobot = false;
      }
    },

    resetRobotForm() {
      this.robotForm = {
        name: '',
        avatar: '',
        description: '',
        prompt: ''
      };
    }
  },
  mounted() {
    if (this.initialTab === 'group') {
      this.switchTab('group');
      return;
    }
    this.$nextTick(() => {
      this.$refs.searchInput?.focus();
    });
  }
}
</script>

<style scoped>
/* ===== 添加弹窗 · iOS 设计语言 ===== */
.dialog-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.38);
  -webkit-backdrop-filter: blur(12px);
  backdrop-filter: blur(12px);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
  font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', 'SF Pro Text', 'PingFang SC', 'Helvetica Neue', 'Microsoft YaHei', sans-serif;
  -webkit-font-smoothing: antialiased;
}

/* iOS 弹窗卡片 */
.multi-card-dialog {
  width: 480px;
  max-width: 92vw;
  background: var(--card);
  border-radius: 24px;
  overflow: hidden;
  box-shadow: 0 24px 64px rgba(0, 0, 0, 0.24), 0 4px 12px var(--separator);
  animation: dialog-fade 0.3s cubic-bezier(0.32, 0.72, 0, 1);
}

/* 入场只做位移与缩放，不碰 opacity：动画时钟冻结时弹层依然完整可读 */
@keyframes dialog-fade {
  from {
    transform: translateY(20px) scale(0.97);
  }
  to {
    transform: translateY(0) scale(1);
  }
}

/* 弹窗标题栏 */
.dialog-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
  border-bottom: 1px solid var(--separator);
  height: 58px;
  background: var(--bg-blur);
  -webkit-backdrop-filter: saturate(180%) blur(24px);
  backdrop-filter: saturate(180%) blur(24px);
}

.dialog-tabs {
  display: flex;
  height: 100%;
  gap: 4px;
}

.tab-item {
  padding: 0 14px;
  height: 100%;
  display: flex;
  align-items: center;
  cursor: pointer;
  font-size: 15px;
  font-weight: 500;
  color: var(--label-secondary);
  position: relative;
  transition: all 0.25s cubic-bezier(0.32, 0.72, 0, 1);
}

.tab-item:hover {
  color: var(--label);
}

.tab-item.active {
  color: #007aff;
  font-weight: 600;
}

.tab-item.active::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 14px;
  right: 14px;
  height: 3px;
  background: #007aff;
  border-radius: 3px 3px 0 0;
}

.dialog-close {
  font-size: 22px;
  font-weight: 300;
  cursor: pointer;
  color: var(--label-secondary);
  padding: 0 5px;
  line-height: 1;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: all 0.25s cubic-bezier(0.32, 0.72, 0, 1);
}

.dialog-close:hover {
  background: var(--fill);
  color: var(--label);
}

/* 内容区 */
.card-content {
  padding: 20px;
  min-height: 300px;
  max-height: 500px;
  overflow-y: auto;
  background: var(--bg);
}

/* 搜索区 · iOS */
.search-section {
  display: flex;
  margin-bottom: 16px;
}

.search-section input {
  flex: 1;
  padding: 10px 14px;
  border: none;
  border-radius: 12px;
  background: rgba(118, 118, 128, 0.12);
  outline: none;
  font-size: 14px;
  font-family: inherit;
  color: var(--label);
  transition: all 0.25s cubic-bezier(0.32, 0.72, 0, 1);
}

.search-section input::placeholder {
  color: var(--label-tertiary);
}

.search-section input:focus {
  background: var(--card);
  box-shadow: 0 0 0 2px rgba(0, 122, 255, 0.25);
}

.search-btn {
  margin-left: 12px;
  padding: 0 18px;
  background: #007aff;
  color: white;
  border: none;
  border-radius: 999px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 600;
  box-shadow: 0 2px 8px rgba(0, 122, 255, 0.3);
  transition: all 0.25s cubic-bezier(0.32, 0.72, 0, 1);
}

.search-btn:hover {
  background: #3395ff;
}

/* 搜索结果 · iOS 分组 */
.search-results {
  margin-top: 12px;
  background: var(--card);
  border-radius: 16px;
  box-shadow: 0 1px 3px var(--separator);
  padding: 4px 12px;
  overflow: hidden;
}

.result-item {
  display: flex;
  align-items: center;
  padding: 12px 4px;
  border-bottom: 1px solid var(--separator);
  transition: background-color 0.2s cubic-bezier(0.32, 0.72, 0, 1);
  border-radius: 10px;
}

.result-item:last-child {
  border-bottom: none;
}

.result-item:hover {
  background: var(--fill);
}

.user-avatar {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  background: linear-gradient(145deg, #4da3ff, #007aff);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 12px;
  font-size: 17px;
  font-weight: 600;
  flex-shrink: 0;
  overflow: hidden;
  box-shadow: 0 2px 6px rgba(0, 122, 255, 0.2);
}

.user-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.user-info {
  flex: 1;
  min-width: 0;
}

.name-line {
  display: flex;
  align-items: center;
  margin-bottom: 4px;
}

.user-name {
  font-size: 15px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.86);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-right: 8px;
}

.user-sex {
  margin-right: 8px;
  display: inline-flex;
  align-items: center;
}

.sex-icon {
  width: 14px;
  height: 14px;
  margin-right: 2px;
}

.user-status {
  font-size: 11px;
  display: inline-flex;
  align-items: center;
  padding: 2px 8px;
  border-radius: 999px;
  font-weight: 600;
}

.user-status.online {
  color: #34c759;
  background: rgba(52, 199, 89, 0.12);
}

.user-status.offline {
  color: var(--label-secondary);
  background: var(--fill);
}

.status-icon {
  display: inline-block;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  margin-right: 4px;
  background-color: currentColor;
}

.user-id {
  font-size: 12px;
  color: var(--label-secondary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  text-align: left;
}

.add-btn {
  padding: 7px 18px;
  background: #007aff;
  color: white;
  border: none;
  border-radius: 999px;
  cursor: pointer;
  font-size: 13px;
  font-weight: 600;
  transition: all 0.25s cubic-bezier(0.32, 0.72, 0, 1);
  flex-shrink: 0;
  margin-left: 12px;
  box-shadow: 0 2px 6px rgba(0, 122, 255, 0.25);
}

.add-btn:hover {
  background: #3395ff;
}

.add-btn:disabled {
  background: var(--fill-strong);
  color: rgba(255, 255, 255, 0.7);
  box-shadow: none;
  cursor: not-allowed;
}

/* 空态 */
.no-results,
.search-tips {
  padding: 48px 20px;
  text-align: center;
  color: var(--label-secondary);
  font-size: 14px;
  line-height: 1.6;
  font-weight: 500;
}

/* 表单 · iOS 输入 */
.group-creation-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.group-join-search {
  margin-bottom: 18px;
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.08);
}
.group-join-title { font-size: 14px; font-weight: 600; margin-bottom: 10px; }
.group-join-bar { display: flex; gap: 8px; }
.group-join-input {
  flex: 1;
  height: 36px;
  border-radius: 10px;
  border: 1px solid rgba(0, 0, 0, 0.12);
  padding: 0 12px;
  font-size: 13px;
  outline: none;
  background: rgba(255, 255, 255, 0.72);
}
.group-join-btn {
  height: 36px;
  padding: 0 16px;
  border-radius: 10px;
  border: none;
  background: #111;
  color: #fff;
  font-size: 13px;
  cursor: pointer;
}
.group-join-btn.joined { background: rgba(0, 0, 0, 0.06); color: #888; cursor: default; }
.group-join-results { margin-top: 12px; display: flex; flex-direction: column; gap: 8px; }
.group-join-row { display: flex; align-items: center; gap: 10px; padding: 10px 12px; border-radius: 12px; background: rgba(0, 0, 0, 0.03); }
.group-join-avatar { width: 38px; height: 38px; border-radius: 10px; overflow: hidden; background: #111; color: #fff; display: flex; align-items: center; justify-content: center; font-size: 14px; }
.group-join-avatar img { width: 100%; height: 100%; object-fit: cover; }
.group-join-info { flex: 1; min-width: 0; }
.group-join-name { font-size: 14px; color: #111; font-weight: 600; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.group-join-meta { font-size: 12px; color: #888; margin-top: 2px; }
.group-join-empty { margin-top: 12px; font-size: 12px; color: #999; }

.form-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-item label {
  font-size: 14px;
  color: var(--label);
  font-weight: 600;
}

.form-item input,
.form-item select,
.form-item textarea {
  padding: 10px 14px;
  border: none;
  border-radius: 12px;
  background: var(--card);
  outline: none;
  font-size: 14px;
  font-family: inherit;
  color: var(--label);
  box-shadow: 0 0 0 1px var(--separator-strong);
  transition: box-shadow 0.25s cubic-bezier(0.32, 0.72, 0, 1);
}

.form-item input:focus,
.form-item select:focus,
.form-item textarea:focus {
  box-shadow: 0 0 0 2px rgba(0, 122, 255, 0.3);
}

.form-item textarea {
  resize: vertical;
}

/* 成员选择 */
.member-selection {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 12px;
}

.member-selection label {
  font-size: 14px;
  color: var(--label);
  font-weight: 600;
}

.member-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
  max-height: 200px;
  overflow-y: auto;
  background: var(--card);
  border-radius: 16px;
  padding: 6px 10px;
}

.member-item {
  display: flex;
  align-items: center;
  padding: 8px 10px;
  border-radius: 12px;
  cursor: pointer;
  transition: background-color 0.2s cubic-bezier(0.32, 0.72, 0, 1);
}

.member-item:hover {
  background: var(--fill);
}

.member-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: linear-gradient(145deg, #4da3ff, #007aff);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 12px;
  font-size: 15px;
  font-weight: 600;
  flex-shrink: 0;
  overflow: hidden;
}

.member-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.member-name {
  flex: 1;
  font-size: 14.5px;
  color: var(--label);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.member-checkbox {
  width: 20px;
  height: 20px;
  border: 1.5px solid var(--separator-strong);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s cubic-bezier(0.32, 0.72, 0, 1);
}

.member-checkbox.selected {
  background: #007aff;
  border-color: #007aff;
  box-shadow: 0 2px 6px rgba(0, 122, 255, 0.3);
}

.member-checkbox .check-icon {
  width: 10px;
  height: 6px;
  border-left: 2px solid white;
  border-bottom: 2px solid white;
  transform: rotate(-45deg);
  margin-top: -2px;
}

/* iOS 主按钮 */
.create-group-btn,
.create-robot-btn {
  margin-top: 16px;
  padding: 13px 0;
  background: #007aff;
  color: white;
  border: none;
  border-radius: 999px;
  cursor: pointer;
  font-size: 15.5px;
  font-weight: 600;
  transition: all 0.25s cubic-bezier(0.32, 0.72, 0, 1);
  box-shadow: 0 4px 14px rgba(0, 122, 255, 0.35);
}

.create-group-btn:hover,
.create-robot-btn:hover {
  background: #3395ff;
}

.create-group-btn:disabled,
.create-robot-btn:disabled {
  background: var(--fill-strong);
  box-shadow: none;
  cursor: not-allowed;
}

.create-robot-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 100%;
}

.btn-icon {
  font-size: 18px;
}

/* 机器人表单 */
.robot-creation-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.avatar-upload-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.avatar-upload-label {
  font-size: 14px;
  color: var(--label);
  font-weight: 600;
}

.avatar-upload-area {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 8px;
}

.avatar-preview {
  width: 84px;
  height: 84px;
  border: 2px dashed rgba(0, 122, 255, 0.35);
  border-radius: 18px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  transition: all 0.3s cubic-bezier(0.32, 0.72, 0, 1);
  background: rgba(0, 122, 255, 0.04);
}

.avatar-preview:hover {
  border-color: #007aff;
  background: rgba(0, 122, 255, 0.08);
}

.avatar-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: rgba(0, 122, 255, 0.6);
  font-size: 12px;
}

.upload-icon {
  font-size: 22px;
  margin-bottom: 4px;
}

.avatar-input {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  opacity: 0;
  cursor: pointer;
}

.avatar-tips {
  font-size: 12px;
  color: var(--label-secondary);
}
</style>