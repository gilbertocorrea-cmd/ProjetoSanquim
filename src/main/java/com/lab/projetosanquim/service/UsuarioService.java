package com.lab.projetosanquim.service;

import com.lab.projetosanquim.entity.Role;
import com.lab.projetosanquim.entity.Usuario;
import com.lab.projetosanquim.exception.RegraNegocioException;
import com.lab.projetosanquim.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UsuarioService {
    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;

    public Usuario criarUsuario(String email, String senha, Role role) {
        String emailNormalizado = email.strip().toLowerCase(Locale.ROOT);
        if (usuarios.existsByEmailIgnoreCase(emailNormalizado)) {
            throw new RegraNegocioException("Já existe um usuário com este e-mail");
        }
        return Usuario.builder()
                .email(emailNormalizado)
                .senhaHash(passwordEncoder.encode(senha))
                .role(role)
                .build();
    }
}
