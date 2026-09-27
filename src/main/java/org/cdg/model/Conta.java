package org.cdg.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Conta {
    private Integer idConta;
    private String nome;
    private BigDecimal saldoInicial;
    private BigDecimal saldoAtual;
}