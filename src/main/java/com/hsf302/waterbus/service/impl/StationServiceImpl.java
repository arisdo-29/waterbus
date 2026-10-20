package com.hsf302.waterbus.service.impl;

import com.hsf302.waterbus.dto.StationDTO;
import com.hsf302.waterbus.entity.Station;
import com.hsf302.waterbus.repository.StationRepository;
import com.hsf302.waterbus.service.StationService;
import org.springframework.stereotype.Service;

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

    private StationDTO toDTO(Station s) {
        return new StationDTO(s.getId(), s.getCode(), s.getName(), s.getAddress(), s.getIsActive());
    }
}
