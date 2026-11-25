<template>
  <div class="todo-page">
    <!-- 顶部导航 -->
    <div class="todo-nav">
      <span class="todo-nav-title">待办事项</span>
      <button class="todo-nav-add" title="新建待办" @click="openAdd">＋</button>
    </div>

    <!-- 未完成 -->
    <div class="todo-section">
      <div class="todo-section-title">未完成 · {{ pendingList.length }}</div>
      <div v-if="pendingList.length === 0" class="todo-empty">暂无待办，点右上角 ＋ 新建</div>
      <div v-for="t in pendingList" :key="t.id" class="todo-card">
        <div class="todo-check" :class="{ 'todo-check--overdue': isOverdue(t) }" @click="onToggle(t)">
          <span class="todo-ring"></span>
        </div>
        <div class="todo-body" @click="openEdit(t)">
          <div class="todo-title">{{ t.title }}</div>
          <div v-if="t.note" class="todo-note">{{ t.note }}</div>
          <div v-if="t.remindAt" class="todo-time" :class="{ 'todo-time--overdue': isOverdue(t) }">
            ⏰ {{ formatRemindAt(t.remindAt) }} <span v-if="isOverdue(t)">已到期</span>
          </div>
        </div>
        <button class="todo-del" title="删除" @click="onRemove(t)">✕</button>
      </div>
    </div>

    <!-- 已完成 -->
    <div v-if="doneList.length" class="todo-section">
      <div class="todo-section-title">已完成 · {{ doneList.length }}</div>
      <div v-for="t in doneList" :key="t.id" class="todo-card todo-card--done">
        <div class="todo-check todo-check--done" @click="onToggle(t)">
          <span>✓</span>
        </div>
        <div class="todo-body">
          <div class="todo-title">{{ t.title }}</div>
          <div v-if="t.remindAt" class="todo-time">{{ formatRemindAt(t.remindAt) }}</div>
        </div>
        <button class="todo-del" title="删除" @click="onRemove(t)">✕</button>
      </div>
    </div>

    <!-- 新增/编辑弹窗（原生浮层，保持 iOS 风格） -->
    <div v-if="showModal" class="todo-mask" @click.self="showModal = false">
      <div class="todo-modal">
        <div class="todo-modal-head">
          <span>{{ editingId ? '编辑待办' : '新建待办' }}</span>
          <button class="todo-modal-close" @click="showModal = false">✕</button>
        </div>
        <div class="todo-form">
          <label class="todo-form-label">标题</label>
          <input class="todo-form-input" v-model="form.title" placeholder="要做什么？" maxlength="50" />
          <label class="todo-form-label">备注（可选）</label>
          <textarea class="todo-form-input todo-form-textarea" v-model="form.note" placeholder="补充说明" rows="2" maxlength="200"></textarea>
          <label class="todo-form-label">提醒时间（可选）</label>
          <input class="todo-form-input" type="datetime-local" v-model="form.remindAtText" />
        </div>
        <div class="todo-form-actions">
          <button class="todo-btn todo-btn-plain" @click="showModal = false">取消</button>
          <button class="todo-btn todo-btn-primary" :disabled="!form.title.trim()" @click="onSave">保存</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ElMessage } from '@/plugins/message'
import {
  loadTodos,
  addTodo,
  updateTodo,
  removeTodo,
  toggleTodo,
  formatRemindAt,
} from '@/utils/todo'

export default {
  name: 'Todo',
  data() {
    return {
      todos: [],
      showModal: false,
      editingId: null,
      form: { title: '', note: '', remindAtText: '' }
    }
  },
  computed: {
    pendingList() {
      return this.todos
        .filter((t) => !t.done)
        .sort((a, b) => (a.remindAt || 0) - (b.remindAt || 0) || b.createdAt - a.createdAt)
    },
    doneList() {
      return this.todos.filter((t) => t.done)
    }
  },
  created() {
    this.refresh()
  },
  methods: {
    // 暴露给模板（import 的函数不会自动挂到 Options API 实例）
    formatRemindAt,
    isOverdue(t) {
      return t.remindAt > 0 && t.remindAt <= Date.now()
    },
    refresh() {
      this.todos = loadTodos()
    },
    openAdd() {
      this.editingId = null
      this.form = { title: '', note: '', remindAtText: '' }
      this.showModal = true
    },
    openEdit(t) {
      this.editingId = t.id
      this.form = {
        title: t.title,
        note: t.note || '',
        remindAtText: t.remindAt ? this.toLocalInput(t.remindAt) : ''
      }
      this.showModal = true
    },
    toLocalInput(ts) {
      const d = new Date(ts)
      const pad = (n) => String(n).padStart(2, '0')
      return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`
    },
    onSave() {
      const remindAt = this.form.remindAtText
        ? new Date(this.form.remindAtText).getTime()
        : 0
      if (this.editingId) {
        updateTodo(this.editingId, {
          title: this.form.title,
          note: this.form.note,
          remindAt
        })
        ElMessage.success('已保存')
      } else {
        addTodo({ title: this.form.title, note: this.form.note, remindAt })
        ElMessage.success('已添加')
      }
      this.showModal = false
      this.refresh()
    },
    onToggle(t) {
      toggleTodo(t.id)
      this.refresh()
    },
    onRemove(t) {
      removeTodo(t.id)
      ElMessage.info('已删除')
      this.refresh()
    }
  }
}
</script>

<style scoped>
.todo-page {
  height: 100%;
  padding: 16px 20px 32px;
  background: var(--bg);
  box-sizing: border-box;
  overflow-y: auto;
}

/* 顶部导航 */
.todo-nav {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  margin-bottom: 16px;
  border-radius: 16px;
  background: var(--bg-blur-strong);
  -webkit-backdrop-filter: saturate(180%) blur(28px);
  backdrop-filter: saturate(180%) blur(28px);
  border: 1px solid var(--separator);
  box-shadow: var(--shadow-sm);
}
.todo-nav-title {
  font-size: 19px;
  font-weight: 700;
  color: var(--label);
}
.todo-nav-add {
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 50%;
  background: var(--accent);
  color: #fff;
  font-size: 20px;
  line-height: 1;
  cursor: pointer;
  transition: transform 0.15s ease;
}
.todo-nav-add:active {
  transform: scale(0.92);
}

/* 分组 */
.todo-section {
  margin-bottom: 20px;
}
.todo-section-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--label-secondary);
  padding: 0 8px 8px;
}
.todo-empty {
  padding: 28px 0;
  text-align: center;
  font-size: 14px;
  color: var(--label-tertiary);
  background: var(--card);
  border-radius: 12px;
  border: 1px solid var(--separator);
}

/* 卡片 */
.todo-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  margin-bottom: 10px;
  border-radius: 12px;
  background: var(--card);
  border: 1px solid var(--separator);
  box-shadow: var(--shadow-sm);
  transition: opacity 0.2s ease;
}
.todo-card--done {
  opacity: 0.55;
}
.todo-check {
  flex: 0 0 auto;
  width: 22px;
  height: 22px;
  cursor: pointer;
}
.todo-ring {
  display: block;
  width: 20px;
  height: 20px;
  border: 2px solid var(--fill-strong);
  border-radius: 50%;
  box-sizing: border-box;
  transition: border-color 0.15s ease;
}
.todo-check--overdue .todo-ring {
  border-color: var(--accent-red);
}
.todo-check--done {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: var(--accent-green);
  color: #fff;
  font-size: 13px;
}
.todo-body {
  flex: 1;
  min-width: 0;
  cursor: pointer;
}
.todo-title {
  font-size: 15px;
  font-weight: 500;
  color: var(--label);
  word-break: break-all;
}
.todo-card--done .todo-title {
  text-decoration: line-through;
  color: var(--label-secondary);
}
.todo-note {
  margin-top: 2px;
  font-size: 13px;
  color: var(--label-secondary);
  word-break: break-all;
}
.todo-time {
  margin-top: 4px;
  font-size: 12px;
  color: var(--label-tertiary);
}
.todo-time--overdue {
  color: var(--accent-red);
}
.todo-del {
  flex: 0 0 auto;
  width: 28px;
  height: 28px;
  border: none;
  border-radius: 50%;
  background: transparent;
  color: var(--label-tertiary);
  font-size: 14px;
  cursor: pointer;
}
.todo-del:hover {
  color: var(--accent-red);
}

/* 弹窗浮层 */
.todo-mask {
  position: fixed;
  inset: 0;
  z-index: 3000;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--overlay);
  backdrop-filter: blur(6px);
  -webkit-backdrop-filter: blur(6px);
}
.todo-modal {
  width: 380px;
  max-width: 86vw;
  padding: 8px 18px 18px;
  border-radius: 22px;
  background: var(--card);
  border: 1px solid var(--separator);
  box-shadow: var(--shadow-lg);
}
.todo-modal-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 2px 4px;
  font-size: 17px;
  font-weight: 700;
  color: var(--label);
}
.todo-modal-close {
  width: 28px;
  height: 28px;
  border: none;
  border-radius: 50%;
  font-size: 14px;
  line-height: 1;
  color: var(--label-secondary);
  background: var(--fill);
  cursor: pointer;
}
.todo-form-label {
  display: block;
  margin: 12px 0 6px;
  font-size: 13px;
  font-weight: 600;
  color: var(--label-secondary);
}
.todo-form-input {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid var(--separator-strong);
  border-radius: 10px;
  background: var(--bg);
  color: var(--label);
  font-size: 14px;
  box-sizing: border-box;
  outline: none;
  transition: border-color 0.15s ease;
}
.todo-form-input:focus {
  border-color: var(--accent);
}
.todo-form-textarea {
  resize: vertical;
  min-height: 52px;
  font-family: inherit;
}
.todo-form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 18px;
}
.todo-btn {
  padding: 8px 16px;
  border: none;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: opacity 0.15s ease;
}
.todo-btn-plain {
  background: var(--fill);
  color: var(--label);
}
.todo-btn-primary {
  background: var(--accent);
  color: #fff;
}
.todo-btn-primary:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
</style>
