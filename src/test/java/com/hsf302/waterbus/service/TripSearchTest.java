package com.hsf302.waterbus.service;

import com.hsf302.waterbus.controller.TripController;
import com.hsf302.waterbus.dto.TripSearchDTO;
import com.hsf302.waterbus.entity.Trip;
import com.hsf302.waterbus.job.TripStatusJob;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    @Autowired private TripStatusJob tripStatusJob;
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
        trip("TEST_SEARCH_DONE", routeId, busId, date.atTime(9, 0), "COMPLETED");
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
    void hidesDepartedTripsTodayAndRejectsDirectSeatPage() {
        int routeId = id("route", "code", "TEST_SEARCH_R1");
        int busId = id("waterbus", "code", "TEST_SEARCH_W1");
        LocalDateTime now = LocalDateTime.now();
        trip("TEST_SEARCH_PAST", routeId, busId, now.minusMinutes(5), "SCHEDULED");
        trip("TEST_SEARCH_FUTURE", routeId, busId, now.plusHours(1), "SCHEDULED");

        assertEquals(List.of("TEST_SEARCH_FUTURE"),
                tripService.searchTrips(firstStationId, lastStationId, LocalDate.now()).stream()
                        .map(Trip::getTripCode).toList());
        assertNull(tripService.getBookableTrip(id("trip", "trip_code", "TEST_SEARCH_PAST")));
        assertTrue(tripService.getBookableTrip(id("trip", "trip_code", "TEST_SEARCH_FUTURE")) != null);
    }

    @Test
    void updatesTripStatusAtDepartureAndArrivalWithoutChangingCancelledTrips() {
        int routeId = id("route", "code", "TEST_SEARCH_R1");
        int busId = id("waterbus", "code", "TEST_SEARCH_W1");
        LocalDateTime now = LocalDateTime.now();
        trip("TI1_FUTURE", routeId, busId, now.plusMinutes(20), "SCHEDULED");
        trip("TI1_RUNNING", routeId, busId, now.minusMinutes(5), "SCHEDULED");
        trip("TI1_ARRIVED", routeId, busId, now.minusMinutes(40), "SCHEDULED");
        trip("TI1_OLD_DEPARTED", routeId, busId, now.minusMinutes(40), "DEPARTED");
        trip("TI1_CANCELLED", routeId, busId, now.minusMinutes(40), "CANCELLED");

        tripStatusJob.updateTripStatuses();

        assertEquals("SCHEDULED", tripStatus("TI1_FUTURE"));
        assertEquals("DEPARTED", tripStatus("TI1_RUNNING"));
        assertEquals("COMPLETED", tripStatus("TI1_ARRIVED"));
        assertEquals("COMPLETED", tripStatus("TI1_OLD_DEPARTED"));
        assertEquals("CANCELLED", tripStatus("TI1_CANCELLED"));
        assertTrue(tripService.getTodayTrips().stream()
                .anyMatch(trip -> "TI1_RUNNING".equals(trip.tripCode())));
        assertFalse(tripService.getTodayTrips().stream()
                .anyMatch(trip -> "TI1_ARRIVED".equals(trip.tripCode())));
    }

    @Test
    void sellsFutureIntermediateStopOnDepartedTrip() {
        int routeId = id("route", "code", "TEST_SEARCH_R1");
        int busId = id("waterbus", "code", "TEST_SEARCH_W1");
        jdbc.update("UPDATE route_station SET minutes_from_start = 30 WHERE route_id = ? AND station_id = ?",
                routeId, middleStationId);
        jdbc.update("UPDATE route_station SET minutes_from_start = 60 WHERE route_id = ? AND station_id = ?",
                routeId, lastStationId);
        trip("TI1_BOARDING", routeId, busId, LocalDateTime.now().minusMinutes(5), "DEPARTED");
        int tripId = id("trip", "trip_code", "TI1_BOARDING");
        jdbc.update("UPDATE trip SET arrival_time = ? WHERE id = ?", LocalDateTime.now().plusMinutes(55), tripId);

        assertTrue(tripService.searchTrips(middleStationId, lastStationId, LocalDate.now()).stream()
                .anyMatch(trip -> tripId == trip.getId()));
        assertFalse(tripService.searchTrips(firstStationId, lastStationId, LocalDate.now()).stream()
                .anyMatch(trip -> tripId == trip.getId()));
        assertTrue(tripService.getBookableTrip(tripId, middleStationId, lastStationId) != null);
        assertNull(tripService.getBookableTrip(tripId));
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

    @Test
    void showsSegmentTimesAndSortsByBoardingTime() throws Exception {
        int routeId = id("route", "code", "TEST_SEARCH_R1");
        int busId = id("waterbus", "code", "TEST_SEARCH_W1");
        jdbc.update("UPDATE route_station SET minutes_from_start = 33 WHERE route_id = ? AND station_id = ?",
                routeId, middleStationId);
        jdbc.update("UPDATE route_station SET minutes_from_start = 60 WHERE route_id = ? AND station_id = ?",
                routeId, lastStationId);
        trip("TEST_SEARCH_SEGMENT", routeId, busId, date.atTime(18, 25), "SCHEDULED");

        jdbc.update("INSERT INTO route (code, name, is_active) VALUES ('TEST_SEARCH_R3', 'Short route', 1)");
        int shortRouteId = id("route", "code", "TEST_SEARCH_R3");
        stop(shortRouteId, middleStationId, 1, 0);
        stop(shortRouteId, lastStationId, 2, 20);
        trip("TEST_SEARCH_EARLIER", shortRouteId, busId, date.atTime(18, 50), "SCHEDULED");

        List<TripSearchDTO> results = tripService.searchTripResults(middleStationId, lastStationId, date);
        assertEquals(List.of("TEST_SEARCH_T1", "TEST_SEARCH_T2", "TEST_SEARCH_EARLIER", "TEST_SEARCH_SEGMENT"),
                results.stream().map(TripSearchDTO::getTripCode).toList());
        TripSearchDTO segment = results.get(3);
        assertEquals(date.atTime(18, 58), segment.getDepartureTime());
        assertEquals(date.atTime(19, 25), segment.getArrivalTime());

        int tripId = id("trip", "trip_code", "TEST_SEARCH_SEGMENT");
        var seatTrip = tripService.getBookableTrip(tripId, middleStationId, lastStationId);
        assertEquals(segment.getDepartureTime(), seatTrip.getDepartureTime());
        assertEquals(segment.getArrivalTime(), seatTrip.getArrivalTime());
        MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).build();
        String seatsPage = mvc.perform(get("/trips/" + tripId + "/seats")
                        .param("fromStationId", String.valueOf(middleStationId))
                        .param("toStationId", String.valueOf(lastStationId)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertTrue(seatsPage.contains("18:58"));
        assertTrue(seatsPage.contains("19:25"));
    }

    @Test
    void usesBoardingDateAcrossMidnightAndKeepsFutureIntermediateStop() {
        int routeId = id("route", "code", "TEST_SEARCH_R1");
        int busId = id("waterbus", "code", "TEST_SEARCH_W1");
        jdbc.update("UPDATE route_station SET minutes_from_start = 33 WHERE route_id = ? AND station_id = ?",
                routeId, middleStationId);
        jdbc.update("UPDATE route_station SET minutes_from_start = 60 WHERE route_id = ? AND station_id = ?",
                routeId, lastStationId);
        trip("TEST_SEARCH_MIDNIGHT", routeId, busId, date.minusDays(1).atTime(23, 40), "SCHEDULED");

        List<TripSearchDTO> results = tripService.searchTripResults(middleStationId, lastStationId, date);
        TripSearchDTO midnight = results.stream()
                .filter(result -> "TEST_SEARCH_MIDNIGHT".equals(result.getTripCode())).findFirst().orElseThrow();
        assertEquals(date.atTime(0, 13), midnight.getDepartureTime());
        assertEquals(date.atTime(0, 40), midnight.getArrivalTime());

        LocalDateTime routeDeparture = LocalDateTime.now().minusMinutes(5);
        trip("TEST_BOARD_FUTURE", routeId, busId, routeDeparture, "SCHEDULED");
        LocalDate boardingDate = routeDeparture.plusMinutes(33).toLocalDate();
        assertTrue(tripService.searchTripResults(middleStationId, lastStationId, boardingDate).stream()
                .anyMatch(result -> "TEST_BOARD_FUTURE".equals(result.getTripCode())));
        assertTrue(tripService.getBookableTrip(id("trip", "trip_code", "TEST_BOARD_FUTURE"),
                middleStationId, lastStationId) != null);
    }

    @Test
    void stopsOnlineSalesTenMinutesBeforeBoardingAtSelectedStop() throws Exception {
        int routeId = id("route", "code", "TEST_SEARCH_R1");
        int busId = id("waterbus", "code", "TEST_SEARCH_W1");
        jdbc.update("UPDATE route_station SET minutes_from_start = 20 WHERE route_id = ? AND station_id = ?",
                routeId, middleStationId);
        jdbc.update("UPDATE route_station SET minutes_from_start = 40 WHERE route_id = ? AND station_id = ?",
                routeId, lastStationId);

        LocalDateTime now = LocalDateTime.now();
        trip("TEST_CUTOFF_FIRST", routeId, busId, now.plusMinutes(9), "SCHEDULED");
        trip("TEST_OPEN_FIRST", routeId, busId, now.plusMinutes(12), "SCHEDULED");
        trip("TEST_CUTOFF_MID", routeId, busId, now.minusMinutes(11), "SCHEDULED");
        trip("TEST_OPEN_MID", routeId, busId, now.minusMinutes(8), "SCHEDULED");

        List<String> firstStopCodes = tripService.searchTrips(firstStationId, lastStationId, now.toLocalDate())
                .stream().map(Trip::getTripCode).toList();
        assertFalse(firstStopCodes.contains("TEST_CUTOFF_FIRST"));
        assertTrue(firstStopCodes.contains("TEST_OPEN_FIRST"));
        assertNull(tripService.getBookableTrip(id("trip", "trip_code", "TEST_CUTOFF_FIRST")));
        assertTrue(tripService.getBookableTrip(id("trip", "trip_code", "TEST_OPEN_FIRST")) != null);

        LocalDate boardingDate = now.minusMinutes(8).plusMinutes(20).toLocalDate();
        List<String> middleStopCodes = tripService.searchTrips(middleStationId, lastStationId, boardingDate)
                .stream().map(Trip::getTripCode).toList();
        assertFalse(middleStopCodes.contains("TEST_CUTOFF_MID"));
        assertTrue(middleStopCodes.contains("TEST_OPEN_MID"));
        assertNull(tripService.getBookableTrip(id("trip", "trip_code", "TEST_CUTOFF_MID"),
                middleStationId, lastStationId));
        assertTrue(tripService.getBookableTrip(id("trip", "trip_code", "TEST_OPEN_MID"),
                middleStationId, lastStationId) != null);

        MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).build();
        mvc.perform(get("/trips/" + id("trip", "trip_code", "TEST_CUTOFF_MID") + "/seats")
                        .param("fromStationId", String.valueOf(middleStationId))
                        .param("toStationId", String.valueOf(lastStationId)))
                .andExpect(status().is3xxRedirection());
        mvc.perform(get("/trips/" + id("trip", "trip_code", "TEST_OPEN_MID") + "/seats")
                        .param("fromStationId", String.valueOf(middleStationId))
                        .param("toStationId", String.valueOf(lastStationId)))
                .andExpect(status().isOk());
    }

    private void stop(int routeId, int stationId, int order) {
        stop(routeId, stationId, order, 0);
    }

    private void stop(int routeId, int stationId, int order, int minutes) {
        jdbc.update("INSERT INTO route_station (route_id, station_id, stop_order, minutes_from_start) VALUES (?, ?, ?, ?)",
                routeId, stationId, order, minutes);
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

    private String tripStatus(String code) {
        return jdbc.queryForObject("SELECT status FROM trip WHERE trip_code = ?", String.class, code);
    }
}
