import axios from 'axios'
import { ElNotification, ElMessage } from '@/plugins/message'
import { getToken, removeToken } from '@/utils/auth'
import errorCode from '@/utils/errorCode'
import router from '@/router'
import { createHttpClient } from '@xigua/core'
import { normalizeAssetUrlsDeep } from '@/utils/imageFallback'

export const isRelogin = { show: false }

const TOKEN_KEY = 'xigua-token'
const WHITE_LIST = ['/client/auth/login', '/client/auth/register', '/client/email/send']
const BASE_URL = import.meta.env.VITE_APP_BASE_API || ''
const TIMEOUT = 100000

function tansParams(params) {
  return Object.keys(params)
    .map(key => `${key}=${params[key]}`)
    .join('&')
}

function appendParams(url, params) {
  if (params === undefined || params === null) return url
  const query = typeof params === 'string' ? params : tansParams(params)
  if (!query) return url
  return url + (url.includes('?') ? '&' : '?') + query
}

function isWhiteUrl(url) {
  return WHITE_LIST.some(path => String(url || '').includes(path))
}

// PC 端的成功语义与移动端不同：没有 code 字段的普通对象/数组/Blob 也视为成功。
function isPcSuccess(data) {
  if (data === undefined || data === null) return true
  if (typeof data !== 'object') return true
  return data.code === undefined || data.code === null || data.code === 200 || data.code === 0
}

function getRequestToken(requestOptions) {
  return isWhiteUrl(requestOptions && requestOptions.url) ? '' : getToken()
}

function transport(options) {
  const method = String(options.method || 'GET').toUpperCase()
  const isGet = method === 'GET'
  return axios.request({
    url: options.url,
    method,
    headers: options.header,
    // 共享 core 的 http.get 把参数放在 data 中；axios 的 GET 查询参数必须走 params
    params: isGet ? options.data : undefined,
    data: isGet ? undefined : options.data,
    timeout: options.timeout,
    responseType: options.responseType,
    withCredentials: options.withCredentials,
    onUploadProgress: options.onUploadProgress,
    // 上传取消需要把 AbortController 的 signal 透传给 axios
    signal: options.signal,
    // 与旧 request.js 一致：4xx 走响应判定，5xx 进入 error 回调
    validateStatus: status => status < 500
  }).then(res => ({
    // axios 对 204 无响应体；按旧逻辑仍视为业务成功
    statusCode: res.status === 204 ? 200 : res.status,
    // 后端可能回传旧 IP 的绝对媒体地址；统一改为同源路径，避免 HTTPS 下 Mixed Content
    data: normalizeAssetUrlsDeep(res.data),
    raw: res
  }))
}

function onUnauthorized(message) {
  removeToken(TOKEN_KEY)
  ElMessage.warning({ message: '登录已过期，请重新登录', plain: true })
  router.push('/login')
  return message
}

function onBusinessError(data, requestOptions) {
  const code = data && data.code
  const msg = errorCode[code] || (data && data.msg) || errorCode.default
  const silent = !!(requestOptions && requestOptions.silent)

  if (code === 500) {
    if (!silent) ElMessage({ message: msg, type: 'error' })
  } else if (code === 601) {
    if (!silent) ElMessage({ message: msg, type: 'warning' })
  } else if (!silent) {
    ElNotification.error({ title: msg })
  }
}

function onHttpError(res, requestOptions) {
  const data = res && res.data
  const msg = errorCode[res && res.statusCode] || (data && data.msg) || errorCode.default
  if (!(requestOptions && requestOptions.silent)) {
    ElNotification.error({ title: msg })
  }
}

function onNetworkError(err, requestOptions) {
  console.log('err' + err)
  let message = (err && err.message) || err || '请求失败'
  if (message === 'Network Error') {
    message = '后端接口连接异常'
  } else if (String(message).includes('timeout')) {
    message = '系统接口请求超时'
  } else if (String(message).includes('Request failed with status code')) {
    message = '系统接口' + String(message).substr(String(message).length - 3) + '异常'
  }
  if (!(requestOptions && requestOptions.silent)) {
    ElMessage({ message, type: 'error', duration: 5 * 1000 })
  }
}

const http = createHttpClient({
  baseURL: BASE_URL,
  timeout: TIMEOUT,
  tokenKey: TOKEN_KEY,
  getToken: getRequestToken,
  isSuccess: isPcSuccess,
  transport,
  onUnauthorized,
  onBusinessError,
  onHttpError,
  onNetworkError
})

function normalizeConfig(config) {
  const requestConfig = config || {}
  const method = String(requestConfig.method || 'get').toUpperCase()
  const url = requestConfig.url || ''
  const isWhite = isWhiteUrl(url)
  const token = isWhite ? '' : getToken()

  // 保持旧 request.js 的行为：非白名单且无 token 时直接回登录页
  if (!isWhite && !token) {
    router.push('/login')
    return Promise.reject(new Error('请先登录'))
  }

  const header = Object.assign({}, requestConfig.headers || requestConfig.header || {})
  if (!isWhite && token) header[TOKEN_KEY] = token

  const options = Object.assign({}, requestConfig, {
    url: appendParams(url, requestConfig.params),
    method,
    header,
    data: requestConfig.data || (method === 'POST' ? {} : requestConfig.data)
  })
  delete options.params
  delete options.headers
  return options
}

function request(config) {
  return Promise.resolve(normalizeConfig(config)).then(options => http.request(options))
}

request.get = function (url, params, config) {
  return request(Object.assign({ url, method: 'get', params }, config || {}))
}
request.post = function (url, data, config) {
  return request(Object.assign({ url, method: 'post', data }, config || {}))
}
request.put = function (url, data, config) {
  return request(Object.assign({ url, method: 'put', data }, config || {}))
}
request.del = function (url, data, config) {
  return request(Object.assign({ url, method: 'delete', data }, config || {}))
}

export { http }
export default request
