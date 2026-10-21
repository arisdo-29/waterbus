package com.hsf302.waterbus.service.impl;

import com.hsf302.waterbus.dto.BookingDTO;
import com.hsf302.waterbus.dto.BookingTripDTO;
import com.hsf302.waterbus.dto.TicketDTO;
import com.hsf302.waterbus.entity.Booking;
import com.hsf302.waterbus.entity.Ticket;
import com.hsf302.waterbus.entity.Trip;
import com.hsf302.waterbus.repository.BookingRepository;
import com.hsf302.waterbus.repository.TicketRepository;
import com.hsf302.waterbus.service.BookingService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Module: BE4 - Đặt vé & Thanh toán. TODO: thay các dòng throw bằng code thật.
@Service
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final TicketRepository ticketRepository;

    // Constructor injection: Spring tự đưa 2 repository vào
    public BookingServiceImpl(BookingRepository bookingRepository, TicketRepository ticketRepository) {
        this.bookingRepository = bookingRepository;
        this.ticketRepository = ticketRepository;
    }

    @Override
    public Booking createBooking(int tripId, List<Integer> seatIds, int ticketTypeId, int fromStationId, int toStationId, String promoCode, Integer accountId, String contactName, String contactPhone, String contactEmail) {
        throw new UnsupportedOperationException("TODO BE4");
    }

    @Override
    public Booking getBookingByCode(String bookingCode) {
        return bookingRepository.findByBookingCode(bookingCode).orElse(null);
    }

    @Override
    public List<Booking> getBookingsOfAccount(int accountId) {
        throw new UnsupportedOperationException("TODO BE4");
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
        Trip trip = tickets.get(0).getTrip();
        return new BookingTripDTO(trip.getId(), trip.getTripCode(), trip.getRoute().getName(),
                trip.getDepartureTime(), trip.getArrivalTime());
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
}
