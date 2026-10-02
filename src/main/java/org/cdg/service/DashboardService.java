package org.cdg.service;

import org.cdg.model.*;
import org.cdg.repository.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class DashboardService {

    private final TransacaoService transacaoService = new TransacaoService();
    private final MetaRepository metaRepo = new MetaRepository();
    private final CartaoRepository cartaoRepo = new CartaoRepository();
    private final CategoriaRepository categoriaRepo = new CategoriaRepository();

    public DashboardViewModel carregarDadosDashboard(LocalDate mesAtual) throws Exception {
        // 1. Obtém o resumo mensal unificado
        ResumoMensalDTO resumo = transacaoService.obterResumoMensal(mesAtual);

        // 2. Busca as listas auxiliares através dos repositórios ou serviços
        List<Meta> metas = metaRepo.listarTodas();
        List<GastoAgrupadoDTO> faturas = cartaoRepo.obterFaturasPorMes(mesAtual);
        List<GastoAgrupadoDTO> gastosCat = categoriaRepo.obterGastosPorMes(mesAtual);

        BigDecimal sobra = resumo.getSobra();

        // 3. Retorna o ViewModel consolidado pronto para a UI consumir
        return DashboardViewModel.builder()
                .sobra(sobra)
                .totalDespesas(resumo.getTotalDespesas())
                .totalReembolsos(resumo.getTotalReembolsos())
                .temSobraPositiva(sobra.compareTo(BigDecimal.ZERO) > 0)
                .transacoes(resumo.getTransacoes())
                .metas(metas)
                .faturasCartoes(faturas)
                .gastosCategorias(gastosCat)
                .build();
    }
}