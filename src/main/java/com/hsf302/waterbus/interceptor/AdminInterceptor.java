package com.hsf302.waterbus.interceptor;

import com.hsf302.waterbus.dto.AccountDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.support.RequestContextUtils;

// Module BE3: Phân quyền bảo vệ các trang quản trị /admin/**
// ADMIN: Toàn quyền truy cập mọi trang quản trị
// STAFF: Chỉ soát vé (/admin/checkin/**) và xem danh sách chuyến tàu (GET /admin/trips)
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

        String role = account.getRole();

        // 2. ADMIN có toàn quyền vào tất cả các trang /admin/**
        if ("ADMIN".equalsIgnoreCase(role)) {
            return true;
        }

        // 3. STAFF: Chỉ được soát vé và xem danh sách chuyến tàu
        if ("STAFF".equalsIgnoreCase(role)) {
            String uri = request.getRequestURI();
            String contextPath = request.getContextPath();
            String path = (contextPath != null && !contextPath.isEmpty()) ? uri.substring(contextPath.length()) : uri;
            String method = request.getMethod();

            // Cho phép toàn quyền ở trang soát vé
            if (path.startsWith("/admin/checkin")) {
                return true;
            }

            // Cho phép xem danh sách chuyến tàu (GET /admin/trips)
            if ("GET".equalsIgnoreCase(method) && ("/admin/trips".equals(path) || "/admin/trips/".equals(path))) {
                return true;
            }

            // Mọi thao tác khác (thêm/sửa/huỷ chuyến hoặc vào tàu, bến, tuyến, KM, loại vé) -> chặn
            sendRedirectWithFlashMessage(request, response, "/admin/checkin?error=forbidden",
                    "Nhân viên bến chỉ có quyền soát vé và xem lịch chuyến tàu.");
            return false;
        }

        // 4. Khách hàng thông thường (CUSTOMER) truy cập trái phép -> chuyển về trang chủ
        sendRedirectWithFlashMessage(request, response, "/?error=forbidden",
                "Bạn không có quyền truy cập trang quản trị.");
        return false;
    }

    private void sendRedirectWithFlashMessage(HttpServletRequest request, HttpServletResponse response,
                                              String redirectUrl, String message) throws Exception {
        try {
            FlashMap flashMap = new FlashMap();
            flashMap.put("errorMessage", message);
            var manager = RequestContextUtils.getFlashMapManager(request);
            if (manager != null) {
                manager.saveOutputFlashMap(flashMap, request, response);
            }
        } catch (Exception ignored) {
        }
        response.sendRedirect(request.getContextPath() + redirectUrl);
    }
}
