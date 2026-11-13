package com.hsf302.waterbus.service;

import com.hsf302.waterbus.dto.AccountDTO;
import com.hsf302.waterbus.dto.AdminWaterbusForm;
import com.hsf302.waterbus.dto.AdminTripForm;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    void statusChangePreviewsAndCancelsTripsThenReturnsAfterMaintenance() throws Exception {
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
        assertTrue(html.contains("Tiếp tục"));
        assertFalse(html.contains("/admin/waterbuses/" + busId + "/edit"));
        mvc.perform(get("/admin/waterbuses/" + busId + "/edit")
                        .sessionAttr("loginAccount", admin))
                .andExpect(status().isNotFound());

        LocalDateTime departure = jdbc.queryForObject("SELECT departure_time FROM trip WHERE id = ?",
                LocalDateTime.class, tripId);
        String startsAt = departure.minusMinutes(5).format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
        String endsAt = departure.plusMinutes(5).format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
        String preview = mvc.perform(get("/admin/waterbuses/" + busId + "/status")
                        .param("status", "MAINTENANCE").param("startsAt", startsAt)
                        .param("endsAt", endsAt).param("preview", "true")
                        .sessionAttr("loginAccount", admin))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertTrue(preview.contains("WSTATUS_T"));
        assertTrue(preview.contains("waterbusStatusConfirm"));
        mvc.perform(post("/admin/waterbuses/" + busId + "/status")
                        .param("status", "MAINTENANCE").param("startsAt", startsAt)
                        .param("endsAt", endsAt).param("tripIds", String.valueOf(tripId))
                        .sessionAttr("loginAccount", admin))
                .andExpect(status().is3xxRedirection());
        entityManager.flush();
        assertEquals("ACTIVE", jdbc.queryForObject("SELECT status FROM waterbus WHERE id = ?", String.class, busId));
        assertEquals("MAINTENANCE", jdbc.queryForObject("SELECT pending_status FROM waterbus WHERE id = ?",
                String.class, busId));
        assertEquals("CANCELLED", jdbc.queryForObject("SELECT status FROM trip WHERE id = ?", String.class, tripId));
        assertEquals(0, tripService.countUpcomingTripsOfRoute(routeId));
        jdbc.update("UPDATE waterbus SET status_starts_at = ?, status_ends_at = ? WHERE id = ?",
                LocalDateTime.now().minusMinutes(2), LocalDateTime.now().plusMinutes(30), busId);
        entityManager.clear();
        waterbusService.applyScheduledStatuses();
        entityManager.flush();
        assertEquals("MAINTENANCE", jdbc.queryForObject("SELECT status FROM waterbus WHERE id = ?", String.class, busId));
        jdbc.update("UPDATE waterbus SET status_ends_at = ? WHERE id = ?",
                LocalDateTime.now().minusMinutes(1), busId);
        entityManager.clear();
        waterbusService.applyScheduledStatuses();
        entityManager.flush();
        assertEquals("ACTIVE", jdbc.queryForObject("SELECT status FROM waterbus WHERE id = ?", String.class, busId));
    }

    @Test
    void indefiniteShutdownCancelsOnlyTripsAfterStart() {
        int busId = id("waterbus", "code", "WSTATUS_W");
        int routeId = id("route", "code", "WSTATUS_R");
        LocalDateTime firstDeparture = jdbc.queryForObject("SELECT departure_time FROM trip WHERE trip_code='WSTATUS_T'",
                LocalDateTime.class);
        LocalDateTime laterDeparture = firstDeparture.plusDays(1);
        jdbc.update("INSERT INTO trip (trip_code, route_id, waterbus_id, departure_time, arrival_time, status) "
                        + "VALUES ('WSTATUS_LATER', ?, ?, ?, ?, 'SCHEDULED')",
                routeId, busId, laterDeparture, laterDeparture.plusMinutes(30));
        int laterTripId = id("trip", "trip_code", "WSTATUS_LATER");
        LocalDateTime startsAt = firstDeparture.plusHours(1);

        var previews = waterbusService.previewStatusChange(busId, "INACTIVE", startsAt, null);
        assertEquals(1, previews.size());
        assertEquals(laterTripId, previews.get(0).trip().id());
        assertEquals(1, waterbusService.scheduleStatusChange(
                busId, "INACTIVE", startsAt, null, java.util.List.of(laterTripId)));
        entityManager.flush();
        assertEquals("SCHEDULED", jdbc.queryForObject("SELECT status FROM trip WHERE trip_code='WSTATUS_T'",
                String.class));
        assertEquals("CANCELLED", jdbc.queryForObject("SELECT status FROM trip WHERE id=?",
                String.class, laterTripId));
        jdbc.update("UPDATE waterbus SET status_starts_at=? WHERE id=?",
                LocalDateTime.now().minusMinutes(1), busId);
        entityManager.clear();
        waterbusService.applyScheduledStatuses();
        entityManager.flush();
        assertEquals("INACTIVE", jdbc.queryForObject("SELECT status FROM waterbus WHERE id=?",
                String.class, busId));
    }

    @Test
    void scheduledMaintenanceBlocksNewTripsInsideWindow() {
        int busId = id("waterbus", "code", "WSTATUS_W");
        int routeId = id("route", "code", "WSTATUS_R");
        LocalDateTime startsAt = LocalDateTime.now().plusDays(2).withSecond(0).withNano(0);
        LocalDateTime endsAt = startsAt.plusHours(2);
        assertEquals(0, waterbusService.scheduleStatusChange(
                busId, "MAINTENANCE", startsAt, endsAt, java.util.List.of()));

        AdminTripForm trip = new AdminTripForm();
        trip.setRouteId(routeId);
        trip.setWaterbusId(busId);
        trip.setDepartureTime(startsAt.plusMinutes(30));
        trip.setStatus("SCHEDULED");
        assertThrows(IllegalArgumentException.class, () -> tripService.createAdminTrip(trip));
    }

    @Test
    void statusPageShowsEndTimeOnlyForMaintenance() throws Exception {
        int busId = id("waterbus", "code", "WSTATUS_W");
        var mvc = MockMvcBuilders.webAppContextSetup(context).build();
        AccountDTO admin = new AccountDTO(1, "admin@test.vn", "Admin", null, "ADMIN", "ACTIVE");
        String inactivePage = mvc.perform(get("/admin/waterbuses/" + busId + "/status")
                        .param("status", "INACTIVE").sessionAttr("loginAccount", admin))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertTrue(inactivePage.contains("id=\"startsAt\""));
        assertFalse(inactivePage.contains("id=\"endsAt\""));

        jdbc.update("UPDATE waterbus SET status='MAINTENANCE' WHERE id=?", busId);
        entityManager.clear();
        String activePage = mvc.perform(get("/admin/waterbuses/" + busId + "/status")
                        .param("status", "ACTIVE").sessionAttr("loginAccount", admin))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertTrue(activePage.contains("Các chuyến đã hủy trước đó không tự khôi phục"));
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
