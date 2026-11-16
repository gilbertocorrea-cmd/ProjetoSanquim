package com.lab.projetosanquim.security;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final String BEARER = "Bearer ";

    private final JwtService jwtService;
    private final UsuarioDetailsService usuarios;
    private final RestAuthenticationEntryPoint entryPoint;

    public JwtAuthenticationFilter(
            JwtService jwtService, UsuarioDetailsService usuarios, RestAuthenticationEntryPoint entryPoint
    ) {
        this.jwtService = jwtService;
        this.usuarios = usuarios;
        this.entryPoint = entryPoint;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return "POST".equals(request.getMethod())
                && (request.getContextPath() + "/api/auth/login").equals(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain
    ) throws ServletException, IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.regionMatches(true, 0, BEARER, 0, BEARER.length())) {
            chain.doFilter(request, response);
            return;
        }

        try {
            DecodedJWT token = jwtService.validar(authorization.substring(BEARER.length()));
            UsuarioDetails usuario = usuarios.loadById(Long.valueOf(token.getSubject()));
            if (!usuario.getRole().name().equals(token.getClaim("role").asString())) {
                throw new BadCredentialsException("Perfil do token inválido");
            }

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(
                    UsernamePasswordAuthenticationToken.authenticated(usuario, null, usuario.getAuthorities()));
            SecurityContextHolder.setContext(context);
        } catch (JWTVerificationException | NumberFormatException | UsernameNotFoundException
                 | BadCredentialsException exception) {
            SecurityContextHolder.clearContext();
            entryPoint.commence(request, response, new BadCredentialsException("Token inválido ou expirado"));
            return;
        }

        chain.doFilter(request, response);
    }
}
