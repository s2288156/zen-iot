package org.zeniot;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * @author Wu.Chunyang
 */
@Slf4j
public class DemoTest {

    @Test
    void testBcrypt() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String inputPwd = "123123";
//        String encodePwd = encoder.encode(inputPwd);
        String encodePwd = "$2a$10$192rKbVwo2TGc0mOw//.AeDZdBmVZappJ.ywohE2L/pCH4eSTPdEa";
        log.info("inputPwd: {}, encodePwd: {}", inputPwd, encodePwd);
        log.warn("{}", encoder.matches(inputPwd, encodePwd));
    }
}
