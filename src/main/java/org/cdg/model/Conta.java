package org.cdg.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Conta {
    private Integer idConta;
    private String nome;
    private String tipoConta; // "CORRENTE" ou "CREDITO"
    private Double saldoInicial;
    private Integer diaFecho;     // Apenas para crédito
    private Integer diaVencimento; // Apenas para crédito
}