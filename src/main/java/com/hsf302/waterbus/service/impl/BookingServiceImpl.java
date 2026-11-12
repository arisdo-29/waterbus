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

// Module: BE4 - Đặt vé & Thanh toán.
@Service
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final TicketRepository ticketRepository;
    // Module khác: chỉ gọi qua Service, không gọi Repository của họ
    private final TripService tripService;
    private final RefundService refundService; // module khác: trạng thái hoàn tiền khi chuyến bị huỷ
    private final RouteService routeService;
    private final TicketTypeService ticketTypeService;
    private final PromotionService promotionService;
    private final AccountService accountService; // module Tân: lấy tài khoản đang đăng nhập

    // Constructor injection: Spring tự đưa các repository/service vào
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
    @Transactional // lưu 1 booking + nhiều ticket: lỗi giữa chừng thì huỷ hết, không lưu nửa vời
    public BookingDTO createBooking(BookingForm form, Integer accountId) {
        // Form thiếu chuyến/loại vé/bến (bị sửa tay) -> báo lỗi rõ ràng thay vì NullPointerException
        // (controller đã chặn trước bằng lỗi 400; kiểm tra lại vì service có thể được gọi từ chỗ khác)
        if (form.getTripId() == null || form.getTicketTypeId() == null
                || form.getFromStationId() == null || form.getToStationId() == null) {
            throw new IllegalArgumentException("Thiếu thông tin chuyến, loại vé hoặc bến.");
        }
        // Lấy dữ liệu trên form ra biến TRÙNG TÊN tham số cũ -> các bước 1-9 bên dưới giữ nguyên.
        // Vé thường: form không có họ tên/năm sinh từng hành khách -> passengerNames, birthYears = null
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
        // Đếm SAU khi bỏ ghế trùng để đếm đúng số ghế thật. BookingForm (@Size) đã chặn ở controller,
        // kiểm tra lại ở đây vì service có thể được gọi từ chỗ khác không đi qua form
        if (seatIds.size() > BookingService.MAX_SEATS_PER_BOOKING) {
            throw new IllegalArgumentException("Bạn chỉ có thể chọn tối đa "
                    + BookingService.MAX_SEATS_PER_BOOKING + " ghế trong một lần đặt.");
        }
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
        // Kiểm tra giờ ở ĐÂY (sau khi có fromStop) vì phải biết bến khách lên mới tính được giờ tàu tới bến đó
        if (isOnlineSalesClosed(trip, fromStop)) {
            throw new IllegalArgumentException("Chuyến tàu sắp khởi hành, đã ngừng bán vé online.");
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
        // Vé ưu đãi: phải đăng nhập và tối đa 2 vé mỗi đơn (xem checkPriorityTicketRules)
        if (Boolean.TRUE.equals(ticketType.getRequiresVerification())) {
            checkPriorityTicketRules(accountId, seatIds.size());
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

        // 7. Lưu booking: trạng thái PENDING, giữ chỗ 10 phút.
        // Riêng đơn 0 đ (vé ưu đãi miễn phí, hoặc mã KM giảm hết) -> không cần trả tiền -> PAID luôn
        BigDecimal total = subtotal.subtract(discount);
        boolean isFree = total.compareTo(BigDecimal.ZERO) == 0;
        String number = newBookingNumber();
        // Phần khách nhập (họ tên, SĐT, email đã bỏ khoảng trắng) do form tự chép sang entity
        Booking booking = form.toEntity();
        // Phần còn lại do hệ thống tính ra -> service set
        booking.setBookingCode("BK" + number);
        booking.setPromotion(promotion);
        // Đã đăng nhập -> gắn tài khoản vào đơn. Chưa đăng nhập (accountId = null) -> khách vãng lai, account_id để NULL
        if (accountId != null) {
            booking.setAccount(accountService.getAccountEntityById(accountId));
        }
        booking.setSubtotal(subtotal);
        booking.setDiscountAmount(discount);
        booking.setTotalAmount(total); // DB bắt buộc: total = subtotal - discount
        booking.setCreatedAt(LocalDateTime.now());
        booking.setExpiresAt(booking.getCreatedAt().plusMinutes(10)); // cột NOT NULL nên đơn 0 đ vẫn ghi
        if (isFree) {
            booking.setStatus("PAID");
            booking.setPaidAt(booking.getCreatedAt()); // KHÔNG tạo payment vì không có tiền nào được trả
        } else {
            booking.setStatus("PENDING");
        }
        bookingRepository.save(booking);

        // 8. Mỗi ghế 1 vé: HOLD (giữ ghế tới khi thanh toán hoặc hết hạn), đơn 0 đ thì VALID dùng được ngay
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
                ticket.setStatus(isFree ? "VALID" : "HOLD");
                // Vé ưu đãi: hành khách thứ i ngồi ghế thứ i (vé thường không gửi -> để null)
                if (passengerNames != null && i < passengerNames.size() && !isBlank(passengerNames.get(i))) {
                    ticket.setPassengerName(passengerNames.get(i).trim());
                }
                if (birthYears != null && i < birthYears.size()) {
                    ticket.setBirthYear(birthYears.get(i));
                }
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

        // 9. Đơn 0 đ đã coi như thanh toán xong -> có dùng mã KM thì tăng lượt dùng (giống confirmPaid)
        if (isFree && promotion != null) {
            promotionService.increaseUsedCount(promotion.getCode());
        }
        return toDTO(booking); // trả DTO cho controller, entity không ra khỏi service
    }

    // Luật riêng của vé ưu đãi (loại vé cần xác minh giấy tờ, thường miễn phí -> đơn PAID ngay, không phải trả tiền).
    // Không chặn thì khách vãng lai giữ được hàng loạt ghế miễn phí mà không tốn gì và không để lại danh tính.
    private void checkPriorityTicketRules(Integer accountId, int seatCount) {
        // accountId = null nghĩa là khách vãng lai (chưa đăng nhập)
        if (accountId == null) {
            throw new IllegalArgumentException("Vui lòng đăng nhập để đặt vé ưu đãi.");
        }
        if (seatCount > BookingService.MAX_PRIORITY_TICKETS_PER_BOOKING) {
            throw new IllegalArgumentException("Mỗi đơn chỉ được đặt tối đa "
                    + BookingService.MAX_PRIORITY_TICKETS_PER_BOOKING + " vé ưu đãi.");
        }
    }

    // Đã ngừng bán online chưa: khi tàu tới bến khách lên chỉ còn ONLINE_SALES_CUTOFF_MINUTES phút
    // (hoặc ít hơn), hoặc đã qua bến đó. Dùng chung hằng số và cách tính với trang tìm chuyến của Trí,
    // để chuyến nào còn hiện trong kết quả tìm thì vẫn đặt được, hết hiện thì cũng hết đặt.
    private boolean isOnlineSalesClosed(Trip trip, RouteStation fromStop) {
        // Giờ tàu tới bến đi = giờ rời bến đầu tuyến + số phút từ bến đầu tới bến đó
        int minutesFromStart = 0;
        if (fromStop.getMinutesFromStart() != null) {
            minutesFromStart = fromStop.getMinutesFromStart();
        }
        LocalDateTime boardingTime = trip.getDepartureTime().plusMinutes(minutesFromStart);
        LocalDateTime stopSellingAt = LocalDateTime.now().plusMinutes(TripService.ONLINE_SALES_CUTOFF_MINUTES);
        // Giờ tới bến phải SAU mốc ngừng bán thì mới còn bán
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
        return BookingTripDTO.fromEntity(tickets.get(0).getTrip()); // Entity -> DTO do BookingTripDTO lo (B2 của Trí)
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
        // Lọc trong danh sách ghế còn trống -> ghế vừa bị người khác đặt sẽ không hiện nữa
        List<SeatDTO> selectedSeats = new ArrayList<>();
        for (Seat seat : tripService.getAvailableSeats(tripId)) {
            if (seatIds.contains(seat.getId())) {
                // Entity -> DTO do SeatDTO.fromEntity của Trí lo (B3), để controller không phải nhận entity Seat.
                // available = true vì ghế lấy từ danh sách ghế còn trống.
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
        // Dùng bản có bến của Trí (giống trang chọn ghế): giờ hiển thị là giờ tại bến khách chọn,
        // và mốc ngừng bán online tính theo giờ tàu tới bến đi, khớp với createBooking
        return tripService.getBookableTrip(tripId, fromStationId, toStationId);
    }

    @Override
    public void validatePassengers(BookingForm form, Errors errors) {
        // Thiếu chuyến/loại vé/bến -> Controller đã tự xử lý trước, ở đây bỏ qua
        if (form.getTripId() == null || form.getTicketTypeId() == null
                || form.getFromStationId() == null || form.getToStationId() == null) {
            return;
        }
        // Chỉ kiểm tra vé ưu đãi (requiresVerification = true). Vé thường -> không cần họ tên, năm sinh
        TicketTypeDTO ticketType = ticketTypeService.getTicketTypeById(form.getTicketTypeId());
        if (ticketType == null || !Boolean.TRUE.equals(ticketType.getRequiresVerification())) {
            return;
        }
        BookingTripDTO trip = getTripInfo(form.getTripId(), form.getFromStationId(), form.getToStationId());
        if (trip == null) {
            return;
        }

        // 1. Mỗi ghế phải có đúng 1 họ tên và 1 năm sinh (thứ tự giống seatIds)
        int seatCount = (form.getSeatIds() == null) ? 0 : form.getSeatIds().size();
        List<String> names = form.getPassengerNames();
        List<Integer> years = form.getBirthYears();
        if (names == null || years == null || names.size() != seatCount || years.size() != seatCount) {
            // Lỗi của cả danh sách -> template hiện ở cuối khung "Thông tin hành khách"
            errors.rejectValue("passengerNames", "passengers.count",
                    "Vui lòng nhập họ tên và năm sinh cho từng ghế.");
            return; // số lượng sai thì không kiểm tra từng ghế được
        }

        // 2. Kiểm tra từng ghế thứ i: lỗi ghi vào đúng ô passengerNames[i] / birthYears[i]
        int currentYear = LocalDate.now().getYear();
        int travelYear = trip.getDepartureTime().getYear(); // tuổi tính theo năm tàu chạy
        for (int i = 0; i < seatCount; i++) {
            String name = names.get(i);
            if (isBlank(name)) {
                errors.rejectValue("passengerNames[" + i + "]", "passenger.name",
                        "Vui lòng nhập họ tên hành khách.");
            } else if (name.trim().length() > 100) {
                // Cột passenger_name trong DB chỉ chứa 100 ký tự
                errors.rejectValue("passengerNames[" + i + "]", "passenger.name",
                        "Họ tên hành khách tối đa 100 ký tự.");
            }

            Integer year = years.get(i);
            if (year == null || year < 1900 || year > currentYear) {
                errors.rejectValue("birthYears[" + i + "]", "passenger.birthYear",
                        "Năm sinh phải từ 1900 đến " + currentYear + ".");
                continue; // năm sinh sai thì không tính tuổi nữa
            }
            int age = travelYear - year;
            if ("SENIOR_70".equals(ticketType.getCode()) && age < 70) {
                errors.rejectValue("birthYears[" + i + "]", "passenger.age",
                        "Vé người cao tuổi chỉ dành cho hành khách từ 70 tuổi trở lên.");
            }
            // Vé ưu tiên (PRIORITY: người khuyết tật, thương binh) KHÔNG giới hạn tuổi: nhóm không có vé trẻ em,
            // nên nếu chặn dưới 13 tuổi thì trẻ khuyết tật không mua được vé miễn phí nào.
            // Nhân viên vẫn xác minh giấy tờ khi khách lên tàu.
        }
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

    @Override
    @Transactional(readOnly = true) // đọc tài khoản chủ đơn (quan hệ LAZY)
    public ViewAccess checkViewAccess(String bookingCode, Integer loginAccountId, String loginRole,
                                      Set<String> viewableBookingCodes) {
        Booking booking = bookingRepository.findByBookingCode(bookingCode).orElse(null);
        if (booking == null) {
            return ViewAccess.NOT_FOUND;
        }
        // Nhân viên, quản trị xem được mọi đơn (soát vé, đặt hộ, hỗ trợ khách tại bến)
        if ("STAFF".equalsIgnoreCase(loginRole) || "ADMIN".equalsIgnoreCase(loginRole)) {
            return ViewAccess.ALLOWED;
        }
        // Đơn gắn tài khoản -> chỉ chính chủ đơn
        if (booking.getAccount() != null) {
            if (loginAccountId == null) {
                return ViewAccess.NEED_LOGIN;
            }
            if (booking.getAccount().getId().equals(loginAccountId)) {
                return ViewAccess.ALLOWED;
            }
            return ViewAccess.NOT_OWNER;
        }
        // Đơn khách vãng lai -> chỉ trình duyệt đã được phép (vừa đặt đơn này, hoặc đã nhập đúng SĐT)
        if (viewableBookingCodes != null && viewableBookingCodes.contains(booking.getBookingCode())) {
            return ViewAccess.ALLOWED;
        }
        return ViewAccess.NEED_PHONE;
    }

    @Override
    public boolean verifyContactPhone(String bookingCode, String contactPhone) {
        Booking booking = bookingRepository.findByBookingCode(bookingCode).orElse(null);
        // Chỉ đơn khách vãng lai mới xác minh bằng SĐT; đơn của tài khoản thì phải đăng nhập
        if (booking == null || booking.getAccount() != null || isBlank(contactPhone)) {
            return false;
        }
        // SĐT trong DB đã được bỏ khoảng trắng lúc đặt -> so với SĐT khách nhập (cũng bỏ khoảng trắng)
        return contactPhone.trim().equals(booking.getContactPhone());
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

    // Đổi 1 đơn sang DTO cho controller. Việc chép field nằm trong BookingDTO.fromEntity;
    // ở đây chỉ tính 2 trạng thái (là nghiệp vụ nên phải ở service) rồi truyền vào.
    private BookingDTO toDTO(Booking b) {
        return BookingDTO.fromEntity(b, bookingDisplayStatus(b), refundStatusOf(b));
    }

    private String refundStatusOf(Booking b) {
        // Trạng thái hoàn tiền (PENDING = chờ hoàn, DONE = đã hoàn) để trang "Vé của tôi" hiện nhãn đúng.
        // Refund là module khác -> hỏi qua RefundService, không gọi thẳng RefundRepository
        String refundStatus = null; // chuyến không bị huỷ thì đơn không có khoản hoàn nào
        Refund refund = refundService.getRefundOfBooking(b.getId());
        if (refund != null) {
            refundStatus = refund.getStatus();
        }
        return refundStatus;
    }

    private String bookingDisplayStatus(Booking b) {
        // Đơn đã quá hạn giữ chỗ nhưng job chưa kịp đổi trong DB -> hiển thị luôn là hết hạn
        String status = b.getStatus();
        if (isHoldExpired(b)) {
            status = "EXPIRED";
        }
        return status;
    }

    // Đơn còn PENDING trong DB mà đã quá giờ giữ chỗ. BookingExpiryJob chạy 1 phút/lần mới đổi đơn sang
    // EXPIRED (và nhả ghế), nên trong lúc chờ job, trang vẫn phải hiện là hết hạn chứ không mời khách trả tiền.
    // Chỉ đổi chữ HIỂN THỊ trong DTO, không ghi DB: việc ghi DB + nhả ghế để job làm, tránh làm 2 nơi.
    private boolean isHoldExpired(Booking b) {
        if (!"PENDING".equals(b.getStatus()) || b.getExpiresAt() == null) {
            return false;
        }
        return b.getExpiresAt().isBefore(LocalDateTime.now());
    }

    // Đổi 1 vé sang DTO: chép field do TicketDTO.fromEntity lo (mã ghế, tên bến, requiresVerification...);
    // ở đây chỉ tính trạng thái hiển thị (nghiệp vụ) rồi truyền vào.
    private TicketDTO toTicketDTO(Ticket t) {
        return TicketDTO.fromEntity(t, ticketDisplayStatus(t));
    }

    private String ticketDisplayStatus(Ticket t) {
        // Vé đang giữ chỗ của đơn đã quá hạn: job sẽ huỷ vé (CANCELLED) -> hiển thị trước như vậy,
        // để trang không vừa ghi "Đơn đã hết hạn" vừa ghi vé "Đang giữ chỗ"
        String status = t.getStatus();
        if ("HOLD".equals(status) && isHoldExpired(t.getBooking())) {
            status = "CANCELLED";
        }
        return status;
    }
}
