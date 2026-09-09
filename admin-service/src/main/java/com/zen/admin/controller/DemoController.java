package com.zen.admin.controller;

import com.zen.common.core.api.ApiResponse;
import com.zen.common.core.security.ModuleCode;
import com.zen.common.core.security.RequireModule;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 模块级授权的样例接口，本阶段用于自证鉴权闭环，不含业务逻辑。
 *
 * <p>路径不占用业务资源名，经网关后即 {@code /api/admin/demo/admin}。
 */
@RestController
@RequestMapping("/demo")
public class DemoController {

  @GetMapping("/admin")
  @RequireModule(ModuleCode.ADMIN)
  public ApiResponse<String> admin() {
    return ApiResponse.success("admin 模块接口");
  }

  @GetMapping("/ecs")
  @RequireModule(ModuleCode.ECS)
  public ApiResponse<String> ecs() {
    return ApiResponse.success("ecs 模块接口");
  }
}
