package com.hsf302.waterbus.service;

import com.hsf302.waterbus.entity.Route;
import com.hsf302.waterbus.entity.RouteStation;
import com.hsf302.waterbus.dto.RouteDTO;
import com.hsf302.waterbus.dto.RouteForm;
import com.hsf302.waterbus.dto.StationDTO;
import java.util.List;

// Module: BE1 - Tuyến & Bến
// Các method có chữ "HỢP ĐỒNG" là module khác sẽ gọi -> muốn đổi chữ ký phải báo nhóm trước.
public interface RouteService {
    /** Danh sách tuyến đang chạy (mỗi chiều là 1 route). */
    List<Route> getActiveRoutes();

    /** Các bến của 1 tuyến, sắp theo stop_order. */
    List<RouteStation> getStopsOfRoute(int routeId);

    List<RouteDTO> getActiveRouteViews();

    List<StationDTO> getStationViewsOfRoute(int routeId);

    List<RouteDTO> getRoutesForAdmin();

    RouteForm getRouteForEdit(int routeId);

    int saveRoute(RouteForm form);

    void hideRoute(int routeId);
}
