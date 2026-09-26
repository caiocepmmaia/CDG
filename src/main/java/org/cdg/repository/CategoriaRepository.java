package org.cdg.repository;

import org.cdg.model.Categoria;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CategoriaRepository {

    public void salvar(Categoria categoria) {
        String sql = "INSERT INTO tb_categoria (nome) VALUES (?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, categoria.getNome());
            pstmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Erro ao salvar categoria: " + e.getMessage());
        }
    }

    public List<Categoria> listarTodas() {
        List<Categoria> lista = new ArrayList<>();
        String sql = "SELECT * FROM tb_categoria ORDER BY id_categoria ASC";

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
}