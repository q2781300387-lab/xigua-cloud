# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 开发命令

- 安装依赖：`npm install`
- 启动本地开发：`npm run dev`
- 构建生产产物：`npm run build`
- 预览生产构建：`npm run preview`

说明：
- Vite 开发服务配置为 `0.0.0.0:5100`。
- 当前 `package.json` 中没有 `lint` 脚本，也没有 `test` 脚本。
- 当前仓库未接入测试框架/配置，因此暂不支持“运行单个测试”。

## 高层架构

本仓库是单体前端应用（非 monorepo），技术栈为 Vue 3 + Vite。

### 应用入口与骨架

- 应用启动入口：`src/main.js`
- 根组件与 WebSocket 初始化：`src/App.vue`
- 路由定义与鉴权守卫：`src/router/index.js`

### 主要页面结构

路由主要分为：
- 认证页面：`/login`、`/register`
- 主壳路由 `/` 下的子页面：`/home`、`/chat`、`/bot`、`/bot/create`、`/profile`、`/friend`、`/ai-chat/:botId`、`/ai-chat/:botId/session/:sessionId`

全局路由守卫通过 `xigua-token` cookie 做登录态拦截。

### 状态管理与本地持久化

- 全局状态使用较轻；Pinia 主要用于语音通话相关 UI 状态：
  - `src/store/voiceCallStore.js`
  - `src/store/voiceWindow.js`
- 本地存储工具集中在 `src/utils/localStorage.js`。

### API 与网络层

- Axios 客户端统一在 `src/utils/request.js`：
  - 基础地址来自 `VITE_APP_BASE_API`
  - 认证请求头使用 `xigua-token`
  - 统一响应码处理与认证白名单路径
- 业务 API 按领域拆分在 `src/api/`（如 auth/chat/contact/group/user/file/call 等）。

### 实时通信（WebSocket）

- WebSocket 单例客户端：`src/ws/WebSocketClient.js`
  - 连接与重连生命周期
  - 心跳机制
  - 监听器订阅/分发模型
- 登录后由根组件初始化 WebSocket（`src/App.vue`）。
- 聊天页（`src/views/ChatWindow.vue`）通过 WebSocket 处理：
  - 消息接收
  - 发送确认（ack）
  - 已读回执
  - 系统消息

### 语音通话链路

- 语音信令通过 WebSocket，消息类型为 `audio_call`。
- 来电提示组件：`src/components/IncomingCallToast.vue`
- 语音窗口（WebRTC 信令与媒体处理）：`src/components/VoiceChatWindow.vue`

## 后端边界与环境变量

本仓库仅包含前端，后端为独立项目：
- 后端仓库：`https://github.com/w3777/xigua-cloud`

关键环境变量：
- `VITE_APP_BASE_API`：HTTP API 地址
- `VITE_WS_BASE_URL`：WebSocket 地址

当前示例配置：
- 开发环境：`127.0.0.1:8090` / `ws://localhost:8090`
- 生产环境：`api.xgchat.cn`

## 来自现有文档的仓库约束

根据 `README.md`：
- 项目定位为学习交流用途，并明确标注禁止商业用途。
- README 中提供了公开演示环境账号，仅用于演示，不应复用于生产场景。

当前仓库未发现以下规则文件：
- `.cursor/rules/`、`.cursorrules`
- `.github/copilot-instructions.md`
