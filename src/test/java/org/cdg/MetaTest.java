package org.cdg;

import org.cdg.model.Meta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MetaTest {

    @Test
    @DisplayName("1. Deve calcular o valor restante da meta corretamente")
    void deveCalcularValorRestanteDaMeta() {
        // Arrange
        Meta meta = Meta.builder()
                .descricao("Reserva")
                .valorAlvo(new BigDecimal("10000.00"))
                .valorAtual(new BigDecimal("2500.00"))
                .build();

        // Act
        BigDecimal falta = meta.getValorAlvo().subtract(meta.getValorAtual());

        // Assert
        assertEquals(new BigDecimal("7500.00"), falta);
    }

    @Test
    @DisplayName("2. Deve calcular a porcentagem de progresso da meta corretamente")
    void deveCalcularPorcentagemDeProgresso() {
        // Arrange
        Meta meta = Meta.builder()
                .descricao("Motos")
                .valorAlvo(new BigDecimal("25000.00"))
                .valorAtual(new BigDecimal("4636.66"))
                .build();

        // Act: (4.636,66 * 100) / 25.000,00 = 18,54664... -> Arredondado para 19%
        BigDecimal percentual = meta.getValorAtual()
                .multiply(new BigDecimal("100"))
                .divide(meta.getValorAlvo(), 0, RoundingMode.HALF_UP);

        // Assert
        assertEquals(new BigDecimal("19"), percentual);
    }

    @Test
    @DisplayName("3. Não deve permitir valor em falta negativo quando o acumulado ultrapassa o alvo")
    void naoDeveRetornarValorFaltaNegativo() {
        // Arrange: Guarda mais do que o alvo estipulado
        Meta meta = Meta.builder()
                .descricao("Viagem")
                .valorAlvo(new BigDecimal("3000.00"))
                .valorAtual(new BigDecimal("3500.00"))
                .build();

        // Act
        BigDecimal falta = meta.getValorAlvo().subtract(meta.getValorAtual());
        if (falta.compareTo(BigDecimal.ZERO) < 0) {
            falta = BigDecimal.ZERO;
        }

        // Assert
        assertEquals(BigDecimal.ZERO, falta);
    }

    @Test
    @DisplayName("4. Deve incrementar o valor atual corretamente ao realizar um aporte")
    void deveIncrementarValorAtualAosSalvarAporte() {
        // Arrange
        Meta meta = Meta.builder()
                .descricao("Carro")
                .valorAlvo(new BigDecimal("60000.00"))
                .valorAtual(new BigDecimal("15000.00"))
                .build();

        BigDecimal valorAporte = new BigDecimal("2500.00");

        // Act
        BigDecimal novoValorAtual = meta.getValorAtual().add(valorAporte);
        meta.setValorAtual(novoValorAtual);

        // Assert
        assertEquals(new BigDecimal("17500.00"), meta.getValorAtual());
    }
    @Test
    @DisplayName("5. Deve identificar corretamente quando uma meta foi concluída")
    void deveIdentificarMetaConcluida() {
        // Arrange
        Meta meta = Meta.builder()
                .descricao("Viagem")
                .valorAlvo(new BigDecimal("5000.00"))
                .valorAtual(new BigDecimal("5000.00"))
                .build();

        // Act - Verifica se valorAtual >= valorAlvo
        boolean concluida = meta.getValorAtual().compareTo(meta.getValorAlvo()) >= 0;

        // Assert
        org.junit.jupiter.api.Assertions.assertTrue(concluida);
    }

    @Test
    @DisplayName("6. Deve subtrair valor corretamente ao realizar um resgate da meta")
    void deveSubtrairValorAoRealizarResgate() {
        // Arrange
        Meta meta = Meta.builder()
                .descricao("Emergência")
                .valorAlvo(new BigDecimal("10000.00"))
                .valorAtual(new BigDecimal("4000.00"))
                .build();

        BigDecimal valorResgate = new BigDecimal("1500.00");

        // Act
        BigDecimal novoValorAtual = meta.getValorAtual().subtract(valorResgate);
        meta.setValorAtual(novoValorAtual);

        // Assert
        assertEquals(new BigDecimal("2500.00"), meta.getValorAtual());
    }

    @Test
    @DisplayName("7. Não deve permitir que o valor atual fique negativo após resgate excessivo")
    void naoDevePermitirValorAtualNegativoAposResgate() {
        // Arrange
        Meta meta = Meta.builder()
                .descricao("Reserva")
                .valorAlvo(new BigDecimal("5000.00"))
                .valorAtual(new BigDecimal("1000.00"))
                .build();

        BigDecimal valorResgate = new BigDecimal("1500.00");

        // Act - Regra de proteção: se resgate > valorAtual, define como ZERO
        BigDecimal novoValorAtual = meta.getValorAtual().subtract(valorResgate);
        if (novoValorAtual.compareTo(BigDecimal.ZERO) < 0) {
            novoValorAtual = BigDecimal.ZERO;
        }
        meta.setValorAtual(novoValorAtual);

        // Assert
        assertEquals(BigDecimal.ZERO, meta.getValorAtual());
    }
}