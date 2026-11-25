/**
 * 待办/日程提醒 数据工具（前端本地存储方案）
 * 存储键：xigua_todos
 * 数据结构：{ id, title, note, remindAt(时间戳ms, 0=不提醒), done, reminded, createdAt }
 */
const STORAGE_KEY = 'xigua_todos'

/** 读取全部待办（按创建时间倒序） */
export function loadTodos() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    const list = raw ? JSON.parse(raw) : []
    return Array.isArray(list) ? list : []
  } catch (e) {
    console.warn('读取待办失败', e)
    return []
  }
}

/** 保存全部待办 */
export function saveTodos(list) {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(list))
  } catch (e) {
    console.warn('保存待办失败', e)
  }
}

/** 新增待办 */
export function addTodo({ title, note = '', remindAt = 0 }) {
  const list = loadTodos()
  const todo = {
    id: Date.now() + Math.random().toString(36).slice(2, 8),
    title: title.trim(),
    note,
    remindAt: Number(remindAt) || 0,
    done: false,
    reminded: false,
    createdAt: Date.now(),
  }
  list.unshift(todo)
  saveTodos(list)
  return todo
}

/** 更新待办 */
export function updateTodo(id, patch) {
  const list = loadTodos()
  const idx = list.findIndex((t) => t.id === id)
  if (idx === -1) return null
  list[idx] = { ...list[idx], ...patch }
  saveTodos(list)
  return list[idx]
}

/** 删除待办 */
export function removeTodo(id) {
  const list = loadTodos()
  const next = list.filter((t) => t.id !== id)
  saveTodos(next)
  return next
}

/** 切换完成状态 */
export function toggleTodo(id) {
  const list = loadTodos()
  const idx = list.findIndex((t) => t.id === id)
  if (idx === -1) return null
  list[idx].done = !list[idx].done
  if (list[idx].done) list[idx].reminded = true // 已完成不再提醒
  saveTodos(list)
  return list[idx]
}

/**
 * 扫描到期待办：返回本次需要提醒的列表，并将它们标记为已提醒
 * @returns {Array} 需要提醒的待办 [{id, title, note, remindAt}]
 */
export function checkTodoReminders() {
  const list = loadTodos()
  const now = Date.now()
  const due = []
  let changed = false
  list.forEach((t) => {
    if (!t.done && !t.reminded && t.remindAt > 0 && t.remindAt <= now) {
      due.push({ id: t.id, title: t.title, note: t.note || '', remindAt: t.remindAt })
      t.reminded = true
      changed = true
    }
  })
  if (changed) saveTodos(list)
  return due
}

/** 格式化提醒时间为本地显示（YYYY-MM-DD HH:mm） */
export function formatRemindAt(ts) {
  if (!ts) return ''
  const d = new Date(ts)
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}
