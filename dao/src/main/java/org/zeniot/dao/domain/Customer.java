package org.zeniot.dao.domain;

import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * @author Wu.Chunyang
 */
@Entity
@Table(name = "t_customer")
public class Customer extends BaseEntity{
    private String name;
}
