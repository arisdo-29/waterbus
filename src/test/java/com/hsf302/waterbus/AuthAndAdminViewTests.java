package com.hsf302.waterbus;

import com.hsf302.waterbus.dto.AccountDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.jpa.open-in-view=false")
@Transactional
class AuthAndAdminViewTests {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mvc;
    private MockHttpSession adminSession;
    private MockHttpSession customerSession;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();

        adminSession = new MockHttpSession();
        adminSession.setAttribute("loginAccount", new AccountDTO(1, "admin@test.com", "Admin Tân", "0900000001", "ADMIN", "ACTIVE"));

        customerSession = new MockHttpSession();
        customerSession.setAttribute("loginAccount", new AccountDTO(2, "user@test.com", "Khách Hàng", "0900000002", "CUSTOMER", "ACTIVE"));
    }

    @Test
    void testLoginPageRenders() throws Exception {
        mvc.perform(get("/auth/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"))
                .andExpect(model().attributeExists("loginRequest"));
    }

    @Test
    void testRegisterPageRenders() throws Exception {
        mvc.perform(get("/auth/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().attributeExists("registerRequest"));
    }

    @Test
    void testAdminPromotionsListPageRenders() throws Exception {
        mvc.perform(get("/admin/promotions").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/promotion/list"))
                .andExpect(model().attributeExists("promotions"));
    }

    @Test
    void testAdminPromotionsCreatePageRenders() throws Exception {
        mvc.perform(get("/admin/promotions/create").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/promotion/form"))
                .andExpect(model().attributeExists("promotion"));
    }

    @Test
    void testAdminTicketTypesListPageRenders() throws Exception {
        mvc.perform(get("/admin/ticket-types").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/tickettype/list"))
                .andExpect(model().attributeExists("ticketTypes"));
    }

    @Test
    void testAdminTicketTypesCreatePageRenders() throws Exception {
        mvc.perform(get("/admin/ticket-types/create").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/tickettype/form"))
                .andExpect(model().attributeExists("ticketType"));
    }

    @Test
    void testAccountProfilePageRenders() throws Exception {
        mvc.perform(get("/account/profile").session(customerSession))
                .andExpect(status().isOk())
                .andExpect(view().name("account/profile"))
                .andExpect(model().attributeExists("account", "updateProfileRequest"));
    }

    @Test
    void testAccountChangePasswordPageRenders() throws Exception {
        mvc.perform(get("/account/change-password").session(customerSession))
                .andExpect(status().isOk())
                .andExpect(view().name("account/change-password"))
                .andExpect(model().attributeExists("changePasswordRequest"));
    }
}
