// 用户接口
import request from '../utils/request'

/**
 * 当前登录用户资料
 * @returns {Promise<{id:number, username:string, nickname:string,
 *                    avatar:string|null, role:string, createTime:string}>}
 *          avatar 是签名后的地址，可直接当 img src 用
 */
export function getMe() {
  return request.get('/user/me')
}

/**
 * 修改昵称 / 头像
 * @param {{nickname:string, avatar?:string}} data
 *        avatar 传 objectKey（items/... 开头），不传表示这次不换头像
 */
export function updateProfile(data) {
  return request.put('/user/profile', data)
}

/**
 * 修改密码
 * @param {{oldPassword:string, newPassword:string}} data
 */
export function updatePassword(data) {
  return request.put('/user/password', data)
}