package com.hsf302.waterbus.service;

import com.hsf302.waterbus.entity.Route;
import com.hsf302.waterbus.entity.RouteStation;
import com.hsf302.waterbus.dto.RouteDTO;
import com.hsf302.waterbus.dto.RouteForm;
import com.hsf302.waterbus.dto.StationDTO;
import java.util.List;
import java.util.Map;

// Module: BE1 - Tuyến & Bến
// Các method có chữ "HỢP ĐỒNG" là module khác sẽ gọi -> muốn đổi chữ ký phải báo nhóm trước.
public interface RouteService {
    // getActiveRoutes, getStopsOfRoute trả entity: chỉ dùng giữa các service, controller dùng bản ...View
    /** Danh sách tuyến đang chạy (mỗi chiều là 1 route). */
    List<Route> getActiveRoutes();

    /** Các bến của 1 tuyến, sắp theo stop_order. */
    List<RouteStation> getStopsOfRoute(int routeId);

    List<RouteDTO> getActiveRouteViews();

    List<StationDTO> getStationViewsOfRoute(int routeId);

    /** Bến của từng tuyến đang chạy: key = id tuyến, thứ tự giống getActiveRouteViews(). */
    Map<Integer, List<StationDTO>> getRouteViewsWithStops();

    List<RouteDTO> getRoutesForAdmin();

    RouteForm getRouteForEdit(int routeId);

    int saveRoute(RouteForm form);

    void hideRoute(int routeId);
}
