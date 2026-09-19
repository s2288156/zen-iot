package com.zen.ecs.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.zen.common.core.page.PageQuery;
import com.zen.common.core.page.PageResult;
import com.zen.common.core.web.GlobalExceptionHandler;
import com.zen.common.security.error.BusinessException;
import com.zen.ecs.dto.CommandResponse;
import com.zen.ecs.dto.DeviceGroupResponse;
import com.zen.ecs.dto.DeviceResponse;
import com.zen.ecs.entity.CommandStatus;
import com.zen.ecs.error.EcsErrorCode;
import com.zen.ecs.service.CommandDispatchService;
import com.zen.ecs.service.DeviceGroupService;
import com.zen.ecs.service.DeviceService;
import com.zen.ecs.service.HeartbeatService;
import java.util.List;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * 入口层契约：standalone MockMvc，只注册被测 controller 与 {@link GlobalExceptionHandler}。
 *
 * <p>不用 {@code @WebMvcTest}：common-core 的安全自动配置会在切片上下文里装配拦截器，未带身份的测试请求会直接 401，
 * 那是 {@code AuthInterceptor} 的行为、不是这些 controller 的。鉴权与模块校验留给经网关的端到端。
 */
@ExtendWith(MockitoExtension.class)
class DeviceControllerTest {

    private static final DeviceResponse DEVICE =
            new DeviceResponse(1L, "PLC-01", "1 号光栅", 7L, "loopback", "loopback://PLC-01", false, null);

    private MockMvc mockMvc;

    @Mock
    private DeviceService deviceService;

    @Mock
    private HeartbeatService heartbeatService;

    @Mock
    private DeviceGroupService deviceGroupService;

    @Mock
    private CommandDispatchService commandDispatchService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new DeviceController(deviceService),
                        new HeartbeatController(heartbeatService),
                        new DeviceGroupController(deviceGroupService),
                        new CommandController(commandDispatchService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createReturnsTheArchiveOnValidRequest() throws Exception {
        when(deviceService.create(any())).thenReturn(DEVICE);

        mockMvc.perform(post("/device").contentType(MediaType.APPLICATION_JSON).content("""
                                {
                                  "deviceCode": "PLC-01",
                                  "deviceName": "1 号光栅",
                                  "groupId": 7,
                                  "protocolType": "loopback",
                                  "endpoint": "loopback://PLC-01"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.deviceCode").value("PLC-01"))
                .andExpect(jsonPath("$.data.online").value(false));
    }

    @Test
    void createWithBlankDeviceCodeReturns400() throws Exception {
        mockMvc.perform(post("/device").contentType(MediaType.APPLICATION_JSON).content("""
                                { "deviceCode": "", "deviceName": "x", "protocolType": "loopback", "endpoint": "e" }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(Matchers.containsString("deviceCode")));

        verify(deviceService, never()).create(any());
    }

    @Test
    void updatePassesThePathIdToTheService() throws Exception {
        when(deviceService.update(eq(1L), any())).thenReturn(DEVICE);

        mockMvc.perform(put("/device/1").contentType(MediaType.APPLICATION_JSON).content("""
                                { "deviceName": "改名", "protocolType": "loopback", "endpoint": "loopback://x" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.deviceName").value("1 号光栅"));
    }

    /**
     * 业务码 1xxx 不是 HTTP 状态：{@code GlobalExceptionHandler} 让 HTTP 回落 200，错误由响应体 code 表达。
     * 这条契约随 ECS 第一次落地，写死在这里免得被后人当成 bug 改掉。
     */
    @Test
    void missingDeviceReportsBusinessCodeWithHttpOk() throws Exception {
        when(deviceService.get(404L)).thenThrow(new BusinessException(EcsErrorCode.DEVICE_NOT_FOUND));

        mockMvc.perform(get("/device/404"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1001))
                .andExpect(jsonPath("$.message").value("设备不存在"));
    }

    @Test
    void pageForwardsPaginationAndGroupFilter() throws Exception {
        when(deviceService.page(any(PageQuery.class), eq(7L))).thenReturn(PageResult.empty());

        mockMvc.perform(get("/device")
                        .param("groupId", "7")
                        .param("pageNum", "2")
                        .param("pageSize", "5"))
                .andExpect(status().isOk());

        ArgumentCaptor<PageQuery> captor = ArgumentCaptor.forClass(PageQuery.class);
        verify(deviceService).page(captor.capture(), eq(7L));
        assertThat(captor.getValue().getPageNum()).isEqualTo(2);
    }

    @Test
    void heartbeatIsAddressedByDeviceCodeNotById() throws Exception {
        mockMvc.perform(post("/heartbeat/PLC-01")).andExpect(status().isOk());

        verify(heartbeatService).report("PLC-01");
    }

    @Test
    void groupListIsReturnedAsDtos() throws Exception {
        when(deviceGroupService.list()).thenReturn(List.of(new DeviceGroupResponse(7L, "line-1", "一号产线", null)));

        mockMvc.perform(get("/device-group"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].groupCode").value("line-1"));
    }

    @Test
    void deletingAGroupStillHoldingDevicesReturnsItsBusinessCode() throws Exception {
        doThrow(new BusinessException(EcsErrorCode.DEVICE_GROUP_IN_USE))
                .when(deviceGroupService)
                .delete(7L);

        mockMvc.perform(delete("/device-group/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1005));
    }

    @Test
    void commandStatusIsReadableByCommandNo() throws Exception {
        when(commandDispatchService.get("RC-1"))
                .thenReturn(new CommandResponse(1L, "RC-1", 1L, "MOVE", "pos-3", CommandStatus.SUCCESS, 0, null, null));

        mockMvc.perform(get("/command/RC-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUCCESS"));
    }
}
