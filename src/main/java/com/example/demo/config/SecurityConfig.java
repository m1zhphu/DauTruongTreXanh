package com.example.demo.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.example.demo.security.CustomUserDetailsService;
import com.example.demo.security.JwtAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;
    private final CustomUserDetailsService userDetailsService;

    public SecurityConfig(JwtAuthenticationFilter jwtFilter, CustomUserDetailsService userDetailsService) {
        this.jwtFilter = jwtFilter;
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // 1. Cho phép mọi nguồn (Frontend, Mobile, LAN IP...)
        configuration.setAllowedOriginPatterns(Arrays.asList("*")); 
        
        // 2. Cho phép mọi method
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD"));
        
        // 3. Cho phép Header "Range" (QUAN TRỌNG ĐỂ PHÁT NHẠC)
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization", 
            "Content-Type", 
            "X-Requested-With", 
            "Accept", 
            "Origin", 
            "Access-Control-Request-Method", 
            "Access-Control-Request-Headers",
            "Range" // <--- Bắt buộc phải có để tua nhạc
        ));

        // 4. Cho phép Frontend ĐỌC các header trả về (QUAN TRỌNG ĐỂ HIỆN THANH THỜI GIAN)
        configuration.setExposedHeaders(Arrays.asList(
            "Access-Control-Allow-Origin", 
            "Access-Control-Allow-Credentials",
            "Content-Disposition",
            "Content-Length", // Để biết dung lượng file
            "Content-Range",  // Để biết đang phát giây thứ mấy
            "Accept-Ranges"   // Để báo hiệu server hỗ trợ tua
        ));

        configuration.setAllowCredentials(true); 

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> auth
                
                // ✅ 2. Cho phép phương thức OPTIONS (Preflight request) đi qua tất cả các endpoint
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                // ===== PUBLIC ENDPOINTS (Không cần token) =====
                .requestMatchers(
                    "/api/auth/**",
                    "/api/public/**",
                    "/api/menus/**",
                    "/api/upload/**",
                    "/api/events/next",
                    "/api/events/upcoming",
                    "/api/events/upcoming-list",
                    "/api/events/public-list",
                    "/api/game/topics",       
                    "/api/game/topics/**",
                    "/api/game/join",         
                    "/ws-game/**",            
                    "/ws-game",
                    "/api/gacha/pool",        // Gacha Pool công khai
                    "/api/upload/files/**",
                    "/files/**",               // Đề phòng trường hợp URL bị rewrite
                    "/uploads/**"
                ).permitAll()

                // ===== ADMIN RESTRICTED (Chỉ Admin mới vào được) =====
                // Lưu ý: Đảm bảo user trong DB có role là ROLE_ADMIN
                .requestMatchers("/api/admin/**").hasRole("ADMIN") 
                
                .requestMatchers(HttpMethod.POST, "/api/events/create").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/events/*/start").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/events/all").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/events/*/participants").hasRole("ADMIN")

                // ===== AUTHENTICATED REQUIRED (User đã đăng nhập) =====
                .requestMatchers("/api/map/**").authenticated()
                .requestMatchers("/api/users/**").authenticated()
                .requestMatchers("/api/game/**").authenticated() 
                
                // ✅ Thêm quyền Gacha (Trừ pool đã public ở trên)
                .requestMatchers("/api/gacha/**").authenticated() 

                // ===== FALLBACK =====
                .anyRequest().authenticated()
            );

        return http.build();
    }
}