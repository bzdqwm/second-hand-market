package com.yjx.market.service.impl;

import com.yjx.market.common.exception.BusinessException;
import com.yjx.market.common.oss.OssTemplate;
import com.yjx.market.common.result.ResultCode;
import com.yjx.market.common.util.ImageUtil;
import com.yjx.market.pojo.vo.UploadVO;
import com.yjx.market.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    /** 允许的类型 */
    private static final Set<String> ALLOWED_TYPES =
            Set.of("image/jpeg", "image/jpg", "image/png");

    /** 上传原文件大小上限：10MB */
    private static final long MAX_SIZE = 10L * 1024 * 1024;

    /** 压缩后长边上限 */
    private static final int MAX_EDGE = 1920;

    private final OssTemplate ossTemplate;

    @Override
    public UploadVO uploadImage(MultipartFile file) {
        // ① 基础校验
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "请选择要上传的图片");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "图片不能超过 10MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType.toLowerCase())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "只支持 JPG / PNG 格式");
        }

        try {
            // ② 压缩：长边 ≤1920，质量 0.85
            byte[] compressed = ImageUtil.compressToJpg(file.getInputStream(), MAX_EDGE, 0.85f);
            // ③ 缩略图：长边 400
            byte[] thumb = ImageUtil.thumbnail(compressed);

            // ④ 生成 objectKey：items/年/月/日/uuid.jpg
            //    按日期分目录避免单目录文件过多；UUID 避免重名和猜解
            String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            String uuid = UUID.randomUUID().toString().replace("-", "");
            String objectKey = "items/" + datePath + "/" + uuid + ".jpg";
            String thumbKey = "items/" + datePath + "/" + uuid + "_thumb.jpg";

            // ⑤ 上传：先传缩略图，成功再传原图（任一失败都不留脏数据到返回值）
            ossTemplate.upload(thumb, thumbKey, "image/jpeg");
            ossTemplate.upload(compressed, objectKey, "image/jpeg");

            // ⑥ 读尺寸（用于前端布局占位，避免图片加载时页面抖动）
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(compressed));

            // ⑦ 返回：原图地址带水印，缩略图不带
            return UploadVO.builder()
                    .objectKey(objectKey)
                    .url(ossTemplate.signedUrl(objectKey, true))
                    .thumbUrl(ossTemplate.signedUrl(thumbKey, false))
                    .width(img == null ? null : img.getWidth())
                    .height(img == null ? null : img.getHeight())
                    .build();

        } catch (IOException e) {
            log.error("图片处理失败", e);
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "图片处理失败，请换一张试试");
        }
    }
}