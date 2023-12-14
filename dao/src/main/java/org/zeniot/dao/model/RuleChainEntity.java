package org.zeniot.dao.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.LinkedHashSet;
import java.util.Set;


/**
 * @author wcy-auto
 **/
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
@Entity
@Table(name = "t_rule_chain")
public class RuleChainEntity extends BaseEntity {

    @Column(name = "name")
    private String name;

}
