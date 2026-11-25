/**
 * PC 请求兼容门面。
 * 实际请求由 src/core/http.js 基于 @xigua/core 的 createHttpClient 完成。
 * 保留原有默认导出与 isRelogin，业务 API 模块无需改动。
 */
import request, { isRelogin } from '@/core/http'

export { isRelogin }
export default request
