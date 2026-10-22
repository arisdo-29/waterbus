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

    private MockMvc mvc;
    private AccountDTO staffAccount;
    private AccountDTO customerAccount;
    private Ticket validTicket;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();

        staffAccount = new AccountDTO(1, "Nhân Viên Bến", "staff@waterbus.vn", "0901234567", "STAFF", "ACTIVE");
        customerAccount = new AccountDTO(2, "Khách Hàng", "customer@test.com", "0909876543", "CUSTOMER", "ACTIVE");

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

        Seat seat = new Seat();
        seat.setWaterbus(waterbus);
        seat.setSeatCode("B2");
        seat.setSeatRow(2);
        seat.setSeatCol(2);
        seat.setIsActive(true);
        seat = seatRepository.save(seat);

        Trip trip = new Trip();
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

        Booking booking = new Booking();
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
        validTicket.setSeat(seat);
        validTicket.setTicketType(ticketType);
        validTicket.setFromStation(st1);
        validTicket.setToStation(st2);
        validTicket.setPrice(BigDecimal.valueOf(15000));
        validTicket.setStatus("VALID");
        validTicket = ticketRepository.save(validTicket);
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
                .andExpect(view().name("admin/checkin/index"));
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
