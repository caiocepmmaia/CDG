package org.cdg.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumoMensalDTO {
    private List<Transacao> transacoes;
    private BigDecimal totalReceitas;
    private BigDecimal totalDespesas;
    private BigDecimal totalReembolsos;
    private BigDecimal investimentos;
    private BigDecimal sobra;
}