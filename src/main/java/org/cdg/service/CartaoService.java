package org.cdg.service;

import org.cdg.model.Cartao;
import org.cdg.repository.CartaoRepository;

import java.util.List;

public class CartaoService {

    private final CartaoRepository repo = new CartaoRepository();

    public List<Cartao> listarTodos() {
        return repo.listarTodos();
    }

    public void salvar(Cartao cartao, String nome, String diaVencTexto, String diaFechTexto) throws Exception {
        // 1. Regra de Negócio: Campos obrigatórios
        if (nome == null || nome.trim().isEmpty() || diaVencTexto == null || diaVencTexto.trim().isEmpty() || diaFechTexto == null || diaFechTexto.trim().isEmpty()) {
            throw new IllegalArgumentException("Preencha todos os campos do cartão.");
        }

        int diaVenc;
        int diaFech;

        // 2. Regra de Negócio: Validação de formato numérico
        try {
            diaVenc = Integer.parseInt(diaVencTexto.trim());
            diaFech = Integer.parseInt(diaFechTexto.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Os dias de vencimento e fechamento devem ser números válidos.");
        }

        // 3. Regra de Negócio: Validação do intervalo do mês (1 a 31)
        if (diaVenc < 1 || diaVenc > 31 || diaFech < 1 || diaFech > 31) {
            throw new IllegalArgumentException("Os dias do mês devem estar entre 1 e 31.");
        }

        // 4. Persistência
        if (cartao != null) {
            cartao.setNome(nome.trim());
            cartao.setDiaVencimento(diaVenc);
            cartao.setDiaFechamento(diaFech);
            repo.atualizar(cartao);
        } else {
            Cartao novoCartao = Cartao.builder()
                    .nome(nome.trim())
                    .diaVencimento(diaVenc)
                    .diaFechamento(diaFech)
                    .build();
            repo.salvar(novoCartao);
        }
    }

    public void excluir(int idCartao) throws Exception {
        repo.excluir(idCartao);
    }
}