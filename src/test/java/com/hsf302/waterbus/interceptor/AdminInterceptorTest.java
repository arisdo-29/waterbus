package com.hsf302.waterbus.interceptor;

import com.hsf302.waterbus.dto.AccountDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class AdminInterceptorTest {

    private AdminInterceptor interceptor;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private HttpSession session;

    @BeforeEach
    void setUp() {
        interceptor = new AdminInterceptor();
        request = Mockito.mock(HttpServletRequest.class);
        response = Mockito.mock(HttpServletResponse.class);
        session = Mockito.mock(HttpSession.class);

        when(request.getContextPath()).thenReturn("");
    }

    @Test
    void testPreHandle_NotLoggedIn_RedirectsToLogin() throws Exception {
        when(request.getSession(false)).thenReturn(null);

        boolean result = interceptor.preHandle(request, response, new Object());

        assertFalse(result);
        verify(response, times(1)).sendRedirect("/auth/login?error=unauthorized");
    }

    @Test
    void testPreHandle_CustomerRole_RedirectsToHome() throws Exception {
        when(request.getSession(false)).thenReturn(session);
        AccountDTO customer = new AccountDTO(1, "customer@gmail.com", "Nguyen Van A", "0900000003", "CUSTOMER", "ACTIVE");
        when(session.getAttribute("loginAccount")).thenReturn(customer);

        boolean result = interceptor.preHandle(request, response, new Object());

        assertFalse(result);
        verify(response, times(1)).sendRedirect("/?error=forbidden");
    }

    @Test
    void testPreHandle_StaffRole_AllowsCheckinAndGetTrips() throws Exception {
        when(request.getSession(false)).thenReturn(session);
        AccountDTO staff = new AccountDTO(2, "staff@waterbus.vn", "Nhan vien", "0900000002", "STAFF", "ACTIVE");
        when(session.getAttribute("loginAccount")).thenReturn(staff);

        // 1. Soát vé -> Cho phép
        when(request.getRequestURI()).thenReturn("/admin/checkin");
        when(request.getMethod()).thenReturn("GET");
        assertTrue(interceptor.preHandle(request, response, new Object()));

        // 2. Xem danh sách chuyến tàu -> Cho phép
        when(request.getRequestURI()).thenReturn("/admin/trips");
        when(request.getMethod()).thenReturn("GET");
        assertTrue(interceptor.preHandle(request, response, new Object()));

        verify(response, never()).sendRedirect(anyString());
    }

    @Test
    void testPreHandle_StaffRole_BlocksRestrictedAdminActions() throws Exception {
        when(request.getSession(false)).thenReturn(session);
        AccountDTO staff = new AccountDTO(2, "staff@waterbus.vn", "Nhan vien", "0900000002", "STAFF", "ACTIVE");
        when(session.getAttribute("loginAccount")).thenReturn(staff);

        // 1. Vào trang Khuyến mãi -> Bị chặn
        when(request.getRequestURI()).thenReturn("/admin/promotions");
        when(request.getMethod()).thenReturn("GET");
        assertFalse(interceptor.preHandle(request, response, new Object()));
        verify(response, times(1)).sendRedirect("/admin/checkin?error=forbidden");

        // 2. Tạo chuyến mới (POST /admin/trips/save) -> Bị chặn
        when(request.getRequestURI()).thenReturn("/admin/trips/save");
        when(request.getMethod()).thenReturn("POST");
        assertFalse(interceptor.preHandle(request, response, new Object()));
    }

    @Test
    void testPreHandle_AdminRole_AllowsAccess() throws Exception {
        when(request.getSession(false)).thenReturn(session);
        AccountDTO admin = new AccountDTO(3, "admin@waterbus.vn", "Quan tri vien", "0900000001", "ADMIN", "ACTIVE");
        when(session.getAttribute("loginAccount")).thenReturn(admin);

        when(request.getRequestURI()).thenReturn("/admin/promotions");
        when(request.getMethod()).thenReturn("GET");

        boolean result = interceptor.preHandle(request, response, new Object());

        assertTrue(result);
        verify(response, never()).sendRedirect(anyString());
    }
}
