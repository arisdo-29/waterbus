package com.hsf302.waterbus.dto;

import com.hsf302.waterbus.entity.Route;
import com.hsf302.waterbus.entity.Trip;
import com.hsf302.waterbus.entity.Waterbus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDateTime;

public class AdminTripForm {
    @NotNull(message = "Vui lòng chọn tuyến.")
    @Positive(message = "Tuyến không hợp lệ.")
    private Integer routeId;

    @NotNull(message = "Vui lòng chọn tàu.")
    @Positive(message = "Tàu không hợp lệ.")
    private Integer waterbusId;

    @NotNull(message = "Vui lòng nhập giờ khởi hành.")
    @Future(message = "Giờ khởi hành phải ở trong tương lai.")
    private LocalDateTime departureTime;

    @NotBlank(message = "Vui lòng chọn trạng thái.")
    @Pattern(regexp = "SCHEDULED", message = "Chuyến mới phải ở trạng thái Đã lên lịch.")
    private String status = "SCHEDULED";

    public Trip toEntity(Route route, Waterbus waterbus, String tripCode,
                         LocalDateTime departureTime, LocalDateTime arrivalTime) {
        Trip trip = new Trip();
        trip.setTripCode(tripCode);
        trip.setRoute(route);
        trip.setWaterbus(waterbus);
        trip.setDepartureTime(departureTime);
        trip.setArrivalTime(arrivalTime);
        trip.setStatus(status);
        return trip;
    }

    public Integer getRouteId() { return routeId; }
    public void setRouteId(Integer routeId) { this.routeId = routeId; }
    public Integer getWaterbusId() { return waterbusId; }
    public void setWaterbusId(Integer waterbusId) { this.waterbusId = waterbusId; }
    public LocalDateTime getDepartureTime() { return departureTime; }
    public void setDepartureTime(LocalDateTime departureTime) { this.departureTime = departureTime; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
