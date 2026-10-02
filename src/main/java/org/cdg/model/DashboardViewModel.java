package org.cdg.model;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class DashboardViewModel {
    private BigDecimal sobra;
    private BigDecimal totalDespesas;
    private BigDecimal totalReembolsos;
    private boolean temSobraPositiva;
    private List<Transacao> transacoes;
    private List<Meta> metas;
    private List<GastoAgrupadoDTO> faturasCartoes;
    private List<GastoAgrupadoDTO> gastosCategorias;
}