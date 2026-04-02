package com.community.enums;

import com.community.common.BusinessException;

public enum ServiceBookingStatus {
    BOOKED("\u5df2\u9884\u7ea6"),
    CANCELLED("\u5df2\u53d6\u6d88");

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
            throw new BusinessException("操作失败");
        }
    }
}
