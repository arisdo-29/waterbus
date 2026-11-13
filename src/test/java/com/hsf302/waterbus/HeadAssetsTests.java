package com.hsf302.waterbus;

import com.hsf302.waterbus.dto.AccountDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class HeadAssetsTests {
    @Autowired WebApplicationContext context;

    @Test
    void adminCssOnlyOnAdminPagesAndNoGoogleFonts() throws Exception {
        MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).build();
        MockHttpSession admin = new MockHttpSession();
        admin.setAttribute("loginAccount", new AccountDTO(null, "admin@test.vn", "Admin", null, "ADMIN", "ACTIVE"));

        String adminPage = mvc.perform(get("/admin/revenue").session(admin)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(adminPage).contains("href=\"/css/app.css\"", "href=\"/css/admin.css\"")
                .doesNotContain("fonts.googleapis.com", "fonts.gstatic.com");

        String guestPage = mvc.perform(get("/")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(guestPage).contains("href=\"/css/app.css\"")
                .doesNotContain("admin.css", "fonts.googleapis.com", "fonts.gstatic.com");
    }
}
