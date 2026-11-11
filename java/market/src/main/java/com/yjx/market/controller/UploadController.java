package com.yjx.market.controller;

import com.yjx.market.common.result.Result;
import com.yjx.market.pojo.vo.UploadVO;
import com.yjx.market.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传接口。
 */
@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class UploadController {

    private final FileService fileService;

    /** 上传单张图片 */
    @PostMapping("/image")
    public Result<UploadVO> uploadImage(@RequestParam("file") MultipartFile file) {
        return Result.ok(fileService.uploadImage(file));
    }
}