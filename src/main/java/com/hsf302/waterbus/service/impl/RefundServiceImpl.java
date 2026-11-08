package com.hsf302.waterbus.service.impl;

import com.hsf302.waterbus.dto.RefundAdminDTO;
import com.hsf302.waterbus.entity.Booking;
import com.hsf302.waterbus.entity.Refund;
import com.hsf302.waterbus.entity.Ticket;
import com.hsf302.waterbus.repository.BookingRepository;
import com.hsf302.waterbus.repository.RefundRepository;
import com.hsf302.waterbus.repository.TicketRepository;
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

    public RefundServiceImpl(RefundRepository refundRepository, BookingRepository bookingRepository,
                             TicketRepository ticketRepository) {
        this.refundRepository = refundRepository;
        this.bookingRepository = bookingRepository;
        this.ticketRepository = ticketRepository;
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
                    }
                } else {
                    booking.setStatus("CANCELLED");
                    bookingRepository.save(booking);
                }
            }
        }
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
        return refunds.stream().map(refund -> new RefundAdminDTO(refund.getId(),
                refund.getBooking().getBookingCode(), refund.getAmount(), refund.getStatus(),
                refund.getReason(), refund.getTransactionRef(), refund.getRefundedAt())).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Refund getRefundOfBooking(int bookingId) {
        return refundRepository.findByBookingId(bookingId).orElse(null);
    }

    @Override
    @Transactional
    public void markRefunded(int id, String transactionRef, LocalDateTime refundedAt, String note) {
        Refund refund = refundRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khoản hoàn tiền."));
        if (!"PENDING".equals(refund.getStatus())) {
            throw new IllegalArgumentException("Khoản này đã được ghi nhận hoàn tiền.");
        }
        String reference = transactionRef == null ? "" : transactionRef.trim();
        if (reference.isEmpty() || reference.length() > 100) {
            throw new IllegalArgumentException("Mã giao dịch hoàn phải có từ 1 đến 100 ký tự.");
        }
        if (refundedAt == null || refundedAt.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Thời điểm hoàn tiền không hợp lệ.");
        }
        if (note != null && note.length() > 255) {
            throw new IllegalArgumentException("Ghi chú tối đa 255 ký tự.");
        }
        refund.setTransactionRef(reference);
        refund.setRefundedAt(refundedAt);
        refund.setNote(note == null ? null : note.trim());
        refund.setStatus("DONE");
        refund.getBooking().setStatus("REFUNDED");
    }
}
