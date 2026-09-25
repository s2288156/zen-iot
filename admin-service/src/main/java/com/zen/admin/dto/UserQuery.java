package com.zen.admin.dto;

import com.zen.common.core.page.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** 用户分页查询条件：用户名模糊匹配、状态精确匹配，均可空；分页与排序参数继承自 {@link PageQuery}。 */
@Getter
@Setter
public class UserQuery extends PageQuery {

    @Size(max = 64)
    @Schema(description = "用户名，模糊匹配，可空")
    private String username;

    @Min(0)
    @Max(1)
    @Schema(description = "状态过滤：1 启用 / 0 禁用；缺省不过滤", example = "1")
    private Integer status;
}
