package org.zeniot.server.controller;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;
import org.zeniot.common.util.JacksonUtil;
import org.zeniot.server.controller.request.TAccount;
import org.zeniot.server.controller.response.RestResponse;
import org.zeniot.server.dto.account.Account;

import java.io.UnsupportedEncodingException;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Wu.Chunyang
 */
@Slf4j
public class AccountControllerTest extends AbstractControllerTest {

    TAccount admin;

    @BeforeEach
    void setUp() {
        admin = TAccount.admin();
    }

    @Test
    void test_query_account_by_id() throws Exception {
        ResultActions resultActions = doPost("/api/account/register", admin);
        Account account = extractAccount(resultActions);
        doGet("/api/account/" + account.getAccountId().getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accountId.id").value(account.getAccountId().getId()));
    }

    @Test
    void test_account_register() throws Exception {
        ResultActions resultActions = doPost("/api/account/register", admin)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value(admin.getUsername()));
        Account account = extractAccount(resultActions);
        doDelete("/api/account/" + account.getAccountId().getId());
    }

    @Test
    void test_account_delete() throws Exception {
        ResultActions resultActions = doPost("/api/account/register", admin);
        Account account = extractAccount(resultActions);
        doDelete("/api/account/" + account.getAccountId().getId())
                .andExpect(status().isOk());
        doGet("/api/account/" + account.getAccountId().getId())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    private Account extractAccount(ResultActions resultActions) throws UnsupportedEncodingException {
        RestResponse response = getResponse(resultActions, RestResponse.class);
        return JacksonUtil.convertValue(response.getData(), Account.class);
    }
}
