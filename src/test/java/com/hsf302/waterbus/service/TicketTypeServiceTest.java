package com.hsf302.waterbus.service;

import com.hsf302.waterbus.dto.TicketTypeDTO;
import com.hsf302.waterbus.entity.TicketType;
import com.hsf302.waterbus.repository.TicketTypeRepository;
import com.hsf302.waterbus.service.impl.TicketTypeServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TicketTypeServiceTest {

    private TicketTypeRepository ticketTypeRepository;
    private TicketTypeService ticketTypeService;

    @BeforeEach
    void setUp() {
        ticketTypeRepository = Mockito.mock(TicketTypeRepository.class);
        ticketTypeService = new TicketTypeServiceImpl(ticketTypeRepository);
    }

    @Test
    void testGetPrice_Found() {
        TicketType tt = new TicketType();
        tt.setId(1);
        tt.setPrice(BigDecimal.valueOf(15000));
        when(ticketTypeRepository.findById(1)).thenReturn(Optional.of(tt));

        BigDecimal price = ticketTypeService.getPrice(1);
        assertEquals(BigDecimal.valueOf(15000), price);
    }

    @Test
    void testGetPrice_NotFound_ReturnsZero() {
        when(ticketTypeRepository.findById(999)).thenReturn(Optional.empty());

        BigDecimal price = ticketTypeService.getPrice(999);
        assertEquals(BigDecimal.ZERO, price);
    }

    @Test
    void testGetActiveTicketTypes() {
        TicketType tt1 = new TicketType();
        tt1.setId(1);
        tt1.setIsActive(true);

        when(ticketTypeRepository.findByIsActiveTrue()).thenReturn(List.of(tt1));

        List<TicketType> activeList = ticketTypeService.getActiveTicketTypes();
        assertEquals(1, activeList.size());
    }

    @Test
    void testSaveTicketType_CreateNew() {
        TicketTypeDTO dto = new TicketTypeDTO(null, "VIP", "Vé VIP", BigDecimal.valueOf(30000), false, "Mô tả", true);

        ticketTypeService.saveTicketType(dto);
        verify(ticketTypeRepository, times(1)).save(any(TicketType.class));
    }

    @Test
    void testDeleteTicketType_SoftDelete() {
        TicketType tt = new TicketType();
        tt.setId(2);
        tt.setIsActive(true);

        when(ticketTypeRepository.findById(2)).thenReturn(Optional.of(tt));

        ticketTypeService.deleteTicketType(2);

        assertFalse(tt.getIsActive());
        verify(ticketTypeRepository, times(1)).save(tt);
    }
}
