package com.hsf302.waterbus.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

/** Bảng [route_station] - module phụ trách: BE1 - Tuyến & Bến. Khớp với database/WaterbusDB_SQLServer.sql */
@Entity
@Table(name = "route_station")
public class RouteStation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "route_id", nullable = false)
    private Route route;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "station_id", nullable = false)
    private Station station;

    @NotNull
    @Column(name = "stop_order", nullable = false)
    private Integer stopOrder;

    @NotNull
    @Column(name = "minutes_from_start", nullable = false)
    private Integer minutesFromStart = 0;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Route getRoute() {
        return route;
    }

    public void setRoute(Route route) {
        this.route = route;
    }

    public Station getStation() {
        return station;
    }

    public void setStation(Station station) {
        this.station = station;
    }

    public Integer getStopOrder() {
        return stopOrder;
    }

    public void setStopOrder(Integer stopOrder) {
        this.stopOrder = stopOrder;
    }

    public Integer getMinutesFromStart() {
        return minutesFromStart;
    }

    public void setMinutesFromStart(Integer minutesFromStart) {
        this.minutesFromStart = minutesFromStart;
    }
}
