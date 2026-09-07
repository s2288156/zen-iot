package com.zen.common.core.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.zen.common.core.exception.BusinessException;
import com.zen.common.core.exception.GlobalErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

class GlobalExceptionHandlerTest {

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(new TestController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
  }

  @Test
  void businessExceptionReturnsItsOwnCode() throws Exception {
    mockMvc
        .perform(get("/test/business"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value(409))
        .andExpect(jsonPath("$.message").value("资源状态冲突"));
  }

  @Test
  void missingRequestParameterReturnsBadRequest() throws Exception {
    mockMvc
        .perform(get("/test/param"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value(400));
  }

  @Test
  void unsupportedMethodReturns405() throws Exception {
    mockMvc
        .perform(put("/test/business"))
        .andExpect(status().isMethodNotAllowed())
        .andExpect(jsonPath("$.code").value(405));
  }

  @Test
  void unmappedPathReturns404() throws Exception {
    mockMvc
        .perform(get("/test/never-mapped"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value(404))
        .andExpect(jsonPath("$.message").value("资源不存在"));
  }

  @Test
  void unexpectedExceptionNeverLeaksDetails() throws Exception {
    mockMvc
        .perform(get("/test/boom"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.code").value(500))
        .andExpect(jsonPath("$.message").value("系统异常"))
        .andExpect(jsonPath("$.data").doesNotExist());
  }

  @RestController
  static class TestController {

    @GetMapping("/test/business")
    String business() {
      throw new BusinessException(GlobalErrorCode.CONFLICT);
    }

    @GetMapping("/test/param")
    String param(@RequestParam("id") String id) {
      return id;
    }

    @GetMapping("/test/boom")
    String boom() {
      throw new IllegalStateException("internal-detail-should-not-leak");
    }
  }
}
