package org.zeniot.data;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * @author Wu.Chunyang
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class PageResponse<T> extends Response {

    private final List<T> data;
    private final int size;
    private final int totalPages;
    private final long totalElements;
    private final boolean hasNext;

    private PageResponse(List<T> data, int size, int totalPages, long totalElements, boolean hasNext) {
        super(true);
        this.data = data;
        this.size = size;
        this.totalPages = totalPages;
        this.totalElements = totalElements;
        this.hasNext = hasNext;
    }

    public static <T> PageResponse<T> ok(List<T> data, int size, int totalPages, long totalElements, boolean hasNext) {
        return new PageResponse<>(data, size, totalPages, totalElements, hasNext);
    }
}
