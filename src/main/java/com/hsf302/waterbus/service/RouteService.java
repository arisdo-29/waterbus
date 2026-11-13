package com.hsf302.waterbus.service;

import com.hsf302.waterbus.entity.Route;
import com.hsf302.waterbus.entity.RouteStation;
import com.hsf302.waterbus.dto.RouteDTO;
import com.hsf302.waterbus.dto.RouteForm;
import com.hsf302.waterbus.dto.StationDTO;
import java.util.List;
import java.util.Map;

public interface RouteService {
    List<Route> getActiveRoutes();

    List<RouteStation> getStopsOfRoute(int routeId);

    List<RouteDTO> getActiveRouteViews();

    List<StationDTO> getStationViewsOfRoute(int routeId);

    Map<Integer, List<StationDTO>> getRouteViewsWithStops();

    List<RouteDTO> getRoutesForAdmin();

    RouteForm getRouteForEdit(int routeId);

    int saveRoute(RouteForm form);

    void hideRoute(int routeId);
}
