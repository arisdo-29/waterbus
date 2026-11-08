package com.hsf302.waterbus.service;

import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class TripCancellationRefundTest {
    @Autowired private JdbcTemplate jdbc;
    @Autowired private EntityManager entityManager;
    @Autowired private TripService tripService;
    @Autowired private RefundService refundService;
    @Autowired private RevenueService revenueService;

    private int tripId;

    @BeforeEach
    void setUp() {
        jdbc.update("INSERT INTO station (code, name, is_active) VALUES ('CANCEL_S1', 'Bến A', 1)");
        jdbc.update("INSERT INTO station (code, name, is_active) VALUES ('CANCEL_S2', 'Bến B', 1)");
        jdbc.update("INSERT INTO route (code, name, is_active) VALUES ('CANCEL_R', 'Tuyến A', 1)");
        jdbc.update("INSERT INTO waterbus (code, name, capacity, status) VALUES ('CANCEL_W', 'Tàu A', 3, 'ACTIVE')");
        int routeId = id("route", "code", "CANCEL_R");
        int busId = id("waterbus", "code", "CANCEL_W");
        LocalDateTime departure = LocalDateTime.now().plusDays(1);
        jdbc.update("INSERT INTO trip (trip_code, route_id, waterbus_id, departure_time, arrival_time, status) "
                        + "VALUES ('CANCEL_T', ?, ?, ?, ?, 'SCHEDULED')",
                routeId, busId, departure, departure.plusMinutes(30));
        tripId = id("trip", "trip_code", "CANCEL_T");
        jdbc.update("INSERT INTO seat (waterbus_id, seat_code, seat_row, seat_col, is_active) VALUES (?, 'A1', 1, 1, 1)", busId);
        jdbc.update("INSERT INTO seat (waterbus_id, seat_code, seat_row, seat_col, is_active) VALUES (?, 'A2', 1, 2, 1)", busId);
        jdbc.update("INSERT INTO seat (waterbus_id, seat_code, seat_row, seat_col, is_active) VALUES (?, 'A3', 1, 3, 1)", busId);
        jdbc.update("INSERT INTO ticket_type (code, name, price, requires_verification, is_active) VALUES ('CANCEL_TYPE', 'Vé thường', 20000, 0, 1)");
        jdbc.update("INSERT INTO ticket_type (code, name, price, requires_verification, is_active) VALUES ('CANCEL_FREE_TYPE', 'Vé miễn phí', 0, 1, 1)");
        booking("CANCEL_PENDING", "PENDING", 20000);
        booking("CANCEL_PAID", "PAID", 20000);
        booking("CANCEL_FREE", "PAID", 0);
        ticket("CANCEL_T1", "CANCEL_PENDING", "A1", "HOLD");
        ticket("CANCEL_T2", "CANCEL_PAID", "A2", "VALID");
        ticket("CANCEL_T3", "CANCEL_FREE", "A3", "VALID");
        jdbc.update("INSERT INTO payment (booking_id, method, amount, status, transaction_ref, created_at, paid_at) "
                        + "VALUES (?, 'MOCK', 20000, 'SUCCESS', 'MOCK-CANCEL', ?, ?)",
                id("booking", "booking_code", "CANCEL_PAID"), LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    void cancellationReleasesHoldAndCreatesRefundInSameTransaction() {
        assertEquals(2, tripService.getManifest(tripId).totalCount());
        assertEquals(2, tripService.getTripsForAdmin().stream()
                .filter(trip -> trip.id() == tripId).findFirst().orElseThrow().soldTickets());
        assertThrows(IllegalArgumentException.class, () -> tripService.cancelTrip(tripId, " "));

        tripService.cancelTrip(tripId, "Thời tiết xấu");
        entityManager.flush();
        assertEquals("CANCELLED", value("SELECT status FROM trip WHERE id = ?", tripId));
        assertEquals("Thời tiết xấu", value("SELECT cancel_reason FROM trip WHERE id = ?", tripId));
        assertEquals("CANCELLED", value("SELECT status FROM booking WHERE booking_code = 'CANCEL_PENDING'"));
        assertEquals("PAID", value("SELECT status FROM booking WHERE booking_code = 'CANCEL_PAID'"));
        assertEquals("CANCELLED", value("SELECT status FROM booking WHERE booking_code = 'CANCEL_FREE'"));
        assertEquals("CANCELLED", value("SELECT status FROM ticket WHERE ticket_code = 'CANCEL_T1'"));
        assertEquals("CANCELLED", value("SELECT status FROM ticket WHERE ticket_code = 'CANCEL_T2'"));
        assertEquals("CANCELLED", value("SELECT status FROM ticket WHERE ticket_code = 'CANCEL_T3'"));
        assertEquals("PENDING", value("SELECT status FROM refund WHERE booking_id = ?",
                id("booking", "booking_code", "CANCEL_PAID")));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM refund", Integer.class));
        assertEquals(0, tripService.getManifest(tripId).totalCount());

        int refundId = jdbc.queryForObject("SELECT id FROM refund WHERE booking_id = ?", Integer.class,
                id("booking", "booking_code", "CANCEL_PAID"));
        refundService.markRefunded(refundId, "BANK-123", LocalDateTime.now().minusMinutes(1), "Đã chuyển khoản");
        entityManager.flush();
        assertEquals("DONE", value("SELECT status FROM refund WHERE id = ?", refundId));
        assertEquals("REFUNDED", value("SELECT status FROM booking WHERE booking_code = 'CANCEL_PAID'"));
        var report = revenueService.getReport(LocalDate.now(), LocalDate.now());
        assertEquals(BigDecimal.ZERO, report.summary().totalRevenue());
        assertEquals(report.summary().totalRevenue(), report.dailyRevenue().stream()
                .map(day -> day.revenue()).reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private void booking(String code, String status, int amount) {
        LocalDateTime now = LocalDateTime.now();
        jdbc.update("INSERT INTO booking (booking_code, contact_name, contact_phone, contact_email, "
                        + "subtotal, discount_amount, total_amount, status, created_at, expires_at, paid_at) "
                        + "VALUES (?, 'Khách', '0900000000', 'guest@example.com', ?, 0, ?, ?, ?, ?, ?)",
                code, amount, amount, status, now, now.plusMinutes(10), "PAID".equals(status) ? now : null);
    }

    private void ticket(String code, String bookingCode, String seatCode, String status) {
        boolean free = "CANCEL_FREE".equals(bookingCode);
        jdbc.update("INSERT INTO ticket (ticket_code, booking_id, trip_id, seat_id, ticket_type_id, "
                        + "from_station_id, to_station_id, price, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                code, id("booking", "booking_code", bookingCode), tripId,
                jdbc.queryForObject("SELECT id FROM seat WHERE seat_code = ?", Integer.class, seatCode),
                id("ticket_type", "code", free ? "CANCEL_FREE_TYPE" : "CANCEL_TYPE"),
                id("station", "code", "CANCEL_S1"), id("station", "code", "CANCEL_S2"),
                free ? 0 : 20000, status);
    }

    private int id(String table, String column, String code) {
        return jdbc.queryForObject("SELECT id FROM " + table + " WHERE " + column + " = ?", Integer.class, code);
    }

    private String value(String sql, Object... args) {
        return jdbc.queryForObject(sql, String.class, args);
    }
}
