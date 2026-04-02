package com.community.enums;

import com.community.common.BusinessException;

public enum ServiceAuditStatus {
    PENDING("\u5f85\u5ba1\u6838"),
    APPROVED("\u5df2\u901a\u8fc7"),
    REJECTED("\u5df2\u9a73\u56de"),
    RETURNED("\u5df2\u9000\u56de");

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
            throw new BusinessException("操作失败");
        }
    }
}
