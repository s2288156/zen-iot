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
@Table(name = "t_node_relation")
public class NodeRelationEntity extends BaseEntity {

    @Column(name = "source_id")
    private Integer sourceId;

    @Column(name = "target_id")
    private Integer targetId;

}
