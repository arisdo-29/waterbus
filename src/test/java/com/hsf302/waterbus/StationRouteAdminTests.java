package com.hsf302.waterbus;

import com.hsf302.waterbus.dto.AccountDTO;
import com.hsf302.waterbus.dto.RouteForm;
import com.hsf302.waterbus.dto.StationForm;
import com.hsf302.waterbus.entity.RouteStation;
import com.hsf302.waterbus.entity.Station;
import com.hsf302.waterbus.repository.RouteRepository;
import com.hsf302.waterbus.repository.RouteStationRepository;
import com.hsf302.waterbus.repository.StationRepository;
import com.hsf302.waterbus.service.RouteService;
import com.hsf302.waterbus.service.StationService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.jpa.open-in-view=false")
@Transactional
class StationRouteAdminTests {
    @Autowired WebApplicationContext context;
    @Autowired StationService stationService;
    @Autowired RouteService routeService;
    @Autowired StationRepository stations;
    @Autowired RouteRepository routes;
    @Autowired RouteStationRepository stops;
    @Autowired EntityManager entityManager;
    MockMvc mvc;
    MockHttpSession session;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        session = new MockHttpSession();
        session.setAttribute("loginAccount", new AccountDTO(1, "admin@example.test", "Quản trị BE1", "0900000000", "ADMIN", "ACTIVE"));
        stops.deleteAll();
        routes.deleteAll();
        stations.deleteAll();
        // Entity-generated H2 schema lacks the SQL script's unique/check constraints. Add them in this test only.
        entityManager.createNativeQuery("CREATE UNIQUE INDEX IF NOT EXISTS test_station_code ON station(code)").executeUpdate();
        entityManager.createNativeQuery("CREATE UNIQUE INDEX IF NOT EXISTS test_route_code ON route(code)").executeUpdate();
        entityManager.createNativeQuery("CREATE UNIQUE INDEX IF NOT EXISTS test_route_order ON route_station(route_id, stop_order)").executeUpdate();
        entityManager.createNativeQuery("CREATE UNIQUE INDEX IF NOT EXISTS test_route_station ON route_station(route_id, station_id)").executeUpdate();
        entityManager.createNativeQuery("ALTER TABLE route_station ADD CONSTRAINT IF NOT EXISTS test_stop_order CHECK (stop_order >= 1)").executeUpdate();
        entityManager.createNativeQuery("ALTER TABLE route_station ADD CONSTRAINT IF NOT EXISTS test_stop_minutes CHECK (minutes_from_start >= 0)").executeUpdate();
    }

    @Test
    void stationCreateEditHideAndCheckboxMarkerWorkWithoutDeletingMemberships() throws Exception {
        MvcResult createdResult = mvc.perform(post("/admin/stations/save").session(session).param("code", "NEW")
                        .param("name", "Bến mới tiếng Việt").param("address", "Địa chỉ mới")
                        .param("_isActive", "on").param("isActive", "true").param("unexpectedField", "ignored"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/admin/stations"))
                .andExpect(flash().attributeExists("successMessage")).andReturn();
        assertThat(html(mvc.perform(get("/admin/stations").session(session).flashAttrs(createdResult.getFlashMap()))
                .andExpect(status().isOk()).andReturn())).contains("Đã lưu thông tin bến.");
        Station created = stations.findFirstByCodeIgnoreCase("NEW").orElseThrow();
        int second = station("SECOND", true);
        int routeId = routeService.saveRoute(route("R", true, created.getId(), second));
        long memberships = stops.count();
        mvc.perform(post("/admin/stations/save").session(session).param("id", created.getId().toString())
                        .param("code", "NEW").param("name", "Tên đã sửa").param("_isActive", "on"))
                .andExpect(redirectedUrl("/admin/stations"));
        entityManager.clear();
        assertThat(stations.findById(created.getId()).orElseThrow().getIsActive()).isFalse();
        assertThat(stationService.getStations()).hasSize(2);
        assertThat(stationService.getActiveStations()).extracting(s -> s.getId()).containsExactly(second);
        mvc.perform(post("/admin/stations/" + second + "/delete").session(session))
                .andExpect(redirectedUrl("/admin/stations")).andExpect(flash().attributeExists("successMessage"));
        assertThat(stations.count()).isEqualTo(2);
        assertThat(stops.count()).isEqualTo(memberships);
        assertThat(routes.findById(routeId).orElseThrow().getIsActive()).isTrue();
        mvc.perform(get("/stations")).andExpect(status().isOk()).andExpect(view().name("station/list"));
    }

    @Test
    void duplicateCodesAndMissingStationIdsReturnUsefulErrorsAndPreserveInputs() throws Exception {
        int id = station("DUP", true);
        String html = html(mvc.perform(post("/admin/stations/save").session(session).param("code", "dup")
                        .param("name", "Tên giữ lại").param("address", "Địa chỉ giữ lại").param("isActive", "true"))
                .andExpect(view().name("admin/station/form")).andExpect(model().hasErrors()).andReturn());
        assertThat(html).contains("Mã bến đã tồn tại", "Tên giữ lại", "Địa chỉ giữ lại");
        mvc.perform(post("/admin/stations/save").session(session).param("id", "2147483647")
                        .param("code", "MISSING").param("name", "Không tạo mới").param("isActive", "true"))
                .andExpect(view().name("admin/station/form")).andExpect(model().attributeExists("errorMessage"));
        assertThat(stations.count()).isEqualTo(1);
        mvc.perform(get("/admin/stations/2147483647/edit").session(session)).andExpect(status().isNotFound());
        mvc.perform(post("/admin/stations/2147483647/delete").session(session))
                .andExpect(flash().attributeExists("errorMessage"));
        StationForm same = stationService.getStationForEdit(id);
        same.setName("Đổi tên cùng mã");
        assertThat(stationService.saveStation(same)).isEqualTo(id);
    }

    @Test
    void stationValidationEnforcesSchemaLengthsAndDoesNotBindEntities() throws Exception {
        for (String[] values : List.of(new String[]{"", "Tên", ""}, new String[]{"A".repeat(21), "Tên", ""},
                new String[]{"CODE", " ", ""}, new String[]{"CODE", "N".repeat(101), ""},
                new String[]{"CODE", "Tên", "A".repeat(256)})) {
            mvc.perform(post("/admin/stations/save").session(session).param("code", values[0])
                            .param("name", values[1]).param("address", values[2]).param("isActive", "true"))
                    .andExpect(view().name("admin/station/form")).andExpect(model().hasErrors());
        }
        mvc.perform(post("/admin/stations/save").session(session).param("id", "not-an-id")
                        .param("code", "GOOD").param("name", "Dữ liệu đã nhập").param("isActive", "true"))
                .andExpect(view().name("admin/station/form")).andExpect(model().attributeHasFieldErrors("station", "id"));
        assertThat(stations.count()).isZero();
    }

    @Test
    void routeCreateReorderRetainsIdsMinutesAndLoadsSameOrder() throws Exception {
        int first = station("A", true), second = station("B", true), third = station("C", true);
        mvc.perform(post("/admin/routes/save").session(session).param("code", "ORDER")
                        .param("name", "Tuyến mới tiếng Việt").param("description", "Theo thứ tự yêu cầu")
                        .param("_isActive", "on").param("isActive", "true")
                        .param("stationIds", "" + second, "" + first))
                .andExpect(redirectedUrl("/admin/routes")).andExpect(flash().attributeExists("successMessage"));
        int id = routes.findFirstByCodeIgnoreCase("ORDER").orElseThrow().getId();
        List<RouteStation> original = stops.findByRouteIdOrderByStopOrderAsc(id);
        original.get(0).setMinutesFromStart(17);
        original.get(1).setMinutesFromStart(41);
        stops.flush();
        Integer retainedId = original.get(0).getId();
        RouteForm form = routeService.getRouteForEdit(id);
        form.setStationIds(List.of(first, third, second));
        routeService.saveRoute(form);
        entityManager.clear();
        List<RouteStation> reloaded = stops.findByRouteIdOrderByStopOrderAsc(id);
        assertThat(reloaded).extracting(s -> s.getStation().getId()).containsExactly(first, third, second);
        assertThat(reloaded).extracting(RouteStation::getStopOrder).containsExactly(1, 2, 3);
        assertThat(reloaded).extracting(RouteStation::getMinutesFromStart).containsExactly(41, 0, 17);
        assertThat(reloaded.get(2).getId()).isEqualTo(retainedId);
        assertThat(routeService.getRouteForEdit(id).getStationIds()).containsExactly(first, third, second);
        String html = html(mvc.perform(get("/admin/routes/" + id + "/edit").session(session))
                .andExpect(status().isOk()).andReturn());
        assertThat(html).contains("Tuyến mới tiếng Việt", "selected=\"selected\"");
        export("admin-route-edit", html);
        // Remove one membership while retaining the other IDs and minute values.
        form.setStationIds(List.of(third, second));
        routeService.saveRoute(form);
        entityManager.clear();
        assertThat(stops.findByRouteIdOrderByStopOrderAsc(id)).extracting(RouteStation::getMinutesFromStart).containsExactly(0, 17);
    }

    @Test
    void routeRejectsInvalidStationsAndAllowsInactiveStationsOnlyOnInactiveRoutes() throws Exception {
        int first = station("A", true), second = station("B", true), inactive = station("OFF", false);
        for (List<Integer> ids : List.of(List.of(first), List.of(first, first), List.of(first, Integer.MAX_VALUE), List.of(first, inactive))) {
            RouteForm invalid = route("INVALID", true, first, second);
            invalid.setStationIds(ids);
            assertThatThrownBy(() -> routeService.saveRoute(invalid)).isInstanceOf(IllegalArgumentException.class);
        }
        assertThat(routes.count()).isZero();
        int id = routeService.saveRoute(route("INACTIVE", false, first, inactive));
        assertThat(routeService.getRouteForEdit(id).getStationIds()).containsExactly(first, inactive);
        RouteForm form = routeService.getRouteForEdit(id);
        form.setIsActive(true);
        assertThatThrownBy(() -> routeService.saveRoute(form)).hasMessageContaining("bến đang hoạt động");
    }

    @Test
    void routeValidationReturnsCompleteModelAndPreservesOrderedSelections() throws Exception {
        int first = station("A", true), second = station("B", true);
        MvcResult invalid = mvc.perform(post("/admin/routes/save").session(session).param("code", "")
                        .param("name", "Tên giữ lại").param("description", "Mô tả giữ lại")
                        .param("_isActive", "on").param("stationIds", "" + second, "" + first))
                .andExpect(view().name("admin/route/form"))
                .andExpect(model().attributeHasFieldErrors("route", "code"))
                .andExpect(model().attributeExists("stations", "errorMessage")).andReturn();
        RouteForm retained = (RouteForm) invalid.getModelAndView().getModel().get("route");
        assertThat(retained.getStationIds()).containsExactly(second, first);
        assertThat(retained.getIsActive()).isFalse();
        assertThat(html(invalid)).contains("Tên giữ lại", "Mô tả giữ lại", "Vui lòng nhập mã tuyến.");
        for (String[] ids : List.of(new String[]{"" + first}, new String[]{"" + first, "" + first},
                new String[]{"" + first, "2147483647"}, new String[]{"", "" + first}, new String[]{"oops", "" + first})) {
            mvc.perform(post("/admin/routes/save").session(session).param("code", "INVALID")
                            .param("name", "Tuyến chưa hợp lệ").param("stationIds", ids).param("isActive", "true"))
                    .andExpect(view().name("admin/route/form")).andExpect(model().hasErrors())
                    .andExpect(model().attributeExists("stations", "errorMessage"));
        }
        assertThat(routes.count()).isZero();
    }

    @Test
    void routeFieldValidationAndUncheckedMarker() throws Exception {
        int first = station("A", true), second = station("B", true);
        for (String[] values : List.of(new String[]{"A".repeat(21), "Tên", ""},
                new String[]{"CODE", " ", ""}, new String[]{"CODE", "N".repeat(101), ""},
                new String[]{"CODE", "Tên", "D".repeat(256)})) {
            mvc.perform(post("/admin/routes/save").session(session).param("code", values[0])
                            .param("name", values[1]).param("description", values[2]).param("isActive", "true")
                            .param("stationIds", "" + first, "" + second))
                    .andExpect(view().name("admin/route/form")).andExpect(model().hasErrors());
        }
        mvc.perform(post("/admin/routes/save").session(session).param("code", "UNCHECKED")
                        .param("name", "Tuyến tạm ngừng").param("_isActive", "on")
                        .param("stationIds", "" + first, "" + second))
                .andExpect(redirectedUrl("/admin/routes"));
        assertThat(routes.findFirstByCodeIgnoreCase("UNCHECKED").orElseThrow().getIsActive()).isFalse();
    }

    @Test
    void routeDuplicateMissingIdsAndSoftDelete() throws Exception {
        int first = station("A", true), second = station("B", true);
        int id = routeService.saveRoute(route("CODE", true, first, second));
        mvc.perform(post("/admin/routes/save").session(session).param("code", "code").param("name", "Bản trùng")
                        .param("stationIds", "" + first, "" + second).param("isActive", "true"))
                .andExpect(view().name("admin/route/form")).andExpect(model().attributeExists("errorMessage"));
        mvc.perform(post("/admin/routes/save").session(session).param("id", "2147483647")
                        .param("code", "NEW").param("name", "Không tạo mới").param("isActive", "true")
                        .param("stationIds", "" + first, "" + second))
                .andExpect(view().name("admin/route/form")).andExpect(model().hasErrors());
        mvc.perform(get("/admin/routes/2147483647/edit").session(session)).andExpect(status().isNotFound());
        mvc.perform(post("/admin/routes/2147483647/delete").session(session)).andExpect(flash().attributeExists("errorMessage"));
        mvc.perform(post("/admin/routes/" + id + "/delete").session(session))
                .andExpect(redirectedUrl("/admin/routes")).andExpect(flash().attributeExists("successMessage"));
        assertThat(routes.count()).isEqualTo(1);
        assertThat(stops.count()).isEqualTo(2);
        assertThat(routeService.getActiveRoutes()).isEmpty();
        mvc.perform(get("/admin/routes/" + id + "/delete").session(session)).andExpect(status().isMethodNotAllowed());
        mvc.perform(get("/admin/stations/" + first + "/delete").session(session)).andExpect(status().isMethodNotAllowed());
    }

    @Test
    void publicRoutesRenderOnlyActiveWithOrderedDtoStopsAndEmptyState() throws Exception {
        String empty = html(mvc.perform(get("/routes")).andExpect(status().isOk()).andReturn());
        assertThat(empty).contains("Chưa có tuyến nào").doesNotContain("Chuyến ngắn buổi tối");
        export("routes-empty", empty);
        int first = station("A", true), second = station("B", true);
        routeService.saveRoute(route("ACTIVE", true, second, first));
        routeService.saveRoute(route("HIDDEN", false, first, second));
        entityManager.clear();
        String html = html(mvc.perform(get("/routes")).andExpect(view().name("route/index"))
                .andExpect(model().attributeExists("routes", "stopsByRoute")).andReturn());
        assertThat(html).contains("ACTIVE").doesNotContain("HIDDEN", "Chưa có tuyến nào");
        assertThat(html.indexOf("Bến B")).isLessThan(html.indexOf("Bến A"));
        export("routes", html);
        export("stations", html(mvc.perform(get("/stations")).andReturn()));
        export("station", html(mvc.perform(get("/stations/" + first)).andReturn()));
        export("admin-station-list", html(mvc.perform(get("/admin/stations").session(session)).andReturn()));
        export("admin-station-edit", html(mvc.perform(get("/admin/stations/" + first + "/edit").session(session)).andReturn()));
        export("admin-route-list", html(mvc.perform(get("/admin/routes").session(session)).andReturn()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/admin/stations/new", "/admin/routes/new"})
    void newFormsRenderRealTemplatesWithRequestDtos(String url) throws Exception {
        String html = html(mvc.perform(get(url).session(session)).andExpect(status().isOk()).andReturn());
        assertThat(html).contains("name=\"_isActive\"", "checked=\"checked\"", "method=\"post\"");
    }

    private int station(String code, boolean active) {
        StationForm form = new StationForm();
        form.setCode(code);
        form.setName("Bến " + code);
        form.setAddress("Địa chỉ " + code);
        form.setIsActive(active);
        return stationService.saveStation(form);
    }

    private RouteForm route(String code, boolean active, Integer... ids) {
        RouteForm form = new RouteForm();
        form.setCode(code);
        form.setName("Tuyến " + code);
        form.setIsActive(active);
        form.setStationIds(List.of(ids));
        return form;
    }

    private String html(MvcResult result) throws Exception {
        return result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private void export(String name, String html) throws Exception {
        Files.createDirectories(Path.of("target/be1-admin-preview"));
        Files.writeString(Path.of("target/be1-admin-preview/" + name + ".html"), html);
    }
}
