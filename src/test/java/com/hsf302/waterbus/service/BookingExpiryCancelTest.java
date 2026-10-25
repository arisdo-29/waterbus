package com.hsf302.waterbus.service;

import com.hsf302.waterbus.dto.AccountDTO;
import com.hsf302.waterbus.entity.Booking;
import com.hsf302.waterbus.entity.Seat;
import com.hsf302.waterbus.job.BookingExpiryJob;
import jakarta.persistence.EntityManager;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// BE4 M4: job tự huỷ đơn quá hạn (BookingExpiryJob) và khách huỷ đơn chưa thanh toán (cancelBooking).
@SpringBootTest
@Transactional
class BookingExpiryCancelTest {
    @Autowired private JdbcTemplate jdbc;
    @Autowired private BookingService bookingService;
    @Autowired private PaymentService paymentService;
    @Autowired private TripService tripService;
    @Autowired private BookingExpiryJob expiryJob;
    @Autowired private EntityManager entityManager;
    @Autowired private WebApplicationContext context;

    private int tripId;
    private int ticketTypeId;
    private int s1;
    private int s2;
    private List<Integer> seats; // 4 ghế A1..A4

    @BeforeEach
    void setUp() {
        jdbc.update("INSERT INTO station (code, name, is_active) VALUES ('TEST_S1', 'Ben mot', 1)");
        jdbc.update("INSERT INTO station (code, name, is_active) VALUES ('TEST_S2', 'Ben hai', 1)");
        s1 = id("station", "code", "TEST_S1");
        s2 = id("station", "code", "TEST_S2");
        jdbc.update("INSERT INTO route (code, name, is_active) VALUES ('TEST_R1', 'Tuyen test', 1)");
        int routeId = id("route", "code", "TEST_R1");
        jdbc.update("INSERT INTO route_station (route_id, station_id, stop_order, minutes_from_start) VALUES (?, ?, 1, 0)", routeId, s1);
        jdbc.update("INSERT INTO route_station (route_id, station_id, stop_order, minutes_from_start) VALUES (?, ?, 2, 0)", routeId, s2);
        jdbc.update("INSERT INTO waterbus (code, name, capacity, status) VALUES ('TEST_WB1', 'Tau test', 4, 'ACTIVE')");
        int busId = id("waterbus", "code", "TEST_WB1");
        for (int col = 1; col <= 4; col++) {
            jdbc.update("INSERT INTO seat (waterbus_id, seat_code, seat_row, seat_col, is_active) VALUES (?, ?, 1, ?, 1)",
                    busId, "A" + col, col);
        }
        seats = jdbc.queryForList("SELECT id FROM seat WHERE waterbus_id = ? ORDER BY seat_col", Integer.class, busId);
        LocalDateTime departure = LocalDateTime.now().plusDays(1);
        jdbc.update("INSERT INTO trip (trip_code, route_id, waterbus_id, departure_time, arrival_time, status) "
                + "VALUES ('TEST_TRIP1', ?, ?, ?, ?, 'SCHEDULED')", routeId, busId, departure, departure.plusMinutes(30));
        tripId = id("trip", "trip_code", "TEST_TRIP1");
        jdbc.update("INSERT INTO ticket_type (code, name, price, requires_verification, is_active) "
                + "VALUES ('TEST_STD', 'Ve thuong', 15000, 0, 1)");
        ticketTypeId = id("ticket_type", "code", "TEST_STD");
    }

    @Test
    void jobExpiresOnlyOverduePendingBookingsAndReleasesTheirSeats() {
        Booking overdue = book(List.of(seats.get(0), seats.get(1)), null);  // quá hạn -> phải bị huỷ
        Booking stillHeld = book(List.of(seats.get(2)), null);             // còn hạn -> giữ nguyên
        Booking paid = book(List.of(seats.get(3)), null);                  // đã trả tiền -> giữ nguyên
        paymentService.payMock(paid.getId());
        overdue.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        paid.setExpiresAt(LocalDateTime.now().minusMinutes(1));

        expiryJob.expireOverdueBookings();
        entityManager.flush(); // ghi xuống DB trước khi đọc lại bằng SQL

        assertThat(status(overdue)).isEqualTo("EXPIRED");
        assertThat(ticketStatuses(overdue)).containsExactly("CANCELLED", "CANCELLED");
        assertThat(status(stillHeld)).isEqualTo("PENDING");
        assertThat(ticketStatuses(stillHeld)).containsExactly("HOLD");
        assertThat(status(paid)).isEqualTo("PAID");
        assertThat(ticketStatuses(paid)).containsExactly("VALID");
        // 2 ghế của đơn quá hạn đã được nhả -> người khác đặt lại được
        assertThat(tripService.getAvailableSeats(tripId)).extracting(Seat::getId)
                .containsExactly(seats.get(0), seats.get(1));
        Booking rebook = book(List.of(seats.get(0)), null);
        assertThat(status(rebook)).isEqualTo("PENDING");
    }

    @Test
    void cancelBookingChecksStatusOwnerAndReleasesSeats() {
        int lanId = account("lan@example.com");
        int minhId = account("minh@example.com");
        Booking guest = book(List.of(seats.get(0)), null);
        Booking ofLan = book(List.of(seats.get(1)), lanId);

        // Đơn khách vãng lai: huỷ được, vé được nhả
        bookingService.cancelBooking(guest.getBookingCode(), null);
        entityManager.flush();
        assertThat(status(guest)).isEqualTo("CANCELLED");
        assertThat(ticketStatuses(guest)).containsExactly("CANCELLED");

        // Huỷ lần 2 / sai mã / không phải chủ đơn -> báo lỗi, đơn của Lan vẫn PENDING
        assertCancelFails(guest.getBookingCode(), null, "Chỉ huỷ được đơn chưa thanh toán.");
        assertCancelFails("SAIMA123", null, "Không tìm thấy đơn đặt vé.");
        assertCancelFails(ofLan.getBookingCode(), minhId, "Bạn không có quyền huỷ đơn này.");
        assertCancelFails(ofLan.getBookingCode(), null, "Bạn không có quyền huỷ đơn này.");
        assertThat(status(ofLan)).isEqualTo("PENDING");

        // Đúng chủ đơn -> huỷ được
        bookingService.cancelBooking(ofLan.getBookingCode(), lanId);
        entityManager.flush();
        assertThat(status(ofLan)).isEqualTo("CANCELLED");
        assertThat(ticketStatuses(ofLan)).containsExactly("CANCELLED");
    }

    @Test
    void cancelButtonRedirectsToBookingPageWithMessage() throws Exception {
        int lanId = account("lan@example.com");
        Booking ofLan = book(List.of(seats.get(0)), lanId);
        AccountDTO lan = new AccountDTO(lanId, "lan@example.com", "Test", "0900000000", "CUSTOMER", "ACTIVE");
        MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).build();
        String url = "/booking/" + ofLan.getBookingCode();

        // Chưa đăng nhập -> không phải chủ đơn -> lỗi
        mvc.perform(post(url + "/cancel"))
                .andExpect(redirectedUrl(url))
                .andExpect(flash().attribute("errorMessage", "Bạn không có quyền huỷ đơn này."));
        // Đăng nhập đúng chủ đơn -> huỷ thành công
        mvc.perform(post(url + "/cancel").sessionAttr("loginAccount", lan))
                .andExpect(redirectedUrl(url))
                .andExpect(flash().attribute("successMessage", "Đã huỷ đơn."));
        entityManager.flush();
        assertThat(status(ofLan)).isEqualTo("CANCELLED");
    }

    private Booking book(List<Integer> seatIds, Integer accountId) {
        return bookingService.createBooking(tripId, seatIds, ticketTypeId, s1, s2,
                "", accountId, "Lan", "0901234567", "lan@example.com");
    }

    private void assertCancelFails(String code, Integer accountId, String message) {
        assertThatThrownBy(() -> bookingService.cancelBooking(code, accountId))
                .isInstanceOf(IllegalArgumentException.class).hasMessage(message);
    }

    private int account(String email) {
        jdbc.update("INSERT INTO account (email, password_hash, full_name, phone, role, status, created_at) "
                + "VALUES (?, 'x', 'Test', '0900000000', 'CUSTOMER', 'ACTIVE', ?)", email, LocalDateTime.now());
        return id("account", "email", email);
    }

    private String status(Booking booking) {
        return jdbc.queryForObject("SELECT status FROM booking WHERE id = ?", String.class, booking.getId());
    }

    private List<String> ticketStatuses(Booking booking) {
        return jdbc.queryForList("SELECT status FROM ticket WHERE booking_id = ? ORDER BY id", String.class, booking.getId());
    }

    private int id(String table, String codeColumn, String code) {
        return jdbc.queryForObject("SELECT id FROM " + table + " WHERE " + codeColumn + " = ?", Integer.class, code);
    }
}
