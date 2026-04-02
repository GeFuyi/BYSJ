package com.community.enums;

import com.community.common.BusinessException;

public enum ElectricityOrderStatus {

    CREATED("\u5df2\u521b\u5efa"),
    WAIT_PAY("\u5f85\u652f\u4ed8"),
    PAID("\u5df2\u652f\u4ed8"),
    CLOSED("\u5df2\u5173\u95ed"),
    FAILED("\u5931\u8d25");

    private final String label;

    ElectricityOrderStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static ElectricityOrderStatus fromCode(String code) {
        for (ElectricityOrderStatus item : values()) {
            if (item.name().equalsIgnoreCase(code)) {
                return item;
            }
        }
        throw new BusinessException("操作失败");
    }
}
