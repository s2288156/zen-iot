package org.zeniot.dao.domain;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;
import java.util.List;

/**
 * @author Wu.Chunyang
 */
@Entity
@Table(name = "t_account")
public class Account extends BaseEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @ManyToOne
    @JoinColumn(name = "customer_id",
    nullable = false,
    foreignKey = @ForeignKey(name = "FK_CUSTOMER_ID"))
    private Customer customer;

    @OneToMany(mappedBy = "account")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<Item> items;
}
