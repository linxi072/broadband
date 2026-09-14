package com.broadband.install.model;

/**
 * SLA 与赔付相关枚举。
 * 业务类型、评估方式、状态、赔付类型与单位、赔付状态集中定义，便于前后端与 DB 字段对齐。
 */
public final class SlaEnums {

    /** 业务类型：决定适用哪条 SLA 规则。 */
    public enum OrderType {
        NEW_INSTALL("新装宽带"),
        MOVE("宽带移机"),
        REPAIR("故障报修"),
        SPEED_UP("宽带提速"),
        RENEW("续费");
        public final String label;
        OrderType(String label) { this.label = label; }
    }

    /** 评估方式：时限类（装维是否在承诺时间内完成）/ 速率类（装机测速是否达标）。 */
    public enum EvalType {
        TIME("时限类"),
        SPEED("速率类");
        public final String label;
        EvalType(String label) { this.label = label; }
    }

    /** SLA 判定结果。 */
    public enum SlaStatus {
        PENDING("待判定"),
        MET("达标"),
        OVERTIME("超时");
        public final String label;
        SlaStatus(String label) { this.label = label; }
    }

    /** 赔付形式。 */
    public enum CompType {
        VOUCHER("流量券/电子券"),
        CASH("现金/话费"),
        FEE_WAIVE("费用减免");
        public final String label;
        CompType(String label) { this.label = label; }
    }

    /** 赔付金额计算单位。 */
    public enum CompUnit {
        PER_ORDER("每单固定"),
        PER_OVERTIME_HOUR("每超时小时");
        public final String label;
        CompUnit(String label) { this.label = label; }
    }

    /** 赔付工单状态。 */
    public enum CompStatus {
        PENDING("待赔付"),
        VERIFYING("待核实"),
        PAID("已赔付"),
        REJECTED("已驳回");
        public final String label;
        CompStatus(String label) { this.label = label; }
    }

    private SlaEnums() {}
}
