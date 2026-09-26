package org.cdg.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Meta {
    private Integer idMeta;
    private String nome;
    private Double valorAlvo;
    private Double valorAtual;
    private LocalDate dataLimite;

    // Método de cálculo direto (regra de negócio)
    public Double getValorFaltante() {
        return (valorAlvo != null && valorAtual != null) ? (valorAlvo - valorAtual) : 0.0;
    }
}