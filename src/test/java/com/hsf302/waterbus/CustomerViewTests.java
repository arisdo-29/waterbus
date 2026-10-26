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

    @ParameterizedTest
    @ValueSource(strings = {"PENDING", "PAID", "EXPIRED", "CANCELLED"})
    void paymentDetailAndHistoryRenderRealDataAndStatuses(String state) throws Exception {
        Map<String, Object> model = fixture(state);
        String payment = render("payment", model);
        assertThat(payment).contains("action=\"/payment/REAL-42/mock\"", "method=\"post\"", "42.000 đ", "C7");
        assertThat(payment).containsPattern(state.equals("PENDING")
                ? "<button[^>]*data-payment-submit(?![^>]*disabled)[^>]*>"
                : "<button[^>]*data-payment-submit[^>]*disabled=\"disabled\"");
        String detail = render("detail", model);
        assertThat(detail).contains("REAL-42", "TICKET-91", "42.000 đ", "C7")
                .doesNotContain("BK0000002", "TK0000003", "09:13 19/10/2026");
        assertThat(detail.contains("Thanh toán ngay")).isEqualTo(state.equals("PENDING"));
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
        model.put("ticketType", Map.of("id", 8, "name", "Vé thường", "price", new java.math.BigDecimal("22500")));
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
                default -> throw new IllegalArgumentException("Unknown fixture view");
            };
            return new ModelAndView(view, (Map<String, Object>) request.getAttribute("fixture"));
        }
    }
}
