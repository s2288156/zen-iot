package org.zeniot.dao.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;


/**
 * @author wcy-auto
 **/
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
@Entity
@Table(name = "t_node_relation")
public class NodeRelationEntity {

    @Id
    @Column(name = "id", nullable = false)
    private String id;

    @Column(name = "rule_chain_id")
    private Long ruleChainId;

    @Column(name = "source_id")
    private String sourceId;

    @Column(name = "target_id")
    private String targetId;

    @UpdateTimestamp
    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    @ToString.Exclude
    @ManyToOne
    @JoinColumn(name = "rule_chain_id", insertable = false, updatable = false)
    private RuleChainEntity ruleChainEntity;
}
