package com.hsf302.waterbus.service;

import com.hsf302.waterbus.dto.AccountDTO;
import com.hsf302.waterbus.dto.AdminTripForm;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@Transactional
class TripScheduleConflictTest {
    @Autowired private JdbcTemplate jdbc;
    @Autowired private TripService tripService;
    @Autowired private WebApplicationContext context;

    private int routeId;
    private int busId;
    private int otherBusId;
    private LocalDate date;

    @BeforeEach
    void setUp() {
        date = LocalDate.now().plusDays(2);
        jdbc.update("INSERT INTO route (code, name, is_active) VALUES ('TEST_OVERLAP_R', 'Test route', 1)");
        routeId = id("route", "TEST_OVERLAP_R");
        jdbc.update("INSERT INTO station (code, name, is_active) VALUES ('TEST_OVERLAP_S1', 'Start', 1)");
        jdbc.update("INSERT INTO station (code, name, is_active) VALUES ('TEST_OVERLAP_S2', 'End', 1)");
        jdbc.update("INSERT INTO route_station (route_id, station_id, stop_order, minutes_from_start) VALUES (?, ?, 1, 0)",
                routeId, id("station", "TEST_OVERLAP_S1"));
        jdbc.update("INSERT INTO route_station (route_id, station_id, stop_order, minutes_from_start) VALUES (?, ?, 2, 50)",
                routeId, id("station", "TEST_OVERLAP_S2"));
        jdbc.update("INSERT INTO waterbus (code, name, capacity, status) VALUES ('TEST_OVERLAP_W1', 'Bus 1', 2, 'ACTIVE')");
        jdbc.update("INSERT INTO waterbus (code, name, capacity, status) VALUES ('TEST_OVERLAP_W2', 'Bus 2', 2, 'ACTIVE')");
        busId = id("waterbus", "TEST_OVERLAP_W1");
        otherBusId = id("waterbus", "TEST_OVERLAP_W2");
        existingTrip("TEST_OVERLAP_T1", busId, date.atTime(10, 0), date.atTime(11, 0), "SCHEDULED");
        existingTrip("TEST_OVERLAP_T2", busId, date.atTime(12, 0), date.atTime(13, 0), "CANCELLED");
        existingTrip("TEST_OVERLAP_T3", busId, date.atTime(14, 0), date.atTime(15, 0), "DEPARTED");
    }

    @Test
    void rejectsAnyOverlapWithScheduledTripOnSameBus() {
        for (LocalDateTime departure : new LocalDateTime[]{
                date.atTime(9, 30), date.atTime(10, 5), date.atTime(10, 30)}) {
            IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                    () -> tripService.createAdminTrip(form(busId, departure)));
            assertTrue(error.getMessage().contains("trùng giờ"));
        }
        IllegalArgumentException departedConflict = assertThrows(IllegalArgumentException.class,
                () -> tripService.createAdminTrip(form(busId, date.atTime(14, 10))));
        assertTrue(departedConflict.getMessage().contains("trùng giờ"));
        assertEquals(3, jdbc.queryForObject("SELECT COUNT(*) FROM trip WHERE route_id = ?", Integer.class, routeId));
    }

    @Test
    void allowsBoundaryOtherBusAndCancelledTrip() {
        tripService.createAdminTrip(form(busId, date.atTime(9, 10)));
        tripService.createAdminTrip(form(busId, date.atTime(11, 0)));
        tripService.createAdminTrip(form(otherBusId, date.atTime(10, 15)));
        tripService.createAdminTrip(form(busId, date.atTime(12, 10)));
        assertEquals(7, jdbc.queryForObject("SELECT COUNT(*) FROM trip WHERE route_id = ?", Integer.class, routeId));
    }

    @Test
    void adminFormShowsConflictInsteadOfSaving() throws Exception {
        MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).build();
        AccountDTO admin = new AccountDTO(1, "admin@test.vn", "Admin", null, "ADMIN", "ACTIVE");
        mvc.perform(post("/admin/trips/save")
                        .sessionAttr("loginAccount", admin)
                        .param("routeId", String.valueOf(routeId))
                        .param("waterbusId", String.valueOf(busId))
                        .param("departureTime", date.atTime(10, 30).toString())
                        .param("status", "SCHEDULED"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/trip/form"))
                .andExpect(model().attributeExists("errorMessage"));
        assertEquals(3, jdbc.queryForObject("SELECT COUNT(*) FROM trip WHERE route_id = ?", Integer.class, routeId));
    }

    private AdminTripForm form(int waterbusId, LocalDateTime departure) {
        AdminTripForm form = new AdminTripForm();
        form.setRouteId(routeId);
        form.setWaterbusId(waterbusId);
        form.setDepartureTime(departure);
        form.setStatus("SCHEDULED");
        return form;
    }

    private void existingTrip(String code, int waterbusId, LocalDateTime departure,
                              LocalDateTime arrival, String status) {
        jdbc.update("INSERT INTO trip (trip_code, route_id, waterbus_id, departure_time, arrival_time, status) "
                        + "VALUES (?, ?, ?, ?, ?, ?)", code, routeId, waterbusId, departure, arrival, status);
    }

    private int id(String table, String code) {
        return jdbc.queryForObject("SELECT id FROM " + table + " WHERE code = ?", Integer.class, code);
    }
}
