package com.zen.common.core.page;

import java.util.List;
import lombok.Getter;
import org.springframework.data.domain.Page;

/** 分页查询结果,pageNum 为 1-based,与 {@link PageQuery} 对称。 */
@Getter
public class PageResult<T> {

  private final List<T> list;

  private final long total;

  private final int pageNum;

  private final int pageSize;

  private final int pages;

  private PageResult(List<T> list, long total, int pageNum, int pageSize, int pages) {
    this.list = list;
    this.total = total;
    this.pageNum = pageNum;
    this.pageSize = pageSize;
    this.pages = pages;
  }

  public static <T> PageResult<T> of(Page<T> page) {
    return new PageResult<>(
        page.getContent(),
        page.getTotalElements(),
        page.getNumber() + 1,
        page.getSize(),
        page.getTotalPages());
  }

  /** 内容已映射为 DTO 时使用:mappedList 须与 page 当前页内容一一对应。 */
  public static <S, T> PageResult<T> of(Page<S> page, List<T> mappedList) {
    return new PageResult<>(
        mappedList,
        page.getTotalElements(),
        page.getNumber() + 1,
        page.getSize(),
        page.getTotalPages());
  }

  public static <T> PageResult<T> empty() {
    return new PageResult<>(List.of(), 0, 1, 0, 0);
  }
}
