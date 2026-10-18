package com.hsf302.waterbus.service.impl;

import com.hsf302.waterbus.entity.Booking;
import com.hsf302.waterbus.service.BookingService;
import java.util.List;
import org.springframework.stereotype.Service;

// Module: BE4 - Đặt vé & Thanh toán. TODO: thay các dòng throw bằng code thật.
@Service
public class BookingServiceImpl implements BookingService {

    @Override
    public Booking createBooking(int tripId, List<Integer> seatIds, int ticketTypeId, int fromStationId, int toStationId, String promoCode, Integer accountId, String contactName, String contactPhone, String contactEmail) {
        throw new UnsupportedOperationException("TODO BE4");
    }

    @Override
    public Booking getBookingByCode(String bookingCode) {
        throw new UnsupportedOperationException("TODO BE4");
    }

    @Override
    public List<Booking> getBookingsOfAccount(int accountId) {
        throw new UnsupportedOperationException("TODO BE4");
    }
}
