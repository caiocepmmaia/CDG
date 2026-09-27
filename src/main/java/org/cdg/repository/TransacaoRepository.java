package org.cdg.repository;

import org.cdg.model.Transacao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class TransacaoRepository {

    public void salvar(Transacao transacao) {
        String sql = """
            INSERT INTO tb_transacao (
                descricao, valor, data_registo, data_cobranca, 
                parcela_atual, total_parcelas, status, tipo, 
                id_categoria, id_conta, id_titular, id_cartao, reembolsavel
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, transacao.getDescricao());
            pstmt.setBigDecimal(2, transacao.getValor()); // <--- Ajustado para BigDecimal
            pstmt.setDate(3, java.sql.Date.valueOf(transacao.getDataRegisto()));
            pstmt.setDate(4, java.sql.Date.valueOf(transacao.getDataCobranca()));
            pstmt.setInt(5, transacao.getParcelaAtual());
            pstmt.setInt(6, transacao.getTotalParcelas());
            pstmt.setString(7, transacao.getStatus());
            pstmt.setString(8, transacao.getTipo());

            pstmt.setInt(9, transacao.getCategoria().getIdCategoria());
            pstmt.setInt(10, transacao.getConta().getIdConta());
            pstmt.setInt(11, transacao.getTitular().getIdTitular());
            pstmt.setInt(12, transacao.getCartao().getIdCartao());
            pstmt.setInt(13, transacao.isReembolsavel() ? 1 : 0);

            pstmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Erro ao salvar transação: " + e.getMessage());
        }
    }

    public void excluir(int idTransacao) {
        String sql = "DELETE FROM tb_transacao WHERE id_transacao = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idTransacao);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erro ao excluir transação: " + e.getMessage());
        }
    }

    public void atualizar(Transacao t) {
        String sql = """
        UPDATE tb_transacao 
        SET descricao = ?, valor = ?, data_cobranca = ?, tipo = ?, id_cartao = ?, id_categoria = ?, reembolsavel = ?
        WHERE id_transacao = ?
        """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, t.getDescricao());
            pstmt.setBigDecimal(2, t.getValor()); // <--- Ajustado para BigDecimal
            pstmt.setDate(3, java.sql.Date.valueOf(t.getDataCobranca()));
            pstmt.setString(4, t.getTipo());
            pstmt.setInt(5, t.getCartao().getIdCartao());
            pstmt.setInt(6, t.getCategoria().getIdCategoria());
            pstmt.setInt(7, t.isReembolsavel() ? 1 : 0);
            pstmt.setInt(8, t.getIdTransacao());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erro ao atualizar transação: " + e.getMessage());
        }
    }
}