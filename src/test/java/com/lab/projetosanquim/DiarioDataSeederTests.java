package com.lab.projetosanquim;

import com.lab.projetosanquim.config.DiarioDataSeeder;
import com.lab.projetosanquim.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:seed-diario;DB_CLOSE_DELAY=-1;MODE=MySQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "sanquim.demo.seed=true",
        "SANQUIM_DEMO_PASSWORD=" + ProjetoSanquimApplicationTests.SENHA_TESTE,
        "SANQUIM_JWT_SECRET=" + ProjetoSanquimApplicationTests.CHAVE_TESTE
})
class DiarioDataSeederTests {
    @Autowired private DiarioDataSeeder seeder;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private AtribuicaoDocenteRepository atribuicoes;
    @Autowired private MatriculaRepository matriculas;
    @Autowired private ChamadaRepository chamadas;
    @Autowired private FrequenciaRepository frequencias;
    @Autowired private AvaliacaoRepository avaliacoes;
    @Autowired private NotaRepository notas;

    @Test
    void cargaCompletaERepeticaoPreservamQuantidadeDeRegistros() {
        seeder.run();
        assertThat(usuarios.count()).isEqualTo(8);
        assertThat(atribuicoes.count()).isEqualTo(2);
        assertThat(matriculas.count()).isEqualTo(5);
        assertThat(chamadas.count()).isEqualTo(1);
        assertThat(frequencias.count()).isEqualTo(5);
        assertThat(avaliacoes.count()).isEqualTo(1);
        assertThat(notas.count()).isEqualTo(5);
    }

    @Test
    void reinicializacaoNaoSobrescreveNotaExistente() {
        var nota = notas.findAll().get(0);
        nota.setValor(BigDecimal.valueOf(9));
        notas.saveAndFlush(nota);
        seeder.run();
        assertThat(notas.findById(nota.getId()).orElseThrow().getValor()).isEqualByComparingTo("9");
    }
}
