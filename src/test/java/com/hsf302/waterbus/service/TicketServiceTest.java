package com.hsf302.waterbus.service;

import com.hsf302.waterbus.dto.TicketCheckinDTO;
import com.hsf302.waterbus.entity.*;
import com.hsf302.waterbus.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class TicketServiceTest {

    @Autowired
    private TicketService ticketService;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private WaterbusRepository waterbusRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private RouteRepository routeRepository;

    @Autowired
    private StationRepository stationRepository;

    @Autowired
    private TicketTypeRepository ticketTypeRepository;

    private Ticket validTicket;
    private Ticket usedTicket;
    private Ticket holdTicket;
    private Ticket cancelledTicket;

    @BeforeEach
    void setUp() {
        // Tạo các entity liên quan
        Station st1 = new Station();
        st1.setCode("ST_TEST_1");
        st1.setName("Bến Test 1");
        st1.setIsActive(true);
        st1 = stationRepository.save(st1);

        Station st2 = new Station();
        st2.setCode("ST_TEST_2");
        st2.setName("Bến Test 2");
        st2.setIsActive(true);
        st2 = stationRepository.save(st2);

        Route route = new Route();
        route.setCode("RT_TEST_1");
        route.setName("Tuyến Test 1");
        route.setIsActive(true);
        route = routeRepository.save(route);

        Waterbus waterbus = new Waterbus();
        waterbus.setCode("WB_TEST_1");
        waterbus.setName("Tàu Test 1");
        waterbus.setCapacity(50);
        waterbus.setStatus("ACTIVE");
        waterbus = waterbusRepository.save(waterbus);

        Seat seat = new Seat();
        seat.setWaterbus(waterbus);
        seat.setSeatCode("A1");
        seat.setSeatRow(1);
        seat.setSeatCol(1);
        seat.setIsActive(true);
        seat = seatRepository.save(seat);

        Trip trip = new Trip();
        trip.setTripCode("TRIP_TEST_1");
        trip.setRoute(route);
        trip.setWaterbus(waterbus);
        trip.setDepartureTime(LocalDateTime.now().plusMinutes(30));
        trip.setArrivalTime(LocalDateTime.now().plusMinutes(75));
        trip.setStatus("SCHEDULED");
        trip = tripRepository.save(trip);

        TicketType ticketType = new TicketType();
        ticketType.setCode("TT_TEST_1");
        ticketType.setName("Vé Tiêu Chuẩn Test");
        ticketType.setPrice(BigDecimal.valueOf(15000));
        ticketType.setRequiresVerification(false);
        ticketType.setIsActive(true);
        ticketType = ticketTypeRepository.save(ticketType);

        Booking booking = new Booking();
        booking.setBookingCode("BK_TEST_1");
        booking.setContactName("Nguyễn Văn A");
        booking.setContactPhone("0901234567");
        booking.setContactEmail("vana@test.com");
        booking.setSubtotal(BigDecimal.valueOf(15000));
        booking.setDiscountAmount(BigDecimal.ZERO);
        booking.setTotalAmount(BigDecimal.valueOf(15000));
        booking.setStatus("PAID");
        booking.setCreatedAt(LocalDateTime.now());
        booking.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        booking.setPaidAt(LocalDateTime.now());
        booking = bookingRepository.save(booking);

        // 1. Vé hợp lệ (VALID)
        validTicket = new Ticket();
        validTicket.setTicketCode("TK_VALID_01");
        validTicket.setBooking(booking);
        validTicket.setTrip(trip);
        validTicket.setSeat(seat);
        validTicket.setTicketType(ticketType);
        validTicket.setFromStation(st1);
        validTicket.setToStation(st2);
        validTicket.setPrice(BigDecimal.valueOf(15000));
        validTicket.setStatus("VALID");
        validTicket = ticketRepository.save(validTicket);

        // 2. Vé đã dùng (USED)
        usedTicket = new Ticket();
        usedTicket.setTicketCode("TK_USED_01");
        usedTicket.setBooking(booking);
        usedTicket.setTrip(trip);
        usedTicket.setSeat(seat);
        usedTicket.setTicketType(ticketType);
        usedTicket.setFromStation(st1);
        usedTicket.setToStation(st2);
        usedTicket.setPrice(BigDecimal.valueOf(15000));
        usedTicket.setStatus("USED");
        usedTicket.setCheckedInAt(LocalDateTime.now().minusMinutes(30));
        usedTicket = ticketRepository.save(usedTicket);

        // 3. Vé đang giữ chỗ chưa thanh toán (HOLD)
        holdTicket = new Ticket();
        holdTicket.setTicketCode("TK_HOLD_01");
        holdTicket.setBooking(booking);
        holdTicket.setTrip(trip);
        holdTicket.setSeat(seat);
        holdTicket.setTicketType(ticketType);
        holdTicket.setFromStation(st1);
        holdTicket.setToStation(st2);
        holdTicket.setPrice(BigDecimal.valueOf(15000));
        holdTicket.setStatus("HOLD");
        holdTicket = ticketRepository.save(holdTicket);

        // 4. Vé đã hủy (CANCELLED)
        cancelledTicket = new Ticket();
        cancelledTicket.setTicketCode("TK_CANCELLED_01");
        cancelledTicket.setBooking(booking);
        cancelledTicket.setTrip(trip);
        cancelledTicket.setSeat(seat);
        cancelledTicket.setTicketType(ticketType);
        cancelledTicket.setFromStation(st1);
        cancelledTicket.setToStation(st2);
        cancelledTicket.setPrice(BigDecimal.valueOf(15000));
        cancelledTicket.setStatus("CANCELLED");
        cancelledTicket = ticketRepository.save(cancelledTicket);
    }

    @Test
    @DisplayName("Soát vé thành công khi vé ở trạng thái VALID")
    void checkIn_validTicket_success() {
        TicketCheckinDTO result = ticketService.checkIn("TK_VALID_01");

        assertNotNull(result);
        assertEquals("TK_VALID_01", result.getTicketCode());
        assertEquals("USED", result.getStatus());
        assertNotNull(result.getCheckedInAt());
        assertEquals("Nguyễn Văn A", result.getPassengerName());
        assertEquals("A1", result.getSeatCode());
        assertEquals("Tàu Test 1", result.getWaterbusName());
        assertEquals("Bến Test 1", result.getFromStationName());
        assertEquals("Bến Test 2", result.getToStationName());

        // Kiểm tra trong database thực tế
        Ticket inDb = ticketRepository.findByTicketCode("TK_VALID_01").orElseThrow();
        assertEquals("USED", inDb.getStatus());
        assertNotNull(inDb.getCheckedInAt());
    }

    @Test
    @DisplayName("Không tìm thấy mã vé -> ném ngoại lệ IllegalArgumentException")
    void checkIn_ticketNotFound_throwsException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            ticketService.checkIn("TK_KHONG_TON_TAI");
        });
        assertTrue(ex.getMessage().contains("Không tìm thấy vé"));
    }

    @Test
    @DisplayName("Mã vé rỗng hoặc null -> ném ngoại lệ IllegalArgumentException")
    void checkIn_emptyCode_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> ticketService.checkIn(null));
        assertThrows(IllegalArgumentException.class, () -> ticketService.checkIn("   "));
    }

    @Test
    @DisplayName("Vé đã soát trước đó (USED) -> ném ngoại lệ IllegalStateException")
    void checkIn_alreadyUsed_throwsException() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            ticketService.checkIn("TK_USED_01");
        });
        assertTrue(ex.getMessage().contains("đã được soát"));
    }

    @Test
    @DisplayName("Vé chưa thanh toán (HOLD) -> ném ngoại lệ IllegalStateException")
    void checkIn_holdTicket_throwsException() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            ticketService.checkIn("TK_HOLD_01");
        });
        assertTrue(ex.getMessage().contains("chưa được thanh toán"));
    }

    @Test
    @DisplayName("Vé đã bị hủy (CANCELLED) -> ném ngoại lệ IllegalStateException")
    void checkIn_cancelledTicket_throwsException() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            ticketService.checkIn("TK_CANCELLED_01");
        });
        assertTrue(ex.getMessage().contains("đã bị hủy"));
    }

    @Test
    @DisplayName("Vé của chuyến tàu đã bị hủy (CANCELLED) -> ném ngoại lệ IllegalStateException")
    void checkIn_cancelledTrip_throwsException() {
        // Cập nhật trip sang CANCELLED
        Trip trip = validTicket.getTrip();
        trip.setStatus("CANCELLED");
        tripRepository.save(trip);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            ticketService.checkIn("TK_VALID_01");
        });
        assertTrue(ex.getMessage().contains("Chuyến tàu") && ex.getMessage().contains("đã bị hủy"));
    }

    @Test
    @DisplayName("Vé của đơn đặt vé đã bị hủy (CANCELLED) hoặc hoàn tiền (REFUNDED) -> ném ngoại lệ IllegalStateException")
    void checkIn_cancelledOrRefundedBooking_throwsException() {
        // 1. Đơn bị CANCELLED
        Booking booking = validTicket.getBooking();
        booking.setStatus("CANCELLED");
        bookingRepository.save(booking);

        IllegalStateException ex1 = assertThrows(IllegalStateException.class, () -> {
            ticketService.checkIn("TK_VALID_01");
        });
        assertTrue(ex1.getMessage().contains("đã bị hủy"));

        // 2. Đơn bị REFUNDED
        booking.setStatus("REFUNDED");
        bookingRepository.save(booking);

        IllegalStateException ex2 = assertThrows(IllegalStateException.class, () -> {
            ticketService.checkIn("TK_VALID_01");
        });
        assertTrue(ex2.getMessage().contains("đã được hoàn tiền"));
    }

    @Test
    @DisplayName("Tra cứu thông tin vé không làm thay đổi trạng thái hay thời gian soát vé")
    void getTicketInfo_success() {
        TicketCheckinDTO info = ticketService.getTicketInfo("TK_VALID_01");

        assertNotNull(info);
        assertEquals("TK_VALID_01", info.getTicketCode());
        assertEquals("VALID", info.getStatus());
        assertNull(info.getCheckedInAt());

        // Database vẫn là VALID
        Ticket inDb = ticketRepository.findByTicketCode("TK_VALID_01").orElseThrow();
        assertEquals("VALID", inDb.getStatus());
        assertNull(inDb.getCheckedInAt());
    }

    @Test
    @DisplayName("Tra cứu đơn cả nhóm (getBookingCheckinInfo) -> trả về thông tin đơn và danh sách vé")
    void getBookingCheckinInfo_success() {
        var groupInfo = ticketService.getBookingCheckinInfo("BK_TEST_1", 1, "MANUAL");

        assertNotNull(groupInfo);
        assertEquals("BK_TEST_1", groupInfo.getBookingCode());
        assertFalse(groupInfo.getTickets().isEmpty());
    }

    @Test
    @DisplayName("Soát vé cả nhóm (checkInGroup) -> các vé hợp lệ được chuyển sang USED")
    void checkInGroup_success() {
        var results = ticketService.checkInGroup(java.util.List.of("TK_VALID_01"), 1, "GROUP");

        assertEquals(1, results.size());
        assertEquals("USED", results.get(0).getStatus());
        assertNotNull(results.get(0).getCheckedInAt());
    }

    @Test
    @DisplayName("Từ chối vé ưu đãi (rejectPriorityTicket) -> chuyển sang CANCELLED và nhả ghế")
    void rejectPriorityTicket_success() {
        TicketCheckinDTO rejected = ticketService.rejectPriorityTicket("TK_VALID_01", "Không mang CCCD", 1);

        assertEquals("CANCELLED", rejected.getStatus());
        Ticket inDb = ticketRepository.findByTicketCode("TK_VALID_01").orElseThrow();
        assertEquals("CANCELLED", inDb.getStatus());
    }

    @Test
    @DisplayName("Lấy danh sách hành khách theo chuyến (getPassengerManifest) -> trả về đầy đủ các vé")
    void getPassengerManifest_success() {
        var manifest = ticketService.getPassengerManifest(validTicket.getTrip().getId());

        assertNotNull(manifest);
        assertFalse(manifest.isEmpty());
        assertTrue(manifest.stream().anyMatch(t -> "TK_VALID_01".equals(t.getTicketCode())));
    }

    @Test
    @DisplayName("Soát vé chuyến ngày khác -> ném ngoại lệ IllegalStateException yêu cầu Override")
    void checkIn_differentDate_throwsException() {
        Trip trip = validTicket.getTrip();
        trip.setDepartureTime(LocalDateTime.now().plusDays(1));
        tripRepository.save(trip);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            ticketService.checkIn("TK_VALID_01");
        });
        assertTrue(ex.getMessage().contains("không phải hôm nay"));
    }

    @Test
    @DisplayName("Soát vé quá sớm (> 60 phút trước khởi hành) -> ném ngoại lệ IllegalStateException")
    void checkIn_tooEarly_throwsException() {
        Trip trip = validTicket.getTrip();
        trip.setDepartureTime(LocalDateTime.now().plusHours(3));
        tripRepository.save(trip);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            ticketService.checkIn("TK_VALID_01");
        });
        assertTrue(ex.getMessage().contains("Chỉ được soát vé trước giờ khởi hành tối đa 60 phút"));
    }

    @Test
    @DisplayName("Soát vé quá sớm nhưng có xác nhận Override -> soát vé thành công")
    void checkIn_overrideTime_success() {
        Trip trip = validTicket.getTrip();
        trip.setDepartureTime(LocalDateTime.now().plusHours(3));
        tripRepository.save(trip);

        TicketCheckinDTO result = ticketService.checkIn("TK_VALID_01", 1, "MANUAL", true);
        assertNotNull(result);
        assertEquals("USED", result.getStatus());
    }
}
