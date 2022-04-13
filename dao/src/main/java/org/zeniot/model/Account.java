package org.zeniot.model;

import javax.persistence.*;

/**
 * @author Wu.Chunyang
 */
@Entity
@Table(name = "t_account")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

}
