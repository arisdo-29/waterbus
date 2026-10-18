package com.hsf302.waterbus.service;

import com.hsf302.waterbus.entity.Route;
import com.hsf302.waterbus.entity.RouteStation;
import java.util.List;

// Module: BE1 - Tuyến & Bến
// Các method có chữ "HỢP ĐỒNG" là module khác sẽ gọi -> muốn đổi chữ ký phải báo nhóm trước.
public interface RouteService {
    /** Danh sách tuyến đang chạy (mỗi chiều là 1 route). */
    List<Route> getActiveRoutes();

    /** Các bến của 1 tuyến, sắp theo stop_order. */
    List<RouteStation> getStopsOfRoute(int routeId);
}
