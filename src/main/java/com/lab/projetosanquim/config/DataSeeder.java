package com.lab.projetosanquim.config;

import com.lab.projetosanquim.entity.Aluno;
import com.lab.projetosanquim.entity.Gestor;
import com.lab.projetosanquim.entity.Professor;
import com.lab.projetosanquim.entity.Role;
import com.lab.projetosanquim.entity.Usuario;
import com.lab.projetosanquim.repository.AlunoRepository;
import com.lab.projetosanquim.repository.GestorRepository;
import com.lab.projetosanquim.repository.ProfessorRepository;
import com.lab.projetosanquim.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.core.annotation.Order;
import org.springframework.transaction.annotation.Transactional;

import com.lab.projetosanquim.validation.MaxBytes;
import com.lab.projetosanquim.validation.MaxBytesValidator;

@Component
@Order(0)
@ConditionalOnProperty(name = "sanquim.demo.seed", havingValue = "true")
public class DataSeeder implements CommandLineRunner {
    private final UsuarioRepository usuarios;
    private final GestorRepository gestores;
    private final ProfessorRepository professores;
    private final AlunoRepository alunos;
    private final PasswordEncoder encoder;
    private final String senha;

    public DataSeeder(
            UsuarioRepository usuarios, GestorRepository gestores, ProfessorRepository professores,
            AlunoRepository alunos, PasswordEncoder encoder, @Value("${SANQUIM_DEMO_PASSWORD:}") String senha
    ) {
        this.usuarios = usuarios;
        this.gestores = gestores;
        this.professores = professores;
        this.alunos = alunos;
        this.encoder = encoder;
        this.senha = senha;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (senha.isBlank() || !MaxBytesValidator.cabeEmUtf8(senha, MaxBytes.LIMITE_SENHA)) {
            throw new IllegalStateException("Defina SANQUIM_DEMO_PASSWORD com uma senha de até 72 bytes");
        }

        String adminEmail = "admin@sanquim.br";
        if (!usuarios.existsByEmail(adminEmail)) {
            gestores.save(Gestor.builder()
                    .nome("Gestão de teste")
                    .cargo("Gestor")
                    .usuario(usuario(adminEmail, Role.ROLE_GESTAO))
                    .build());
        }

        for (int i = 1; i <= 2; i++) {
            String email = "professor" + i + "@sanquim.br";
            if (!usuarios.existsByEmail(email)) {
                professores.save(Professor.builder()
                        .nome("Professor de teste " + i)
                        .usuario(usuario(email, Role.ROLE_PROFESSOR))
                        .build());
            }
        }

        for (int i = 1; i <= 5; i++) {
            String email = "aluno" + i + "@sanquim.br";
            if (!usuarios.existsByEmail(email)) {
                alunos.save(Aluno.builder()
                        .nome("Aluno de teste " + i)
                        .usuario(usuario(email, Role.ROLE_ALUNO))
                        .build());
            }
        }
    }

    private Usuario usuario(String email, Role role) {
        return Usuario.builder().email(email).senhaHash(encoder.encode(senha)).role(role).build();
    }
}
