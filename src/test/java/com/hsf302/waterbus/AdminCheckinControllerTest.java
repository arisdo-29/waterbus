package com.hsf302.waterbus;

import com.hsf302.waterbus.dto.AccountDTO;
import com.hsf302.waterbus.entity.*;
import com.hsf302.waterbus.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
class AdminCheckinControllerTest {

    @Autowired
    private WebApplicationContext context;

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

    @Autowired
    private AccountRepository accountRepository;

    private MockMvc mvc;
    private AccountDTO staffAccount;
    private AccountDTO customerAccount;
    private Ticket validTicket;
    private Ticket priorityTicket;
    private Trip trip;
    private Booking booking;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();

        Account staff = new Account();
        staff.setFullName("Nhân Viên Bến");
        staff.setEmail("staff@waterbus.vn");
        staff.setPhone("0901234567");
        staff.setPasswordHash("hash");
        staff.setRole("STAFF");
        staff.setStatus("ACTIVE");
        staff.setCreatedAt(LocalDateTime.now());
        staff = accountRepository.save(staff);

        staffAccount = new AccountDTO(staff.getId(), "staff@waterbus.vn", "Nhân Viên Bến", "0901234567", "STAFF", "ACTIVE");
        customerAccount = new AccountDTO(999, "customer@test.com", "Khách Hàng", "0909876543", "CUSTOMER", "ACTIVE");

        Station st1 = new Station();
        st1.setCode("ST_CK_1");
        st1.setName("Bến 1");
        st1.setIsActive(true);
        st1 = stationRepository.save(st1);

        Station st2 = new Station();
        st2.setCode("ST_CK_2");
        st2.setName("Bến 2");
        st2.setIsActive(true);
        st2 = stationRepository.save(st2);

        Route route = new Route();
        route.setCode("RT_CK_1");
        route.setName("Tuyến 1");
        route.setIsActive(true);
        route = routeRepository.save(route);

        Waterbus waterbus = new Waterbus();
        waterbus.setCode("WB_CK_1");
        waterbus.setName("Tàu 1");
        waterbus.setCapacity(50);
        waterbus.setStatus("ACTIVE");
        waterbus = waterbusRepository.save(waterbus);

        Seat seat1 = new Seat();
        seat1.setWaterbus(waterbus);
        seat1.setSeatCode("B2");
        seat1.setSeatRow(2);
        seat1.setSeatCol(2);
        seat1.setIsActive(true);
        seat1 = seatRepository.save(seat1);

        Seat seat2 = new Seat();
        seat2.setWaterbus(waterbus);
        seat2.setSeatCode("B3");
        seat2.setSeatRow(2);
        seat2.setSeatCol(3);
        seat2.setIsActive(true);
        seat2 = seatRepository.save(seat2);

        trip = new Trip();
        trip.setTripCode("TRIP_CK_1");
        trip.setRoute(route);
        trip.setWaterbus(waterbus);
        trip.setDepartureTime(LocalDateTime.now().plusHours(1));
        trip.setArrivalTime(LocalDateTime.now().plusHours(2));
        trip.setStatus("SCHEDULED");
        trip = tripRepository.save(trip);

        TicketType ticketType = new TicketType();
        ticketType.setCode("TT_CK_1");
        ticketType.setName("Vé Tiêu Chuẩn");
        ticketType.setPrice(BigDecimal.valueOf(15000));
        ticketType.setRequiresVerification(false);
        ticketType.setIsActive(true);
        ticketType = ticketTypeRepository.save(ticketType);

        TicketType priorityType = new TicketType();
        priorityType.setCode("SENIOR_70");
        priorityType.setName("Người Cao Tuổi (>=70)");
        priorityType.setPrice(BigDecimal.ZERO);
        priorityType.setRequiresVerification(true);
        priorityType.setIsActive(true);
        priorityType = ticketTypeRepository.save(priorityType);

        booking = new Booking();
        booking.setBookingCode("BK_CK_1");
        booking.setContactName("Trần Văn B");
        booking.setContactPhone("0988776655");
        booking.setContactEmail("vanb@test.com");
        booking.setSubtotal(BigDecimal.valueOf(15000));
        booking.setDiscountAmount(BigDecimal.ZERO);
        booking.setTotalAmount(BigDecimal.valueOf(15000));
        booking.setStatus("PAID");
        booking.setCreatedAt(LocalDateTime.now());
        booking.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        booking.setPaidAt(LocalDateTime.now());
        booking = bookingRepository.save(booking);

        validTicket = new Ticket();
        validTicket.setTicketCode("TK_MOCK_CHECKIN");
        validTicket.setBooking(booking);
        validTicket.setTrip(trip);
        validTicket.setSeat(seat1);
        validTicket.setTicketType(ticketType);
        validTicket.setFromStation(st1);
        validTicket.setToStation(st2);
        validTicket.setPrice(BigDecimal.valueOf(15000));
        validTicket.setStatus("VALID");
        validTicket.setPassengerName("Trần Văn B");
        validTicket = ticketRepository.save(validTicket);

        priorityTicket = new Ticket();
        priorityTicket.setTicketCode("TK_MOCK_PRIORITY");
        priorityTicket.setBooking(booking);
        priorityTicket.setTrip(trip);
        priorityTicket.setSeat(seat2);
        priorityTicket.setTicketType(priorityType);
        priorityTicket.setFromStation(st1);
        priorityTicket.setToStation(st2);
        priorityTicket.setPrice(BigDecimal.ZERO);
        priorityTicket.setStatus("VALID");
        priorityTicket.setPassengerName("Trần Cụ C");
        priorityTicket.setBirthYear(1950);
        priorityTicket = ticketRepository.save(priorityTicket);
    }

    @Test
    @DisplayName("Chưa đăng nhập truy cập /admin/checkin -> chuyển hướng về trang login")
    void getCheckin_unauthenticated_redirectsToLogin() throws Exception {
        mvc.perform(get("/admin/checkin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/login?error=unauthorized"));
    }

    @Test
    @DisplayName("Khách hàng (CUSTOMER) truy cập /admin/checkin -> chuyển hướng về trang chủ do không đủ quyền")
    void getCheckin_customer_forbidden() throws Exception {
        mvc.perform(get("/admin/checkin").sessionAttr("loginAccount", customerAccount))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/?error=forbidden"));
    }

    @Test
    @DisplayName("Nhân viên (STAFF) truy cập /admin/checkin -> 200 OK và render view admin/checkin/index")
    void getCheckin_staff_success() throws Exception {
        mvc.perform(get("/admin/checkin").sessionAttr("loginAccount", staffAccount))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/checkin/index"))
                .andExpect(model().attributeExists("tripsToday"))
                .andExpect(model().attributeExists("manifest"));
    }

    @Test
    @DisplayName("Tra cứu mã vé đơn lẻ -> trả về previewTicket")
    void getCheckin_previewSingleTicket_success() throws Exception {
        mvc.perform(get("/admin/checkin")
                        .sessionAttr("loginAccount", staffAccount)
                        .param("ticketCode", "TK_MOCK_CHECKIN"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/checkin/index"))
                .andExpect(model().attributeExists("previewTicket"));
    }

    @Test
    @DisplayName("Tra cứu mã đơn đặt vé BK... -> trả về previewBooking để soát cả đoàn")
    void getCheckin_previewBookingGroup_success() throws Exception {
        mvc.perform(get("/admin/checkin")
                        .sessionAttr("loginAccount", staffAccount)
                        .param("ticketCode", "BK_CK_1"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/checkin/index"))
                .andExpect(model().attributeExists("previewBooking"));
    }

    @Test
    @DisplayName("Soát vé thành công qua POST /admin/checkin -> chuyển hướng kèm flash attribute successMessage và checkedInTicket")
    void postCheckin_validTicket_success() throws Exception {
        mvc.perform(post("/admin/checkin")
                        .sessionAttr("loginAccount", staffAccount)
                        .param("ticketCode", "TK_MOCK_CHECKIN"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/checkin"))
                .andExpect(flash().attributeExists("successMessage"))
                .andExpect(flash().attributeExists("checkedInTicket"));
    }

    @Test
    @DisplayName("Soát vé cả nhóm qua POST /admin/checkin/group -> thành công")
    void postCheckin_group_success() throws Exception {
        mvc.perform(post("/admin/checkin/group")
                        .sessionAttr("loginAccount", staffAccount)
                        .param("ticketCodes", "TK_MOCK_CHECKIN", "TK_MOCK_PRIORITY"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/checkin"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    @DisplayName("Từ chối vé ưu đãi qua POST /admin/checkin/reject -> huỷ vé và nhả ghế")
    void postCheckin_rejectPriority_success() throws Exception {
        mvc.perform(post("/admin/checkin/reject")
                        .sessionAttr("loginAccount", staffAccount)
                        .param("ticketCode", "TK_MOCK_PRIORITY")
                        .param("reason", "Không mang CCCD"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/checkin"))
                .andExpect(flash().attributeExists("errorMessage"));
    }

    @Test
    @DisplayName("Soát vé với mã không tồn tại -> chuyển hướng kèm flash attribute errorMessage")
    void postCheckin_invalidTicket_showsError() throws Exception {
        mvc.perform(post("/admin/checkin")
                        .sessionAttr("loginAccount", staffAccount)
                        .param("ticketCode", "TK_NOT_EXIST"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/checkin"))
                .andExpect(flash().attributeExists("errorMessage"));
    }
}
