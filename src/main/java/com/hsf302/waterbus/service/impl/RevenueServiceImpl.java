package com.hsf302.waterbus.service.impl;

import com.hsf302.waterbus.dto.DailyRevenueDTO;
import com.hsf302.waterbus.dto.RevenueGroupDTO;
import com.hsf302.waterbus.dto.RevenueReportDTO;
import com.hsf302.waterbus.dto.RevenueSummaryDTO;
import com.hsf302.waterbus.entity.Booking;
import com.hsf302.waterbus.entity.Payment;
import com.hsf302.waterbus.entity.Refund;
import com.hsf302.waterbus.entity.Ticket;
import com.hsf302.waterbus.repository.BookingRepository;
import com.hsf302.waterbus.repository.PaymentRepository;
import com.hsf302.waterbus.repository.RefundRepository;
import com.hsf302.waterbus.repository.TicketRepository;
import com.hsf302.waterbus.service.RevenueService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RevenueServiceImpl implements RevenueService {
    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final BookingRepository bookingRepository;
    private final TicketRepository ticketRepository;

    public RevenueServiceImpl(PaymentRepository paymentRepository, RefundRepository refundRepository,
                              BookingRepository bookingRepository, TicketRepository ticketRepository) {
        this.paymentRepository = paymentRepository;
        this.refundRepository = refundRepository;
        this.bookingRepository = bookingRepository;
        this.ticketRepository = ticketRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public RevenueReportDTO getReport(LocalDate from, LocalDate to) {
        if (from == null || to == null || from.isAfter(to)) {
            throw new IllegalArgumentException("Khoảng ngày doanh thu không hợp lệ.");
        }
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.plusDays(1).atStartOfDay();
        Map<LocalDate, Day> days = new LinkedHashMap<>();
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) days.put(date, new Day());
        Map<String, Group> routes = new LinkedHashMap<>();
        Map<String, Group> types = new LinkedHashMap<>();
        Map<String, Group> methods = new LinkedHashMap<>();
        Map<String, Group> promotions = new LinkedHashMap<>();
        BigDecimal gross = BigDecimal.ZERO;
        BigDecimal refunded = BigDecimal.ZERO;
        BigDecimal discount = BigDecimal.ZERO;
        long paidBookings = 0;
        long ticketsSold = 0;
        long freeTickets = 0;
        long cancelledOrExpired = 0;

        for (Payment payment : paymentRepository.findByStatusAndPaidAtGreaterThanEqualAndPaidAtLessThan(
                "SUCCESS", start, end)) {
            if ("MOCK".equalsIgnoreCase(payment.getMethod())) {
                continue; // giao dich thu nghiem, khong tinh vao doanh thu
            }
            Booking booking = payment.getBooking();
            List<Ticket> tickets = ticketRepository.findByBookingIdOrderByIdAsc(booking.getId());
            gross = gross.add(payment.getAmount());
            discount = discount.add(booking.getDiscountAmount());
            paidBookings++;
            ticketsSold += tickets.size();
            Day day = days.get(payment.getPaidAt().toLocalDate());
            day.revenue = day.revenue.add(payment.getAmount());
            day.bookings++;
            addGroups(routes, types, methods, promotions, booking, tickets, payment.getMethod(),
                    payment.getAmount(), 1, tickets.size());
        }

        for (Booking booking : bookingRepository.findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(start, end)) {
            if ("CANCELLED".equals(booking.getStatus()) || "EXPIRED".equals(booking.getStatus())) {
                cancelledOrExpired++;
            }
            if ("PAID".equals(booking.getStatus()) && booking.getTotalAmount().compareTo(BigDecimal.ZERO) == 0) {
                List<Ticket> tickets = ticketRepository.findByBookingIdOrderByIdAsc(booking.getId());
                freeTickets += tickets.size();
                paidBookings++;
                days.get(booking.getCreatedAt().toLocalDate()).bookings++;
                addGroups(routes, types, methods, promotions, booking, tickets, null,
                        BigDecimal.ZERO, 1, tickets.size());
            }
        }

        for (Refund refund : refundRepository.findByStatusAndRefundedAtGreaterThanEqualAndRefundedAtLessThan(
                "DONE", start, end)) {
            Booking booking = refund.getBooking();
            String method = paymentRepository.findByBookingIdAndStatus(booking.getId(), "SUCCESS").stream()
                    .findFirst().map(Payment::getMethod).orElse(null);
            if ("MOCK".equalsIgnoreCase(method)) {
                continue;
            }
            refunded = refunded.add(refund.getAmount());
            days.get(refund.getRefundedAt().toLocalDate()).revenue =
                    days.get(refund.getRefundedAt().toLocalDate()).revenue.subtract(refund.getAmount());
            List<Ticket> tickets = ticketRepository.findByBookingIdOrderByIdAsc(booking.getId());
            addGroups(routes, types, methods, promotions, booking, tickets, method,
                    refund.getAmount().negate(), 0, 0);
        }

        List<DailyRevenueDTO> daily = new ArrayList<>();
        days.forEach((date, day) -> daily.add(new DailyRevenueDTO(date, day.revenue, day.bookings)));
        return new RevenueReportDTO(new RevenueSummaryDTO(gross.subtract(refunded), paidBookings,
                ticketsSold, freeTickets, discount, refunded, cancelledOrExpired), daily,
                toGroups(routes), toGroups(types), toGroups(methods), toGroups(promotions));
    }

    private void addGroups(Map<String, Group> routes, Map<String, Group> types,
                           Map<String, Group> methods, Map<String, Group> promotions,
                           Booking booking, List<Ticket> tickets, String method, BigDecimal amount,
                           long bookingCount, long ticketCount) {
        if (tickets.isEmpty()) return;
        Ticket first = tickets.get(0);
        add(routes, first.getTrip().getRoute().getName(), amount, bookingCount, ticketCount);
        add(types, first.getTicketType().getName(), amount, bookingCount, ticketCount);
        if (method != null) add(methods, method, amount, bookingCount, ticketCount);
        add(promotions, booking.getPromotion() == null ? "Không dùng" : booking.getPromotion().getCode(),
                amount, bookingCount, ticketCount);
    }

    private void add(Map<String, Group> groups, String name, BigDecimal amount,
                     long bookingCount, long ticketCount) {
        Group group = groups.computeIfAbsent(name, ignored -> new Group());
        group.revenue = group.revenue.add(amount);
        group.bookings += bookingCount;
        group.tickets += ticketCount;
    }

    private List<RevenueGroupDTO> toGroups(Map<String, Group> groups) {
        return groups.entrySet().stream()
                .map(entry -> new RevenueGroupDTO(entry.getKey(), entry.getValue().bookings,
                        entry.getValue().tickets, entry.getValue().revenue))
                .sorted(Comparator.comparing(RevenueGroupDTO::revenue).reversed()
                        .thenComparing(RevenueGroupDTO::name))
                .toList();
    }

    private static class Day {
        private BigDecimal revenue = BigDecimal.ZERO;
        private long bookings;
    }

    private static class Group {
        private BigDecimal revenue = BigDecimal.ZERO;
        private long bookings;
        private long tickets;
    }
}
