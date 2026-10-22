package com.hsf302.waterbus.service;

import com.hsf302.waterbus.dto.AccountDTO;
import com.hsf302.waterbus.dto.BookingDTO;
import com.hsf302.waterbus.entity.Booking;
import com.hsf302.waterbus.entity.Seat;
import com.hsf302.waterbus.entity.Ticket;
import com.hsf302.waterbus.repository.TicketRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// BE4 #13: POST /booking/checkout. BE4 #14: tạo đơn (createBooking) và POST /booking. BE4 #24: gắn tài khoản. BE4 #25: GET /my-bookings. Dữ liệu test tạo bằng SQL trong H2, tự rollback sau mỗi test.
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
        // #24: chưa đăng nhập -> khách vãng lai, account_id để NULL
        assertThat(jdbc.queryForObject("SELECT account_id FROM booking", Integer.class)).isNull();
    }

    @Test
    void postBookingLinksLoggedInAccount() throws Exception {
        jdbc.update("INSERT INTO account (email, password_hash, full_name, phone, role, status, created_at) "
                + "VALUES ('lan@example.com', 'x', 'Lan', '0901234567', 'CUSTOMER', 'ACTIVE', ?)", LocalDateTime.now());
        int accountId = id("account", "email", "lan@example.com");
        AccountDTO loginAccount = new AccountDTO(accountId, "lan@example.com", "Lan", "0901234567", "CUSTOMER", "ACTIVE");

        MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).build();
        mvc.perform(post("/booking")
                        .sessionAttr("loginAccount", loginAccount) // giả lập đã đăng nhập
                        .param("tripId", String.valueOf(tripId))
                        .param("seatIds", String.valueOf(seatA1))
                        .param("ticketTypeId", String.valueOf(ticketTypeId))
                        .param("fromStationId", String.valueOf(s1))
                        .param("toStationId", String.valueOf(s2))
                        .param("contactName", "Lan").param("contactPhone", "0901234567")
                        .param("contactEmail", "lan@example.com").param("promoCode", ""))
                .andExpect(status().is3xxRedirection());
        // #24: đơn được gắn đúng tài khoản đang đăng nhập
        assertThat(jdbc.queryForObject("SELECT account_id FROM booking", Integer.class)).isEqualTo(accountId);
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

    @Test
    void checkoutShowsSelectedSeatsAndSubtotalWithoutSaving() throws Exception {
        MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).build();
        MvcResult result = mvc.perform(post("/booking/checkout")
                        .param("tripId", String.valueOf(tripId))
                        .param("seatIds", String.valueOf(seatA1), String.valueOf(seatA2))
                        .param("ticketTypeId", String.valueOf(ticketTypeId))
                        .param("fromStationId", String.valueOf(s1))
                        .param("toStationId", String.valueOf(s3)))
                .andExpect(status().isOk())
                .andExpect(view().name("booking/checkout"))
                .andExpect(model().attributeExists("trip", "seats", "ticketType", "subtotal",
                        "fromStation", "toStation", "fromStationId", "toStationId"))
                .andReturn();
        // subtotal = 15.000 x 2 ghế
        assertThat((BigDecimal) result.getModelAndView().getModel().get("subtotal")).isEqualByComparingTo("30000");
        String html = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        // Form trên trang checkout gửi tiếp sang POST /booking, mang theo đủ chuyến + ghế đã chọn
        assertThat(html).contains("action=\"/booking\"", "Tuyen test", "TEST_S1", "TEST_S3", "30.000 đ",
                "name=\"tripId\" value=\"" + tripId + "\"",
                "name=\"seatIds\" value=\"" + seatA1 + "\"", "name=\"seatIds\" value=\"" + seatA2 + "\"");
        assertThat(count("booking")).isZero(); // checkout chỉ để xem lại, chưa tạo đơn
    }

    @Test
    void checkoutGoesBackToSeatPageWhenSeatTakenOrNoSeatChosen() throws Exception {
        bookingService.createBooking(tripId, List.of(seatA1), ticketTypeId, s1, s2,
                "", null, "Nguoi khac", "0907654321", "khac@example.com");
        MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).build();
        String seatPage = "/trips/" + tripId + "/seats?fromStationId=" + s1 + "&toStationId=" + s3;

        mvc.perform(post("/booking/checkout")
                        .param("tripId", String.valueOf(tripId))
                        .param("seatIds", String.valueOf(seatA1), String.valueOf(seatA2))
                        .param("ticketTypeId", String.valueOf(ticketTypeId))
                        .param("fromStationId", String.valueOf(s1))
                        .param("toStationId", String.valueOf(s3)))
                .andExpect(redirectedUrl(seatPage))
                .andExpect(flash().attribute("errorMessage", "Ghế bạn chọn vừa có người đặt. Vui lòng chọn ghế khác."));

        mvc.perform(post("/booking/checkout") // không tick ghế nào
                        .param("tripId", String.valueOf(tripId))
                        .param("ticketTypeId", String.valueOf(ticketTypeId))
                        .param("fromStationId", String.valueOf(s1))
                        .param("toStationId", String.valueOf(s3)))
                .andExpect(redirectedUrl(seatPage))
                .andExpect(flash().attribute("errorMessage", "Vui lòng chọn ít nhất 1 ghế."));
    }

    @Test
    void myBookingsRequiresLoginAndShowsOnlyOwnBookingsNewestFirst() throws Exception {
        MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).build();
        // Chưa đăng nhập -> sang trang đăng nhập
        mvc.perform(get("/my-bookings")).andExpect(redirectedUrl("/auth/login"));

        AccountDTO lan = account("lan@example.com");
        AccountDTO minh = account("minh@example.com");
        Booking older = bookingService.createBooking(tripId, List.of(seatA1), ticketTypeId, s1, s2,
                "", lan.getId(), "Lan", "0901234567", "lan@example.com");
        Booking newer = bookingService.createBooking(tripId, List.of(seatA2), ticketTypeId, s1, s2,
                "", lan.getId(), "Lan", "0901234567", "lan@example.com");
        Booking ofMinh = bookingService.createBooking(tripId, List.of(seatA3), ticketTypeId, s1, s2,
                "", minh.getId(), "Minh", "0907654321", "minh@example.com");
        older.setCreatedAt(LocalDateTime.now().minusHours(1)); // để chắc chắn thứ tự thời gian

        // Lan chỉ thấy 2 đơn của mình, đơn mới nhất lên đầu
        MvcResult result = mvc.perform(get("/my-bookings").sessionAttr("loginAccount", lan))
                .andExpect(status().isOk())
                .andExpect(view().name("booking/history"))
                .andReturn();
        @SuppressWarnings("unchecked")
        List<BookingDTO> bookings = (List<BookingDTO>) result.getModelAndView().getModel().get("bookings");
        assertThat(bookings).extracting(BookingDTO::getBookingCode)
                .containsExactly(newer.getBookingCode(), older.getBookingCode());
        assertThat(result.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .contains(newer.getBookingCode(), older.getBookingCode()).doesNotContain(ofMinh.getBookingCode());

        // Tài khoản chưa có đơn -> danh sách rỗng
        AccountDTO empty = account("empty@example.com");
        mvc.perform(get("/my-bookings").sessionAttr("loginAccount", empty))
                .andExpect(status().isOk())
                .andExpect(model().attribute("bookings", List.of()));
    }

    // Tạo 1 tài khoản trong DB test, trả về AccountDTO giống cái Tân lưu vào session khi đăng nhập
    private AccountDTO account(String email) {
        jdbc.update("INSERT INTO account (email, password_hash, full_name, phone, role, status, created_at) "
                + "VALUES (?, 'x', 'Test', '0900000000', 'CUSTOMER', 'ACTIVE', ?)", email, LocalDateTime.now());
        return new AccountDTO(id("account", "email", email), email, "Test", "0900000000", "CUSTOMER", "ACTIVE");
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
