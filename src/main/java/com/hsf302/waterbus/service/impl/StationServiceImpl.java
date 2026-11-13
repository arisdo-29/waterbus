package com.hsf302.waterbus.service.impl;

import com.hsf302.waterbus.dto.StationDTO;
import com.hsf302.waterbus.dto.StationForm;
import com.hsf302.waterbus.entity.Station;
import com.hsf302.waterbus.repository.StationRepository;
import com.hsf302.waterbus.repository.RouteStationRepository;
import com.hsf302.waterbus.service.StationService;
import com.hsf302.waterbus.service.TripService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class StationServiceImpl implements StationService {

    private final StationRepository stationRepository;
    private final RouteStationRepository routeStationRepository;
    private final TripService tripService;

    public StationServiceImpl(StationRepository stationRepository, RouteStationRepository routeStationRepository,
                              TripService tripService) {
        this.stationRepository = stationRepository;
        this.routeStationRepository = routeStationRepository;
        this.tripService = tripService;
    }

    @Override
    public List<StationDTO> getStations() {
        return stationRepository.findAll().stream()
                .map(StationDTO::fromEntity)
                .toList();
    }

    @Override
    public List<StationDTO> getActiveStations() {
        return stationRepository.findByIsActiveTrueOrderByIdAsc().stream()
                .map(StationDTO::fromEntity)
                .toList();
    }

    @Override
    public StationDTO getStationById(int stationId) {
        return stationRepository.findById(stationId)
                .map(StationDTO::fromEntity)
                .orElse(null);
    }

    @Override
    public StationForm getStationForEdit(int stationId) {
        Station station = requireStation(stationId);
        requireNoActiveTrip(stationId);
        return StationForm.fromEntity(station);
    }

    @Override
    @Transactional
    public int saveStation(StationForm form) {
        Station station = form.getId() == null ? new Station() : requireStation(form.getId());
        if (form.getId() != null) {
            requireNoActiveTrip(form.getId());
        }
        stationRepository.findFirstByCodeIgnoreCase(form.getCode()).ifPresent(existing -> {
            if (!existing.getId().equals(form.getId())) {
                throw new IllegalArgumentException("Mã bến đã tồn tại. Vui lòng chọn mã khác.");
            }
        });
        if (station.getId() != null && Boolean.FALSE.equals(form.getIsActive())) {
            requireNoActiveRoute(station.getId());
        }
        form.applyTo(station);
        return stationRepository.saveAndFlush(station).getId();
    }

    @Override
    @Transactional
    public void hideStation(int stationId) {
        Station station = requireStation(stationId);
        requireNoActiveTrip(stationId);
        requireNoActiveRoute(stationId);
        station.setIsActive(false);
        stationRepository.flush();
    }

    private Station requireStation(int stationId) {
        return stationRepository.findById(stationId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bến cần sửa hoặc ẩn."));
    }

    private void requireNoActiveTrip(int stationId) {
        long active = tripService.countActiveTripsOfStation(stationId);
        if (active > 0) {
            throw new IllegalStateException("Bến còn thuộc tuyến có " + active
                    + " chuyến đang chạy hoặc sắp khởi hành. Chỉ được sửa hoặc ẩn bến sau khi các chuyến này hoàn tất hoặc bị huỷ.");
        }
    }

    private void requireNoActiveRoute(int stationId) {
        if (routeStationRepository.existsByStationIdAndRouteIsActiveTrue(stationId)) {
            throw new IllegalArgumentException("Bến còn thuộc tuyến đang hoạt động. Vui lòng ngừng tuyến trước khi ẩn bến.");
        }
    }
}
