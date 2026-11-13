package com.hsf302.waterbus.service;

import com.hsf302.waterbus.dto.BookingDTO;
import com.hsf302.waterbus.dto.BookingForm;
import com.hsf302.waterbus.entity.Booking;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class BookingTestHelper {

    private BookingTestHelper() {
    }

    static BookingForm form(int tripId, List<Integer> seatIds, int ticketTypeId, int fromStationId, int toStationId,
                            String promoCode, String contactName, String contactPhone, String contactEmail) {
        BookingForm form = new BookingForm();
        form.setTripId(tripId);
        form.setSeatIds(seatIds);
        form.setTicketTypeId(ticketTypeId);
        form.setFromStationId(fromStationId);
        form.setToStationId(toStationId);
        form.setPromoCode(promoCode);
        form.setContactName(contactName);
        form.setContactPhone(contactPhone);
        form.setContactEmail(contactEmail);
        form.setAcceptPolicy(true);
        return form;
    }

    static Set<String> viewable(String bookingCode) {
        Set<String> codes = new HashSet<>();
        codes.add(bookingCode);
        return codes;
    }

    static Booking createBooking(BookingService bookingService, int tripId, List<Integer> seatIds, int ticketTypeId,
                                 int fromStationId, int toStationId, String promoCode, Integer accountId,
                                 String contactName, String contactPhone, String contactEmail) {
        BookingForm form = form(tripId, seatIds, ticketTypeId, fromStationId, toStationId,
                promoCode, contactName, contactPhone, contactEmail);
        BookingDTO created = bookingService.createBooking(form, accountId);
        return bookingService.getBookingByCode(created.getBookingCode());
    }
}
