package org.zeniot.server.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.zeniot.common.util.JacksonUtil;

import java.io.UnsupportedEncodingException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Wu.Chunyang
 */
@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
public class AbstractControllerTest {
    @Autowired
    protected MockMvc mockMvc;

    protected ResultActions doPost(String urlTemplate, Object content) throws Exception {

        MockHttpServletRequestBuilder requestBuilder;
        if (content != null) {
            requestBuilder = post(urlTemplate).contentType(MediaType.APPLICATION_JSON).content(toJson(content));
        } else {
            requestBuilder = post(urlTemplate).contentType(MediaType.APPLICATION_JSON);
        }
        return mockMvc.perform(requestBuilder);
    }

    protected ResultActions doMultipart(String urlTemplate, String name, byte[] content) throws Exception {
        return mockMvc.perform(multipart(urlTemplate).file(name, content))
                .andExpect(status().isOk());
    }

    protected ResultActions doDelete(String urlTemplate) throws Exception {
        return mockMvc.perform(delete(urlTemplate).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    protected ResultActions doGet(String urlTemplate) throws Exception {
        return mockMvc.perform(get(urlTemplate).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    protected ResultActions doGet(String urlTemplate, String content) throws Exception {
        return mockMvc.perform(get(urlTemplate).contentType(MediaType.APPLICATION_JSON).content(content))
                .andExpect(status().isOk());
    }

    private String toJson(Object content) {
        return JacksonUtil.toString(content);
    }

    protected <T> T getResponse(ResultActions resultActions, Class<T> responseClass) throws UnsupportedEncodingException {
        return JacksonUtil.convertValue(resultActions.andReturn().getResponse().getContentAsString(), responseClass);
    }

}
