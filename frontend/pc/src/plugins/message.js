import { reactive } from 'vue'

/**
 * 全局轻提示（iOS 风格）
 * 替代 element-plus 的 ElMessage —— 后者在 chat 端没有引入样式，
 * 渲染出来的提示节点实际不可见，所有“已撤回/发送失败”类反馈都会丢失。
 *
 * 渲染交给主应用里的 ToastHost（App.vue 内），不再单独 createApp 挂载：
 * 独立挂载的浮层脱离 #app 渲染树，登录页等场景既看不到也截不到。
 * 这里只维护消息队列，ToastHost 消费 toastList。
 */

const state = reactive({ list: [] })
let seq = 0
const MAX = 3

export const toastList = state.list

function norm(opt) {
  if (opt && typeof opt === 'object' && opt.message !== undefined) {
    return { message: opt.message, duration: opt.duration }
  }
  return { message: opt, duration: undefined }
}

function push(input, type = 'info', duration) {
  const { message, duration: d } = norm(input)
  if (message === undefined || message === null || String(message).trim() === '') return
  const id = ++seq
  state.list.push({ id, message: String(message), type })
  if (state.list.length > MAX) state.list.shift()
  const ms = typeof duration === 'number' ? duration : (typeof d === 'number' ? d : (type === 'error' ? 3600 : 2400))
  setTimeout(() => {
    const i = state.list.findIndex(t => t.id === id)
    if (i > -1) state.list.splice(i, 1)
  }, ms)
}

/** 兼容 ElMessage(...) 直接调用 */
const ElMessage = (opt) => {
  const t = (opt && typeof opt === 'object' && opt.type) || 'info'
  push(opt, t)
}
ElMessage.info = (m, d) => push(m, 'info', d)
ElMessage.success = (m, d) => push(m, 'success', d)
ElMessage.warning = (m, d) => push(m, 'warning', d)
ElMessage.error = (m, d) => push(m, 'error', d)
ElMessage.closeAll = () => { state.list.splice(0, state.list.length) }

/** 兼容 ElNotification({title}) 的调用 */
const ElNotification = {
  info: (opt) => push(pickText(opt), 'info'),
  success: (opt) => push(pickText(opt), 'success'),
  warning: (opt) => push(pickText(opt), 'warning'),
  error: (opt) => push(pickText(opt), 'error')
}

function pickText(opt) {
  if (opt && typeof opt === 'object') return opt.title || opt.message || ''
  return opt
}

export function showToast(message, type = 'info', duration) {
  push(message, type, duration)
}

export { ElMessage, ElNotification }
export default ElMessage
