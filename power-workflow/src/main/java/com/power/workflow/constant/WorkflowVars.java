package com.power.workflow.constant;

public final class WorkflowVars {

    public static final String START_USER_ID = "startUserId";
    public static final String START_USERNAME = "startUsername";
    public static final String TITLE = "title";
    public static final String BUSINESS_TYPE = "businessType";
    public static final String APPROVED = "approved";

    /** 变量办理人：部门经理 userId 字符串 */
    public static final String MANAGER_USER_ID = "managerUserId";

    /** 会签人列表：List&lt;String&gt; userId */
    public static final String COUNTERSIGN_USER_IDS = "countersignUserIds";

    public static final String BUSINESS_TYPE_LEAVE = "leave";
    public static final String BUSINESS_TYPE_EXPENSE = "expense";
    public static final String BUSINESS_TYPE_COUNTERSIGN_OR = "countersign-or";
    public static final String BUSINESS_TYPE_COUNTERSIGN_SEQ = "countersign-seq";

    /** 加签模式：BEFORE / AFTER */
    public static final String ADD_SIGN_MODE = "addSignMode";

    /** 前加签归还目标 userId */
    public static final String ADD_SIGN_RETURN_TO = "addSignReturnTo";

    private WorkflowVars() {
    }
}
