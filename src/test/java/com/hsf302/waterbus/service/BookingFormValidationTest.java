package com.hsf302.waterbus.service;

import com.hsf302.waterbus.dto.BookingForm;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
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
    @Autowired private BookingService bookingService;

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
                        "fromStation", "toStation", "fromStationId", "toStationId"))
                // L-14: alert đầu trang chỉ nhắc chung, không lặp lại từng lỗi
                .andExpect(model().attribute("errorMessage", "Vui lòng kiểm tra các ô được đánh dấu."))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(html).contains("value=\"12345\"", "value=\"abc\""); // giữ lại giá trị khách vừa gõ
        // Mỗi câu lỗi hiện đúng 1 lần (dưới ô của nó): tách HTML theo câu lỗi -> được 2 mảnh = có 1 lần
        assertThat(html.split("Vui lòng nhập họ và tên.", -1)).hasSize(2);
        assertThat(html.split("Số điện thoại phải gồm 10 chữ số", -1)).hasSize(2);
        assertThat(html.split("Email không đúng định dạng.", -1)).hasSize(2);
        assertThat(html.split("Mã khuyến mãi tối đa 30 ký tự.", -1)).hasSize(2);
        assertThat(count("booking")).isZero(); // nhập sai -> chưa tạo đơn
    }

    @Test
    void emptyPhoneOnlyAsksToEnterPhone() throws Exception {
        String html = mvc.perform(booking("Lan", "", "lan@example.com", ""))
                .andExpect(view().name("booking/checkout"))
                .andExpect(model().attributeHasFieldErrorCode("bookingForm", "contactPhone", "NotBlank"))
                .andExpect(model().attribute("errorMessage", "Vui lòng kiểm tra các ô được đánh dấu."))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(html.split("Vui lòng nhập số điện thoại.", -1)).hasSize(2); // chỉ hiện 1 lần, dưới ô SĐT
    }

    @Test
    void seatErrorStaysInAlertBecauseSeatHasNoFieldOnPage() throws Exception {
        // Vừa trống họ tên (có ô để hiện) vừa không có ghế (seatIds là ô ẩn, không có chỗ hiện lỗi)
        mvc.perform(post("/booking").param("acceptPolicy", "true")
                        .param("tripId", String.valueOf(tripId))
                        .param("ticketTypeId", String.valueOf(ticketTypeId))
                        .param("fromStationId", String.valueOf(s1)).param("toStationId", String.valueOf(s2))
                        .param("contactName", "").param("contactPhone", "0901234567")
                        .param("contactEmail", "lan@example.com"))
                .andExpect(view().name("booking/checkout"))
                .andExpect(model().attribute("errorMessage",
                        "Vui lòng kiểm tra các ô được đánh dấu. Vui lòng chọn ít nhất 1 ghế."));
    }

    @Test
    void noSeatOrMissingTripIsRejected() throws Exception {
        mvc.perform(post("/booking").param("acceptPolicy", "true")
                        .param("tripId", String.valueOf(tripId))
                        .param("ticketTypeId", String.valueOf(ticketTypeId))
                        .param("fromStationId", String.valueOf(s1)).param("toStationId", String.valueOf(s2))
                        .param("contactName", "Lan").param("contactPhone", "0901234567")
                        .param("contactEmail", "lan@example.com"))
                .andExpect(view().name("booking/checkout"))
                .andExpect(model().attributeHasFieldErrors("bookingForm", "seatIds"))
                .andExpect(model().attribute("errorMessage", "Vui lòng chọn ít nhất 1 ghế."));

        mvc.perform(post("/booking").param("acceptPolicy", "true") // thiếu tripId (form bị sửa tay) -> 400 như trước
                        .param("seatIds", String.valueOf(seatId))
                        .param("ticketTypeId", String.valueOf(ticketTypeId))
                        .param("fromStationId", String.valueOf(s1)).param("toStationId", String.valueOf(s2))
                        .param("contactName", "Lan").param("contactPhone", "0901234567")
                        .param("contactEmail", "lan@example.com"))
                .andExpect(status().isBadRequest());
        assertThat(count("booking")).isZero();
    }

    @Test
    void moreThanTenSeatsIsRejected() throws Exception {
        // Giống người dùng sửa tay request (bỏ qua JS trang chọn ghế): gửi 11 ghế
        MockHttpServletRequestBuilder request = booking("Lan", "0901234567", "lan@example.com", "");
        for (int i = 1; i <= 10; i++) {
            request.param("seatIds", String.valueOf(seatId + i)); // thêm 10 ghế nữa -> tổng 11
        }
        mvc.perform(request)
                .andExpect(view().name("booking/checkout"))
                .andExpect(model().attributeHasFieldErrors("bookingForm", "seatIds"))
                .andExpect(model().attribute("errorMessage", "Bạn chỉ có thể chọn tối đa 10 ghế trong một lần đặt."));
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
        return post("/booking").param("acceptPolicy", "true")
                .param("tripId", String.valueOf(tripId)).param("seatIds", String.valueOf(seatId))
                .param("ticketTypeId", String.valueOf(ticketTypeId))
                .param("fromStationId", String.valueOf(s1)).param("toStationId", String.valueOf(s2))
                .param("contactName", name).param("contactPhone", phone)
                .param("contactEmail", email).param("promoCode", promo);
    }

    @Test
    void priorityTicketHasNoMinimumAgeButSeniorTicketStillNeeds70() {
        jdbc.update("INSERT INTO ticket_type (code, name, price, requires_verification, is_active) "
                + "VALUES ('PRIORITY', 'Nguoi khuyet tat', 0, 1, 1)");
        jdbc.update("INSERT INTO ticket_type (code, name, price, requires_verification, is_active) "
                + "VALUES ('SENIOR_70', 'Nguoi cao tuoi', 0, 1, 1)");
        // Chuyến trong setUp chạy ngày mai -> tuổi tính theo năm của ngày mai
        int childBirthYear = LocalDate.now().plusDays(1).getYear() - 8; // trẻ 8 tuổi

        // Vé ưu tiên (khuyết tật, thương binh) cho trẻ 8 tuổi -> hợp lệ (trước đây bị chặn vì dưới 13 tuổi)
        Errors priority = passengerErrors(id("ticket_type", "code", "PRIORITY"), childBirthYear);
        assertThat(priority.hasFieldErrors("birthYears[0]")).isFalse();

        // Vé người cao tuổi vẫn giữ luật từ 70 tuổi trở lên
        Errors senior = passengerErrors(id("ticket_type", "code", "SENIOR_70"), childBirthYear);
        assertThat(senior.getFieldError("birthYears[0]").getDefaultMessage()).contains("từ 70 tuổi");
    }

    // Gọi validatePassengers với 1 ghế, 1 hành khách có năm sinh cho trước; trả về các lỗi tìm được
    private Errors passengerErrors(int ticketTypeId, int birthYear) {
        BookingForm form = new BookingForm();
        form.setTripId(tripId);
        form.setTicketTypeId(ticketTypeId);
        form.setFromStationId(s1);
        form.setToStationId(s2);
        form.setSeatIds(List.of(seatId));
        form.setPassengerNames(List.of("Be An"));
        form.setBirthYears(List.of(birthYear));
        Errors errors = new BeanPropertyBindingResult(form, "bookingForm");
        bookingService.validatePassengers(form, errors);
        return errors;
    }

    private int id(String table, String codeColumn, String code) {
        return jdbc.queryForObject("SELECT id FROM " + table + " WHERE " + codeColumn + " = ?", Integer.class, code);
    }

    private int count(String table) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
    }
}
