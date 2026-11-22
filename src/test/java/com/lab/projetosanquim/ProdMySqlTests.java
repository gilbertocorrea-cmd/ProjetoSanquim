package com.lab.projetosanquim;

import com.lab.projetosanquim.entity.Usuario;
import com.lab.projetosanquim.entity.Role;
import jakarta.persistence.EntityManagerFactory;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.core.io.ClassPathResource;

import java.sql.*;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Executar somente contra uma instância MySQL descartável.
 * SANQUIM_MYSQL_TEST_URL aponta para o servidor, sem nome de banco.
 */
@EnabledIfEnvironmentVariable(named = "SANQUIM_MYSQL_TEST_URL", matches = ".+")
class ProdMySqlTests {
    @Test
    void migraBancoVazioEValidaEntidadesNoPerfilProd() throws Exception {
        verificarProducao(false);
    }

    @Test
    void baselinePreservaBancoExistente() throws Exception {
        verificarProducao(true);
    }

    private void verificarProducao(boolean existente) throws Exception {
        String servidor = System.getenv("SANQUIM_MYSQL_TEST_URL");
        String usuario = System.getenv("SANQUIM_MYSQL_TEST_USER");
        String senha = System.getenv().getOrDefault("SANQUIM_MYSQL_TEST_PASSWORD", "");
        String banco = "sanquim_test_" + UUID.randomUUID().toString().replace("-", "");
        try (Connection admin = DriverManager.getConnection(servidor, usuario, senha);
             Statement statement = admin.createStatement()) {
            statement.execute("CREATE DATABASE " + banco);
            String url = servidor + "/" + banco;
            try {
                if (existente) {
                    try (Connection connection = DriverManager.getConnection(url, usuario, senha)) {
                        ScriptUtils.executeSqlScript(connection,
                                new ClassPathResource("db/migration/V1__esquema_inicial.sql"));
                        connection.createStatement().executeUpdate(
                                "INSERT INTO usuarios(email,role,senha_hash) VALUES "
                                        + "('baseline@sanquim.br','ROLE_GESTOR','fixture')");
                    }
                }

                try (ConfigurableApplicationContext context = new SpringApplicationBuilder(
                        ProjetoSanquimApplication.class).profiles("prod")
                        .run("--spring.datasource.url=" + url,
                                "--spring.datasource.username=" + usuario,
                                "--spring.datasource.password=" + senha,
                                "--SANQUIM_JWT_SECRET=" + ProjetoSanquimApplicationTests.CHAVE_TESTE,
                                "--sanquim.demo.seed=false",
                                "--server.port=0")) {
                    assertThat(context.getBean(Flyway.class).info().current().getVersion().toString())
                            .isEqualTo("2");
                    assertThat(context.getBean(Flyway.class).info().pending()).isEmpty();
                    EntityManagerFactory factory = context.getBean(EntityManagerFactory.class);
                    try (var manager = factory.createEntityManager()) {
                        if (existente) {
                            Usuario registro = manager.createQuery(
                                    "select u from Usuario u where u.email = :email", Usuario.class)
                                    .setParameter("email", "baseline@sanquim.br").getSingleResult();
                            assertThat(registro.getRole()).isEqualTo(Role.ROLE_GESTAO);
                        }
                    }
                }
                try (Connection connection = DriverManager.getConnection(url, usuario, senha);
                     ResultSet result = connection.createStatement().executeQuery(
                             "SELECT type,success FROM flyway_schema_history ORDER BY installed_rank")) {
                    assertThat(result.next()).isTrue();
                    assertThat(result.getString("type")).isEqualTo(existente ? "BASELINE" : "SQL");
                    assertThat(result.getBoolean("success")).isTrue();
                }
            } finally {
                statement.execute("DROP DATABASE " + banco);
            }
        }
    }
}
