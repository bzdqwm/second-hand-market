// 认证相关接口
// 每个函数只负责"发请求 + 返回 data"，不做业务判断

import request from '../utils/request'

/**
 * 登录
 * @param {{username: string, password: string}} data
 * @returns {Promise<{token: string, id: number, username: string, nickname: string, avatar: string|null, role: string}>}
 */
export function login(data) {
  return request.post('/auth/login', data)
}

/**
 * 注册
 * @param {{username: string, password: string, nickname?: string}} data
 * @returns {Promise<number>} 新用户 id
 */
export function register(data) {
  return request.post('/auth/register', data)
}

/**
 * 查询当前登录用户资料（靠拦截器自动带的 token）
 * @returns {Promise<{id: number, username: string, nickname: string, avatar: string|null, role: string, createTime: string}>}
 */
export function getProfile() {
  return request.get('/user/me')
}