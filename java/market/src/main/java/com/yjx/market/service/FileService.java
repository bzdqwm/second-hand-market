package com.yjx.market.service;

import com.yjx.market.pojo.vo.UploadVO;
import org.springframework.web.multipart.MultipartFile;

public interface FileService {

    /**
     * 上传单张图片：校验 → 压缩 → 生成缩略图 → 传私有桶 → 返回签名地址
     *
     * @param file multipart 文件，表单字段名必须是 file
     * @return 对象路径 + 原图签名地址（带水印）+ 缩略图签名地址
     */
    UploadVO uploadImage(MultipartFile file);
}