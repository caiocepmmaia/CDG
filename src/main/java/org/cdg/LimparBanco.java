package org.cdg;

import org.cdg.repository.DatabaseConnection;
import java.sql.Connection;
import java.sql.Statement;

public class LimparBanco {
    public static void main(String[] args) {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            // 1. Apaga todos os registos da tabela de transações
            stmt.execute("DELETE FROM tb_transacao");

            // 2. Reseta o contador de IDs para que a próxima compra volte a ser a número 1
            stmt.execute("DELETE FROM sqlite_sequence WHERE name='tb_transacao'");

            System.out.println("SUCESSO: A base de dados foi limpa e está pronta para o mundo real!");

        } catch (Exception e) {
            System.out.println("Erro ao limpar banco: " + e.getMessage());
        }
    }
}