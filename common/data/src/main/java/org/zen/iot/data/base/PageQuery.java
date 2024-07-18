package org.zen.iot.data.base;

import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.PageRequest;

/**
 * @author Wu.Chunyang
 */
@NoArgsConstructor
@Getter(AccessLevel.PRIVATE)
@Data
public class PageQuery {
    @NotNull
    private Integer page;
    @NotNull
    private Integer size;
    private Boolean asc;
    private String orderBy;

    public PageQuery(Integer page, Integer size) {
        this.page = page;
        this.size = size;
    }

    public PageRequest toPageable() {
        return PageRequest.of(page, size);
    }
}
