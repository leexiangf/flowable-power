package com.power.common.constant;

/**
 * 系统内置资源标识（与初始化 SQL 种子数据 ID 段保持一致）。
 */
public final class SystemConstants {

  private SystemConstants() {}

  /** 内置管理员用户 ID */
  public static final long BUILT_IN_ADMIN_USER_ID = 1L;

  /**
   * 内置菜单最大 ID。种子数据使用 100–362 等固定 ID；业务新增菜单为雪花 ID，远大于此值。
   */
  public static final long BUILT_IN_MENU_MAX_ID = 999L;

  public static boolean isBuiltInMenu(Long menuId) {
    return menuId != null && menuId <= BUILT_IN_MENU_MAX_ID;
  }
}
