package com.hsf302.waterbus.service.impl;

import com.hsf302.waterbus.dto.BookingTripDTO;
import com.hsf302.waterbus.dto.AdminTripDTO;
import com.hsf302.waterbus.dto.AdminTripForm;
import com.hsf302.waterbus.entity.Trip;
import com.hsf302.waterbus.dto.SeatDTO;
import com.hsf302.waterbus.dto.TripSearchDTO;
import com.hsf302.waterbus.entity.Seat;
import com.hsf302.waterbus.entity.Trip;
import com.hsf302.waterbus.repository.SeatRepository;
import com.hsf302.waterbus.repository.TripRepository;
import com.hsf302.waterbus.service.RouteService;
import com.hsf302.waterbus.service.WaterbusService;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import com.hsf302.waterbus.service.TripService;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Module: BE2 - Tàu, Ghế & Chuyến.
@Service
public class TripServiceImpl implements TripService {
    private final TripRepository tripRepository;
    private final SeatRepository seatRepository;
    private final RouteService routeService;
    private final WaterbusService waterbusService;

    public TripServiceImpl(TripRepository tripRepository, SeatRepository seatRepository,
                           RouteService routeService, WaterbusService waterbusService) {
        this.tripRepository = tripRepository;
        this.seatRepository = seatRepository;
        this.routeService = routeService;
        this.waterbusService = waterbusService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminTripDTO> getTripsForAdmin() {
        return tripRepository.findAllForAdmin().stream()
                .map(trip -> new AdminTripDTO(trip.getId(), trip.getTripCode(), trip.getRoute().getName(),
                        trip.getWaterbus().getName(), trip.getDepartureTime(), trip.getArrivalTime(), trip.getStatus()))
                .toList();
    }

    @Override
    @Transactional
    public void createAdminTrip(AdminTripForm form) {
        if (!"SCHEDULED".equals(form.getStatus())) {
            throw new IllegalArgumentException("Chuyến mới phải ở trạng thái Đã lên lịch.");
        }
        var route = routeService.getActiveRoutes().stream()
                .filter(candidate -> candidate.getId().equals(form.getRouteId())).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Tuyến không tồn tại hoặc đã ngừng hoạt động."));
        var waterbus = waterbusService.getWaterbuses().stream()
                .filter(candidate -> candidate.getId().equals(form.getWaterbusId())).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Tàu không tồn tại."));
        if (!"ACTIVE".equals(waterbus.getStatus())) {
            throw new IllegalArgumentException("Chỉ có thể xếp chuyến cho tàu đang hoạt động.");
        }

        int duration = routeService.getStopsOfRoute(route.getId()).stream()
                .map(stop -> stop.getMinutesFromStart())
                .filter(minutes -> minutes != null)
                .max(Comparator.naturalOrder()).orElse(0);
        // Routes without configured travel times use the same 50-minute estimate as the demo schedule.
        if (duration <= 0) duration = 50;

        var trip = new Trip();
        trip.setTripCode(nextTripCode(form));
        trip.setRoute(route);
        trip.setWaterbus(waterbus);
        trip.setDepartureTime(form.getDepartureTime().withSecond(0).withNano(0));
        trip.setArrivalTime(trip.getDepartureTime().plusMinutes(duration));
        trip.setStatus("SCHEDULED");
        tripRepository.save(trip);
    }

    @Override
    @Transactional
    public void cancelTrip(int tripId) {
        var trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy chuyến tàu."));
        if (!"SCHEDULED".equals(trip.getStatus())) {
            throw new IllegalArgumentException("Chỉ có thể huỷ chuyến đang ở trạng thái Đã lên lịch.");
        }
        trip.setStatus("CANCELLED");
    }

    private String nextTripCode(AdminTripForm form) {
        String prefix = "T" + form.getDepartureTime().format(DateTimeFormatter.ofPattern("yyMMddHHmm"))
                + String.format("%03d", form.getRouteId());
        for (int suffix = 1; suffix <= 999; suffix++) {
            String code = prefix + String.format("%03d", suffix);
            if (!tripRepository.existsByTripCode(code)) return code;
        }
        throw new IllegalArgumentException("Đã đạt số chuyến tối đa trong cùng một phút.");
    }

    @Override
    @Transactional(readOnly = true)
    public List<Trip> searchTrips(int fromStationId, int toStationId, LocalDate date) {
        if (date == null || fromStationId <= 0 || toStationId <= 0 || fromStationId == toStationId) {
            return List.of();
        }
        return tripRepository.searchTrips(fromStationId, toStationId,
                date.atStartOfDay(), date.plusDays(1).atStartOfDay());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TripSearchDTO> searchTripResults(int fromStationId, int toStationId, LocalDate date) {
        return searchTrips(fromStationId, toStationId, date).stream()
                .map(trip -> new TripSearchDTO(trip.getId(), trip.getTripCode(), trip.getRoute().getName(),
                        trip.getDepartureTime(), trip.getArrivalTime(),
                        seatRepository.countAvailableSeats(trip.getId(), trip.getWaterbus().getId())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BookingTripDTO getBookableTrip(int tripId) {
        return tripRepository.findById(tripId)
                .filter(trip -> "SCHEDULED".equals(trip.getStatus()))
                .map(trip -> new BookingTripDTO(trip.getId(), trip.getTripCode(), trip.getRoute().getName(),
                        trip.getDepartureTime(), trip.getArrivalTime()))
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SeatDTO> getSeatMap(int tripId) {
        return tripRepository.findById(tripId)
                .map(trip -> {
                    Set<Integer> availableIds = getAvailableSeats(tripId).stream()
                            .map(Seat::getId).collect(Collectors.toSet());
                    return seatRepository.findActiveSeats(trip.getWaterbus().getId()).stream()
                            .map(seat -> new SeatDTO(seat.getId(), seat.getSeatCode(), seat.getSeatRow(),
                                    seat.getSeatCol(), availableIds.contains(seat.getId())))
                            .toList();
                })
                .orElseGet(List::of);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Seat> getAvailableSeats(int tripId) {
        return tripRepository.findById(tripId)
                .map(trip -> seatRepository.findAvailableSeats(tripId, trip.getWaterbus().getId()))
                .orElseGet(List::of);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isSeatAvailable(int tripId, int seatId) {
        return tripRepository.findById(tripId)
                .map(trip -> seatRepository.countAvailableSeat(tripId, trip.getWaterbus().getId(), seatId) > 0)
                .orElse(false);
    }
}
