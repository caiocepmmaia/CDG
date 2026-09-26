package org.cdg;

import org.cdg.model.Categoria;
import org.cdg.model.Conta;
import org.cdg.model.Titular;
import org.cdg.model.Transacao;
import org.cdg.repository.TransacaoRepository;

import java.time.LocalDate;

public class Main {
    public static void main(String[] args) {
        System.out.println("A iniciar a ligação ao sistema financeiro...");

        // 1. Mapear os dados de suporte (sem o .tipo() na categoria)
        Categoria categoriaBase = Categoria.builder().idCategoria(1).nome("Alimentação").build();
        Conta contaBase = Conta.builder().idConta(1).build();
        Titular titularBase = Titular.builder().idTitular(1).build();

        // 2. Construir a transação de teste
        Transacao novaCompra = Transacao.builder()
                .descricao("Supermercado - Teste de Integração")
                .valor(150.75)
                .dataRegisto(LocalDate.now())
                .dataCobranca(LocalDate.now().plusDays(5))
                .parcelaAtual(1)
                .totalParcelas(1)
                .status("PENDENTE")
                .tipo("DESPESA") // O tipo agora pertence à transação!
                .categoria(categoriaBase)
                .conta(contaBase)
                .titular(titularBase)
                .build();

        // 3. Enviar para a base de dados
        TransacaoRepository repository = new TransacaoRepository();
        repository.salvar(novaCompra);

        System.out.println("Teste concluído com sucesso!");
    }
}