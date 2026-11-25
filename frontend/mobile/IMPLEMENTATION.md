# IM Messenger - 登录注册页面实现文档

## 概述

基于 Stitch 设计稿,实现了像素级还原的登录和注册页面。

## 已完成的页面

### 1. 登录页面 (pages/login/login.vue)

**功能特性:**
- ✅ 邮箱/用户名输入框
- ✅ 密码输入框,带显示/隐藏切换
- ✅ "忘记密码"链接
- ✅ 主登录按钮,带渐变效果和阴影
- ✅ 社交登录按钮 (Google, Apple, 微信)
- ✅ "立即注册"跳转链接
- ✅ 完整的表单验证
- ✅ 响应式布局

**设计还原要点:**
- 背景渐变: 从白色过渡到淡蓝色 (rgba(50, 171, 253, 0.08))
- 主色调: #32ABFD (亮蓝色)
- 圆角: 输入框 46rpx, 按钮完全圆角 (9999rpx)
- 字体: Manrope (标题), Plus Jakarta Sans (正文)
- 阴影: 按钮带蓝色调阴影 (0 15rpx 46rpx rgba(50, 171, 253, 0.2))
- 间距: 严格按照设计稿的 padding 和 margin

### 2. 注册页面 (pages/register/register.vue)

**功能特性:**
- ✅ 邮箱输入框
- ✅ 验证码输入框,带倒计时功能 (60秒)
- ✅ 密码输入框,带显示/隐藏切换
- ✅ 服务协议和隐私政策复选框
- ✅ 主注册按钮,带渐变效果
- ✅ "去登录"返回链接
- ✅ 完整的表单验证 (邮箱格式、验证码、密码长度等)
- ✅ 响应式布局

**设计还原要点:**
- 与登录页面保持一致的设计语言
- 验证码按钮: 灰色背景 (#f3f4f6), 蓝色文字
- 协议复选框: 自定义样式,圆角 12rpx
- 所有交互元素的 hover 和 active 状态

## 设计系统

### 颜色规范
```scss
$primary: #32ABFD;           // 主色 - 亮蓝色
$on-primary: #ffffff;        // 主色上的文字
$on-surface: #1e1b13;        // 表面文字色
$on-surface-variant: #6f7882; // 次要文字色
$outline-variant: #bec7d3;   // 边框/图标色
$surface-container-high: #F8F9FA; // 输入框背景
$surface-container-highest: #f3f4f6; // 按钮背景
$wechat-green: #07C160;      // 微信绿
```

### 尺寸规范 (rpx)
- 标题字号: 92rpx
- 副标题字号: 27rpx
- 按钮字号: 35rpx
- 小标签字号: 23rpx
- 输入框高度: 108rpx
- 按钮高度: 108rpx
- 圆角 (输入框): 46rpx
- 圆角 (按钮): 9999rpx (完全圆角)
- 页面左右内边距: 62rpx
- 页面顶部内边距: 185rpx

### 字体规范
- **标题**: Manrope, 700 (Bold)
- **正文**: Plus Jakarta Sans, 400/500/600
- **图标**: Material Icons

### 间距规范
- 组件间距: 46rpx
- 表单项间距: 15rpx
- 社交按钮间距: 62rpx
- 分隔线文字间距: 31rpx

## 技术实现细节

### 1. 响应式设计
- 使用 rpx 单位实现多设备适配
- 基准: 750rpx = 375px 物理宽度
- 设计稿 390px 逻辑宽度转换: 1px ≈ 2rpx

### 2. 交互效果
- 按钮点击缩放: `transform: scale(0.98)`
- 社交按钮点击: `transform: scale(0.95)`
- 输入框聚焦: 边框变蓝色,背景变白色
- 密码显示/隐藏切换
- 验证码倒计时 (60秒)

### 3. 表单验证
**登录页:**
- 用户名/邮箱非空验证
- 密码非空验证

**注册页:**
- 邮箱格式验证 (正则: `/^[^\s@]+@[^\s@]+\.[^\s@]+$/`)
- 验证码非空验证
- 密码长度验证 (最少6位)
- 协议勾选验证

### 4. 路由配置
```json
{
  "pages": [
    {
      "path": "pages/login/login",
      "style": {
        "navigationBarTitleText": "登录",
        "navigationStyle": "custom"
      }
    },
    {
      "path": "pages/register/register",
      "style": {
        "navigationBarTitleText": "注册",
        "navigationStyle": "custom"
      }
    }
  ]
}
```

## 待完成事项

### 图标资源
- [ ] 添加 Google 图标 (static/google-icon.png)
  - 当前使用占位符,需替换为实际图标
  - 建议尺寸: 48x48px (@2x) 或 72x72px (@3x)

### 后端集成
- [ ] 实现登录 API 接口
- [ ] 实现注册 API 接口
- [ ] 实现发送验证码 API
- [ ] 实现忘记密码功能
- [ ] 实现社交登录 (Google, Apple, 微信)

### 优化建议
- [ ] 添加输入框防抖
- [ ] 添加加载状态 (loading)
- [ ] 添加错误提示动画
- [ ] 实现自定义字体加载 (Manrope, Plus Jakarta Sans)
- [ ] 添加页面切换动画
- [ ] 实现记住密码功能
- [ ] 添加生物识别登录 (指纹/人脸)

## 使用说明

### 1. 运行项目
```bash
# 在 HBuilderX 中打开项目
# 选择运行 > 运行到浏览器/模拟器/真机

# 或使用 CLI
npm run dev:mp-weixin  # 微信小程序
npm run dev:h5         # H5
npm run dev:app        # App
```

### 2. 页面跳转
```javascript
// 跳转到登录页
uni.navigateTo({
  url: '/pages/login/login'
});

// 跳转到注册页
uni.navigateTo({
  url: '/pages/register/register'
});
```

### 3. 替换 Google 图标
1. 准备 Google 图标 PNG 文件
2. 放置到 `static/google-icon.png`
3. 确保尺寸为 48x48px (@2x) 或更高分辨率

## 设计稿对比

### 登录页面
- 设计稿: stitch-designs/login-page.png
- 实现文件: pages/login/login.vue
- HTML 参考: stitch-designs/login-page.html

### 注册页面
- 设计稿: stitch-designs/register-page.png
- 实现文件: pages/register/register.vue
- HTML 参考: stitch-designs/register-page.html

## 设计系统文档

完整的设计系统说明请参考 Stitch 项目中的 Design MD:
- 创意方向: The Ethereal Curator (空灵策展人)
- 设计风格: Soft Minimalist (柔和极简主义)
- 核心原则:
  - 无边框规则 (通过颜色层次定义空间)
  - 色调分层 (Tonal Layering)
  - 高端编辑排版
  - Glassmorphism (玻璃拟态) 用于浮动元素

## 兼容性

- ✅ 微信小程序
- ✅ H5
- ✅ App (iOS/Android)
- ✅ 支付宝小程序
- ✅ 百度小程序
- ✅ 字节跳动小程序

## 许可证

本项目代码遵循项目根目录的许可证。

## 更新日志

### 2026-04-03
- ✅ 完成登录页面 UI 实现
- ✅ 完成注册页面 UI 实现
- ✅ 实现表单验证逻辑
- ✅ 实现页面路由配置
- ✅ 像素级还原设计稿
- ✅ 添加交互效果和动画
- ✅ 完善文档说明
