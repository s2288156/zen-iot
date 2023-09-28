package org.zeniot.dao.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
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

    @Column(name = "source_id")
    private Integer sourceId;

    @Column(name = "target_id")
    private Integer targetId;

    @CreationTimestamp
    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

    @UpdateTimestamp
    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;
}
