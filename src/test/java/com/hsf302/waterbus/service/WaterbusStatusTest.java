package com.hsf302.waterbus.service;

import com.hsf302.waterbus.dto.AccountDTO;
import com.hsf302.waterbus.dto.AdminWaterbusForm;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
class WaterbusStatusTest {
    @Autowired private JdbcTemplate jdbc;
    @Autowired private EntityManager entityManager;
    @Autowired private WaterbusService waterbusService;
    @Autowired private TripService tripService;
    @Autowired private WebApplicationContext context;

    @BeforeEach
    void setUp() {
        jdbc.update("INSERT INTO route (code, name, is_active) VALUES ('WSTATUS_R', 'Tuyến thử', 1)");
        jdbc.update("INSERT INTO waterbus (code, name, capacity, status) VALUES ('WSTATUS_W', 'Tàu thử', 3, 'ACTIVE')");
        LocalDateTime departure = LocalDateTime.now().plusDays(1);
        jdbc.update("INSERT INTO trip (trip_code, route_id, waterbus_id, departure_time, arrival_time, status) "
                        + "VALUES ('WSTATUS_T', ?, ?, ?, ?, 'SCHEDULED')",
                id("route", "code", "WSTATUS_R"), id("waterbus", "code", "WSTATUS_W"),
                departure, departure.plusMinutes(30));
    }

    @Test
    void statusChangeRequiresCancellingUpcomingTrip() throws Exception {
        int busId = id("waterbus", "code", "WSTATUS_W");
        int tripId = id("trip", "trip_code", "WSTATUS_T");
        int routeId = id("route", "code", "WSTATUS_R");
        assertEquals(1, waterbusService.countUpcomingScheduledTrips(busId));
        assertEquals(1, tripService.countUpcomingTripsOfRoute(routeId));
        var mvc = MockMvcBuilders.webAppContextSetup(context).build();
        AccountDTO admin = new AccountDTO(1, "admin@test.vn", "Admin", null, "ADMIN", "ACTIVE");
        String html = mvc.perform(get("/admin/waterbuses").sessionAttr("loginAccount", admin))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertTrue(html.contains("chuyến sắp chạy"));
        assertTrue(html.contains("/admin/trips"));
        assertFalse(html.contains("/admin/waterbuses/" + busId + "/edit"));
        mvc.perform(get("/admin/waterbuses/" + busId + "/edit")
                        .sessionAttr("loginAccount", admin))
                .andExpect(status().isNotFound());

        mvc.perform(post("/admin/waterbuses/" + busId + "/status")
                        .param("status", "MAINTENANCE").sessionAttr("loginAccount", admin))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("errorMessage",
                        "Còn 1 chuyến sắp chạy, hãy huỷ chuyến trước khi đổi trạng thái tàu."));
        entityManager.flush();
        assertEquals("ACTIVE", jdbc.queryForObject("SELECT status FROM waterbus WHERE id = ?", String.class, busId));
        tripService.cancelTrip(tripId, "Tàu bảo trì");
        entityManager.flush();
        assertEquals("CANCELLED", jdbc.queryForObject("SELECT status FROM trip WHERE id = ?", String.class, tripId));
        assertEquals(0, tripService.countUpcomingTripsOfRoute(routeId));
        mvc.perform(post("/admin/waterbuses/" + busId + "/status")
                        .param("status", "MAINTENANCE").sessionAttr("loginAccount", admin))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("successMessage", "Đã cập nhật trạng thái tàu."));
        entityManager.flush();
        assertEquals("MAINTENANCE", jdbc.queryForObject("SELECT status FROM waterbus WHERE id = ?", String.class, busId));
    }

    @Test
    void routeCountExcludesPastCancelledAndOtherRoutes() {
        int routeId = id("route", "code", "WSTATUS_R");
        int busId = id("waterbus", "code", "WSTATUS_W");
        LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
        LocalDateTime tomorrow = LocalDateTime.now().plusDays(1);
        jdbc.update("INSERT INTO trip (trip_code, route_id, waterbus_id, departure_time, arrival_time, status) "
                        + "VALUES ('WSTATUS_PAST', ?, ?, ?, ?, 'SCHEDULED')",
                routeId, busId, yesterday, yesterday.plusMinutes(30));
        jdbc.update("INSERT INTO trip (trip_code, route_id, waterbus_id, departure_time, arrival_time, status) "
                        + "VALUES ('WSTATUS_CANCEL', ?, ?, ?, ?, 'CANCELLED')",
                routeId, busId, tomorrow, tomorrow.plusMinutes(30));
        jdbc.update("INSERT INTO route (code, name, is_active) VALUES ('WSTATUS_OTHER', 'Tuyến khác', 1)");
        jdbc.update("INSERT INTO trip (trip_code, route_id, waterbus_id, departure_time, arrival_time, status) "
                        + "VALUES ('WSTATUS_OTHER_T', ?, ?, ?, ?, 'SCHEDULED')",
                id("route", "code", "WSTATUS_OTHER"), busId, tomorrow, tomorrow.plusMinutes(30));

        assertEquals(1, tripService.countUpcomingTripsOfRoute(routeId));
    }

    @Test
    void newWaterbusCreatesSeatsAndExistingWaterbusCannotBeEdited() {
        AdminWaterbusForm form = new AdminWaterbusForm();
        form.setCode("WSTATUS_NEW");
        form.setName("Tàu mới");
        form.setCapacity(7);
        form.setStatus("ACTIVE");
        waterbusService.saveWaterbus(form);
        entityManager.flush();
        int busId = id("waterbus", "code", "WSTATUS_NEW");
        assertEquals(7, jdbc.queryForObject("SELECT COUNT(*) FROM seat WHERE waterbus_id = ?", Integer.class, busId));

        form.setId(busId);
        form.setCapacity(8);
        assertThrows(IllegalArgumentException.class, () -> waterbusService.saveWaterbus(form));
        assertEquals(7, jdbc.queryForObject("SELECT capacity FROM waterbus WHERE id = ?", Integer.class, busId));
    }

    private int id(String table, String column, String value) {
        return jdbc.queryForObject("SELECT id FROM " + table + " WHERE " + column + " = ?", Integer.class, value);
    }
}
