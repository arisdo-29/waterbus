package com.hsf302.waterbus.service.impl;

import com.hsf302.waterbus.entity.Waterbus;
import com.hsf302.waterbus.entity.Seat;
import com.hsf302.waterbus.dto.AdminWaterbusForm;
import com.hsf302.waterbus.dto.WaterbusDTO;
import com.hsf302.waterbus.repository.WaterbusRepository;
import com.hsf302.waterbus.repository.SeatRepository;
import com.hsf302.waterbus.repository.TripRepository;
import com.hsf302.waterbus.service.WaterbusService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

// Module: BE2 - Tàu, Ghế & Chuyến.
@Service
public class WaterbusServiceImpl implements WaterbusService {
    private final WaterbusRepository waterbusRepository;
    private final SeatRepository seatRepository;
    private final TripRepository tripRepository;

    public WaterbusServiceImpl(WaterbusRepository waterbusRepository, SeatRepository seatRepository,
                               TripRepository tripRepository) {
        this.waterbusRepository = waterbusRepository;
        this.seatRepository = seatRepository;
        this.tripRepository = tripRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Waterbus> getWaterbuses() {
        return waterbusRepository.findAllByOrderByIdAsc();
    }

    @Override
    @Transactional(readOnly = true)
    public List<WaterbusDTO> getWaterbusViews() {
        return waterbusRepository.findAllByOrderByIdAsc().stream().map(WaterbusDTO::fromEntity).toList();
    }

    @Override
    @Transactional
    public void saveWaterbus(AdminWaterbusForm form) {
        if (form.getId() != null) {
            throw new IllegalArgumentException("Chỉ có thể thêm tàu mới.");
        }
        String code = form.getCode().trim();
        String status = form.getStatus() == null ? null : form.getStatus().trim();
        if (!Set.of("ACTIVE", "MAINTENANCE", "INACTIVE").contains(status)) {
            throw new IllegalArgumentException("Trạng thái tàu không hợp lệ.");
        }
        waterbusRepository.findFirstByCodeIgnoreCase(code).ifPresent(existing -> {
            throw new IllegalArgumentException("Mã tàu đã được sử dụng.");
        });
        Waterbus waterbus = form.toEntity();
        waterbus = waterbusRepository.save(waterbus);
        java.util.ArrayList<Seat> newSeats = new java.util.ArrayList<>();
        for (int index = 1; index <= form.getCapacity(); index++) {
            int row = (index - 1) / 6 + 1;
            int column = (index - 1) % 6 + 1;
            var seat = new Seat();
            seat.setWaterbus(waterbus);
            seat.setSeatCode(rowLabel(row) + column);
            seat.setSeatRow(row);
            seat.setSeatCol(column);
            seat.setIsActive(true);
            newSeats.add(seat);
        }
        seatRepository.saveAll(newSeats);
    }

    @Override
    @Transactional
    public void changeStatus(int waterbusId, String status) {
        if (!Set.of("ACTIVE", "MAINTENANCE", "INACTIVE").contains(status)) {
            throw new IllegalArgumentException("Trạng thái tàu không hợp lệ.");
        }
        Waterbus waterbus = waterbusRepository.findById(waterbusId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tàu."));
        if ("ACTIVE".equals(waterbus.getStatus()) && !"ACTIVE".equals(status)) {
            long upcomingTrips = countUpcomingScheduledTrips(waterbusId);
            if (upcomingTrips > 0) {
                throw new IllegalArgumentException("Còn " + upcomingTrips
                        + " chuyến sắp chạy, hãy huỷ chuyến trước khi đổi trạng thái tàu.");
            }
        }
        waterbus.setStatus(status);
    }

    @Override
    @Transactional(readOnly = true)
    public long countUpcomingScheduledTrips(int waterbusId) {
        return tripRepository.countByWaterbusIdAndStatusAndDepartureTimeAfter(
                waterbusId, "SCHEDULED", LocalDateTime.now());
    }

    private String rowLabel(int row) {
        StringBuilder label = new StringBuilder();
        while (row > 0) {
            row--;
            label.insert(0, (char) ('A' + row % 26));
            row /= 26;
        }
        return label.toString();
    }
}
