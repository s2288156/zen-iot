package org.zeniot.dao.model;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;
import org.zeniot.data.enums.NodeTypeEnum;

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
@Table(name = "t_node")
public class NodeEntity {

    @Id
    @Column(name = "id", nullable = false)
    private String id;

    @Column(name = "node_name")
    private String nodeName;

    @Column(name = "rule_chain_id")
    private Long ruleChainId;

    @Enumerated(EnumType.STRING)
    @Column(name = "node_type")
    private NodeTypeEnum nodeType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata")
    private JsonNode metadata;

    @UpdateTimestamp
    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    @ToString.Exclude
    @ManyToOne
    @JoinColumn(name = "rule_chain_id", insertable = false, updatable = false)
    private RuleChainEntity ruleChainEntity;

}
