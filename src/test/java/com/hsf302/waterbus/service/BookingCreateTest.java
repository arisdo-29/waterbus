package com.hsf302.waterbus.service;

import com.hsf302.waterbus.entity.Booking;
import com.hsf302.waterbus.entity.Seat;
import com.hsf302.waterbus.entity.Ticket;
import com.hsf302.waterbus.repository.TicketRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// BE4 #14: tạo đơn (createBooking) và POST /booking. Dữ liệu test tạo bằng SQL trong H2, tự rollback sau mỗi test.
@SpringBootTest
@Transactional
class BookingCreateTest {
    @Autowired private JdbcTemplate jdbc;
    @Autowired private BookingService bookingService;
    @Autowired private TripService tripService;
    @Autowired private TicketRepository ticketRepository;
    @Autowired private WebApplicationContext context;

    private int tripId;
    private int cancelledTripId;
    private int seatA1;
    private int seatA2;
    private int seatA3;
    private int ticketTypeId;
    private int s1;
    private int s2;
    private int s3;
    private int offRouteStation;

    @BeforeEach
    void setUp() {
        // Tuyến S1 -> S2 -> S3, 1 trạm không thuộc tuyến
        s1 = station("TEST_S1");
        s2 = station("TEST_S2");
        s3 = station("TEST_S3");
        offRouteStation = station("TEST_OFF");
        jdbc.update("INSERT INTO route (code, name, is_active) VALUES ('TEST_R1', 'Tuyen test', 1)");
        int routeId = id("route", "code", "TEST_R1");
        stop(routeId, s1, 1);
        stop(routeId, s2, 2);
        stop(routeId, s3, 3);

        // Tàu 3 ghế, 1 chuyến đang bán + 1 chuyến đã huỷ
        jdbc.update("INSERT INTO waterbus (code, name, capacity, status) VALUES ('TEST_WB1', 'Tau test', 3, 'ACTIVE')");
        int busId = id("waterbus", "code", "TEST_WB1");
        seatA1 = seat(busId, "A1", 1);
        seatA2 = seat(busId, "A2", 2);
        seatA3 = seat(busId, "A3", 3);
        LocalDateTime departure = LocalDateTime.now().plusDays(1);
        tripId = trip("TEST_TRIP1", routeId, busId, departure, "SCHEDULED");
        cancelledTripId = trip("TEST_TRIP2", routeId, busId, departure.plusHours(2), "CANCELLED");

        jdbc.update("INSERT INTO ticket_type (code, name, price, requires_verification, is_active) "
                + "VALUES ('TEST_STD', 'Ve thuong', 15000, 0, 1)");
        ticketTypeId = id("ticket_type", "code", "TEST_STD");
        jdbc.update("INSERT INTO promotion (code, name, discount_type, discount_value, min_order_amount, "
                        + "start_at, end_at, used_count, is_active) VALUES ('TEST10', 'Giam 10%', 'PERCENT', 10, 0, ?, ?, 0, 1)",
                LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1));
    }

    @Test
    void createsPendingBookingWithHoldTicketsAndDiscount() {
        Booking booking = bookingService.createBooking(tripId, List.of(seatA1, seatA2), ticketTypeId, s1, s3,
                "test10", null, " Lan ", "0901234567", "lan@example.com");

        assertThat(booking.getBookingCode()).matches("BK[0-9]{8}");
        assertThat(booking.getStatus()).isEqualTo("PENDING");
        assertThat(booking.getContactName()).isEqualTo("Lan");
        assertThat(booking.getSubtotal()).isEqualByComparingTo("30000");
        assertThat(booking.getDiscountAmount()).isEqualByComparingTo("3000");
        assertThat(booking.getTotalAmount()).isEqualByComparingTo("27000");
        assertThat(booking.getPromotion().getCode()).isEqualTo("TEST10");
        assertThat(booking.getExpiresAt()).isEqualTo(booking.getCreatedAt().plusMinutes(10));

        List<Ticket> tickets = ticketRepository.findByBookingIdOrderByIdAsc(booking.getId());
        assertThat(tickets).hasSize(2);
        assertThat(tickets).allSatisfy(t -> {
            assertThat(t.getStatus()).isEqualTo("HOLD");
            assertThat(t.getPrice()).isEqualByComparingTo("15000");
            assertThat(t.getFromStation().getId()).isEqualTo(s1);
            assertThat(t.getToStation().getId()).isEqualTo(s3);
            assertThat(t.getTrip().getId()).isEqualTo(tripId);
        });
        assertThat(tickets).extracting(Ticket::getTicketCode)
                .containsExactly("TK" + booking.getBookingCode().substring(2) + "-1",
                        "TK" + booking.getBookingCode().substring(2) + "-2");
        // 2 ghế vừa đặt không còn trống nữa
        assertThat(tripService.getAvailableSeats(tripId)).extracting(Seat::getId).containsExactly(seatA3);
    }

    @Test
    void rejectsSeatThatWasJustBooked() {
        bookingService.createBooking(tripId, List.of(seatA1), ticketTypeId, s1, s2,
                "", null, "Lan", "0901234567", "lan@example.com");

        assertThatThrownBy(() -> bookingService.createBooking(tripId, List.of(seatA1, seatA2), ticketTypeId, s1, s2,
                "", null, "Minh", "0907654321", "minh@example.com"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Ghế vừa có người đặt");
        assertThat(count("booking")).isEqualTo(1);
        assertThat(count("ticket")).isEqualTo(1);
    }

    @Test
    void rejectsInvalidInputWithoutSavingAnything() {
        assertInvalid(tripId, List.of(seatA1), s3, s1, "", "0901234567", "không đúng với tuyến"); // đi ngược chiều
        assertInvalid(tripId, List.of(seatA1), s1, offRouteStation, "", "0901234567", "không đúng với tuyến");
        assertInvalid(cancelledTripId, List.of(seatA1), s1, s2, "", "0901234567", "không còn nhận đặt vé");
        assertInvalid(tripId, List.of(seatA1), s1, s2, "SAIMA", "0901234567", "Mã khuyến mãi không hợp lệ");
        assertInvalid(tripId, List.of(seatA1), s1, s2, "", "12345", "Số điện thoại");
        assertInvalid(tripId, List.of(), s1, s2, "", "0901234567", "ít nhất 1 ghế");
        assertInvalid(-1, List.of(seatA1), s1, s2, "", "0901234567", "Ghế vừa có người đặt"); // chuyến không tồn tại
        assertThat(count("booking")).isZero();
        assertThat(count("ticket")).isZero();
    }

    @Test
    void postBookingRedirectsToPaymentWhenSuccessful() throws Exception {
        MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).build();
        String location = mvc.perform(post("/booking")
                        .param("tripId", String.valueOf(tripId))
                        .param("seatIds", String.valueOf(seatA1), String.valueOf(seatA2))
                        .param("ticketTypeId", String.valueOf(ticketTypeId))
                        .param("fromStationId", String.valueOf(s1))
                        .param("toStationId", String.valueOf(s2))
                        .param("contactName", "Lan").param("contactPhone", "0901234567")
                        .param("contactEmail", "lan@example.com").param("promoCode", ""))
                .andExpect(status().is3xxRedirection())
                .andReturn().getResponse().getRedirectedUrl();
        assertThat(location).matches("/payment/BK[0-9]{8}");
        assertThat(count("ticket")).isEqualTo(2);
    }

    @Test
    void postBookingShowsCheckoutAgainWithErrorAndKeepsInput() throws Exception {
        bookingService.createBooking(tripId, List.of(seatA1), ticketTypeId, s1, s2,
                "", null, "Nguoi khac", "0907654321", "khac@example.com");

        MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).build();
        String html = mvc.perform(post("/booking")
                        .param("tripId", String.valueOf(tripId))
                        .param("seatIds", String.valueOf(seatA1), String.valueOf(seatA2))
                        .param("ticketTypeId", String.valueOf(ticketTypeId))
                        .param("fromStationId", String.valueOf(s1))
                        .param("toStationId", String.valueOf(s2))
                        .param("contactName", "Lan Anh").param("contactPhone", "0901234567")
                        .param("contactEmail", "lananh@example.com").param("promoCode", "TEST10"))
                .andExpect(status().isOk())
                .andExpect(view().name("booking/checkout"))
                .andExpect(model().attributeExists("trip", "seats", "ticketType", "subtotal",
                        "fromStation", "toStation", "fromStationId", "toStationId", "errorMessage"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        // Ghế A1 đã bị đặt nên chỉ còn A2; dữ liệu khách nhập vẫn còn trên form
        assertThat(html).contains("Ghế vừa có người đặt", "Tuyen test", "TEST_S1", "A2", "15.000 đ",
                "value=\"Lan Anh\"", "value=\"lananh@example.com\"", "value=\"TEST10\"");
        assertThat(count("booking")).isEqualTo(1);
    }

    private void assertInvalid(int trip, List<Integer> seats, int from, int to, String promo, String phone,
                               String message) {
        assertThatThrownBy(() -> bookingService.createBooking(trip, seats, ticketTypeId, from, to,
                promo, null, "Lan", phone, "lan@example.com"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(message);
    }

    private int station(String code) {
        jdbc.update("INSERT INTO station (code, name, is_active) VALUES (?, ?, 1)", code, code);
        return id("station", "code", code);
    }

    private void stop(int routeId, int stationId, int order) {
        jdbc.update("INSERT INTO route_station (route_id, station_id, stop_order, minutes_from_start) "
                + "VALUES (?, ?, ?, 0)", routeId, stationId, order);
    }

    private int seat(int busId, String code, int col) {
        jdbc.update("INSERT INTO seat (waterbus_id, seat_code, seat_row, seat_col, is_active) VALUES (?, ?, 1, ?, 1)",
                busId, code, col);
        return jdbc.queryForObject("SELECT id FROM seat WHERE waterbus_id = ? AND seat_code = ?",
                Integer.class, busId, code);
    }

    private int trip(String code, int routeId, int busId, LocalDateTime departure, String status) {
        jdbc.update("INSERT INTO trip (trip_code, route_id, waterbus_id, departure_time, arrival_time, status) "
                + "VALUES (?, ?, ?, ?, ?, ?)", code, routeId, busId, departure, departure.plusMinutes(30), status);
        return id("trip", "trip_code", code);
    }

    private int id(String table, String codeColumn, String code) {
        return jdbc.queryForObject("SELECT id FROM " + table + " WHERE " + codeColumn + " = ?", Integer.class, code);
    }

    private int count(String table) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
    }
}
