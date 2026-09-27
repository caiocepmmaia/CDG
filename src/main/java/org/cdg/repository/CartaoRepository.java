package org.cdg.repository;

import org.cdg.model.Cartao;
import org.cdg.model.GastoAgrupadoDTO;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CartaoRepository {

    public void salvar(Cartao cartao) throws SQLException {
        String sql = "INSERT INTO tb_cartao (nome, dia_vencimento) VALUES (?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, cartao.getNome());
            pstmt.setInt(2, cartao.getDiaVencimento());
            pstmt.executeUpdate();
        }
    }

    public List<Cartao> listarTodos() {
        List<Cartao> lista = new ArrayList<>();
        String sql = "SELECT * FROM tb_cartao";
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

    public void atualizar(Cartao cartao) throws SQLException {
        String sql = "UPDATE tb_cartao SET nome = ?, dia_vencimento = ? WHERE id_cartao = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, cartao.getNome());
            pstmt.setInt(2, cartao.getDiaVencimento());
            pstmt.setInt(3, cartao.getIdCartao());
            pstmt.executeUpdate();
        }
    }

    public void excluir(int idCartao) throws SQLException {
        String sql = "DELETE FROM tb_cartao WHERE id_cartao = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idCartao);
            pstmt.executeUpdate();
        }
    }
    public List<GastoAgrupadoDTO> obterFaturasPorMes(LocalDate mes) throws SQLException {
        LocalDate inicioMes = mes.withDayOfMonth(1);
        LocalDate fimMes = mes.withDayOfMonth(mes.lengthOfMonth());

        String sql = """
            SELECT c.nome, SUM(t.valor) as total
            FROM tb_transacao t
            INNER JOIN tb_cartao c ON t.id_cartao = c.id_cartao
            WHERE t.data_cobranca BETWEEN ? AND ?
            AND t.tipo = 'DESPESA'
            GROUP BY c.nome
            HAVING total > 0
            ORDER BY total DESC
            """;

        List<GastoAgrupadoDTO> lista = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setDate(1, Date.valueOf(inicioMes));
            pstmt.setDate(2, Date.valueOf(fimMes));

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(new GastoAgrupadoDTO(rs.getString("nome"), rs.getBigDecimal("total")));
                }
            }
        }
        return lista;
    }
}