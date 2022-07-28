package org.zeniot.dao.id;

import lombok.Getter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;

/**
 * @author Wu.Chunyang
 */
@ToString
public abstract class AbstractId implements Serializable {
    @Serial
    private static final long serialVersionUID = 1419327656417149649L;
    @Getter
    private Long id;

    protected AbstractId() {

    }

    protected AbstractId(Long id) {
        this.id = id;
    }

}
