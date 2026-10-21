package com.hsf302.waterbus.interceptor;

import com.hsf302.waterbus.dto.AccountDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

// Module BE3: Phân quyền bảo vệ các trang quản trị /admin/**
// Chỉ tài khoản có vai trò ADMIN hoặc STAFF mới được phép truy cập.
@Component
public class AdminInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession(false);
        AccountDTO account = (session != null) ? (AccountDTO) session.getAttribute("loginAccount") : null;

        // 1. Chưa đăng nhập -> chuyển hướng về trang đăng nhập
        if (account == null) {
            response.sendRedirect(request.getContextPath() + "/auth/login?error=unauthorized");
            return false;
        }

        // 2. Đã đăng nhập nhưng không phải ADMIN hoặc STAFF -> chặn lại
        String role = account.getRole();
        if ("ADMIN".equalsIgnoreCase(role) || "STAFF".equalsIgnoreCase(role)) {
            return true; // Cho phép đi tiếp
        }

        // Khách hàng thông thường (CUSTOMER) truy cập trái phép -> chuyển về trang chủ
        response.sendRedirect(request.getContextPath() + "/?error=forbidden");
        return false;
    }
}
