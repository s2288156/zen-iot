package org.zeniot.server.controller.response;

import lombok.Data;

import java.util.Collections;
import java.util.List;

/**
 * @author Wu.Chunyang
 */
@Data
public class PageResponse<T> {

    private final List<T> data;
    private final int size;
    private final int totalPages;
    private final long totalElements;
    private final boolean hasNext;

    private PageResponse(List<T> data, int size, int totalPages, long totalElements, boolean hasNext) {
        this.data = data;
        this.size = size;
        this.totalPages = totalPages;
        this.totalElements = totalElements;
        this.hasNext = hasNext;
    }

    public static <T> PageResponse<T> of(List<T> data, int size, int totalPages, long totalElements, boolean hasNext) {
        return new PageResponse<>(data, size, totalPages, totalElements, hasNext);
    }
}
