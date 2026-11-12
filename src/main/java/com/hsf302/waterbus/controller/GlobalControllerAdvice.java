package com.hsf302.waterbus.controller;

import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

// Leader: xử lý lỗi DÙNG CHUNG cho mọi controller.
// Luồng: request -> DispatcherServlet -> Interceptor -> Controller -> Service -> Repository -> DB.
// Controller ném lỗi (VD /stations/abc: "abc" không đổi được sang int) -> DispatcherServlet tìm @ExceptionHandler:
//   1. Trong chính controller đó trước (TripController, AdminTripController... đã có handler riêng -> dùng của họ).
//   2. Không có thì mới tới class @ControllerAdvice này.
// Chỉ bắt lỗi URL/tham số sai. KHÔNG bắt Exception chung: lỗi khác vẫn để trang error/500 của Spring xử lý.
@ControllerAdvice
public class GlobalControllerAdvice {

    // Sai kiểu tham số (VD /stations/abc) hoặc thiếu tham số bắt buộc (VD POST /booking/checkout thiếu tripId)
    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
    public String handleInvalidRequest(RedirectAttributes flash) {
        flash.addFlashAttribute("errorMessage", "Đường dẫn không hợp lệ hoặc thiếu thông tin.");
        return "redirect:/";
    }
}
