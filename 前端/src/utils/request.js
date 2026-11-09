// axios 实例：统一的请求出口
//
// 两件事在这里做掉，页面里就不用重复写：
//   ① 请求拦截器：自动往 Headers 里加 Authorization: Bearer <token>
//   ② 响应拦截器：把 { code, msg, data } 拆开
//        code === 200  → 直接返回 data（页面拿到的是业务数据，不是那层壳）
//        code === 401  → 清掉本地登录态，跳登录页
//        其它           → 弹错误提示，Promise 继续 reject

import axios from 'axios'
import { ElMessage } from 'element-plus'
import { getToken, clearAuth } from './token'

const request = axios.create({
  // 统一前缀：开发时 Vite proxy 把 /api 转到 8081，生产时由 Nginx 转发
  baseURL: '/api',
  timeout: 10000
})

// ① 请求拦截器：每个请求自动带 token
request.interceptors.request.use(
  (config) => {
    const token = getToken()
    if (token) {
      // 注意 Bearer 后面有一个空格，后端是 header.startsWith("Bearer ") 判断的
      config.headers.Authorization = 'Bearer ' + token
    }
    return config
  },
  (error) => Promise.reject(error)
)

// ② 响应拦截器：统一处理业务码
request.interceptors.response.use(
  (response) => {
    const body = response.data

    // 防御：后端如果返回了非 JSON（比如网关的 HTML 错误页）
    if (typeof body !== 'object' || body === null || body.code === undefined) {
      ElMessage.error('返回数据格式异常')
      return Promise.reject(new Error('返回数据格式异常'))
    }

    if (body.code === 200) {
      return body.data
    }

    if (body.code === 401) {
      clearAuth()
      ElMessage.error(body.msg || '登录已过期，请重新登录')
      // 已经在登录页就不用再跳，避免死循环
      if (window.location.pathname !== '/login') {
        window.location.href = '/login'
      }
      return Promise.reject(new Error(body.msg || '未登录'))
    }

    ElMessage.error(body.msg || '请求失败')
    return Promise.reject(new Error(body.msg || '请求失败'))
  },
  (error) => {
    // 这里只处理网络层错误（断网、超时、服务没起）
    ElMessage.error('网络异常：' + (error.message || '请求失败'))
    return Promise.reject(error)
  }
)

export default request