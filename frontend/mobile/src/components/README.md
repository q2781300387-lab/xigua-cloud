# 公共组件文档

## BottomNav - 底部导航栏组件

### 概述
可复用的底部导航栏组件，支持路由跳转和激活状态管理。使用 SVG 图标（从 Stitch 设计稿提取），完全遵循设计系统规范。

### 使用方法

#### 1. 导入组件
```vue
<script>
import BottomNav from '@/components/BottomNav.vue';

export default {
  components: {
    BottomNav
  }
}
</script>
```

#### 2. 在模板中使用
```vue
<template>
  <view>
    <!-- 页面内容 -->

    <!-- 底部导航栏 -->
    <BottomNav :current="currentPath" @change="handleNavChange" />
  </view>
</template>
```

#### 3. 组件逻辑
```javascript
export default {
  data() {
    return {
      currentPath: '/pages/chat/chat-list' // 当前页面路径
    };
  },
  methods: {
    handleNavChange(path) {
      console.log('Navigation changed to:', path);
      this.currentPath = path;
    }
  }
}
```

### Props

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| current | String | '/pages/chat/chat-list' | 当前激活的页面路径 |

### Events

| 事件名 | 参数 | 说明 |
|--------|------|------|
| change | (path: String) | 导航切换时触发，返回目标页面路径 |

### 导航配置

组件内置 4 个导航项：

| Key | Label | Path | Badge | Icon |
|-----|-------|------|-------|------|
| chat | 聊天 | /pages/chat/chat-list | false | 聊天气泡 SVG |
| contacts | 通讯录 | /pages/contacts/contacts | false | 用户组 SVG |
| notifications | 通知 | /pages/notifications/notifications | true | 铃铛 SVG (带红点) |
| profile | 我 | /pages/profile/profile | false | 用户 SVG |

### 图标来源

所有 SVG 图标均从 Stitch 设计稿中提取，确保视觉一致性：
- **聊天图标**: 消息气泡形状
- **通讯录图标**: 多用户图标
- **通知图标**: 铃铛图标（带红点徽章）
- **个人图标**: 单用户图标

### 样式特性

#### 1. 激活状态
- **图标颜色**: 从 `rgba(30, 27, 19, 0.4)` 变为 `#32ABFD`
- **文字颜色**: 从 `rgba(30, 27, 19, 0.4)` 变为 `#32ABFD`
- **过渡动画**: 0.3s 平滑过渡

#### 2. 交互效果
- **Hover 状态**: 背景色 `rgba(245, 237, 223, 0.5)`
- **点击反馈**: 实时切换激活状态

#### 3. 徽章系统
- **位置**: 图标右上角
- **颜色**: `#75C6FE` (Accent Blue Container)
- **尺寸**: 16rpx 圆点
- **边框**: 4rpx 白色边框

### 路由跳转方式

组件使用 `uni.redirectTo` 进行页面跳转，不保留历史记录，避免页面栈过深。

```javascript
uni.redirectTo({
  url: item.path,
  fail: (err) => {
    console.error('页面跳转失败:', err);
  }
});
```

### 设计规范

完全遵循项目设计系统：
- **圆角**: 48rpx (顶部圆角)
- **内边距**: 24rpx 32rpx 48rpx (上 左右 下)
- **阴影**: `0 -8rpx 40rpx rgba(30, 27, 19, 0.04)`
- **字体**: Plus Jakarta Sans
- **字号**: 24rpx / 600 weight
- **图标尺寸**: 48rpx x 48rpx

### 兼容性

- ✅ 微信小程序
- ✅ H5
- ✅ App (iOS/Android)
- ✅ 支持自定义导航栏模式

### 自定义修改

#### 1. 修改导航项

编辑 `components/BottomNav.vue` 中的 `navItems` 数组：

```javascript
navItems: [
  {
    key: 'home',
    label: '首页',
    path: '/pages/home/home',
    badge: false
  },
  // ... 更多导航项
]
```

#### 2. 添加新图标

在模板中添加对应的 SVG 图标：

```vue
<svg v-else-if="item.key === 'home'" class="nav-icon" ...>
  <!-- SVG 路径 -->
</svg>
```

#### 3. 修改样式

直接修改 `<style>` 中的样式变量：

```scss
.nav-label-active {
  color: #your-color; // 修改激活颜色
}
```

### 注意事项

1. **路径必须与 pages.json 中配置的路径完全一致**
2. **所有导航目标页面必须存在，否则会跳转失败**
3. **每个页面都需要传入正确的 `current` prop**
4. **SVG 图标的 `stroke` 属性用于控制颜色，不是 `fill`**

### 完整示例

```vue
<template>
  <view class="page-container">
    <!-- 页面内容 -->
    <view class="content">
      <text>页面内容</text>
    </view>

    <!-- 底部导航栏 -->
    <BottomNav :current="currentPath" @change="handleNavChange" />
  </view>
</template>

<script>
import BottomNav from '@/components/BottomNav.vue';

export default {
  components: {
    BottomNav
  },
  data() {
    return {
      currentPath: '/pages/chat/chat-list'
    };
  },
  methods: {
    handleNavChange(path) {
      this.currentPath = path;
      console.log('导航到:', path);
    }
  }
};
</script>

<style lang="scss" scoped>
.page-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
}

.content {
  flex: 1;
  padding-bottom: 200rpx; // 给底部导航栏留空间
}
</style>
```

---

## 开发进度

- ✅ 底部导航栏组件 (BottomNav)
- ⏳ 聊天卡片组件 (ChatCard)
- ⏳ 消息气泡组件 (MessageBubble)
- ⏳ 头像组件 (Avatar)
- ⏳ 搜索框组件 (SearchBar)

---

**更新时间**: 2026-04-07
