package com.yjx.market.service.audit;

import com.yjx.market.mapper.AuditRecordMapper;
import com.yjx.market.pojo.entity.AuditRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 审核记录落库。只负责「写 audit_record」，不碰 item 状态 ——
 * 状态流转是 ItemStatusMachine + 调用方的事，职责分开才不会互相踩。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditRecordService {

    /** 审核类型 */
    public static final String TYPE_MACHINE = "MACHINE";
    public static final String TYPE_MANUAL = "MANUAL";

    /** 结论值（对应 audit_record.machine_result / result） */
    public static final String PASS = "PASS";
    public static final String REJECT = "REJECT";

    private final AuditRecordMapper auditRecordMapper;

    /**
     * 写一条机审记录。
     * <p>
     * 机审只填 machine_result，result 留 null —— 那是最终结论，
     * 要等人工审核（或 Step 14 的自动判定）才有。
     * matched_rule_ids / rule_ver 故意不填：规则库在 Step 13 才建。
     */
    public void saveMachine(Long itemId, AuditResult result) {
        AuditRecord record = new AuditRecord();
        record.setItemId(itemId);
        record.setType(TYPE_MACHINE);
        record.setMachineResult(result.autoPass() ? PASS : REJECT);
        record.setResult(null);
        record.setRisk(result.risk().name());
        record.setScore(result.score());
        record.setReason(result.reason());
        record.setMatchedRuleIds(null);
        record.setRuleVer(null);
        record.setOperatorId(null);
        record.setCreateTime(LocalDateTime.now());

        auditRecordMapper.insert(record);
        log.info("机审记录已落库：itemId={}, risk={}, machineResult={}",
                itemId, record.getRisk(), record.getMachineResult());
    }

    /**
     * 写一条人工审核记录。
     * <p>
     * 人工记录只填 result（最终结论）和 operator_id；
     * machine_result / risk / score 留空 —— 那些是机审的产物，
     * 人审不该覆盖，两张记录放在一起才看得出「机审说了什么、人最后怎么判」。
     */
    public void saveManual(Long itemId, Long operatorId, boolean pass, String reason) {
        AuditRecord record = new AuditRecord();
        record.setItemId(itemId);
        record.setType(TYPE_MANUAL);
        record.setMachineResult(null);
        record.setResult(pass ? PASS : REJECT);
        record.setRisk(null);
        record.setScore(null);
        record.setReason(reason);
        record.setMatchedRuleIds(null);
        record.setRuleVer(null);
        record.setOperatorId(operatorId);
        record.setCreateTime(LocalDateTime.now());

        auditRecordMapper.insert(record);
        log.info("人工审核记录已落库：itemId={}, operatorId={}, result={}",
                itemId, operatorId, record.getResult());
    }
}