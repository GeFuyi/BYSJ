package com.community.enums;

import com.community.common.BusinessException;

public enum ServiceAuditAction {
    APPROVE("审核通过"),
    REJECT("审核拒绝"),
    RETURN("驳回修改");

    private final String label;

    ServiceAuditAction(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static ServiceAuditAction fromCode(String code) {
        try {
            return valueOf(code);
        } catch (Exception ex) {
            throw new BusinessException("操作失败");
        }
    }
}
