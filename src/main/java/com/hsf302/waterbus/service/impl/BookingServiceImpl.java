package com.hsf302.waterbus.service.impl;

import com.hsf302.waterbus.dto.BookingDTO;
import com.hsf302.waterbus.dto.BookingTripDTO;
import com.hsf302.waterbus.dto.TicketDTO;
import com.hsf302.waterbus.entity.Booking;
import com.hsf302.waterbus.entity.Promotion;
import com.hsf302.waterbus.entity.RouteStation;
import com.hsf302.waterbus.entity.Seat;
import com.hsf302.waterbus.entity.Ticket;
import com.hsf302.waterbus.entity.TicketType;
import com.hsf302.waterbus.entity.Trip;
import com.hsf302.waterbus.repository.BookingRepository;
import com.hsf302.waterbus.repository.TicketRepository;
import com.hsf302.waterbus.service.AccountService;
import com.hsf302.waterbus.service.BookingService;
import com.hsf302.waterbus.service.PromotionService;
import com.hsf302.waterbus.service.RouteService;
import com.hsf302.waterbus.service.TicketTypeService;
import com.hsf302.waterbus.service.TripService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

// Module: BE4 - Đặt vé & Thanh toán.
@Service
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final TicketRepository ticketRepository;
    // Module khác: chỉ gọi qua Service, không gọi Repository của họ
    private final TripService tripService;
    private final RouteService routeService;
    private final TicketTypeService ticketTypeService;
    private final PromotionService promotionService;
    private final AccountService accountService; // module Tân: lấy tài khoản đang đăng nhập

    // Constructor injection: Spring tự đưa các repository/service vào
    public BookingServiceImpl(BookingRepository bookingRepository, TicketRepository ticketRepository,
                              TripService tripService, RouteService routeService,
                              TicketTypeService ticketTypeService, PromotionService promotionService,
                              AccountService accountService) {
        this.bookingRepository = bookingRepository;
        this.ticketRepository = ticketRepository;
        this.tripService = tripService;
        this.routeService = routeService;
        this.ticketTypeService = ticketTypeService;
        this.promotionService = promotionService;
        this.accountService = accountService;
    }

    @Override
    @Transactional // lưu 1 booking + nhiều ticket: lỗi giữa chừng thì huỷ hết, không lưu nửa vời
    public Booking createBooking(int tripId, List<Integer> seatIds, int ticketTypeId, int fromStationId, int toStationId, String promoCode, Integer accountId, String contactName, String contactPhone, String contactEmail) {
        // 1. Kiểm tra thông tin khách nhập
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
        seatIds = seatIds.stream().distinct().toList(); // bỏ ghế bị gửi trùng (nếu có)
        if (fromStationId == toStationId) {
            throw new IllegalArgumentException("Bến đi và bến đến phải khác nhau.");
        }

        // 2. Ghế: chỉ nhận ghế còn trống của đúng chuyến này (TripService của Trí)
        Map<Integer, Seat> availableSeats = new HashMap<>();
        for (Seat seat : tripService.getAvailableSeats(tripId)) {
            availableSeats.put(seat.getId(), seat);
        }
        if (!availableSeats.keySet().containsAll(seatIds)) {
            throw new IllegalArgumentException("Ghế vừa có người đặt. Vui lòng chọn ghế khác.");
        }

        // 3. Chuyến: phải tồn tại và còn đang mở bán
        Trip trip = tripService.getTripById(tripId);
        if (trip == null || !"SCHEDULED".equals(trip.getStatus())) {
            throw new IllegalArgumentException("Chuyến tàu này không còn nhận đặt vé.");
        }

        // 4. Bến: cả 2 bến phải nằm trên tuyến của chuyến, bến đi đứng trước bến đến (RouteService của Khuyên)
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

        // 5. Loại vé: phải đang bán (TicketTypeService của Tân)
        TicketType ticketType = null;
        for (TicketType type : ticketTypeService.getActiveTicketTypes()) {
            if (type.getId() == ticketTypeId) {
                ticketType = type;
            }
        }
        if (ticketType == null) {
            throw new IllegalArgumentException("Loại vé không hợp lệ.");
        }

        // 6. Tính tiền: giá vé lấy từ getPrice, giảm giá lấy từ calculateDiscount
        BigDecimal price = ticketTypeService.getPrice(ticketTypeId);
        BigDecimal subtotal = calculateSubtotal(ticketTypeId, seatIds.size());
        BigDecimal discount = promotionService.calculateDiscount(promoCode, subtotal);
        Promotion promotion = null;
        if (!isBlank(promoCode)) {
            // Có nhập mã mà không được giảm đồng nào -> báo cho khách biết thay vì lặng lẽ bỏ qua
            if (discount.compareTo(BigDecimal.ZERO) == 0) {
                throw new IllegalArgumentException("Mã khuyến mãi không hợp lệ, đã hết hạn hoặc đơn chưa đủ điều kiện.");
            }
            promotion = promotionService.getPromotionByCode(promoCode).orElse(null);
        }

        // 7. Lưu booking: trạng thái PENDING, giữ chỗ 10 phút
        String number = newBookingNumber();
        Booking booking = new Booking();
        booking.setBookingCode("BK" + number);
        booking.setPromotion(promotion);
        // Đã đăng nhập -> gắn tài khoản vào đơn. Chưa đăng nhập (accountId = null) -> khách vãng lai, account_id để NULL
        if (accountId != null) {
            booking.setAccount(accountService.getAccountById(accountId));
        }
        booking.setContactName(contactName.trim());
        booking.setContactPhone(contactPhone.trim());
        booking.setContactEmail(contactEmail.trim());
        booking.setSubtotal(subtotal);
        booking.setDiscountAmount(discount);
        booking.setTotalAmount(subtotal.subtract(discount)); // DB bắt buộc: total = subtotal - discount
        booking.setStatus("PENDING");
        booking.setCreatedAt(LocalDateTime.now());
        booking.setExpiresAt(booking.getCreatedAt().plusMinutes(10));
        bookingRepository.save(booking);

        // 8. Mỗi ghế 1 vé HOLD (giữ ghế tới khi thanh toán hoặc hết hạn)
        try {
            for (int i = 0; i < seatIds.size(); i++) {
                Ticket ticket = new Ticket();
                ticket.setTicketCode("TK" + number + "-" + (i + 1)); // VD: TK12345678-1, TK12345678-2
                ticket.setBooking(booking);
                ticket.setTrip(trip);
                ticket.setSeat(availableSeats.get(seatIds.get(i)));
                ticket.setTicketType(ticketType);
                ticket.setFromStation(fromStop.getStation());
                ticket.setToStation(toStop.getStation());
                ticket.setPrice(price); // chụp lại giá lúc mua
                ticket.setStatus("HOLD");
                ticketRepository.save(ticket);
            }
            // Force the unique active-seat index check before this method returns.
            ticketRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            if (isActiveSeatConflict(ex)) {
                throw new IllegalArgumentException("Ghế vừa có người đặt. Vui lòng chọn ghế khác.", ex);
            }
            throw ex;
        }
        return booking;
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
        // Lấy đơn của tài khoản (mới nhất trước) rồi đổi Entity -> DTO, giống getStations() của Station
        return bookingRepository.findByAccountIdOrderByCreatedAtDesc(accountId).stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    public BookingDTO getBookingDetail(String bookingCode) {
        return bookingRepository.findByBookingCode(bookingCode)
                .map(this::toDTO)
                .orElse(null); // không có đơn -> null, controller tự xử lý
    }

    @Override
    @Transactional(readOnly = true) // giữ kết nối DB mở để đọc ghế, bến (quan hệ LAZY) của từng vé
    public List<TicketDTO> getTicketsOfBooking(int bookingId) {
        return ticketRepository.findByBookingIdOrderByIdAsc(bookingId).stream()
                .map(this::toTicketDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true) // giữ kết nối DB mở để đọc chuyến, tuyến (quan hệ LAZY)
    public BookingTripDTO getTripOfBooking(int bookingId) {
        List<Ticket> tickets = ticketRepository.findByBookingIdOrderByIdAsc(bookingId);
        if (tickets.isEmpty()) {
            return null;
        }
        // Mọi vé trong 1 đơn cùng 1 chuyến -> lấy chuyến của vé đầu tiên
        return toTripDTO(tickets.get(0).getTrip());
    }

    @Override
    public List<Seat> getSelectedSeats(int tripId, List<Integer> seatIds) {
        if (seatIds == null) {
            return List.of();
        }
        // Lọc trong danh sách ghế còn trống -> ghế vừa bị người khác đặt sẽ không hiện nữa
        return tripService.getAvailableSeats(tripId).stream()
                .filter(seat -> seatIds.contains(seat.getId()))
                .toList();
    }

    @Override
    public BigDecimal calculateSubtotal(int ticketTypeId, int seatCount) {
        return ticketTypeService.getPrice(ticketTypeId).multiply(BigDecimal.valueOf(seatCount));
    }

    @Override
    public BookingTripDTO getTripInfo(int tripId) {
        // Dùng hợp đồng của Trí: null nếu không có chuyến hoặc chuyến không còn SCHEDULED
        return tripService.getBookableTrip(tripId);
    }

    @Override
    public boolean areSeatsAvailable(int tripId, List<Integer> seatIds) {
        // Hỏi TripService (của Trí) từng ghế một, chỉ cần 1 ghế đã bị đặt là trả false
        for (Integer seatId : seatIds) {
            if (!tripService.isSeatAvailable(tripId, seatId)) {
                return false;
            }
        }
        return true;
    }

    @Override
    @Transactional // đổi nhiều vé cùng lúc: lỗi giữa chừng thì huỷ hết
    public void releaseHoldTickets(int bookingId) {
        // Lấy tất cả vé của đơn
        List<Ticket> tickets = ticketRepository.findByBookingIdOrderByIdAsc(bookingId);
        for (Ticket ticket : tickets) {
            // Chỉ đổi vé đang giữ chỗ. Vé VALID/USED (đã trả tiền / đã lên tàu) giữ nguyên
            if ("HOLD".equals(ticket.getStatus())) {
                // CANCELLED không bị unique index UX_ticket_trip_seat_active tính -> ghế được nhả cho người khác
                ticket.setStatus("CANCELLED");
                ticketRepository.save(ticket);
            }
        }
    }

    @Override
    @Transactional // đổi booking + nhiều vé: lỗi giữa chừng thì huỷ hết
    public void cancelBooking(String bookingCode, Integer accountId) {
        // 1. Tìm đơn theo mã
        Booking booking = bookingRepository.findByBookingCode(bookingCode).orElse(null);
        if (booking == null) {
            throw new IllegalArgumentException("Không tìm thấy đơn đặt vé.");
        }
        // 2. Chỉ huỷ được đơn đang chờ thanh toán (PAID/EXPIRED/CANCELLED thì không)
        if (!"PENDING".equals(booking.getStatus())) {
            throw new IllegalArgumentException("Chỉ huỷ được đơn chưa thanh toán.");
        }
        // 3. Đơn gắn với tài khoản -> chỉ đúng tài khoản đó được huỷ.
        //    Đơn khách vãng lai (account = null) -> ai có mã đơn cũng huỷ được.
        if (booking.getAccount() != null && !booking.getAccount().getId().equals(accountId)) {
            throw new IllegalArgumentException("Bạn không có quyền huỷ đơn này.");
        }
        // 4. Hợp lệ -> huỷ đơn rồi nhả ghế (dùng lại method ở trên)
        booking.setStatus("CANCELLED");
        bookingRepository.save(booking);
        releaseHoldTickets(booking.getId());
    }

    // Sinh 8 chữ số ngẫu nhiên cho mã đơn, thử lại nếu mã đã có trong DB
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
        return new BookingDTO(b.getId(), b.getBookingCode(), b.getStatus(), b.getContactName(),
                b.getContactPhone(), b.getContactEmail(), b.getSubtotal(), b.getDiscountAmount(),
                b.getTotalAmount(), b.getCreatedAt(), b.getExpiresAt(), b.getPaidAt());
    }

    private TicketDTO toTicketDTO(Ticket t) {
        // Lấy mã ghế và TÊN bến từ các entity liên quan
        return new TicketDTO(t.getTicketCode(), t.getSeat().getSeatCode(), t.getFromStation().getName(),
                t.getToStation().getName(), t.getPrice(), t.getStatus());
    }

    private BookingTripDTO toTripDTO(Trip trip) {
        return new BookingTripDTO(trip.getId(), trip.getTripCode(), trip.getRoute().getName(),
                trip.getDepartureTime(), trip.getArrivalTime());
    }
}
