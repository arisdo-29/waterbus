package com.hsf302.waterbus.service.impl;

import com.hsf302.waterbus.dto.AccountDTO;
import com.hsf302.waterbus.dto.RefundAdminDTO;
import com.hsf302.waterbus.dto.RefundViewDTO;
import com.hsf302.waterbus.entity.Account;
import com.hsf302.waterbus.entity.Booking;
import com.hsf302.waterbus.entity.Refund;
import com.hsf302.waterbus.entity.Ticket;
import com.hsf302.waterbus.repository.BookingRepository;
import com.hsf302.waterbus.repository.RefundRepository;
import com.hsf302.waterbus.repository.TicketRepository;
import com.hsf302.waterbus.service.AccountService;
import com.hsf302.waterbus.service.PromotionService;
import com.hsf302.waterbus.service.RefundService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefundServiceImpl implements RefundService {
    private final RefundRepository refundRepository;
    private final BookingRepository bookingRepository;
    private final TicketRepository ticketRepository;
    private final AccountService accountService;
    private final PromotionService promotionService;

    public RefundServiceImpl(RefundRepository refundRepository, BookingRepository bookingRepository,
                             TicketRepository ticketRepository, AccountService accountService,
                             PromotionService promotionService) {
        this.refundRepository = refundRepository;
        this.bookingRepository = bookingRepository;
        this.ticketRepository = ticketRepository;
        this.accountService = accountService;
        this.promotionService = promotionService;
    }

    @Override
    @Transactional
    public void createForCancelledTrip(int tripId, String reason) {
        Map<Integer, List<Ticket>> ticketsByBooking = new LinkedHashMap<>();
        for (Ticket ticket : ticketRepository.findByTripIdOrderByIdAsc(tripId)) {
            ticketsByBooking.computeIfAbsent(ticket.getBooking().getId(), ignored -> new java.util.ArrayList<>())
                    .add(ticket);
        }
        for (List<Ticket> tickets : ticketsByBooking.values()) {
            Booking booking = tickets.get(0).getBooking();
            if ("PENDING".equals(booking.getStatus())) {
                booking.setStatus("CANCELLED");
                cancelTickets(tickets);
                bookingRepository.save(booking);
            } else if ("PAID".equals(booking.getStatus())) {
                cancelTickets(tickets);
                if (booking.getTotalAmount().compareTo(BigDecimal.ZERO) > 0) {
                    if (!refundRepository.existsByBookingId(booking.getId())) {
                        Refund refund = new Refund();
                        refund.setBooking(booking);
                        refund.setAmount(booking.getTotalAmount());
                        refund.setReason(reason);
                        refundRepository.save(refund);
                        releasePromotionUse(booking);
                    }
                } else {
                    releasePromotionUse(booking);
                    booking.setStatus("CANCELLED");
                    bookingRepository.save(booking);
                }
            }
        }
    }

    private void releasePromotionUse(Booking booking) {
        if (booking.getPromotion() != null) {
            promotionService.decreaseUsedCount(booking.getPromotion().getCode());
        }
    }

    @Override
    @Transactional
    public void createForInvalidPayment(int bookingId, String reason) {
        if (refundRepository.existsByBookingId(bookingId)) {
            return;
        }
        if (reason == null || reason.isBlank() || reason.length() > 255) {
            throw new IllegalArgumentException("Lý do hoàn tiền phải có từ 1 đến 255 ký tự.");
        }
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn đặt vé."));
        Refund refund = new Refund();
        refund.setBooking(booking);
        refund.setAmount(booking.getTotalAmount());
        refund.setReason(reason.trim());
        refundRepository.save(refund);
    }

    private void cancelTickets(List<Ticket> tickets) {
        for (Ticket ticket : tickets) {
            if ("HOLD".equals(ticket.getStatus()) || "VALID".equals(ticket.getStatus())
                    || "USED".equals(ticket.getStatus())) {
                ticket.setStatus("CANCELLED");
                ticketRepository.save(ticket);
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<RefundAdminDTO> getRefunds(String status) {
        if (status != null && !status.isBlank() && !"PENDING".equals(status) && !"DONE".equals(status)) {
            throw new IllegalArgumentException("Trạng thái hoàn tiền không hợp lệ.");
        }
        List<Refund> refunds = status == null || status.isBlank()
                ? refundRepository.findAllByOrderByCreatedAtDesc()
                : refundRepository.findByStatusOrderByCreatedAtDesc(status);
        return refunds.stream().map(RefundAdminDTO::fromEntity).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Refund getRefundOfBooking(int bookingId) {
        return refundRepository.findByBookingId(bookingId).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public RefundViewDTO getRefundViewOfBooking(int bookingId) {
        return refundRepository.findByBookingId(bookingId).map(RefundViewDTO::fromEntity).orElse(null);
    }

    @Override
    @Transactional
    public void markRefunded(int id, AccountDTO admin) {
        if (admin == null || admin.getId() == null || !"ADMIN".equals(admin.getRole())) {
            throw new IllegalArgumentException("Chỉ quản trị viên được xác nhận hoàn tiền.");
        }
        Account processor = accountService.getAccountEntityById(admin.getId());
        if (processor == null || !"ADMIN".equals(processor.getRole())
                || !"ACTIVE".equals(processor.getStatus())) {
            throw new IllegalArgumentException("Tài khoản quản trị không còn hợp lệ.");
        }
        Refund refund = refundRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khoản hoàn tiền."));
        if (!"PENDING".equals(refund.getStatus())) {
            throw new IllegalArgumentException("Khoản này đã được ghi nhận hoàn tiền.");
        }
        refund.setRefundedAt(LocalDateTime.now());
        refund.setProcessedBy(processor);
        refund.setStatus("DONE");
        refund.getBooking().setStatus("REFUNDED");
    }
}
