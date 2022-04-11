package org.zeniot.dao.domain;

import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

/**
 * @author Wu.Chunyang
 */
@MappedSuperclass
public class BaseEntity {

    @Id
    private Long id;
}
