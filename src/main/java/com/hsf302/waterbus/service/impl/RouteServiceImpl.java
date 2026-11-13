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
        return getActiveRoutes().stream().map(RouteDTO::fromEntity).toList();
    }

    @Override
    public List<StationDTO> getStationViewsOfRoute(int routeId) {
        return getStopsOfRoute(routeId).stream().map(RouteStation::getStation)
                .map(StationDTO::fromEntity)
                .toList();
    }

    @Override
    public Map<Integer, List<StationDTO>> getRouteViewsWithStops() {
        Map<Integer, List<StationDTO>> stopsByRoute = new LinkedHashMap<>();
        for (RouteDTO route : getActiveRouteViews()) {
            stopsByRoute.put(route.id(), getStationViewsOfRoute(route.id()));
        }
        return stopsByRoute;
    }

    @Override
    public List<RouteDTO> getRoutesForAdmin() {
        return routeRepository.findAllByOrderByIdAsc().stream().map(RouteDTO::fromEntity).toList();
    }

    @Override
    public RouteForm getRouteForEdit(int routeId) {
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tuyến cần sửa."));
        return RouteForm.fromEntity(route, getStopsOfRoute(routeId));
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
        List<Integer> minutes = form.getMinutesFromStart();
        validateMinutes(ids, minutes);
        Map<Integer, Station> stations = stationRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Station::getId, Function.identity()));
        if (stations.size() != ids.size()) {
            throw new IllegalArgumentException("Có bến không tồn tại. Vui lòng chọn lại.");
        }
        if (Boolean.TRUE.equals(form.getIsActive()) && stations.values().stream().anyMatch(s -> !s.getIsActive())) {
            throw new IllegalArgumentException("Tuyến hoạt động chỉ được sử dụng bến đang hoạt động.");
        }
        form.applyTo(route);
        routeRepository.saveAndFlush(route);

        List<RouteStation> existing = getStopsOfRoute(route.getId());
        Map<Integer, RouteStation> byStation = existing.stream()
                .collect(Collectors.toMap(s -> s.getStation().getId(), Function.identity()));
        int temporaryOrder = existing.stream().mapToInt(RouteStation::getStopOrder).max().orElse(0);
        for (RouteStation stop : existing) {
            temporaryOrder = Math.incrementExact(temporaryOrder);
            stop.setStopOrder(temporaryOrder);
        }
        stopRepository.flush();
        stopRepository.deleteAll(existing.stream().filter(s -> !ids.contains(s.getStation().getId())).toList());
        stopRepository.flush();
        for (int index = 0; index < ids.size(); index++) {
            Integer stationId = ids.get(index);
            RouteStation stop = byStation.get(stationId);
            if (stop == null) {
                stop = new RouteStation();
                stop.setRoute(route);
                stop.setStation(stations.get(stationId));
            }
            stop.setStopOrder(index + 1);
            stop.setMinutesFromStart(minutes.get(index));
            stopRepository.save(stop);
        }
        stopRepository.flush();
        return route.getId();
    }

    private void validateMinutes(List<Integer> stationIds, List<Integer> minutes) {
        if (minutes == null || minutes.size() != stationIds.size()) {
            throw new IllegalArgumentException("Mỗi bến phải có một số phút tương ứng.");
        }
        for (int index = 0; index < minutes.size(); index++) {
            Integer value = minutes.get(index);
            if (value == null || value < 0) {
                throw new IllegalArgumentException("Số phút của bến thứ " + (index + 1) + " phải là số không âm.");
            }
            if (index == 0 && value != 0) {
                throw new IllegalArgumentException("Số phút của bến đầu phải bằng 0.");
            }
            if (index > 0 && value <= minutes.get(index - 1)) {
                throw new IllegalArgumentException("Số phút của bến thứ " + (index + 1) + " phải lớn hơn bến trước.");
            }
        }
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
}
