package com.familyhub.demo.security;

import com.familyhub.demo.model.Family;
import com.familyhub.demo.service.FamilyService;
import com.familyhub.demo.service.JwtService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    public static final String JWT_ERROR_ATTRIBUTE = "jwt-error";
    private static final String AUTHORIZATION_HEADER = "Authorization";

    private final JwtService jwtService;
    private final FamilyService familyService;


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        // Check incoming request for JWT in header
        String token = resolveToken(request);

        // Attempt authentication
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                // 1.) validate
                String uuid = jwtService.extractSubject(token);
                Family family = familyService.findFamilyById(UUID.fromString(uuid));

                // 2.) Set security context
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(family, null, family.getAuthorities());

                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (JwtException e) {
                // Store error in request object
                log.debug("JWT validation failed: {}", e.getMessage());
                request.setAttribute(JWT_ERROR_ATTRIBUTE, e);
            } catch (Exception e) {
                log.warn("Unexpected error during authentication", e);
            }
        }
        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}