package com.hsf302.waterbus;

import com.hsf302.waterbus.dto.AccountDTO;
import com.hsf302.waterbus.dto.RouteForm;
import com.hsf302.waterbus.dto.StationForm;
import com.hsf302.waterbus.entity.Route;
import com.hsf302.waterbus.entity.Station;
import com.hsf302.waterbus.entity.Trip;
import com.hsf302.waterbus.entity.Waterbus;
import com.hsf302.waterbus.service.RouteService;
import com.hsf302.waterbus.service.StationService;
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

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.jpa.open-in-view=false")
@Transactional
class StationEditingGuardTests {
    @Autowired WebApplicationContext context;
    @Autowired StationService stationService;
    @Autowired RouteService routeService;
    @Autowired EntityManager entityManager;
    private MockMvc mvc;
    private MockHttpSession session;
    private int stationId;
    private int firstId;
    private int lastId;
    private int routeId;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        session = new MockHttpSession();
        session.setAttribute("loginAccount", new AccountDTO(1, "admin@station.test", "Admin", null, "ADMIN", "ACTIVE"));
        firstId = stationService.saveStation(station("ST_FIRST"));
        stationId = stationService.saveStation(station("ST_MIDDLE"));
        lastId = stationService.saveStation(station("ST_LAST"));
        routeId = route("ST_ROUTE", List.of(firstId, stationId, lastId), List.of(0, 15, 40));
    }

    @ParameterizedTest
    @CsvSource({"SCHEDULED, 30, 90, true", "SCHEDULED, -15, 45, true", "DEPARTED, -15, 45, true", "SCHEDULED, 30, 90, false"})
    void stationOnRouteWithLiveTripsCannotBeEdited(String status, int departure, int arrival, boolean activeRoute) throws Exception {
        entityManager.find(Route.class, routeId).setIsActive(activeRoute);
        trip(routeId, status, departure, arrival);
        mvc.perform(get("/admin/stations/" + stationId + "/edit").session(session))
                .andExpect(redirectedUrl("/admin/stations"))
                .andExpect(flash().attribute("errorMessage", containsString("1 chuyến đang chạy hoặc sắp khởi hành")));
        mvc.perform(saveRequest()).andExpect(status().isOk())
                .andExpect(view().name("admin/station/form"))
                .andExpect(model().hasErrors()).andExpect(model().attribute("errorMessage", containsString("Chỉ được sửa hoặc ẩn bến")));
        mvc.perform(post("/admin/stations/" + stationId + "/delete").session(session))
                .andExpect(redirectedUrl("/admin/stations"))
                .andExpect(flash().attribute("errorMessage", containsString("chuyến đang chạy hoặc sắp khởi hành")));
        assertOriginalStation();
    }

    @ParameterizedTest
    @CsvSource({"COMPLETED, -90, -30", "CANCELLED, 30, 90", "DEPARTED, -90, -30", "SCHEDULED, -90, -30"})
    void completedOrCancelledTripsDoNotBlockStationEditing(String status, int departure, int arrival) throws Exception {
        trip(routeId, status, departure, arrival);
        assertEditAllowed();
    }

    @Test
    void tripOnAnotherRouteWithoutTheStationDoesNotBlockEditing() throws Exception {
        int otherRoute = route("ST_OTHER", List.of(firstId, lastId), List.of(0, 40));
        trip(otherRoute, "SCHEDULED", 30, 90);
        assertEditAllowed();
    }

    @Test
    void savingOldFormAndCallingServiceDirectlyCannotBypassNewTrip() throws Exception {
        StationForm form = stationService.getStationForEdit(stationId);
        mvc.perform(get("/admin/stations/" + stationId + "/edit").session(session)).andExpect(status().isOk());
        trip(routeId, "SCHEDULED", 30, 90);
        mvc.perform(saveRequest()).andExpect(view().name("admin/station/form"))
                .andExpect(model().hasErrors()).andExpect(model().attributeExists("errorMessage"));
        form.setName("Không được lưu");
        assertThatThrownBy(() -> stationService.saveStation(form)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> stationService.hideStation(stationId)).isInstanceOf(IllegalStateException.class);
        assertOriginalStation();
    }

    private void assertEditAllowed() throws Exception {
        mvc.perform(get("/admin/stations/" + stationId + "/edit").session(session))
                .andExpect(status().isOk()).andExpect(view().name("admin/station/form"));
        mvc.perform(saveRequest()).andExpect(redirectedUrl("/admin/stations"))
                .andExpect(flash().attributeExists("successMessage"));
        entityManager.clear();
        Station stored = entityManager.find(Station.class, stationId);
        assertThat(stored.getName()).isEqualTo("Tên bến đã sửa");
        assertThat(stored.getCode()).isEqualTo("ST_CHANGED");
        assertThat(stored.getAddress()).isEqualTo("Địa chỉ mới");
    }

    private void assertOriginalStation() {
        entityManager.clear();
        Station stored = entityManager.find(Station.class, stationId);
        assertThat(stored.getCode()).isEqualTo("ST_MIDDLE");
        assertThat(stored.getName()).isEqualTo("Tên bến gốc");
        assertThat(stored.getAddress()).isEqualTo("Địa chỉ gốc");
        assertThat(stored.getIsActive()).isTrue();
    }

    private MockHttpServletRequestBuilder saveRequest() {
        return post("/admin/stations/save").session(session).param("id", "" + stationId)
                .param("code", "ST_CHANGED").param("name", "Tên bến đã sửa")
                .param("address", "Địa chỉ mới").param("isActive", "true");
    }

    private StationForm station(String code) {
        StationForm form = new StationForm();
        form.setCode(code);
        form.setName("Tên bến gốc");
        form.setAddress("Địa chỉ gốc");
        form.setIsActive(true);
        return form;
    }

    private int route(String code, List<Integer> stations, List<Integer> minutes) {
        RouteForm form = new RouteForm();
        form.setCode(code);
        form.setName(code);
        form.setIsActive(true);
        form.setStationIds(stations);
        form.setMinutesFromStart(minutes);
        return routeService.saveRoute(form);
    }

    private void trip(int routeId, String status, int departure, int arrival) {
        Waterbus waterbus = new Waterbus();
        waterbus.setCode("ST_GUARD_WB");
        waterbus.setName("Tàu kiểm thử bến");
        waterbus.setCapacity(20);
        waterbus.setStatus("ACTIVE");
        entityManager.persist(waterbus);
        LocalDateTime now = LocalDateTime.now();
        Trip trip = new Trip();
        trip.setTripCode("ST_GUARD_TRIP");
        trip.setRoute(entityManager.getReference(Route.class, routeId));
        trip.setWaterbus(waterbus);
        trip.setDepartureTime(now.plusMinutes(departure));
        trip.setArrivalTime(now.plusMinutes(arrival));
        trip.setStatus(status);
        entityManager.persist(trip);
        entityManager.flush();
    }
}
