package com.zen.admin.dto;

import com.zen.common.core.page.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** 角色分页查询条件：编码/名称模糊匹配，均可空；分页与排序参数继承自 {@link PageQuery}。 */
@Getter
@Setter
public class RoleQuery extends PageQuery {

    @Size(max = 64)
    @Schema(description = "角色编码，模糊匹配，可空")
    private String roleCode;

    @Size(max = 64)
    @Schema(description = "角色名称，模糊匹配，可空")
    private String roleName;
}
