package com.lab.projetosanquim.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.lab.projetosanquim.dto.LoginResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

@Service
public class JwtService {
    private static final String EMISSOR = "projetoSanquim";
    private static final Duration VALIDADE = Duration.ofHours(1);
    private static final int CHAVE_MINIMA_BYTES = 32;

    private final Algorithm algorithm;
    private final JWTVerifier verifier;

    public JwtService(
            @Value("${sanquim.jwt.secret:${SANQUIM_JWT_SECRET:}}") String segredoBase64,
            @Value("${sanquim.jwt.require-secret:false}") boolean exigeSegredo
    ) {
        if (exigeSegredo && segredoBase64.isBlank()) {
            throw new IllegalArgumentException("SANQUIM_JWT_SECRET é obrigatório no perfil prod");
        }
        byte[] chave = new byte[CHAVE_MINIMA_BYTES];
        if (segredoBase64.isBlank()) {
            new SecureRandom().nextBytes(chave);
        } else {
            chave = Base64.getDecoder().decode(segredoBase64.trim());
            if (chave.length < CHAVE_MINIMA_BYTES) {
                throw new IllegalArgumentException("SANQUIM_JWT_SECRET deve conter ao menos 32 bytes em Base64");
            }
        }

        this.algorithm = Algorithm.HMAC256(chave);
        this.verifier = JWT.require(algorithm)
                .withIssuer(EMISSOR)
                .withClaimPresence("sub")
                .withClaimPresence("exp")
                .withClaimPresence("role")
                .build();
    }

    public LoginResponse gerar(UsuarioDetails usuario) {
        Instant agora = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        Instant expiraEm = agora.plus(VALIDADE);
        String token = JWT.create()
                .withIssuer(EMISSOR)
                .withSubject(usuario.getId().toString())
                .withClaim("role", usuario.getRole().name())
                .withIssuedAt(agora)
                .withExpiresAt(expiraEm)
                .sign(algorithm);
        return new LoginResponse(token, expiraEm, usuario.getRole());
    }

    public DecodedJWT validar(String token) {
        return verifier.verify(token);
    }
}
