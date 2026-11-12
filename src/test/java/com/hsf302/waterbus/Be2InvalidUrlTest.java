package com.hsf302.waterbus;

import com.hsf302.waterbus.dto.AccountDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class Be2InvalidUrlTest {
    @Autowired private WebApplicationContext context;

    @Test
    void publicTripUrlsWithInvalidParametersReturnToSearch() throws Exception {
        MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).build();
        mvc.perform(get("/trips/abc/seats"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/trips"))
                .andExpect(flash().attributeExists("errorMessage"));
        mvc.perform(get("/trips").param("date", "not-a-date"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/trips"))
                .andExpect(flash().attributeExists("errorMessage"));
    }

    @Test
    void adminUrlsWithInvalidParametersReturnToTheirList() throws Exception {
        MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).build();
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("loginAccount", new AccountDTO(null, "admin@test.vn", "Admin", null,
                "ADMIN", "ACTIVE"));
        mvc.perform(post("/admin/trips/abc/cancel").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/trips"))
                .andExpect(flash().attributeExists("errorMessage"));
        mvc.perform(post("/admin/waterbuses/abc/status").param("status", "ACTIVE").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/waterbuses"))
                .andExpect(flash().attributeExists("errorMessage"));
        mvc.perform(post("/admin/waterbuses/1/status").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/waterbuses"))
                .andExpect(flash().attributeExists("errorMessage"));
        mvc.perform(post("/admin/refunds/abc/complete").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/refunds"))
                .andExpect(flash().attributeExists("errorMessage"));
        mvc.perform(get("/admin/revenue").param("from", "not-a-date").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/revenue"))
                .andExpect(flash().attributeExists("errorMessage"));
    }
}
