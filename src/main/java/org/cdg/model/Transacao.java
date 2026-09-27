package org.cdg.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class Transacao {
    private int idTransacao;
    private String descricao;
    private BigDecimal valor;
    private LocalDate dataRegisto;
    private LocalDate dataCobranca;
    private int parcelaAtual;
    private int totalParcelas;
    private String status;
    private String tipo; // NOVO CAMPO: RECEITA, DESPESA ou INVESTIMENTO

    private Categoria categoria;
    private Conta conta;
    private Titular titular;

    private Cartao cartao;
    private boolean reembolsavel;
}