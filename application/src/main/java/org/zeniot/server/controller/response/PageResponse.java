package org.zeniot.server.controller.response;

import java.util.Collections;
import java.util.List;

/**
 * @author Wu.Chunyang
 */
public class PageResponse<T> {

    private final List<T> data;
    private final int totalPages;
    private final long totalElements;
    private final boolean hasNext;

    public PageResponse() {
        this(Collections.emptyList(), 0, 0, false);
    }

    public PageResponse(List<T> data, int totalPages, long totalElements, boolean hasNext) {
        this.data = data;
        this.totalPages = totalPages;
        this.totalElements = totalElements;
        this.hasNext = hasNext;
    }
}
