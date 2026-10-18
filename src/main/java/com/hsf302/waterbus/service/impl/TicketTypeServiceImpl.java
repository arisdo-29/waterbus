package com.hsf302.waterbus.service.impl;

import com.hsf302.waterbus.entity.TicketType;
import com.hsf302.waterbus.service.TicketTypeService;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;

// Module: BE3 - Tài khoản, Loại vé & Khuyến mãi. TODO: thay các dòng throw bằng code thật.
@Service
public class TicketTypeServiceImpl implements TicketTypeService {

    @Override
    public List<TicketType> getActiveTicketTypes() {
        throw new UnsupportedOperationException("TODO BE3");
    }

    @Override
    public BigDecimal getPrice(int ticketTypeId) {
        throw new UnsupportedOperationException("TODO BE3");
    }
}
