package com.lab.projetosanquim.service;

import com.lab.projetosanquim.dto.LoginRequest;
import com.lab.projetosanquim.dto.LoginResponse;
import com.lab.projetosanquim.security.JwtService;
import com.lab.projetosanquim.security.UsuarioDetails;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(AuthenticationManager authenticationManager, JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    public LoginResponse login(LoginRequest request) {
        if (request.senha().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new BadCredentialsException("Credenciais inválidas");
        }

        var credenciais = UsernamePasswordAuthenticationToken.unauthenticated(request.email(), request.senha());
        var autenticacao = authenticationManager.authenticate(credenciais);
        return jwtService.gerar((UsuarioDetails) autenticacao.getPrincipal());
    }
}
