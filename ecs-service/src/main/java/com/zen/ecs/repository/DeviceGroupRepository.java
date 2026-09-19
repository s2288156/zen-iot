package com.zen.ecs.repository;

import com.zen.ecs.entity.DeviceGroupEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DeviceGroupRepository extends JpaRepository<DeviceGroupEntity, Long> {

    List<DeviceGroupEntity> findAllByOrderByIdAsc();

    /** 与 {@code DeviceRepository#countByDeviceCodeIncludingDeleted} 同理：唯一索引不区分逻辑删除，查重必须绕过过滤。 */
    @Query(value = "SELECT COUNT(1) FROM t_device_group WHERE group_code = :groupCode", nativeQuery = true)
    long countByGroupCodeIncludingDeleted(@Param("groupCode") String groupCode);
}
