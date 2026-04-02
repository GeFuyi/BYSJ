package com.community.enums;

import com.community.common.BusinessException;

public enum ServiceAuditAction {
    APPROVE,
    REJECT,
    RETURN;

    public static ServiceAuditAction fromCode(String code) {
        try {
            return valueOf(code);
        } catch (Exception ex) {
            throw new BusinessException("操作失败");
        }
    }
}
