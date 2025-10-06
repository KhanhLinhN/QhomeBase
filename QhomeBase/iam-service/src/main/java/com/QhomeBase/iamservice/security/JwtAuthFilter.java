package com.QhomeBase.iamservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    @Autowired
    private JwtVerifier jwtVerifier;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            try {
                String token = auth.substring(7);
                Claims claims = jwtVerifier.verify(token);
                

                UUID uid = UUID.fromString(claims.get("uid", String.class));
                String username = claims.getSubject(); // Sử dụng getSubject() thay vì get("username")
                UUID tenant = UUID.fromString(claims.get("tenant", String.class));
                List<String> roles = claims.get("roles", List.class);
                List<String> perms = claims.get("perms", List.class);
                

                if (uid == null || username == null || tenant == null || roles == null || perms == null) {
                    throw new IllegalArgumentException("Invalid JWT claims");
                }
                
                var authorities = new ArrayList<SimpleGrantedAuthority>();
                for (String role : roles) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
                }
                for (String perm : perms) {
                    authorities.add(new SimpleGrantedAuthority("PERM_" + perm));
                }
                
                var principal = new UserPrincipal(uid, username, tenant, roles, perms, token);
                var authn = new UsernamePasswordAuthenticationToken(principal, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authn);
            }
            catch (Exception e) {

                SecurityContextHolder.clearContext();
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
        }
        
        // Tiếp tục filter chain
        filterChain.doFilter(request, response);
    }
}
