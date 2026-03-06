package com.example.demo.security;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        
        String path = request.getRequestURI();
        if (path.startsWith("/api/auth") || path.startsWith("/api/public") || path.startsWith("/api/health")) {
            filterChain.doFilter(request, response);
            return; // Bỏ qua không kiểm tra token cho các đường dẫn này
        }

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String username;

        // 1. Kiểm tra header có chứa Bearer Token không
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. Lấy token
        jwt = authHeader.substring(7);
        
        // 3. Trích xuất username từ token
        try {
            username = jwtService.extractUsername(jwt);
        } catch (Exception e) {
            // Nếu token lỗi thì bỏ qua, để Spring Security tự xử lý 403 sau
            filterChain.doFilter(request, response);
            return;
        }

        // 4. Nếu có username và chưa được xác thực trong Context
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
            
            // 5. Nếu token hợp lệ, set thông tin vào SecurityContext
            if (jwtService.isTokenValid(jwt, userDetails.getUsername())) {
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }
        
        filterChain.doFilter(request, response);
    }
}