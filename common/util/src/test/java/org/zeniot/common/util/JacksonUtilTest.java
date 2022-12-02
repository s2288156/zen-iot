package org.zeniot.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.zeniot.common.util.JacksonUtil.isJson;

/**
 * @author Wu.Chunyang
 */
class JacksonUtilTest {

    @Test
    void test_isJson() {
        String str1 = "";
        String str2 = "[";
        String str3 = "{}";
        String str4 = "[]";
        String str5 = """
                {"name": "Lao Wang"}
                """;
        assertTrue(isJson(str1));
        assertFalse(isJson(str2));
        assertTrue(isJson(str3));
        assertTrue(isJson(str4));
        assertTrue(isJson(str5));
    }
}
