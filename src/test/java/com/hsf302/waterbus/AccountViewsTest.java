package com.hsf302.waterbus;

import com.hsf302.waterbus.dto.AccountDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class AccountViewsTest {
    @Autowired private WebApplicationContext context;

    @Test
    void profileShowsPhoneRulesAndUnchangedState() throws Exception {
        var mvc = MockMvcBuilders.webAppContextSetup(context).build();
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("loginAccount", new AccountDTO(1, "tri@test.vn", "Trí", "0900000000", "CUSTOMER", "ACTIVE"));

        String profile = mvc.perform(get("/account/profile").session(session))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertTrue(profile.contains("pattern=\"0[0-9]{9}\""));
        assertTrue(profile.contains("maxlength=\"10\""));
        assertTrue(profile.contains("inputmode=\"numeric\""));
        assertTrue(profile.contains("Chưa thay đổi thông tin nào."));
        assertTrue(profile.contains("data-original-name=\"Trí\""));
        assertTrue(profile.contains("data-original-phone=\"0900000000\""));

        String password = mvc.perform(get("/account/change-password").session(session))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertTrue(password.contains("Mật khẩu mới phải khác mật khẩu hiện tại."));
    }
}
