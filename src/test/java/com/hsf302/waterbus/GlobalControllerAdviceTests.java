package com.hsf302.waterbus;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Kiểm tra GlobalControllerAdvice: URL sai kiểu / thiếu tham số -> về trang chủ kèm errorMessage
@SpringBootTest
class GlobalControllerAdviceTests {
    private static final String MESSAGE = "Đường dẫn không hợp lệ hoặc thiếu thông tin.";

    @Autowired private WebApplicationContext context;

    @Test
    void wrongTypeInUrlRedirectsHomeWithMessage() throws Exception {
        MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).build();
        mvc.perform(get("/stations/abc"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(flash().attribute("errorMessage", MESSAGE));
    }

    @Test
    void missingRequiredParameterRedirectsHomeWithMessage() throws Exception {
        MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).build();
        // Thiếu tripId (bắt buộc) -> MissingServletRequestParameterException
        mvc.perform(post("/booking/checkout").param("ticketTypeId", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(flash().attribute("errorMessage", MESSAGE));
    }

    @Test
    void homePageStillWorks() throws Exception {
        MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).build();
        mvc.perform(get("/")).andExpect(status().isOk());
    }
}
