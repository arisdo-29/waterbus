package com.hsf302.waterbus.service.impl;

import com.hsf302.waterbus.entity.Route;
import com.hsf302.waterbus.entity.RouteStation;
import com.hsf302.waterbus.service.RouteService;
import java.util.List;
import org.springframework.stereotype.Service;

// Module: BE1 - Tuyến & Bến. TODO: thay các dòng throw bằng code thật.
@Service
public class RouteServiceImpl implements RouteService {

    @Override
    public List<Route> getActiveRoutes() {
        throw new UnsupportedOperationException("TODO BE1");
    }

    @Override
    public List<RouteStation> getStopsOfRoute(int routeId) {
        throw new UnsupportedOperationException("TODO BE1");
    }
}
