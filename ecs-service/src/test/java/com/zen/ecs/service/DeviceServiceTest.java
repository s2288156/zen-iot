package com.zen.ecs.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zen.common.core.page.PageQuery;
import com.zen.common.security.error.BusinessException;
import com.zen.ecs.dto.DeviceCreateRequest;
import com.zen.ecs.dto.DeviceUpdateRequest;
import com.zen.ecs.entity.DeviceEntity;
import com.zen.ecs.entity.DeviceGroupEntity;
import com.zen.ecs.error.EcsErrorCode;
import com.zen.ecs.repository.DeviceEventRepository;
import com.zen.ecs.repository.DeviceGroupRepository;
import com.zen.ecs.repository.DeviceRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

/** 设备档案的业务规则：编码占位、分组存在性、排序白名单。 */
class DeviceServiceTest {

    private static final DeviceCreateRequest CREATE =
            new DeviceCreateRequest("PLC-01", "1 号光栅", 7L, "loopback", "loopback://PLC-01");

    private final DeviceRepository deviceRepository = mock(DeviceRepository.class);

    private final DeviceGroupRepository deviceGroupRepository = mock(DeviceGroupRepository.class);

    private final DeviceEventRepository deviceEventRepository = mock(DeviceEventRepository.class);

    @BeforeEach
    void setUp() {
        when(deviceRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createStartsOfflineAndKeepsTheGivenGroup() {
        when(deviceRepository.countByDeviceCodeIncludingDeleted("PLC-01")).thenReturn(0L);
        when(deviceGroupRepository.findById(7L)).thenReturn(Optional.of(new DeviceGroupEntity()));

        var response = service().create(CREATE);

        assertThat(response.online()).isFalse();
        assertThat(response.groupId()).isEqualTo(7L);
        ArgumentCaptor<DeviceEntity> captor = ArgumentCaptor.forClass(DeviceEntity.class);
        verify(deviceRepository).save(captor.capture());
        assertThat(captor.getValue().isOnline()).isFalse();
    }

    /** 分组为空是合法的「未分组」，不该去查库。 */
    @Test
    void createWithoutGroupSkipsTheGroupExistenceCheck() {
        when(deviceRepository.countByDeviceCodeIncludingDeleted("PLC-01")).thenReturn(0L);

        service().create(new DeviceCreateRequest("PLC-01", "1 号光栅", null, "loopback", "loopback://PLC-01"));

        verify(deviceGroupRepository, never()).findById(any());
    }

    /** 唯一索引不区分逻辑删除，所以占位判断必须连已删除的一起算，否则要等插入才撞键、对外只是一次 500。 */
    @Test
    void createRejectsCodeOccupiedByADeletedDevice() {
        when(deviceRepository.countByDeviceCodeIncludingDeleted("PLC-01")).thenReturn(1L);

        assertThatThrownBy(() -> service().create(CREATE))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(EcsErrorCode.DEVICE_CODE_DUPLICATED);
    }

    @Test
    void createRejectsUnknownGroup() {
        when(deviceRepository.countByDeviceCodeIncludingDeleted("PLC-01")).thenReturn(0L);
        when(deviceGroupRepository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().create(CREATE))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(EcsErrorCode.DEVICE_GROUP_NOT_FOUND);
    }

    /** 设备编码不在修改入参里：它是 MQ 指令定位设备的依据，改了会让在途指令指向别的设备。 */
    @Test
    void updateNeverRewritesDeviceCode() {
        DeviceEntity stored = storedDevice();
        when(deviceRepository.findById(1L)).thenReturn(Optional.of(stored));
        when(deviceGroupRepository.findById(7L)).thenReturn(Optional.of(new DeviceGroupEntity()));

        var response = service().update(1L, new DeviceUpdateRequest("改名", 7L, "loopback", "loopback://renamed"));

        assertThat(response.deviceCode()).isEqualTo("PLC-01");
        assertThat(response.endpoint()).isEqualTo("loopback://renamed");
        assertThat(stored.getDeviceName()).isEqualTo("改名");
    }

    @Test
    void deleteOfUnknownDeviceFailsAsBusinessError() {
        when(deviceRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().delete(404L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(EcsErrorCode.DEVICE_NOT_FOUND);
        verify(deviceRepository, never()).delete(any());
    }

    @Test
    void deleteRemovesTheArchive() {
        DeviceEntity stored = storedDevice();
        when(deviceRepository.findById(1L)).thenReturn(Optional.of(stored));

        service().delete(1L);

        verify(deviceRepository).delete(stored);
    }

    /**
     * {@code orderBy} 来自客户端，未白名单的属性名会被拒掉：交给 {@code Sort} 就等于把列名与查询形状交出去。
     */
    @Test
    void pageRejectsSortFieldsOutsideTheWhitelist() {
        PageQuery query = new PageQuery();
        query.setOrderBy("(SELECT 1)");

        assertThatThrownBy(() -> service().page(query, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("orderBy");
        verify(deviceRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void pageAcceptsWhitelistedSortField() {
        PageQuery query = new PageQuery();
        query.setOrderBy("deviceCode");
        when(deviceRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), query.toPageable(), 0L));

        assertThat(service().page(query, null).getList()).isEmpty();
    }

    private DeviceEntity storedDevice() {
        DeviceEntity device = new DeviceEntity();
        device.setId(1L);
        device.setDeviceCode("PLC-01");
        device.setDeviceName("1 号光栅");
        device.setProtocolType("loopback");
        device.setEndpoint("loopback://PLC-01");
        device.markOnline(false);
        return device;
    }

    private DeviceService service() {
        return new DeviceService(deviceRepository, deviceGroupRepository, deviceEventRepository);
    }
}
