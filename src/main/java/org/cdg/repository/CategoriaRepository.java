package org.cdg.repository;

import org.cdg.model.Categoria;
import org.cdg.model.GastoAgrupadoDTO;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CategoriaRepository {

    public void salvar(Categoria categoria) throws SQLException {
        String sql = "INSERT INTO tb_categoria (nome) VALUES (?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, categoria.getNome());
            pstmt.executeUpdate();
        }
    }

    public List<Categoria> listarTodas() {
        List<Categoria> lista = new ArrayList<>();
        String sql = "SELECT * FROM tb_categoria";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                Categoria c = Categoria.builder()
                        .idCategoria(rs.getInt("id_categoria"))
                        .nome(rs.getString("nome"))
                        .build();
                lista.add(c);
            }
        } catch (SQLException e) {
            System.err.println("Erro ao listar categorias: " + e.getMessage());
        }
        return lista;
    }

    public void atualizar(Categoria categoria) throws SQLException {
        String sql = "UPDATE tb_categoria SET nome = ? WHERE id_categoria = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, categoria.getNome());
            pstmt.setInt(2, categoria.getIdCategoria());
            pstmt.executeUpdate();
        }
    }

    public void excluir(int idCategoria) throws SQLException {
        String sql = "DELETE FROM tb_categoria WHERE id_categoria = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idCategoria);
            pstmt.executeUpdate();
        }
    }
    public List<GastoAgrupadoDTO> obterGastosPorMes(LocalDate mes) throws SQLException {
        LocalDate inicioMes = mes.withDayOfMonth(1);
        LocalDate fimMes = mes.withDayOfMonth(mes.lengthOfMonth());

        String sql = """
            SELECT c.nome, SUM(t.valor) as total
            FROM tb_transacao t
            INNER JOIN tb_categoria c ON t.id_categoria = c.id_categoria
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