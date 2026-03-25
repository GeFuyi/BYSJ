package com.community.enums;

import com.community.common.BusinessException;

public enum ServiceAuditStatus {
    PENDING("待审核"),
    APPROVED("审核通过"),
    REJECTED("审核拒绝"),
    RETURNED("驳回修改");

    private final String label;

    ServiceAuditStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static ServiceAuditStatus fromCode(String code) {
        try {
            return valueOf(code);
        } catch (Exception ex) {
            throw new BusinessException("不支持的审核状态: " + code);
        }
    }
}

