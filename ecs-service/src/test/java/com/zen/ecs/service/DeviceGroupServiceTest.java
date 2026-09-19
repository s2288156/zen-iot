package com.zen.ecs.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zen.common.security.error.BusinessException;
import com.zen.ecs.dto.DeviceGroupRequest;
import com.zen.ecs.dto.DeviceGroupUpdateRequest;
import com.zen.ecs.entity.DeviceGroupEntity;
import com.zen.ecs.error.EcsErrorCode;
import com.zen.ecs.repository.DeviceGroupRepository;
import com.zen.ecs.repository.DeviceRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** 分组的两条业务规则：编码不许重复占用，组内还有设备就不许删。 */
class DeviceGroupServiceTest {

    private final DeviceGroupRepository deviceGroupRepository = mock(DeviceGroupRepository.class);

    private final DeviceRepository deviceRepository = mock(DeviceRepository.class);

    @BeforeEach
    void setUp() {
        when(deviceGroupRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createStoresTheGivenGroupCode() {
        when(deviceGroupRepository.countByGroupCodeIncludingDeleted("line-1")).thenReturn(0L);

        var response = service().create(new DeviceGroupRequest("line-1", "一号产线", "装配线"));

        assertThat(response.groupCode()).isEqualTo("line-1");
        ArgumentCaptor<DeviceGroupEntity> captor = ArgumentCaptor.forClass(DeviceGroupEntity.class);
        verify(deviceGroupRepository).save(captor.capture());
        assertThat(captor.getValue().getDescription()).isEqualTo("装配线");
    }

    @Test
    void createRejectsCodeOccupiedByADeletedGroup() {
        when(deviceGroupRepository.countByGroupCodeIncludingDeleted("line-1")).thenReturn(1L);

        assertThatThrownBy(() -> service().create(new DeviceGroupRequest("line-1", "一号产线", null)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(EcsErrorCode.DEVICE_GROUP_CODE_DUPLICATED);
    }

    /** 分组编码是设备档案引用它的方式，改码会让既有档案指向别的分组，因此改名只动名称与描述。 */
    @Test
    void updateNeverRewritesGroupCode() {
        DeviceGroupEntity stored = stored();
        when(deviceGroupRepository.findById(7L)).thenReturn(Optional.of(stored));

        service().update(7L, new DeviceGroupUpdateRequest("改名", null));

        assertThat(stored.getGroupCode()).isEqualTo("line-1");
        assertThat(stored.getGroupName()).isEqualTo("改名");
    }

    @Test
    void deleteIsRefusedWhileTheGroupStillHoldsDevices() {
        DeviceGroupEntity stored = stored();
        when(deviceGroupRepository.findById(7L)).thenReturn(Optional.of(stored));
        when(deviceRepository.countByGroupId(7L)).thenReturn(2L);

        assertThatThrownBy(() -> service().delete(7L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(EcsErrorCode.DEVICE_GROUP_IN_USE);
        verify(deviceGroupRepository, never()).delete(any());
    }

    @Test
    void deleteSucceedsOnceTheGroupIsEmpty() {
        DeviceGroupEntity stored = stored();
        when(deviceGroupRepository.findById(7L)).thenReturn(Optional.of(stored));
        when(deviceRepository.countByGroupId(7L)).thenReturn(0L);

        service().delete(7L);

        verify(deviceGroupRepository).delete(stored);
    }

    private DeviceGroupEntity stored() {
        DeviceGroupEntity group = new DeviceGroupEntity();
        group.setId(7L);
        group.setGroupCode("line-1");
        group.setGroupName("一号产线");
        return group;
    }

    private DeviceGroupService service() {
        return new DeviceGroupService(deviceGroupRepository, deviceRepository);
    }
}
