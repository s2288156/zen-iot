package org.zeniot.server.security;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.zeniot.server.AbstractBootTest;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Wu.Chunyang
 */
@Slf4j
class JwtHandlerTest extends AbstractBootTest {

    private UserDetails admin;
    private UserDetails guest;

    @Autowired
    private JwtHandler jwtHandler;

    @BeforeEach
    void setUp() {
        admin = new User("admin", "123123", Collections.emptySet());
        guest = new User("guest", "123123", Collections.emptySet());
    }

    @Test
    void test_getUsernameForToken() {
        String adminToken = jwtHandler.newToken(admin);
        String username = jwtHandler.getUsernameForToken(adminToken);
        assertEquals(admin.getUsername(), username);
    }

    @Test
    void test_newToken() throws InterruptedException {
        String token1 = jwtHandler.newToken(admin);
        TimeUnit.SECONDS.sleep(1);
        String token2 = jwtHandler.newToken(admin);
        assertTrue(StringUtils.isNotBlank(token1));
        assertNotEquals(token1, token2);
    }

    @Test
    void test_verifyToken_when_success() throws InterruptedException {
        String adminToken = jwtHandler.newToken(admin);
        String guestToken = jwtHandler.newToken(guest);
        assertTrue(jwtHandler.verifyToken(adminToken, admin));
        assertTrue(jwtHandler.verifyToken(guestToken, guest));

//        assertFalse(jwtHandler.verifyToken(adminToken, guest));
//        TimeUnit.SECONDS.sleep(3);
//        assertFalse(jwtHandler.verifyToken(adminToken, admin));
    }

}
