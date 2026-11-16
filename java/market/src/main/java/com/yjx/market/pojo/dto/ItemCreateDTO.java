package com.yjx.market.pojo.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 发布商品的入参。
 * <p>
 * 只收「用户能填」的字段。seller_id、status、version、create_time
 * 一律由后端决定，绝不从请求体接收——否则等于把权限交给前端。
 */
@Data
public class ItemCreateDTO {

    /** 标题 */
    @NotBlank(message = "标题不能为空")
    @Size(max = 200, message = "标题最多 200 字")
    private String title;

    /** 描述：可以为空，但不能超长 */
    @Size(max = 5000, message = "描述最多 5000 字")
    private String description;

    /** 类目 ID */
    @NotNull(message = "请选择类目")
    private Long categoryId;

    /** 价格 */
    @NotNull(message = "请填写价格")
    @DecimalMin(value = "0.01", message = "价格必须大于 0")
    @Digits(integer = 8, fraction = 2, message = "价格最多 8 位整数、2 位小数")
    private BigDecimal price;

    /** 图片 objectKey 列表，第 1 张作封面，最多 5 张 */
    @NotEmpty(message = "至少上传 1 张图片")
    @Size(max = 5, message = "最多 5 张图片")
    private List<String> images;
}