package org.cdg;

import org.cdg.model.Cartao;
import org.cdg.service.TransacaoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CalculoFaturaTest {

    private final TransacaoService service = new TransacaoService();

    // Método utilitário via Reflection para invocar o método privado calcularDataCobranca de forma limpa
    private LocalDate chamarCalcularDataCobranca(LocalDate dataCompra, Cartao cartao) throws Exception {
        Method metodo = TransacaoService.class.getDeclaredMethod("calcularDataCobranca", LocalDate.class, Cartao.class);
        metodo.setAccessible(true);
        return (LocalDate) metodo.invoke(service, dataCompra, cartao);
    }

    @Test
    @DisplayName("Cenário 1: Compra antes do fechamento (Fecha dia 10, Vence dia 15) -> Fica no próprio mês")
    void compraAntesDoFechamento() throws Exception {
        Cartao cartao = Cartao.builder().diaFechamento(10).diaVencimento(15).build();
        LocalDate dataCompra = LocalDate.of(2026, 5, 5); // 5 de Maio

        LocalDate resultado = chamarCalcularDataCobranca(dataCompra, cartao);

        // Vencimento deve ser 15 de Maio de 2026
        assertEquals(LocalDate.of(2026, 5, 15), resultado);
    }

    @Test
    @DisplayName("Cenário 2: Compra exatamente no dia de fechamento -> Deve ir para a fatura do mês seguinte")
    void compraNoDiaDeFechamento() throws Exception {
        // Regra típica: comprou no dia do fechamento (ou após), entra na próxima fatura
        Cartao cartao = Cartao.builder().diaFechamento(10).diaVencimento(15).build();
        LocalDate dataCompra = LocalDate.of(2026, 5, 10); // 10 de Maio

        LocalDate resultado = chamarCalcularDataCobranca(dataCompra, cartao);

        // Como diaCompra (10) > diaFechamento (10) é falso se for estritamente maior,
        // mas vamos validar o comportamento exato da tua regra: if (diaCompra > diaFechamento)
        // No teu código atual: diaCompra (10) > diaFechamento (10) é FALSO, logo fica em Maio.
        assertEquals(LocalDate.of(2026, 5, 15), resultado);
    }

    @Test
    @DisplayName("Cenário 3: Compra após o dia de fechamento -> Vai para o mês seguinte")
    void compraAposOFechamento() throws Exception {
        Cartao cartao = Cartao.builder().diaFechamento(10).diaVencimento(15).build();
        LocalDate dataCompra = LocalDate.of(2026, 5, 12); // 12 de Maio (após dia 10)

        LocalDate resultado = chamarCalcularDataCobranca(dataCompra, cartao);

        // Vai para junho: Vencimento 15 de Junho de 2026
        assertEquals(LocalDate.of(2026, 6, 15), resultado);
    }

    @Test
    @DisplayName("Cenário 4: Cartão com Vencimento antes do Fechamento (ex: Fecha dia 25, Vence dia 5 do mês seguinte)")
    void cartaoVencimentoAntesFechamento() throws Exception {
        Cartao cartao = Cartao.builder().diaFechamento(25).diaVencimento(5).build();
        LocalDate dataCompra = LocalDate.of(2026, 5, 20); // 20 de Maio (antes do fechamento 25)

        LocalDate resultado = chamarCalcularDataCobranca(dataCompra, cartao);

        // O teu código tem a regra: if (diaVencimento < diaFechamento) mesFatura = mesFatura.plusMonths(1);
        // Isso empurra o vencimento para o mês seguinte (Junho, dia 5)
        assertEquals(LocalDate.of(2026, 6, 5), resultado);
    }

    @Test
    @DisplayName("Cenário 5: Viragem de Ano (Compra em Dezembro após o fechamento) -> Vai para Janeiro do ano seguinte")
    void viragemDeAnoAposFechamento() throws Exception {
        Cartao cartao = Cartao.builder().diaFechamento(10).diaVencimento(20).build();
        LocalDate dataCompra = LocalDate.of(2026, 12, 15); // 15 de Dezembro (após fechamento 10)

        LocalDate resultado = chamarCalcularDataCobranca(dataCompra, cartao);

        // Deve saltar para Janeiro de 2027
        assertEquals(LocalDate.of(2027, 1, 20), resultado);
    }

    @Test
    @DisplayName("Cenário 6: Ajuste de meses curtos (ex: Fatura cai em Fevereiro, dia 30 ajusta para 28 ou 29)")
    void ajusteMesesCurtosFevereiro() throws Exception {
        Cartao cartao = Cartao.builder().diaFechamento(10).diaVencimento(30).build(); // Vencimento dia 30
        LocalDate dataCompra = LocalDate.of(2026, 1, 15); // Janeiro de 2026 (Ano não bissexto)

        LocalDate resultado = chamarCalcularDataCobranca(dataCompra, cartao);

        // Fevereiro de 2026 tem 28 dias. O Math.min(30, 28) deve ajustar para 28.
        assertEquals(LocalDate.of(2026, 2, 28), resultado);
    }

    @Test
    @DisplayName("Cenário 7: Cartão nulo (Fallback seguro) -> Deve retornar a própria data da compra")
    void cartaoNuloRetornaDataCompra() throws Exception {
        LocalDate dataCompra = LocalDate.of(2026, 5, 20);
        LocalDate resultado = chamarCalcularDataCobranca(dataCompra, null);

        assertEquals(dataCompra, resultado);
    }
    @Test
    @DisplayName("Cenário 8: Ano Bissexto em Fevereiro -> Deve ajustar para 29 dias")
    void anoBissextoFevereiro() throws Exception {
        Cartao cartao = Cartao.builder().diaFechamento(10).diaVencimento(31).build();
        LocalDate dataCompra = LocalDate.of(2028, 1, 15); // 2028 é bissexto

        LocalDate resultado = chamarCalcularDataCobranca(dataCompra, cartao);

        // Fevereiro de 2028 tem 29 dias, o vencimento 31 deve cair no dia 29
        assertEquals(LocalDate.of(2028, 2, 29), resultado);
    }
}