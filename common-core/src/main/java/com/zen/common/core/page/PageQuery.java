package com.zen.common.core.page;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.util.StringUtils;

/** 分页查询请求参数,通过 {@link #toPageable()} 转换为 Spring Data 的 {@link Pageable}。 */
@Getter
@Setter
public class PageQuery {

  /** 单页最大条数,防止恶意超大分页拖垮数据库。 */
  public static final int MAX_PAGE_SIZE = 200;

  @Min(1)
  private int pageNum = 1;

  @Min(1)
  @Max(MAX_PAGE_SIZE)
  private int pageSize = 10;

  /** 排序属性名,可空。取值来自客户端:调用方必须先用实体属性白名单校验后再传入, 否则排序方向/属性可被任意构造(如利用子查询属性造成慢查询)。 */
  private String orderBy;

  /** 排序方向:asc / desc,默认 asc。 */
  private String orderDirection = "asc";

  public Pageable toPageable() {
    if (!StringUtils.hasText(orderBy)) {
      return PageRequest.of(pageNum - 1, pageSize);
    }
    Sort.Direction direction =
        "desc".equalsIgnoreCase(orderDirection) ? Sort.Direction.DESC : Sort.Direction.ASC;
    return PageRequest.of(pageNum - 1, pageSize, Sort.by(direction, orderBy));
  }
}
