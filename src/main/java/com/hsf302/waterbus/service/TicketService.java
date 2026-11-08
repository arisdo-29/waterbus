package com.hsf302.waterbus.service;

import com.hsf302.waterbus.dto.BookingCheckinDTO;
import com.hsf302.waterbus.dto.TicketCheckinDTO;

import java.util.List;

// Module: BE3 (Tân) - Soát vé (Staff Check-in) theo M4 & M5
public interface TicketService {
    /**
     * Tra cứu thông tin vé theo mã vé (không thay đổi trạng thái).
     * @param ticketCode mã vé cần tra cứu
     * @return TicketCheckinDTO chứa thông tin chi tiết vé
     * @throws IllegalArgumentException nếu mã vé rỗng hoặc không tìm thấy
     */
    TicketCheckinDTO getTicketInfo(String ticketCode);

    /**
     * Tra cứu thông tin vé và ghi nhật ký quét (VIEW).
     */
    TicketCheckinDTO getTicketInfoWithLog(String ticketCode, Integer staffAccountId, String source);

    /**
     * Tra cứu theo mã đơn đặt vé để phục vụ soát vé cả nhóm.
     * @param bookingCode mã đơn (bắt đầu bằng BK...)
     * @return BookingCheckinDTO chứa thông tin đơn và danh sách vé
     */
    BookingCheckinDTO getBookingCheckinInfo(String bookingCode, Integer staffAccountId, String source);

    /**
     * Thực hiện soát vé: kiểm tra tính hợp lệ và chuyển status sang 'USED', ghi nhận checked_in_at.
     * @param ticketCode mã vé cần soát
     * @return TicketCheckinDTO chứa thông tin vé sau khi đã cập nhật sang 'USED'
     */
    TicketCheckinDTO checkIn(String ticketCode);

    /**
     * Thực hiện soát vé nâng cao có ghi log, thông tin nhân viên và kiểm tra thời gian/nguồn soát.
     */
    TicketCheckinDTO checkIn(String ticketCode, Integer staffAccountId, String source, boolean overrideTripTime);

    /**
     * Soát vé theo nhóm cho nhiều mã vé trong cùng một đơn.
     * @param ticketCodes danh sách mã vé được chọn
     * @return danh sách vé đã soát thành công
     */
    List<TicketCheckinDTO> checkInGroup(List<String> ticketCodes, Integer staffAccountId, String source);

    /**
     * Từ chối vé ưu đãi khi khách không xuất trình được giấy tờ hoặc không đúng đối tượng.
     * Chuyển vé sang 'CANCELLED', nhả ghế, ghi log REJECT.
     */
    TicketCheckinDTO rejectPriorityTicket(String ticketCode, String reason, Integer staffAccountId);

    /**
     * Lấy danh sách toàn bộ hành khách của chuyến tàu (Passenger Manifest).
     * @param tripId mã chuyến tàu
     * @return danh sách vé của chuyến
     */
    List<TicketCheckinDTO> getPassengerManifest(int tripId);
}
