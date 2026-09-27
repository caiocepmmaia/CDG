package org.cdg;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TransacaoTest {

    @Test
    @DisplayName("Deve dividir R$ 100,00 em 3x sem perder centavos (33,34 na 1ª e 33,33 nas demais)")
    void deveDividirParcelasSemPerderCentavos() {
        // Arrange
        BigDecimal valorTotal = new BigDecimal("100.00");
        int totalParcelas = 3;

        // Act - Regra de divisão de parcelas
        BigDecimal valorBase = valorTotal.divide(new BigDecimal(totalParcelas), 2, RoundingMode.DOWN); // 33.33
        BigDecimal somaBase = valorBase.multiply(new BigDecimal(totalParcelas)); // 99.99
        BigDecimal diferencaCentavos = valorTotal.subtract(somaBase); // 0.01

        List<BigDecimal> parcelas = new ArrayList<>();
        for (int i = 1; i <= totalParcelas; i++) {
            if (i == 1) {
                parcelas.add(valorBase.add(diferencaCentavos)); // 1ª parcela: 33.34
            } else {
                parcelas.add(valorBase); // Demais: 33.33
            }
        }

        // Assert
        assertEquals(new BigDecimal("33.34"), parcelas.get(0));
        assertEquals(new BigDecimal("33.33"), parcelas.get(1));
        assertEquals(new BigDecimal("33.33"), parcelas.get(2));

        // Valida se a soma das parcelas bate 100% com o valor total inicial
        BigDecimal somaTotal = parcelas.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(valorTotal, somaTotal);
    }
}