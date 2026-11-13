package com.hsf302.waterbus.service.impl;

import com.hsf302.waterbus.entity.Waterbus;
import com.hsf302.waterbus.entity.Seat;
import com.hsf302.waterbus.dto.AdminWaterbusForm;
import com.hsf302.waterbus.dto.WaterbusDTO;
import com.hsf302.waterbus.dto.TripCancellationPreviewDTO;
import com.hsf302.waterbus.repository.WaterbusRepository;
import com.hsf302.waterbus.repository.SeatRepository;
import com.hsf302.waterbus.repository.TripRepository;
import com.hsf302.waterbus.service.WaterbusService;
import com.hsf302.waterbus.service.TripService;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

@Service
public class WaterbusServiceImpl implements WaterbusService {
    private final WaterbusRepository waterbusRepository;
    private final SeatRepository seatRepository;
    private final TripRepository tripRepository;
    private final TripService tripService;

    public WaterbusServiceImpl(WaterbusRepository waterbusRepository, SeatRepository seatRepository,
                               TripRepository tripRepository, TripService tripService) {
        this.waterbusRepository = waterbusRepository;
        this.seatRepository = seatRepository;
        this.tripRepository = tripRepository;
        this.tripService = tripService;
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
        if (status == null || !Set.of("ACTIVE", "MAINTENANCE", "INACTIVE").contains(status)) {
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
        if ("ACTIVE".equals(status)) {
            clearSchedule(waterbus);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TripCancellationPreviewDTO> previewStatusChange(int waterbusId, String status,
                                                                 LocalDateTime startsAt, LocalDateTime endsAt) {
        Waterbus waterbus = waterbusRepository.findById(waterbusId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tàu."));
        validateStatusChange(waterbus, status, startsAt, endsAt, LocalDateTime.now());
        if ("ACTIVE".equals(status)) {
            return List.of();
        }
        if (tripRepository.countStartedTripsOverlappingOutage(waterbusId, startsAt, LocalDateTime.now()) > 0) {
            throw new IllegalArgumentException("Tàu đang có chuyến chạy qua giờ bắt đầu. Hãy chọn giờ sau khi chuyến kết thúc.");
        }
        return tripService.getTripsAffectedByOutage(waterbusId, startsAt, endsAt);
    }

    @Override
    @Transactional
    public int scheduleStatusChange(int waterbusId, String status, LocalDateTime startsAt,
                                    LocalDateTime endsAt, List<Integer> previewedTripIds) {
        Waterbus waterbus = waterbusRepository.findByIdForUpdate(waterbusId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tàu."));
        LocalDateTime now = LocalDateTime.now();
        validateStatusChange(waterbus, status, startsAt, endsAt, now);
        if ("ACTIVE".equals(status)) {
            if (previewedTripIds != null && !previewedTripIds.isEmpty()) {
                throw new IllegalArgumentException("Danh sách chuyến không hợp lệ.");
            }
            waterbus.setStatus("ACTIVE");
            clearSchedule(waterbus);
            return 0;
        }

        if (tripRepository.countStartedTripsOverlappingOutage(waterbusId, startsAt, now) > 0) {
            throw new IllegalArgumentException("Tàu đang có chuyến chạy qua giờ bắt đầu. Hãy chọn giờ sau khi chuyến kết thúc.");
        }
        List<Integer> currentTripIds = tripService.getTripsAffectedByOutage(waterbusId, startsAt, endsAt)
                .stream().map(preview -> preview.trip().id()).toList();
        if (previewedTripIds == null || previewedTripIds.stream().anyMatch(id -> id == null || id <= 0)
                || previewedTripIds.size() != currentTripIds.size()
                || !Set.copyOf(previewedTripIds).equals(Set.copyOf(currentTripIds))) {
            throw new IllegalArgumentException("Danh sách chuyến đã thay đổi. Vui lòng xem trước và xác nhận lại.");
        }
        String reason = "MAINTENANCE".equals(status)
                ? "Tàu " + waterbus.getCode() + " bảo trì từ " + format(startsAt) + " đến " + format(endsAt)
                : "Tàu " + waterbus.getCode() + " ngưng hoạt động từ " + format(startsAt);
        if (!currentTripIds.isEmpty()) {
            tripService.cancelTrips(currentTripIds, reason);
        }
        waterbus.setStatusStartsAt(startsAt);
        waterbus.setStatusEndsAt(endsAt);
        waterbus.setPendingStatus(status);
        return currentTripIds.size();
    }

    @Override
    @Transactional
    public void applyScheduledStatuses() {
        LocalDateTime now = LocalDateTime.now();
        for (Waterbus candidate : waterbusRepository.findByPendingStatusIsNotNullOrStatusEndsAtIsNotNull()) {
            Waterbus waterbus = waterbusRepository.findByIdForUpdate(candidate.getId()).orElse(null);
            if (waterbus == null) continue;
            if ("MAINTENANCE".equals(waterbus.getStatus()) && waterbus.getStatusEndsAt() != null
                    && !waterbus.getStatusEndsAt().isAfter(now)) {
                waterbus.setStatus("ACTIVE");
                clearSchedule(waterbus);
            } else if (waterbus.getPendingStatus() != null && waterbus.getStatusStartsAt() != null
                    && !waterbus.getStatusStartsAt().isAfter(now)) {
                String nextStatus = waterbus.getPendingStatus();
                waterbus.setPendingStatus(null);
                if ("MAINTENANCE".equals(nextStatus) && waterbus.getStatusEndsAt() != null
                        && !waterbus.getStatusEndsAt().isAfter(now)) {
                    waterbus.setStatus("ACTIVE");
                    clearSchedule(waterbus);
                } else {
                    waterbus.setStatus(nextStatus);
                }
            }
        }
    }

    private void validateStatusChange(Waterbus waterbus, String status, LocalDateTime startsAt,
                                      LocalDateTime endsAt, LocalDateTime now) {
        if (status == null || !Set.of("ACTIVE", "MAINTENANCE", "INACTIVE").contains(status)) {
            throw new IllegalArgumentException("Trạng thái tàu không hợp lệ.");
        }
        if ("ACTIVE".equals(status)) {
            if ("ACTIVE".equals(waterbus.getStatus()) && waterbus.getPendingStatus() == null) {
                throw new IllegalArgumentException("Tàu đã ở trạng thái hoạt động.");
            }
            return;
        }
        if (!"ACTIVE".equals(waterbus.getStatus()) || waterbus.getPendingStatus() != null) {
            throw new IllegalArgumentException("Tàu đã có lịch ngưng hoạt động hoặc không còn hoạt động.");
        }
        if (startsAt == null || !startsAt.isAfter(now)) {
            throw new IllegalArgumentException("Giờ bắt đầu phải ở trong tương lai.");
        }
        if ("MAINTENANCE".equals(status) && (endsAt == null || !endsAt.isAfter(startsAt))) {
            throw new IllegalArgumentException("Giờ hoạt động lại phải sau giờ bắt đầu bảo trì.");
        }
        if ("INACTIVE".equals(status) && endsAt != null) {
            throw new IllegalArgumentException("Ngưng hoạt động không có giờ kết thúc.");
        }
    }

    private void clearSchedule(Waterbus waterbus) {
        waterbus.setPendingStatus(null);
        waterbus.setStatusStartsAt(null);
        waterbus.setStatusEndsAt(null);
    }

    private String format(LocalDateTime value) {
        return value.format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy"));
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
