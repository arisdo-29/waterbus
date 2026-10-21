package com.hsf302.waterbus;

import com.hsf302.waterbus.dto.RouteForm;
import com.hsf302.waterbus.dto.StationForm;
import com.hsf302.waterbus.entity.RouteStation;
import com.hsf302.waterbus.repository.RouteRepository;
import com.hsf302.waterbus.repository.RouteStationRepository;
import com.hsf302.waterbus.repository.StationRepository;
import com.hsf302.waterbus.service.RouteService;
import com.hsf302.waterbus.service.StationService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
class RouteTransactionTests {
    @Autowired RouteService service;
    @Autowired StationService stationService;
    @Autowired RouteRepository routes;
    @Autowired StationRepository stations;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired EntityManager entityManager;
    @MockitoSpyBean RouteStationRepository stops;

    @Test
    void failureAfterMembershipDeleteAndPartialInsertRollsBackWholeRoute() {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        int[] ids = transaction.execute(status -> {
            stops.deleteAll();
            routes.deleteAll();
            stations.deleteAll();
            return new int[]{station("ROLL-A"), station("ROLL-B"), station("ROLL-C")};
        });
        RouteForm form = new RouteForm();
        form.setCode("ROLLBACK");
        form.setName("Tên gốc");
        form.setStationIds(List.of(ids[0], ids[1]));
        int routeId = service.saveRoute(form);
        form.setId(routeId);
        form.setName("Không được lưu dở");
        form.setStationIds(List.of(ids[2], ids[0]));
        AtomicInteger saves = new AtomicInteger();
        doAnswer(call -> {
            if (saves.incrementAndGet() == 2) {
                throw new IllegalStateException("Simulated failure after first membership save");
            }
            RouteStation stop = call.getArgument(0);
            if (stop.getId() == null) {
                entityManager.persist(stop);
                return stop;
            }
            return entityManager.merge(stop);
        }).when(stops).save(any(RouteStation.class));
        try {
            assertThatThrownBy(() -> service.saveRoute(form)).isInstanceOf(IllegalStateException.class);
            assertThat(service.getRouteForEdit(routeId).getName()).isEqualTo("Tên gốc");
            assertThat(service.getRouteForEdit(routeId).getStationIds()).containsExactly(ids[0], ids[1]);
            assertThat(service.getStopsOfRoute(routeId)).extracting(RouteStation::getStopOrder).containsExactly(1, 2);
        } finally {
            reset(stops);
            transaction.executeWithoutResult(status -> {
                stops.deleteAll();
                routes.deleteAll();
                stations.deleteAll();
            });
        }
    }

    private int station(String code) {
        StationForm form = new StationForm();
        form.setCode(code);
        form.setName(code);
        return stationService.saveStation(form);
    }
}
