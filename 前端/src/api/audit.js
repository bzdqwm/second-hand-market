// 管理端审核台接口
import request from '../utils/request'

/**
 * 待审列表
 * @param {{page?:number, size?:number}} params
 * @returns {Promise<{records:Array, total:number, page:number, size:number, pages:number}>}
 */
export function getAuditList(params) {
  return request.get('/admin/audit/list', { params })
}

/**
 * 审核详情（商品图文 + 最近一次机审结论）
 * @param {number} itemId
 */
export function getAuditDetail(itemId) {
  return request.get(`/admin/audit/${itemId}`)
}

/**
 * 处置：通过 → 上架，驳回 → 驳回态
 * @param {number} itemId
 * @param {{result:'PASS'|'REJECT', reason?:string}} data 驳回时 reason 必填
 */
export function handleAudit(itemId, data) {
  return request.put(`/admin/audit/${itemId}`, data)
}