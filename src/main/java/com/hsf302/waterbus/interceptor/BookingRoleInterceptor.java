package com.hsf302.waterbus.interceptor;

import com.hsf302.waterbus.dto.AccountDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.support.RequestContextUtils;

@Component
public class BookingRoleInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession(false);
        AccountDTO account = (session != null) ? (AccountDTO) session.getAttribute("loginAccount") : null;

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

        return true;
    }
}
