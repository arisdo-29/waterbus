package com.hsf302.waterbus.controller;

import com.hsf302.waterbus.config.VnPayConfig;
import com.hsf302.waterbus.dto.BookingDTO;
import com.hsf302.waterbus.dto.VnPayResult;
import com.hsf302.waterbus.entity.Booking;
import com.hsf302.waterbus.service.BookingService;
import com.hsf302.waterbus.service.PaymentService;
import com.hsf302.waterbus.service.VnPayService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

// Module: BE4 - Đặt vé & Thanh toán. Mọi URL của controller này bắt đầu bằng /payment
// View đặt trong templates/payment/ (giao diện: Khuyên)
@Controller
@RequestMapping("/payment")
public class PaymentController {

    private final PaymentService paymentService;
    private final BookingService bookingService; // cùng module BE4: đọc thông tin đơn
    private final VnPayService vnPayService;     // cổng VNPay Sandbox
    private final VnPayConfig vnPayConfig;       // biết máy chủ đã có khoá VNPay chưa

    public PaymentController(PaymentService paymentService, BookingService bookingService,
                             VnPayService vnPayService, VnPayConfig vnPayConfig) {
        this.paymentService = paymentService;
        this.bookingService = bookingService;
        this.vnPayService = vnPayService;
        this.vnPayConfig = vnPayConfig;
    }

    // GET /payment/BK12345678 -> trang thanh toán của đơn (làm giống detail() trong BookingController)
    @GetMapping("/{bookingCode}")
    public String index(@PathVariable String bookingCode, Model model, RedirectAttributes flash) {
        BookingDTO booking = bookingService.getBookingDetail(bookingCode);
        if (booking == null) {
            // Sai mã đơn -> về trang chủ và báo lỗi
            flash.addFlashAttribute("errorMessage", "Không tìm thấy đơn đặt vé có mã " + bookingCode + ".");
            return "redirect:/";
        }
        // Tên attribute đúng theo docs/PAGES.md: booking, trip, tickets
        model.addAttribute("booking", booking);
        model.addAttribute("trip", bookingService.getTripOfBooking(booking.getId()));
        model.addAttribute("tickets", bookingService.getTicketsOfBooking(booking.getId()));
        return "payment/index";
    }

    // POST /payment/BK12345678/mock -> nút "Thanh toán" trên trang thanh toán
    @PostMapping("/{bookingCode}/mock")
    public String payMock(@PathVariable String bookingCode, RedirectAttributes flash) {
        Booking booking = bookingService.getBookingByCode(bookingCode);
        if (booking == null) {
            flash.addFlashAttribute("errorMessage", "Không tìm thấy đơn đặt vé có mã " + bookingCode + ".");
            return "redirect:/";
        }
        try {
            paymentService.payMock(booking.getId());
            // Thành công -> sang trang xem vé, báo xanh qua fragment alerts
            flash.addFlashAttribute("successMessage", "Thanh toán thành công! Vé của bạn đã sẵn sàng.");
            return "redirect:/booking/" + booking.getBookingCode();
        } catch (IllegalArgumentException ex) {
            // Đơn đã trả tiền / hết hạn -> quay lại trang thanh toán, báo đỏ
            flash.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/payment/" + booking.getBookingCode();
        }
    }

    // POST /payment/BK12345678/vnpay -> chọn "VNPay" trên trang thanh toán (form gửi kèm field method, không cần đọc)
    // bankCode (không bắt buộc, nằm trên URL action): VNPAYQR = mở thẳng màn hình QR, VNBANK = mở thẳng trang thẻ ATM,
    // bỏ trống -> VNPay hiện trang cho khách tự chọn phương thức
    @PostMapping("/{bookingCode}/vnpay")
    public String payVnPay(@PathVariable String bookingCode, @RequestParam(required = false) String bankCode,
                           HttpServletRequest request, RedirectAttributes flash) {
        Booking booking = bookingService.getBookingByCode(bookingCode);
        if (booking == null) {
            // Giống payMock: sai mã đơn -> về trang chủ
            flash.addFlashAttribute("errorMessage", "Không tìm thấy đơn đặt vé có mã " + bookingCode + ".");
            return "redirect:/";
        }
        if (!vnPayConfig.isConfigured()) {
            // Máy này chưa điền khoá VNPay (VD máy bạn khác) -> mời dùng thanh toán mô phỏng
            flash.addFlashAttribute("errorMessage", "VNPay chưa được cấu hình trên máy chủ. Vui lòng chọn Thanh toán mô phỏng.");
            return "redirect:/payment/" + booking.getBookingCode();
        }
        try {
            // IP của khách (VNPay bắt buộc gửi kèm)
            String url = vnPayService.createPaymentUrl(booking.getBookingCode(), request.getRemoteAddr(), bankCode);
            // "redirect:" + địa chỉ đầy đủ https://... -> trình duyệt tự chuyển sang trang VNPay
            return "redirect:" + url;
        } catch (IllegalArgumentException ex) {
            // Đơn đã trả tiền / hết hạn / chuyến bị huỷ... -> quay lại trang thanh toán, báo đỏ
            flash.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/payment/" + booking.getBookingCode();
        }
    }

    // GET /payment/vnpay-return?vnp_Amount=...&vnp_SecureHash=... -> VNPay đưa khách quay về đây sau khi trả tiền
    // @RequestParam Map: gom TẤT CẢ tham số trên URL vào 1 Map (tên -> giá trị), vì VNPay gửi rất nhiều tham số
    @GetMapping("/vnpay-return")
    public String vnPayReturn(@RequestParam Map<String, String> params, RedirectAttributes flash) {
        VnPayResult result = vnPayService.handleReturn(params);
        if (result.getBookingCode() == null) {
            // Chữ ký sai / không biết đơn nào -> không có trang đơn để quay về -> về trang chủ
            flash.addFlashAttribute("errorMessage", result.getMessage());
            return "redirect:/";
        }
        if (result.isSuccess()) {
            flash.addFlashAttribute("successMessage", "Thanh toán VNPay thành công! Vé của bạn đã sẵn sàng.");
            return "redirect:/booking/" + result.getBookingCode();
        }
        // Thất bại (khách huỷ, sai số tiền, đơn hết hạn...) -> về trang thanh toán, báo đỏ
        flash.addFlashAttribute("errorMessage", result.getMessage());
        return "redirect:/payment/" + result.getBookingCode();
    }
}
