package org.zeniot.dao.model;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

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

    @Column(name = "node_type")
    private String nodeType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata")
    private JsonNode metadata;

    @CreationTimestamp
    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

    @UpdateTimestamp
    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    // @ManyToOne
    // @JoinColumn(name = "rule_chain_id")
    // private RuleChainEntity ruleChainEntity;

}
