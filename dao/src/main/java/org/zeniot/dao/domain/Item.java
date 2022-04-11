package org.zeniot.dao.domain;

import javax.persistence.*;
import java.math.BigDecimal;

/**
 * @author Wu.Chunyang
 */
@Entity
@Table(name = "t_item")
public class Item extends BaseEntity{

    private String title;
    private BigDecimal amount;

    @ManyToOne
    @JoinColumn(name = "account_id",
    nullable = false,
    foreignKey = @ForeignKey(name = "FK_ACCOUNT_ID"))
    private Account account;
}
