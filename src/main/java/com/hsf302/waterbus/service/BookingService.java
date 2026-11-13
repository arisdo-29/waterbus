package com.hsf302.waterbus.service;

import com.hsf302.waterbus.dto.BookingDTO;
import com.hsf302.waterbus.dto.BookingForm;
import com.hsf302.waterbus.dto.BookingTripDTO;
import com.hsf302.waterbus.dto.SeatDTO;
import com.hsf302.waterbus.dto.TicketDTO;
import com.hsf302.waterbus.entity.Booking;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import org.springframework.validation.Errors;

public interface BookingService {
    int MAX_SEATS_PER_BOOKING = 10;

    int MAX_PRIORITY_TICKETS_PER_BOOKING = 2;

    enum ViewAccess {
        ALLOWED,
        NOT_FOUND,
        NEED_LOGIN,
        NOT_OWNER,
        NEED_PHONE
    }

    ViewAccess checkViewAccess(String bookingCode, Integer loginAccountId, String loginRole,
                               Set<String> viewableBookingCodes);

    boolean verifyContactPhone(String bookingCode, String contactPhone);

    BookingDTO createBooking(BookingForm form, Integer accountId);

    Booking getBookingByCode(String bookingCode);

    List<BookingDTO> getBookingsOfAccount(int accountId);

    BookingDTO getBookingDetail(String bookingCode);

    List<TicketDTO> getTicketsOfBooking(int bookingId);

    BookingTripDTO getTripOfBooking(int bookingId);

    String getTripCancelReason(int bookingId);

    List<SeatDTO> getSelectedSeats(int tripId, List<Integer> seatIds);

    BigDecimal calculateSubtotal(int ticketTypeId, int seatCount);

    BookingTripDTO getTripInfo(int tripId, int fromStationId, int toStationId);

    void validatePassengers(BookingForm form, Errors errors);

    boolean areSeatsAvailable(int tripId, List<Integer> seatIds);

    void releaseHoldTickets(int bookingId);

    void cancelBooking(String bookingCode, Integer accountId);
}
