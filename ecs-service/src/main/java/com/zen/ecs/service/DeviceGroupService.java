package com.zen.ecs.service;

import com.zen.common.security.error.BusinessException;
import com.zen.ecs.dto.DeviceGroupRequest;
import com.zen.ecs.dto.DeviceGroupResponse;
import com.zen.ecs.dto.DeviceGroupUpdateRequest;
import com.zen.ecs.entity.DeviceGroupEntity;
import com.zen.ecs.error.EcsErrorCode;
import com.zen.ecs.repository.DeviceGroupRepository;
import com.zen.ecs.repository.DeviceRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 设备分组：分组编码是设备档案引用它的方式，因此建后不可改；组内还有设备时不允许删除。 */
@Service
public class DeviceGroupService {

    private final DeviceGroupRepository deviceGroupRepository;

    private final DeviceRepository deviceRepository;

    public DeviceGroupService(DeviceGroupRepository deviceGroupRepository, DeviceRepository deviceRepository) {
        this.deviceGroupRepository = deviceGroupRepository;
        this.deviceRepository = deviceRepository;
    }

    @Transactional
    public DeviceGroupResponse create(DeviceGroupRequest request) {
        if (deviceGroupRepository.countByGroupCodeIncludingDeleted(request.groupCode()) > 0) {
            throw new BusinessException(
                    EcsErrorCode.DEVICE_GROUP_CODE_DUPLICATED, "分组编码 " + request.groupCode() + " 已被占用（已删除的分组同样占位）");
        }
        DeviceGroupEntity group = new DeviceGroupEntity();
        group.setGroupCode(request.groupCode());
        group.setGroupName(request.groupName());
        group.setDescription(request.description());
        return DeviceGroupResponse.from(deviceGroupRepository.save(group));
    }

    @Transactional
    public DeviceGroupResponse update(long groupId, DeviceGroupUpdateRequest request) {
        DeviceGroupEntity group = require(groupId);
        group.setGroupName(request.groupName());
        group.setDescription(request.description());
        return DeviceGroupResponse.from(group);
    }

    @Transactional
    public void delete(long groupId) {
        DeviceGroupEntity group = require(groupId);
        long inUse = deviceRepository.countByGroupId(groupId);
        if (inUse > 0) {
            throw new BusinessException(
                    EcsErrorCode.DEVICE_GROUP_IN_USE, "分组 " + groupId + " 下仍有 " + inUse + " 台设备，不能删除");
        }
        deviceGroupRepository.delete(group);
    }

    @Transactional(readOnly = true)
    public List<DeviceGroupResponse> list() {
        return deviceGroupRepository.findAllByOrderByIdAsc().stream()
                .map(DeviceGroupResponse::from)
                .toList();
    }

    private DeviceGroupEntity require(long groupId) {
        return deviceGroupRepository
                .findById(groupId)
                .orElseThrow(() -> new BusinessException(
                        EcsErrorCode.DEVICE_GROUP_NOT_FOUND,
                        EcsErrorCode.DEVICE_GROUP_NOT_FOUND.getMessage() + "：" + groupId));
    }
}
