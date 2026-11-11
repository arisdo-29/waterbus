package com.hsf302.waterbus.interceptor;

import com.hsf302.waterbus.dto.AccountDTO;
import com.hsf302.waterbus.service.AccountService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.support.RequestContextUtils;

// Module BE3: Phân quyền bảo vệ các trang quản trị /admin/**
// ADMIN: Toàn quyền truy cập mọi trang quản trị
// STAFF: Chỉ soát vé (/admin/checkin/**) và xem danh sách chuyến tàu (GET /admin/trips)
@Component
public class AdminInterceptor implements HandlerInterceptor {

    private final AccountService accountService;

    public AdminInterceptor() {
        this.accountService = null;
    }

    public AdminInterceptor(@Autowired(required = false) AccountService accountService) {
        this.accountService = accountService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession(false);
        AccountDTO account = (session != null) ? (AccountDTO) session.getAttribute("loginAccount") : null;

        // 1. Chưa đăng nhập -> lưu đường dẫn nội bộ và chuyển hướng về trang đăng nhập
        if (account == null) {
            String uri = request.getRequestURI();
            if (uri != null) {
                String query = request.getQueryString();
                String targetUrl = (query != null && !query.isEmpty()) ? uri + "?" + query : uri;
                String contextPath = request.getContextPath();
                if (contextPath != null && !contextPath.isEmpty() && targetUrl.startsWith(contextPath)) {
                    targetUrl = targetUrl.substring(contextPath.length());
                }
                if (targetUrl.startsWith("/") && !targetUrl.startsWith("//") && !targetUrl.contains("\\")) {
                    HttpSession newSession = request.getSession(true);
                    if (newSession != null) {
                        newSession.setAttribute("redirectAfterLogin", targetUrl);
                    }
                }
            }
            response.sendRedirect(request.getContextPath() + "/auth/login?error=unauthorized");
            return false;
        }

        // Kiểm tra trạng thái tài khoản: Nếu bị khoá trong DB thì tự động đăng xuất
        if (accountService != null && account.getId() != null) {
            if (accountService.isLocked(account.getId())) {
                session.removeAttribute("loginAccount");
                session.invalidate();
                sendRedirectWithFlashMessage(request, response, "/auth/login?error=locked",
                        "Tài khoản của bạn đã bị khóa. Vui lòng liên hệ quản trị viên.");
                return false;
            }
        } else if ("LOCKED".equalsIgnoreCase(account.getStatus())) {
            session.removeAttribute("loginAccount");
            session.invalidate();
            sendRedirectWithFlashMessage(request, response, "/auth/login?error=locked",
                    "Tài khoản của bạn đã bị khóa. Vui lòng liên hệ quản trị viên.");
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
