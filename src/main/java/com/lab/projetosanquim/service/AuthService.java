package com.lab.projetosanquim.service;

import com.lab.projetosanquim.dto.LoginRequest;
import com.lab.projetosanquim.dto.LoginResponse;
import com.lab.projetosanquim.security.JwtService;
import com.lab.projetosanquim.security.LoginAttemptLimiter;
import com.lab.projetosanquim.security.UsuarioDetails;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

import com.lab.projetosanquim.validation.MaxBytes;
import com.lab.projetosanquim.validation.MaxBytesValidator;

@Service
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final LoginAttemptLimiter limiter;

    public AuthService(AuthenticationManager authenticationManager, JwtService jwtService, LoginAttemptLimiter limiter) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.limiter = limiter;
    }

    public LoginResponse login(LoginRequest request, String ip) {
        return limiter.executar(request.email(), ip, () -> autenticar(request));
    }

    private LoginResponse autenticar(LoginRequest request) {
        if (request.senha() == null || request.senha().isBlank()
                || !MaxBytesValidator.cabeEmUtf8(request.senha(), MaxBytes.LIMITE_SENHA)) {
            throw new BadCredentialsException("Credenciais inválidas");
        }

        var credenciais = UsernamePasswordAuthenticationToken.unauthenticated(request.email(), request.senha());
        var autenticacao = authenticationManager.authenticate(credenciais);
        return jwtService.gerar((UsuarioDetails) autenticacao.getPrincipal());
    }
}
