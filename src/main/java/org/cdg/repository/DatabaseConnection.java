package org.cdg.repository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    // O caminho para o arquivo .db que você criou na pasta resources
    private static final String URL = "jdbc:sqlite:src/main/resources/database/financas.db";

    public static Connection getConnection() {
        try {
            return DriverManager.getConnection(URL);
        } catch (SQLException e) {
            System.err.println("Erro ao conectar com o banco de dados SQLite: " + e.getMessage());
            return null;
        }
    }
}