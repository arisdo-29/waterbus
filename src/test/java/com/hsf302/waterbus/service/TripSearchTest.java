package com.hsf302.waterbus.service;

import com.hsf302.waterbus.controller.TripController;
import com.hsf302.waterbus.dto.TripSearchDTO;
import com.hsf302.waterbus.entity.Trip;
import java.time.LocalDate;
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
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@Transactional
class TripSearchTest {
    @Autowired private JdbcTemplate jdbc;
    @Autowired private TripService tripService;
    @Autowired private TripController tripController;
    @Autowired private WebApplicationContext context;

    private int firstStationId;
    private int middleStationId;
    private int lastStationId;
    private LocalDate date;

    @BeforeEach
    void setUp() {
        date = LocalDate.now().plusDays(2);
        jdbc.update("INSERT INTO station (code, name, is_active) VALUES ('TEST_SEARCH_S1', 'Start', 1)");
        jdbc.update("INSERT INTO station (code, name, is_active) VALUES ('TEST_SEARCH_S2', 'Middle', 1)");
        jdbc.update("INSERT INTO station (code, name, is_active) VALUES ('TEST_SEARCH_S3', 'End', 1)");
        firstStationId = id("station", "code", "TEST_SEARCH_S1");
        middleStationId = id("station", "code", "TEST_SEARCH_S2");
        lastStationId = id("station", "code", "TEST_SEARCH_S3");

        jdbc.update("INSERT INTO route (code, name, is_active) VALUES ('TEST_SEARCH_R1', 'Test route', 1)");
        jdbc.update("INSERT INTO route (code, name, is_active) VALUES ('TEST_SEARCH_R2', 'Closed route', 0)");
        int routeId = id("route", "code", "TEST_SEARCH_R1");
        int closedRouteId = id("route", "code", "TEST_SEARCH_R2");
        stop(routeId, firstStationId, 1);
        stop(routeId, middleStationId, 2);
        stop(routeId, lastStationId, 3);
        stop(closedRouteId, firstStationId, 1);
        stop(closedRouteId, lastStationId, 2);

        jdbc.update("INSERT INTO waterbus (code, name, capacity, status) VALUES ('TEST_SEARCH_W1', 'Bus', 3, 'ACTIVE')");
        int busId = id("waterbus", "code", "TEST_SEARCH_W1");
        jdbc.update("INSERT INTO seat (waterbus_id, seat_code, seat_row, seat_col, is_active) VALUES (?, 'A1', 1, 1, 1)", busId);
        jdbc.update("INSERT INTO seat (waterbus_id, seat_code, seat_row, seat_col, is_active) VALUES (?, 'A2', 1, 2, 1)", busId);
        jdbc.update("INSERT INTO seat (waterbus_id, seat_code, seat_row, seat_col, is_active) VALUES (?, 'A3', 1, 3, 0)", busId);

        trip("TEST_SEARCH_T1", routeId, busId, date.atTime(8, 0), "SCHEDULED");
        trip("TEST_SEARCH_T2", routeId, busId, date.atTime(12, 0), "SCHEDULED");
        trip("TEST_SEARCH_OLD", routeId, busId, date.minusDays(1).atTime(8, 0), "SCHEDULED");
        trip("TEST_SEARCH_NEXT", routeId, busId, date.plusDays(1).atTime(8, 0), "SCHEDULED");
        trip("TEST_SEARCH_DONE", routeId, busId, date.atTime(9, 0), "DEPARTED");
        trip("TEST_SEARCH_CLOSED", closedRouteId, busId, date.atTime(10, 0), "SCHEDULED");
    }

    @Test
    void filtersByStopOrderDateAndOpenTrips() {
        List<Trip> trips = tripService.searchTrips(firstStationId, lastStationId, date);
        assertEquals(List.of("TEST_SEARCH_T1", "TEST_SEARCH_T2"),
                trips.stream().map(Trip::getTripCode).toList());
        assertEquals(List.of("TEST_SEARCH_T1", "TEST_SEARCH_T2"),
                tripService.searchTrips(middleStationId, lastStationId, date).stream()
                        .map(Trip::getTripCode).toList());
        assertEquals(List.of(), tripService.searchTrips(lastStationId, firstStationId, date));
        assertEquals(List.of(), tripService.searchTrips(firstStationId, firstStationId, date));
    }

    @Test
    void suppliesResultPageModelWithActualSeatCount() {
        ExtendedModelMap model = new ExtendedModelMap();
        assertEquals("trip/index", tripController.index(null, null, null, model));
        assertNull(model.get("trips"));

        model = new ExtendedModelMap();
        assertEquals("trip/index", tripController.index(firstStationId, lastStationId, date, model));
        @SuppressWarnings("unchecked")
        List<TripSearchDTO> results = (List<TripSearchDTO>) model.get("trips");
        assertEquals(2, results.size());
        assertEquals("Test route", results.get(0).getRouteName());
        assertEquals(2, results.get(0).getAvailableSeats());
        assertEquals(firstStationId, model.get("fromStationId"));
        assertEquals(lastStationId, model.get("toStationId"));
        assertEquals(date, model.get("date"));
    }

    @Test
    void rendersSearchPageAndResults() throws Exception {
        MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).build();
        String initialPage = mvc.perform(get("/trips"))
                .andExpect(status().isOk()).andExpect(view().name("trip/index"))
                .andReturn().getResponse().getContentAsString();
        assertTrue(initialPage.contains("fromStationId"));

        String resultsPage = mvc.perform(get("/trips")
                        .param("fromStationId", String.valueOf(firstStationId))
                        .param("toStationId", String.valueOf(lastStationId))
                        .param("date", date.toString()))
                .andExpect(status().isOk()).andExpect(view().name("trip/index"))
                .andReturn().getResponse().getContentAsString();
        assertTrue(resultsPage.contains("TEST_SEARCH_T1"));
        assertTrue(resultsPage.contains("TEST_SEARCH_T2"));
        assertTrue(resultsPage.contains("Còn 2 ghế trống"));
    }

    private void stop(int routeId, int stationId, int order) {
        jdbc.update("INSERT INTO route_station (route_id, station_id, stop_order, minutes_from_start) VALUES (?, ?, ?, 0)",
                routeId, stationId, order);
    }

    private void trip(String code, int routeId, int busId, LocalDateTime departure, String status) {
        jdbc.update("INSERT INTO trip (trip_code, route_id, waterbus_id, departure_time, arrival_time, status) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                code, routeId, busId, departure, departure.plusMinutes(30), status);
    }

    private int id(String table, String codeColumn, String code) {
        return jdbc.queryForObject("SELECT id FROM " + table + " WHERE " + codeColumn + " = ?",
                Integer.class, code);
    }
}
