// AI 相关接口（都是 Java 透传给 Python 的）
import request from '../utils/request'

/**
 * 根据标题猜类目
 * @param {{title:string, description?:string, topK?:number}} data
 * @returns {Promise<{
 *   title: string,
 *   hit: {categoryId:number, name:string, path:string, score:number} | null,
 *   candidates: Array<{categoryId:number, name:string, path:string, score:number}>,
 *   matched: boolean,
 *   threshold: number
 * }>}
 */
export function recognizeCategory(data) {
  return request.post('/ai/category', data)
}