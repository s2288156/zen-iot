package org.zeniot.data;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import org.springframework.data.domain.PageRequest;

import javax.validation.constraints.NotNull;

/**
 * @author Wu.Chunyang
 */
@Getter(AccessLevel.PRIVATE)
@Data
public class PageQuery {
    @NotNull
    private Integer page;
    @NotNull
    private Integer size;
    private Boolean asc;
    private String orderBy;

    public PageRequest toPageable() {
        return PageRequest.of(page, size);
    }
}
