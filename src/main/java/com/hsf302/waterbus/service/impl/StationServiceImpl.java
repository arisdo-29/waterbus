package com.hsf302.waterbus.service.impl;

import com.hsf302.waterbus.dto.StationDTO;
import com.hsf302.waterbus.dto.StationForm;
import com.hsf302.waterbus.entity.Station;
import com.hsf302.waterbus.repository.StationRepository;
import com.hsf302.waterbus.service.StationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/*
 * VÍ DỤ MẪU ĐẦY ĐỦ 1 LUỒNG: Entity -> Repository -> Service -> DTO -> Controller -> View.
 * Các bạn copy cách làm này cho module của mình.
 *
 * Lưu ý khác với project mẫu của giảng viên: ArtifactServiceImpl trong đó THIẾU @Service,
 * nên Spring không tạo bean và Controller không inject được. Ở đây nhớ luôn có @Service.
 */
@Service
public class StationServiceImpl implements StationService {

    private final StationRepository stationRepository;

    // Constructor injection (khuyên dùng thay cho @Autowired trên field)
    public StationServiceImpl(StationRepository stationRepository) {
        this.stationRepository = stationRepository;
    }

    @Override
    public List<StationDTO> getStations() {
        return stationRepository.findAll().stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    public List<StationDTO> getActiveStations() {
        return stationRepository.findByIsActiveTrueOrderByIdAsc().stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    public StationDTO getStationById(int stationId) {
        return stationRepository.findById(stationId)
                .map(this::toDTO)
                .orElse(null);
    }

    @Override
    public StationForm getStationForEdit(int stationId) {
        Station station = requireStation(stationId);
        StationForm form = new StationForm();
        form.setId(station.getId());
        form.setCode(station.getCode());
        form.setName(station.getName());
        form.setAddress(station.getAddress());
        form.setIsActive(station.getIsActive());
        return form;
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
        station.setCode(form.getCode());
        station.setName(form.getName());
        station.setAddress(form.getAddress());
        station.setIsActive(form.getIsActive());
        return stationRepository.saveAndFlush(station).getId();
    }

    @Override
    @Transactional
    public void hideStation(int stationId) {
        Station station = requireStation(stationId);
        station.setIsActive(false);
        stationRepository.flush();
    }

    private Station requireStation(int stationId) {
        return stationRepository.findById(stationId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bến cần sửa hoặc ẩn."));
    }

    private StationDTO toDTO(Station s) {
        return new StationDTO(s.getId(), s.getCode(), s.getName(), s.getAddress(), s.getIsActive());
    }
}
