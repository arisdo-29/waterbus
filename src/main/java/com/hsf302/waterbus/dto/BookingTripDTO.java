package com.hsf302.waterbus.dto;

import com.hsf302.waterbus.entity.Trip;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

// Module: BE4 - Đặt vé & Thanh toán. Thông tin chuyến của 1 đơn đưa ra view (attribute "trip").
// Đặt tên BookingTripDTO (không phải TripDTO) để không trùng file với module Chuyến của Trí.
public class BookingTripDTO implements Serializable {
    private final Integer id;
    private final String tripCode;
    private final String routeName;
    private final LocalDateTime departureTime;
    private final LocalDateTime arrivalTime;

    public BookingTripDTO(Integer id, String tripCode, String routeName,
                          LocalDateTime departureTime, LocalDateTime arrivalTime) {
        this.id = id;
        this.tripCode = tripCode;
        this.routeName = routeName;
        this.departureTime = departureTime;
        this.arrivalTime = arrivalTime;
    }

    public static BookingTripDTO fromEntity(Trip trip) {
        return new BookingTripDTO(trip.getId(), trip.getTripCode(), trip.getRoute().getName(),
                trip.getDepartureTime(), trip.getArrivalTime());
    }

    public static BookingTripDTO forSegment(Trip trip, int departureMinutes, int arrivalMinutes) {
        return new BookingTripDTO(trip.getId(), trip.getTripCode(), trip.getRoute().getName(),
                trip.getDepartureTime().plusMinutes(departureMinutes),
                trip.getDepartureTime().plusMinutes(arrivalMinutes));
    }

    public Integer getId() {
        return id;
    }

    public String getTripCode() {
        return tripCode;
    }

    public String getRouteName() {
        return routeName;
    }

    public LocalDateTime getDepartureTime() {
        return departureTime;
    }

    public LocalDateTime getArrivalTime() {
        return arrivalTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BookingTripDTO that = (BookingTripDTO) o;
        return Objects.equals(id, that.id) && Objects.equals(tripCode, that.tripCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, tripCode);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(id = " + id + ", tripCode = " + tripCode + ", routeName = " + routeName + ")";
    }
}
