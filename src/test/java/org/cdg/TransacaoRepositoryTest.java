package org.cdg;

import org.cdg.model.Cartao;
import org.cdg.model.Categoria;
import org.cdg.model.ResumoMensalDTO;
import org.cdg.model.Transacao;
import org.cdg.repository.TransacaoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TransacaoRepositoryTest {

    private TransacaoRepository repository;

    @BeforeEach
    void setUp() {
        repository = new TransacaoRepository();
    }

    @Test
    @DisplayName("1. Deve salvar uma transação com todos os dados obrigatórios")
    void deveSalvarTransacaoComSucesso() throws SQLException {
        // Arrange - Criando dependências de Categoria e Cartão com IDs válidos
        Categoria categoria = Categoria.builder()
                .idCategoria(1)
                .nome("Alimentação")
                .build();

        Cartao cartao = Cartao.builder()
                .idCartao(1)
                .nome("Visa")
                .diaVencimento(10)
                .build();

        // Construindo a Transacao fornecendo dataRegisto e dataCobranca para evitar NullPointer
        Transacao transacao = Transacao.builder()
                .descricao("Compras Supermercado")
                .valor(new BigDecimal("250.50"))
                .dataRegisto(LocalDate.now())
                .dataCobranca(LocalDate.now())
                .parcelaAtual(1)
                .totalParcelas(1)
                .status("PAGO")
                .tipo("DESPESA")
                .categoria(categoria)
                .cartao(cartao)
                .reembolsavel(false)
                .build();

        // Act & Assert - Garante que o salvar executa no SQLite sem lançar exceções
        assertDoesNotThrow(() -> repository.salvar(transacao));
    }

    @Test
    @DisplayName("2. Deve obter o resumo mensal sem erros")
    void deveObterResumoMensal() throws SQLException {
        // Act
        ResumoMensalDTO resumo = repository.obterResumoMensal(LocalDate.now());

        // Assert
        assertNotNull(resumo, "O resumo mensal não deve ser nulo");
        assertNotNull(resumo.getTransacoes(), "A lista de transações não deve ser nula");
        assertNotNull(resumo.getTotalReceitas(), "Total de receitas não deve ser nulo");
        assertNotNull(resumo.getTotalDespesas(), "Total de despesas não deve ser nulo");
    }

    @Test
    @DisplayName("3. Deve listar transações reembolsáveis do mês")
    void deveListarReembolsaveisDoMes() throws SQLException {
        // Act
        List<Transacao> lista = repository.listarReembolsaveisPorMes(LocalDate.now());

        // Assert
        assertNotNull(lista, "A lista de reembolsáveis não deve ser nula");
    }
}