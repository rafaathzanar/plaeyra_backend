package com.zanar.playera.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

  @Value("${app.base-url:http://localhost:8080}")
  private String baseUrl;

  @Value("${app.frontend-url:http://localhost:3000}")
  private String frontendUrl;

  @Bean
  public OpenAPI customOpenAPI() {
    return new OpenAPI()
        .info(apiInfo())
        .servers(servers())
        .components(components())
        .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
        .addSecurityItem(new SecurityRequirement().addList("Basic Authentication"));
  }

  private Info apiInfo() {
    return new Info()
        .title("PlayEra API Documentation")
        .description("""
            Comprehensive API documentation for the PlayEra sports venue booking platform.

            ## Overview
            PlayEra is a complete sports venue booking platform that allows users to:
            - Discover and book sports venues and courts
            - Rent sports equipment
            - Manage bookings and payments
            - Participate in loyalty programs
            - Leave reviews and ratings

            ## Key Features
            - **User Management**: Registration, authentication, and profile management
            - **Venue Management**: CRUD operations for venues with advanced filtering
            - **Court Management**: Court availability and booking management
            - **Equipment Rental**: Sports equipment rental system
            - **Booking System**: Real-time availability and booking management
            - **Payment Processing**: Stripe integration for secure payments
            - **Dynamic Pricing**: Peak hour and demand-based pricing
            - **Loyalty Program**: Points-based reward system
            - **Review System**: Multi-type review and rating system
            - **Notification System**: Push notifications and email alerts

            ## Authentication
            The API uses JWT (JSON Web Token) authentication. Include the token in the Authorization header:
            ```
            Authorization: Bearer <your-jwt-token>
            ```

            ## Rate Limiting
            API endpoints are rate-limited to ensure fair usage. Please respect the rate limits.

            ## Support
            For technical support or questions, please contact our development team.
            """)
        .version("1.0.0")
        .contact(new Contact()
            .name("PlayEra Development Team")
            .email("dev@playera.com")
            .url("https://playera.com"))
        .license(new License()
            .name("MIT License")
            .url("https://opensource.org/licenses/MIT"));
  }

  private List<Server> servers() {
    return List.of(
        new Server()
            .url(baseUrl)
            .description("Production Server"),
        new Server()
            .url("http://localhost:8080")
            .description("Local Development Server"),
        new Server()
            .url("https://api.playera.com")
            .description("Staging Server"));
  }

  private Components components() {
    return new Components()
        .addSecuritySchemes("Bearer Authentication", new SecurityScheme()
            .type(SecurityScheme.Type.HTTP)
            .scheme("bearer")
            .bearerFormat("JWT")
            .description("Enter your JWT token in the format: Bearer <token>"))
        .addSecuritySchemes("Basic Authentication", new SecurityScheme()
            .type(SecurityScheme.Type.HTTP)
            .scheme("basic")
            .description("Basic authentication for admin endpoints"));
  }
}