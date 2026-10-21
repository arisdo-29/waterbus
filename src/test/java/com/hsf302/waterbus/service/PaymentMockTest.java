package com.hsf302.waterbus.service;

import com.hsf302.waterbus.entity.Booking;
import com.hsf302.waterbus.entity.Payment;
import jakarta.persistence.EntityManager;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// BE4 #15: thanh toán giả lập (payMock), GET /payment/{bookingCode}, POST /payment/{bookingCode}/mock.
@SpringBootTest
@Transactional
class PaymentMockTest {
    @Autowired private JdbcTemplate jdbc;
    @Autowired private BookingService bookingService;
    @Autowired private PaymentService paymentService;
    @Autowired private EntityManager entityManager;
    @Autowired private WebApplicationContext context;

    private Booking booking; // đơn PENDING, 2 vé HOLD, có mã khuyến mãi

    @BeforeEach
    void setUp() {
        jdbc.update("INSERT INTO station (code, name, is_active) VALUES ('TEST_S1', 'Ben mot', 1)");
        jdbc.update("INSERT INTO station (code, name, is_active) VALUES ('TEST_S2', 'Ben hai', 1)");
        int s1 = id("station", "code", "TEST_S1");
        int s2 = id("station", "code", "TEST_S2");
        jdbc.update("INSERT INTO route (code, name, is_active) VALUES ('TEST_R1', 'Tuyen test', 1)");
        int routeId = id("route", "code", "TEST_R1");
        jdbc.update("INSERT INTO route_station (route_id, station_id, stop_order, minutes_from_start) VALUES (?, ?, 1, 0)", routeId, s1);
        jdbc.update("INSERT INTO route_station (route_id, station_id, stop_order, minutes_from_start) VALUES (?, ?, 2, 0)", routeId, s2);
        jdbc.update("INSERT INTO waterbus (code, name, capacity, status) VALUES ('TEST_WB1', 'Tau test', 2, 'ACTIVE')");
        int busId = id("waterbus", "code", "TEST_WB1");
        jdbc.update("INSERT INTO seat (waterbus_id, seat_code, seat_row, seat_col, is_active) VALUES (?, 'A1', 1, 1, 1)", busId);
        jdbc.update("INSERT INTO seat (waterbus_id, seat_code, seat_row, seat_col, is_active) VALUES (?, 'A2', 1, 2, 1)", busId);
        List<Integer> seatIds = jdbc.queryForList("SELECT id FROM seat WHERE waterbus_id = ? ORDER BY seat_col", Integer.class, busId);
        LocalDateTime departure = LocalDateTime.now().plusDays(1);
        jdbc.update("INSERT INTO trip (trip_code, route_id, waterbus_id, departure_time, arrival_time, status) "
                + "VALUES ('TEST_TRIP1', ?, ?, ?, ?, 'SCHEDULED')", routeId, busId, departure, departure.plusMinutes(30));
        int tripId = id("trip", "trip_code", "TEST_TRIP1");
        jdbc.update("INSERT INTO ticket_type (code, name, price, requires_verification, is_active) "
                + "VALUES ('TEST_STD', 'Ve thuong', 15000, 0, 1)");
        int ticketTypeId = id("ticket_type", "code", "TEST_STD");
        jdbc.update("INSERT INTO promotion (code, name, discount_type, discount_value, min_order_amount, "
                        + "start_at, end_at, used_count, is_active) VALUES ('TEST10', 'Giam 10%', 'PERCENT', 10, 0, ?, ?, 0, 1)",
                LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1));

        // Tạo đơn bằng chính luồng đặt vé (#14): 2 vé x 15.000 - 10% = 27.000
        booking = bookingService.createBooking(tripId, seatIds, ticketTypeId, s1, s2,
                "TEST10", null, "Lan", "0901234567", "lan@example.com");
    }

    @Test
    void payMockMarksEverythingPaidAndCountsPromotionUse() {
        Payment payment = paymentService.payMock(booking.getId());
        entityManager.flush(); // ghi thay đổi xuống DB trước khi đọc lại bằng SQL

        assertThat(payment.getStatus()).isEqualTo("SUCCESS");
        assertThat(payment.getMethod()).isEqualTo("MOCK");
        assertThat(payment.getAmount()).isEqualByComparingTo("27000");
        assertThat(payment.getTransactionRef()).isEqualTo("MOCK-" + booking.getBookingCode());
        assertThat(jdbc.queryForObject("SELECT status FROM booking WHERE id = ?", String.class, booking.getId()))
                .isEqualTo("PAID");
        assertThat(jdbc.queryForObject("SELECT paid_at FROM booking WHERE id = ?", LocalDateTime.class, booking.getId()))
                .isNotNull();
        assertThat(jdbc.queryForList("SELECT status FROM ticket WHERE booking_id = ?", String.class, booking.getId()))
                .containsExactly("VALID", "VALID");
        assertThat(jdbc.queryForObject("SELECT used_count FROM promotion WHERE code = 'TEST10'", Integer.class))
                .isEqualTo(1);
        assertThat(count("payment")).isEqualTo(1);
    }

    @Test
    void payMockRejectsPaidOrExpiredBookingWithoutChangingIt() {
        paymentService.payMock(booking.getId());
        assertThatThrownBy(() -> paymentService.payMock(booking.getId())) // trả tiền lần 2
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("không còn chờ thanh toán");

        booking.setStatus("PENDING"); // giả lập đơn còn chờ nhưng đã quá 10 phút giữ chỗ
        booking.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        assertThatThrownBy(() -> paymentService.payMock(booking.getId()))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("hết thời gian giữ chỗ");
        assertThatThrownBy(() -> paymentService.payMock(-1))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Không tìm thấy");
        assertThat(count("payment")).isEqualTo(1); // chỉ lần thanh toán đầu tiên tạo payment
    }

    @Test
    void paymentPageShowsBookingTripAndTickets() throws Exception {
        MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).build();
        String html = mvc.perform(get("/payment/" + booking.getBookingCode()))
                .andExpect(status().isOk())
                .andExpect(view().name("payment/index"))
                .andExpect(model().attributeExists("booking", "trip", "tickets"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(html).contains(booking.getBookingCode(), "Tuyen test", "TEST_TRIP1", "A1", "A2",
                "Ben mot", "Ben hai", "27.000 đ", "-3.000 đ", "/payment/" + booking.getBookingCode() + "/mock");

        mvc.perform(get("/payment/SAIMA123"))
                .andExpect(redirectedUrl("/"))
                .andExpect(flash().attribute("errorMessage", "Không tìm thấy đơn đặt vé có mã SAIMA123."));
    }

    @Test
    void payButtonRedirectsToTicketPageOrBackToPaymentOnError() throws Exception {
        MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).build();
        mvc.perform(post("/payment/" + booking.getBookingCode() + "/mock"))
                .andExpect(redirectedUrl("/booking/" + booking.getBookingCode()))
                .andExpect(flash().attributeExists("successMessage"));
        String detail = mvc.perform(get("/booking/" + booking.getBookingCode()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(detail).contains("Đặt vé thành công!");

        // Bấm thanh toán lần 2 -> quay lại trang thanh toán kèm lỗi
        mvc.perform(post("/payment/" + booking.getBookingCode() + "/mock"))
                .andExpect(redirectedUrl("/payment/" + booking.getBookingCode()))
                .andExpect(flash().attribute("errorMessage", "Đơn này không còn chờ thanh toán."));
        mvc.perform(post("/payment/SAIMA123/mock")).andExpect(redirectedUrl("/"));
    }

    private int id(String table, String codeColumn, String code) {
        return jdbc.queryForObject("SELECT id FROM " + table + " WHERE " + codeColumn + " = ?", Integer.class, code);
    }

    private int count(String table) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
    }
}
