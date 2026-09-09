package com.zen.common.core.security;

/**
 * 业务模块编码，接口授权的最小管控单位。
 *
 * <p>{@code ADMIN} 是「管理后台」模块而不是超级权限：管理员账号之所以全通，是因为种子数据给它挂了含全部模块的角色，判定逻辑对本枚举所有取值一视同仁。
 */
public enum ModuleCode {
  ADMIN("admin"),
  WCS("wcs"),
  RCS("rcs"),
  ECS("ecs");

  /** 落库与 {@code modules} claim 里使用的码值，均为小写。 */
  private final String code;

  ModuleCode(String code) {
    this.code = code;
  }

  public String getCode() {
    return code;
  }
}
