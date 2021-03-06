package org.zeniot.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.zeniot.common.util.JacksonUtil;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Wu.Chunyang
 */
@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
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
        return mockMvc.perform(requestBuilder)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    protected ResultActions doMultipart(String urlTemplate,String name, byte[] content) throws Exception {
        return mockMvc.perform(multipart(urlTemplate).file(name, content))
                .andExpect(status().isOk());
    }

    protected ResultActions doDelete(String urlTemplate) throws Exception {
        return mockMvc.perform(delete(urlTemplate).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    protected ResultActions doGet(String urlTemplate) throws Exception {
        return mockMvc.perform(get(urlTemplate).contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    private String toJson(Object content) {
        return JacksonUtil.toString(content);
    }

    protected <T> T getResponse(ResultActions resultActions, Class<T> responseClass) throws UnsupportedEncodingException {
        return JacksonUtil.fromString(resultActions.andReturn().getResponse().getContentAsString(), responseClass);
    }

    protected <T> T getResponse(ResultActions resultActions, TypeReference<T> responseType) throws IOException {
        byte[] content = resultActions.andReturn().getResponse().getContentAsByteArray();
        return JacksonUtil.OBJECT_MAPPER.readerFor(responseType).readValue(content);
    }
}
