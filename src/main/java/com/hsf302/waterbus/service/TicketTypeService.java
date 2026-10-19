package com.hsf302.waterbus.service;

import com.hsf302.waterbus.dto.TicketTypeDTO;
import com.hsf302.waterbus.entity.TicketType;
import java.math.BigDecimal;
import java.util.List;

// Module: BE3 - Tài khoản, Loại vé & Khuyến mãi
// Các method có chữ "HỢP ĐỒNG" là module khác sẽ gọi -> muốn đổi chữ ký phải báo nhóm trước.
public interface TicketTypeService {
    /** Loại vé đang bán (BE4 hiển thị khi chọn vé). */
    List<TicketType> getActiveTicketTypes();

    /** HỢP ĐỒNG cho BE4: giá hiện tại của loại vé. */
    BigDecimal getPrice(int ticketTypeId);

    /** Lấy tất cả loại vé phục vụ trang quản trị. */
    List<TicketTypeDTO> getAllTicketTypes();

    /** Lấy chi tiết loại vé theo ID. */
    TicketTypeDTO getTicketTypeById(int id);

    /** Lưu (thêm mới hoặc cập nhật) loại vé. */
    void saveTicketType(TicketTypeDTO dto);

    /** Xoá mềm (ẩn) loại vé. */
    void deleteTicketType(int id);
}
