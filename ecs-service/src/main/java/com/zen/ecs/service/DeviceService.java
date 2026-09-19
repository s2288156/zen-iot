package com.zen.ecs.service;

import com.zen.common.core.page.PageQuery;
import com.zen.common.core.page.PageResult;
import com.zen.common.security.error.BusinessException;
import com.zen.common.security.error.GlobalErrorCode;
import com.zen.ecs.dto.DeviceCreateRequest;
import com.zen.ecs.dto.DeviceEventResponse;
import com.zen.ecs.dto.DeviceResponse;
import com.zen.ecs.dto.DeviceUpdateRequest;
import com.zen.ecs.entity.DeviceEntity;
import com.zen.ecs.error.EcsErrorCode;
import com.zen.ecs.repository.DeviceEventRepository;
import com.zen.ecs.repository.DeviceGroupRepository;
import com.zen.ecs.repository.DeviceRepository;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 设备档案的增删改查与上下线事件回读。
 *
 * <p>删除是逻辑删除（实体上的 {@code @SQLDelete}）：事件与指令按 {@code deviceId} 引用档案，物理删行会让历史查不到主语。
 */
@Service
public class DeviceService {

    /**
     * 可排序属性白名单。{@code PageQuery.orderBy} 是客户端传来的字符串，直接交给 {@code Sort} 等于把列名与查询形状交出去，
     * 而未映射的属性名会让整条查询报错，所以只放行这几个稳定字段。
     */
    private static final Set<String> SORTABLE_FIELDS =
            Set.of("id", "deviceCode", "deviceName", "createTime", "updateTime");

    /** 事件只增不改，回读取最近若干条即可，条数不交给调用方决定，免得一次拉走整张表。 */
    private static final int RECENT_EVENT_LIMIT = 20;

    private final DeviceRepository deviceRepository;

    private final DeviceGroupRepository deviceGroupRepository;

    private final DeviceEventRepository deviceEventRepository;

    public DeviceService(
            DeviceRepository deviceRepository,
            DeviceGroupRepository deviceGroupRepository,
            DeviceEventRepository deviceEventRepository) {
        this.deviceRepository = deviceRepository;
        this.deviceGroupRepository = deviceGroupRepository;
        this.deviceEventRepository = deviceEventRepository;
    }

    @Transactional
    public DeviceResponse create(DeviceCreateRequest request) {
        if (deviceRepository.countByDeviceCodeIncludingDeleted(request.deviceCode()) > 0) {
            throw new BusinessException(
                    EcsErrorCode.DEVICE_CODE_DUPLICATED, "设备编码 " + request.deviceCode() + " 已被占用（已删除的档案同样占位）");
        }
        requireGroupExists(request.groupId());
        DeviceEntity device = new DeviceEntity();
        device.setDeviceCode(request.deviceCode());
        device.setDeviceName(request.deviceName());
        device.setGroupId(request.groupId());
        device.setProtocolType(request.protocolType());
        device.setEndpoint(request.endpoint());
        // 新建即离线：只有心跳上报能把 online 置起来，档案本身不构成在线证据
        device.markOnline(false);
        return DeviceResponse.from(deviceRepository.save(device));
    }

    @Transactional
    public DeviceResponse update(long deviceId, DeviceUpdateRequest request) {
        DeviceEntity device = require(deviceId);
        requireGroupExists(request.groupId());
        device.setDeviceName(request.deviceName());
        device.setGroupId(request.groupId());
        device.setProtocolType(request.protocolType());
        device.setEndpoint(request.endpoint());
        return DeviceResponse.from(device);
    }

    @Transactional
    public void delete(long deviceId) {
        deviceRepository.delete(require(deviceId));
    }

    @Transactional(readOnly = true)
    public DeviceResponse get(long deviceId) {
        return DeviceResponse.from(require(deviceId));
    }

    /** {@code groupId} 为空表示不按分组过滤，返回全部设备。 */
    @Transactional(readOnly = true)
    public PageResult<DeviceResponse> page(PageQuery query, Long groupId) {
        rejectUnlistedSortField(query);
        Pageable pageable = query.toPageable();
        Page<DeviceEntity> page = groupId == null
                ? deviceRepository.findAll(pageable)
                : deviceRepository.findByGroupId(groupId, pageable);
        return PageResult.of(
                page, page.getContent().stream().map(DeviceResponse::from).toList());
    }

    /** 设备不存在时报业务错，而不是回一个空列表：调用方分不清「没事件」和「设备编码写错」。 */
    @Transactional(readOnly = true)
    public List<DeviceEventResponse> recentEvents(long deviceId) {
        require(deviceId);
        return deviceEventRepository
                .findByDeviceIdOrderByOccurredAtDesc(deviceId, PageRequest.of(0, RECENT_EVENT_LIMIT))
                .stream()
                .map(DeviceEventResponse::from)
                .toList();
    }

    private DeviceEntity require(long deviceId) {
        return deviceRepository
                .findById(deviceId)
                .orElseThrow(() -> new BusinessException(
                        EcsErrorCode.DEVICE_NOT_FOUND, EcsErrorCode.DEVICE_NOT_FOUND.getMessage() + "：" + deviceId));
    }

    private void requireGroupExists(Long groupId) {
        if (groupId != null && deviceGroupRepository.findById(groupId).isEmpty()) {
            throw new BusinessException(EcsErrorCode.DEVICE_GROUP_NOT_FOUND);
        }
    }

    private void rejectUnlistedSortField(PageQuery query) {
        String orderBy = query.getOrderBy();
        if (orderBy != null && !orderBy.isBlank() && !SORTABLE_FIELDS.contains(orderBy)) {
            throw new BusinessException(GlobalErrorCode.BAD_REQUEST, "orderBy 只支持 " + SORTABLE_FIELDS);
        }
    }
}
