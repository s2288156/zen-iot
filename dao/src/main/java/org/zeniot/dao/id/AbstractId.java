package org.zeniot.dao.id;

import lombok.Getter;
import lombok.ToString;

/**
 * @author Wu.Chunyang
 */
@ToString
public abstract class AbstractId {
    @Getter
    private Long id;

    protected AbstractId() {

    }

    protected AbstractId(Long id) {
        this.id = id;
    }

}
