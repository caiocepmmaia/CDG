package org.cdg.repository;

import org.cdg.model.Meta;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MetaRepository {

    public void salvar(Meta meta) {
        String sql = """
            INSERT INTO tb_meta (nome, valor_alvo, valor_atual, data_limite)
            VALUES (?, ?, ?, ?)
            """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, meta.getDescricao());
            pstmt.setBigDecimal(2, meta.getValorAlvo());
            pstmt.setBigDecimal(3, meta.getValorAtual() != null ? meta.getValorAtual() : java.math.BigDecimal.ZERO);
            pstmt.setDate(4, java.sql.Date.valueOf(meta.getDataLimite()));
            pstmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Erro ao salvar meta: " + e.getMessage());
        }
    }

    public void atualizar(Meta meta) {
        String sql = """
            UPDATE tb_meta
            SET nome = ?, valor_alvo = ?, valor_atual = ?, data_limite = ?
            WHERE id_meta = ?
            """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, meta.getDescricao());
            pstmt.setBigDecimal(2, meta.getValorAlvo());
            pstmt.setBigDecimal(3, meta.getValorAtual());
            pstmt.setDate(4, java.sql.Date.valueOf(meta.getDataLimite()));
            pstmt.setInt(5, meta.getIdMeta());
            pstmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Erro ao atualizar meta: " + e.getMessage());
        }
    }

    public void excluir(int idMeta) {
        String sql = "DELETE FROM tb_meta WHERE id_meta = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idMeta);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erro ao excluir meta: " + e.getMessage());
        }
    }

    public List<Meta> listarTodas() {
        List<Meta> metas = new ArrayList<>();
        String sql = "SELECT * FROM tb_meta ORDER BY data_limite ASC";

        // Connection, PreparedStatement e ResultSet todos no try-with-resources para liberar o trava do SQLite imediatamente
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                Meta m = Meta.builder()
                        .idMeta(rs.getInt("id_meta"))
                        .descricao(rs.getString("nome"))
                        .valorAlvo(rs.getBigDecimal("valor_alvo"))
                        .valorAtual(rs.getBigDecimal("valor_atual"))
                        .dataLimite(rs.getDate("data_limite").toLocalDate())
                        .build();
                metas.add(m);
            }
        } catch (SQLException e) {
            System.err.println("Erro ao listar metas: " + e.getMessage());
        }
        return metas;
    }
}