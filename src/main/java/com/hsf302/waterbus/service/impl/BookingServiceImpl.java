package com.hsf302.waterbus.service.impl;

import com.hsf302.waterbus.dto.BookingDTO;
import com.hsf302.waterbus.dto.BookingForm;
import com.hsf302.waterbus.dto.BookingTripDTO;
import com.hsf302.waterbus.dto.SeatDTO;
import com.hsf302.waterbus.dto.TicketDTO;
import com.hsf302.waterbus.dto.TicketTypeDTO;
import com.hsf302.waterbus.entity.Booking;
import com.hsf302.waterbus.entity.Promotion;
import com.hsf302.waterbus.entity.RouteStation;
import com.hsf302.waterbus.entity.Seat;
import com.hsf302.waterbus.entity.Ticket;
import com.hsf302.waterbus.entity.TicketType;
import com.hsf302.waterbus.entity.Trip;
import com.hsf302.waterbus.repository.BookingRepository;
import com.hsf302.waterbus.repository.TicketRepository;
import com.hsf302.waterbus.entity.Refund;
import com.hsf302.waterbus.service.RefundService;
import com.hsf302.waterbus.service.AccountService;
import com.hsf302.waterbus.service.BookingService;
import com.hsf302.waterbus.service.PromotionService;
import com.hsf302.waterbus.service.RouteService;
import com.hsf302.waterbus.service.TicketTypeService;
import com.hsf302.waterbus.service.TripService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Errors;

@Service
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final TicketRepository ticketRepository;
    private final TripService tripService;
    private final RefundService refundService;
    private final RouteService routeService;
    private final TicketTypeService ticketTypeService;
    private final PromotionService promotionService;
    private final AccountService accountService;

    public BookingServiceImpl(BookingRepository bookingRepository, TicketRepository ticketRepository,
                              TripService tripService, RouteService routeService, RefundService refundService,
                              TicketTypeService ticketTypeService, PromotionService promotionService,
                              AccountService accountService) {
        this.bookingRepository = bookingRepository;
        this.ticketRepository = ticketRepository;
        this.tripService = tripService;
        this.refundService = refundService;
        this.routeService = routeService;
        this.ticketTypeService = ticketTypeService;
        this.promotionService = promotionService;
        this.accountService = accountService;
    }

    @Override
    @Transactional
    public BookingDTO createBooking(BookingForm form, Integer accountId) {
        if (form.getTripId() == null || form.getTicketTypeId() == null
                || form.getFromStationId() == null || form.getToStationId() == null) {
            throw new IllegalArgumentException("Thiếu thông tin chuyến, loại vé hoặc bến.");
        }
        int tripId = form.getTripId();
        int ticketTypeId = form.getTicketTypeId();
        int fromStationId = form.getFromStationId();
        int toStationId = form.getToStationId();
        List<Integer> seatIds = form.getSeatIds();
        String promoCode = form.getPromoCode();
        String contactName = form.getContactName();
        String contactPhone = form.getContactPhone();
        String contactEmail = form.getContactEmail();
        List<String> passengerNames = form.getPassengerNames();
        List<Integer> birthYears = form.getBirthYears();

        if (isBlank(contactName) || isBlank(contactPhone) || isBlank(contactEmail)) {
            throw new IllegalArgumentException("Vui lòng nhập đầy đủ họ tên, số điện thoại và email.");
        }
        if (!contactPhone.trim().matches("0[0-9]{9}")) {
            throw new IllegalArgumentException("Số điện thoại phải gồm 10 chữ số và bắt đầu bằng 0.");
        }
        if (contactName.trim().length() > 100 || contactEmail.trim().length() > 100) {
            throw new IllegalArgumentException("Họ tên và email tối đa 100 ký tự.");
        }
        if (seatIds == null || seatIds.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn ít nhất 1 ghế.");
        }
        seatIds = seatIds.stream().distinct().toList();
        if (seatIds.size() > BookingService.MAX_SEATS_PER_BOOKING) {
            throw new IllegalArgumentException("Bạn chỉ có thể chọn tối đa "
                    + BookingService.MAX_SEATS_PER_BOOKING + " ghế trong một lần đặt.");
        }
        if (fromStationId == toStationId) {
            throw new IllegalArgumentException("Bến đi và bến đến phải khác nhau.");
        }

        Map<Integer, Seat> availableSeats = new HashMap<>();
        for (Seat seat : tripService.getAvailableSeats(tripId)) {
            availableSeats.put(seat.getId(), seat);
        }
        if (!availableSeats.keySet().containsAll(seatIds)) {
            throw new IllegalArgumentException("Ghế vừa có người đặt. Vui lòng chọn ghế khác.");
        }

        Trip trip = tripService.getTripById(tripId);
        if (trip == null || !("SCHEDULED".equals(trip.getStatus()) || "DEPARTED".equals(trip.getStatus()))) {
            throw new IllegalArgumentException("Chuyến tàu này không còn nhận đặt vé.");
        }

        RouteStation fromStop = null;
        RouteStation toStop = null;
        for (RouteStation stop : routeService.getStopsOfRoute(trip.getRoute().getId())) {
            if (stop.getStation().getId() == fromStationId) {
                fromStop = stop;
            }
            if (stop.getStation().getId() == toStationId) {
                toStop = stop;
            }
        }
        if (fromStop == null || toStop == null || fromStop.getStopOrder() >= toStop.getStopOrder()) {
            throw new IllegalArgumentException("Bến đi và bến đến không đúng với tuyến của chuyến này.");
        }
        if (isOnlineSalesClosed(trip, fromStop)) {
            throw new IllegalArgumentException("Chuyến tàu sắp khởi hành, đã ngừng bán vé online.");
        }

        TicketType ticketType = null;
        for (TicketType type : ticketTypeService.getActiveTicketTypes()) {
            if (type.getId() == ticketTypeId) {
                ticketType = type;
            }
        }
        if (ticketType == null) {
            throw new IllegalArgumentException("Loại vé không hợp lệ.");
        }
        if (Boolean.TRUE.equals(ticketType.getRequiresVerification())) {
            checkPriorityTicketRules(accountId, seatIds.size());
        }

        BigDecimal price = ticketTypeService.getPrice(ticketTypeId);
        BigDecimal subtotal = calculateSubtotal(ticketTypeId, seatIds.size());
        BigDecimal discount = promotionService.calculateDiscount(promoCode, subtotal);
        Promotion promotion = null;
        if (!isBlank(promoCode)) {
            if (discount.compareTo(BigDecimal.ZERO) == 0) {
                throw new IllegalArgumentException("Mã khuyến mãi không hợp lệ, đã hết hạn hoặc đơn chưa đủ điều kiện.");
            }
            promotion = promotionService.getPromotionByCode(promoCode).orElse(null);
        }

        BigDecimal total = subtotal.subtract(discount);
        boolean isFree = total.compareTo(BigDecimal.ZERO) == 0;
        String number = newBookingNumber();
        Booking booking = form.toEntity();
        booking.setBookingCode("BK" + number);
        booking.setPromotion(promotion);
        if (accountId != null) {
            booking.setAccount(accountService.getAccountEntityById(accountId));
        }
        booking.setSubtotal(subtotal);
        booking.setDiscountAmount(discount);
        booking.setTotalAmount(total);
        booking.setCreatedAt(LocalDateTime.now());
        booking.setExpiresAt(booking.getCreatedAt().plusMinutes(10));
        if (isFree) {
            booking.setStatus("PAID");
            booking.setPaidAt(booking.getCreatedAt());
        } else {
            booking.setStatus("PENDING");
        }
        bookingRepository.save(booking);

        try {
            for (int i = 0; i < seatIds.size(); i++) {
                Ticket ticket = new Ticket();
                ticket.setTicketCode("TK" + number + "-" + (i + 1));
                ticket.setBooking(booking);
                ticket.setTrip(trip);
                ticket.setSeat(availableSeats.get(seatIds.get(i)));
                ticket.setTicketType(ticketType);
                ticket.setFromStation(fromStop.getStation());
                ticket.setToStation(toStop.getStation());
                ticket.setPrice(price);
                ticket.setStatus(isFree ? "VALID" : "HOLD");
                if (passengerNames != null && i < passengerNames.size() && !isBlank(passengerNames.get(i))) {
                    ticket.setPassengerName(passengerNames.get(i).trim());
                }
                if (birthYears != null && i < birthYears.size()) {
                    ticket.setBirthYear(birthYears.get(i));
                }
                ticketRepository.save(ticket);
            }
            ticketRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            if (isActiveSeatConflict(ex)) {
                throw new IllegalArgumentException("Ghế vừa có người đặt. Vui lòng chọn ghế khác.", ex);
            }
            throw ex;
        }

        if (isFree && promotion != null) {
            promotionService.increaseUsedCount(promotion.getCode());
        }
        return toDTO(booking);
    }

    private void checkPriorityTicketRules(Integer accountId, int seatCount) {
        if (accountId == null) {
            throw new IllegalArgumentException("Vui lòng đăng nhập để đặt vé ưu đãi.");
        }
        if (seatCount > BookingService.MAX_PRIORITY_TICKETS_PER_BOOKING) {
            throw new IllegalArgumentException("Mỗi đơn chỉ được đặt tối đa "
                    + BookingService.MAX_PRIORITY_TICKETS_PER_BOOKING + " vé ưu đãi.");
        }
    }

    private boolean isOnlineSalesClosed(Trip trip, RouteStation fromStop) {
        int minutesFromStart = 0;
        if (fromStop.getMinutesFromStart() != null) {
            minutesFromStart = fromStop.getMinutesFromStart();
        }
        LocalDateTime boardingTime = trip.getDepartureTime().plusMinutes(minutesFromStart);
        LocalDateTime stopSellingAt = LocalDateTime.now().plusMinutes(TripService.ONLINE_SALES_CUTOFF_MINUTES);
        return !boardingTime.isAfter(stopSellingAt);
    }

    private boolean isActiveSeatConflict(DataIntegrityViolationException exception) {
        String indexName = "ux_ticket_trip_seat_active";
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof org.hibernate.exception.ConstraintViolationException violation
                    && indexName.equalsIgnoreCase(violation.getConstraintName())) {
                return true;
            }
            if (cause.getMessage() != null
                    && cause.getMessage().toLowerCase(java.util.Locale.ROOT).contains(indexName)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Booking getBookingByCode(String bookingCode) {
        return bookingRepository.findByBookingCode(bookingCode).orElse(null);
    }

    @Override
    public List<BookingDTO> getBookingsOfAccount(int accountId) {
        return bookingRepository.findByAccountIdOrderByCreatedAtDesc(accountId).stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    public BookingDTO getBookingDetail(String bookingCode) {
        return bookingRepository.findByBookingCode(bookingCode)
                .map(this::toDTO)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketDTO> getTicketsOfBooking(int bookingId) {
        return ticketRepository.findByBookingIdOrderByIdAsc(bookingId).stream()
                .map(this::toTicketDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BookingTripDTO getTripOfBooking(int bookingId) {
        List<Ticket> tickets = ticketRepository.findByBookingIdOrderByIdAsc(bookingId);
        if (tickets.isEmpty()) {
            return null;
        }
        return BookingTripDTO.fromEntity(tickets.get(0).getTrip());
    }

    @Override
    @Transactional(readOnly = true)
    public String getTripCancelReason(int bookingId) {
        List<Ticket> tickets = ticketRepository.findByBookingIdOrderByIdAsc(bookingId);
        return tickets.isEmpty() ? null : tickets.get(0).getTrip().getCancelReason();
    }

    @Override
    public List<SeatDTO> getSelectedSeats(int tripId, List<Integer> seatIds) {
        if (seatIds == null) {
            return List.of();
        }
        List<SeatDTO> selectedSeats = new ArrayList<>();
        for (Seat seat : tripService.getAvailableSeats(tripId)) {
            if (seatIds.contains(seat.getId())) {
                selectedSeats.add(SeatDTO.fromEntity(seat, true));
            }
        }
        return selectedSeats;
    }

    @Override
    public BigDecimal calculateSubtotal(int ticketTypeId, int seatCount) {
        return ticketTypeService.getPrice(ticketTypeId).multiply(BigDecimal.valueOf(seatCount));
    }

    @Override
    public BookingTripDTO getTripInfo(int tripId, int fromStationId, int toStationId) {
        return tripService.getBookableTrip(tripId, fromStationId, toStationId);
    }

    @Override
    public void validatePassengers(BookingForm form, Errors errors) {
        if (form.getTripId() == null || form.getTicketTypeId() == null
                || form.getFromStationId() == null || form.getToStationId() == null) {
            return;
        }
        TicketTypeDTO ticketType = ticketTypeService.getTicketTypeById(form.getTicketTypeId());
        if (ticketType == null || !Boolean.TRUE.equals(ticketType.getRequiresVerification())) {
            return;
        }
        BookingTripDTO trip = getTripInfo(form.getTripId(), form.getFromStationId(), form.getToStationId());
        if (trip == null) {
            return;
        }

        int seatCount = (form.getSeatIds() == null) ? 0 : form.getSeatIds().size();
        List<String> names = form.getPassengerNames();
        List<Integer> years = form.getBirthYears();
        if (names == null || years == null || names.size() != seatCount || years.size() != seatCount) {
            errors.rejectValue("passengerNames", "passengers.count",
                    "Vui lòng nhập họ tên và năm sinh cho từng ghế.");
            return;
        }

        int currentYear = LocalDate.now().getYear();
        int travelYear = trip.getDepartureTime().getYear();
        for (int i = 0; i < seatCount; i++) {
            String name = names.get(i);
            if (isBlank(name)) {
                errors.rejectValue("passengerNames[" + i + "]", "passenger.name",
                        "Vui lòng nhập họ tên hành khách.");
            } else if (name.trim().length() > 100) {
                errors.rejectValue("passengerNames[" + i + "]", "passenger.name",
                        "Họ tên hành khách tối đa 100 ký tự.");
            }

            Integer year = years.get(i);
            if (year == null || year < 1900 || year > currentYear) {
                errors.rejectValue("birthYears[" + i + "]", "passenger.birthYear",
                        "Năm sinh phải từ 1900 đến " + currentYear + ".");
                continue;
            }
            int age = travelYear - year;
            if ("SENIOR_70".equals(ticketType.getCode()) && age < 70) {
                errors.rejectValue("birthYears[" + i + "]", "passenger.age",
                        "Vé người cao tuổi chỉ dành cho hành khách từ 70 tuổi trở lên.");
            }
        }
    }

    @Override
    public boolean areSeatsAvailable(int tripId, List<Integer> seatIds) {
        for (Integer seatId : seatIds) {
            if (!tripService.isSeatAvailable(tripId, seatId)) {
                return false;
            }
        }
        return true;
    }

    @Override
    @Transactional
    public void releaseHoldTickets(int bookingId) {
        List<Ticket> tickets = ticketRepository.findByBookingIdOrderByIdAsc(bookingId);
        for (Ticket ticket : tickets) {
            if ("HOLD".equals(ticket.getStatus())) {
                ticket.setStatus("CANCELLED");
                ticketRepository.save(ticket);
            }
        }
    }

    @Override
    @Transactional
    public void cancelBooking(String bookingCode, Integer accountId) {
        Booking booking = bookingRepository.findByBookingCode(bookingCode).orElse(null);
        if (booking == null) {
            throw new IllegalArgumentException("Không tìm thấy đơn đặt vé.");
        }
        if (!"PENDING".equals(booking.getStatus())) {
            throw new IllegalArgumentException("Chỉ huỷ được đơn chưa thanh toán.");
        }
        if (booking.getAccount() != null && !booking.getAccount().getId().equals(accountId)) {
            throw new IllegalArgumentException("Bạn không có quyền huỷ đơn này.");
        }
        booking.setStatus("CANCELLED");
        bookingRepository.save(booking);
        releaseHoldTickets(booking.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public ViewAccess checkViewAccess(String bookingCode, Integer loginAccountId, String loginRole,
                                      Set<String> viewableBookingCodes) {
        Booking booking = bookingRepository.findByBookingCode(bookingCode).orElse(null);
        if (booking == null) {
            return ViewAccess.NOT_FOUND;
        }
        if ("STAFF".equalsIgnoreCase(loginRole) || "ADMIN".equalsIgnoreCase(loginRole)) {
            return ViewAccess.ALLOWED;
        }
        if (booking.getAccount() != null) {
            if (loginAccountId == null) {
                return ViewAccess.NEED_LOGIN;
            }
            if (booking.getAccount().getId().equals(loginAccountId)) {
                return ViewAccess.ALLOWED;
            }
            return ViewAccess.NOT_OWNER;
        }
        if (viewableBookingCodes != null && viewableBookingCodes.contains(booking.getBookingCode())) {
            return ViewAccess.ALLOWED;
        }
        return ViewAccess.NEED_PHONE;
    }

    @Override
    public boolean verifyContactPhone(String bookingCode, String contactPhone) {
        Booking booking = bookingRepository.findByBookingCode(bookingCode).orElse(null);
        if (booking == null || booking.getAccount() != null || isBlank(contactPhone)) {
            return false;
        }
        return contactPhone.trim().equals(booking.getContactPhone());
    }

    private String newBookingNumber() {
        String number;
        do {
            number = String.format("%08d", (int) (Math.random() * 100_000_000));
        } while (bookingRepository.existsByBookingCode("BK" + number));
        return number;
    }

    private boolean isBlank(String text) {
        return text == null || text.trim().isEmpty();
    }

    private BookingDTO toDTO(Booking b) {
        return BookingDTO.fromEntity(b, bookingDisplayStatus(b), refundStatusOf(b));
    }

    private String refundStatusOf(Booking b) {
        String refundStatus = null;
        Refund refund = refundService.getRefundOfBooking(b.getId());
        if (refund != null) {
            refundStatus = refund.getStatus();
        }
        return refundStatus;
    }

    private String bookingDisplayStatus(Booking b) {
        String status = b.getStatus();
        if (isHoldExpired(b)) {
            status = "EXPIRED";
        }
        return status;
    }

    private boolean isHoldExpired(Booking b) {
        if (!"PENDING".equals(b.getStatus()) || b.getExpiresAt() == null) {
            return false;
        }
        return b.getExpiresAt().isBefore(LocalDateTime.now());
    }

    private TicketDTO toTicketDTO(Ticket t) {
        return TicketDTO.fromEntity(t, ticketDisplayStatus(t));
    }

    private String ticketDisplayStatus(Ticket t) {
        String status = t.getStatus();
        if ("HOLD".equals(status) && isHoldExpired(t.getBooking())) {
            status = "CANCELLED";
        }
        if ("VALID".equals(status) && "COMPLETED".equals(t.getTrip().getStatus())) {
            status = "PAST_TRIP";
        }
        return status;
    }
}
