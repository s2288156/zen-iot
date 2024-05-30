package org.zeniot.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Jack Wu
 */
class PatternValidatorTest {
    @Test
    void isAmsNetId_test() {
        assertTrue(PatternValidator.isAmsNetId("127.0.0.1.1.1"));
        assertTrue(PatternValidator.isAmsNetId("0.0.0.0.0.0"));
        assertTrue(PatternValidator.isAmsNetId("255.255.255.255.255.255"));
        assertFalse(PatternValidator.isAmsNetId("0.0.0.0"));
    }
}
