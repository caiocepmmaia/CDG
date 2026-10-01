package org.cdg;

import org.cdg.model.Cartao;
import org.cdg.model.Categoria;
import org.cdg.model.Meta;
import org.cdg.service.CartaoService;
import org.cdg.service.CategoriaService;
import org.cdg.service.MetaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class ServicosTest {

    private final CartaoService cartaoService = new CartaoService();
    private final CategoriaService categoriaService = new CategoriaService();
    private final MetaService metaService = new MetaService();

    @Test
    @DisplayName("Validação de Cartão: Deve rejeitar dias de vencimento ou fechamento inválidos")
    void deveRejeitarDiasInvalidosNoCartao() {
        // Tenta passar o dia 35 (inválido no calendário)
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            cartaoService.salvar(null, "Cartão Teste", "35", "10");
        });

        assertEquals("Os dias do mês devem estar entre 1 e 31.", exception.getMessage());
    }

    @Test
    @DisplayName("Validação de Categoria: Deve rejeitar nome vazio ou nulo")
    void deveRejeitarNomeVazioNaCategoria() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            categoriaService.salvar(null, "   ");
        });

        assertEquals("O nome da categoria não pode estar vazio.", exception.getMessage());
    }

    @Test
    @DisplayName("Validação de Metas: Deve rejeitar valor alvo menor ou igual a zero")
    void deveRejeitarValorAlvoInvalidoNaMeta() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            metaService.salvar(null, "Viagem", BigDecimal.ZERO, LocalDate.now().plusMonths(6));
        });

        assertEquals("O valor alvo deve ser maior que zero.", exception.getMessage());
    }

    @Test
    @DisplayName("Validação de Metas: Deve exigir descrição obrigatória")
    void deveExigirDescricaoNaMeta() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            metaService.salvar(null, "", new BigDecimal("1000.00"), LocalDate.now().plusMonths(6));
        });

        assertEquals("A descrição da meta é obrigatória.", exception.getMessage());
    }

    @Test
    @DisplayName("TransacaoService: Deve rejeitar descrição vazia ao processar transação")
    void deveRejeitarDescricaoVaziaNaTransacao() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            // Tentamos passar uma transação com descrição vazia
            org.cdg.service.TransacaoService service = new org.cdg.service.TransacaoService();
            service.processarESalvar(
                    null, "DESPESA",
                    org.cdg.model.Cartao.builder().idCartao(1).diaFechamento(10).diaVencimento(15).build(),
                    org.cdg.model.Categoria.builder().idCategoria(1).build(),
                    "   ", new BigDecimal("150.00"), LocalDate.now(), 1, false
            );
        });

        assertEquals("A descrição é obrigatória.", exception.getMessage());
    }

    @Test
    @DisplayName("TransacaoService: Deve rejeitar valor menor ou igual a zero")
    void deveRejeitarValorInvalidoNaTransacao() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            org.cdg.service.TransacaoService service = new org.cdg.service.TransacaoService();
            service.processarESalvar(
                    null, "DESPESA",
                    org.cdg.model.Cartao.builder().idCartao(1).diaFechamento(10).diaVencimento(15).build(),
                    org.cdg.model.Categoria.builder().idCategoria(1).build(),
                    "Compra", BigDecimal.ZERO, LocalDate.now(), 1, false
            );
        });

        assertEquals("O valor deve ser maior que zero.", exception.getMessage());
    }
}

