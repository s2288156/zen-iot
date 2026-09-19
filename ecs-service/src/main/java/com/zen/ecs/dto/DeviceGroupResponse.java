package com.zen.ecs.dto;

import com.zen.ecs.entity.DeviceGroupEntity;

/** 设备分组的对外契约。 */
public record DeviceGroupResponse(Long id, String groupCode, String groupName, String description) {

    public static DeviceGroupResponse from(DeviceGroupEntity group) {
        return new DeviceGroupResponse(
                group.getId(), group.getGroupCode(), group.getGroupName(), group.getDescription());
    }
}
