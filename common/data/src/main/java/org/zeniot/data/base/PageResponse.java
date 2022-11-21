package org.zeniot.data.base;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.domain.Page;

import java.util.Collection;

/**
 * @author Wu.Chunyang
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class PageResponse<T> extends Response {

    private final Collection<T> data;
    private final int size;
    private final int totalPages;
    private final long totalElements;
    private final boolean hasNext;

    private PageResponse(Collection<T> data, int size, int totalPages, long totalElements, boolean hasNext) {
        super(true);
        this.data = data;
        this.size = size;
        this.totalPages = totalPages;
        this.totalElements = totalElements;
        this.hasNext = hasNext;
    }

    public static <T> PageResponse<T> ok(Collection<T> data, Page<?> page) {
        return new PageResponse<>(data, page.getSize(), page.getTotalPages(), page.getTotalElements(), page.hasNext());
    }
}
