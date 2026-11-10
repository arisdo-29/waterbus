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
    @Transactional
    public Payment payMock(int bookingId) {
        // Thanh toán giả lập: không gọi cổng thật, mã giao dịch tự đặt là MOCK-<mã đơn>
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn đặt vé."));
        return confirmPaid(bookingId, "MOCK", "MOCK-" + booking.getBookingCode());
    }

    @Override
    @Transactional(readOnly = true) // chỉ đọc; vẫn cần transaction để đọc được chuyến (quan hệ LAZY) của vé
    public String getCannotPayReason(int bookingId) {
        Booking booking = bookingRepository.findById(bookingId).orElse(null);
        if (booking == null) {
            return "Không tìm thấy đơn đặt vé.";
        }
        // Chỉ thanh toán đơn đang chờ (PENDING) và còn trong 10 phút giữ chỗ
        if (!"PENDING".equals(booking.getStatus())) {
            return "Đơn này không còn chờ thanh toán.";
        }
        if (booking.getExpiresAt().isBefore(LocalDateTime.now())) {
            return "Đơn đã hết thời gian giữ chỗ. Vui lòng đặt vé lại.";
        }
        // Chuyến phải còn SCHEDULED (admin có thể đã huỷ chuyến trong lúc khách chưa trả tiền).
        // Mọi vé của 1 đơn đều cùng 1 chuyến, nên chỉ cần xem chuyến của vé đầu tiên.
        List<Ticket> tickets = ticketRepository.findByBookingIdOrderByIdAsc(bookingId);
        if (tickets.isEmpty() || !"SCHEDULED".equals(tickets.get(0).getTrip().getStatus())) {
            return "Chuyến tàu đã bị huỷ hoặc không còn nhận thanh toán.";
        }
        // Mã KM chỉ được tính lượt lúc trả tiền, nên trong lúc đơn này chờ, khách khác có thể đã dùng lượt cuối.
        // Phải chặn ở đây: nếu để confirmPaid tăng used_count vượt usage_limit thì SQL Server từ chối
        // (ràng buộc CK_promo_usage) và khách thấy trang lỗi 500 thay vì một lời báo dễ hiểu.
        Promotion promotion = booking.getPromotion();
        if (isPromotionUsedUp(promotion)) {
            return "Mã khuyến mãi " + promotion.getCode() + " đã hết lượt sử dụng. Vui lòng đặt vé lại không dùng mã này.";
        }
        return null; // không có lý do nào để chặn -> thanh toán được
    }

    // Mã KM đã dùng hết số lượt cho phép chưa. usage_limit = NULL nghĩa là mã không giới hạn lượt.
    private boolean isPromotionUsedUp(Promotion promotion) {
        if (promotion == null || promotion.getUsageLimit() == null) {
            return false; // đơn không dùng mã, hoặc mã không giới hạn lượt
        }
        int usedCount = 0;
        if (promotion.getUsedCount() != null) {
            usedCount = promotion.getUsedCount();
        }
        return usedCount >= promotion.getUsageLimit();
    }

    @Override
    @Transactional // ghi 4 bảng (payment, booking, ticket, promotion): lỗi giữa chừng thì huỷ hết
    public Payment confirmPaid(int bookingId, String method, String transactionRef) {
        String reason = getCannotPayReason(bookingId);
        if (reason != null) {
            throw new IllegalArgumentException(reason);
        }
        Booking booking = bookingRepository.findById(bookingId).orElseThrow(); // đã kiểm tra ở trên nên chắc chắn có
        LocalDateTime now = LocalDateTime.now();

        // 1. Payment SUCCESS. DB chỉ cho mỗi đơn 1 payment -> đã có thì cập nhật, chưa có thì tạo mới
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

        // 2. Đơn -> PAID, ghi thời điểm thanh toán
        booking.setStatus("PAID");
        booking.setPaidAt(now);
        bookingRepository.save(booking);

        // 3. Vé đang giữ chỗ (HOLD) -> vé dùng được (VALID)
        for (Ticket ticket : ticketRepository.findByBookingIdOrderByIdAsc(bookingId)) {
            if ("HOLD".equals(ticket.getStatus())) {
                ticket.setStatus("VALID");
                ticketRepository.save(ticket);
            }
        }

        // 4. Đơn có dùng mã khuyến mãi -> tăng used_count lên 1
        // (getCannotPayReason ở đầu hàm đã chặn mã hết lượt, nên tăng ở đây không vượt usage_limit)
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
