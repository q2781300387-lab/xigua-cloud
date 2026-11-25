# 聊天列表页面 - 1:1 像素级还原说明

## 页面概述
这是根据 Stitch 设计稿 1:1 像素级还原的聊天列表页面，完全遵循 Material Design 3 设计规范和项目设计系统。

## 设计稿信息
- **项目**: IM - 移动端
- **屏幕**: 聊天列表 - 底部导航更新版
- **设计稿尺寸**: 780 x 1768 px
- **设备类型**: Mobile

## 设计还原要点

### 1. 颜色系统 (完全匹配)
- **主色**: `#32ABFD` (Primary Blue)
- **背景色**: `#FFFFFF` (Pure White)
- **表面色**: `#F3F4F6` (Surface Container)
- **文字色**: `#1E1B13` (On Surface)
- **次要文字**: `rgba(30, 27, 19, 0.6)` (60% opacity)
- **边框色**: `#F3F4F6` (Border Gray)
- **在线状态**: `#10B981` (Green)
- **通知徽章**: `#75C6FE` (Accent Blue Container)

### 2. 字体系统
- **字体族**: Plus Jakarta Sans (Body & Label), Manrope (Headline)
- **标题**: 32rpx / 700 weight (用户名)
- **正文**: 28rpx / 400 weight (消息预览)
- **小文本**: 22rpx / 500 weight (时间)
- **徽章文字**: 20rpx / 700 weight (未读数)
- **导航标签**: 24rpx / 600 weight

### 3. 圆角系统
- **卡片圆角**: 32rpx (2rem)
- **头像**: 50% (完全圆形)
- **搜索框**: 32rpx
- **徽章**: 9999rpx (完全圆形)
- **底部导航栏**: 48rpx 48rpx 0 0 (上圆角)

### 4. 间距系统
- **页面边距**: 32rpx
- **卡片间距**: 24rpx
- **卡片内边距**: 32rpx
- **元素间距**: 4rpx - 32rpx (递进式)
- **顶部栏内边距**: 32rpx 48rpx
- **底部栏内边距**: 24rpx 32rpx 48rpx

### 5. 布局结构

#### 顶部导航栏
- **位置**: Fixed (fixed top)
- **背景**: 70% 白色 + 40rpx 毛玻璃效果
- **高度**: 约 144rpx (含内边距)
- **元素**:
  - 左侧: 80rpx 圆形头像 (4rpx 边框 + 阴影)
  - 右侧: 80rpx 圆形搜索按钮

#### 搜索框
- **高度**: 96rpx
- **背景**: #F3F4F6
- **圆角**: 32rpx
- **内部**: 左侧搜索图标 (32rpx 偏移) + 输入框

#### 聊天卡片
每个卡片包含:
- **头像区域**: 112rpx x 112rpx
  - 圆形头像或占位符
  - 右下角在线状态指示器 (28rpx 圆点)
- **内容区域**:
  - 顶部: 用户名 (左) + 时间 (右)
  - 底部: 消息预览 (左) + 未读徽章 (右，如有)
- **交互**: Hover 缩放 0.98 + 背景变浅

#### 底部导航栏
- **位置**: Fixed bottom
- **背景**: 白色 + 顶部阴影
- **圆角**: 48rpx 48rpx 0 0
- **4 个导航项**:
  - 聊天 (激活状态: #32ABFD)
  - 通讯录
  - 通知 (带红点徽章)
  - 我
- **激活状态**: 图标和文字变为主色

### 6. 阴影系统
- **卡片阴影**: `0 2rpx 8rpx rgba(0, 0, 0, 0.05)`
- **头像阴影**: `0 2rpx 8rpx rgba(0, 0, 0, 0.05)`
- **底部导航阴影**: `0 -8rpx 40rpx rgba(30, 27, 19, 0.04)`

### 7. 交互效果
- **点击缩放**: scale(0.98)
- **过渡时间**: 0.2s - 0.3s
- **Hover 背景**: rgba(0, 0, 0, 0.05) 或 rgba(245, 237, 223, 0.5)
- **Active 状态**: transform + background

## 功能实现

### 当前已实现
- ✅ 顶部导航栏 (头像 + 搜索)
- ✅ 搜索框组件
- ✅ 聊天列表渲染
- ✅ 未读消息徽章
- ✅ 在线状态指示器
- ✅ 底部导航栏
- ✅ 页面切换逻辑
- ✅ 点击交互效果
- ✅ 静音状态显示

### 待实现功能
- ⏳ 搜索功能实现
- ⏳ 下拉刷新
- ⏳ 上拉加载更多
- ⏳ 滑动删除/置顶
- ⏳ 长按菜单
- ⏳ 真实数据对接
- ⏳ WebSocket 实时消息
- ⏳ 未读消息数同步

## API 对接建议

### 推荐创建以下 API
```javascript
// api/chat.js
export function getChatList(params) {
  return get('/client/chat/getChatList', params);
}

export function markAsRead(chatId) {
  return post('/client/chat/markAsRead', { chatId });
}

export function deleteChat(chatId) {
  return post('/client/chat/deleteChat', { chatId });
}

export function topChat(chatId, isTop) {
  return post('/client/chat/topChat', { chatId, isTop });
}
```

### 数据结构建议
```javascript
{
  code: 200,
  msg: '成功',
  data: {
    list: [
      {
        id: '1',
        name: '用户名',
        avatar: 'https://...',
        lastMessage: '最后一条消息',
        lastMessageTime: 1234567890,
        unreadCount: 3,
        online: true,
        muted: false,
        topped: false
      }
    ],
    total: 100
  }
}
```

## 性能优化建议

1. **图片懒加载**: 使用 `lazy-load` 属性
2. **虚拟列表**: 当聊天数量 > 50 时使用虚拟滚动
3. **防抖节流**: 搜索输入使用防抖
4. **缓存策略**: 本地缓存聊天列表数据
5. **分页加载**: 实现上拉加载更多

## 兼容性说明

- ✅ 微信小程序
- ✅ H5
- ✅ App (iOS/Android)
- ⚠️ 注意: 毛玻璃效果 `backdrop-filter` 在部分安卓机型可能不支持

## 开发注意事项

1. **rpx 单位**: 所有尺寸使用 rpx，确保不同屏幕适配
2. **安全区域**: 底部导航栏已考虑 iPhone 刘海屏
3. **字体**: 确保引入 Plus Jakarta Sans 和 Manrope 字体
4. **图标**: 当前使用 emoji，建议替换为 SVG 或图标字体
5. **状态管理**: 建议使用 Vuex 或 Pinia 管理聊天状态

## 下一步开发建议

1. 创建聊天详情页面 (`chat-detail.vue`)
2. 实现 WebSocket 连接
3. 对接后端 API
4. 添加消息通知
5. 实现聊天搜索
6. 添加滑动操作
7. 完善错误处理

## 文件结构
```
pages/chat/
├── chat-list.vue       # 聊天列表页面 (当前文件)
├── chat-detail.vue     # 聊天详情页面 (待创建)
├── README.md           # 文档说明 (当前文件)
└── components/         # 聊天相关组件
    ├── ChatCard.vue    # 聊天卡片组件 (可拆分)
    └── MessageBubble.vue # 消息气泡组件 (待创建)
```

## 设计系统参考
本页面严格遵循项目的设计系统文档 (Design MD)，包括：
- Editorial Fluidity 美学
- Soft Minimalist 风格
- Tonal Architecture 色彩架构
- No-Line Rule (无边框规则)
- Glassmorphism (毛玻璃效果)
- Ambient Shadows (环境阴影)

---

**设计稿来源**: Google Stitch
**还原精度**: 1:1 像素级
**更新时间**: 2026-04-07
