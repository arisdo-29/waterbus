package com.hsf302.waterbus.service;

import com.hsf302.waterbus.entity.Waterbus;
import com.hsf302.waterbus.dto.AdminWaterbusForm;
import java.util.List;

// Module: BE2 - Tàu, Ghế & Chuyến
// Các method có chữ "HỢP ĐỒNG" là module khác sẽ gọi -> muốn đổi chữ ký phải báo nhóm trước.
public interface WaterbusService {
    /** Danh sách tàu (trang admin). */
    List<Waterbus> getWaterbuses();

    AdminWaterbusForm getWaterbusForEdit(int waterbusId);

    void saveWaterbus(AdminWaterbusForm form);

    void changeStatus(int waterbusId, String status);
}
