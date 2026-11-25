# @xigua/core

PC 端与移动端共享的业务核心。这里只放与框架、平台无关的纯 JavaScript：

- HTTP 客户端与 API 地址
- WebSocket 客户端
- 各业务模块接口
- 消息模型、常量、工具函数

不放入：

- `.vue` 组件、页面、布局、CSS
- `uni.*`、`localStorage`、`window`、`document`
- Element Plus、Vue Router、Pinia 等框架绑定

平台差异通过 `createHttpClient` 的 `transport`、`getToken`、`isSuccess` 和各类 hook 注入；WebSocket 通过 `createSocket`、`send`、`close` 等适配函数注入。
