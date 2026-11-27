package com.yjx.market.pojo.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 类目识别请求（转发给 Python /ai/category）。
 *
 * 这些校验是「第一道闸」：Python 那边也有 Pydantic 校验，
 * 但坏请求不该跨进程跑到 Python 才被拒，那样日志里会出现无意义的远程错误。
 */
@Data
public class AiCategoryDTO {

    @NotBlank(message = "标题不能为空")
    @Size(max = 100, message = "标题最多 100 字")
    private String title;

    @Size(max = 2000, message = "描述最多 2000 字")
    private String description;

    @Min(value = 1, message = "topK 至少 1")
    @Max(value = 10, message = "topK 最多 10")
    private Integer topK = 3;
}