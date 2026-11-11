package com.yjx.market.common.util;

import net.coobird.thumbnailator.Thumbnails;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 图片压缩工具：统一转成 JPG，控制长边和体积。
 */
public final class ImageUtil {

    private ImageUtil() {
    }

    /**
     * 压缩图片，返回 JPG 字节数组。
     *
     * @param input   原始图片流
     * @param maxEdge 长边上限（像素），小于这个值不放大
     * @param quality 质量 0~1（0.85 肉眼几乎无损，体积能降 60%+）
     */
    public static byte[] compressToJpg(InputStream input, int maxEdge, float quality) throws IOException {
        BufferedImage src = ImageIO.read(input);
        if (src == null) {
            throw new IOException("无法解析图片，可能不是有效的图片文件");
        }

        // PNG 可能带透明通道，直接转 JPG 会变黑底，先铺白底
        BufferedImage canvas = new BufferedImage(
                src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = canvas.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, src.getWidth(), src.getHeight());
        g.drawImage(src, 0, 0, null);
        g.dispose();

        int w = canvas.getWidth();
        int h = canvas.getHeight();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        if (w <= maxEdge && h <= maxEdge) {
            // 已经够小，只重新编码降体积，不放大（放大只会变糊）
            Thumbnails.of(canvas)
                    .scale(1.0)
                    .outputQuality(quality)
                    .outputFormat("jpg")
                    .toOutputStream(out);
        } else {
            Thumbnails.of(canvas)
                    .size(maxEdge, maxEdge)   // 等比缩放到长边不超过 maxEdge
                    .outputQuality(quality)
                    .outputFormat("jpg")
                    .toOutputStream(out);
        }
        return out.toByteArray();
    }

    /** 生成缩略图（长边 400） */
    public static byte[] thumbnail(byte[] jpgBytes) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Thumbnails.of(new ByteArrayInputStream(jpgBytes))
                .size(400, 400)
                .outputQuality(0.8f)
                .outputFormat("jpg")
                .toOutputStream(out);
        return out.toByteArray();
    }
}