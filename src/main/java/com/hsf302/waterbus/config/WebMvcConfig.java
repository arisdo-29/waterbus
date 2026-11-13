package com.hsf302.waterbus.config;

import com.hsf302.waterbus.interceptor.AdminInterceptor;
import com.hsf302.waterbus.interceptor.BookingRoleInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final AdminInterceptor adminInterceptor;
    private final BookingRoleInterceptor bookingRoleInterceptor;

    public WebMvcConfig(AdminInterceptor adminInterceptor, BookingRoleInterceptor bookingRoleInterceptor) {
        this.adminInterceptor = adminInterceptor;
        this.bookingRoleInterceptor = bookingRoleInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(adminInterceptor)
                .addPathPatterns("/admin/**");

        registry.addInterceptor(bookingRoleInterceptor)
                .addPathPatterns("/booking", "/booking/", "/booking/checkout");
    }
}
