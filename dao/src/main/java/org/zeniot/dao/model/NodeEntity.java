package org.zeniot.dao.model;

import jakarta.persistence.*;
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
@Table(name = "t_node")
public class NodeEntity extends BaseEntity {

    @Column(name = "name")
    private String name;

    @Column(name = "rule_chain_id")
    private Integer ruleChainId;

    @Column(name = "graph_info")
    private String graphInfo;

}
