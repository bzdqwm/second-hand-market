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

/**
 * 商品分页搜索（游客可调用，不需要 token）
 * @param {{page?:number, size?:number, categoryId?:number|null,
 *          keyword?:string, minPrice?:number|null, maxPrice?:number|null,
 *          sort?:'latest'|'price_asc'|'price_desc'}} params
 * @returns {Promise<{records:Array, total:number, page:number, size:number, pages:number}>}
 */
export function getItemList(params) {
  // 空值不发给后端：传 categoryId= 空串会被 Spring 当成 null 之外的怪值，容易出意外
  const clean = {}
  Object.entries(params || {}).forEach(([k, v]) => {
    if (v !== null && v !== undefined && v !== '') clean[k] = v
  })
  return request.get('/item/list', { params: clean })
}

/**
 * 商品详情（游客可调用）
 * @param {number|string} id
 * @returns {Promise<{id:number, title:string, description:string, price:number,
 *                    categoryId:number, categoryName:string, status:string,
 *                    createTime:string, images:Array<{url:string, thumbUrl:string}>,
 *                    seller:{id:number, nickname:string, avatar:string|null, createTime:string},
 *                    owner:boolean}>}
 */
export function getItemDetail(id) {
  return request.get(`/item/${id}`)
}