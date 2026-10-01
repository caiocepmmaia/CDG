package org.cdg.service;

import org.cdg.model.Meta;
import org.cdg.repository.MetaRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class MetaService {

    private final MetaRepository repo = new MetaRepository();

    public List<Meta> listarTodas() {
        return repo.listarTodas();
    }

    public void salvar(Meta meta, String descricao, BigDecimal alvo, LocalDate dataLimite) throws Exception {
        if (descricao == null || descricao.trim().isEmpty()) {
            throw new IllegalArgumentException("A descrição da meta é obrigatória.");
        }
        if (alvo == null || alvo.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("O valor alvo deve ser maior que zero.");
        }

        if (meta != null) {
            meta.setDescricao(descricao.trim());
            meta.setValorAlvo(alvo);
            meta.setDataLimite(dataLimite);
            repo.atualizar(meta);
        } else {
            Meta nova = Meta.builder()
                    .descricao(descricao.trim())
                    .valorAlvo(alvo)
                    .valorAtual(BigDecimal.ZERO)
                    .dataLimite(dataLimite)
                    .build();
            repo.salvar(nova);
        }
    }

    public void excluir(int idMeta) throws Exception {
        repo.excluir(idMeta);
    }
}