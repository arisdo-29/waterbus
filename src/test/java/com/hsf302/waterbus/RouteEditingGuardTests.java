package com.hsf302.waterbus;

import com.hsf302.waterbus.dto.AccountDTO;
import com.hsf302.waterbus.dto.RouteForm;
import com.hsf302.waterbus.entity.Route;
import com.hsf302.waterbus.entity.Station;
import com.hsf302.waterbus.entity.Trip;
import com.hsf302.waterbus.entity.Waterbus;
import com.hsf302.waterbus.service.RouteService;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.jpa.open-in-view=false")
@Transactional
class RouteEditingGuardTests {
    @Autowired WebApplicationContext context;
    @Autowired RouteService routeService;
    @Autowired EntityManager entityManager;

    private MockMvc mvc;
    private MockHttpSession session;
    private int routeId;
    private int firstStationId;
    private int secondStationId;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        session = new MockHttpSession();
        session.setAttribute("loginAccount", new AccountDTO(1, "admin@route.test", "Admin", null, "ADMIN", "ACTIVE"));
        firstStationId = station("GUARD_A");
        secondStationId = station("GUARD_B");
        routeId = createRoute("GUARD_ROUTE");
    }

    @ParameterizedTest
    @CsvSource({"SCHEDULED, 30, 90", "SCHEDULED, -15, 45", "DEPARTED, -15, 45"})
    void activeTripsBlockEditPageDirectSaveAndDeactivation(String status, int departure, int arrival) throws Exception {
        trip(routeId, status, departure, arrival);
        mvc.perform(get("/admin/routes/" + routeId + "/edit").session(session))
                .andExpect(redirectedUrl("/admin/routes"))
                .andExpect(flash().attribute("errorMessage", containsString("1 chuyến đang chạy hoặc sắp khởi hành")));
        mvc.perform(saveRequest())
                .andExpect(status().isOk())
                .andExpect(view().name("admin/route/form"))
                .andExpect(model().hasErrors())
                .andExpect(model().attribute("errorMessage", containsString("Chỉ được sửa tuyến")));
        mvc.perform(post("/admin/routes/" + routeId + "/delete").session(session))
                .andExpect(redirectedUrl("/admin/routes"))
                .andExpect(flash().attributeExists("errorMessage"));
        assertOriginalRoute();
    }

    @ParameterizedTest
    @CsvSource({"COMPLETED, -90, -30", "CANCELLED, 30, 90", "DEPARTED, -90, -30", "SCHEDULED, -90, -30"})
    void finishedOrCancelledTripsDoNotBlockEditing(String status, int departure, int arrival) throws Exception {
        trip(routeId, status, departure, arrival);
        assertEditAllowed();
    }

    @Test
    void routeWithoutTripsCanBeEditedEvenWhenAnotherRouteHasAnUpcomingTrip() throws Exception {
        int otherRoute = createRoute("OTHER_ROUTE");
        trip(otherRoute, "SCHEDULED", 30, 90);
        assertEditAllowed();
    }

    @Test
    void tripCreatedAfterOpeningTheFormIsCheckedAgainOnSave() throws Exception {
        mvc.perform(get("/admin/routes/" + routeId + "/edit").session(session))
                .andExpect(status().isOk()).andExpect(view().name("admin/route/form"));
        trip(routeId, "SCHEDULED", 30, 90);
        mvc.perform(saveRequest()).andExpect(view().name("admin/route/form"))
                .andExpect(model().hasErrors()).andExpect(model().attributeExists("errorMessage"));
        assertOriginalRoute();
    }

    private void assertEditAllowed() throws Exception {
        mvc.perform(get("/admin/routes/" + routeId + "/edit").session(session))
                .andExpect(status().isOk()).andExpect(view().name("admin/route/form"));
        mvc.perform(saveRequest()).andExpect(redirectedUrl("/admin/routes"))
                .andExpect(flash().attributeExists("successMessage"));
        entityManager.clear();
        RouteForm stored = routeService.getRouteForEdit(routeId);
        assertThat(stored.getCode()).isEqualTo("GUARD_CHANGED");
        assertThat(stored.getName()).isEqualTo("Tên đã sửa");
        assertThat(stored.getStationIds()).containsExactly(secondStationId, firstStationId);
        assertThat(stored.getMinutesFromStart()).containsExactly(0, 55);
        assertThat(stored.getIsActive()).isFalse();
    }

    private void assertOriginalRoute() {
        entityManager.clear();
        RouteForm stored = routeService.getRouteForEdit(routeId);
        assertThat(stored.getCode()).isEqualTo("GUARD_ROUTE");
        assertThat(stored.getName()).isEqualTo("Tuyến gốc");
        assertThat(stored.getStationIds()).containsExactly(firstStationId, secondStationId);
        assertThat(stored.getMinutesFromStart()).containsExactly(0, 30);
        assertThat(stored.getIsActive()).isTrue();
    }

    private MockHttpServletRequestBuilder saveRequest() {
        return post("/admin/routes/save").session(session).param("id", "" + routeId)
                .param("code", "GUARD_CHANGED").param("name", "Tên đã sửa").param("_isActive", "on")
                .param("stationIds", "" + secondStationId, "" + firstStationId)
                .param("minutesFromStart", "0", "55");
    }

    private int station(String code) {
        Station station = new Station();
        station.setCode(code);
        station.setName(code);
        station.setIsActive(true);
        entityManager.persist(station);
        return station.getId();
    }

    private int createRoute(String code) {
        RouteForm form = new RouteForm();
        form.setCode(code);
        form.setName("Tuyến gốc");
        form.setIsActive(true);
        form.setStationIds(List.of(firstStationId, secondStationId));
        form.setMinutesFromStart(List.of(0, 30));
        return routeService.saveRoute(form);
    }

    private void trip(int routeId, String status, int departureMinutes, int arrivalMinutes) {
        Waterbus waterbus = new Waterbus();
        waterbus.setCode("GUARD_WB");
        waterbus.setName("Tàu kiểm thử");
        waterbus.setCapacity(20);
        waterbus.setStatus("ACTIVE");
        entityManager.persist(waterbus);
        LocalDateTime now = LocalDateTime.now();
        Trip trip = new Trip();
        trip.setTripCode("GUARD_TRIP");
        trip.setRoute(entityManager.getReference(Route.class, routeId));
        trip.setWaterbus(waterbus);
        trip.setDepartureTime(now.plusMinutes(departureMinutes));
        trip.setArrivalTime(now.plusMinutes(arrivalMinutes));
        trip.setStatus(status);
        entityManager.persist(trip);
        entityManager.flush();
    }
}
