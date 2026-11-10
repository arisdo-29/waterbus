package com.hsf302.waterbus;

import com.hsf302.waterbus.dto.AccountDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.ModelAndView;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Import(CustomerViewTests.FixtureController.class)
class CustomerViewTests {
    @Autowired WebApplicationContext context;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void guestNavbarAndMissingAlertsRenderOnAllCustomerPages() throws Exception {
        for (String page : List.of("home", "checkout", "payment", "detail", "history")) {
            String html = render(page, fixture("PENDING"));
            assertThat(html).contains("href=\"/auth/login\"", "href=\"/auth/register\"", "<footer",
                            "<title", "src=\"/js/app.js\"", "href=\"/css/app.css\"")
                    .doesNotContain("action=\"/auth/logout\"", "href=\"/admin/trips\"", "alert-danger");
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"CUSTOMER", "ADMIN", "STAFF", "UNKNOWN", ""})
    void navbarReadsAccountDtoAndOnlyOffersAdminForExplicitRoles(String role) throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("loginAccount", account(role));
        for (String page : List.of("home", "checkout", "payment", "detail", "history")) {
            String html = render(page, fixture("PENDING"), get("/__test/views/" + page).session(session));
            assertThat(html).contains("Khách &lt;script&gt;", "href=\"/my-bookings\"", "method=\"post\"",
                            "action=\"/auth/logout\"")
                    .doesNotContain("href=\"/auth/login\"", "href=\"/auth/register\"");
            assertThat(html.contains("href=\"/admin/trips\"")).isEqualTo(role.equals("ADMIN") || role.equals("STAFF"));
        }
    }

    @Test
    void alertsEscapeUserContentOnEveryPage() throws Exception {
        for (String page : List.of("home", "checkout", "payment", "detail", "history")) {
            Map<String, Object> model = fixture("PENDING");
            model.put("successMessage", "<script>success()</script>");
            model.put("errorMessage", "<img src=x onerror=fail()>");
            assertThat(render(page, model)).contains("&lt;script&gt;success()&lt;/script&gt;", "&lt;img")
                    .doesNotContain("<script>success()", "<img src=x");
        }
    }

    @Test
    void checkoutPreservesSelectionAndSubmittedContactValuesOnValidationError() throws Exception {
        Map<String, Object> model = fixture("PENDING");
        model.put("errorMessage", "Kiểm tra lại thông tin");
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("loginAccount", account("CUSTOMER"));
        String html = render("checkout", model, get("/__test/views/checkout").session(session)
                .param("contactName", "Tên đã nhập").param("contactPhone", "0987654321")
                .param("contactEmail", "edited@example.test").param("promoCode", "SAVE"));
        assertThat(html).contains("action=\"/booking\"", "method=\"post\"", "value=\"Tên đã nhập\"",
                "value=\"0987654321\"", "value=\"edited@example.test\"", "value=\"SAVE\"")
                .containsPattern("name=\"tripId\"[^>]*value=\"71\"")
                .containsPattern("name=\"ticketTypeId\"[^>]*value=\"8\"")
                .containsPattern("name=\"fromStationId\"[^>]*value=\"11\"")
                .containsPattern("name=\"toStationId\"[^>]*value=\"12\"")
                .containsPattern("name=\"seatIds\"[^>]*value=\"91\"")
                .containsPattern("name=\"seatIds\"[^>]*value=\"92\"");
        // Explicitly empty submitted values must not be replaced with account defaults.
        assertThat(render("checkout", model, get("/__test/views/checkout").session(session)
                .param("contactName", "").param("contactPhone", "").param("contactEmail", "")))
                .containsPattern("name=\"contactName\"[^>]*value=\"\"")
                .doesNotContain("value=\"account@example.test\"");
    }

    @Test
    void confirmModalIsSharedAndBookingFormsAskBeforeSubmitting() throws Exception {
        Map<String, Object> model = fixture("PENDING");
        for (String page : List.of("home", "checkout", "payment", "detail", "history")) {
            String html = render(page, model);
            assertThat(html).contains("id=\"wbConfirmModal\"", "data-confirm-modal-ok", ">Huỷ</button>", ">Đồng ý</button>");
            assertThat(html.indexOf("id=\"wbConfirmModal\"")).isLessThan(html.indexOf("src=\"/js/app.js\""));
        }
        assertThat(render("checkout", model)).containsPattern("<button[^>]*type=\"submit\"[^>]*data-confirm=\"Đặt 2 vé chuyến "
                + "Tuyến thực \\([^\"]*\\)[^\"]*, ghế C7, C8\\?\"");
        assertThat(render("payment", model)).containsPattern("data-payment-submit[^>]*data-confirm=\"Thanh toán 42.000 đ qua VNPay\\?\"")
                .contains("data-payment-amount=\"42.000 đ\"", "action=\"/payment/REAL-42/vnpay\"")
                .doesNotContain("MOCK", "/mock", "mô phỏng");
        assertThat(render("payment", fixture("PAID"))).contains("wb-method-option is-static is-locked");
        assertThat(render("detail", model)).containsPattern("action=\"/booking/REAL-42/cancel\"[^>]*data-confirm-variant=\"danger\"");
        model.put("seats", List.of());
        model.put("trip", null);
        assertThat(render("checkout", model)).contains("data-confirm=\"Đặt 0 vé chuyến").doesNotContain("chuyến null", "null →", "lúc null");
    }

    @Test
    void checkoutAsksPassengerInfoOnlyForVerifiedTicketsAndKeepsInputsAndErrors() throws Exception {
        Map<String, Object> standard = fixture("PENDING");
        String html = render("checkout", standard);
        assertThat(html).doesNotContain("name=\"passengerNames\"", "data-passenger-section")
                .containsPattern("name=\"acceptPolicy\"[^>]*value=\"true\"[^>]*required")
                .contains("không hoàn tiền (trừ khi chuyến bị huỷ bởi nhà vận hành)", "id=\"wbPolicyModal\"",
                        "data-bs-target=\"#wbPolicyModal\"", "chuyển sang thanh toán", ">Đặt vé</span>");

        Map<String, Object> senior = fixture("PENDING");
        senior.put("ticketType", Map.of("id", 9, "code", "SENIOR_70", "name", "Người từ 70 tuổi",
                "price", java.math.BigDecimal.ZERO, "requiresVerification", true));
        senior.put("subtotal", java.math.BigDecimal.ZERO);
        org.springframework.validation.BeanPropertyBindingResult errors =
                new org.springframework.validation.BeanPropertyBindingResult(senior.get("bookingForm"), "bookingForm");
        errors.addError(new org.springframework.validation.FieldError("bookingForm", "birthYears[1]", "Hành khách ghế C8 chưa đủ 70 tuổi."));
        errors.addError(new org.springframework.validation.FieldError("bookingForm", "passengerNames", "Thiếu tên hành khách."));
        errors.addError(new org.springframework.validation.FieldError("bookingForm", "acceptPolicy", "Vui lòng đồng ý chính sách."));
        senior.put(org.springframework.validation.BindingResult.MODEL_KEY_PREFIX + "bookingForm", errors);
        html = render("checkout", senior, get("/__test/views/checkout").param("passengerNames", "Bà Một", "Ông Hai")
                .param("birthYears", "1950", "1990").param("acceptPolicy", "true"));
        assertThat(html).contains("data-passenger-section", "data-ticket-code=\"SENIOR_70\"",
                        "Mang CCCD / thẻ khuyết tật, thẻ thương binh bản gốc khi lên tàu.",
                        "value=\"Bà Một\"", "value=\"Ông Hai\"", "value=\"1950\"", "value=\"1990\"",
                        "Hành khách ghế C8 chưa đủ 70 tuổi.", "Thiếu tên hành khách.", "Vui lòng đồng ý chính sách.",
                        ">Xác nhận đặt vé</span>", "data-confirm-ok=\"Xác nhận đặt vé\"", "không cần thanh toán")
                .doesNotContain("chuyển sang thanh toán")
                .containsPattern("class=\"[^\"]*is-invalid[^\"]*\" id=\"birthYear1\"")
                .containsPattern("id=\"acceptPolicy\"[^>]*checked")
                .containsPattern("name=\"birthYears\"[^>]*min=\"1900\"[^>]*max=\"\\d{4}\"");
        assertThat(html.split("name=\"passengerNames\"", -1)).hasSize(3);
    }

    @Test
    void detailShowsPassengersGroupQrPaymentAndRefundBanners() throws Exception {
        Map<String, Object> model = fixture("PAID");
        model.put("tickets", List.of(new HashMap<>(Map.of("ticketCode", "TICKET-91", "seatCode", "C7", "fromStation", "Bến đi",
                "toStation", "Bến đến", "price", java.math.BigDecimal.ZERO, "status", "VALID",
                "passengerName", "Bà <Một>", "birthYear", 1950))));
        model.put("payment", Map.of("method", "VNPAY", "transactionRef", "VNP-778"));
        String html = render("detail", model);
        assertThat(html).contains("Bà &lt;Một&gt;", "sinh năm <span>1950</span>", "Mang giấy tờ gốc khi lên tàu",
                "data-qr-value=\"REAL-42\"", "Đưa nhân viên quét để lên tàu cả nhóm", "VNPay", "VNP-778")
                .doesNotContain("wb-refund-banner")
                .contains("Bạn chưa đăng nhập nên vé chỉ xem được bằng mã đơn");
        MockHttpSession loggedIn = new MockHttpSession();
        loggedIn.setAttribute("loginAccount", account("CUSTOMER"));
        assertThat(render("detail", model, get("/__test/views/detail").session(loggedIn)))
                .doesNotContain("Bạn chưa đăng nhập nên vé chỉ xem được bằng mã đơn");
        assertThat(render("detail", fixture("PENDING"))).doesNotContain("Bạn chưa đăng nhập nên vé chỉ xem được bằng mã đơn");

        model.put("refund", Map.of("status", "PENDING", "amount", new java.math.BigDecimal("42000")));
        model.put("tripCancelReason", "Nước lớn");
        assertThat(render("detail", model)).contains("wb-refund-banner is-pending", "Chuyến đã bị huỷ (lý do: <span>Nước lớn</span>)",
                "Đơn đang chờ hoàn tiền").doesNotContain("is-done");

        Map<String, Object> refunded = fixture("REFUNDED");
        refunded.put("refund", Map.of("status", "DONE", "amount", new java.math.BigDecimal("42000"),
                "refundedAt", LocalDateTime.of(2026, 11, 2, 9, 30), "transactionRef", "HT-001"));
        html = render("detail", refunded);
        assertThat(html).contains("wb-refund-banner is-done", "42.000 đ", "09:30 02/11/2026", "HT-001", "Đơn đã hoàn tiền")
                .doesNotContain("is-pending", "Đưa nhân viên quét", "Mang giấy tờ gốc");
        refunded.remove("refund");
        assertThat(render("detail", refunded)).contains("wb-refund-banner is-done", "Đã hoàn <strong>42.000 đ</strong>.");
    }

    @Test
    void historyShowsRefundStatusOnTableAndCards() throws Exception {
        Map<String, Object> model = fixture("PAID");
        Map<String, Object> pending = new HashMap<>(booking(model));
        pending.put("bookingCode", "WAIT-1");
        pending.put("refundStatus", "PENDING");
        Map<String, Object> done = new HashMap<>(booking(model));
        done.put("bookingCode", "DONE-1");
        done.put("refundStatus", "DONE");
        model.put("bookings", List.of(pending, done, booking(model)));
        String html = render("history", model);
        assertThat(html.split("Chờ hoàn tiền", -1)).hasSize(3);
        assertThat(html.split("</i>Đã hoàn tiền", -1)).hasSize(3);
    }

    @Test
    void adminMenuShowsRevenueRefundsAccountsForAdminAndCheckinFirstForStaff() throws Exception {
        MockHttpSession admin = new MockHttpSession();
        admin.setAttribute("loginAccount", account("ADMIN"));
        String adminMenu = render("sidebar", fixture("PENDING"), get("/__test/views/sidebar").session(admin));
        int trips = adminMenu.indexOf("data-admin-nav=\"/admin/trips\"");
        int revenue = adminMenu.indexOf("data-admin-nav=\"/admin/revenue\"");
        int refunds = adminMenu.indexOf("data-admin-nav=\"/admin/refunds\"");
        int accounts = adminMenu.indexOf("data-admin-nav=\"/admin/accounts\"");
        int waterbuses = adminMenu.indexOf("data-admin-nav=\"/admin/waterbuses\"");
        assertThat(trips).isNotNegative().isLessThan(revenue);
        assertThat(revenue).isLessThan(refunds);
        assertThat(refunds).isLessThan(accounts);
        assertThat(accounts).isLessThan(waterbuses);
        assertThat(adminMenu).contains("bi-graph-up-arrow", "bi-arrow-counterclockwise", "bi-people",
                "href=\"/admin/revenue\"", "href=\"/admin/refunds\"", "href=\"/admin/accounts\"");
        assertThat(adminMenu.split("data-admin-nav=\"/admin/checkin\"", -1)).hasSize(2);
        assertThat(adminMenu.indexOf("data-admin-nav=\"/admin/checkin\"")).isGreaterThan(waterbuses);

        MockHttpSession staff = new MockHttpSession();
        staff.setAttribute("loginAccount", account("STAFF"));
        String staffMenu = render("sidebar", fixture("PENDING"), get("/__test/views/sidebar").session(staff));
        assertThat(staffMenu.split("data-admin-nav=\"/admin/checkin\"", -1)).hasSize(2);
        assertThat(staffMenu.indexOf("data-admin-nav=\"/admin/checkin\""))
                .isLessThan(staffMenu.indexOf("data-admin-nav=\"/admin/trips\""));
        assertThat(staffMenu).doesNotContain("/admin/revenue", "/admin/refunds", "/admin/accounts", "/admin/waterbuses");

        assertThat(render("navbar", fixture("PENDING"), get("/__test/views/navbar").session(admin)))
                .contains("href=\"/admin/revenue\"");
        assertThat(render("navbar", fixture("PENDING"), get("/__test/views/navbar").session(staff)))
                .contains("href=\"/admin/trips\"").doesNotContain("/admin/revenue");
    }

    @ParameterizedTest
    @ValueSource(strings = {"PENDING", "PAID", "EXPIRED", "CANCELLED"})
    void paymentDetailAndHistoryRenderRealDataAndStatuses(String state) throws Exception {
        Map<String, Object> model = fixture(state);
        String payment = render("payment", model);
        assertThat(payment).contains("action=\"/payment/REAL-42/vnpay\"", "method=\"post\"", "42.000 đ", "C7");
        assertThat(payment).containsPattern(state.equals("PENDING")
                ? "<button[^>]*data-payment-submit(?![^>]*disabled)[^>]*>"
                : "<button[^>]*data-payment-submit[^>]*disabled=\"disabled\"");
        String detail = render("detail", model);
        assertThat(detail).contains("REAL-42", "TICKET-91", "42.000 đ", "C7")
                .doesNotContain("BK0000002", "TK0000003", "09:13 19/10/2026");
        assertThat(detail.contains("Thanh toán ngay")).isEqualTo(state.equals("PENDING"));
        assertThat(detail.contains("Huỷ đơn")).isEqualTo(state.equals("PENDING"));
        if (state.equals("PENDING")) {
            assertThat(detail).contains("action=\"/booking/REAL-42/cancel\"");
        }
        String history = render("history", model);
        assertThat(history).contains("wb-booking-table", "wb-booking-cards", "18:05 24/10/2026");
        assertThat(history.split("href=\"/booking/REAL-42\"", -1)).hasSize(3);
        assertThat(history.split("42.000 đ", -1)).hasSize(3);
    }

    @Test
    void exportRenderedFixturesForBrowserChecks() throws Exception {
        java.nio.file.Path directory = java.nio.file.Path.of("target/be1-preview");
        java.nio.file.Files.createDirectories(directory);
        for (String state : List.of("PENDING", "PAID", "EXPIRED", "CANCELLED")) {
            Map<String, Object> model = fixture(state);
            booking(model).put("expiresAt", LocalDateTime.now().plusMinutes(10));
            for (String page : List.of("payment", "detail", "history", "checkout")) {
                java.nio.file.Files.writeString(directory.resolve(page + "-" + state + ".html"), render(page, model));
            }
        }
        Map<String, Object> senior = fixture("PENDING");
        senior.put("ticketType", Map.of("id", 9, "code", "SENIOR_70", "name", "Người từ 70 tuổi",
                "price", java.math.BigDecimal.ZERO, "requiresVerification", true));
        senior.put("subtotal", java.math.BigDecimal.ZERO);
        java.nio.file.Files.writeString(directory.resolve("checkout-SENIOR.html"), render("checkout", senior));
        for (String value : List.of("missing", "invalid", "elapsed")) {
            Map<String, Object> model = fixture("PENDING");
            booking(model).put("expiresAt", switch (value) {
                case "missing" -> null;
                case "invalid" -> "invalid-date";
                default -> LocalDateTime.now().minusMinutes(1);
            });
            java.nio.file.Files.writeString(directory.resolve("payment-" + value + ".html"), render("payment", model));
        }
    }

    @Test
    void emptyCollectionsAndMissingTimesDoNotExposeSampleData() throws Exception {
        Map<String, Object> model = fixture("PENDING");
        model.put("seats", List.of());
        model.put("tickets", List.of());
        model.put("bookings", List.of());
        model.put("trip", null);
        booking(model).put("expiresAt", null);
        booking(model).put("createdAt", null);
        for (String page : List.of("checkout", "payment", "detail", "history")) {
            assertThat(render(page, model)).doesNotContain("2026-10-20T23:59:00", "08:30", "NaN", "09:12 19/10/2026");
        }
        assertThat(render("history", model)).contains("Bạn chưa có đơn nào");
        model.put("booking", null);
        assertThat(render("payment", model)).contains("Chưa có đơn để thanh toán").doesNotContain("data-payment-submit");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> booking(Map<String, Object> model) {
        return (Map<String, Object>) model.get("booking");
    }

    private String render(String page, Map<String, Object> model) throws Exception {
        return render(page, model, get("/__test/views/" + page));
    }

    private String render(String page, Map<String, Object> model, MockHttpServletRequestBuilder request) throws Exception {
        return mvc.perform(request.requestAttr("fixture", model)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private AccountDTO account(String role) {
        return new AccountDTO(3, "account@example.test", "Khách <script>", "0900000000", role, "ACTIVE");
    }

    private Map<String, Object> fixture(String state) {
        Map<String, Object> booking = new HashMap<>(Map.of(
                "bookingCode", "REAL-42", "status", state, "subtotal", new java.math.BigDecimal("45000"),
                "discountAmount", new java.math.BigDecimal("3000"), "totalAmount", new java.math.BigDecimal("42000"),
                "expiresAt", LocalDateTime.of(2026, 10, 24, 18, 15), "createdAt", LocalDateTime.of(2026, 10, 24, 18, 5),
                "contactName", "Người đặt", "contactPhone", "0912345678", "contactEmail", "guest@example.test"));
        booking.put("paidAt", state.equals("PAID") ? LocalDateTime.of(2026, 10, 24, 18, 6) : null);
        Map<String, Object> trip = new HashMap<>(Map.of("id", 71, "routeName", "Tuyến thực", "tripCode", "TRIP-71"));
        trip.put("departureTime", null);
        trip.put("arrivalTime", null);
        Map<String, Object> model = new HashMap<>();
        model.put("stations", List.of());
        model.put("trip", trip);
        model.put("booking", booking);
        model.put("bookings", List.of(booking));
        model.put("seats", List.of(Map.of("id", 91, "seatCode", "C7"), Map.of("id", 92, "seatCode", "C8")));
        model.put("ticketType", Map.of("id", 8, "code", "STANDARD", "name", "Vé thường", "price", new java.math.BigDecimal("22500"),
                "requiresVerification", false));
        model.put("bookingForm", new com.hsf302.waterbus.dto.BookingForm()); // controller thật luôn đưa bookingForm vào checkout
        model.put("subtotal", new java.math.BigDecimal("45000"));
        model.put("fromStationId", 11);
        model.put("toStationId", 12);
        model.put("fromStation", Map.of("name", "Bến đi"));
        model.put("toStation", Map.of("name", "Bến đến"));
        model.put("tickets", List.of(Map.of("ticketCode", "TICKET-91", "seatCode", "C7", "fromStation", "Bến đi",
                "toStation", "Bến đến", "price", new java.math.BigDecimal("42000"), "status", state.equals("PAID") ? "VALID" : "HOLD")));
        return model;
    }

    // Test-only routes exercise the real view resolver while other modules' endpoints are unfinished.
    @Controller
    public static class FixtureController {
        @GetMapping("/__test/views/{page}")
        @SuppressWarnings("unchecked")
        public ModelAndView render(@PathVariable String page, HttpServletRequest request) {
            String view = switch (page) {
                case "home" -> "index";
                case "checkout" -> "booking/checkout";
                case "payment" -> "payment/index";
                case "detail" -> "booking/detail";
                case "history" -> "booking/history";
                case "sidebar" -> "fragments/layout :: adminSidebar";
                case "navbar" -> "fragments/layout :: navbar";
                default -> throw new IllegalArgumentException("Unknown fixture view");
            };
            return new ModelAndView(view, (Map<String, Object>) request.getAttribute("fixture"));
        }
    }
}
