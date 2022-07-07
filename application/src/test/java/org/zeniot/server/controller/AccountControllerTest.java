package org.zeniot.server.controller;

import org.junit.jupiter.api.Test;
import org.zeniot.server.controller.request.TAccount;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * @author Wu.Chunyang
 */
public class AccountControllerTest extends AbstractControllerTest {

    @Test
    void test_account_register() throws Exception {
        doPost("/api/account/register", TAccount.admin())
                .andExpect(jsonPath("$.msg").value("Ok!"));
    }
}
