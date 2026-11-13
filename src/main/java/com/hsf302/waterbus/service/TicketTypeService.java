package com.hsf302.waterbus.service;

import com.hsf302.waterbus.dto.TicketTypeDTO;
import com.hsf302.waterbus.entity.TicketType;
import java.math.BigDecimal;
import java.util.List;

public interface TicketTypeService {
    List<TicketType> getActiveTicketTypes();

    List<TicketTypeDTO> getActiveTicketTypeDTOs();

    BigDecimal getPrice(int ticketTypeId);

    List<TicketTypeDTO> getAllTicketTypes();

    TicketTypeDTO getTicketTypeById(int id);

    void saveTicketType(TicketTypeDTO dto);

    void deleteTicketType(int id);
}
