package com.hsf302.waterbus.job;

import com.hsf302.waterbus.entity.Booking;
import com.hsf302.waterbus.repository.BookingRepository;
import com.hsf302.waterbus.service.BookingService;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@EnableScheduling
public class BookingExpiryJob {

    private final BookingRepository bookingRepository;
    private final BookingService bookingService;

    public BookingExpiryJob(BookingRepository bookingRepository, BookingService bookingService) {
        this.bookingRepository = bookingRepository;
        this.bookingService = bookingService;
    }

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void expireOverdueBookings() {
        List<Booking> overdueBookings = bookingRepository.findByStatusAndExpiresAtBefore("PENDING", LocalDateTime.now());

        for (Booking booking : overdueBookings) {
            booking.setStatus("EXPIRED");
            bookingRepository.save(booking);
            bookingService.releaseHoldTickets(booking.getId());
        }
    }
}
