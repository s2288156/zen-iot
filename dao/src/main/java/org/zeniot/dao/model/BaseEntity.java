package org.zeniot.dao.model;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.UpdateTimestamp;
import org.zeniot.dao.config.SnowFlakeIdGenerator;

import java.time.LocalDateTime;

/**
 * @author s2288
 */
@Getter
@Setter
@ToString
@MappedSuperclass
public class BaseEntity {
    @Id
    @GenericGenerator(name = "id", type= SnowFlakeIdGenerator.class)
    @GeneratedValue(generator = "id")
    @Column(name = "id", nullable = false)
    private Long id;

    @CreationTimestamp
    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

    @UpdateTimestamp
    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

}
