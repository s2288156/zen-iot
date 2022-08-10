package org.zeniot.server.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * @author Wu.Chunyang
 */
@Data
public class PageQuery {
    @NotNull
    private Integer page;
    @NotNull
    private Integer size;
    private Boolean asc;
    private String orderBy;
}
