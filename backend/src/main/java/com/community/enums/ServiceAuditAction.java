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
            throw new BusinessException("不支持的审核动作: " + code);
        }
    }
}

