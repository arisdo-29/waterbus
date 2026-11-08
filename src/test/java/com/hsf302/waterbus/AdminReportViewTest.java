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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
class AdminReportViewTest {
    @Autowired private WebApplicationContext context;

    @Test
    void rendersRevenueAndRefundPagesForAdmin() throws Exception {
        var mvc = MockMvcBuilders.webAppContextSetup(context).build();
        var session = new MockHttpSession();
        session.setAttribute("loginAccount", new AccountDTO(null, "admin@test.vn", "Admin", null,
                "ADMIN", "ACTIVE"));

        String revenue = mvc.perform(get("/admin/revenue").session(session))
                .andExpect(status().isOk()).andExpect(view().name("admin/revenue/index"))
                .andReturn().getResponse().getContentAsString();
        assertTrue(revenue.contains("Doanh thu theo ngày"));
        assertTrue(revenue.contains("chart.umd.min.js"));

        String refunds = mvc.perform(get("/admin/refunds").session(session))
                .andExpect(status().isOk()).andExpect(view().name("admin/refund/list"))
                .andReturn().getResponse().getContentAsString();
        assertTrue(refunds.contains("Chưa có khoản hoàn tiền"));
    }
}
