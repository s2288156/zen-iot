package com.zen.ecs.repository;

import com.zen.ecs.entity.DeviceEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DeviceRepository extends JpaRepository<DeviceEntity, Long> {

    Optional<DeviceEntity> findByDeviceCode(String deviceCode);

    /**
     * 连软删掉的行一起数：{@code uk_device_code} 不区分 {@code deleted}，只看未删除的行会让新建在插入时才撞唯一键，
     * 变成一个没有信息的 500。原生 SQL 不受实体上 {@code @SQLRestriction} 影响，正好用来做这件事。
     */
    @Query(value = "SELECT COUNT(1) FROM t_device WHERE device_code = :deviceCode", nativeQuery = true)
    long countByDeviceCodeIncludingDeleted(@Param("deviceCode") String deviceCode);

    /** 超时探测只扫在线设备：离线设备的心跳键本来就该缺席，扫它纯属浪费。 */
    List<DeviceEntity> findByOnline(Byte online);

    Page<DeviceEntity> findByGroupId(Long groupId, Pageable pageable);

    long countByGroupId(Long groupId);
}
