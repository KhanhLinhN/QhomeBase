package com.QhomeBase.baseservice.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class JwtAuthFilter {
    private final JwtVerifier jwtVerifier;
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        String authorizationHeader = request.getHeader("Authorization");
        String authToken = null;
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            authToken = authorizationHeader.substring(7);
        }
        Claims claims = jwtVerifier.verify(authToken);
        UUID uid = UUID.fromString(claims.get("uid", String.class));
        String username = claims.getSubject();
        UUID tenant = UUID.fromString(claims.get("tenant", String.class));
        List<String> roles = claims.get("roles", List.class);
        List<String> perms = claims.get("perms", List.class);
        List<SimpleGrantedAuthority> authorities = new ArrayList<SimpleGrantedAuthority>();


        for (String role : roles) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
        }
        for (String perm : perms) {
            authorities.add(new SimpleGrantedAuthority("PERM_" + perm));
        }

        var principal = new UserPrincipal(uid, username, tenant, roles, perms, authToken);
        var authn = new UsernamePasswordAuthenticationToken(principal, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authn);
        filterChain.doFilter(request, response);

    }
}
