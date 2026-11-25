<template>
  <div class="privacy-page">
    <div class="page-head">
      <button class="back-btn" title="返回" @click="goBack">←</button>
      <div class="head-title">
        <h1>隐私与安全</h1>
        <p>PC 与手机共用同一套偏好，保存后立即同步</p>
      </div>
      <span class="head-badge">P2</span>
    </div>

    <div class="privacy-grid">
      <section class="column">
        <div class="card">
          <div class="card-title">消息通知</div>
          <div class="row" @click="toggle('notificationPreview')">
            <div class="row-main">
              <span class="row-label">通知显示消息内容</span>
              <span class="row-desc">关闭后锁屏通知只提示“收到一条新消息”</span>
            </div>
            <span class="ios-switch" :class="{ on: settings.notificationPreview === 1 }"><span class="knob" /></span>
          </div>
          <div class="row" @click="toggle('notificationSound')">
            <div class="row-main">
              <span class="row-label">新消息提示音</span>
              <span class="row-desc">收到消息时播放提示音</span>
            </div>
            <span class="ios-switch" :class="{ on: settings.notificationSound === 1 }"><span class="knob" /></span>
          </div>
        </div>

        <div class="card">
          <div class="card-title">隐私</div>
          <div class="row" @click="toggle('showOnlineStatus')">
            <div class="row-main">
              <span class="row-label">展示在线状态</span>
              <span class="row-desc">关闭后好友看不到你的在线/离线状态</span>
            </div>
            <span class="ios-switch" :class="{ on: settings.showOnlineStatus === 1 }"><span class="knob" /></span>
          </div>
          <div class="row" @click="toggle('showReadReceipt')">
            <div class="row-main">
              <span class="row-label">发送已读回执</span>
              <span class="row-desc">关闭后对方不会看到“已读”</span>
            </div>
            <span class="ios-switch" :class="{ on: settings.showReadReceipt === 1 }"><span class="knob" /></span>
          </div>
          <div class="row" @click="toggle('allowPhoneSearch')">
            <div class="row-main">
              <span class="row-label">允许通过手机号找到我</span>
              <span class="row-desc">关闭后不能通过手机号搜索添加</span>
            </div>
            <span class="ios-switch" :class="{ on: settings.allowPhoneSearch === 1 }"><span class="knob" /></span>
          </div>
          <div class="row" @click="toggle('friendVerify')">
            <div class="row-main">
              <span class="row-label">加好友需要验证</span>
              <span class="row-desc">关闭后任何人可直接添加你为好友</span>
            </div>
            <span class="ios-switch" :class="{ on: settings.friendVerify === 1 }"><span class="knob" /></span>
          </div>
        </div>

        <div class="card">
          <div class="card-title">黑名单</div>
          <div v-if="!blocks.length" class="empty">暂无被屏蔽的用户</div>
          <div v-for="item in blocks" :key="item.id" class="row block-row">
            <span class="avatar">{{ (item.name || '?').slice(0, 1) }}</span>
            <div class="row-main">
              <span class="row-label">{{ item.name || item.id }}</span>
              <span class="row-desc">已屏蔽消息</span>
            </div>
            <button class="mini-btn" @click="removeBlock(item)">移出</button>
          </div>
          <div class="block-add">
            <select v-model="blockTarget" class="field">
              <option value="" disabled>选择要屏蔽的好友</option>
              <option v-for="friend in friends" :key="friend.id" :value="friend.id">{{ friend.name }}</option>
            </select>
            <button class="mini-btn" :disabled="!blockTarget" @click="addBlock">拉黑</button>
          </div>
        </div>
      </section>

      <section class="column">
        <div class="card">
          <div class="card-title">登录设备</div>
          <div v-for="(device, index) in devices" :key="device.id || index" class="device-row">
            <span class="device-icon">{{ device.platform === 'App' ? '📱' : '💻' }}</span>
            <div class="row-main">
              <div class="device-title">
                <span class="row-label">{{ device.region || device.ip || '未知位置' }}</span>
                <span v-if="device.current" class="current-tag">当前设备</span>
              </div>
              <span class="row-desc">{{ friendlyUa(device.userAgent) }} · {{ device.lastLoginTime }}</span>
            </div>
          </div>
          <div v-if="!devices.length" class="empty">暂无设备记录</div>
          <button class="secondary-btn" @click="kickOthers">下线其它所有设备</button>
        </div>

        <div class="card">
          <div class="card-title">安全与数据</div>
          <div class="report-box">
            <label class="field-label">举报违规用户</label>
            <select v-model="reportForm.targetId" class="field">
              <option value="" disabled>选择好友</option>
              <option v-for="friend in friends" :key="friend.id" :value="friend.id">{{ friend.name }}</option>
            </select>
            <select v-model="reportForm.reason" class="field">
              <option v-for="reason in reasons" :key="reason" :value="reason">{{ reason }}</option>
            </select>
            <button class="primary-btn" :disabled="!reportForm.targetId" @click="submitReport">提交举报</button>
          </div>

          <button class="row action-row" @click="exportData">
            <div class="row-main">
              <span class="row-label">导出个人数据</span>
              <span class="row-desc">下载账号资料、联系人、群与最近消息</span>
            </div>
            <span class="chevron">›</span>
          </button>

          <button class="row action-row danger-row" @click="openDeactivate">
            <div class="row-main">
              <span class="row-label danger-text">注销账号</span>
              <span class="row-desc">注销后所有设备下线，且无法登录</span>
            </div>
            <span class="chevron">›</span>
          </button>
        </div>
      </section>
    </div>

    <div v-if="deactivateVisible" class="modal-mask" @click.self="deactivateVisible = false">
      <div class="modal-card">
        <h3>注销账号</h3>
        <p>此操作会立即下线所有设备并停用账号。请输入登录密码，并输入“注销账号”确认。</p>
        <input v-model="deactivateForm.password" class="field" type="password" placeholder="登录密码" />
        <input v-model="deactivateForm.confirmText" class="field" placeholder="输入：注销账号" />
        <button class="danger-btn" :disabled="deactivateBusy" @click="deactivate">
          {{ deactivateBusy ? '处理中…' : '确认注销' }}
        </button>
        <button class="cancel-btn" @click="deactivateVisible = false">取消</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { privacy } from '@/api/privacy'
import { getFriendList } from '@/api/contact'
import { ElMessage } from '@/plugins/message'
import { removeToken } from '@/utils/auth.js'

const router = useRouter()
const settings = reactive({
  notificationPreview: 1,
  notificationSound: 1,
  showOnlineStatus: 1,
  showReadReceipt: 1,
  allowPhoneSearch: 1,
  friendVerify: 1,
})
const blocks = ref([])
const blockTarget = ref('')
const devices = ref([])
const friends = ref([])
const reasons = ['骚扰辱骂', '虚假身份', '垃圾广告', '色情低俗', '诈骗欺诈']
const reportForm = reactive({ targetId: '', reason: reasons[0] })
const deactivateVisible = ref(false)
const deactivateBusy = ref(false)
const deactivateForm = reactive({ password: '', confirmText: '' })

function unwrap(res) {
  return res && res.data !== undefined ? res.data : res
}

async function loadSettings() {
  const data = unwrap(await privacy.getSettings())
  if (data) Object.assign(settings, data)
}

async function loadBlocks() {
  blocks.value = unwrap(await privacy.getBlocks()) || []
}

async function loadDevices() {
  devices.value = unwrap(await privacy.getDevices()) || []
}

async function loadFriends() {
  const data = unwrap(await getFriendList()) || []
  const flat = Array.isArray(data) ? data.flatMap(item => item.list || [item]) : []
  friends.value = flat.filter(item => item && (item.id || item.userId)).map(item => ({
    id: item.id || item.userId,
    name: item.username || item.name || item.friendName || '用户',
  }))
}

async function toggle(key) {
  const previous = settings[key]
  settings[key] = previous === 1 ? 0 : 1
  try {
    const data = unwrap(await privacy.saveSettings({ ...settings }))
    if (data) Object.assign(settings, data)
    ElMessage.success('已保存')
  } catch (e) {
    settings[key] = previous
    ElMessage.error('保存失败，请重试')
  }
}

async function addBlock() {
  if (!blockTarget.value) return
  try {
    await privacy.block(blockTarget.value)
    blockTarget.value = ''
    await loadBlocks()
    ElMessage.success('已加入黑名单')
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

function removeBlock(item) {
  if (!window.confirm(`确定不再屏蔽「${item.name || item.id}」吗？`)) return
  privacy.unblock(item.id).then(() => {
    blocks.value = blocks.value.filter(block => block.id !== item.id)
    ElMessage.success('已移出黑名单')
  }).catch(() => ElMessage.error('操作失败'))
}

async function kickOthers() {
  if (!window.confirm('其它设备上的登录会立即失效，当前设备不受影响。')) return
  try {
    const count = unwrap(await privacy.kickOthers()) || 0
    ElMessage.success(count > 0 ? `已下线 ${count} 台设备` : '没有其它在线设备')
    await loadDevices()
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

async function submitReport() {
  const friend = friends.value.find(item => String(item.id) === String(reportForm.targetId))
  try {
    await privacy.report({
      targetId: reportForm.targetId,
      targetType: 'user',
      reason: reportForm.reason,
      detail: `举报用户：${friend ? friend.name : reportForm.targetId}`,
    })
    ElMessage.success('举报已提交')
  } catch (e) {
    ElMessage.error('举报失败')
  }
}

async function exportData() {
  try {
    const res = await privacy.exportData()
    const payload = JSON.stringify(unwrap(res) || res, null, 2)
    const blob = new Blob([payload], { type: 'application/json' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `xigua-data-${Date.now()}.json`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    setTimeout(() => URL.revokeObjectURL(url), 3000)
    ElMessage.success('已开始下载')
  } catch (e) {
    ElMessage.error('导出失败')
  }
}

function openDeactivate() {
  deactivateForm.password = ''
  deactivateForm.confirmText = ''
  deactivateVisible.value = true
}

async function deactivate() {
  if (!deactivateForm.password) {
    ElMessage.warning('请输入登录密码')
    return
  }
  if (deactivateForm.confirmText.trim() !== '注销账号') {
    ElMessage.warning('请输入“注销账号”确认')
    return
  }
  deactivateBusy.value = true
  try {
    await privacy.deactivate({ ...deactivateForm })
    removeToken('xigua-token')
    localStorage.removeItem('userInfo')
    localStorage.removeItem('currentChatWindow')
    deactivateVisible.value = false
    ElMessage.success('账号已注销')
    setTimeout(() => router.replace('/login'), 1000)
  } catch (e) {
    ElMessage.error('注销失败')
  } finally {
    deactivateBusy.value = false
  }
}

function friendlyUa(ua) {
  if (!ua) return '未知设备'
  if (/iPhone|iPad|iOS/i.test(ua)) return 'iOS 设备'
  if (/Android/i.test(ua)) return 'Android 设备'
  if (/Macintosh/i.test(ua)) return 'Mac'
  if (/Windows/i.test(ua)) return 'Windows'
  return ua.slice(0, 28)
}

function goBack() {
  router.back()
}

onMounted(() => {
  Promise.allSettled([loadSettings(), loadBlocks(), loadDevices(), loadFriends()])
    .then(results => {
      const failed = results.filter(item => item.status === 'rejected')
      if (failed.length) ElMessage.warning('部分数据加载失败，请刷新重试')
    })
})
</script>

<style scoped>
.privacy-page {
  min-height: 100%;
  box-sizing: border-box;
  padding: 24px 28px 42px;
  background: var(--bg, #f2f2f7);
  color: var(--label, #1c1c1e);
  font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Text', 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

.page-head {
  display: flex;
  align-items: center;
  gap: 14px;
  max-width: 1080px;
  margin: 0 auto 18px;
}

.back-btn {
  width: 38px;
  height: 38px;
  border: 1px solid var(--separator, rgba(60, 60, 67, 0.12));
  border-radius: 50%;
  background: var(--card, #fff);
  color: var(--label, #1c1c1e);
  font-size: 18px;
  cursor: pointer;
}

.head-title h1 {
  margin: 0;
  font-size: 24px;
  line-height: 1.2;
  font-weight: 700;
}

.head-title p {
  margin: 5px 0 0;
  font-size: 12.5px;
  color: var(--secondary-label, rgba(60, 60, 67, 0.55));
}

.head-badge {
  margin-left: auto;
  padding: 4px 10px;
  border-radius: 999px;
  background: var(--fill, rgba(120, 120, 128, 0.12));
  color: var(--secondary-label, rgba(60, 60, 67, 0.55));
  font-size: 11px;
  letter-spacing: 0.08em;
}

.privacy-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.08fr) minmax(330px, 0.92fr);
  gap: 18px;
  max-width: 1080px;
  margin: 0 auto;
  align-items: start;
}

.column {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.card {
  overflow: hidden;
  background: var(--card, #fff);
  border: 1px solid var(--separator, rgba(60, 60, 67, 0.12));
  border-radius: 18px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.03);
  backdrop-filter: blur(20px);
}

.card-title {
  padding: 18px 20px 8px;
  color: var(--secondary-label, rgba(60, 60, 67, 0.5));
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.row {
  display: flex;
  align-items: center;
  gap: 14px;
  width: 100%;
  min-height: 66px;
  box-sizing: border-box;
  padding: 13px 20px;
  border: 0;
  border-top: 1px solid var(--separator, rgba(60, 60, 67, 0.1));
  background: transparent;
  color: inherit;
  text-align: left;
  cursor: pointer;
}

.row:first-of-type {
  border-top: 0;
}

.row:hover {
  background: var(--fill, rgba(120, 120, 128, 0.06));
}

.row-main {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.row-label {
  font-size: 14px;
  font-weight: 600;
}

.row-desc {
  color: var(--secondary-label, rgba(60, 60, 67, 0.5));
  font-size: 11.5px;
  line-height: 1.45;
}

.ios-switch {
  position: relative;
  flex: 0 0 auto;
  width: 48px;
  height: 29px;
  border-radius: 999px;
  background: rgba(120, 120, 128, 0.22);
  transition: background 0.22s ease;
}

.ios-switch.on {
  background: #111;
}

.knob {
  position: absolute;
  top: 3px;
  left: 3px;
  width: 23px;
  height: 23px;
  border-radius: 50%;
  background: #fff;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.18);
  transition: transform 0.22s cubic-bezier(0.32, 0.72, 0, 1);
}

.ios-switch.on .knob {
  transform: translateX(19px);
}

.block-row {
  cursor: default;
}

.block-add {
  display: flex;
  gap: 10px;
  align-items: center;
  margin-top: 10px;
}

.block-add .field {
  flex: 1;
  margin: 0;
}

.avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: var(--fill, #f2f2f7);
  font-size: 13px;
  font-weight: 700;
}

.mini-btn {
  padding: 6px 13px;
  border: 0;
  border-radius: 999px;
  background: #111;
  color: #fff;
  font-size: 11.5px;
  cursor: pointer;
}

.device-row {
  display: flex;
  gap: 12px;
  align-items: center;
  padding: 12px 20px;
  border-top: 1px solid var(--separator, rgba(60, 60, 67, 0.1));
}

.device-row:first-of-type {
  border-top: 0;
}

.device-icon {
  width: 28px;
  font-size: 19px;
  text-align: center;
}

.device-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.current-tag {
  padding: 2px 7px;
  border: 1px solid rgba(17, 17, 17, 0.24);
  border-radius: 999px;
  font-size: 9px;
}

.secondary-btn {
  width: calc(100% - 40px);
  margin: 10px 20px 18px;
  height: 38px;
  border: 0;
  border-radius: 11px;
  background: var(--fill, #f2f2f7);
  color: var(--label, #1c1c1e);
  font-size: 13px;
  cursor: pointer;
}

.report-box {
  display: grid;
  gap: 9px;
  padding: 12px 20px 16px;
}

.field-label {
  color: var(--secondary-label, rgba(60, 60, 67, 0.55));
  font-size: 11px;
  font-weight: 600;
}

.field {
  width: 100%;
  height: 36px;
  box-sizing: border-box;
  padding: 0 10px;
  border: 1px solid var(--separator, rgba(60, 60, 67, 0.14));
  border-radius: 9px;
  background: var(--fill, #f7f7fa);
  color: var(--label, #1c1c1e);
  font-size: 12.5px;
}

.primary-btn {
  height: 36px;
  border: 0;
  border-radius: 9px;
  background: #111;
  color: #fff;
  font-size: 12.5px;
  font-weight: 600;
  cursor: pointer;
}

.primary-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.action-row {
  border-top: 1px solid var(--separator, rgba(60, 60, 67, 0.1));
}

.chevron {
  color: var(--secondary-label, rgba(60, 60, 67, 0.28));
  font-size: 20px;
}

.danger-text {
  color: #d92d20;
}

.danger-row:hover {
  background: rgba(217, 45, 32, 0.05);
}

.empty {
  padding: 14px 20px 20px;
  color: var(--secondary-label, rgba(60, 60, 67, 0.45));
  font-size: 12px;
}

.modal-mask {
  position: fixed;
  inset: 0;
  z-index: 300;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: rgba(0, 0, 0, 0.38);
  backdrop-filter: blur(8px);
}

.modal-card {
  width: 400px;
  max-width: 100%;
  box-sizing: border-box;
  padding: 24px;
  border-radius: 18px;
  background: var(--card, #fff);
  box-shadow: 0 18px 60px rgba(0, 0, 0, 0.2);
}

.modal-card h3 {
  margin: 0;
  font-size: 17px;
}

.modal-card p {
  margin: 8px 0 16px;
  color: var(--secondary-label, rgba(60, 60, 67, 0.55));
  font-size: 12px;
  line-height: 1.55;
}

.modal-card .field {
  margin-top: 8px;
}

.danger-btn,
.cancel-btn {
  width: 100%;
  height: 38px;
  margin-top: 12px;
  border: 0;
  border-radius: 10px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}

.danger-btn {
  background: #d92d20;
  color: #fff;
}

.cancel-btn {
  background: var(--fill, #f2f2f7);
  color: var(--label, #1c1c1e);
}

@media (max-width: 860px) {
  .privacy-grid {
    grid-template-columns: 1fr;
  }

  .privacy-page {
    padding: 18px 16px 32px;
  }
}
</style>
