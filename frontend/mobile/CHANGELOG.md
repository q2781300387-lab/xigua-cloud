# 更新日志

## 2026-04-07

### 🔄 路由结构优化

#### 统一路由命名风格
为保持项目路由风格一致，所有底部导航栏相关页面统一使用 `-list` 后缀：

**修改前：**
```
/pages/chat/chat-list          ✅ (已是标准格式)
/pages/contacts/contacts        ❌ (不一致)
/pages/notifications/notifications  ❌ (不一致，且文件夹名太长)
/pages/profile/profile          ✅ (个人中心保持原样)
```

**修改后：**
```
/pages/chat/chat-list          ✅ 聊天列表
/pages/contacts/contact-list   ✅ 通讯录列表
/pages/notice/notice-list      ✅ 通知列表
/pages/profile/profile         ✅ 个人中心
```

### 📁 文件结构变更

#### 重命名文件夹
- `pages/notifications/` → `pages/notice/`

#### 重命名文件
- `pages/contacts/contacts.vue` → `pages/contacts/contact-list.vue`
- `pages/notifications/notifications.vue` → `pages/notice/notice-list.vue`

### 🔧 代码修改

#### 1. BottomNav 组件 (`components/BottomNav.vue`)

**navItems 配置更新：**
```javascript
// 修改前
{
  key: 'notifications',
  label: '通知',
  path: '/pages/notifications/notifications',
  badge: true
}

// 修改后
{
  key: 'notice',
  label: '通知',
  path: '/pages/notice/notice-list',
  badge: true
}
```

**SVG 图标条件判断更新：**
```vue
<!-- 修改前 -->
<svg v-else-if="item.key === 'notifications'" ...>

<!-- 修改后 -->
<svg v-else-if="item.key === 'notice'" ...>
```

#### 2. 页面 currentPath 更新

**contact-list.vue:**
```javascript
// 修改前
currentPath: '/pages/contacts/contacts'

// 修改后
currentPath: '/pages/contacts/contact-list'
```

**notice-list.vue:**
```javascript
// 修改前
currentPath: '/pages/notifications/notifications'

// 修改后
currentPath: '/pages/notice/notice-list'
```

**CSS 类名更新:**
```scss
/* 修改前 */
.notifications-container { ... }

/* 修改后 */
.notice-container { ... }
```

#### 3. 路由配置 (`pages.json`)

```json
// 修改前
{
  "path": "pages/contacts/contacts",
  "style": { "navigationBarTitleText": "通讯录" }
},
{
  "path": "pages/notifications/notifications",
  "style": { "navigationBarTitleText": "通知" }
}

// 修改后
{
  "path": "pages/contacts/contact-list",
  "style": { "navigationBarTitleText": "通讯录" }
},
{
  "path": "pages/notice/notice-list",
  "style": { "navigationBarTitleText": "通知" }
}
```

### ✨ 优化效果

1. **命名一致性**
   - 所有列表页面统一使用 `-list` 后缀
   - 文件夹名称更简洁（notice vs notifications）

2. **代码可读性**
   - 路由路径更清晰，一目了然
   - key 值更简短（notice vs notifications）

3. **维护便利性**
   - 遵循统一的命名规范
   - 便于后续添加新页面

### 📊 完整路由对照表

| 功能 | Key | 路由路径 | 文件路径 |
|------|-----|---------|---------|
| 聊天 | chat | /pages/chat/chat-list | pages/chat/chat-list.vue |
| 通讯录 | contacts | /pages/contacts/contact-list | pages/contacts/contact-list.vue |
| 通知 | notice | /pages/notice/notice-list | pages/notice/notice-list.vue |
| 个人中心 | profile | /pages/profile/profile | pages/profile/profile.vue |

### 🎯 命名规范

今后新增页面请遵循以下规范：

#### 列表页面
- **文件夹名**: 复数或功能名（如 `chat`、`notice`）
- **文件名**: `功能-list.vue` （如 `chat-list.vue`）
- **路由路径**: `/pages/功能/功能-list` （如 `/pages/chat/chat-list`）

#### 详情页面
- **文件名**: `功能-detail.vue` （如 `chat-detail.vue`）
- **路由路径**: `/pages/功能/功能-detail` （如 `/pages/chat/chat-detail`）

#### 表单页面
- **文件名**: `功能-form.vue` 或 `功能-edit.vue`
- **路由路径**: `/pages/功能/功能-form` 或 `/pages/功能/功能-edit`

### ⚠️ 注意事项

1. **清除缓存**
   - 修改路由后，建议重新编译项目
   - 如使用微信开发者工具，建议清除缓存并重新编译

2. **兼容性**
   - 所有旧的路由路径已失效
   - 确保代码中没有硬编码的旧路径引用

3. **测试验证**
   - 测试底部导航栏所有 Tab 的跳转功能
   - 验证页面激活状态是否正确显示

---

**版本**: v1.0.1
**更新时间**: 2026-04-07
**影响范围**: 路由系统、底部导航栏组件、通讯录页面、通知页面
