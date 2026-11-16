// 商品接口
import request from '../utils/request'

/**
 * 发布商品
 * @param {{title:string, description?:string, categoryId:number, price:number, images:string[]}} data
 *        images 传的是 objectKey 数组，不是签名地址
 * @returns {Promise<number>} 新商品 id
 */
export function createItem(data) {
  return request.post('/item', data)
}

/**
 * 编辑商品（只能改自己的，且在售/已售不能改）
 * @param {number} id
 * @param {{title:string, description?:string, categoryId:number, price:number, images:string[]}} data
 */
export function updateItem(id, data) {
  return request.put(`/item/${id}`, data)
}

/**
 * 上架 / 下架
 * @param {number} id
 * @param {'ON_SALE'|'OFF_SALE'} status
 */
export function changeItemStatus(id, status) {
  return request.put(`/item/${id}/status`, { status })
}