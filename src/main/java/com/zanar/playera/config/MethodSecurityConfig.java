package com.zanar.playera.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@Configuration
@EnableMethodSecurity(securedEnabled = false, prePostEnabled = false)
public class MethodSecurityConfig {
  // This configuration explicitly disables method security
  // to prevent @PreAuthorize annotations from interfering with public endpoints
}
