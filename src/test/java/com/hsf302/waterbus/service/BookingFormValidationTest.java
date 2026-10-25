package com.hsf302.waterbus.service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// BE4 M4: validate form POST /booking bằng BookingForm + @Valid + BindingResult.
@SpringBootTest
@Transactional
class BookingFormValidationTest {
    @Autowired private JdbcTemplate jdbc;
    @Autowired private WebApplicationContext context;

    private MockMvc mvc;
    private int tripId;
    private int seatId;
    private int ticketTypeId;
    private int s1;
    private int s2;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        jdbc.update("INSERT INTO station (code, name, is_active) VALUES ('TEST_S1', 'Ben mot', 1)");
        jdbc.update("INSERT INTO station (code, name, is_active) VALUES ('TEST_S2', 'Ben hai', 1)");
        s1 = id("station", "code", "TEST_S1");
        s2 = id("station", "code", "TEST_S2");
        jdbc.update("INSERT INTO route (code, name, is_active) VALUES ('TEST_R1', 'Tuyen test', 1)");
        int routeId = id("route", "code", "TEST_R1");
        jdbc.update("INSERT INTO route_station (route_id, station_id, stop_order, minutes_from_start) VALUES (?, ?, 1, 0)", routeId, s1);
        jdbc.update("INSERT INTO route_station (route_id, station_id, stop_order, minutes_from_start) VALUES (?, ?, 2, 0)", routeId, s2);
        jdbc.update("INSERT INTO waterbus (code, name, capacity, status) VALUES ('TEST_WB1', 'Tau test', 1, 'ACTIVE')");
        int busId = id("waterbus", "code", "TEST_WB1");
        jdbc.update("INSERT INTO seat (waterbus_id, seat_code, seat_row, seat_col, is_active) VALUES (?, 'A1', 1, 1, 1)", busId);
        seatId = jdbc.queryForObject("SELECT id FROM seat WHERE waterbus_id = ?", Integer.class, busId);
        LocalDateTime departure = LocalDateTime.now().plusDays(1);
        jdbc.update("INSERT INTO trip (trip_code, route_id, waterbus_id, departure_time, arrival_time, status) "
                + "VALUES ('TEST_TRIP1', ?, ?, ?, ?, 'SCHEDULED')", routeId, busId, departure, departure.plusMinutes(30));
        tripId = id("trip", "trip_code", "TEST_TRIP1");
        jdbc.update("INSERT INTO ticket_type (code, name, price, requires_verification, is_active) "
                + "VALUES ('TEST_STD', 'Ve thuong', 15000, 0, 1)");
        ticketTypeId = id("ticket_type", "code", "TEST_STD");
    }

    @Test
    void wrongContactShowsCheckoutAgainWithErrorUnderEachField() throws Exception {
        String html = mvc.perform(booking("  ", "12345", "abc", "X".repeat(31)))
                .andExpect(status().isOk())
                .andExpect(view().name("booking/checkout"))
                // Mỗi ô sai có lỗi riêng trong BindingResult "bookingForm" -> template hiện được th:errors
                .andExpect(model().attributeHasFieldErrors("bookingForm",
                        "contactName", "contactPhone", "contactEmail", "promoCode"))
                // Vẫn đủ model như trước để trang checkout vẽ lại được
                .andExpect(model().attributeExists("trip", "seats", "ticketType", "subtotal",
                        "fromStation", "toStation", "fromStationId", "toStationId", "errorMessage"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(html).contains("Vui lòng nhập họ và tên.", "Số điện thoại phải gồm 10 chữ số",
                "Email không đúng định dạng.", "Mã khuyến mãi tối đa 30 ký tự.",
                "value=\"12345\"", "value=\"abc\""); // giữ lại giá trị khách vừa gõ
        assertThat(count("booking")).isZero(); // nhập sai -> chưa tạo đơn
    }

    @Test
    void emptyPhoneOnlyAsksToEnterPhone() throws Exception {
        mvc.perform(booking("Lan", "", "lan@example.com", ""))
                .andExpect(view().name("booking/checkout"))
                .andExpect(model().attributeHasFieldErrorCode("bookingForm", "contactPhone", "NotBlank"))
                .andExpect(model().attribute("errorMessage", "Vui lòng nhập số điện thoại."));
    }

    @Test
    void noSeatOrMissingTripIsRejected() throws Exception {
        mvc.perform(post("/booking")
                        .param("tripId", String.valueOf(tripId))
                        .param("ticketTypeId", String.valueOf(ticketTypeId))
                        .param("fromStationId", String.valueOf(s1)).param("toStationId", String.valueOf(s2))
                        .param("contactName", "Lan").param("contactPhone", "0901234567")
                        .param("contactEmail", "lan@example.com"))
                .andExpect(view().name("booking/checkout"))
                .andExpect(model().attributeHasFieldErrors("bookingForm", "seatIds"))
                .andExpect(model().attribute("errorMessage", "Vui lòng chọn ít nhất 1 ghế."));

        mvc.perform(post("/booking") // thiếu tripId (form bị sửa tay) -> 400 như trước
                        .param("seatIds", String.valueOf(seatId))
                        .param("ticketTypeId", String.valueOf(ticketTypeId))
                        .param("fromStationId", String.valueOf(s1)).param("toStationId", String.valueOf(s2))
                        .param("contactName", "Lan").param("contactPhone", "0901234567")
                        .param("contactEmail", "lan@example.com"))
                .andExpect(status().isBadRequest());
        assertThat(count("booking")).isZero();
    }

    @Test
    void validFormStillCreatesBookingAndTrimsSpaces() throws Exception {
        String location = mvc.perform(booking(" Lan ", " 0901234567 ", " lan@example.com ", " "))
                .andExpect(status().is3xxRedirection())
                .andReturn().getResponse().getRedirectedUrl();
        assertThat(location).matches("/payment/BK[0-9]{8}");
        assertThat(jdbc.queryForObject("SELECT contact_phone FROM booking", String.class)).isEqualTo("0901234567");
        assertThat(jdbc.queryForObject("SELECT contact_name FROM booking", String.class)).isEqualTo("Lan");
    }

    @Test
    void checkoutPageAlwaysHasEmptyBookingForm() throws Exception {
        mvc.perform(post("/booking/checkout")
                        .param("tripId", String.valueOf(tripId)).param("seatIds", String.valueOf(seatId))
                        .param("ticketTypeId", String.valueOf(ticketTypeId))
                        .param("fromStationId", String.valueOf(s1)).param("toStationId", String.valueOf(s2)))
                .andExpect(view().name("booking/checkout"))
                .andExpect(model().attributeExists("bookingForm"));
    }

    // Form ở trang checkout gửi lên, đủ chuyến/ghế/bến; chỉ thay phần khách tự nhập
    private MockHttpServletRequestBuilder booking(String name, String phone, String email, String promo) {
        return post("/booking")
                .param("tripId", String.valueOf(tripId)).param("seatIds", String.valueOf(seatId))
                .param("ticketTypeId", String.valueOf(ticketTypeId))
                .param("fromStationId", String.valueOf(s1)).param("toStationId", String.valueOf(s2))
                .param("contactName", name).param("contactPhone", phone)
                .param("contactEmail", email).param("promoCode", promo);
    }

    private int id(String table, String codeColumn, String code) {
        return jdbc.queryForObject("SELECT id FROM " + table + " WHERE " + codeColumn + " = ?", Integer.class, code);
    }

    private int count(String table) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
    }
}
