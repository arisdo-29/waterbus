package com.hsf302.waterbus.service;

import com.hsf302.waterbus.dto.AccountDTO;
import com.hsf302.waterbus.dto.BookingDTO;
import com.hsf302.waterbus.dto.BookingTripDTO;
import com.hsf302.waterbus.entity.Booking;
import com.hsf302.waterbus.entity.Seat;
import com.hsf302.waterbus.entity.Ticket;
import com.hsf302.waterbus.repository.SeatRepository;
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
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:booking-create-test;MODE=MSSQLServer;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1")
@Transactional
class BookingCreateTest {
    @Autowired private JdbcTemplate jdbc;
    @Autowired private BookingService bookingService;
    @Autowired private TripService tripService;
    @Autowired private TicketRepository ticketRepository;
    @Autowired private SeatRepository seatRepository;
    @Autowired private WebApplicationContext context;
    @MockitoSpyBean private TripService tripServiceSpy;

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
        jdbc.execute("CREATE UNIQUE INDEX IF NOT EXISTS UX_ticket_trip_seat_active ON ticket (trip_id, seat_id)");
        s1 = station("TEST_S1");
        s2 = station("TEST_S2");
        s3 = station("TEST_S3");
        offRouteStation = station("TEST_OFF");
        jdbc.update("INSERT INTO route (code, name, is_active) VALUES ('TEST_R1', 'Tuyen test', 1)");
        int routeId = id("route", "code", "TEST_R1");
        stop(routeId, s1, 1);
        stop(routeId, s2, 2);
        stop(routeId, s3, 3);

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
        BookingDTO created = bookingService.createBooking(BookingTestHelper.form(tripId, List.of(seatA1, seatA2),
                ticketTypeId, s1, s3, "test10", " Lan ", " 0901234567 ", " lan@example.com "), null);

        assertThat(created.getBookingCode()).matches("BK[0-9]{8}");
        assertThat(created.getStatus()).isEqualTo("PENDING");
        assertThat(created.getContactName()).isEqualTo("Lan");
        assertThat(created.getContactPhone()).isEqualTo("0901234567");
        assertThat(created.getContactEmail()).isEqualTo("lan@example.com");
        assertThat(created.getSubtotal()).isEqualByComparingTo("30000");
        assertThat(created.getDiscountAmount()).isEqualByComparingTo("3000");
        assertThat(created.getTotalAmount()).isEqualByComparingTo("27000");

        Booking booking = bookingService.getBookingByCode(created.getBookingCode());
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
        assertThat(tripService.getAvailableSeats(tripId)).extracting(Seat::getId).containsExactly(seatA3);
    }

    @Test
    void rejectsSeatThatWasJustBooked() {
        BookingTestHelper.createBooking(bookingService, tripId, List.of(seatA1), ticketTypeId, s1, s2,
                "", null, "Lan", "0901234567", "lan@example.com");

        assertThatThrownBy(() -> BookingTestHelper.createBooking(bookingService, tripId, List.of(seatA1, seatA2), ticketTypeId, s1, s2,
                "", null, "Minh", "0907654321", "minh@example.com"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Ghế vừa có người đặt");
        assertThat(count("booking")).isEqualTo(1);
        assertThat(count("ticket")).isEqualTo(1);
    }

    @Test
    void concurrentSeatConflictFromUniqueIndexShowsFriendlyCheckoutError() throws Exception {
        BookingTestHelper.createBooking(bookingService, tripId, List.of(seatA1), ticketTypeId, s1, s2,
                "", null, "Nguoi khac", "0907654321", "khac@example.com");

        Seat staleAvailability = seatRepository.findById(seatA1).orElseThrow();
        doReturn(List.of(staleAvailability)).when(tripServiceSpy).getAvailableSeats(tripId);
        BookingTripDTO tripInfo = tripService.getBookableTrip(tripId, s1, s2);
        doReturn(tripInfo).when(tripServiceSpy).getBookableTrip(tripId, s1, s2);

        MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).build();
        String html = mvc.perform(post("/booking").param("acceptPolicy", "true")
                        .param("tripId", String.valueOf(tripId))
                        .param("seatIds", String.valueOf(seatA1))
                        .param("ticketTypeId", String.valueOf(ticketTypeId))
                        .param("fromStationId", String.valueOf(s1))
                        .param("toStationId", String.valueOf(s2))
                        .param("contactName", "Lan").param("contactPhone", "0901234567")
                        .param("contactEmail", "lan@example.com").param("promoCode", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("booking/checkout"))
                .andExpect(model().attribute("errorMessage",
                        "Ghế vừa có người đặt. Vui lòng chọn ghế khác."))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertThat(html).contains("Ghế vừa có người đặt. Vui lòng chọn ghế khác.");
        assertThat(count("ticket")).isEqualTo(1);
    }

    @Test
    void priorityTicketsNeedLoginAndAtMostTwoPerBooking() {
        jdbc.update("INSERT INTO ticket_type (code, name, price, requires_verification, is_active) "
                + "VALUES ('TEST_FREE', 'Ve uu dai', 0, 1, 1)");
        int priorityTypeId = id("ticket_type", "code", "TEST_FREE");
        AccountDTO lan = account("lan@example.com");

        assertThatThrownBy(() -> BookingTestHelper.createBooking(bookingService, tripId, List.of(seatA1),
                priorityTypeId, s1, s2, "", null, "Lan", "0901234567", "lan@example.com"))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("Vui lòng đăng nhập để đặt vé ưu đãi.");
        assertThatThrownBy(() -> BookingTestHelper.createBooking(bookingService, tripId, List.of(seatA1, seatA2, seatA3),
                priorityTypeId, s1, s2, "", lan.getId(), "Lan", "0901234567", "lan@example.com"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("tối đa 2 vé ưu đãi");
        assertThat(count("booking")).isZero();

        Booking booking = BookingTestHelper.createBooking(bookingService, tripId, List.of(seatA1, seatA2),
                priorityTypeId, s1, s2, "", lan.getId(), "Lan", "0901234567", "lan@example.com");
        assertThat(booking.getStatus()).isEqualTo("PAID");
    }

    @Test
    void rejectsInvalidInputWithoutSavingAnything() {
        assertInvalid(tripId, List.of(seatA1), s3, s1, "", "0901234567", "không đúng với tuyến");
        assertInvalid(tripId, List.of(seatA1), s1, offRouteStation, "", "0901234567", "không đúng với tuyến");
        assertInvalid(cancelledTripId, List.of(seatA1), s1, s2, "", "0901234567", "không còn nhận đặt vé");
        assertInvalid(tripId, List.of(seatA1), s1, s2, "SAIMA", "0901234567", "Mã khuyến mãi không hợp lệ");
        assertInvalid(tripId, List.of(seatA1), s1, s2, "", "12345", "Số điện thoại");
        assertInvalid(tripId, List.of(), s1, s2, "", "0901234567", "ít nhất 1 ghế");
        assertInvalid(tripId, List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11), s1, s2, "", "0901234567",
                "tối đa 10 ghế");
        assertInvalid(-1, List.of(seatA1), s1, s2, "", "0901234567", "Ghế vừa có người đặt");
        assertThat(count("booking")).isZero();
        assertThat(count("ticket")).isZero();
    }

    @Test
    void departedTripStillSellsToLaterStationsButCompletedTripDoesNot() {
        int routeId = id("route", "code", "TEST_R1");
        jdbc.update("UPDATE route_station SET minutes_from_start = 20 WHERE route_id = ? AND station_id = ?", routeId, s2);
        jdbc.update("UPDATE route_station SET minutes_from_start = 40 WHERE route_id = ? AND station_id = ?", routeId, s3);
        int busId = id("waterbus", "code", "TEST_WB1");
        int departedTripId = trip("TEST_TRIP_DEP", routeId, busId, LocalDateTime.now().minusMinutes(5), "DEPARTED");

        Booking booking = BookingTestHelper.createBooking(bookingService, departedTripId, List.of(seatA1),
                ticketTypeId, s2, s3, "", null, "Lan", "0901234567", "lan@example.com");
        assertThat(booking.getStatus()).isEqualTo("PENDING");
        assertInvalid(departedTripId, List.of(seatA2), s1, s3, "", "0901234567", "ngừng bán vé online");

        int completedTripId = trip("TEST_TRIP_DONE", routeId, busId, LocalDateTime.now().plusHours(3), "COMPLETED");
        assertInvalid(completedTripId, List.of(seatA1), s2, s3, "", "0901234567", "không còn nhận đặt vé");
    }

    @Test
    void stopsOnlineSalesByTimeShipReachesBoardingStation() throws Exception {
        int routeId = id("route", "code", "TEST_R1");
        jdbc.update("UPDATE route_station SET minutes_from_start = 20 WHERE route_id = ? AND station_id = ?", routeId, s2);
        jdbc.update("UPDATE route_station SET minutes_from_start = 40 WHERE route_id = ? AND station_id = ?", routeId, s3);
        int busId = id("waterbus", "code", "TEST_WB1");
        int soonTripId = trip("TEST_TRIP_SOON", routeId, busId, LocalDateTime.now().plusMinutes(5), "SCHEDULED");

        assertInvalid(soonTripId, List.of(seatA1), s1, s3, "", "0901234567", "ngừng bán vé online");
        assertThat(count("booking")).isZero();

        Booking booking = BookingTestHelper.createBooking(bookingService, soonTripId, List.of(seatA1), ticketTypeId, s2, s3,
                "", null, "Lan", "0901234567", "lan@example.com");
        assertThat(booking.getStatus()).isEqualTo("PENDING");

        MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).build();
        mvc.perform(checkout(soonTripId, s1, s3)).andExpect(redirectedUrl("/trips"));
        MvcResult result = mvc.perform(checkout(soonTripId, s2, s3))
                .andExpect(view().name("booking/checkout"))
                .andReturn();
        BookingTripDTO trip = (BookingTripDTO) result.getModelAndView().getModel().get("trip");
        assertThat(trip.getDepartureTime()).isAfter(LocalDateTime.now().plusMinutes(20));
    }

    private MockHttpServletRequestBuilder checkout(int trip, int from, int to) {
        return post("/booking/checkout")
                .param("tripId", String.valueOf(trip))
                .param("seatIds", String.valueOf(seatA2))
                .param("ticketTypeId", String.valueOf(ticketTypeId))
                .param("fromStationId", String.valueOf(from))
                .param("toStationId", String.valueOf(to));
    }

    @Test
    void postBookingRedirectsToPaymentWhenSuccessful() throws Exception {
        MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).build();
        String location = mvc.perform(post("/booking").param("acceptPolicy", "true")
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
        assertThat(jdbc.queryForObject("SELECT account_id FROM booking", Integer.class)).isNull();
    }

    @Test
    void postBookingLinksLoggedInAccount() throws Exception {
        jdbc.update("INSERT INTO account (email, password_hash, full_name, phone, role, status, created_at) "
                + "VALUES ('lan@example.com', 'x', 'Lan', '0901234567', 'CUSTOMER', 'ACTIVE', ?)", LocalDateTime.now());
        int accountId = id("account", "email", "lan@example.com");
        AccountDTO loginAccount = new AccountDTO(accountId, "lan@example.com", "Lan", "0901234567", "CUSTOMER", "ACTIVE");

        MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).build();
        mvc.perform(post("/booking").param("acceptPolicy", "true")
                        .sessionAttr("loginAccount", loginAccount)
                        .param("tripId", String.valueOf(tripId))
                        .param("seatIds", String.valueOf(seatA1))
                        .param("ticketTypeId", String.valueOf(ticketTypeId))
                        .param("fromStationId", String.valueOf(s1))
                        .param("toStationId", String.valueOf(s2))
                        .param("contactName", "Lan").param("contactPhone", "0901234567")
                        .param("contactEmail", "lan@example.com").param("promoCode", ""))
                .andExpect(status().is3xxRedirection());
        assertThat(jdbc.queryForObject("SELECT account_id FROM booking", Integer.class)).isEqualTo(accountId);
    }

    @Test
    void postBookingShowsCheckoutAgainWithErrorAndKeepsInput() throws Exception {
        BookingTestHelper.createBooking(bookingService, tripId, List.of(seatA1), ticketTypeId, s1, s2,
                "", null, "Nguoi khac", "0907654321", "khac@example.com");

        MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).build();
        String html = mvc.perform(post("/booking").param("acceptPolicy", "true")
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
        assertThat((BigDecimal) result.getModelAndView().getModel().get("subtotal")).isEqualByComparingTo("30000");
        String html = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(html).contains("action=\"/booking\"", "Tuyen test", "TEST_S1", "TEST_S3", "30.000 đ",
                "name=\"tripId\" value=\"" + tripId + "\"",
                "name=\"seatIds\" value=\"" + seatA1 + "\"", "name=\"seatIds\" value=\"" + seatA2 + "\"");
        assertThat(count("booking")).isZero();
    }

    @Test
    void checkoutGoesBackToSeatPageWhenSeatTakenOrNoSeatChosen() throws Exception {
        BookingTestHelper.createBooking(bookingService, tripId, List.of(seatA1), ticketTypeId, s1, s2,
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

        mvc.perform(post("/booking/checkout")
                        .param("tripId", String.valueOf(tripId))
                        .param("ticketTypeId", String.valueOf(ticketTypeId))
                        .param("fromStationId", String.valueOf(s1))
                        .param("toStationId", String.valueOf(s3)))
                .andExpect(redirectedUrl(seatPage))
                .andExpect(flash().attribute("errorMessage", "Vui lòng chọn ít nhất 1 ghế."));
    }

    @Test
    void checkoutWithoutStationsGoesBackToTripSearch() throws Exception {
        MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).build();
        String message = "Vui lòng chọn bến đi và bến đến trước khi đặt vé.";

        mvc.perform(post("/booking/checkout")
                        .param("tripId", String.valueOf(tripId))
                        .param("seatIds", String.valueOf(seatA1))
                        .param("ticketTypeId", String.valueOf(ticketTypeId)))
                .andExpect(redirectedUrl("/trips"))
                .andExpect(flash().attribute("errorMessage", message));

        mvc.perform(post("/booking/checkout")
                        .param("tripId", String.valueOf(tripId))
                        .param("seatIds", String.valueOf(seatA1))
                        .param("ticketTypeId", String.valueOf(ticketTypeId))
                        .param("fromStationId", "")
                        .param("toStationId", ""))
                .andExpect(redirectedUrl("/trips"))
                .andExpect(flash().attribute("errorMessage", message));
        assertThat(count("booking")).isZero();
    }

    @Test
    void myBookingsRequiresLoginAndShowsOnlyOwnBookingsNewestFirst() throws Exception {
        MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).build();
        mvc.perform(get("/my-bookings")).andExpect(redirectedUrl("/auth/login"));

        AccountDTO lan = account("lan@example.com");
        AccountDTO minh = account("minh@example.com");
        Booking older = BookingTestHelper.createBooking(bookingService, tripId, List.of(seatA1), ticketTypeId, s1, s2,
                "", lan.getId(), "Lan", "0901234567", "lan@example.com");
        Booking newer = BookingTestHelper.createBooking(bookingService, tripId, List.of(seatA2), ticketTypeId, s1, s2,
                "", lan.getId(), "Lan", "0901234567", "lan@example.com");
        Booking ofMinh = BookingTestHelper.createBooking(bookingService, tripId, List.of(seatA3), ticketTypeId, s1, s2,
                "", minh.getId(), "Minh", "0907654321", "minh@example.com");
        older.setCreatedAt(LocalDateTime.now().minusHours(1));

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

        AccountDTO empty = account("empty@example.com");
        mvc.perform(get("/my-bookings").sessionAttr("loginAccount", empty))
                .andExpect(status().isOk())
                .andExpect(model().attribute("bookings", List.of()));
    }

    @Test
    void myBookingsReturnsToSamePageAfterLogin() throws Exception {
        jdbc.update("INSERT INTO account (email, password_hash, full_name, phone, role, status, created_at) "
                        + "VALUES ('an@example.com', ?, 'An', '0900000001', 'CUSTOMER', 'ACTIVE', ?)",
                new BCryptPasswordEncoder().encode("Matkhau@123"), LocalDateTime.now());
        MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).build();
        MockHttpSession session = new MockHttpSession();

        mvc.perform(get("/my-bookings").session(session)).andExpect(redirectedUrl("/auth/login"));
        assertThat(session.getAttribute("redirectAfterLogin")).isEqualTo("/my-bookings");

        mvc.perform(post("/auth/login").session(session)
                        .param("email", "an@example.com").param("password", "Matkhau@123"))
                .andExpect(redirectedUrl("/my-bookings"));
    }

    @Test
    void checkoutFillsContactFromLoggedInAccount() throws Exception {
        AccountDTO lan = new AccountDTO(1, "lan@example.com", "Nguyen Thi Lan", "0901234567", "CUSTOMER", "ACTIVE");
        MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).build();

        String html = mvc.perform(checkoutRequest().sessionAttr("loginAccount", lan))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(html).contains("value=\"Nguyen Thi Lan\"", "value=\"0901234567\"", "value=\"lan@example.com\"");

        String guestHtml = mvc.perform(checkoutRequest())
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(guestHtml).doesNotContain("Nguyen Thi Lan", "lan@example.com");
    }

    private MockHttpServletRequestBuilder checkoutRequest() {
        return post("/booking/checkout")
                .param("tripId", String.valueOf(tripId))
                .param("seatIds", String.valueOf(seatA1))
                .param("ticketTypeId", String.valueOf(ticketTypeId))
                .param("fromStationId", String.valueOf(s1))
                .param("toStationId", String.valueOf(s3));
    }

    private AccountDTO account(String email) {
        jdbc.update("INSERT INTO account (email, password_hash, full_name, phone, role, status, created_at) "
                + "VALUES (?, 'x', 'Test', '0900000000', 'CUSTOMER', 'ACTIVE', ?)", email, LocalDateTime.now());
        return new AccountDTO(id("account", "email", email), email, "Test", "0900000000", "CUSTOMER", "ACTIVE");
    }

    private void assertInvalid(int trip, List<Integer> seats, int from, int to, String promo, String phone,
                               String message) {
        assertThatThrownBy(() -> BookingTestHelper.createBooking(bookingService, trip, seats, ticketTypeId, from, to,
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
