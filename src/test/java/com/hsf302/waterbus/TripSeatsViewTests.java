package com.hsf302.waterbus;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.jpa.open-in-view=false")
@Transactional
class TripSeatsViewTests {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JdbcTemplate jdbc;

    private MockMvc mvc;
    private int tripId;
    private int cancelledTripId;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();

        jdbc.update("INSERT INTO route (code, name, is_active) VALUES ('TEST_R1', 'Route 1', 1)");
        int routeId = jdbc.queryForObject("SELECT id FROM route WHERE code = 'TEST_R1'", Integer.class);
        jdbc.update("INSERT INTO waterbus (code, name, capacity, status) VALUES ('TEST_WB1', 'Bus 1', 2, 'ACTIVE')");
        int busId = jdbc.queryForObject("SELECT id FROM waterbus WHERE code = 'TEST_WB1'", Integer.class);
        jdbc.update("INSERT INTO seat (waterbus_id, seat_code, seat_row, seat_col, is_active) VALUES (?, 'A1', 1, 1, 1)", busId);
        jdbc.update("INSERT INTO seat (waterbus_id, seat_code, seat_row, seat_col, is_active) VALUES (?, 'A2', 1, 2, 1)", busId);
        jdbc.update("INSERT INTO ticket_type (code, name, price, requires_verification, is_active) "
                + "VALUES ('TEST_STANDARD', 'Standard', 15000, 0, 1)");

        LocalDateTime departure = LocalDateTime.now().plusDays(1);
        jdbc.update("INSERT INTO trip (trip_code, route_id, waterbus_id, departure_time, arrival_time, status) "
                + "VALUES ('TEST_TRIP1', ?, ?, ?, ?, 'SCHEDULED')", routeId, busId, departure, departure.plusMinutes(30));
        jdbc.update("INSERT INTO trip (trip_code, route_id, waterbus_id, departure_time, arrival_time, status) "
                + "VALUES ('TEST_TRIP2', ?, ?, ?, ?, 'CANCELLED')", routeId, busId, departure, departure.plusMinutes(30));
        tripId = jdbc.queryForObject("SELECT id FROM trip WHERE trip_code = 'TEST_TRIP1'", Integer.class);
        cancelledTripId = jdbc.queryForObject("SELECT id FROM trip WHERE trip_code = 'TEST_TRIP2'", Integer.class);
    }

    @Test
    void seatPageRendersWithSeatsAndTicketTypes() throws Exception {
        mvc.perform(get("/trips/" + tripId + "/seats"))
                .andExpect(status().isOk())
                .andExpect(view().name("trip/seats"))
                .andExpect(model().attributeExists("trip", "seats", "ticketTypes"));
    }

    @Test
    void unknownOrNotScheduledTripRedirectsToSearch() throws Exception {
        mvc.perform(get("/trips/999999/seats"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/trips"));
        mvc.perform(get("/trips/" + cancelledTripId + "/seats"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/trips"));
    }
}
