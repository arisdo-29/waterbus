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

// Module: BE4 - Đặt vé & Thanh toán. Việc chạy ngầm: tự huỷ đơn quá 10 phút giữ chỗ để nhả ghế.
@Component
@EnableScheduling // bật @Scheduled cho cả app; đặt ở đây để không phải sửa WaterbusApplication (file chung)
public class BookingExpiryJob {

    private final BookingRepository bookingRepository;
    private final BookingService bookingService;

    // Constructor injection: Spring tự đưa repository/service vào
    public BookingExpiryJob(BookingRepository bookingRepository, BookingService bookingService) {
        this.bookingRepository = bookingRepository;
        this.bookingService = bookingService;
    }

    // Spring tự gọi method này: chạy xong thì đợi 60 giây (60000 ms) rồi chạy lại
    @Scheduled(fixedDelay = 60000)
    @Transactional // đổi nhiều đơn + nhiều vé: lỗi giữa chừng thì huỷ hết lần chạy này
    public void expireOverdueBookings() {
        // 1. Tìm các đơn còn PENDING mà hạn giữ chỗ đã qua (expires_at < bây giờ)
        List<Booking> overdueBookings = bookingRepository.findByStatusAndExpiresAtBefore("PENDING", LocalDateTime.now());

        // 2. Từng đơn: đổi sang EXPIRED rồi nhả ghế (vé HOLD -> CANCELLED)
        for (Booking booking : overdueBookings) {
            booking.setStatus("EXPIRED");
            bookingRepository.save(booking);
            bookingService.releaseHoldTickets(booking.getId());
        }
    }
}
