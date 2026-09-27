package org.cdg;

import org.cdg.repository.DatabaseConnection;
import java.sql.Connection;
import java.sql.Statement;

public class AtualizarBanco {
    public static void main(String[] args) {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            // Desativa temporariamente as chaves estrangeiras para permitir a limpeza em qualquer ordem
            stmt.execute("PRAGMA foreign_keys = OFF;");

            // 1. Apaga todos os registos de todas as tabelas
            stmt.execute("DELETE FROM tb_transacao;");
            stmt.execute("DELETE FROM tb_meta;");
            stmt.execute("DELETE FROM tb_cartao;");
            stmt.execute("DELETE FROM tb_categoria;");

            // 2. Reseta os contadores de ID (auto-incremento) do SQLite
            stmt.execute("DELETE FROM sqlite_sequence WHERE name IN ('tb_transacao', 'tb_meta', 'tb_cartao', 'tb_categoria');");

            // Reativa as chaves estrangeiras
            stmt.execute("PRAGMA foreign_keys = ON;");

            System.out.println("SUCESSO: O banco de dados foi totalmente limpo (transações, metas, cartões e categorias removidos)!");

        } catch (Exception e) {
            System.out.println("Erro ao limpar o banco de dados: " + e.getMessage());
        }
    }
}