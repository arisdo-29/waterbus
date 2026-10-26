package com.hsf302.waterbus.service.impl;

import com.hsf302.waterbus.entity.Waterbus;
import com.hsf302.waterbus.entity.Seat;
import com.hsf302.waterbus.dto.AdminWaterbusForm;
import com.hsf302.waterbus.repository.WaterbusRepository;
import com.hsf302.waterbus.repository.SeatRepository;
import com.hsf302.waterbus.service.WaterbusService;
import java.util.List;
import java.util.Set;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

// Module: BE2 - Tàu, Ghế & Chuyến.
@Service
public class WaterbusServiceImpl implements WaterbusService {
    private final WaterbusRepository waterbusRepository;
    private final SeatRepository seatRepository;

    public WaterbusServiceImpl(WaterbusRepository waterbusRepository, SeatRepository seatRepository) {
        this.waterbusRepository = waterbusRepository;
        this.seatRepository = seatRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Waterbus> getWaterbuses() {
        return waterbusRepository.findAllByOrderByIdAsc();
    }

    @Override
    @Transactional(readOnly = true)
    public AdminWaterbusForm getWaterbusForEdit(int waterbusId) {
        Waterbus waterbus = waterbusRepository.findById(waterbusId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tàu."));
        AdminWaterbusForm form = new AdminWaterbusForm();
        form.setId(waterbus.getId());
        form.setCode(waterbus.getCode());
        form.setName(waterbus.getName());
        form.setCapacity(waterbus.getCapacity());
        form.setStatus(waterbus.getStatus());
        return form;
    }

    @Override
    @Transactional
    public void saveWaterbus(AdminWaterbusForm form) {
        String code = form.getCode().trim();
        String status = form.getStatus();
        if (!Set.of("ACTIVE", "MAINTENANCE", "INACTIVE").contains(status)) {
            throw new IllegalArgumentException("Trạng thái tàu không hợp lệ.");
        }
        waterbusRepository.findFirstByCodeIgnoreCase(code).ifPresent(existing -> {
            if (!existing.getId().equals(form.getId())) {
                throw new IllegalArgumentException("Mã tàu đã được sử dụng.");
            }
        });
        Waterbus waterbus = form.getId() == null ? new Waterbus() : waterbusRepository.findById(form.getId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tàu để cập nhật."));
        List<Seat> seats = form.getId() == null
                ? List.of() : seatRepository.findByWaterbusIdOrderBySeatRowAscSeatColAsc(form.getId());
        if (form.getCapacity() < seats.size()) {
            throw new IllegalArgumentException("Sức chứa không thể nhỏ hơn số ghế đã được tạo.");
        }
        waterbus.setCode(code);
        waterbus.setName(form.getName().trim());
        waterbus.setCapacity(form.getCapacity());
        waterbus.setStatus(status);
        waterbus = waterbusRepository.save(waterbus);
        if (form.getCapacity() > seats.size()) {
            java.util.ArrayList<Seat> newSeats = new java.util.ArrayList<>();
            for (int index = seats.size() + 1; index <= form.getCapacity(); index++) {
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
    }

    @Override
    @Transactional
    public void changeStatus(int waterbusId, String status) {
        if (!Set.of("ACTIVE", "MAINTENANCE", "INACTIVE").contains(status)) {
            throw new IllegalArgumentException("Trạng thái tàu không hợp lệ.");
        }
        Waterbus waterbus = waterbusRepository.findById(waterbusId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tàu."));
        waterbus.setStatus(status);
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
