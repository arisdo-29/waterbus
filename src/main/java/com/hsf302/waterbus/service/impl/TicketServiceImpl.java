package com.hsf302.waterbus.service.impl;

import com.hsf302.waterbus.dto.BookingCheckinDTO;
import com.hsf302.waterbus.dto.TicketCheckinDTO;
import com.hsf302.waterbus.entity.Account;
import com.hsf302.waterbus.entity.Booking;
import com.hsf302.waterbus.entity.Ticket;
import com.hsf302.waterbus.entity.TicketScanLog;
import com.hsf302.waterbus.entity.TicketType;
import com.hsf302.waterbus.entity.Trip;
import com.hsf302.waterbus.repository.AccountRepository;
import com.hsf302.waterbus.repository.BookingRepository;
import com.hsf302.waterbus.repository.TicketRepository;
import com.hsf302.waterbus.repository.TicketScanLogRepository;
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

// Module: BE3 (Tân) - Soát vé (Staff Check-in) theo M4 & M5
@Service
public class TicketServiceImpl implements TicketService {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    private final TicketRepository ticketRepository;
    private final BookingRepository bookingRepository;
    private final AccountRepository accountRepository;
    private final TicketScanLogRepository ticketScanLogRepository;

    // Constructor Injection
    public TicketServiceImpl(TicketRepository ticketRepository,
                             BookingRepository bookingRepository,
                             AccountRepository accountRepository,
                             TicketScanLogRepository ticketScanLogRepository) {
        this.ticketRepository = ticketRepository;
        this.bookingRepository = bookingRepository;
        this.accountRepository = accountRepository;
        this.ticketScanLogRepository = ticketScanLogRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public TicketCheckinDTO getTicketInfo(String ticketCode) {
        Ticket ticket = findTicketOrThrow(ticketCode);
        return mapToDTO(ticket);
    }

    @Override
    @Transactional
    public TicketCheckinDTO getTicketInfoWithLog(String ticketCode, Integer staffAccountId, String source) {
        try {
            Ticket ticket = findTicketOrThrow(ticketCode);
            logScan(ticket.getTicketCode(),
                    ticket.getBooking() != null ? ticket.getBooking().getBookingCode() : null,
                    ticket.getTrip(), staffAccountId, "VIEW", "SUCCESS", source, "Tra cứu vé thành công");
            return mapToDTO(ticket);
        } catch (Exception ex) {
            logScan(ticketCode, null, null, staffAccountId, "VIEW", "FAILED", source, ex.getMessage());
            throw ex;
        }
    }

    @Override
    @Transactional
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
        List<TicketCheckinDTO> ticketDTOs = new ArrayList<>();
        Trip trip = null;
        for (Ticket t : tickets) {
            ticketDTOs.add(mapToDTO(t));
            if (trip == null && t.getTrip() != null) {
                trip = t.getTrip();
            }
        }

        String routeName = (trip != null && trip.getRoute() != null) ? trip.getRoute().getName() : "";
        String fromStationName = (!tickets.isEmpty() && tickets.get(0).getFromStation() != null)
                ? tickets.get(0).getFromStation().getName() : "";
        String toStationName = (!tickets.isEmpty() && tickets.get(0).getToStation() != null)
                ? tickets.get(0).getToStation().getName() : "";
        String tripCode = (trip != null) ? trip.getTripCode() : "";
        Integer tripId = (trip != null) ? trip.getId() : null;
        LocalDateTime departureTime = (trip != null) ? trip.getDepartureTime() : null;

        logScan(null, trimmed, trip, staffAccountId, "VIEW", "SUCCESS", source, "Tra cứu đơn thành công: " + ticketDTOs.size() + " vé");

        return new BookingCheckinDTO(
                booking.getBookingCode(),
                booking.getContactName(),
                booking.getContactPhone(),
                tripCode,
                tripId,
                routeName,
                fromStationName,
                toStationName,
                departureTime,
                booking.getStatus(),
                ticketDTOs
        );
    }

    @Override
    @Transactional
    public TicketCheckinDTO checkIn(String ticketCode) {
        return checkIn(ticketCode, null, "MANUAL", false);
    }

    @Override
    @Transactional
    public TicketCheckinDTO checkIn(String ticketCode, Integer staffAccountId, String source, boolean overrideTripTime) {
        String safeSource = (source == null || source.isBlank()) ? "MANUAL" : source;
        if (overrideTripTime) {
            safeSource = "OVERRIDE";
        }

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

        // Kiểm tra chuyến tàu: từ chối vé của chuyến đã bị huỷ
        if (ticket.getTrip() != null && "CANCELLED".equalsIgnoreCase(ticket.getTrip().getStatus())) {
            String msg = "Chuyến tàu " + ticket.getTrip().getTripCode() + " đã bị hủy, không thể soát vé!";
            logScan(ticket.getTicketCode(), getBookingCode(ticket), ticket.getTrip(), staffAccountId, "CHECKIN", "FAILED", safeSource, msg);
            throw new IllegalStateException(msg);
        }

        // Kiểm tra đơn đặt vé: từ chối vé của đơn đã bị huỷ hoặc đã hoàn tiền
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

        // Kiểm tra khung giờ khởi hành (chỉ kiểm tra nếu có departureTime và không có override)
        if (ticket.getTrip() != null && ticket.getTrip().getDepartureTime() != null && !overrideTripTime) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime departure = ticket.getTrip().getDepartureTime();
            // Nếu chuyến đã chạy quá 15 phút
            if (now.isAfter(departure.plusMinutes(15))) {
                String msg = "Chuyến tàu " + ticket.getTrip().getTripCode() + " đã khởi hành lúc " + departure.format(TIME_FORMAT) + ". Cần xác nhận Override để soát vé muộn!";
                logScan(ticket.getTicketCode(), getBookingCode(ticket), ticket.getTrip(), staffAccountId, "CHECKIN", "FAILED", safeSource, msg);
                throw new IllegalStateException(msg);
            }
        }

        // Cập nhật trạng thái vé sang USED và đóng dấu thời gian soát vé
        ticket.setStatus("USED");
        ticket.setCheckedInAt(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        Ticket saved = ticketRepository.save(ticket);

        logScan(saved.getTicketCode(), getBookingCode(saved), saved.getTrip(), staffAccountId, "CHECKIN", "SUCCESS", safeSource, "Soát vé thành công");

        return mapToDTO(saved);
    }

    @Override
    @Transactional
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
    @Transactional
    public TicketCheckinDTO rejectPriorityTicket(String ticketCode, String reason, Integer staffAccountId) {
        Ticket ticket = findTicketOrThrow(ticketCode);
        if (!"VALID".equalsIgnoreCase(ticket.getStatus())) {
            throw new IllegalStateException("Chỉ có thể từ chối vé đang ở trạng thái hợp lệ (VALID).");
        }

        String safeReason = (reason == null || reason.isBlank()) ? "Không xuất trình được giấy tờ hợp lệ" : reason.trim();

        // Chuyển vé sang CANCELLED để nhả ghế
        ticket.setStatus("CANCELLED");
        Ticket saved = ticketRepository.save(ticket);

        logScan(saved.getTicketCode(), getBookingCode(saved), saved.getTrip(), staffAccountId, "REJECT", "SUCCESS", "MANUAL", "Từ chối vé ưu đãi: " + safeReason);

        return mapToDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketCheckinDTO> getPassengerManifest(int tripId) {
        List<Ticket> tickets = ticketRepository.findByTripIdOrderByIdAsc(tripId);
        List<TicketCheckinDTO> result = new ArrayList<>();
        for (Ticket t : tickets) {
            result.add(mapToDTO(t));
        }
        return result;
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
            if (staff == null) {
                // Lấy tài khoản đầu tiên làm fallback cho mock/test
                staff = accountRepository.findAll().stream().findFirst().orElse(null);
            }
            if (staff != null) {
                log.setStaff(staff);
                ticketScanLogRepository.save(log);
            }
        } catch (Exception ignored) {
            // Log không được làm fail giao dịch chính
        }
    }

    private TicketCheckinDTO mapToDTO(Ticket ticket) {
        Booking booking = ticket.getBooking();
        Trip trip = ticket.getTrip();
        TicketType ticketType = ticket.getTicketType();

        String routeName = (trip != null && trip.getRoute() != null) ? trip.getRoute().getName() : "";
        String waterbusName = (ticket.getSeat() != null && ticket.getSeat().getWaterbus() != null)
                ? ticket.getSeat().getWaterbus().getName() : "";
        String seatCode = (ticket.getSeat() != null) ? ticket.getSeat().getSeatCode() : "";
        String fromStationName = (ticket.getFromStation() != null) ? ticket.getFromStation().getName() : "";
        String toStationName = (ticket.getToStation() != null) ? ticket.getToStation().getName() : "";
        String ticketTypeName = (ticketType != null) ? ticketType.getName() : "";
        String ticketTypeCode = (ticketType != null) ? ticketType.getCode() : "";
        boolean requiresVerification = (ticketType != null) && Boolean.TRUE.equals(ticketType.getRequiresVerification());

        String bookingCode = (booking != null) ? booking.getBookingCode() : "";
        // Tên hành khách ưu tiên lấy từ ticket.getPassengerName(), nếu null lấy booking.getContactName()
        String passengerName = ticket.getPassengerName();
        if (passengerName == null || passengerName.isBlank()) {
            passengerName = (booking != null) ? booking.getContactName() : "";
        }
        String passengerPhone = (booking != null) ? booking.getContactPhone() : "";
        String tripCode = (trip != null) ? trip.getTripCode() : "";
        Integer tripId = (trip != null) ? trip.getId() : null;
        LocalDateTime departureTime = (trip != null) ? trip.getDepartureTime() : null;

        Integer birthYear = ticket.getBirthYear();
        Integer passengerAge = null;
        if (birthYear != null) {
            passengerAge = LocalDate.now().getYear() - birthYear;
        }

        return new TicketCheckinDTO(
                ticket.getTicketCode(),
                bookingCode,
                passengerName,
                passengerPhone,
                tripCode,
                tripId,
                routeName,
                fromStationName,
                toStationName,
                departureTime,
                seatCode,
                waterbusName,
                ticketTypeName,
                ticketTypeCode,
                ticket.getPrice(),
                ticket.getStatus(),
                ticket.getCheckedInAt(),
                birthYear,
                passengerAge,
                requiresVerification
        );
    }
}
