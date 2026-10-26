package com.hsf302.waterbus.config;

import com.hsf302.waterbus.interceptor.AdminInterceptor;
import com.hsf302.waterbus.interceptor.BookingRoleInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// Cấu hình Spring MVC: Đăng ký các Interceptor
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final AdminInterceptor adminInterceptor;
    private final BookingRoleInterceptor bookingRoleInterceptor;

    // Constructor Injection
    public WebMvcConfig(AdminInterceptor adminInterceptor, BookingRoleInterceptor bookingRoleInterceptor) {
        this.adminInterceptor = adminInterceptor;
        this.bookingRoleInterceptor = bookingRoleInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Áp dụng kiểm tra quyền cho toàn bộ các URL bắt đầu bằng /admin/
        registry.addInterceptor(adminInterceptor)
                .addPathPatterns("/admin/**");

        // Chặn ADMIN truy cập luồng đặt vé thương mại (chỉ khách hàng và STAFF được phép)
        registry.addInterceptor(bookingRoleInterceptor)
                .addPathPatterns("/booking", "/booking/", "/booking/checkout");
    }
}
