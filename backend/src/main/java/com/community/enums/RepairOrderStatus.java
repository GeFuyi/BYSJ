package com.community.enums;

import com.community.common.BusinessException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public enum RepairOrderStatus {
    SUBMITTED("\u5df2\u63d0\u4ea4"),
    ACCEPTED("\u5df2\u53d7\u7406"),
    IN_PROGRESS("\u5904\u7406\u4e2d"),
    COMPLETED_PENDING_CONFIRM("\u5f85\u7528\u6237\u786e\u8ba4"),
    COMPLETED("\u5df2\u5b8c\u6210"),
    ROLLBACK("\u5f02\u5e38\u56de\u9000");

    private static final Map<RepairOrderStatus, Set<RepairOrderStatus>> TRANSITIONS = new HashMap<>();

    static {
        TRANSITIONS.put(SUBMITTED, setOf(ACCEPTED, ROLLBACK));
        TRANSITIONS.put(ACCEPTED, setOf(IN_PROGRESS, ROLLBACK));
        TRANSITIONS.put(IN_PROGRESS, setOf(COMPLETED_PENDING_CONFIRM, ROLLBACK));
        TRANSITIONS.put(COMPLETED_PENDING_CONFIRM, setOf(COMPLETED, ROLLBACK));
        TRANSITIONS.put(ROLLBACK, setOf(ACCEPTED));
        TRANSITIONS.put(COMPLETED, setOf());
    }

    private final String label;

    RepairOrderStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static RepairOrderStatus fromCode(String code) {
        try {
            return valueOf(code);
        } catch (Exception ex) {
            throw new BusinessException("操作失败");
        }
    }

    public static boolean canTransition(String fromCode, String toCode) {
        RepairOrderStatus from = fromCode(fromCode);
        RepairOrderStatus to = fromCode(toCode);
        return TRANSITIONS.getOrDefault(from, setOf()).contains(to);
    }

    public static String labelOf(String code) {
        return fromCode(code).getLabel();
    }

    public static boolean isEmployeeActionTarget(String targetCode) {
        RepairOrderStatus target = fromCode(targetCode);
        return target == ACCEPTED
                || target == IN_PROGRESS
                || target == COMPLETED_PENDING_CONFIRM
                || target == ROLLBACK;
    }

    public static boolean isUserConfirmTarget(String targetCode) {
        return COMPLETED.name().equals(targetCode);
    }

    public static String allowedCodes() {
        return Arrays.toString(values());
    }

    private static Set<RepairOrderStatus> setOf(RepairOrderStatus... statuses) {
        return new HashSet<>(Arrays.asList(statuses));
    }
}
