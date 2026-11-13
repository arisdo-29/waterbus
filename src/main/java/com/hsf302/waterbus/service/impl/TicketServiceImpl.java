package com.hsf302.waterbus.service.impl;

import com.hsf302.waterbus.dto.BookingCheckinDTO;
import com.hsf302.waterbus.dto.TicketCheckinDTO;
import com.hsf302.waterbus.entity.Account;
import com.hsf302.waterbus.entity.Booking;
import com.hsf302.waterbus.entity.RouteStation;
import com.hsf302.waterbus.entity.Ticket;
import com.hsf302.waterbus.entity.TicketScanLog;
import com.hsf302.waterbus.entity.TicketType;
import com.hsf302.waterbus.entity.Trip;
import com.hsf302.waterbus.repository.AccountRepository;
import com.hsf302.waterbus.repository.BookingRepository;
import com.hsf302.waterbus.repository.TicketRepository;
import com.hsf302.waterbus.repository.TicketScanLogRepository;
import com.hsf302.waterbus.service.RouteService;
import com.hsf302.waterbus.service.TicketService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class TicketServiceImpl implements TicketService {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    private final TicketRepository ticketRepository;
    private final BookingRepository bookingRepository;
    private final AccountRepository accountRepository;
    private final TicketScanLogRepository ticketScanLogRepository;
    private final RouteService routeService;

    public TicketServiceImpl(TicketRepository ticketRepository,
                             BookingRepository bookingRepository,
                             AccountRepository accountRepository,
                             TicketScanLogRepository ticketScanLogRepository,
                             RouteService routeService) {
        this.ticketRepository = ticketRepository;
        this.bookingRepository = bookingRepository;
        this.accountRepository = accountRepository;
        this.ticketScanLogRepository = ticketScanLogRepository;
        this.routeService = routeService;
    }

    @Override
    @Transactional(readOnly = true)
    public TicketCheckinDTO getTicketInfo(String ticketCode) {
        Ticket ticket = findTicketOrThrow(ticketCode);
        LocalDateTime boardingTime = calculateBoardingTime(ticket);
        boolean outside = isOutsideCheckinWindow(boardingTime);
        return TicketCheckinDTO.fromEntity(ticket, outside);
    }

    @Override
    @Transactional(noRollbackFor = {IllegalStateException.class, IllegalArgumentException.class})
    public TicketCheckinDTO getTicketInfoWithLog(String ticketCode, Integer staffAccountId, String source) {
        try {
            Ticket ticket = findTicketOrThrow(ticketCode);
            logScan(ticket.getTicketCode(),
                    ticket.getBooking() != null ? ticket.getBooking().getBookingCode() : null,
                    ticket.getTrip(), staffAccountId, "VIEW", "SUCCESS", source, "Tra cứu vé thành công");
            LocalDateTime boardingTime = calculateBoardingTime(ticket);
            boolean outside = isOutsideCheckinWindow(boardingTime);
            return TicketCheckinDTO.fromEntity(ticket, outside);
        } catch (Exception ex) {
            logScan(ticketCode, null, null, staffAccountId, "VIEW", "FAILED", source, ex.getMessage());
            throw ex;
        }
    }

    @Override
    @Transactional(noRollbackFor = {IllegalStateException.class, IllegalArgumentException.class})
    public BookingCheckinDTO getBookingCheckinInfo(String bookingCode, Integer staffAccountId, String source) {
        if (bookingCode == null || bookingCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập mã đơn đặt vé.");
        }
        String trimmed = bookingCode.trim().toUpperCase();
        Booking booking = bookingRepository.findByBookingCode(trimmed)
                .orElseThrow(() -> {
                    logScan(null, trimmed, null, staffAccountId, "VIEW", "FAILED", source, "Không tìm thấy đơn: " + trimmed);
                    return new IllegalArgumentException("Không tìm thấy đơn đặt vé với mã: " + trimmed);
                });

        List<Ticket> tickets = ticketRepository.findByBookingIdOrderByIdAsc(booking.getId());
        Trip trip = tickets.stream().map(Ticket::getTrip).filter(java.util.Objects::nonNull).findFirst().orElse(null);

        String fromStationName = (!tickets.isEmpty() && tickets.get(0).getFromStation() != null)
                ? tickets.get(0).getFromStation().getName() : "";
        String toStationName = (!tickets.isEmpty() && tickets.get(0).getToStation() != null)
                ? tickets.get(0).getToStation().getName() : "";

        List<TicketCheckinDTO> ticketDTOs = new ArrayList<>();
        boolean anyOutside = false;
        for (Ticket t : tickets) {
            LocalDateTime bTime = calculateBoardingTime(t);
            boolean outside = isOutsideCheckinWindow(bTime);
            if (outside) {
                anyOutside = true;
            }
            ticketDTOs.add(TicketCheckinDTO.fromEntity(t, outside));
        }

        logScan(null, trimmed, trip, staffAccountId, "VIEW", "SUCCESS", source, "Tra cứu đơn thành công: " + tickets.size() + " vé");

        return BookingCheckinDTO.from(booking, trip, ticketDTOs, fromStationName, toStationName, anyOutside);
    }

    @Override
    @Transactional(readOnly = true)
    public LocalDateTime calculateBoardingTime(String ticketCode) {
        Ticket ticket = findTicketOrThrow(ticketCode);
        return calculateBoardingTime(ticket);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isOutsideCheckinWindow(String ticketCode) {
        Ticket ticket = findTicketOrThrow(ticketCode);
        LocalDateTime boardingTime = calculateBoardingTime(ticket);
        return isOutsideCheckinWindow(boardingTime);
    }

    public LocalDateTime calculateBoardingTime(Ticket ticket) {
        if (ticket == null || ticket.getTrip() == null || ticket.getTrip().getDepartureTime() == null) {
            return null;
        }
        LocalDateTime baseTime = ticket.getTrip().getDepartureTime();
        if (ticket.getFromStation() != null && ticket.getTrip().getRoute() != null) {
            try {
                List<RouteStation> stops = routeService.getStopsOfRoute(ticket.getTrip().getRoute().getId());
                if (stops != null) {
                    for (RouteStation stop : stops) {
                        if (stop.getStation() != null && stop.getStation().getId().equals(ticket.getFromStation().getId())) {
                            int minutes = stop.getMinutesFromStart() != null ? stop.getMinutesFromStart() : 0;
                            return baseTime.plusMinutes(minutes);
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return baseTime;
    }

    public boolean isOutsideCheckinWindow(LocalDateTime boardingTime) {
        if (boardingTime == null) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        return !now.toLocalDate().isEqual(boardingTime.toLocalDate())
                || now.isBefore(boardingTime.minusMinutes(60))
                || now.isAfter(boardingTime.plusMinutes(15));
    }

    @Override
    @Transactional(noRollbackFor = {IllegalStateException.class, IllegalArgumentException.class})
    public TicketCheckinDTO checkIn(String ticketCode) {
        return checkIn(ticketCode, null, "MANUAL", false);
    }

    @Override
    @Transactional(noRollbackFor = {IllegalStateException.class, IllegalArgumentException.class})
    public TicketCheckinDTO checkIn(String ticketCode, Integer staffAccountId, String source, boolean overrideTripTime) {
        String safeSource = (source == null || source.isBlank()) ? "MANUAL" : source;

        Ticket ticket;
        try {
            ticket = findTicketOrThrow(ticketCode);
        } catch (Exception ex) {
            logScan(ticketCode, null, null, staffAccountId, "CHECKIN", "FAILED", safeSource, ex.getMessage());
            throw ex;
        }

        String currentStatus = ticket.getStatus();

        if ("USED".equalsIgnoreCase(currentStatus)) {
            String timeStr = (ticket.getCheckedInAt() != null)
                    ? ticket.getCheckedInAt().format(TIME_FORMAT)
                    : "trước đó";
            String msg = "Vé " + ticket.getTicketCode() + " đã được soát lúc " + timeStr + ", không thể tái sử dụng!";
            logScan(ticket.getTicketCode(), getBookingCode(ticket), ticket.getTrip(), staffAccountId, "CHECKIN", "FAILED", safeSource, msg);
            throw new IllegalStateException(msg);
        }

        if ("HOLD".equalsIgnoreCase(currentStatus)) {
            String msg = "Vé " + ticket.getTicketCode() + " chưa được thanh toán (trạng thái: Đang giữ chỗ)!";
            logScan(ticket.getTicketCode(), getBookingCode(ticket), ticket.getTrip(), staffAccountId, "CHECKIN", "FAILED", safeSource, msg);
            throw new IllegalStateException(msg);
        }

        if ("CANCELLED".equalsIgnoreCase(currentStatus)) {
            String msg = "Vé " + ticket.getTicketCode() + " đã bị hủy, không hợp lệ để lên tàu!";
            logScan(ticket.getTicketCode(), getBookingCode(ticket), ticket.getTrip(), staffAccountId, "CHECKIN", "FAILED", safeSource, msg);
            throw new IllegalStateException(msg);
        }

        if (!"VALID".equalsIgnoreCase(currentStatus)) {
            String msg = "Vé " + ticket.getTicketCode() + " không hợp lệ để soát (trạng thái: " + currentStatus + ")!";
            logScan(ticket.getTicketCode(), getBookingCode(ticket), ticket.getTrip(), staffAccountId, "CHECKIN", "FAILED", safeSource, msg);
            throw new IllegalStateException(msg);
        }

        if (ticket.getTrip() != null && "CANCELLED".equalsIgnoreCase(ticket.getTrip().getStatus())) {
            String msg = "Chuyến tàu " + ticket.getTrip().getTripCode() + " đã bị hủy, không thể soát vé!";
            logScan(ticket.getTicketCode(), getBookingCode(ticket), ticket.getTrip(), staffAccountId, "CHECKIN", "FAILED", safeSource, msg);
            throw new IllegalStateException(msg);
        }

        if (ticket.getBooking() != null) {
            String bookingStatus = ticket.getBooking().getStatus();
            if ("CANCELLED".equalsIgnoreCase(bookingStatus)) {
                String msg = "Đơn đặt vé " + ticket.getBooking().getBookingCode() + " đã bị hủy, không thể soát vé!";
                logScan(ticket.getTicketCode(), getBookingCode(ticket), ticket.getTrip(), staffAccountId, "CHECKIN", "FAILED", safeSource, msg);
                throw new IllegalStateException(msg);
            }
            if ("REFUNDED".equalsIgnoreCase(bookingStatus)) {
                String msg = "Đơn đặt vé " + ticket.getBooking().getBookingCode() + " đã được hoàn tiền, không thể soát vé!";
                logScan(ticket.getTicketCode(), getBookingCode(ticket), ticket.getTrip(), staffAccountId, "CHECKIN", "FAILED", safeSource, msg);
                throw new IllegalStateException(msg);
            }
        }

        LocalDateTime boardingTime = calculateBoardingTime(ticket);
        if (boardingTime != null) {
            LocalDateTime now = LocalDateTime.now();

            if (!now.toLocalDate().isEqual(boardingTime.toLocalDate())) {
                String msg = "Chuyến tàu " + ticket.getTrip().getTripCode() + " khởi hành ngày "
                        + boardingTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                        + ", không phải hôm nay. Không thể soát vé!";
                logScan(ticket.getTicketCode(), getBookingCode(ticket), ticket.getTrip(), staffAccountId, "CHECKIN", "FAILED", safeSource, msg);
                throw new IllegalStateException(msg);
            }

            if (now.isBefore(boardingTime.minusMinutes(60))) {
                String msg = "Chưa đến giờ soát vé cho chuyến " + ticket.getTrip().getTripCode()
                        + " (giờ lên tàu dự kiến lúc " + boardingTime.format(TIME_FORMAT)
                        + "). Chỉ được soát vé trước giờ lên tàu tối đa 60 phút.";
                logScan(ticket.getTicketCode(), getBookingCode(ticket), ticket.getTrip(), staffAccountId, "CHECKIN", "FAILED", safeSource, msg);
                throw new IllegalStateException(msg);
            }

            if (now.isAfter(boardingTime.plusMinutes(15))) {
                String msg = "Chuyến tàu " + ticket.getTrip().getTripCode() + " đã qua bến lúc " + boardingTime.format(TIME_FORMAT) + ". Không thể soát vé!";
                logScan(ticket.getTicketCode(), getBookingCode(ticket), ticket.getTrip(), staffAccountId, "CHECKIN", "FAILED", safeSource, msg);
                throw new IllegalStateException(msg);
            }
        }

        ticket.setStatus("USED");
        ticket.setCheckedInAt(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        Ticket saved = ticketRepository.save(ticket);

        logScan(saved.getTicketCode(), getBookingCode(saved), saved.getTrip(), staffAccountId, "CHECKIN", "SUCCESS", safeSource, "Soát vé thành công");

        LocalDateTime finalBoardingTime = calculateBoardingTime(saved);
        boolean outside = isOutsideCheckinWindow(finalBoardingTime);
        return TicketCheckinDTO.fromEntity(saved, outside);
    }

    @Override
    @Transactional(noRollbackFor = {IllegalStateException.class, IllegalArgumentException.class})
    public List<TicketCheckinDTO> checkInGroup(List<String> ticketCodes, Integer staffAccountId, String source) {
        if (ticketCodes == null || ticketCodes.isEmpty()) {
            return Collections.emptyList();
        }
        List<TicketCheckinDTO> result = new ArrayList<>();
        for (String code : ticketCodes) {
            if (code != null && !code.trim().isEmpty()) {
                result.add(checkIn(code.trim(), staffAccountId, source, false));
            }
        }
        return result;
    }

    @Override
    @Transactional(noRollbackFor = {IllegalStateException.class, IllegalArgumentException.class})
    public TicketCheckinDTO rejectPriorityTicket(String ticketCode, String reason, Integer staffAccountId) {
        Ticket ticket = findTicketOrThrow(ticketCode);
        if (ticket.getTicketType() == null || !Boolean.TRUE.equals(ticket.getTicketType().getRequiresVerification())) {
            String msg = "Chỉ có thể từ chối vé ưu đãi cần xác minh giấy tờ.";
            logScan(ticket.getTicketCode(), getBookingCode(ticket), ticket.getTrip(), staffAccountId, "REJECT", "FAILED", "MANUAL", msg);
            throw new IllegalArgumentException(msg);
        }
        if (!"VALID".equalsIgnoreCase(ticket.getStatus())) {
            String msg = "Chỉ có thể từ chối vé đang ở trạng thái hợp lệ (VALID).";
            logScan(ticket.getTicketCode(), getBookingCode(ticket), ticket.getTrip(), staffAccountId, "REJECT", "FAILED", "MANUAL", msg);
            throw new IllegalStateException(msg);
        }

        String safeReason = (reason == null || reason.isBlank()) ? "Không xuất trình được giấy tờ hợp lệ" : reason.trim();

        ticket.setStatus("CANCELLED");
        Ticket saved = ticketRepository.save(ticket);

        logScan(saved.getTicketCode(), getBookingCode(saved), saved.getTrip(), staffAccountId, "REJECT", "SUCCESS", "MANUAL", "Từ chối vé ưu đãi: " + safeReason);

        LocalDateTime boardingTime = calculateBoardingTime(saved);
        boolean outside = isOutsideCheckinWindow(boardingTime);
        return TicketCheckinDTO.fromEntity(saved, outside);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketCheckinDTO> getPassengerManifest(int tripId) {
        List<Ticket> tickets = ticketRepository.findByTripIdOrderByIdAsc(tripId);
        return tickets.stream().map(TicketCheckinDTO::fromEntity).toList();
    }

    private Ticket findTicketOrThrow(String ticketCode) {
        if (ticketCode == null || ticketCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập mã vé cần soát.");
        }
        String trimmed = ticketCode.trim().toUpperCase();
        return ticketRepository.findByTicketCode(trimmed)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy vé với mã: " + trimmed));
    }

    private String getBookingCode(Ticket ticket) {
        return ticket.getBooking() != null ? ticket.getBooking().getBookingCode() : null;
    }

    private void logScan(String ticketCode, String bookingCode, Trip trip, Integer staffAccountId,
                         String action, String result, String source, String message) {
        try {
            TicketScanLog log = new TicketScanLog();
            log.setTicketCode(ticketCode);
            log.setBookingCode(bookingCode);
            log.setTrip(trip);
            log.setAction(action);
            log.setResult(result);
            log.setSource(source != null ? source : "MANUAL");
            log.setMessage(message);
            log.setCreatedAt(LocalDateTime.now());

            Account staff = null;
            if (staffAccountId != null) {
                staff = accountRepository.findById(staffAccountId).orElse(null);
            }
            if (staff != null) {
                log.setStaff(staff);
                ticketScanLogRepository.save(log);
            }
        } catch (Exception ignored) {
        }
    }
}
