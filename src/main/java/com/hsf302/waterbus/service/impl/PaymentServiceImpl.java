package com.hsf302.waterbus.service.impl;

import com.hsf302.waterbus.entity.Payment;
import com.hsf302.waterbus.service.PaymentService;
import java.util.List;
import org.springframework.stereotype.Service;

// Module: BE4 - Đặt vé & Thanh toán. TODO: thay các dòng throw bằng code thật.
@Service
public class PaymentServiceImpl implements PaymentService {

    @Override
    public Payment payMock(int bookingId) {
        throw new UnsupportedOperationException("TODO BE4");
    }
}
