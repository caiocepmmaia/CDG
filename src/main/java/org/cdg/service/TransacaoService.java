package org.cdg.service;

import org.cdg.model.*;
import org.cdg.repository.TransacaoRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.LocalDate;

public class TransacaoService {

    private final TransacaoRepository repo = new TransacaoRepository();

    public ResumoMensalDTO obterResumoMensal(LocalDate mesAtual) throws SQLException {
        if (mesAtual == null) {
            throw new IllegalArgumentException("O mês de referência não pode ser nulo.");
        }
        return repo.obterResumoMensal(mesAtual);
    }

    public void processarESalvar(Transacao transacaoExistente, String tipo, Cartao cartao, Categoria categoria,
                                 String descricao, BigDecimal valorTotal, LocalDate dataCompra,
                                 int totalParcelas, boolean reembolsavel) throws Exception {

        // 1. Validações de Regra de Negócio
        if (cartao == null) throw new IllegalArgumentException("Selecione um cartão.");
        if (categoria == null) throw new IllegalArgumentException("Selecione uma categoria.");
        if (descricao == null || descricao.trim().isEmpty()) throw new IllegalArgumentException("A descrição é obrigatória.");
        if (valorTotal == null || valorTotal.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("O valor deve ser maior que zero.");
        if (totalParcelas < 1) totalParcelas = 1;

        if (transacaoExistente != null) {
            // --- MODO EDIÇÃO ---
            LocalDate dataCobrancaEditada = calcularDataCobranca(dataCompra, cartao);

            transacaoExistente.setDescricao(descricao.trim());
            transacaoExistente.setValor(valorTotal);
            transacaoExistente.setDataRegisto(dataCompra);
            transacaoExistente.setDataCobranca(dataCobrancaEditada);
            transacaoExistente.setTipo(tipo);
            transacaoExistente.setCartao(cartao);
            transacaoExistente.setCategoria(categoria);
            transacaoExistente.setReembolsavel(reembolsavel);

            repo.atualizar(transacaoExistente);
        } else {
            // --- MODO CRIAÇÃO (DIVISÃO DE PARCELAS) ---
            BigDecimal valorParcelaBase = valorTotal.divide(BigDecimal.valueOf(totalParcelas), 2, RoundingMode.DOWN);
            BigDecimal somaBase = valorParcelaBase.multiply(BigDecimal.valueOf(totalParcelas));
            BigDecimal diferencaCentavos = valorTotal.subtract(somaBase);

            Conta contaBase = Conta.builder().idConta(1).build();
            Titular titularBase = Titular.builder().idTitular(1).build();

            for (int i = 1; i <= totalParcelas; i++) {
                String descricaoFormatada = totalParcelas > 1 ? descricao.trim() + " (" + i + "/" + totalParcelas + ")" : descricao.trim();

                LocalDate dataCompraParcela = dataCompra.plusMonths(i - 1);
                LocalDate dataCobrancaFinal = calcularDataCobranca(dataCompraParcela, cartao);

                BigDecimal valorEstaParcela = (i == 1) ? valorParcelaBase.add(diferencaCentavos) : valorParcelaBase;

                Transacao parcela = Transacao.builder()
                        .descricao(descricaoFormatada)
                        .valor(valorEstaParcela)
                        .dataRegisto(dataCompraParcela)
                        .dataCobranca(dataCobrancaFinal)
                        .parcelaAtual(i)
                        .totalParcelas(totalParcelas)
                        .status("PENDENTE")
                        .tipo(tipo)
                        .cartao(cartao)
                        .categoria(categoria)
                        .reembolsavel(reembolsavel)
                        .conta(contaBase)
                        .titular(titularBase)
                        .build();

                repo.salvar(parcela);
            }
        }
    }

    // Regra de negócio isolada: Cálculo do vencimento da fatura
    private LocalDate calcularDataCobranca(LocalDate dataCompra, Cartao cartao) {
        LocalDate mesFatura = dataCompra;

        if (cartao != null && cartao.getDiaFechamento() > 0) {
            int diaCompra = dataCompra.getDayOfMonth();
            int diaFechamento = cartao.getDiaFechamento();
            int diaVencimento = cartao.getDiaVencimento();

            if (diaCompra > diaFechamento) {
                mesFatura = mesFatura.plusMonths(1);
            }
            if (diaVencimento < diaFechamento) {
                mesFatura = mesFatura.plusMonths(1);
            }

            int diaAjustado = Math.min(diaVencimento, mesFatura.lengthOfMonth());
            return mesFatura.withDayOfMonth(diaAjustado);
        }
        return mesFatura;
    }

    public void excluir(int idTransacao) throws Exception {
        repo.excluir(idTransacao);
    }
}