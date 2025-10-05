package com.appointment.auth;

import com.appointment.service.CustomUserDetailsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, CustomUserDetailsService userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        final String authorizationHeader = request.getHeader("Authorization");
        log.debug("JWT Filter - Authorization header: {}", authorizationHeader);

        String username = null;
        String jwt = null;

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwt = authorizationHeader.substring(7);
            log.debug("JWT Filter - Extracted token: {}", jwt);

            try {
                username = jwtUtil.getUsernameFromToken(jwt);
                log.debug("JWT Filter - Extracted username: {}", username);
            } catch (Exception e) {
                log.warn("JWT token validation failed: {}", e.getMessage());
            }
        } else {
            log.debug("JWT Filter - No Bearer token found");
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            log.debug("JWT Filter - Loading user details for: {}", username);
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

            if (jwtUtil.validateToken(jwt)) {
                log.debug("JWT Filter - Token validated successfully for user: {}", username);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("JWT Filter - Authentication set in SecurityContext");
            } else {
                log.debug("JWT Filter - Token validation failed for user: {}", username);
            }
        } else {
            log.debug("JWT Filter - Username null or already authenticated");
        }

        filterChain.doFilter(request, response);
    }
}