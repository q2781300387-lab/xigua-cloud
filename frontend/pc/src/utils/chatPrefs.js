// 会话级偏好：置顶与免打扰
// 存储结构：{ "<chatId>": { pinned: true, muted: true } }
// 按用户隔离，避免同机多账号互相影响

const KEY_PREFIX = 'chatPrefs_'

function storageKey() {
  let uid = ''
  try {
    const raw = localStorage.getItem('userInfo')
    if (raw) uid = (JSON.parse(raw) || {}).id || ''
  } catch (e) { uid = '' }
  return KEY_PREFIX + (uid || 'anon')
}

export function loadChatPrefs() {
  try {
    const raw = localStorage.getItem(storageKey())
    const obj = raw ? JSON.parse(raw) : {}
    return obj && typeof obj === 'object' ? obj : {}
  } catch (e) {
    return {}
  }
}

export function saveChatPrefs(prefs) {
  try {
    localStorage.setItem(storageKey(), JSON.stringify(prefs || {}))
  } catch (e) { /* 存储失败时保持内存态，不影响本次会话 */ }
}

export function isPinned(prefs, chatId) {
  return !!(prefs && prefs[chatId] && prefs[chatId].pinned)
}

export function isMuted(prefs, chatId) {
  return !!(prefs && prefs[chatId] && prefs[chatId].muted)
}

export function togglePref(prefs, chatId, field) {
  const next = { ...(prefs || {}) }
  const cur = { ...(next[chatId] || {}) }
  cur[field] = !cur[field]
  if (!cur.pinned && !cur.muted) {
    delete next[chatId]
  } else {
    next[chatId] = cur
  }
  saveChatPrefs(next)
  return next
}

// 置顶排前，其余保持后端返回的顺序
export function sortByPinned(list, prefs) {
  if (!Array.isArray(list)) return []
  const pinned = []
  const rest = []
  list.forEach((item) => {
    if (isPinned(prefs, item.chatId)) pinned.push(item)
    else rest.push(item)
  })
  return pinned.concat(rest)
}
