package org.zen.iot.common.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

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
        Assertions.assertTrue(JacksonUtil.isJson(str1));
        Assertions.assertFalse(JacksonUtil.isJson(str2));
        Assertions.assertTrue(JacksonUtil.isJson(str3));
        Assertions.assertTrue(JacksonUtil.isJson(str4));
        Assertions.assertTrue(JacksonUtil.isJson(str5));
    }
}
