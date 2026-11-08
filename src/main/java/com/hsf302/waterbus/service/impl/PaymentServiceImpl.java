package com.hsf302.waterbus.service.impl;

import com.hsf302.waterbus.dto.PaymentInfoDTO;
import com.hsf302.waterbus.entity.Booking;
import com.hsf302.waterbus.entity.Payment;
import com.hsf302.waterbus.entity.Ticket;
import com.hsf302.waterbus.repository.BookingRepository;
import com.hsf302.waterbus.repository.PaymentRepository;
import com.hsf302.waterbus.repository.TicketRepository;
import com.hsf302.waterbus.service.PaymentService;
import com.hsf302.waterbus.service.PromotionService;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Module: BE4 - Đặt vé & Thanh toán.
@Service
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final TicketRepository ticketRepository;
    private final PromotionService promotionService; // module Tân: tăng lượt dùng mã KM

    // Constructor injection: Spring tự đưa các repository/service vào
    public PaymentServiceImpl(PaymentRepository paymentRepository, BookingRepository bookingRepository,
                              TicketRepository ticketRepository, PromotionService promotionService) {
        this.paymentRepository = paymentRepository;
        this.bookingRepository = bookingRepository;
        this.ticketRepository = ticketRepository;
        this.promotionService = promotionService;
    }

    @Override
    @Transactional // ghi 4 bảng (payment, booking, ticket, promotion): lỗi giữa chừng thì huỷ hết
    public Payment payMock(int bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn đặt vé."));

        // Chỉ thanh toán đơn đang chờ (PENDING) và còn trong 10 phút giữ chỗ
        if (!"PENDING".equals(booking.getStatus())) {
            throw new IllegalArgumentException("Đơn này không còn chờ thanh toán.");
        }
        LocalDateTime now = LocalDateTime.now();
        if (booking.getExpiresAt().isBefore(now)) {
            throw new IllegalArgumentException("Đơn đã hết thời gian giữ chỗ. Vui lòng đặt vé lại.");
        }

        // Chuyến phải còn SCHEDULED (admin có thể đã huỷ chuyến trong lúc khách chưa trả tiền).
        // Mọi vé của 1 đơn đều cùng 1 chuyến, nên chỉ cần xem chuyến của vé đầu tiên.
        List<Ticket> tickets = ticketRepository.findByBookingIdOrderByIdAsc(bookingId);
        if (tickets.isEmpty() || !"SCHEDULED".equals(tickets.get(0).getTrip().getStatus())) {
            throw new IllegalArgumentException("Chuyến tàu đã bị huỷ hoặc không còn nhận thanh toán.");
        }

        // 1. Tạo payment SUCCESS (thanh toán giả lập, không gọi cổng thanh toán thật)
        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setMethod("MOCK");
        payment.setAmount(booking.getTotalAmount());
        payment.setStatus("SUCCESS");
        payment.setTransactionRef("MOCK-" + booking.getBookingCode());
        payment.setCreatedAt(now);
        payment.setPaidAt(now);
        paymentRepository.save(payment);

        // 2. Đơn -> PAID, ghi thời điểm thanh toán
        booking.setStatus("PAID");
        booking.setPaidAt(now);
        bookingRepository.save(booking);

        // 3. Vé đang giữ chỗ (HOLD) -> vé dùng được (VALID). Dùng lại danh sách vé đã lấy ở trên
        for (Ticket ticket : tickets) {
            if ("HOLD".equals(ticket.getStatus())) {
                ticket.setStatus("VALID");
                ticketRepository.save(ticket);
            }
        }

        // 4. Đơn có dùng mã khuyến mãi -> tăng used_count lên 1
        if (booking.getPromotion() != null) {
            promotionService.increaseUsedCount(booking.getPromotion().getCode());
        }
        return payment;
    }

    @Override
    public PaymentInfoDTO getPaymentOfBooking(int bookingId) {
        // Có payment -> đổi sang DTO nhỏ (chỉ method + transactionRef). Không có -> null (đơn chưa trả / đơn 0 đ)
        return paymentRepository.findByBookingId(bookingId)
                .map(payment -> new PaymentInfoDTO(payment.getMethod(), payment.getTransactionRef()))
                .orElse(null);
    }
}
