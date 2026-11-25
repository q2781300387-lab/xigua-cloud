<template>
  <div class="group-right">
    <div class="group-detail">
      <div class="detail-close" @click="closeDetail">
        <i class="el-icon-close"> × </i>
      </div>
      <div class="detail-header">
        <img :src="groupDetail.groupAvatar" alt="群头像" class="detail-avatar">
        <div class="detail-name">
          {{ groupDetail.groupName }}
          <span class="detail-status">
            {{ groupDetail.currentMember }}人
          </span>
        </div>
        <div class="detail-signature">
          {{ groupDetail.description || '这个群很懒，什么都没写' }}
        </div>
      </div>

      <div class="detail-section">
        <div class="section-title-row">
          <span class="section-title">群成员（{{ members.length }}）</span>
          <button v-if="canManage" class="mini-btn invite" @click="openInvite">＋ 邀请</button>
        </div>
        <div class="member-grid">
          <div
            v-for="m in members"
            :key="m.userId"
            class="member-cell"
            :class="{ clickable: isActionable(m) }"
            @click="openMemberAction(m)"
          >
            <div class="member-avatar-wrap">
              <img v-if="m.avatar" :src="m.avatar" class="member-avatar" alt="成员头像">
              <div v-else class="member-avatar member-avatar-fallback">
                {{ m.username ? m.username.charAt(0) : '?' }}
              </div>
              <span v-if="m.owner" class="member-badge owner">群主</span>
              <span v-else-if="m.role === 2" class="member-badge admin">管理</span>
            </div>
            <div class="member-name">{{ m.username }}</div>
          </div>
          <div v-if="!members.length" class="member-empty">成员加载中…</div>
        </div>
      </div>

      <div class="detail-section">
        <div class="section-title">基本信息</div>
        <div class="detail-rows">
          <div class="detail-row">
            <span class="row-label">群主：</span>
            <span class="row-value">{{ groupDetail.groupOwner || '未知' }}</span>
          </div>
          <div class="detail-row">
            <span class="row-label">地区：</span>
            <span class="row-value">{{ groupDetail.region || '未知' }}</span>
          </div>
          <div class="detail-row">
            <span class="row-label">创建时间：</span>
            <span class="row-value">{{ groupDetail.createTime }}</span>
          </div>
          <div class="detail-row">
            <span class="row-label">群号：</span>
            <span class="row-value">{{ 'xg_' + groupDetail.groupId || '未知' }}</span>
          </div>
        </div>
      </div>

      <div class="detail-section">
        <div class="section-title">社交信息</div>
        <div class="detail-row">
          <span class="row-label">加入时间：</span>
          <span class="row-value">{{ displayJoinTime }}</span>
        </div>
        <div class="detail-row">
          <span class="row-label">我的角色：</span>
          <span class="row-value">{{ groupDetail.roleName }}</span>
        </div>
      </div>

      <div class="detail-actions">
        <div class="action-btn-container">
          <button class="action-btn chat-btn" @click="startGroupChat(groupDetail.groupId)">
            <i class="icon-message"></i> 发送消息
          </button>
        </div>
        <div class="action-btn-container">
          <button class="action-btn quit-btn" @click="quitGroup()" v-if="groupDetail.role !== 1">
            <i class="icon-delete"></i> 退出群聊
          </button>
          <button class="action-btn delete-btn" @click="disbandGroup()" v-if="groupDetail.role === 1">
            <i class="icon-delete"></i> 解散群聊
          </button>
        </div>
      </div>
    </div>

    <!-- 邀请成员 -->
    <div v-if="showInvite" class="sheet-mask" @click.self="showInvite = false">
      <div class="sheet-panel">
        <div class="sheet-title">邀请好友入群</div>
        <div class="sheet-subtitle">已选 {{ selectedIds.length }} 人 · 入群后会收到一条系统提示</div>
        <div class="invite-list">
          <label v-for="f in invitableFriends" :key="f.userId" class="invite-item">
            <input type="checkbox" :value="f.userId" v-model="selectedIds">
            <img v-if="f.avatar" :src="f.avatar" class="invite-avatar" alt="好友头像">
            <div v-else class="invite-avatar invite-avatar-fallback">
              {{ f.username ? f.username.charAt(0) : '?' }}
            </div>
            <span class="invite-name">{{ f.username }}</span>
            <span class="invite-check">{{ selectedIds.includes(f.userId) ? '✓' : '' }}</span>
          </label>
          <div v-if="!invitableFriends.length" class="invite-empty">
            没有可邀请的好友（好友已全部在群内或还没有好友）
          </div>
        </div>
        <div class="sheet-footer">
          <button class="action-btn ghost-btn" @click="showInvite = false">取消</button>
          <button
            class="action-btn chat-btn"
            :disabled="!selectedIds.length || inviting"
            @click="doInvite"
          >
            {{ inviting ? '邀请中…' : '邀请' + (selectedIds.length ? '(' + selectedIds.length + ')' : '') }}
          </button>
        </div>
      </div>
    </div>

    <!-- 成员操作 -->
    <div v-if="showMemberAction && actionMember" class="sheet-mask" @click.self="closeMemberAction">
      <div class="sheet-panel member-panel">
        <template v-if="!confirmText">
          <div class="sheet-title">
            {{ actionMember.username }}
            <span class="sheet-role">{{ actionMember.roleName }}</span>
          </div>
          <button
            v-if="canSetAdmin && actionMember.role === 3"
            class="sheet-action"
            @click="doSetRole(2)"
          >设为管理员</button>
          <button
            v-if="canSetAdmin && actionMember.role === 2"
            class="sheet-action"
            @click="doSetRole(3)"
          >取消管理员</button>
          <button
            v-if="canKick"
            class="sheet-action danger"
            @click="askKick"
          >移出群聊</button>
          <button class="sheet-action plain" @click="closeMemberAction">取消</button>
        </template>
        <template v-else>
          <div class="sheet-title">{{ confirmTitle }}</div>
          <div class="confirm-hint">{{ confirmText }}</div>
          <button
            class="sheet-action danger"
            :disabled="confirmLoading"
            @click="runConfirm"
          >{{ confirmLoading ? '正在移出…' : confirmOkText }}</button>
          <button class="sheet-action plain" @click="cancelConfirm">取消</button>
        </template>
      </div>
    </div>
  </div>
</template>

<script>
import {
  getGroupDetail,
  inviteMembers,
  kickMember,
  setMemberRole,
  quitGroup,
  disbandGroup
} from '@/api/group.js'
// 好友列表统一走 contact 接口（friendRelation 里没有 getFriendList）
import { getFriendList } from '@/api/contact.js'
import { set } from '@/utils/localStorage.js'

export default {
  data() {
    return {
      groupDetail: {
        groupId: '',
        groupName: '',
        groupAvatar: '',
        description: '',
        groupOwner: '',
        currentMember: 0,
        region: '',
        createTime: '',
        joinTime: '',
        role: null,
        roleName: ''
      },
      members: [],
      // 邀请
      showInvite: false,
      friends: [],
      selectedIds: [],
      inviting: false,
      // 成员操作
      showMemberAction: false,
      actionMember: null,
      confirmTitle: '',
      confirmText: '',
      confirmOkText: '确定',
      confirmLoading: false
    }
  },
  computed: {
    displayJoinTime() {
      // 入群时间来自 Redis 入群时间戳；缓存异常时会落到 1970，这里兜底显示「—」
      const t = this.groupDetail && this.groupDetail.joinTime
      if (!t || String(t).startsWith('1970')) {
        return '—'
      }
      return t
    },
    canManage() {
      // 群主(1) 与管理员(2) 可以邀请
      return this.groupDetail.role === 1 || this.groupDetail.role === 2
    },
    myUserId() {
      const info = localStorage.getItem('userInfo')
      try {
        return JSON.parse(info).id
      } catch (e) {
        return null
      }
    },
    invitableFriends() {
      const joined = this.members.map(m => m.userId)
      return (this.friends || []).filter(f => f && f.userId && !joined.includes(f.userId))
    },
    canSetAdmin() {
      // 仅群主可以设/取消管理员，且目标不是自己
      return this.groupDetail.role === 1 &&
        this.actionMember &&
        this.actionMember.userId !== this.myUserId &&
        !this.actionMember.owner
    },
    canKick() {
      if (!this.actionMember || this.actionMember.userId === this.myUserId) {
        return false
      }
      if (this.groupDetail.role === 1) {
        return !this.actionMember.owner
      }
      if (this.groupDetail.role === 2) {
        return this.actionMember.role === 3
      }
      return false
    }
  },
  created() {
  },
  methods: {
    startGroupChat(groupId) {
      set('activeMenu', 'chat');
      this.$router.push({
        path: 'Chat',
        query: {
          "groupId": groupId
        }
      })
    },

    getGroupDetail(groupId) {
      this.members = []
      this.showInvite = false
      this.closeMemberAction()
      getGroupDetail(groupId).then(response => {
        if (response.code === 200) {
          this.groupDetail = response.data || {}
          this.members = (response.data && response.data.members) || []
        } else {
          this.groupDetail = null;
          this.members = []
          this.$message.error('获取群组详情失败')
        }
      }).catch(() => {
        this.$message.error('获取群组详情失败')
      })
    },

    // ===== 邀请成员 =====
    async openInvite() {
      this.selectedIds = []
      this.showInvite = true
      try {
        const res = await getFriendList()
        if (res.code === 200) {
          this.friends = res.data || []
        } else {
          this.friends = []
        }
      } catch (e) {
        this.friends = []
        this.$message.error('好友列表加载失败')
      }
    },

    async doInvite() {
      if (!this.selectedIds.length || this.inviting) {
        return
      }
      this.inviting = true
      try {
        const res = await inviteMembers(this.groupDetail.groupId, this.selectedIds)
        if (res.code === 200) {
          this.$message.success(`已邀请 ${res.data} 人加入群聊`)
          this.showInvite = false
          this.selectedIds = []
          this.getGroupDetail(this.groupDetail.groupId)
          // 左侧会话/群列表的人数与预览也要跟着变，否则会停在旧值（如「5人」）
          this.$emit('refresh')
        } else {
          this.$message.error(res.msg || '邀请失败')
        }
      } catch (e) {
        this.$message.error((e && e.message) || '邀请失败')
      } finally {
        this.inviting = false
      }
    },

    // ===== 成员操作 =====
    isActionable(m) {
      return m && m.userId !== this.myUserId && this.canManage
    },

    openMemberAction(m) {
      if (!this.isActionable(m)) {
        return
      }
      this.actionMember = m
      this.showMemberAction = true
    },

    closeMemberAction() {
      this.showMemberAction = false
      this.actionMember = null
      this.cancelConfirm()
    },

    async doSetRole(role) {
      const target = this.actionMember
      if (!target) {
        return
      }
      try {
        const res = await setMemberRole(this.groupDetail.groupId, target.userId, role)
        if (res.code === 200) {
          this.$message.success(role === 2 ? '已设为管理员' : '已取消管理员')
          this.closeMemberAction()
          this.getGroupDetail(this.groupDetail.groupId)
        } else {
          this.$message.error(res.msg || '设置角色失败')
        }
      } catch (e) {
        this.$message.error((e && e.message) || '设置角色失败')
      }
    },

    // 移出成员：在同层操作面板内切到确认态（iOS ActionSheet 的破坏性操作惯例），
    // 不依赖全屏 $confirm 遮罩，确保在任何主题/尺寸下都看得见、点得到。
    askKick() {
      const target = this.actionMember
      if (!target) {
        return
      }
      this.confirmTitle = `移出 ${target.username}`
      this.confirmText = '移出后 TA 将不再收到本群的新消息，其他成员会看到一条系统提示。'
      this.confirmOkText = '确定移出'
      this.confirmLoading = false
    },

    cancelConfirm() {
      this.confirmTitle = ''
      this.confirmText = ''
      this.confirmLoading = false
    },

    async runConfirm() {
      const target = this.actionMember
      if (!target || this.confirmLoading) {
        return
      }
      this.confirmLoading = true
      try {
        const res = await kickMember(this.groupDetail.groupId, target.userId)
        if (res.code === 200) {
          this.$message.success(`已把 ${target.username} 移出群聊`)
          this.confirmLoading = false
          this.cancelConfirm()
          this.closeMemberAction()
          this.getGroupDetail(this.groupDetail.groupId)
          // 左侧列表的「N人」需要同步刷新
          this.$emit('refresh')
        } else {
          this.$message.error(res.msg || '移出失败')
          this.confirmLoading = false
        }
      } catch (e) {
        this.$message.error((e && e.message) || '移出失败')
        this.confirmLoading = false
      }
    },

    // ===== 退群 / 解散（真实接口）=====
    quitGroup() {
      this.$confirm(`确定要退出群聊 ${this.groupDetail.groupName} 吗?`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          const res = await quitGroup(this.groupDetail.groupId)
          if (res.code === 200) {
            this.$message.success('已退出群聊')
            this.$emit('refresh')
            this.closeDetail()
          } else {
            this.$message.error(res.msg || '退出群聊失败')
          }
        } catch (e) {
          this.$message.error((e && e.message) || '退出群聊失败')
        }
      }).catch(() => {})
    },

    disbandGroup() {
      this.$confirm(`确定要解散群聊 ${this.groupDetail.groupName} 吗?`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          const res = await disbandGroup(this.groupDetail.groupId)
          if (res.code === 200) {
            this.$message.success('群聊已解散')
            this.$emit('refresh')
            this.closeDetail()
          } else {
            this.$message.error(res.msg || '解散群聊失败')
          }
        } catch (e) {
          this.$message.error((e && e.message) || '解散群聊失败')
        }
      }).catch(() => {})
    },

    closeDetail() {
      this.$emit('close');
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

.section-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 8px 8px;
}

.section-title-row .section-title {
  padding: 0;
}

.mini-btn {
  border: none;
  border-radius: 999px;
  padding: 5px 12px;
  font-size: 12.5px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.25s cubic-bezier(0.32, 0.72, 0, 1);
  background: rgba(0, 122, 255, 0.1);
  color: #007aff;
}

.mini-btn:hover {
  background: rgba(0, 122, 255, 0.18);
  transform: translateY(-1px);
}

/* 成员网格 */
.member-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 14px 10px;
  padding: 16px 14px;
  background: var(--card);
  border-radius: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}

.member-cell {
  width: 62px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}

.member-cell.clickable {
  cursor: pointer;
}

.member-cell.clickable:hover .member-avatar-wrap {
  transform: translateY(-2px);
}

.member-avatar-wrap {
  position: relative;
  width: 44px;
  height: 44px;
  transition: transform 0.25s cubic-bezier(0.32, 0.72, 0, 1);
}

.member-avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid #ffffff;
  box-shadow: 0 3px 10px rgba(0, 0, 0, 0.12);
}

.member-avatar-fallback {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: 600;
  color: #ffffff;
  background: linear-gradient(135deg, #5ac8fa, #007aff);
}

.member-badge {
  position: absolute;
  left: 50%;
  bottom: -7px;
  transform: translateX(-50%);
  padding: 1px 6px;
  border-radius: 999px;
  font-size: 10px;
  font-weight: 700;
  line-height: 14px;
  color: #ffffff;
  white-space: nowrap;
}

.member-badge.owner {
  background: linear-gradient(135deg, #ff9f0a, #ff6b00);
}

.member-badge.admin {
  background: linear-gradient(135deg, #5ac8fa, #007aff);
}

.member-name {
  max-width: 62px;
  font-size: 12px;
  color: var(--label-secondary);
  text-align: center;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.member-empty {
  width: 100%;
  text-align: center;
  font-size: 13px;
  color: var(--label-tertiary);
  padding: 8px 0;
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

.chat-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
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

/* ===== 底部弹层（邀请 / 成员操作）· iOS ActionSheet ===== */
.sheet-mask {
  position: fixed;
  inset: 0;
  z-index: 2100;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  /* iOS ActionSheet 的遮罩只做压暗、不做模糊：
     模糊统一交给弹层自己做（backdrop-filter），
     这样既省一次全屏滤镜开销，也避免旧渲染器把弹层内容一起吞掉 */
  background: rgba(0, 0, 0, 0.45);
}

.sheet-panel {
  width: 100%;
  max-width: 520px;
  margin: 0 auto;
  padding: 20px 20px 16px;
  box-sizing: border-box;
  /* 玻璃主题下 var(--card) 会退化成 2.9% 白的近乎透明底，
     弹层文字就浮在模糊背景上完全看不清，这里固定给一层可读的实底玻璃 */
  background: rgba(255, 255, 255, 0.86);
  -webkit-backdrop-filter: blur(30px) saturate(180%);
  backdrop-filter: blur(30px) saturate(180%);
  border-radius: 20px 20px 0 0;
  box-shadow: 0 -8px 30px rgba(0, 0, 0, 0.18);
  animation: sheet-up 0.28s cubic-bezier(0.32, 0.72, 0, 1);
}

html[data-theme="dark"] .sheet-panel {
  background: rgba(28, 28, 30, 0.88);
}

@keyframes sheet-up {
  from { transform: translateY(40px); opacity: 0; }
  to { transform: translateY(0); opacity: 1; }
}

.sheet-title {
  font-size: 17px;
  font-weight: 700;
  color: var(--label);
  text-align: center;
  margin-bottom: 6px;
}

.confirm-hint {
  font-size: 13px;
  line-height: 1.5;
  color: var(--label-secondary);
  text-align: center;
  padding: 2px 8px 10px;
}

.sheet-role {
  font-size: 11px;
  font-weight: 600;
  color: var(--label-secondary);
  margin-left: 6px;
}

.sheet-subtitle {
  font-size: 12.5px;
  color: var(--label-secondary);
  text-align: center;
  margin-bottom: 12px;
}

.invite-list {
  max-height: 320px;
  overflow-y: auto;
  margin-bottom: 12px;
}

.invite-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 4px;
  border-bottom: 1px solid var(--separator);
  cursor: pointer;
}

.invite-item:last-child {
  border-bottom: none;
}

.invite-item input {
  width: 18px;
  height: 18px;
  accent-color: #007aff;
  cursor: pointer;
}

.invite-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  object-fit: cover;
}

.invite-avatar-fallback {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #ffffff;
  font-size: 14px;
  font-weight: 600;
  background: linear-gradient(135deg, #5ac8fa, #007aff);
}

.invite-name {
  flex: 1;
  font-size: 14.5px;
  color: var(--label);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.invite-check {
  color: #007aff;
  font-size: 15px;
  font-weight: 700;
}

.invite-empty {
  text-align: center;
  font-size: 13px;
  color: var(--label-tertiary);
  padding: 28px 0;
}

.sheet-footer {
  display: flex;
  gap: 12px;
}

.sheet-footer .action-btn {
  flex: 1;
  min-width: 0;
}

.ghost-btn {
  background: var(--fill);
  color: var(--label);
}

.member-panel {
  padding-bottom: 22px;
}

.sheet-action {
  width: 100%;
  padding: 15px 0;
  border: none;
  background: transparent;
  border-bottom: 1px solid var(--separator);
  font-size: 15px;
  font-weight: 600;
  color: #007aff;
  cursor: pointer;
}

.sheet-action:last-child {
  border-bottom: none;
}

.sheet-action.danger {
  color: #ff3b30;
}

.sheet-action.plain {
  color: var(--label-secondary);
  font-weight: 500;
}
</style>
