package com.hsf302.waterbus.controller;

import com.hsf302.waterbus.dto.AccountDTO;
import com.hsf302.waterbus.service.BookingService;
import jakarta.servlet.http.HttpSession;
import java.util.HashSet;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

// Module: BE4 - Đặt vé & Thanh toán. Dùng chung cho BookingController và PaymentController (TR2).
// LUẬT ai được xem đơn nằm trong BookingService.checkViewAccess (tầng service).
// Lớp này thuộc tầng controller: chỉ đọc giá trị trong session, gọi service, rồi chọn trang chuyển tới.
@Component
public class BookingAccessHelper {

    // Tên attribute trong session: các mã đơn mà trình duyệt này được phép xem (vừa đặt / đã nhập đúng SĐT)
    public static final String VIEWABLE_CODES = "viewableBookingCodes";

    private final BookingService bookingService;

    public BookingAccessHelper(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    /**
     * Được xem đơn -> trả null (controller làm tiếp như bình thường).
     * Không được -> trả chuỗi "redirect:..." để controller return luôn.
     * pageUrl: trang khách đang muốn vào (VD /booking/BK12345678), để đăng nhập xong quay lại đúng trang đó.
     */
    public String redirectIfCannotView(String bookingCode, String pageUrl, HttpSession session,
                                       RedirectAttributes flash) {
        // Lấy giá trị thường từ session rồi mới truyền cho service (không đưa HttpSession xuống service)
        AccountDTO loginAccount = (AccountDTO) session.getAttribute("loginAccount");
        Integer accountId = null;
        String role = null;
        if (loginAccount != null) {
            accountId = loginAccount.getId();
            role = loginAccount.getRole();
        }
        BookingService.ViewAccess access = bookingService.checkViewAccess(bookingCode, accountId, role,
                getViewableCodes(session));

        if (access == BookingService.ViewAccess.ALLOWED) {
            return null;
        }
        if (access == BookingService.ViewAccess.NEED_LOGIN) {
            // Giống AccountController: nhớ trang muốn vào, AuthController đưa khách quay lại sau khi đăng nhập
            session.setAttribute("redirectAfterLogin", pageUrl);
            return "redirect:/auth/login";
        }
        if (access == BookingService.ViewAccess.NEED_PHONE) {
            return "redirect:/booking/" + bookingCode + "/verify";
        }
        if (access == BookingService.ViewAccess.NOT_OWNER) {
            flash.addFlashAttribute("errorMessage", "Bạn không có quyền xem đơn đặt vé này.");
            return "redirect:/";
        }
        // Còn lại: NOT_FOUND
        flash.addFlashAttribute("errorMessage", "Không tìm thấy đơn đặt vé có mã " + bookingCode + ".");
        return "redirect:/";
    }

    // Cho trình duyệt này xem đơn: gọi sau khi khách vãng lai vừa đặt xong, hoặc vừa nhập đúng SĐT
    public void allowInThisBrowser(HttpSession session, String bookingCode) {
        Set<String> codes = getViewableCodes(session);
        codes.add(bookingCode);
        session.setAttribute(VIEWABLE_CODES, codes);
    }

    // Danh sách mã đơn được phép xem đang lưu trong session; chưa có thì trả về danh sách rỗng
    @SuppressWarnings("unchecked") // session lưu Object -> phải ép kiểu về Set<String>
    private Set<String> getViewableCodes(HttpSession session) {
        Set<String> codes = (Set<String>) session.getAttribute(VIEWABLE_CODES);
        if (codes == null) {
            codes = new HashSet<>();
        }
        return codes;
    }
}
