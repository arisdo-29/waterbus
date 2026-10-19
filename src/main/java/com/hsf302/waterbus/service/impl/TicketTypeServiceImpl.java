package com.hsf302.waterbus.service.impl;

import com.hsf302.waterbus.dto.TicketTypeDTO;
import com.hsf302.waterbus.entity.TicketType;
import com.hsf302.waterbus.repository.TicketTypeRepository;
import com.hsf302.waterbus.service.TicketTypeService;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Module: BE3 - Tài khoản, Loại vé & Khuyến mãi.
@Service
public class TicketTypeServiceImpl implements TicketTypeService {

    private final TicketTypeRepository ticketTypeRepository;

    public TicketTypeServiceImpl(TicketTypeRepository ticketTypeRepository) {
        this.ticketTypeRepository = ticketTypeRepository;
    }

    @Override
    public List<TicketType> getActiveTicketTypes() {
        return ticketTypeRepository.findByIsActiveTrue();
    }

    @Override
    public BigDecimal getPrice(int ticketTypeId) {
        return ticketTypeRepository.findById(ticketTypeId)
                .map(TicketType::getPrice)
                .orElse(BigDecimal.ZERO);
    }

    @Override
    public List<TicketTypeDTO> getAllTicketTypes() {
        return ticketTypeRepository.findAll().stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    public TicketTypeDTO getTicketTypeById(int id) {
        return ticketTypeRepository.findById(id)
                .map(this::toDTO)
                .orElse(null);
    }

    @Override
    @Transactional
    public void saveTicketType(TicketTypeDTO dto) {
        if (dto == null) {
            return;
        }

        TicketType ticketType;
        if (dto.getId() != null) {
            ticketType = ticketTypeRepository.findById(dto.getId())
                    .orElse(new TicketType());
        } else {
            ticketType = new TicketType();
            ticketType.setCode(dto.getCode());
        }

        ticketType.setName(dto.getName());
        ticketType.setPrice(dto.getPrice());
        ticketType.setRequiresVerification(dto.getRequiresVerification() != null ? dto.getRequiresVerification() : false);
        ticketType.setDescription(dto.getDescription());
        ticketType.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);

        ticketTypeRepository.save(ticketType);
    }

    @Override
    @Transactional
    public void deleteTicketType(int id) {
        ticketTypeRepository.findById(id).ifPresent(tt -> {
            tt.setIsActive(false);
            ticketTypeRepository.save(tt);
        });
    }

    private TicketTypeDTO toDTO(TicketType tt) {
        return new TicketTypeDTO(
                tt.getId(),
                tt.getCode(),
                tt.getName(),
                tt.getPrice(),
                tt.getRequiresVerification(),
                tt.getDescription(),
                tt.getIsActive()
        );
    }
}
