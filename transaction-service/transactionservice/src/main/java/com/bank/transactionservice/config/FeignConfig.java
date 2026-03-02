package com.bank.transactionservice.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class FeignConfig implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();

            // Automatically propagate the security headers to Account Service
            if (request.getHeader("X-User-Name") != null) {
                template.header("X-User-Name", request.getHeader("X-User-Name"));
            }
            if (request.getHeader("X-User-Roles") != null) {
                template.header("X-User-Roles", request.getHeader("X-User-Roles"));
            }
        }
    }
}