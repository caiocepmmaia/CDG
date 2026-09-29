// FICHEIRO: src/main/java/org/cdg/service/InvestimentoService.java
package org.cdg.service;

import org.cdg.model.*;
import org.cdg.repository.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class InvestimentoService {

    private final CartaoRepository cartaoRepo = new CartaoRepository();
    private final CategoriaRepository catRepo = new CategoriaRepository();
    private final MetaRepository metaRepo = new MetaRepository();
    private final TransacaoRepository transacaoRepo = new TransacaoRepository();

    public List<Meta> listarMetas() {
        return metaRepo.listarTodas();
    }

    public void realizarInvestimento(Meta metaEscolhida, BigDecimal valorSobra, LocalDate mesAtual) throws Exception {
        List<Cartao> cartoes = cartaoRepo.listarTodos();
        List<Categoria> categorias = catRepo.listarTodas();

        if (cartoes.isEmpty() || categorias.isEmpty()) {
            throw new IllegalStateException("Cadastre pelo menos 1 cartão e 1 categoria antes de investir.");
        }

        BigDecimal atual = metaEscolhida.getValorAtual() != null ? metaEscolhida.getValorAtual() : BigDecimal.ZERO;
        metaEscolhida.setValorAtual(atual.add(valorSobra));
        metaRepo.atualizar(metaEscolhida);

        LocalDate ultimoDiaDoMes = mesAtual.withDayOfMonth(mesAtual.lengthOfMonth());
        Conta contaBase = Conta.builder().idConta(1).build();
        Titular titularBase = Titular.builder().idTitular(1).build();

        Transacao aporte = Transacao.builder()
                .descricao("Aporte: " + metaEscolhida.getDescricao())
                .valor(valorSobra)
                .dataRegisto(LocalDate.now())
                .dataCobranca(ultimoDiaDoMes)
                .parcelaAtual(1)
                .totalParcelas(1)
                .status("PAGO")
                .tipo("INVESTIMENTO")
                .cartao(cartoes.get(0))
                .categoria(categorias.get(0))
                .conta(contaBase)
                .titular(titularBase)
                .build();

        transacaoRepo.salvar(aporte);
    }
}