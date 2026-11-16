// 类目接口
import request from '../utils/request'

/**
 * 查询全部类目（扁平列表，含 parentId）
 * @returns {Promise<Array<{id:number, parentId:number, name:string, sort:number}>>}
 */
export function getCategoryList() {
  return request.get('/category/list')
}