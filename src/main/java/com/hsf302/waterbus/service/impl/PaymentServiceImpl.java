package com.hsf302.waterbus.service.impl;

import com.hsf302.waterbus.dto.PaymentInfoDTO;
import com.hsf302.waterbus.entity.Booking;
import com.hsf302.waterbus.entity.Payment;
import com.hsf302.waterbus.entity.Promotion;
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

@Service
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final TicketRepository ticketRepository;
    private final PromotionService promotionService;

    public PaymentServiceImpl(PaymentRepository paymentRepository, BookingRepository bookingRepository,
                              TicketRepository ticketRepository, PromotionService promotionService) {
        this.paymentRepository = paymentRepository;
        this.bookingRepository = bookingRepository;
        this.ticketRepository = ticketRepository;
        this.promotionService = promotionService;
    }

    @Override
    @Transactional(readOnly = true)
    public String getCannotPayReason(int bookingId) {
        Booking booking = bookingRepository.findById(bookingId).orElse(null);
        if (booking == null) {
            return "Không tìm thấy đơn đặt vé.";
        }
        if (!"PENDING".equals(booking.getStatus())) {
            return "Đơn này không còn chờ thanh toán.";
        }
        if (booking.getExpiresAt().isBefore(LocalDateTime.now())) {
            return "Đơn đã hết thời gian giữ chỗ. Vui lòng đặt vé lại.";
        }
        List<Ticket> tickets = ticketRepository.findByBookingIdOrderByIdAsc(bookingId);
        String tripStatus = null;
        if (!tickets.isEmpty()) {
            tripStatus = tickets.get(0).getTrip().getStatus();
        }
        if (!"SCHEDULED".equals(tripStatus) && !"DEPARTED".equals(tripStatus)) {
            return "Chuyến tàu đã bị huỷ hoặc không còn nhận thanh toán.";
        }
        Promotion promotion = booking.getPromotion();
        if (isPromotionUsedUp(promotion)) {
            return "Mã khuyến mãi " + promotion.getCode() + " đã hết lượt sử dụng. Vui lòng đặt vé lại không dùng mã này.";
        }
        return null;
    }

    private boolean isPromotionUsedUp(Promotion promotion) {
        if (promotion == null || promotion.getUsageLimit() == null) {
            return false;
        }
        int usedCount = 0;
        if (promotion.getUsedCount() != null) {
            usedCount = promotion.getUsedCount();
        }
        return usedCount >= promotion.getUsageLimit();
    }

    @Override
    @Transactional
    public Payment confirmPaid(int bookingId, String method, String transactionRef) {
        String reason = getCannotPayReason(bookingId);
        if (reason != null) {
            throw new IllegalArgumentException(reason);
        }
        Booking booking = bookingRepository.findById(bookingId).orElseThrow();
        LocalDateTime now = LocalDateTime.now();

        Payment payment = paymentRepository.findByBookingId(bookingId).orElse(null);
        if (payment == null) {
            payment = new Payment();
            payment.setBooking(booking);
            payment.setCreatedAt(now);
        }
        payment.setMethod(method);
        payment.setAmount(booking.getTotalAmount());
        payment.setStatus("SUCCESS");
        payment.setTransactionRef(transactionRef);
        payment.setPaidAt(now);
        paymentRepository.save(payment);

        booking.setStatus("PAID");
        booking.setPaidAt(now);
        bookingRepository.save(booking);

        for (Ticket ticket : ticketRepository.findByBookingIdOrderByIdAsc(bookingId)) {
            if ("HOLD".equals(ticket.getStatus())) {
                ticket.setStatus("VALID");
                ticketRepository.save(ticket);
            }
        }

        if (booking.getPromotion() != null) {
            promotionService.increaseUsedCount(booking.getPromotion().getCode());
        }
        return payment;
    }

    @Override
    @Transactional
    public void confirmCashPayment(int bookingId, String loginRole) {
        if (!"STAFF".equalsIgnoreCase(loginRole)) {
            throw new IllegalArgumentException("Chỉ nhân viên tại bến mới được xác nhận thu tiền mặt.");
        }
        confirmPaid(bookingId, "CASH", null);
    }

    @Override
    public PaymentInfoDTO getPaymentOfBooking(int bookingId) {
        return paymentRepository.findByBookingId(bookingId)
                .map(PaymentInfoDTO::fromEntity)
                .orElse(null);
    }
}
