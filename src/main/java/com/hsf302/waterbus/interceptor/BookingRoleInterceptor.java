package com.hsf302.waterbus.interceptor;

import com.hsf302.waterbus.dto.AccountDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.support.RequestContextUtils;

// Module BE3: Phân quyền đặt vé
// Khách vãng lai, CUSTOMER và STAFF (đặt vé hộ khách tại bến) được phép đặt vé.
// ADMIN: Không được phép đặt vé thương mại (tránh chiếm giữ chỗ của hành khách).
@Component
public class BookingRoleInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession(false);
        AccountDTO account = (session != null) ? (AccountDTO) session.getAttribute("loginAccount") : null;

        // Nếu đã đăng nhập với vai trò ADMIN -> chặn đặt vé
        if (account != null && "ADMIN".equalsIgnoreCase(account.getRole())) {
            try {
                FlashMap flashMap = new FlashMap();
                flashMap.put("errorMessage", "Tài khoản Quản trị viên không được phép đặt vé thương mại. Vui lòng sử dụng tài khoản Nhân viên hoặc Khách hàng.");
                var manager = RequestContextUtils.getFlashMapManager(request);
                if (manager != null) {
                    manager.saveOutputFlashMap(flashMap, request, response);
                }
            } catch (Exception ignored) {
            }
            response.sendRedirect(request.getContextPath() + "/trips");
            return false;
        }

        // Khách chưa đăng nhập, CUSTOMER và STAFF đều được phép tiếp tục đặt vé
        return true;
    }
}
