package org.cdg.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Meta {
    private Integer idMeta;
    private String descricao;
    private BigDecimal valorAlvo;
    private BigDecimal valorAtual;
    private LocalDate dataLimite;
}