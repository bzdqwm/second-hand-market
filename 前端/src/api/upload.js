// 文件上传接口
import request from '../utils/request'

/**
 * 上传单张图片
 * @param {File} file 浏览器选中的文件对象
 * @param {(percent:number)=>void} [onProgress] 进度回调 0~100
 * @returns {Promise<{objectKey:string, url:string, thumbUrl:string, width:number, height:number}>}
 */
export function uploadImage(file, onProgress) {
  const form = new FormData()
  // 字段名必须是 file，与后端 @RequestParam("file") 对应
  form.append('file', file)

  return request.post('/upload/image', form, {
    // 上传比普通请求慢，单独放宽超时（全局是 10 秒）
    timeout: 60000,
    onUploadProgress: (e) => {
      if (onProgress && e.total) {
        onProgress(Math.round((e.loaded * 100) / e.total))
      }
    }
  })
  // ⚠️ 不要手动设置 Content-Type！
  // 浏览器会自动带上 multipart/form-data; boundary=----xxx
  // 手动设置会把 boundary 弄丢，后端解析直接失败
}