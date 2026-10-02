package org.cdg.service;

import org.cdg.model.*;
import org.cdg.repository.*;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class InvestimentoService {

    private final CartaoRepository cartaoRepo = new CartaoRepository();
    private final CategoriaRepository catRepo = new CategoriaRepository();
    private final MetaRepository metaRepo = new MetaRepository();
    private final TransacaoRepository transacaoRepo = new TransacaoRepository();

    public List<Meta> listarMetas() {
        return metaRepo.listarTodas();
    }

    public void realizarInvestimento(Meta metaEscolhida, BigDecimal valorSobra, LocalDate mesAtual) throws Exception {
        List<Cartao> cartoes = cartaoRepo.listarTodos();
        List<Categoria> categorias = catRepo.listarTodas();

        if (cartoes.isEmpty() || categorias.isEmpty()) {
            throw new IllegalStateException("Cadastre pelo menos 1 cartão e 1 categoria antes de investir.");
        }

        // Obtém uma única conexão para garantir a transação atómica
        try (Connection conn = DatabaseConnection.getConnection()) {
            // Desativa o auto-commit para controlarmos a transação manualmente
            conn.setAutoCommit(false);

            try {
                // 1. Atualiza o valor atual da meta
                BigDecimal atual = metaEscolhida.getValorAtual() != null ? metaEscolhida.getValorAtual() : BigDecimal.ZERO;
                metaEscolhida.setValorAtual(atual.add(valorSobra));

                String sqlMeta = "UPDATE tb_meta SET nome = ?, valor_alvo = ?, valor_atual = ?, data_limite = ? WHERE id_meta = ?";
                try (PreparedStatement pstmtMeta = conn.prepareStatement(sqlMeta)) {
                    pstmtMeta.setString(1, metaEscolhida.getDescricao());
                    pstmtMeta.setBigDecimal(2, metaEscolhida.getValorAlvo());
                    pstmtMeta.setBigDecimal(3, metaEscolhida.getValorAtual());
                    pstmtMeta.setDate(4, metaEscolhida.getDataLimite() != null ? java.sql.Date.valueOf(metaEscolhida.getDataLimite()) : null);
                    pstmtMeta.setInt(5, metaEscolhida.getIdMeta());
                    pstmtMeta.executeUpdate();
                }

                // 2. Cria e guarda a transação de aporte
                LocalDate ultimoDiaDoMes = mesAtual.withDayOfMonth(mesAtual.lengthOfMonth());

                String sqlTransacao = """
                    INSERT INTO tb_transacao 
                    (descricao, valor, data_registo, data_cobranca, parcela_atual, total_parcelas, status, tipo, id_categoria, id_conta, id_titular, id_cartao, reembolsavel) 
                    VAlUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """;

                try (PreparedStatement pstmtTrans = conn.prepareStatement(sqlTransacao)) {
                    pstmtTrans.setString(1, "Aporte: " + metaEscolhida.getDescricao());
                    pstmtTrans.setBigDecimal(2, valorSobra);
                    pstmtTrans.setDate(3, java.sql.Date.valueOf(LocalDate.now()));
                    pstmtTrans.setDate(4, java.sql.Date.valueOf(ultimoDiaDoMes));
                    pstmtTrans.setInt(5, 1);
                    pstmtTrans.setInt(6, 1);
                    pstmtTrans.setString(7, "PAGO");
                    pstmtTrans.setString(8, "INVESTIMENTO");
                    pstmtTrans.setInt(9, categorias.get(0).getIdCategoria());
                    pstmtTrans.setInt(10, 1); // id_conta padrão
                    pstmtTrans.setInt(11, 1); // id_titular padrão
                    pstmtTrans.setInt(12, cartoes.get(0).getIdCartao());
                    pstmtTrans.setInt(13, 0); // reembolsavel = falso
                    pstmtTrans.executeUpdate();
                }

                // Se tudo correu bem, efetiva as alterações na base de dados
                conn.commit();

            } catch (Exception e) {
                // Se ocorrer qualquer erro, desfaz todas as alterações feitas nesta transação
                conn.rollback();
                throw new Exception("Erro ao processar investimento, operação cancelada: " + e.getMessage(), e);
            }

        } catch (SQLException e) {
            throw new Exception("Erro de ligação à base de dados: " + e.getMessage(), e);
        }
    }
}