package com.power.workflow.support;

import org.springframework.util.StringUtils;

/**
 * 流程变量规范化（避免 JSON 字符串 / 数字导致网关条件失败）。
 */
public final class WorkflowApprovals {

    private WorkflowApprovals() {
    }

    /**
     * 将 approved 规范为 Boolean。
     *
     * @param value        原始值
     * @param defaultWhenNull 值为 null 时的默认（complete 缺省 true；结束监听 fail-closed 传 false）
     * @return 布尔结果
     */
    public static boolean toBoolean(Object value, boolean defaultWhenNull) {
        if (value == null) {
            return defaultWhenNull;
        }
        if (value instanceof Boolean b) {
            return b;
        }
        if (value instanceof Number n) {
            return n.intValue() != 0;
        }
        String text = String.valueOf(value).trim();
        if (!StringUtils.hasText(text)) {
            return defaultWhenNull;
        }
        if ("1".equals(text) || "true".equalsIgnoreCase(text) || "yes".equalsIgnoreCase(text)) {
            return true;
        }
        if ("0".equals(text) || "false".equalsIgnoreCase(text) || "no".equalsIgnoreCase(text)) {
            return false;
        }
        return defaultWhenNull;
    }

    /**
     * 解析 businessKey 为 Long；非法则返回 null。
     */
    public static Long parseLongBusinessKey(String businessKey) {
        if (!StringUtils.hasText(businessKey)) {
            return null;
        }
        try {
            return Long.valueOf(businessKey.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
