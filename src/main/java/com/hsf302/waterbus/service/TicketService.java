package com.hsf302.waterbus.service;

import com.hsf302.waterbus.dto.TicketCheckinDTO;

// Module: BE3 (Tân) - Soát vé (Staff Check-in) theo M4
public interface TicketService {
    /**
     * Tra cứu thông tin vé theo mã vé (không thay đổi trạng thái).
     * @param ticketCode mã vé cần tra cứu
     * @return TicketCheckinDTO chứa thông tin chi tiết vé
     * @throws IllegalArgumentException nếu mã vé rỗng hoặc không tìm thấy
     */
    TicketCheckinDTO getTicketInfo(String ticketCode);

    /**
     * Thực hiện soát vé: kiểm tra tính hợp lệ và chuyển status sang 'USED', ghi nhận checked_in_at.
     * @param ticketCode mã vé cần soát
     * @return TicketCheckinDTO chứa thông tin vé sau khi đã cập nhật sang 'USED'
     * @throws IllegalArgumentException nếu mã vé rỗng hoặc không tìm thấy
     * @throws IllegalStateException nếu vé không ở trạng thái 'VALID' (VD: 'HOLD', 'USED', 'CANCELLED')
     */
    TicketCheckinDTO checkIn(String ticketCode);
}
