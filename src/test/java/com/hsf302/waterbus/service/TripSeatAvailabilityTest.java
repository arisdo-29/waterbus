package com.hsf302.waterbus.service;

import com.hsf302.waterbus.entity.Seat;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class TripSeatAvailabilityTest {
    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private TripService tripService;

    private int tripId;
    private int otherTripId;
    private int holdSeatId;
    private int validSeatId;
    private int usedSeatId;
    private int cancelledSeatId;
    private int freeSeatId;
    private int inactiveSeatId;
    private int otherBusSeatId;

    @BeforeEach
    void setUp() {
        jdbc.update("INSERT INTO route (code, name, is_active) VALUES ('TEST_R1', 'Route 1', 1)");
        int routeId = id("route", "code", "TEST_R1");
        jdbc.update("INSERT INTO waterbus (code, name, capacity, status) VALUES ('TEST_WB1', 'Bus 1', 6, 'ACTIVE')");
        jdbc.update("INSERT INTO waterbus (code, name, capacity, status) VALUES ('TEST_WB2', 'Bus 2', 1, 'ACTIVE')");
        int busId = id("waterbus", "code", "TEST_WB1");
        int otherBusId = id("waterbus", "code", "TEST_WB2");
        LocalDateTime departure = LocalDateTime.now().plusDays(1);
        jdbc.update("INSERT INTO trip (trip_code, route_id, waterbus_id, departure_time, arrival_time, status) "
                        + "VALUES (?, ?, ?, ?, ?, 'SCHEDULED')",
                "TEST_TRIP1", routeId, busId, departure, departure.plusMinutes(30));
        jdbc.update("INSERT INTO trip (trip_code, route_id, waterbus_id, departure_time, arrival_time, status) "
                        + "VALUES (?, ?, ?, ?, ?, 'SCHEDULED')",
                "TEST_TRIP2", routeId, busId, departure.plusHours(2), departure.plusHours(2).plusMinutes(30));
        tripId = id("trip", "trip_code", "TEST_TRIP1");
        otherTripId = id("trip", "trip_code", "TEST_TRIP2");

        holdSeatId = seat(busId, "A1", 1, 1, true);
        validSeatId = seat(busId, "A2", 1, 2, true);
        usedSeatId = seat(busId, "A3", 1, 3, true);
        cancelledSeatId = seat(busId, "B1", 2, 1, true);
        freeSeatId = seat(busId, "A4", 1, 4, true);
        inactiveSeatId = seat(busId, "B2", 2, 2, false);
        otherBusSeatId = seat(otherBusId, "A1", 1, 1, true);

        jdbc.update("INSERT INTO station (code, name, is_active) VALUES ('TEST_S1', 'Station 1', 1)");
        jdbc.update("INSERT INTO station (code, name, is_active) VALUES ('TEST_S2', 'Station 2', 1)");
        jdbc.update("INSERT INTO ticket_type (code, name, price, requires_verification, is_active) "
                + "VALUES ('TEST_STANDARD', 'Standard', 15000, 0, 1)");
        jdbc.update("INSERT INTO booking (booking_code, contact_name, contact_phone, contact_email, "
                        + "subtotal, discount_amount, total_amount, status, created_at, expires_at) "
                        + "VALUES (?, 'Test', '0900000000', 'test@example.com', ?, 0, ?, 'PAID', ?, ?)",
                "TEST_BOOK1", BigDecimal.valueOf(60000), BigDecimal.valueOf(60000),
                departure.minusDays(1), departure.minusDays(1).plusMinutes(10));

        ticket("TEST_T1", holdSeatId, "HOLD");
        ticket("TEST_T2", validSeatId, "VALID");
        ticket("TEST_T3", usedSeatId, "USED");
        ticket("TEST_T4", cancelledSeatId, "CANCELLED");
    }

    @Test
    void excludesOccupiedInactiveAndOtherBusSeatsButReleasesCancelledSeats() {
        List<Seat> available = tripService.getAvailableSeats(tripId);

        assertEquals(List.of(freeSeatId, cancelledSeatId), available.stream().map(Seat::getId).toList());
        assertFalse(tripService.isSeatAvailable(tripId, holdSeatId));
        assertFalse(tripService.isSeatAvailable(tripId, validSeatId));
        assertFalse(tripService.isSeatAvailable(tripId, usedSeatId));
        assertTrue(tripService.isSeatAvailable(tripId, cancelledSeatId));
        assertTrue(tripService.isSeatAvailable(tripId, freeSeatId));
        assertFalse(tripService.isSeatAvailable(tripId, inactiveSeatId));
        assertFalse(tripService.isSeatAvailable(tripId, otherBusSeatId));
    }

    @Test
    void checksTicketsPerTripAndHandlesUnknownIds() {
        assertTrue(tripService.isSeatAvailable(otherTripId, holdSeatId));
        assertEquals(5, tripService.getAvailableSeats(otherTripId).size());
        assertTrue(tripService.getAvailableSeats(-1).isEmpty());
        assertFalse(tripService.isSeatAvailable(-1, freeSeatId));
        assertFalse(tripService.isSeatAvailable(tripId, -1));
    }

    private int seat(int busId, String code, int row, int col, boolean active) {
        jdbc.update("INSERT INTO seat (waterbus_id, seat_code, seat_row, seat_col, is_active) "
                + "VALUES (?, ?, ?, ?, ?)", busId, code, row, col, active);
        return jdbc.queryForObject("SELECT id FROM seat WHERE waterbus_id = ? AND seat_code = ?",
                Integer.class, busId, code);
    }

    private void ticket(String code, int seatId, String status) {
        jdbc.update("INSERT INTO ticket (ticket_code, booking_id, trip_id, seat_id, ticket_type_id, "
                        + "from_station_id, to_station_id, price, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                code, id("booking", "booking_code", "TEST_BOOK1"), tripId, seatId,
                id("ticket_type", "code", "TEST_STANDARD"), id("station", "code", "TEST_S1"),
                id("station", "code", "TEST_S2"), BigDecimal.valueOf(15000), status);
    }

    private int id(String table, String codeColumn, String code) {
        return jdbc.queryForObject("SELECT id FROM " + table + " WHERE " + codeColumn + " = ?",
                Integer.class, code);
    }
}
