package org.zeniot.server.controller;

import org.junit.jupiter.api.Test;
import org.zeniot.server.controller.request.TAccount;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Wu.Chunyang
 */
public class AccountControllerTest extends AbstractControllerTest {

    @Test
    void test_account_register() throws Exception {
        TAccount admin = TAccount.admin();
        doPost("/api/account/register", admin)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value(admin.getUsername()));
    }

    @Test
    void test_account_delete() {

    }
}
