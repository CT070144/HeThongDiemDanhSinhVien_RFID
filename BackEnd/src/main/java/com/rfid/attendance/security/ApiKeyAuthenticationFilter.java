package com.rfid.attendance.security;

import com.rfid.attendance.service.ApiKeyService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * Filter để xác thực API key từ ESP32
 * API key được gửi trong header "X-API-Key"
 */
@Component
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {
    
    private static final String API_KEY_HEADER = "X-API-Key";
    
    @Autowired
    private ApiKeyService apiKeyService;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        
        String apiKey = request.getHeader(API_KEY_HEADER);
        
        if (apiKey != null && !apiKey.isEmpty()) {
            // Validate API key
            var apiKeyOpt = apiKeyService.validateAndUpdateUsage(apiKey);
            
            if (apiKeyOpt.isPresent()) {
                // API key hợp lệ, tạo authentication token
                var apiKeyEntity = apiKeyOpt.get();
                
                // Tạo authentication với role ESP32_DEVICE
                UsernamePasswordAuthenticationToken authentication = 
                    new UsernamePasswordAuthenticationToken(
                        apiKeyEntity.getMaThietBi(), // Principal là mã thiết bị
                        null, // No credentials needed
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_ESP32_DEVICE"))
                    );
                
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
                
                // Lưu mã thiết bị vào request attribute để sử dụng sau này
                request.setAttribute("deviceId", apiKeyEntity.getMaThietBi());
            } else {
                // API key không hợp lệ
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Invalid or expired API key\"}");
                return;
            }
        }
        
        filterChain.doFilter(request, response);
    }
}

