package com.community.enums;

import com.community.common.BusinessException;

public enum ServiceOperateStatus {
    RESERVABLE("\u53ef\u9884\u7ea6"),
    FULL("\u5df2\u7ea6\u6ee1"),
    IN_SERVICE("\u670d\u52a1\u4e2d"),
    PAUSED("\u5df2\u6682\u505c");

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
            throw new BusinessException("操作失败");
        }
    }
}
