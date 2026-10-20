package com.hsf302.waterbus;

import com.hsf302.waterbus.dto.StationDTO;
import com.hsf302.waterbus.entity.Station;
import com.hsf302.waterbus.repository.StationRepository;
import com.hsf302.waterbus.service.StationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
class HomeStationTests {
    @Autowired WebApplicationContext context;
    @Autowired StationRepository repository;
    @Autowired StationService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        repository.deleteAll();
    }

    @Test
    void homeUsesActiveStationsFromDatabaseInIdOrderWithoutChangingStationList() throws Exception {
        Station first = save("Z", "Bến Z", true);
        Station hidden = save("HIDDEN", "Bến ngừng hoạt động", false);
        Station second = save("A", "Bến A", true);
        assertThat(service.getActiveStations()).extracting(StationDTO::getId)
                .containsExactly(first.getId(), second.getId());
        assertThat(service.getStations()).extracting(StationDTO::getId).contains(hidden.getId());

        String html = mvc.perform(get("/"))
                .andExpect(status().isOk()).andExpect(view().name("index"))
                .andExpect(model().attribute("stations", service.getActiveStations()))
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertThat(html).contains("action=\"/trips\"", "name=\"fromStationId\"", "name=\"toStationId\"", "name=\"date\"")
                .doesNotContain("Bến ngừng hoạt động", "disabled=\"disabled\"");
        assertThat(html.indexOf("Bến Z")).isLessThan(html.indexOf("Bến A"));
        java.nio.file.Files.createDirectories(java.nio.file.Path.of("target/be1-preview"));
        java.nio.file.Files.writeString(java.nio.file.Path.of("target/be1-preview/home.html"), html);
    }

    @Test
    void emptyAndInactiveOnlyDatabasesRenderDisabledSearch() throws Exception {
        assertEmptyHome();
        save("OFF", "Bến ngừng hoạt động", false);
        assertEmptyHome();
    }

    private void assertEmptyHome() throws Exception {
        assertThat(service.getActiveStations()).isEmpty();
        String html = mvc.perform(get("/")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertThat(html).contains("disabled=\"disabled\"", "Thông tin bến tàu đang được cập nhật")
                .doesNotContain("Bến ngừng hoạt động");
    }

    private Station save(String code, String name, boolean active) {
        Station station = new Station();
        station.setCode(code);
        station.setName(name);
        station.setIsActive(active);
        return repository.saveAndFlush(station);
    }
}
