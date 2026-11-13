package com.hsf302.waterbus.controller;

import com.hsf302.waterbus.dto.AccountDTO;
import com.hsf302.waterbus.dto.BookingDTO;
import com.hsf302.waterbus.dto.VnPayResult;
import com.hsf302.waterbus.service.BookingService;
import com.hsf302.waterbus.service.PaymentService;
import com.hsf302.waterbus.service.VnPayService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/payment")
public class PaymentController {

    private final BookingService bookingService;
    private final VnPayService vnPayService;
    private final BookingAccessHelper bookingAccess;
    private final PaymentService paymentService;

    public PaymentController(BookingService bookingService, VnPayService vnPayService,
                             BookingAccessHelper bookingAccess, PaymentService paymentService) {
        this.bookingService = bookingService;
        this.vnPayService = vnPayService;
        this.bookingAccess = bookingAccess;
        this.paymentService = paymentService;
    }

    @GetMapping("/{bookingCode}")
    public String index(@PathVariable String bookingCode, HttpSession session, Model model,
                        RedirectAttributes flash) {
        BookingDTO booking = bookingService.getBookingDetail(bookingCode);
        if (booking == null) {
            flash.addFlashAttribute("errorMessage", "Không tìm thấy đơn đặt vé có mã " + bookingCode + ".");
            return "redirect:/";
        }
        String blocked = bookingAccess.redirectIfCannotView(bookingCode, "/payment/" + bookingCode, session, flash);
        if (blocked != null) {
            return blocked;
        }
        model.addAttribute("booking", booking);
        model.addAttribute("trip", bookingService.getTripOfBooking(booking.getId()));
        model.addAttribute("tickets", bookingService.getTicketsOfBooking(booking.getId()));
        return "payment/index";
    }

    @PostMapping("/{bookingCode}/vnpay")
    public String payVnPay(@PathVariable String bookingCode, @RequestParam(required = false) String bankCode,
                           HttpServletRequest request, HttpSession session, RedirectAttributes flash) {
        BookingDTO booking = bookingService.getBookingDetail(bookingCode);
        if (booking == null) {
            flash.addFlashAttribute("errorMessage", "Không tìm thấy đơn đặt vé có mã " + bookingCode + ".");
            return "redirect:/";
        }
        String blocked = bookingAccess.redirectIfCannotView(bookingCode, "/payment/" + bookingCode, session, flash);
        if (blocked != null) {
            return blocked;
        }
        try {
            String url = vnPayService.createPaymentUrl(booking.getBookingCode(), request.getRemoteAddr(), bankCode);
            return "redirect:" + url;
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/payment/" + booking.getBookingCode();
        }
    }

    @PostMapping("/{bookingCode}/cash")
    public String payCash(@PathVariable String bookingCode, HttpSession session, RedirectAttributes flash) {
        BookingDTO booking = bookingService.getBookingDetail(bookingCode);
        if (booking == null) {
            flash.addFlashAttribute("errorMessage", "Không tìm thấy đơn đặt vé có mã " + bookingCode + ".");
            return "redirect:/";
        }
        AccountDTO loginAccount = (AccountDTO) session.getAttribute("loginAccount");
        String role = null;
        if (loginAccount != null) {
            role = loginAccount.getRole();
        }
        try {
            paymentService.confirmCashPayment(booking.getId(), role);
            flash.addFlashAttribute("successMessage", "Đã xác nhận thu tiền mặt. Vé đã sẵn sàng.");
            return "redirect:/booking/" + bookingCode;
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/payment/" + bookingCode;
        }
    }

    @GetMapping("/vnpay-return")
    public String vnPayReturn(@RequestParam Map<String, String> params, RedirectAttributes flash) {
        VnPayResult result = vnPayService.handleReturn(params);
        if (result.getBookingCode() == null) {
            flash.addFlashAttribute("errorMessage", result.getMessage());
            return "redirect:/";
        }
        if (result.isSuccess()) {
            flash.addFlashAttribute("successMessage", "Thanh toán VNPay thành công! Vé của bạn đã sẵn sàng.");
            return "redirect:/booking/" + result.getBookingCode();
        }
        flash.addFlashAttribute("errorMessage", result.getMessage());
        return "redirect:/payment/" + result.getBookingCode();
    }
}
