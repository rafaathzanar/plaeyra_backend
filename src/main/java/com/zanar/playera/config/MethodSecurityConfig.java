package com.zanar.playera.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@Configuration
@EnableMethodSecurity(securedEnabled = true, prePostEnabled = true)
public class MethodSecurityConfig {
  // This configuration enables method security
  // to allow @PreAuthorize annotations to work properly
}
