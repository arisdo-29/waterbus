package com.hsf302.waterbus.config;

import com.hsf302.waterbus.interceptor.AdminInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// Cấu hình Spring MVC: Đăng ký các Interceptor
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final AdminInterceptor adminInterceptor;

    // Constructor Injection
    public WebMvcConfig(AdminInterceptor adminInterceptor) {
        this.adminInterceptor = adminInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Áp dụng kiểm tra quyền cho toàn bộ các URL bắt đầu bằng /admin/
        registry.addInterceptor(adminInterceptor)
                .addPathPatterns("/admin/**");
    }
}
