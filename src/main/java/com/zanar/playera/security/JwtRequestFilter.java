package com.zanar.playera.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.core.GrantedAuthority;
import java.util.Collection;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {
  private static final Logger logger = LoggerFactory.getLogger(JwtRequestFilter.class);

  @Autowired
  private JwtUtil jwtUtil;
  @Autowired
  private CustomUserDetailsService userDetailsService;

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {

    String requestURI = request.getRequestURI();
    logger.debug("JWT Filter processing request: {}", requestURI);

    // Skip JWT processing only for public auth endpoints (login and register)
    if (requestURI.equals("/api/auth/login") || requestURI.equals("/api/auth/register")) {
      logger.debug("Skipping JWT processing for public endpoint: {}", requestURI);
      chain.doFilter(request, response);
      return;
    }

    final String authHeader = request.getHeader("Authorization");
    logger.debug("Authorization header: {}", authHeader != null ? "Present" : "Missing");

    // For protected endpoints, require valid Authorization header
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      logger.warn("Missing or invalid Authorization header for request: {}", requestURI);
      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      response.getWriter().write("{\"error\":\"Missing or invalid Authorization header\"}");
      return;
    }

    String username = null;
    String jwt = null;

    try {
      jwt = authHeader.substring(7);
      username = jwtUtil.extractUsername(jwt);
      logger.debug("Extracted username from JWT: {}", username);
    } catch (Exception e) {
      logger.error("Error extracting username from JWT: {}", e.getMessage());
      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      response.getWriter().write("{\"error\":\"Invalid JWT token format\"}");
      return;
    }

    if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
      try {
        logger.debug("Validating JWT token for username: {}", username);

        // Extract authorities from token
        Collection<? extends GrantedAuthority> authorities = jwtUtil.extractAuthorities(jwt);
        logger.debug("Authorities extracted from token: {}", authorities);

        // Simple token validation (check if not expired)
        if (!jwtUtil.isTokenExpired(jwt)) {
          logger.debug("JWT token validated successfully for user: {}", username);
          UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
              username, null, authorities);
          authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
          SecurityContextHolder.getContext().setAuthentication(authToken);
          logger.debug("Authentication set in SecurityContext for user: {} with authorities: {}", username,
              authorities);
        } else {
          logger.warn("JWT token has expired for user: {}", username);
          response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
          response.getWriter().write("{\"error\":\"JWT token expired\"}");
          return;
        }
      } catch (Exception e) {
        logger.error("Authentication failed for user {}: {}", username, e.getMessage());
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.getWriter().write("{\"error\":\"Authentication failed\"}");
        return;
      }
    } else {
      logger.debug("Username is null or authentication already exists for request: {}", requestURI);
    }

    logger.debug("JWT Filter completed successfully for request: {}", requestURI);
    chain.doFilter(request, response);
  }
}
