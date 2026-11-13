package com.hsf302.waterbus.controller;

import com.hsf302.waterbus.dto.AccountDTO;
import com.hsf302.waterbus.service.BookingService;
import jakarta.servlet.http.HttpSession;
import java.util.HashSet;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Component
public class BookingAccessHelper {

    public static final String VIEWABLE_CODES = "viewableBookingCodes";

    private final BookingService bookingService;

    public BookingAccessHelper(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    public String redirectIfCannotView(String bookingCode, String pageUrl, HttpSession session,
                                       RedirectAttributes flash) {
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
        flash.addFlashAttribute("errorMessage", "Không tìm thấy đơn đặt vé có mã " + bookingCode + ".");
        return "redirect:/";
    }

    public void allowInThisBrowser(HttpSession session, String bookingCode) {
        Set<String> codes = getViewableCodes(session);
        codes.add(bookingCode);
        session.setAttribute(VIEWABLE_CODES, codes);
    }

    @SuppressWarnings("unchecked")
    private Set<String> getViewableCodes(HttpSession session) {
        Set<String> codes = (Set<String>) session.getAttribute(VIEWABLE_CODES);
        if (codes == null) {
            codes = new HashSet<>();
        }
        return codes;
    }
}
