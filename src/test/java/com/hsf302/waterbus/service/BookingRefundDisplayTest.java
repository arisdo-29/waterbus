package com.hsf302.waterbus.service;

import com.hsf302.waterbus.dto.AccountDTO;
import com.hsf302.waterbus.dto.BookingDTO;
import com.hsf302.waterbus.entity.Booking;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// BE4 R6: đơn có chuyến bị huỷ phải hiện đúng ở trang xem vé (/booking/{mã}) và "Vé của tôi" (/my-bookings).
// Đi hết luồng thật: đặt vé -> trả tiền VNPay -> nhà vận hành huỷ chuyến (chờ hoàn tiền) -> admin xác nhận đã hoàn.
@SpringBootTest
@Transactional
class BookingRefundDisplayTest {
    @Autowired private JdbcTemplate jdbc;
    @Autowired private BookingService bookingService;
    @Autowired private PaymentService paymentService;
    @Autowired private TripService tripService;
    @Autowired private RefundService refundService;
    @Autowired private WebApplicationContext context;

    private MockMvc mvc;
    private AccountDTO lan;  // khách đã đăng nhập, chủ của đơn
    private AccountDTO admin;
    private Booking booking; // đơn đã trả tiền, sau đó chuyến bị huỷ

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        jdbc.update("INSERT INTO station (code, name, is_active) VALUES ('TEST_S1', 'Ben mot', 1)");
        jdbc.update("INSERT INTO station (code, name, is_active) VALUES ('TEST_S2', 'Ben hai', 1)");
        int s1 = id("station", "code", "TEST_S1");
        int s2 = id("station", "code", "TEST_S2");
        jdbc.update("INSERT INTO route (code, name, is_active) VALUES ('TEST_R1', 'Tuyen test', 1)");
        int routeId = id("route", "code", "TEST_R1");
        jdbc.update("INSERT INTO route_station (route_id, station_id, stop_order, minutes_from_start) VALUES (?, ?, 1, 0)", routeId, s1);
        jdbc.update("INSERT INTO route_station (route_id, station_id, stop_order, minutes_from_start) VALUES (?, ?, 2, 0)", routeId, s2);
        jdbc.update("INSERT INTO waterbus (code, name, capacity, status) VALUES ('TEST_WB1', 'Tau test', 1, 'ACTIVE')");
        int busId = id("waterbus", "code", "TEST_WB1");
        jdbc.update("INSERT INTO seat (waterbus_id, seat_code, seat_row, seat_col, is_active) VALUES (?, 'A1', 1, 1, 1)", busId);
        int seatId = jdbc.queryForObject("SELECT id FROM seat WHERE waterbus_id = ?", Integer.class, busId);
        LocalDateTime departure = LocalDateTime.now().plusDays(1);
        jdbc.update("INSERT INTO trip (trip_code, route_id, waterbus_id, departure_time, arrival_time, status) "
                + "VALUES ('TEST_TRIP1', ?, ?, ?, ?, 'SCHEDULED')", routeId, busId, departure, departure.plusMinutes(30));
        int tripId = id("trip", "trip_code", "TEST_TRIP1");
        jdbc.update("INSERT INTO ticket_type (code, name, price, requires_verification, is_active) "
                + "VALUES ('TEST_STD', 'Ve thuong', 15000, 0, 1)");
        int ticketTypeId = id("ticket_type", "code", "TEST_STD");
        jdbc.update("INSERT INTO account (email, password_hash, full_name, phone, role, status, created_at) "
                + "VALUES ('lan@example.com', 'x', 'Lan', '0901234567', 'CUSTOMER', 'ACTIVE', ?)", LocalDateTime.now());
        lan = new AccountDTO(id("account", "email", "lan@example.com"), "lan@example.com", "Lan", "0901234567",
                "CUSTOMER", "ACTIVE");
        jdbc.update("INSERT INTO account (email, password_hash, full_name, role, status, created_at) "
                + "VALUES ('refund-admin@example.com', 'x', 'Admin hoàn tiền', 'ADMIN', 'ACTIVE', ?)",
                LocalDateTime.now());
        admin = new AccountDTO(id("account", "email", "refund-admin@example.com"),
                "refund-admin@example.com", "Admin hoàn tiền", null, "ADMIN", "ACTIVE");

        // Đặt vé + trả tiền qua VNPay, rồi nhà vận hành huỷ chuyến -> hệ thống tạo khoản hoàn tiền PENDING
        booking = bookingService.createBooking(tripId, List.of(seatId), ticketTypeId, s1, s2,
                "", lan.getId(), "Lan", "0901234567", "lan@example.com");
        paymentService.confirmPaid(booking.getId(), "VNPAY", "14123456");
        tripService.cancelTrip(tripId, "Nuoc lon");
    }

    @Test
    void cancelledTripShowsWaitingForRefund() throws Exception {
        // Chưa hoàn xong: đơn vẫn PAID, khoản hoàn PENDING
        BookingDTO dto = bookingService.getBookingDetail(booking.getBookingCode());
        assertThat(dto.getStatus()).isEqualTo("PAID");
        assertThat(dto.getRefundStatus()).isEqualTo("PENDING");

        // Trang xem vé: banner vàng "Chuyến đã bị huỷ" kèm lý do, vé đã huỷ, không báo "Đặt vé thành công"
        assertThat(page("/booking/" + booking.getBookingCode()))
                .contains("wb-refund-banner is-pending", "Nuoc lon", "Đã huỷ")
                .doesNotContain("Đặt vé thành công!", "wb-refund-banner is-done");
        // "Vé của tôi": đơn có nhãn "Chờ hoàn tiền"
        assertThat(page("/my-bookings")).contains(booking.getBookingCode(), "Chờ hoàn tiền");
    }

    @Test
    void refundedBookingShowsRefundedEverywhere() throws Exception {
        // Admin ghi nhận đã chuyển tiền hoàn cho khách -> khoản hoàn DONE, đơn REFUNDED
        int refundId = refundService.getRefundOfBooking(booking.getId()).getId();
        refundService.markRefunded(refundId, admin);
        assertThat(refundService.getRefundOfBooking(booking.getId()).getRefundedAt()).isNotNull();

        BookingDTO dto = bookingService.getBookingDetail(booking.getBookingCode());
        assertThat(dto.getStatus()).isEqualTo("REFUNDED");
        assertThat(dto.getRefundStatus()).isEqualTo("DONE");

        // Trang xem vé: "Đơn đã hoàn tiền" + banner xanh, không còn mã QR lên tàu
        assertThat(page("/booking/" + booking.getBookingCode()))
                .contains("Đơn đã hoàn tiền", "wb-refund-banner is-done")
                .doesNotContain("wb-refund-banner is-pending", "Đưa nhân viên quét");
        // "Vé của tôi": nhãn "Đã hoàn tiền", không còn "Chờ hoàn tiền"
        assertThat(page("/my-bookings")).contains(booking.getBookingCode(), "Đã hoàn tiền")
                .doesNotContain("Chờ hoàn tiền");
    }

    // Mở trang bằng tài khoản của Lan (đã đăng nhập), trả về HTML
    private String page(String url) throws Exception {
        return mvc.perform(get(url).sessionAttr("loginAccount", lan))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private int id(String table, String codeColumn, String code) {
        return jdbc.queryForObject("SELECT id FROM " + table + " WHERE " + codeColumn + " = ?", Integer.class, code);
    }
}
