package org.zeniot.dao.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;


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

    // @ToString.Exclude
    // @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    // private Set<NodeEntity> nodeEntities = new LinkedHashSet<>();

}
