package org.zeniot.dao.model;

import com.vladmihalcea.hibernate.type.json.JsonType;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import java.time.LocalDateTime;

/**
 * @author s2288
 */
@Getter
@Setter
@ToString
@TypeDef(name = "jsonp", typeClass = JsonType.class)
@MappedSuperclass
public class BaseEntity {
    @Id
    @GenericGenerator(name = "id", strategy = "org.zeniot.dao.config.SnowFlakeIdGenerator")
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
