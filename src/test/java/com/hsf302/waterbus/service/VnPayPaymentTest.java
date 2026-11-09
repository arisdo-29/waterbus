package com.hsf302.waterbus.service;

import com.hsf302.waterbus.entity.Booking;
import com.hsf302.waterbus.service.impl.VnPayServiceImpl;
import jakarta.persistence.EntityManager;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// BE4: thanh toán VNPay Sandbox. Không gọi VNPay thật: test tự tạo dữ liệu "VNPay trả về"
// rồi ký bằng chính buildQuery + hmacSHA512 với khoá test bên dưới.
@SpringBootTest
@Transactional
@TestPropertySource(properties = {"vnpay.tmn-code=TESTCODE", "vnpay.hash-secret=TESTSECRET"})
class VnPayPaymentTest {
    @Autowired private JdbcTemplate jdbc;
    @Autowired private BookingService bookingService;
    @Autowired private VnPayService vnPayService;
    @Autowired private EntityManager entityManager;
    @Autowired private WebApplicationContext context;

    private MockMvc mvc;
    private Booking booking; // đơn PENDING, 2 vé HOLD, có mã khuyến mãi: 2 x 15.000 - 10% = 27.000

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        // Dữ liệu mẫu giống PaymentMockTest
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

        booking = bookingService.createBooking(tripId, seatIds, ticketTypeId, s1, s2,
                "TEST10", null, "Lan", "0901234567", "lan@example.com");
    }

    @Test
    void paymentUrlHasRightAmountAndValidSignature() {
        String url = vnPayService.createPaymentUrl(booking.getBookingCode(), "0:0:0:0:0:0:0:1");

        assertThat(url).startsWith("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?");
        // Tách phần dữ liệu và chữ ký ra khỏi URL
        String query = url.substring(url.indexOf('?') + 1, url.indexOf("&vnp_SecureHash="));
        String hash = url.substring(url.indexOf("&vnp_SecureHash=") + "&vnp_SecureHash=".length());
        Map<String, String> params = parseQuery(query);

        assertThat(params.get("vnp_Amount")).isEqualTo("2700000"); // 27.000 đ x 100
        assertThat(params.get("vnp_TmnCode")).isEqualTo("TESTCODE");
        assertThat(params.get("vnp_TxnRef")).startsWith(booking.getBookingCode())
                .hasSize(booking.getBookingCode().length() + 14); // mã đơn + yyyyMMddHHmmss
        assertThat(params.get("vnp_IpAddr")).isEqualTo("127.0.0.1"); // IPv6 localhost -> IPv4
        assertThat(params).hasSize(13);
        assertThat(hash).isEqualTo(VnPayServiceImpl.hmacSHA512("TESTSECRET", query)); // chữ ký đúng
        assertThat(count("payment")).isZero(); // tạo link không ghi gì vào DB
    }

    @Test
    void allowedBankCodeIsSentAndSigned() {
        for (String bankCode : List.of("VNPAYQR", "VNBANK")) {
            String url = vnPayService.createPaymentUrl(booking.getBookingCode(), "127.0.0.1", bankCode);
            String query = url.substring(url.indexOf('?') + 1, url.indexOf("&vnp_SecureHash="));
            String hash = url.substring(url.indexOf("&vnp_SecureHash=") + "&vnp_SecureHash=".length());
            Map<String, String> params = parseQuery(query);

            assertThat(params.get("vnp_BankCode")).isEqualTo(bankCode);
            assertThat(params).hasSize(14); // 13 tham số cũ + vnp_BankCode
            // vnp_BankCode nằm trong phần được ký -> ai sửa mã trên URL thì VNPay báo sai chữ ký
            assertThat(hash).isEqualTo(VnPayServiceImpl.hmacSHA512("TESTSECRET", query));
        }
    }

    @Test
    void emptyOrUnknownBankCodeIsNotSent() {
        // Arrays.asList (không dùng List.of) vì List.of không cho chứa null
        for (String bankCode : Arrays.asList(null, "", "ABC", "vnpayqr")) {
            String url = vnPayService.createPaymentUrl(booking.getBookingCode(), "127.0.0.1", bankCode);
            assertThat(url).as("bankCode = %s", bankCode).doesNotContain("vnp_BankCode");
        }
    }

    @Test
    void vnPayButtonPassesBankCodeToPaymentUrl() throws Exception {
        // Giống form trang thanh toán: bankCode nằm trên URL action, kèm field method
        String qrUrl = mvc.perform(post("/payment/" + booking.getBookingCode() + "/vnpay?bankCode=VNPAYQR").param("method", "VNPAY"))
                .andExpect(status().is3xxRedirection())
                .andReturn().getResponse().getRedirectedUrl();
        assertThat(qrUrl).startsWith("https://sandbox.vnpayment.vn/").contains("&vnp_BankCode=VNPAYQR&");

        String plainUrl = mvc.perform(post("/payment/" + booking.getBookingCode() + "/vnpay").param("method", "VNPAY"))
                .andExpect(status().is3xxRedirection())
                .andReturn().getResponse().getRedirectedUrl();
        assertThat(plainUrl).startsWith("https://sandbox.vnpayment.vn/").doesNotContain("vnp_BankCode");

        assertThat(count("payment")).isZero(); // chỉ tạo link, chưa ghi gì
    }

    @Test
    void successfulReturnMarksBookingPaid() throws Exception {
        mvc.perform(vnPayReturn(returnParams("2700000", "00", "00"), "TESTSECRET"))
                .andExpect(redirectedUrl("/booking/" + booking.getBookingCode()))
                .andExpect(flash().attribute("successMessage", "Thanh toán VNPay thành công! Vé của bạn đã sẵn sàng."));
        entityManager.flush(); // ghi thay đổi xuống DB trước khi đọc lại bằng SQL

        assertThat(bookingStatus()).isEqualTo("PAID");
        assertThat(jdbc.queryForMap("SELECT method, status, transaction_ref, amount FROM payment WHERE booking_id = ?", booking.getId()))
                .containsEntry("method", "VNPAY").containsEntry("status", "SUCCESS")
                .containsEntry("transaction_ref", "14123456");
        assertThat(jdbc.queryForList("SELECT status FROM ticket WHERE booking_id = ?", String.class, booking.getId()))
                .containsExactly("VALID", "VALID");
        assertThat(promotionUsedCount()).isEqualTo(1);
    }

    @Test
    void returningTwiceKeepsOnePaymentAndCountsPromotionOnce() throws Exception {
        Map<String, String> params = returnParams("2700000", "00", "00");
        mvc.perform(vnPayReturn(params, "TESTSECRET")).andExpect(redirectedUrl("/booking/" + booking.getBookingCode()));
        // Khách bấm F5 trang kết quả -> VNPay gửi lại y hệt
        mvc.perform(vnPayReturn(params, "TESTSECRET")).andExpect(redirectedUrl("/booking/" + booking.getBookingCode()));
        entityManager.flush();

        assertThat(count("payment")).isEqualTo(1);
        assertThat(promotionUsedCount()).isEqualTo(1);
    }

    @Test
    void wrongSignatureIsRejectedWithoutTouchingDatabase() throws Exception {
        // Ký bằng chuỗi bí mật khác = giống ai đó tự sửa URL
        mvc.perform(vnPayReturn(returnParams("2700000", "00", "00"), "SAISECRET"))
                .andExpect(redirectedUrl("/"))
                .andExpect(flash().attribute("errorMessage", "Dữ liệu thanh toán trả về không hợp lệ."));
        entityManager.flush();

        assertThat(bookingStatus()).isEqualTo("PENDING");
        assertThat(count("payment")).isZero();
    }

    @Test
    void wrongAmountIsRejected() throws Exception {
        // Chữ ký đúng nhưng số tiền chỉ 1.000 đ (khác 27.000 đ của đơn)
        mvc.perform(vnPayReturn(returnParams("100000", "00", "00"), "TESTSECRET"))
                .andExpect(redirectedUrl("/payment/" + booking.getBookingCode()))
                .andExpect(flash().attribute("errorMessage", "Số tiền thanh toán không khớp với đơn."));
        entityManager.flush();

        assertThat(bookingStatus()).isEqualTo("PENDING");
        assertThat(count("payment")).isZero();
    }

    @Test
    void customerCancelOnVnPayShowsMessage() throws Exception {
        // Mã 24 = khách bấm "Huỷ" trên trang VNPay
        mvc.perform(vnPayReturn(returnParams("2700000", "24", "02"), "TESTSECRET"))
                .andExpect(redirectedUrl("/payment/" + booking.getBookingCode()))
                .andExpect(flash().attribute("errorMessage", "Bạn đã huỷ thanh toán VNPay."));
        entityManager.flush();

        assertThat(bookingStatus()).isEqualTo("PENDING");
        assertThat(count("payment")).isZero();
    }

    @Test
    void expiredBookingKeepsPaymentRecordButIsNotPaid() throws Exception {
        booking.setExpiresAt(LocalDateTime.now().minusMinutes(1)); // giả lập: khách trả tiền khi đã quá 10 phút giữ chỗ

        mvc.perform(vnPayReturn(returnParams("2700000", "00", "00"), "TESTSECRET"))
                .andExpect(redirectedUrl("/payment/" + booking.getBookingCode()))
                .andExpect(flash().attribute("errorMessage",
                        "VNPay đã trừ tiền nhưng đơn không còn hiệu lực. Vui lòng liên hệ nhân viên kèm mã đơn "
                                + booking.getBookingCode() + "."));
        entityManager.flush();

        // Có payment SUCCESS làm bằng chứng tiền đã trừ, nhưng đơn/vé/mã KM không đổi
        assertThat(jdbc.queryForMap("SELECT method, status FROM payment WHERE booking_id = ?", booking.getId()))
                .containsEntry("method", "VNPAY").containsEntry("status", "SUCCESS");
        assertThat(bookingStatus()).isEqualTo("PENDING");
        assertThat(jdbc.queryForList("SELECT status FROM ticket WHERE booking_id = ?", String.class, booking.getId()))
                .containsExactly("HOLD", "HOLD");
        assertThat(promotionUsedCount()).isZero();
    }

    // Tham số giống hệt VNPay gửi kèm khi trả khách về (chưa có chữ ký)
    private Map<String, String> returnParams(String amount, String responseCode, String transactionStatus) {
        Map<String, String> params = new HashMap<>();
        params.put("vnp_Amount", amount);
        params.put("vnp_BankCode", "NCB");
        params.put("vnp_CardType", "ATM");
        params.put("vnp_OrderInfo", "Thanh toan don " + booking.getBookingCode());
        params.put("vnp_PayDate", "20261109120500");
        params.put("vnp_ResponseCode", responseCode);
        params.put("vnp_TmnCode", "TESTCODE");
        params.put("vnp_TransactionNo", "14123456");
        params.put("vnp_TransactionStatus", transactionStatus);
        params.put("vnp_TxnRef", booking.getBookingCode() + "20261109120000");
        return params;
    }

    // GET /payment/vnpay-return kèm các tham số trên và chữ ký tính bằng secret
    private MockHttpServletRequestBuilder vnPayReturn(Map<String, String> params, String secret) {
        MockHttpServletRequestBuilder request = get("/payment/vnpay-return");
        params.forEach((name, value) -> request.param(name, value));
        request.param("vnp_SecureHash", VnPayServiceImpl.hmacSHA512(secret, VnPayServiceImpl.buildQuery(params)));
        request.param("vnp_SecureHashType", "HmacSHA512");
        return request;
    }

    // "a=1&b=x+y" -> {a=1, b=x y}
    private Map<String, String> parseQuery(String query) {
        Map<String, String> params = new HashMap<>();
        for (String pair : query.split("&")) {
            String[] parts = pair.split("=", 2);
            params.put(parts[0], URLDecoder.decode(parts[1], StandardCharsets.US_ASCII));
        }
        return params;
    }

    private String bookingStatus() {
        return jdbc.queryForObject("SELECT status FROM booking WHERE id = ?", String.class, booking.getId());
    }

    private int promotionUsedCount() {
        return jdbc.queryForObject("SELECT used_count FROM promotion WHERE code = 'TEST10'", Integer.class);
    }

    private int id(String table, String codeColumn, String code) {
        return jdbc.queryForObject("SELECT id FROM " + table + " WHERE " + codeColumn + " = ?", Integer.class, code);
    }

    private int count(String table) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
    }
}
