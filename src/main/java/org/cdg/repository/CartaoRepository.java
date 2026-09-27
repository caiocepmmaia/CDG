package org.cdg.repository;

import org.cdg.model.Cartao;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CartaoRepository {

    public void salvar(Cartao cartao) {
        String sql = "INSERT INTO tb_cartao (nome, dia_vencimento) VALUES (?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, cartao.getNome());
            pstmt.setInt(2, cartao.getDiaVencimento());
            pstmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Erro ao salvar cartão: " + e.getMessage());
        }
    }

    public void atualizar(Cartao c) {
        String sql = "UPDATE tb_cartao SET nome = ?, dia_vencimento = ? WHERE id_cartao = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, c.getNome());
            pstmt.setInt(2, c.getDiaVencimento());
            pstmt.setInt(3, c.getIdCartao());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erro ao atualizar cartão: " + e.getMessage());
        }
    }

    public void excluir(int idCartao) {
        String sql = "DELETE FROM tb_cartao WHERE id_cartao = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idCartao);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erro ao excluir cartão (verifique se existem transações associadas): " + e.getMessage());
        }
    }

    public List<Cartao> listarTodos() {
        List<Cartao> lista = new ArrayList<>();
        String sql = "SELECT * FROM tb_cartao ORDER BY id_cartao ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                Cartao c = Cartao.builder()
                        .idCartao(rs.getInt("id_cartao"))
                        .nome(rs.getString("nome"))
                        .diaVencimento(rs.getInt("dia_vencimento"))
                        .build();
                lista.add(c);
            }

        } catch (SQLException e) {
            System.err.println("Erro ao listar cartões: " + e.getMessage());
        }
        return lista;
    }
}