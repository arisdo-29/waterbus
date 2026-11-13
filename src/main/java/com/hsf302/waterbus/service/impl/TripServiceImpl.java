package com.hsf302.waterbus.service.impl;

import com.hsf302.waterbus.dto.AdminTripDTO;
import com.hsf302.waterbus.dto.AdminTripForm;
import com.hsf302.waterbus.dto.BookingTripDTO;
import com.hsf302.waterbus.dto.SeatDTO;
import com.hsf302.waterbus.dto.TripSearchDTO;
import com.hsf302.waterbus.dto.TripManifestDTO;
import com.hsf302.waterbus.dto.TicketCheckinDTO;
import com.hsf302.waterbus.dto.TripCancellationPreviewDTO;
import com.hsf302.waterbus.entity.RouteStation;
import com.hsf302.waterbus.entity.Seat;
import com.hsf302.waterbus.entity.Trip;
import com.hsf302.waterbus.repository.SeatRepository;
import com.hsf302.waterbus.repository.TripRepository;
import com.hsf302.waterbus.repository.WaterbusRepository;
import com.hsf302.waterbus.service.RouteService;
import com.hsf302.waterbus.service.RefundService;
import com.hsf302.waterbus.service.TripService;
import com.hsf302.waterbus.service.TicketService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TripServiceImpl implements TripService {
    private static final LocalDateTime OPEN_END = LocalDateTime.of(9999, 12, 31, 23, 59);
    private final TripRepository tripRepository;
    private final SeatRepository seatRepository;
    private final RouteService routeService;
    private final WaterbusRepository waterbusRepository;
    private final TicketService ticketService;
    private final RefundService refundService;

    public TripServiceImpl(TripRepository tripRepository, SeatRepository seatRepository,
                           RouteService routeService, WaterbusRepository waterbusRepository,
                           TicketService ticketService, RefundService refundService) {
        this.tripRepository = tripRepository;
        this.seatRepository = seatRepository;
        this.routeService = routeService;
        this.waterbusRepository = waterbusRepository;
        this.ticketService = ticketService;
        this.refundService = refundService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminTripDTO> getTripsForAdmin() {
        return tripRepository.findAllForAdmin().stream()
                .map(trip -> AdminTripDTO.fromEntity(trip,
                        ticketService.getPassengerManifest(trip.getId()).stream()
                                .filter(ticket -> ticket.isValid() || ticket.isUsed()).count()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TripCancellationPreviewDTO> getCancellableTrips(int waterbusId, LocalDateTime departureFrom,
                                                                 LocalDateTime departureTo) {
        if (waterbusId <= 0 || departureFrom == null || departureTo == null
                || departureFrom.isAfter(departureTo)) {
            throw new IllegalArgumentException("Vui lòng chọn tàu và khoảng thời gian hợp lệ.");
        }

        LocalDateTime now = LocalDateTime.now();
        return tripRepository.findCancellableForPreview(waterbusId, departureFrom, departureTo, now).stream()
                .map(this::toCancellationPreview).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TripCancellationPreviewDTO> getTripsAffectedByOutage(int waterbusId,
                                                                       LocalDateTime startsAt,
                                                                       LocalDateTime endsAt) {
        if (waterbusId <= 0 || startsAt == null || (endsAt != null && !endsAt.isAfter(startsAt))) {
            throw new IllegalArgumentException("Khoảng thời gian ngưng tàu không hợp lệ.");
        }
        return tripRepository.findScheduledOverlappingOutage(waterbusId, startsAt,
                        endsAt == null ? OPEN_END : endsAt, LocalDateTime.now()).stream()
                .map(this::toCancellationPreview).toList();
    }

    private TripCancellationPreviewDTO toCancellationPreview(Trip trip) {
        List<TicketCheckinDTO> tickets = ticketService.getPassengerManifest(trip.getId()).stream()
                .filter(ticket -> Set.of("HOLD", "VALID", "USED").contains(ticket.getStatus()))
                .sorted(Comparator.comparing(TicketCheckinDTO::getSeatCode,
                                Comparator.nullsFirst(Comparator.naturalOrder()))
                        .thenComparing(TicketCheckinDTO::getTicketCode))
                .toList();
        long confirmedTicketCount = tickets.stream()
                .filter(ticket -> "VALID".equals(ticket.getStatus()) || "USED".equals(ticket.getStatus()))
                .count();
        long heldTicketCount = tickets.stream().filter(ticket -> "HOLD".equals(ticket.getStatus())).count();
        return new TripCancellationPreviewDTO(AdminTripDTO.fromEntity(trip, confirmedTicketCount), tickets,
                confirmedTicketCount, heldTicketCount);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminTripDTO> getTodayTrips() {
        LocalDate today = LocalDate.now();
        return tripRepository.findDepartingBetween(today.atStartOfDay(), today.plusDays(1).atStartOfDay())
                .stream().filter(trip -> "SCHEDULED".equals(trip.getStatus())
                        || "DEPARTED".equals(trip.getStatus()))
                .map(AdminTripDTO::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countUpcomingTripsOfRoute(int routeId) {
        return tripRepository.countByRouteIdAndStatusAndDepartureTimeAfter(
                routeId, "SCHEDULED", LocalDateTime.now());
    }

    @Override
    @Transactional(readOnly = true)
    public TripManifestDTO getManifest(int tripId) {
        if (!tripRepository.existsById(tripId)) {
            throw new IllegalArgumentException("Không tìm thấy chuyến tàu.");
        }
        List<TicketCheckinDTO> passengers = ticketService.getPassengerManifest(tripId).stream()
                .filter(ticket -> ticket.isValid() || ticket.isUsed())
                .sorted(Comparator.comparing(TicketCheckinDTO::getSeatCode)
                        .thenComparing(TicketCheckinDTO::getTicketCode))
                .toList();
        return new TripManifestDTO(passengers, passengers.stream().filter(TicketCheckinDTO::isUsed).count(),
                passengers.size());
    }

    @Override
    @Transactional
    public void createAdminTrip(AdminTripForm form) {
        if (!"SCHEDULED".equals(form.getStatus())) {
            throw new IllegalArgumentException("Chuyến mới phải ở trạng thái Đã lên lịch.");
        }
        if (form.getDepartureTime() == null) {
            throw new IllegalArgumentException("Vui lòng nhập giờ khởi hành.");
        }
        LocalDateTime departureTime = form.getDepartureTime().withSecond(0).withNano(0);
        if (!departureTime.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Giờ khởi hành phải ở trong tương lai.");
        }
        var route = routeService.getActiveRoutes().stream()
                .filter(candidate -> candidate.getId().equals(form.getRouteId())).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Tuyến không tồn tại hoặc đã ngừng hoạt động."));
        var waterbus = waterbusRepository.findByIdForUpdate(form.getWaterbusId())
                .orElseThrow(() -> new IllegalArgumentException("Tàu không tồn tại."));
        if (!"ACTIVE".equals(waterbus.getStatus())) {
            throw new IllegalArgumentException("Chỉ có thể xếp chuyến cho tàu đang hoạt động.");
        }

        int duration = routeService.getStopsOfRoute(route.getId()).stream()
                .map(stop -> stop.getMinutesFromStart())
                .filter(minutes -> minutes != null)
                .max(Comparator.naturalOrder()).orElse(0);
        if (duration <= 0) duration = 50;

        LocalDateTime arrivalTime = departureTime.plusMinutes(duration);
        if ((waterbus.getPendingStatus() != null || "MAINTENANCE".equals(waterbus.getStatus())
                || "INACTIVE".equals(waterbus.getStatus()))
                && waterbus.getStatusStartsAt() != null
                && arrivalTime.isAfter(waterbus.getStatusStartsAt())
                && (waterbus.getStatusEndsAt() == null || departureTime.isBefore(waterbus.getStatusEndsAt()))) {
            throw new IllegalArgumentException("Tàu đã có lịch bảo trì hoặc ngưng hoạt động trong giờ chuyến này.");
        }
        if (tripRepository.countScheduledOverlaps(waterbus.getId(), departureTime, arrivalTime) > 0) {
            throw new IllegalArgumentException("Tàu đã có chuyến khác trùng giờ. Vui lòng chọn tàu hoặc giờ khác.");
        }

        var trip = form.toEntity(route, waterbus, nextTripCode(form), departureTime, arrivalTime);
        tripRepository.save(trip);
    }

    @Override
    @Transactional
    public void cancelTrip(int tripId, String reason) {
        cancelTrips(List.of(tripId), reason);
    }

    @Override
    @Transactional
    public void cancelTrips(List<Integer> tripIds, String reason) {
        cancelTripsInternal(tripIds, reason, null, null, null, false);
    }

    @Override
    @Transactional
    public void cancelTrips(List<Integer> tripIds, String reason, int waterbusId,
                            LocalDateTime departureFrom, LocalDateTime departureTo) {
        if (waterbusId <= 0 || departureFrom == null || departureTo == null
                || departureFrom.isAfter(departureTo)) {
            throw new IllegalArgumentException("Vui lòng lọc lại chuyến trước khi xác nhận hủy.");
        }
        cancelTripsInternal(tripIds, reason, waterbusId, departureFrom, departureTo, true);
    }

    private void cancelTripsInternal(List<Integer> tripIds, String reason, Integer waterbusId,
                                     LocalDateTime departureFrom, LocalDateTime departureTo, boolean checkFilter) {
        String trimmedReason = reason == null ? "" : reason.trim();
        if (trimmedReason.isEmpty() || trimmedReason.length() > 255) {
            throw new IllegalArgumentException("Lý do huỷ chuyến phải có từ 1 đến 255 ký tự.");
        }
        if (tripIds == null || tripIds.isEmpty()
                || tripIds.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new IllegalArgumentException("Vui lòng chọn ít nhất một chuyến cần huỷ.");
        }
        Set<Integer> uniqueTripIds = new LinkedHashSet<>(tripIds);
        if (uniqueTripIds.size() != tripIds.size()) {
            throw new IllegalArgumentException("Danh sách chuyến bị trùng. Vui lòng tải lại và chọn lại.");
        }

        LocalDateTime now = LocalDateTime.now();
        List<Trip> trips = new ArrayList<>();
        for (Integer tripId : uniqueTripIds.stream().sorted().toList()) {
            Trip trip = tripRepository.findByIdForUpdate(tripId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy chuyến ID " + tripId + "."));
            if (!"SCHEDULED".equals(trip.getStatus())) {
                throw new IllegalArgumentException("Chuyến " + trip.getTripCode()
                        + " không còn ở trạng thái Đã lên lịch. Không có chuyến nào bị huỷ.");
            }
            if (trip.getDepartureTime() == null || !trip.getDepartureTime().isAfter(now)) {
                throw new IllegalArgumentException("Chuyến " + trip.getTripCode()
                        + " đã đến giờ khởi hành. Không có chuyến nào bị huỷ.");
            }
            if (checkFilter && (!trip.getWaterbus().getId().equals(waterbusId)
                    || trip.getDepartureTime().isBefore(departureFrom)
                    || trip.getDepartureTime().isAfter(departureTo))) {
                throw new IllegalArgumentException("Chuyến " + trip.getTripCode()
                        + " không còn khớp bộ lọc. Không có chuyến nào bị huỷ.");
            }
            trips.add(trip);
        }

        for (Trip trip : trips) {
            trip.setCancelReason(trimmedReason);
            trip.setStatus("CANCELLED");
            refundService.createForCancelledTrip(trip.getId(), trimmedReason);
        }
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
        LocalDateTime stopSellingAt = LocalDateTime.now().plusMinutes(ONLINE_SALES_CUTOFF_MINUTES);
        List<Trip> matches = new ArrayList<>();
        Map<Integer, Segment> segments = new HashMap<>();
        for (var route : routeService.getActiveRoutes()) {
            Segment segment = segmentForRoute(route.getId(), fromStationId, toStationId);
            if (segment == null) continue;
            segments.put(route.getId(), segment);
            LocalDateTime start = date.atStartOfDay().minusMinutes(segment.departureMinutes());
            LocalDateTime end = date.plusDays(1).atStartOfDay().minusMinutes(segment.departureMinutes());
            tripRepository.searchTripsForRoute(route.getId(), start, end).stream()
                    .filter(trip -> trip.getDepartureTime().plusMinutes(segment.departureMinutes())
                            .isAfter(stopSellingAt))
                    .forEach(matches::add);
        }
        matches.sort(Comparator.comparing((Trip trip) -> trip.getDepartureTime()
                        .plusMinutes(segments.get(trip.getRoute().getId()).departureMinutes()))
                .thenComparing(Trip::getId));
        return matches;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TripSearchDTO> searchTripResults(int fromStationId, int toStationId, LocalDate date) {
        Map<Integer, Segment> segments = new HashMap<>();
        return searchTrips(fromStationId, toStationId, date).stream()
                .map(trip -> {
                    int routeId = trip.getRoute().getId();
                    Segment segment = segments.computeIfAbsent(routeId,
                            id -> segmentForRoute(id, fromStationId, toStationId));
                    return TripSearchDTO.forSegment(trip, segment.departureMinutes(), segment.arrivalMinutes(),
                            seatRepository.countAvailableSeats(trip.getId(), trip.getWaterbus().getId()));
                })
                .toList();
    }

    private Segment segmentForRoute(int routeId, int fromStationId, int toStationId) {
        RouteStation departure = null;
        RouteStation arrival = null;
        for (RouteStation stop : routeService.getStopsOfRoute(routeId)) {
            if (stop.getStation().getId() == fromStationId) departure = stop;
            if (stop.getStation().getId() == toStationId) arrival = stop;
        }
        if (departure == null || arrival == null || departure.getStopOrder() >= arrival.getStopOrder()) {
            return null;
        }
        return new Segment(departure.getMinutesFromStart(), arrival.getMinutesFromStart());
    }

    private record Segment(int departureMinutes, int arrivalMinutes) { }

    @Override
    @Transactional(readOnly = true)
    public BookingTripDTO getBookableTrip(int tripId) {
        return tripRepository.findById(tripId)
                .filter(trip -> "SCHEDULED".equals(trip.getStatus())
                        || "DEPARTED".equals(trip.getStatus()))
                .filter(trip -> trip.getDepartureTime()
                        .isAfter(LocalDateTime.now().plusMinutes(ONLINE_SALES_CUTOFF_MINUTES)))
                .map(BookingTripDTO::fromEntity)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public BookingTripDTO getBookableTrip(int tripId, int fromStationId, int toStationId) {
        return tripRepository.findById(tripId)
                .filter(trip -> "SCHEDULED".equals(trip.getStatus())
                        || "DEPARTED".equals(trip.getStatus()))
                .filter(trip -> Boolean.TRUE.equals(trip.getRoute().getIsActive()))
                .filter(trip -> "ACTIVE".equals(trip.getWaterbus().getStatus()))
                .map(trip -> {
                    Segment segment = segmentForRoute(trip.getRoute().getId(), fromStationId, toStationId);
                    if (segment == null) return null;
                    LocalDateTime boardingTime = trip.getDepartureTime().plusMinutes(segment.departureMinutes());
                    if (!boardingTime.isAfter(LocalDateTime.now().plusMinutes(ONLINE_SALES_CUTOFF_MINUTES))) {
                        return null;
                    }
                    return BookingTripDTO.forSegment(trip, segment.departureMinutes(), segment.arrivalMinutes());
                })
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public Trip getTripById(int tripId) {
        return tripRepository.findById(tripId).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SeatDTO> getSeatMap(int tripId) {
        return tripRepository.findById(tripId)
                .map(trip -> {
                    Set<Integer> availableIds = getAvailableSeats(tripId).stream()
                            .map(Seat::getId).collect(Collectors.toSet());
                    return seatRepository.findActiveSeats(trip.getWaterbus().getId()).stream()
                            .map(seat -> SeatDTO.fromEntity(seat, availableIds.contains(seat.getId())))
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
