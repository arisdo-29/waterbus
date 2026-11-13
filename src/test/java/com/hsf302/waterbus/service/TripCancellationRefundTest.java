package com.hsf302.waterbus.service;

import com.hsf302.waterbus.dto.AccountDTO;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
class TripCancellationRefundTest {
    @Autowired private JdbcTemplate jdbc;
    @Autowired private EntityManager entityManager;
    @Autowired private TripService tripService;
    @Autowired private WaterbusService waterbusService;
    @Autowired private RefundService refundService;
    @Autowired private RevenueService revenueService;
    @Autowired private WebApplicationContext context;

    private int tripId;

    @BeforeEach
    void setUp() {
        jdbc.update("INSERT INTO account (email, password_hash, full_name, role, status, created_at) "
                + "VALUES ('refund-admin@test.vn', 'unused', 'Admin hoàn tiền', 'ADMIN', 'ACTIVE', ?)",
                LocalDateTime.now());
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
        refundService.markRefunded(refundId, admin());
        entityManager.flush();
        assertEquals("DONE", value("SELECT status FROM refund WHERE id = ?", refundId));
        assertEquals("REFUNDED", value("SELECT status FROM booking WHERE booking_code = 'CANCEL_PAID'"));
        assertEquals(admin().getId(), jdbc.queryForObject("SELECT processed_by FROM refund WHERE id = ?",
                Integer.class, refundId));
        assertNotNull(jdbc.queryForObject("SELECT refunded_at FROM refund WHERE id = ?",
                LocalDateTime.class, refundId));
        assertNull(value("SELECT transaction_ref FROM refund WHERE id = ?", refundId));
        assertThrows(IllegalArgumentException.class, () -> refundService.markRefunded(refundId, admin()));
        var report = revenueService.getReport(LocalDate.now(), LocalDate.now());
        assertEquals(BigDecimal.ZERO, report.summary().totalRevenue());
        assertEquals(report.summary().totalRevenue(), report.dailyRevenue().stream()
                .map(day -> day.revenue()).reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    @Test
    void maintenanceCancelsAffectedTripAndQueuesPaidBookingForRefund() {
        int busId = id("waterbus", "code", "CANCEL_W");
        LocalDateTime departure = jdbc.queryForObject("SELECT departure_time FROM trip WHERE id = ?",
                LocalDateTime.class, tripId);
        LocalDateTime startsAt = departure.minusMinutes(5);
        LocalDateTime endsAt = departure.plusMinutes(5);

        var previews = waterbusService.previewStatusChange(busId, "MAINTENANCE", startsAt, endsAt);
        assertEquals(1, previews.size());
        assertEquals(2, previews.get(0).confirmedTicketCount());
        assertEquals(1, previews.get(0).heldTicketCount());
        assertThrows(IllegalArgumentException.class, () -> waterbusService.scheduleStatusChange(
                busId, "MAINTENANCE", startsAt, endsAt, List.of()));
        assertEquals(1, waterbusService.scheduleStatusChange(
                busId, "MAINTENANCE", startsAt, endsAt, List.of(tripId)));
        entityManager.flush();

        assertEquals("CANCELLED", value("SELECT status FROM trip WHERE id = ?", tripId));
        assertEquals("CANCELLED", value("SELECT status FROM booking WHERE booking_code = 'CANCEL_PENDING'"));
        assertEquals("PAID", value("SELECT status FROM booking WHERE booking_code = 'CANCEL_PAID'"));
        assertEquals("PENDING", value("SELECT status FROM refund WHERE booking_id = ?",
                id("booking", "booking_code", "CANCEL_PAID")));
        assertEquals(1, refundService.getRefunds("PENDING").size());
        assertEquals("ACTIVE", value("SELECT status FROM waterbus WHERE id = ?", busId));
        assertEquals("MAINTENANCE", value("SELECT pending_status FROM waterbus WHERE id = ?", busId));
    }

    @Test
    void cancellationReturnsPromotionUseOnceForPaidBookings() {
        LocalDateTime now = LocalDateTime.now();
        jdbc.update("INSERT INTO promotion (code, name, discount_type, discount_value, min_order_amount, "
                        + "start_at, end_at, used_count, is_active) "
                        + "VALUES ('CANCEL_PROMO', 'Mã thử', 'FIXED', 1000, 0, ?, ?, 3, 1)",
                now.minusDays(1), now.plusDays(1));
        int promotionId = id("promotion", "code", "CANCEL_PROMO");
        jdbc.update("UPDATE booking SET promotion_id = ? WHERE booking_code IN "
                + "('CANCEL_PENDING', 'CANCEL_PAID', 'CANCEL_FREE')", promotionId);

        tripService.cancelTrip(tripId, "Tàu hỏng");
        entityManager.flush();
        assertEquals(1, jdbc.queryForObject("SELECT used_count FROM promotion WHERE id = ?", Integer.class,
                promotionId));
        assertEquals("PENDING", value("SELECT status FROM refund WHERE booking_id = ?",
                id("booking", "booking_code", "CANCEL_PAID")));
        assertEquals("CANCELLED", value("SELECT status FROM booking WHERE booking_code = 'CANCEL_FREE'"));

        refundService.createForCancelledTrip(tripId, "Tàu hỏng");
        entityManager.flush();
        assertEquals(1, jdbc.queryForObject("SELECT used_count FROM promotion WHERE id = ?", Integer.class,
                promotionId));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM refund", Integer.class));
    }

    @Test
    void cancellationPreviewShowsConfirmedAndHeldTickets() {
        LocalDateTime departure = jdbc.queryForObject(
                "SELECT departure_time FROM trip WHERE id = ?", LocalDateTime.class, tripId);

        var previews = tripService.getCancellableTrips(id("waterbus", "code", "CANCEL_W"),
                departure.minusMinutes(1), departure.plusMinutes(1));

        assertEquals(1, previews.size());
        var preview = previews.get(0);
        assertEquals("CANCEL_T", preview.trip().tripCode());
        assertEquals(2, preview.confirmedTicketCount());
        assertEquals(1, preview.heldTicketCount());
        assertEquals(3, preview.tickets().size());
        assertTrue(preview.tickets().stream().anyMatch(ticket ->
                "CANCEL_T2".equals(ticket.getTicketCode()) && "Khách".equals(ticket.getPassengerName())
                        && "0900000000".equals(ticket.getPassengerPhone())));
    }

    @Test
    void batchCancellationCancelsAllSelectedTripsAndDoesNotPartiallyCancelInvalidBatch() {
        int routeId = id("route", "code", "CANCEL_R");
        int waterbusId = id("waterbus", "code", "CANCEL_W");
        LocalDateTime secondDeparture = LocalDateTime.now().plusDays(2);
        jdbc.update("INSERT INTO trip (trip_code, route_id, waterbus_id, departure_time, arrival_time, status) "
                        + "VALUES ('CANCEL_T2', ?, ?, ?, ?, 'SCHEDULED')",
                routeId, waterbusId, secondDeparture, secondDeparture.plusMinutes(30));
        int secondTripId = id("trip", "trip_code", "CANCEL_T2");

        tripService.cancelTrips(List.of(tripId, secondTripId), "Thời tiết xấu");
        entityManager.flush();
        assertEquals("CANCELLED", value("SELECT status FROM trip WHERE id = ?", tripId));
        assertEquals("CANCELLED", value("SELECT status FROM trip WHERE id = ?", secondTripId));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM refund", Integer.class));

        jdbc.update("UPDATE trip SET status = 'SCHEDULED' WHERE id = ?", tripId);
        jdbc.update("UPDATE trip SET status = 'DEPARTED' WHERE id = ?", secondTripId);
        entityManager.clear();
        assertThrows(IllegalArgumentException.class,
                () -> tripService.cancelTrips(List.of(tripId, secondTripId), "Lý do khác"));
        assertEquals("SCHEDULED", value("SELECT status FROM trip WHERE id = ?", tripId));
        assertEquals("DEPARTED", value("SELECT status FROM trip WHERE id = ?", secondTripId));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM refund", Integer.class));
    }

    @Test
    void adminCanReviewAndSubmitBatchCancellation() throws Exception {
        LocalDateTime departure = jdbc.queryForObject(
                "SELECT departure_time FROM trip WHERE id = ?", LocalDateTime.class, tripId);
        DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
        int waterbusId = id("waterbus", "code", "CANCEL_W");
        var mvc = MockMvcBuilders.webAppContextSetup(context).build();
        String page = mvc.perform(get("/admin/trips/cancel")
                        .param("waterbusId", String.valueOf(waterbusId))
                        .param("departureFrom", departure.minusMinutes(1).format(format))
                        .param("departureTo", departure.plusMinutes(1).format(format))
                        .sessionAttr("loginAccount", admin()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertTrue(page.contains("CANCEL_T"));
        assertTrue(page.contains("CANCEL_PAID"));
        assertTrue(page.contains("0900000000"));

        mvc.perform(post("/admin/trips/cancel-selected")
                        .param("tripIds", String.valueOf(tripId))
                        .param("waterbusId", String.valueOf(waterbusId))
                        .param("departureFrom", departure.minusMinutes(1).format(format))
                        .param("departureTo", departure.plusMinutes(1).format(format))
                        .param("reason", "Tàu cần bảo trì")
                        .sessionAttr("loginAccount", admin()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/refunds?status=PENDING"));
        entityManager.flush();
        assertEquals("CANCELLED", value("SELECT status FROM trip WHERE id = ?", tripId));
        assertEquals("PENDING", value("SELECT status FROM refund WHERE booking_id = ?",
                id("booking", "booking_code", "CANCEL_PAID")));
    }

    @Test
    void staffCannotViewOrSubmitBulkCancellation() throws Exception {
        jdbc.update("INSERT INTO account (email, password_hash, full_name, role, status, created_at) "
                        + "VALUES ('refund-staff@test.vn', 'unused', 'Nhân viên', 'STAFF', 'ACTIVE', ?)",
                LocalDateTime.now());
        AccountDTO staff = new AccountDTO(id("account", "email", "refund-staff@test.vn"),
                "refund-staff@test.vn", "Nhân viên", null, "STAFF", "ACTIVE");
        var mvc = MockMvcBuilders.webAppContextSetup(context).build();

        mvc.perform(get("/admin/trips/cancel").sessionAttr("loginAccount", staff))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/checkin?error=forbidden"));
        mvc.perform(post("/admin/trips/cancel-selected").sessionAttr("loginAccount", staff))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/checkin?error=forbidden"));
    }

    @Test
    void refundPageShowsContactAndCompletesWithoutFormFields() throws Exception {
        tripService.cancelTrip(tripId, "Tàu hỏng");
        entityManager.flush();
        int refundId = jdbc.queryForObject("SELECT id FROM refund WHERE booking_id = ?", Integer.class,
                id("booking", "booking_code", "CANCEL_PAID"));
        var mvc = MockMvcBuilders.webAppContextSetup(context).build();
        String page = mvc.perform(get("/admin/refunds").param("status", "PENDING")
                        .sessionAttr("loginAccount", admin()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertTrue(page.contains("0900000000"));
        assertTrue(page.contains("guest@example.com"));
        assertTrue(page.contains("CANCEL_PAID"));
        assertTrue(page.contains("Tàu hỏng"));
        assertTrue(page.contains("Xác nhận đã hoàn tiền"));
        assertFalse(page.contains("name=\"transactionRef\""));
        assertFalse(page.contains("name=\"refundedAt\""));
        assertFalse(page.contains("name=\"note\""));

        mvc.perform(post("/admin/refunds/" + refundId + "/complete")
                        .sessionAttr("loginAccount", admin()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/refunds"));
        entityManager.flush();
        assertEquals("DONE", value("SELECT status FROM refund WHERE id = ?", refundId));
        assertEquals("REFUNDED", value("SELECT status FROM booking WHERE booking_code = 'CANCEL_PAID'"));
    }

    @Test
    void invalidPaymentOfExpiredBookingCreatesOneRefundAndSubtractsRevenueWhenCompleted() throws Exception {
        int bookingId = id("booking", "booking_code", "CANCEL_PAID");
        jdbc.update("UPDATE booking SET status = 'EXPIRED' WHERE id = ?", bookingId);
        jdbc.update("UPDATE ticket SET status = 'CANCELLED' WHERE booking_id = ?", bookingId);
        jdbc.update("UPDATE payment SET method = 'VNPAY', transaction_ref = 'VNP-EXPIRED' WHERE booking_id = ?",
                bookingId);

        refundService.createForInvalidPayment(bookingId, "Thanh toán sau khi đơn hết hạn");
        refundService.createForInvalidPayment(bookingId, "Không tạo thêm khoản hoàn");
        entityManager.flush();
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM refund WHERE booking_id = ?", Integer.class,
                bookingId));
        assertEquals("PENDING", value("SELECT status FROM refund WHERE booking_id = ?", bookingId));
        assertEquals("Thanh toán sau khi đơn hết hạn", value("SELECT reason FROM refund WHERE booking_id = ?", bookingId));
        assertEquals(new BigDecimal("20000"), jdbc.queryForObject(
                "SELECT amount FROM refund WHERE booking_id = ?", BigDecimal.class, bookingId));
        assertEquals("EXPIRED", value("SELECT status FROM booking WHERE id = ?", bookingId));

        var mvc = MockMvcBuilders.webAppContextSetup(context).build();
        String page = mvc.perform(get("/admin/refunds").param("status", "PENDING")
                        .sessionAttr("loginAccount", admin()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertTrue(page.contains("Thanh toán sau khi đơn hết hạn"));
        assertEquals(new BigDecimal("20000"), revenueService.getReport(LocalDate.now(), LocalDate.now())
                .summary().totalRevenue());

        int refundId = jdbc.queryForObject("SELECT id FROM refund WHERE booking_id = ?", Integer.class,
                bookingId);
        refundService.markRefunded(refundId, admin());
        entityManager.flush();
        assertEquals("REFUNDED", value("SELECT status FROM booking WHERE id = ?", bookingId));
        assertEquals("DONE", value("SELECT status FROM refund WHERE id = ?", refundId));
        var report = revenueService.getReport(LocalDate.now(), LocalDate.now());
        assertEquals(BigDecimal.ZERO, report.summary().totalRevenue());
        assertEquals(new BigDecimal("20000"), report.summary().totalRefunded());
    }

    private AccountDTO admin() {
        return new AccountDTO(id("account", "email", "refund-admin@test.vn"),
                "refund-admin@test.vn", "Admin hoàn tiền", null, "ADMIN", "ACTIVE");
    }

    @ParameterizedTest
    @ValueSource(strings = {"MAINTENANCE", "INACTIVE"})
    void canCancelTripAfterWaterbusStatusChanges(String waterbusStatus) {
        jdbc.update("UPDATE waterbus SET status = ? WHERE code = 'CANCEL_W'", waterbusStatus);
        assertTrue(tripService.getTripsForAdmin().stream()
                .filter(trip -> trip.id() == tripId).findFirst().orElseThrow().isCancelable());

        tripService.cancelTrip(tripId, "Tàu không thể phục vụ");
        entityManager.flush();
        assertEquals("CANCELLED", value("SELECT status FROM trip WHERE id = ?", tripId));
        assertEquals("PENDING", value("SELECT status FROM refund WHERE booking_id = ?",
                id("booking", "booking_code", "CANCEL_PAID")));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM refund", Integer.class));
    }

    @Test
    void reasonMustContainOneTo255Characters() {
        assertThrows(IllegalArgumentException.class, () -> tripService.cancelTrip(tripId, " "));
        assertThrows(IllegalArgumentException.class, () -> tripService.cancelTrip(tripId, "A".repeat(256)));
        assertEquals("SCHEDULED", value("SELECT status FROM trip WHERE id = ?", tripId));

        String reason = "A".repeat(255);
        tripService.cancelTrip(tripId, reason);
        entityManager.flush();
        assertEquals(reason, value("SELECT cancel_reason FROM trip WHERE id = ?", tripId));
    }

    @Test
    void adminListDisablesCancellationAfterDeparture() throws Exception {
        LocalDateTime past = LocalDateTime.now().minusHours(1);
        jdbc.update("UPDATE trip SET departure_time = ?, arrival_time = ? WHERE id = ?",
                past, past.plusMinutes(30), tripId);
        assertFalse(tripService.getTripsForAdmin().stream()
                .filter(trip -> trip.id() == tripId).findFirst().orElseThrow().isCancelable());

        var mvc = MockMvcBuilders.webAppContextSetup(context).build();
        AccountDTO admin = new AccountDTO(1, "admin@test.vn", "Admin", null, "ADMIN", "ACTIVE");
        String html = mvc.perform(get("/admin/trips").sessionAttr("loginAccount", admin))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        int inputStart = html.indexOf("id=\"cancelReason-" + tripId + "\"");
        assertTrue(inputStart >= 0);
        assertTrue(html.substring(inputStart, html.indexOf('>', inputStart)).contains("disabled"));
    }

    @Test
    void revenueSubtractsCompletedRefundOnRefundDate() {
        tripService.cancelTrip(tripId, "Tàu hỏng");
        entityManager.flush();
        int refundId = jdbc.queryForObject("SELECT id FROM refund WHERE booking_id = ?", Integer.class,
                id("booking", "booking_code", "CANCEL_PAID"));
        LocalDate paymentDate = LocalDate.now();
        LocalDate refundDate = paymentDate.plusDays(1);
        jdbc.update("UPDATE refund SET status = 'DONE', refunded_at = ? WHERE id = ?",
                refundDate.atTime(9, 30), refundId);
        jdbc.update("UPDATE booking SET status = 'REFUNDED' WHERE booking_code = 'CANCEL_PAID'");
        entityManager.clear();

        var paymentReport = revenueService.getReport(paymentDate, paymentDate);
        assertEquals(new BigDecimal("20000"), paymentReport.summary().totalRevenue());
        assertEquals(BigDecimal.ZERO, paymentReport.summary().totalRefunded());

        var refundReport = revenueService.getReport(refundDate, refundDate);
        assertEquals(new BigDecimal("-20000"), refundReport.summary().totalRevenue());
        assertEquals(new BigDecimal("20000"), refundReport.summary().totalRefunded());
        assertEquals(refundReport.summary().totalRevenue(), refundReport.dailyRevenue().get(0).revenue());
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
