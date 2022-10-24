package org.zeniot.server.controller;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;
import org.zeniot.common.util.JacksonUtil;
import org.zeniot.data.PageQuery;
import org.zeniot.data.SingleResponse;
import org.zeniot.dto.account.Account;
import org.zeniot.server.controller.request.TAccount;

import java.io.UnsupportedEncodingException;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Wu.Chunyang
 */
@Slf4j
public class AccountControllerTest extends AbstractControllerTest {

    public static final String API_ACCOUNT_REGISTER = "/api/account/register";
    public static final String API_ACCOUNT = "/api/account/";
    TAccount admin;

    private Long afterCleanAccountId;

    @AfterEach
    void tearDown() throws Exception {
        if (afterCleanAccountId != null) {
            doDelete(API_ACCOUNT + afterCleanAccountId);
        }
    }

    @BeforeEach
    void setUp() {
        admin = TAccount.admin();
    }

    @Test
    void test_query_account_list() throws Exception {
        TAccount.accountList().forEach(account -> {
            try {
                doPost(API_ACCOUNT_REGISTER, account);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        PageQuery pageQuery = new PageQuery();
        pageQuery.setPage(0);
        pageQuery.setSize(2);
        doGet("/api/accounts", JacksonUtil.toString(pageQuery))
                .andExpect(jsonPath("$.data.size()").value(2))
                .andExpect(jsonPath("$.totalPages").value(5))
                .andExpect(jsonPath("$.totalElements").value(10));
    }

    @Test
    void test_query_account_by_id() throws Exception {
        ResultActions resultActions = doPost(API_ACCOUNT_REGISTER, admin);
        Account account = extractAccount(resultActions);
        afterCleanAccountId = account.getId();
        doGet(API_ACCOUNT + account.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accountId.id").value(account.getId()));
    }

    @Test
    void test_account_register() throws Exception {
        ResultActions resultActions = doPost(API_ACCOUNT_REGISTER, admin)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value(admin.getUsername()));
        Account account = extractAccount(resultActions);
        afterCleanAccountId = account.getId();
    }

    @Test
    void test_account_delete() throws Exception {
        ResultActions resultActions = doPost(API_ACCOUNT_REGISTER, admin);
        Account account = extractAccount(resultActions);
        Long accountId = account.getId();
        doDelete(API_ACCOUNT + accountId)
                .andExpect(status().isOk());
        doGet(API_ACCOUNT + accountId)
                .andExpect(jsonPath("$.data").isEmpty());
    }

    private Account extractAccount(ResultActions resultActions) throws UnsupportedEncodingException {
        SingleResponse response = getResponse(resultActions, SingleResponse.class);
        return JacksonUtil.convertValue(response.getData(), Account.class);
    }
}
