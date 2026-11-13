package com.hsf302.waterbus.service;

import com.hsf302.waterbus.dto.AccountDTO;
import com.hsf302.waterbus.entity.Booking;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// BE4 TR2: chỉ chủ đơn, nhân viên, hoặc trình duyệt đã được phép (vừa đặt / nhập đúng SĐT)
// mới xem, huỷ, thanh toán được 1 đơn. Mã đơn chỉ 8 chữ số nên không được coi là "bí mật".
@SpringBootTest
@Transactional
class BookingAccessTest {
    private static final String NOT_OWNER_MESSAGE = "Bạn không có quyền xem đơn đặt vé này.";

    @Autowired private JdbcTemplate jdbc;
    @Autowired private BookingService bookingService;
    @Autowired private WebApplicationContext context;

    private MockMvc mvc;
    private int tripId;
    private int seatA1;
    private int seatA2;
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
        jdbc.update("INSERT INTO waterbus (code, name, capacity, status) VALUES ('TEST_WB1', 'Tau test', 2, 'ACTIVE')");
        int busId = id("waterbus", "code", "TEST_WB1");
        jdbc.update("INSERT INTO seat (waterbus_id, seat_code, seat_row, seat_col, is_active) VALUES (?, 'A1', 1, 1, 1)", busId);
        jdbc.update("INSERT INTO seat (waterbus_id, seat_code, seat_row, seat_col, is_active) VALUES (?, 'A2', 1, 2, 1)", busId);
        seatA1 = jdbc.queryForObject("SELECT id FROM seat WHERE waterbus_id = ? AND seat_code = 'A1'", Integer.class, busId);
        seatA2 = jdbc.queryForObject("SELECT id FROM seat WHERE waterbus_id = ? AND seat_code = 'A2'", Integer.class, busId);
        LocalDateTime departure = LocalDateTime.now().plusDays(1);
        jdbc.update("INSERT INTO trip (trip_code, route_id, waterbus_id, departure_time, arrival_time, status) "
                + "VALUES ('TEST_TRIP1', ?, ?, ?, ?, 'SCHEDULED')", routeId, busId, departure, departure.plusMinutes(30));
        tripId = id("trip", "trip_code", "TEST_TRIP1");
        jdbc.update("INSERT INTO ticket_type (code, name, price, requires_verification, is_active) "
                + "VALUES ('TEST_STD', 'Ve thuong', 15000, 0, 1)");
        ticketTypeId = id("ticket_type", "code", "TEST_STD");
    }

    @Test
    void otherAccountCannotViewCancelOrPayMyBooking() throws Exception {
        AccountDTO lan = account("lan@example.com", "CUSTOMER");
        AccountDTO minh = account("minh@example.com", "CUSTOMER");
        String code = bookFor(lan.getId(), seatA1).getBookingCode();

        // Lan (chủ đơn) xem được
        mvc.perform(get("/booking/" + code).sessionAttr("loginAccount", lan))
                .andExpect(status().isOk()).andExpect(view().name("booking/detail"));

        // Minh đổi mã đơn trên URL -> bị chặn ở cả 4 chỗ: xem, huỷ, trang thanh toán, nút VNPay
        expectNotOwner(get("/booking/" + code).sessionAttr("loginAccount", minh));
        expectNotOwner(post("/booking/" + code + "/cancel").sessionAttr("loginAccount", minh));
        expectNotOwner(get("/payment/" + code).sessionAttr("loginAccount", minh));
        expectNotOwner(post("/payment/" + code + "/vnpay").sessionAttr("loginAccount", minh));
        assertThat(bookingStatus(code)).isEqualTo("PENDING"); // Minh không huỷ được đơn của Lan
    }

    @Test
    void notLoggedInGoesToLoginAndRemembersThePage() throws Exception {
        AccountDTO lan = account("lan@example.com", "CUSTOMER");
        String code = bookFor(lan.getId(), seatA1).getBookingCode();

        // Đơn của tài khoản, chưa đăng nhập -> sang trang đăng nhập, nhớ đúng trang để quay lại
        MockHttpSession session = new MockHttpSession();
        mvc.perform(get("/booking/" + code).session(session)).andExpect(redirectedUrl("/auth/login"));
        assertThat(session.getAttribute("redirectAfterLogin")).isEqualTo("/booking/" + code);

        MockHttpSession paymentSession = new MockHttpSession();
        mvc.perform(post("/payment/" + code + "/vnpay").session(paymentSession)).andExpect(redirectedUrl("/auth/login"));
        assertThat(paymentSession.getAttribute("redirectAfterLogin")).isEqualTo("/payment/" + code);
    }

    @Test
    void staffCanViewAnyBooking() throws Exception {
        AccountDTO lan = account("lan@example.com", "CUSTOMER");
        AccountDTO staff = account("staff@example.com", "STAFF");
        String ofLan = bookFor(lan.getId(), seatA1).getBookingCode();
        String ofGuest = bookFor(null, seatA2).getBookingCode();

        mvc.perform(get("/booking/" + ofLan).sessionAttr("loginAccount", staff)).andExpect(status().isOk());
        mvc.perform(get("/booking/" + ofGuest).sessionAttr("loginAccount", staff)).andExpect(status().isOk());
    }

    @Test
    void guestBookingOnAnotherBrowserNeedsTheCorrectPhone() throws Exception {
        String code = bookFor(null, seatA1).getBookingCode(); // khách vãng lai, SĐT 0901234567
        MockHttpSession otherBrowser = new MockHttpSession();

        // Máy khác mở đơn -> phải xác minh SĐT trước
        mvc.perform(get("/booking/" + code).session(otherBrowser))
                .andExpect(redirectedUrl("/booking/" + code + "/verify"));

        // Nhập SAI SĐT -> quay lại trang xác minh, báo lỗi, vẫn không xem được
        mvc.perform(post("/booking/" + code + "/verify").session(otherBrowser).param("contactPhone", "0999999999"))
                .andExpect(redirectedUrl("/booking/" + code + "/verify"))
                .andExpect(flash().attribute("errorMessage", "Số điện thoại không khớp với đơn đặt vé này."));
        mvc.perform(get("/booking/" + code).session(otherBrowser))
                .andExpect(redirectedUrl("/booking/" + code + "/verify"));

        // Nhập ĐÚNG SĐT -> xem được đơn và trang thanh toán trên trình duyệt này
        mvc.perform(post("/booking/" + code + "/verify").session(otherBrowser).param("contactPhone", " 0901234567 "))
                .andExpect(redirectedUrl("/booking/" + code));
        mvc.perform(get("/booking/" + code).session(otherBrowser))
                .andExpect(status().isOk()).andExpect(view().name("booking/detail"));
        mvc.perform(get("/payment/" + code).session(otherBrowser))
                .andExpect(status().isOk()).andExpect(view().name("payment/index"));
    }

    @Test
    void browserThatJustBookedCanViewWithoutPhone() throws Exception {
        MockHttpSession sameBrowser = new MockHttpSession();
        // Khách vãng lai đặt vé qua form trang checkout
        String paymentUrl = mvc.perform(post("/booking").session(sameBrowser).param("acceptPolicy", "true")
                        .param("tripId", String.valueOf(tripId)).param("seatIds", String.valueOf(seatA1))
                        .param("ticketTypeId", String.valueOf(ticketTypeId))
                        .param("fromStationId", String.valueOf(s1)).param("toStationId", String.valueOf(s2))
                        .param("contactName", "Lan").param("contactPhone", "0901234567")
                        .param("contactEmail", "lan@example.com"))
                .andReturn().getResponse().getRedirectedUrl();
        assertThat(paymentUrl).matches("/payment/BK[0-9]{8}");

        // Cùng trình duyệt vừa đặt -> vào thẳng trang thanh toán, không phải nhập SĐT
        mvc.perform(get(paymentUrl).session(sameBrowser))
                .andExpect(status().isOk()).andExpect(view().name("payment/index"));
        // Trình duyệt khác -> phải xác minh
        String code = paymentUrl.substring("/payment/".length());
        mvc.perform(get(paymentUrl).session(new MockHttpSession()))
                .andExpect(redirectedUrl("/booking/" + code + "/verify"));
    }

    private void expectNotOwner(MockHttpServletRequestBuilder request) throws Exception {
        mvc.perform(request)
                .andExpect(redirectedUrl("/"))
                .andExpect(flash().attribute("errorMessage", NOT_OWNER_MESSAGE));
    }

    // Đặt 1 vé thường cho tài khoản accountId (null = khách vãng lai), SĐT liên hệ 0901234567
    private Booking bookFor(Integer accountId, int seatId) {
        return BookingTestHelper.createBooking(bookingService, tripId, List.of(seatId), ticketTypeId, s1, s2,
                "", accountId, "Lan", "0901234567", "lan@example.com");
    }

    private AccountDTO account(String email, String role) {
        jdbc.update("INSERT INTO account (email, password_hash, full_name, phone, role, status, created_at) "
                + "VALUES (?, 'x', 'Test', '0900000000', ?, 'ACTIVE', ?)", email, role, LocalDateTime.now());
        return new AccountDTO(id("account", "email", email), email, "Test", "0900000000", role, "ACTIVE");
    }

    private String bookingStatus(String code) {
        return jdbc.queryForObject("SELECT status FROM booking WHERE booking_code = ?", String.class, code);
    }

    private int id(String table, String codeColumn, String code) {
        return jdbc.queryForObject("SELECT id FROM " + table + " WHERE " + codeColumn + " = ?", Integer.class, code);
    }
}
