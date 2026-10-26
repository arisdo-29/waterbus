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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class BookingRoleInterceptorTest {

    private BookingRoleInterceptor interceptor;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private HttpSession session;

    @BeforeEach
    void setUp() {
        interceptor = new BookingRoleInterceptor();
        request = Mockito.mock(HttpServletRequest.class);
        response = Mockito.mock(HttpServletResponse.class);
        session = Mockito.mock(HttpSession.class);

        when(request.getContextPath()).thenReturn("");
    }

    @Test
    void testPreHandle_NotLoggedIn_AllowsBooking() throws Exception {
        when(request.getSession(false)).thenReturn(null);

        boolean result = interceptor.preHandle(request, response, new Object());

        assertTrue(result);
        verify(response, never()).sendRedirect(anyString());
    }

    @Test
    void testPreHandle_CustomerRole_AllowsBooking() throws Exception {
        when(request.getSession(false)).thenReturn(session);
        AccountDTO customer = new AccountDTO(1, "customer@gmail.com", "Nguyen Van A", "0900000003", "CUSTOMER", "ACTIVE");
        when(session.getAttribute("loginAccount")).thenReturn(customer);

        boolean result = interceptor.preHandle(request, response, new Object());

        assertTrue(result);
        verify(response, never()).sendRedirect(anyString());
    }

    @Test
    void testPreHandle_StaffRole_AllowsBooking() throws Exception {
        when(request.getSession(false)).thenReturn(session);
        AccountDTO staff = new AccountDTO(2, "staff@waterbus.vn", "Staff", "0900000002", "STAFF", "ACTIVE");
        when(session.getAttribute("loginAccount")).thenReturn(staff);

        boolean result = interceptor.preHandle(request, response, new Object());

        assertTrue(result);
        verify(response, never()).sendRedirect(anyString());
    }

    @Test
    void testPreHandle_AdminRole_BlocksBooking() throws Exception {
        when(request.getSession(false)).thenReturn(session);
        AccountDTO admin = new AccountDTO(3, "admin@waterbus.vn", "Admin", "0900000001", "ADMIN", "ACTIVE");
        when(session.getAttribute("loginAccount")).thenReturn(admin);

        boolean result = interceptor.preHandle(request, response, new Object());

        assertFalse(result);
        verify(response, times(1)).sendRedirect("/trips");
    }
}
