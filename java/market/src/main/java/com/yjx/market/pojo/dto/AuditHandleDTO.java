package com.yjx.market.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 人工审核处置入参。
 */
@Data
public class AuditHandleDTO {

    /** 处置结论：PASS（通过）/ REJECT（驳回） */
    @NotBlank(message = "请指定审核结论")
    @Pattern(regexp = "PASS|REJECT", message = "审核结论只能是 PASS 或 REJECT")
    private String result;

    /** 理由：驳回必填（Service 里校验），通过可留空 */
    @Size(max = 500, message = "理由最多 500 字")
    private String reason;
}