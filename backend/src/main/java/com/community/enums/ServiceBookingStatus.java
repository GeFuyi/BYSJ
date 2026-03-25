package com.community.enums;

import com.community.common.BusinessException;

public enum ServiceBookingStatus {
    BOOKED("已预约"),
    CANCELLED("已取消");

    private final String label;

    ServiceBookingStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static ServiceBookingStatus fromCode(String code) {
        try {
            return valueOf(code);
        } catch (Exception ex) {
            throw new BusinessException("不支持的预约状态: " + code);
        }
    }
}

