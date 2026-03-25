package com.community.enums;

import com.community.common.BusinessException;

public enum ServiceOperateStatus {
    RESERVABLE("可预约"),
    FULL("约满"),
    IN_SERVICE("进行中"),
    PAUSED("暂停");

    private final String label;

    ServiceOperateStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static ServiceOperateStatus fromCode(String code) {
        try {
            return valueOf(code);
        } catch (Exception ex) {
            throw new BusinessException("不支持的服务状态: " + code);
        }
    }
}

