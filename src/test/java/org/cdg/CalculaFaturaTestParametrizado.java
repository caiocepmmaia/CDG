package org.cdg;

import org.cdg.model.Cartao;
import org.cdg.service.TransacaoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CalculaFaturaTestParametrizado {

    private final TransacaoService service = new TransacaoService();

    private LocalDate chamarCalcularDataCobranca(
            LocalDate dataCompra,
            Cartao cartao
    ) throws Exception {

        Method metodo = TransacaoService.class.getDeclaredMethod(
                "calcularDataCobranca",
                LocalDate.class,
                Cartao.class
        );

        metodo.setAccessible(true);

        return (LocalDate) metodo.invoke(service, dataCompra, cartao);
    }

    static Stream<Arguments> cenariosCalculoFatura() {

        return Stream.of(

                // Cenário 1
                Arguments.of(
                        "Compra antes do fechamento",
                        LocalDate.of(2026, 5, 5),
                        Cartao.builder()
                                .diaFechamento(10)
                                .diaVencimento(15)
                                .build(),
                        LocalDate.of(2026, 5, 15)
                ),

                // Cenário 2
                Arguments.of(
                        "Compra exatamente no fechamento",
                        LocalDate.of(2026, 5, 10),
                        Cartao.builder()
                                .diaFechamento(10)
                                .diaVencimento(15)
                                .build(),
                        LocalDate.of(2026, 5, 15)
                ),

                // Cenário 3
                Arguments.of(
                        "Compra após o fechamento",
                        LocalDate.of(2026, 5, 12),
                        Cartao.builder()
                                .diaFechamento(10)
                                .diaVencimento(15)
                                .build(),
                        LocalDate.of(2026, 6, 15)
                ),

                // Cenário 4
                Arguments.of(
                        "Vencimento anterior ao fechamento",
                        LocalDate.of(2026, 5, 20),
                        Cartao.builder()
                                .diaFechamento(25)
                                .diaVencimento(5)
                                .build(),
                        LocalDate.of(2026, 6, 5)
                ),

                // Cenário 5
                Arguments.of(
                        "Virada de ano após o fechamento",
                        LocalDate.of(2026, 12, 15),
                        Cartao.builder()
                                .diaFechamento(10)
                                .diaVencimento(20)
                                .build(),
                        LocalDate.of(2027, 1, 20)
                ),

                // Cenário 6
                Arguments.of(
                        "Fevereiro em ano não bissexto",
                        LocalDate.of(2026, 1, 15),
                        Cartao.builder()
                                .diaFechamento(10)
                                .diaVencimento(30)
                                .build(),
                        LocalDate.of(2026, 2, 28)
                ),

                // Cenário 7
                Arguments.of(
                        "Cartão nulo",
                        LocalDate.of(2026, 5, 20),
                        null,
                        LocalDate.of(2026, 5, 20)
                ),

                // Cenário 8
                Arguments.of(
                        "Fevereiro em ano bissexto",
                        LocalDate.of(2028, 1, 15),
                        Cartao.builder()
                                .diaFechamento(10)
                                .diaVencimento(31)
                                .build(),
                        LocalDate.of(2028, 2, 29)
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("cenariosCalculoFatura")
    @DisplayName("Deve calcular corretamente a data de cobrança")
    void deveCalcularDataCobranca(
            String descricao,
            LocalDate dataCompra,
            Cartao cartao,
            LocalDate dataEsperada
    ) throws Exception {

        LocalDate resultado =
                chamarCalcularDataCobranca(dataCompra, cartao);

        assertEquals(
                dataEsperada,
                resultado,
                descricao
        );
    }
}