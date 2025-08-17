package com.zanar.playera.config;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Custom interceptor for Swagger UI requests to enhance the API testing
 * experience.
 * This interceptor can be used to add custom headers, logging, or validation
 * for requests made through the Swagger UI.
 */
@Component
public class SwaggerRequestInterceptor implements HandlerInterceptor {

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
    // Add custom headers for Swagger UI requests
    if (request.getRequestURI().contains("/swagger-ui")) {
      response.setHeader("X-Swagger-Request", "true");
      response.setHeader("X-PlayEra-API", "v1.0.0");
    }

    // Log Swagger UI requests for debugging
    if (request.getRequestURI().contains("/swagger-ui") || request.getRequestURI().contains("/api-docs")) {
      System.out.println("Swagger UI Request: " + request.getMethod() + " " + request.getRequestURI());
    }

    return true;
  }
}
