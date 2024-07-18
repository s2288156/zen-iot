package org.zen.iot.common.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Wu.Chunyang
 */
@Slf4j
public class JacksonUtil {
    private static final ObjectMapper OBJECT_MAPPER;

    static {
        OBJECT_MAPPER = new ObjectMapper();
        OBJECT_MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    public static ObjectNode newObjectNode() {
        return OBJECT_MAPPER.createObjectNode();
    }

    /**
     * <pre>
     * isJson("")     = true
     * isJson("{}")   = true
     * isJson("[]")   = true
     * isJson("[")    = false
     * isJson("}")    = false
     * </pre>
     *
     * @param str check the str is json
     * @return {@code true} if the str is json
     */
    public static boolean isJson(String str) {
        try {
            OBJECT_MAPPER.readTree(str);
        } catch (IllegalArgumentException | JsonProcessingException e) {
            log.warn("The string is not json: {}", e.getLocalizedMessage());
            return false;
        }
        return true;
    }

    public static <T> T readValue(String content, Class<T> toValueType) {
        try {
            return OBJECT_MAPPER.readValue(content, toValueType);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T convertValue(Object fromValue, Class<T> toValueType) {
        return OBJECT_MAPPER.convertValue(fromValue, toValueType);
    }

    public static <T> T convertValue(Object fromValue, TypeReference<T> toValueType) {
        return OBJECT_MAPPER.convertValue(fromValue, toValueType);
    }

    public static String toJsonStr(Object value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

}
