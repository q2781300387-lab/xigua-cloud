import { createCore } from '@xigua/core'
import { http } from './http'

// PC 端共享业务 API 单例：请求层由 src/core/http.js 注入 axios
export const core = createCore(http)

export { http }
export default core
