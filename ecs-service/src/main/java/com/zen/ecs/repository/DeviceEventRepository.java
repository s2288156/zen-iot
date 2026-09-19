package com.zen.ecs.repository;

import com.zen.ecs.entity.DeviceEventEntity;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceEventRepository extends JpaRepository<DeviceEventEntity, Long> {

    /** 事件只增不改，按时间倒序取最近若干条，条数由 Pageable 限制，避免无界结果集。 */
    List<DeviceEventEntity> findByDeviceIdOrderByOccurredAtDesc(Long deviceId, Pageable pageable);
}
