package org.cdg.repository;

import org.cdg.model.Meta;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MetaRepository {

    public void salvar(Meta meta) throws SQLException {
        String sql = "INSERT INTO tb_meta (nome, valor_alvo, valor_atual, data_limite) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, meta.getDescricao());
            pstmt.setBigDecimal(2, meta.getValorAlvo());
            pstmt.setBigDecimal(3, meta.getValorAtual() != null ? meta.getValorAtual() : java.math.BigDecimal.ZERO);
            pstmt.setDate(4, meta.getDataLimite() != null ? Date.valueOf(meta.getDataLimite()) : null);
            pstmt.executeUpdate();
        }
    }

    public List<Meta> listarTodas() {
        List<Meta> lista = new ArrayList<>();
        String sql = "SELECT * FROM tb_meta";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                Date dataLim = rs.getDate("data_limite");
                Meta m = Meta.builder()
                        .idMeta(rs.getInt("id_meta"))
                        .descricao(rs.getString("nome"))
                        .valorAlvo(rs.getBigDecimal("valor_alvo"))
                        .valorAtual(rs.getBigDecimal("valor_atual"))
                        .dataLimite(dataLim != null ? dataLim.toLocalDate() : null)
                        .build();
                lista.add(m);
            }
        } catch (SQLException e) {
            System.err.println("Erro ao carregar lista de metas: " + e.getMessage());
        }
        return lista;
    }

    public void atualizar(Meta meta) throws SQLException {
        String sql = "UPDATE tb_meta SET nome = ?, valor_alvo = ?, valor_atual = ?, data_limite = ? WHERE id_meta = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, meta.getDescricao());
            pstmt.setBigDecimal(2, meta.getValorAlvo());
            pstmt.setBigDecimal(3, meta.getValorAtual() != null ? meta.getValorAtual() : java.math.BigDecimal.ZERO);
            pstmt.setDate(4, meta.getDataLimite() != null ? Date.valueOf(meta.getDataLimite()) : null);
            pstmt.setInt(5, meta.getIdMeta());
            pstmt.executeUpdate();
        }
    }

    public void excluir(int idMeta) throws SQLException {
        String sql = "DELETE FROM tb_meta WHERE id_meta = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idMeta);
            pstmt.executeUpdate();
        }
    }
}