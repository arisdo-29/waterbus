package com.hsf302.waterbus.service.impl;

import com.hsf302.waterbus.dto.StationDTO;
import com.hsf302.waterbus.dto.StationForm;
import com.hsf302.waterbus.entity.Station;
import com.hsf302.waterbus.repository.StationRepository;
import com.hsf302.waterbus.repository.RouteStationRepository;
import com.hsf302.waterbus.service.StationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class StationServiceImpl implements StationService {

    private final StationRepository stationRepository;
    private final RouteStationRepository routeStationRepository;

    public StationServiceImpl(StationRepository stationRepository, RouteStationRepository routeStationRepository) {
        this.stationRepository = stationRepository;
        this.routeStationRepository = routeStationRepository;
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
        return StationForm.fromEntity(station);
    }

    @Override
    @Transactional
    public int saveStation(StationForm form) {
        Station station = form.getId() == null ? new Station() : requireStation(form.getId());
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
        requireNoActiveRoute(stationId);
        station.setIsActive(false);
        stationRepository.flush();
    }

    private Station requireStation(int stationId) {
        return stationRepository.findById(stationId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bến cần sửa hoặc ẩn."));
    }

    private void requireNoActiveRoute(int stationId) {
        if (routeStationRepository.existsByStationIdAndRouteIsActiveTrue(stationId)) {
            throw new IllegalArgumentException("Bến còn thuộc tuyến đang hoạt động. Vui lòng ngừng tuyến trước khi ẩn bến.");
        }
    }
}
