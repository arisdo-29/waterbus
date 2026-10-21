package com.hsf302.waterbus.service.impl;

import com.hsf302.waterbus.dto.RouteDTO;
import com.hsf302.waterbus.dto.RouteForm;
import com.hsf302.waterbus.dto.StationDTO;
import com.hsf302.waterbus.entity.Route;
import com.hsf302.waterbus.entity.RouteStation;
import com.hsf302.waterbus.entity.Station;
import com.hsf302.waterbus.repository.RouteRepository;
import com.hsf302.waterbus.repository.RouteStationRepository;
import com.hsf302.waterbus.repository.StationRepository;
import com.hsf302.waterbus.service.RouteService;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RouteServiceImpl implements RouteService {
    private final RouteRepository routeRepository;
    private final RouteStationRepository stopRepository;
    private final StationRepository stationRepository;

    public RouteServiceImpl(RouteRepository routeRepository, RouteStationRepository stopRepository,
                            StationRepository stationRepository) {
        this.routeRepository = routeRepository;
        this.stopRepository = stopRepository;
        this.stationRepository = stationRepository;
    }

    @Override
    public List<Route> getActiveRoutes() {
        return routeRepository.findByIsActiveTrueOrderByIdAsc();
    }

    @Override
    public List<RouteStation> getStopsOfRoute(int routeId) {
        return stopRepository.findByRouteIdOrderByStopOrderAsc(routeId);
    }

    @Override
    public List<RouteDTO> getActiveRouteViews() {
        return getActiveRoutes().stream().map(this::toDTO).toList();
    }

    @Override
    public List<StationDTO> getStationViewsOfRoute(int routeId) {
        return getStopsOfRoute(routeId).stream().map(RouteStation::getStation)
                .map(s -> new StationDTO(s.getId(), s.getCode(), s.getName(), s.getAddress(), s.getIsActive()))
                .toList();
    }

    @Override
    public List<RouteDTO> getRoutesForAdmin() {
        return routeRepository.findAllByOrderByIdAsc().stream().map(this::toDTO).toList();
    }

    @Override
    public RouteForm getRouteForEdit(int routeId) {
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tuyến cần sửa."));
        RouteForm form = new RouteForm();
        form.setId(route.getId());
        form.setCode(route.getCode());
        form.setName(route.getName());
        form.setDescription(route.getDescription());
        form.setIsActive(route.getIsActive());
        form.setStationIds(getStopsOfRoute(routeId).stream().map(s -> s.getStation().getId()).toList());
        return form;
    }

    @Override
    @Transactional
    public int saveRoute(RouteForm form) {
        Route route = form.getId() == null ? new Route() : requireForUpdate(form.getId());
        routeRepository.findFirstByCodeIgnoreCase(form.getCode()).ifPresent(existing -> {
            if (!existing.getId().equals(form.getId())) {
                throw new IllegalArgumentException("Mã tuyến đã tồn tại. Vui lòng chọn mã khác.");
            }
        });
        List<Integer> ids = form.getStationIds();
        if (ids == null || ids.size() < 2 || ids.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Tuyến cần ít nhất hai bến hợp lệ.");
        }
        if (new HashSet<>(ids).size() != ids.size()) {
            throw new IllegalArgumentException("Mỗi bến chỉ được xuất hiện một lần trong tuyến.");
        }
        Map<Integer, Station> stations = stationRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Station::getId, Function.identity()));
        if (stations.size() != ids.size()) {
            throw new IllegalArgumentException("Có bến không tồn tại. Vui lòng chọn lại.");
        }
        if (Boolean.TRUE.equals(form.getIsActive()) && stations.values().stream().anyMatch(s -> !s.getIsActive())) {
            throw new IllegalArgumentException("Tuyến hoạt động chỉ được sử dụng bến đang hoạt động.");
        }
        route.setCode(form.getCode());
        route.setName(form.getName());
        route.setDescription(form.getDescription());
        route.setIsActive(form.getIsActive());
        routeRepository.saveAndFlush(route);

        List<RouteStation> existing = getStopsOfRoute(route.getId());
        Map<Integer, RouteStation> byStation = existing.stream()
                .collect(Collectors.toMap(s -> s.getStation().getId(), Function.identity()));
        // Park existing rows above the current range before swapping order, preserving SQL unique/check constraints.
        int temporaryOrder = existing.stream().mapToInt(RouteStation::getStopOrder).max().orElse(0);
        for (RouteStation stop : existing) {
            temporaryOrder = Math.incrementExact(temporaryOrder);
            stop.setStopOrder(temporaryOrder);
        }
        stopRepository.flush();
        // No schema FK references route_station. Only removed memberships are deleted; routes/stations are never deleted.
        stopRepository.deleteAll(existing.stream().filter(s -> !ids.contains(s.getStation().getId())).toList());
        stopRepository.flush();
        for (int index = 0; index < ids.size(); index++) {
            Integer stationId = ids.get(index);
            RouteStation stop = byStation.get(stationId);
            if (stop == null) {
                stop = new RouteStation(); // minutesFromStart keeps entity/schema default 0; no timetable calculation.
                stop.setRoute(route);
                stop.setStation(stations.get(stationId));
            }
            stop.setStopOrder(index + 1);
            stopRepository.save(stop);
        }
        stopRepository.flush();
        return route.getId();
    }

    @Override
    @Transactional
    public void hideRoute(int routeId) {
        Route route = requireForUpdate(routeId);
        route.setIsActive(false);
        routeRepository.flush();
    }

    private Route requireForUpdate(int routeId) {
        return routeRepository.findForUpdate(routeId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tuyến cần sửa hoặc ngừng hoạt động."));
    }

    private RouteDTO toDTO(Route route) {
        return new RouteDTO(route.getId(), route.getCode(), route.getName(), route.getDescription(), route.getIsActive());
    }
}
