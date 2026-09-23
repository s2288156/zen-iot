package com.zen.admin.controller;

import com.zen.admin.config.ZenAdminOpenApiConfiguration;
import com.zen.common.security.api.ApiResponse;
import com.zen.common.security.auth.ModuleCode;
import com.zen.common.security.auth.RequireModule;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 模块级授权的样例接口，本阶段用于自证鉴权闭环，不含业务逻辑。
 *
 * <p>路径不占用业务资源名，经网关后即 {@code /api/admin/demo/admin}。
 *
 * <p>{@link SecurityRequirement} 写在类上：两个接口的鉴权口径一致，且要与运行期一致——{@code /demo/**} 不在
 * {@code zen.security.whitelist} 里，未带身份就是 401。403 由 {@link RequireModule} 推导，见
 * {@link ZenAdminOpenApiConfiguration}。
 */
@Tag(name = "模块鉴权样例", description = "只用来验证 @RequireModule 的判定与网关透传身份是否接得上，不含业务逻辑。")
@RestController
@RequestMapping("/demo")
@SecurityRequirement(name = ZenAdminOpenApiConfiguration.BEARER_JWT_SCHEME)
public class DemoController {

    @Operation(
            summary = "需要 admin 模块权限的样例接口",
            description = "判定依据是 Token 里的 modules 快照：种子账号 demo 只有 ecs/wcs/rcs，带它的 Token 打这里就是 403。")
    @GetMapping("/admin")
    @RequireModule(ModuleCode.ADMIN)
    public ApiResponse<String> admin() {
        return ApiResponse.success("admin 模块接口");
    }

    @Operation(summary = "需要 ecs 模块权限的样例接口", description = "同一账号换到这个模块就是 200，可用来区分「403 来自模块判定」还是「身份本身失效」。")
    @GetMapping("/ecs")
    @RequireModule(ModuleCode.ECS)
    public ApiResponse<String> ecs() {
        return ApiResponse.success("ecs 模块接口");
    }
}
