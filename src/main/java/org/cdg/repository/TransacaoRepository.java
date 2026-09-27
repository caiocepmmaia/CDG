package org.cdg.repository;

import org.cdg.model.*;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class TransacaoRepository {

    public void salvar(Transacao t) throws SQLException {
        String sql = """
            INSERT INTO tb_transacao 
            (descricao, valor, data_registo, data_cobranca, parcela_atual, total_parcelas, status, tipo, id_categoria, id_conta, id_titular, id_cartao, reembolsavel) 
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, t.getDescricao());
            pstmt.setBigDecimal(2, t.getValor());
            pstmt.setDate(3, Date.valueOf(t.getDataRegisto()));
            pstmt.setDate(4, Date.valueOf(t.getDataCobranca()));
            pstmt.setInt(5, t.getParcelaAtual());
            pstmt.setInt(6, t.getTotalParcelas());
            pstmt.setString(7, t.getStatus());
            pstmt.setString(8, t.getTipo());
            pstmt.setInt(9, t.getCategoria().getIdCategoria());
            pstmt.setInt(10, t.getConta() != null ? t.getConta().getIdConta() : 1);
            pstmt.setInt(11, t.getTitular() != null ? t.getTitular().getIdTitular() : 1);
            pstmt.setInt(12, t.getCartao().getIdCartao());
            pstmt.setInt(13, t.isReembolsavel() ? 1 : 0);

            pstmt.executeUpdate();
        }
    }

    public ResumoMensalDTO obterResumoMensal(LocalDate mes) throws SQLException {
        LocalDate inicioMes = mes.withDayOfMonth(1);
        LocalDate fimMes = mes.withDayOfMonth(mes.lengthOfMonth());

        String sql = """
            SELECT t.*, cat.nome AS categoria_nome, car.nome AS cartao_nome, car.dia_vencimento 
            FROM tb_transacao t 
            LEFT JOIN tb_categoria cat ON t.id_categoria = cat.id_categoria 
            LEFT JOIN tb_cartao car ON t.id_cartao = car.id_cartao 
            WHERE t.data_cobranca BETWEEN ? AND ?
            ORDER BY t.data_cobranca ASC
            """;

        List<Transacao> lista = new ArrayList<>();
        BigDecimal totalReceitas = BigDecimal.ZERO;
        BigDecimal totalDespesas = BigDecimal.ZERO;
        BigDecimal totalReembolsos = BigDecimal.ZERO;
        BigDecimal investimentos = BigDecimal.ZERO;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setDate(1, Date.valueOf(inicioMes));
            pstmt.setDate(2, Date.valueOf(fimMes));

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    BigDecimal valor = rs.getBigDecimal("valor");
                    if (valor == null) valor = BigDecimal.ZERO;

                    String tipo = rs.getString("tipo");
                    boolean reembolsavel = rs.getInt("reembolsavel") == 1;
                    if (tipo == null) tipo = "DESPESA";

                    if ("RECEITA".equalsIgnoreCase(tipo)) {
                        totalReceitas = totalReceitas.add(valor);
                    } else if ("INVESTIMENTO".equalsIgnoreCase(tipo)) {
                        investimentos = investimentos.add(valor);
                    } else {
                        if (reembolsavel) {
                            totalReembolsos = totalReembolsos.add(valor);
                        } else {
                            totalDespesas = totalDespesas.add(valor);
                        }
                    }

                    // Tratamento seguro para as datas vindas do banco
                    LocalDate dataRegisto = rs.getDate("data_registo") != null ? rs.getDate("data_registo").toLocalDate() : null;
                    LocalDate dataCobranca = rs.getDate("data_cobranca") != null ? rs.getDate("data_cobranca").toLocalDate() : null;

                    Transacao t = Transacao.builder()
                            .idTransacao(rs.getInt("id_transacao"))
                            .descricao(rs.getString("descricao"))
                            .valor(valor)
                            .dataRegisto(dataRegisto)   // <--- ADICIONADO AQUI
                            .dataCobranca(dataCobranca) // <--- ADICIONADO AQUI
                            .status(rs.getString("status"))
                            .tipo(tipo)
                            .reembolsavel(reembolsavel)
                            .categoria(Categoria.builder()
                                    .idCategoria(rs.getInt("id_categoria"))
                                    .nome(rs.getString("categoria_nome"))
                                    .build())
                            .cartao(Cartao.builder()
                                    .idCartao(rs.getInt("id_cartao"))
                                    .nome(rs.getString("cartao_nome"))
                                    .diaVencimento(rs.getInt("dia_vencimento"))
                                    .build())
                            .build();
                    lista.add(t);
                }
            }
        }

        BigDecimal sobra = totalReceitas.subtract(totalDespesas).subtract(investimentos);

        return ResumoMensalDTO.builder()
                .transacoes(lista)
                .totalReceitas(totalReceitas)
                .totalDespesas(totalDespesas)
                .totalReembolsos(totalReembolsos)
                .investimentos(investimentos)
                .sobra(sobra)
                .build();
    }

    public List<Transacao> listarReembolsaveisPorMes(LocalDate mes) throws SQLException {
        LocalDate inicioMes = mes.withDayOfMonth(1);
        LocalDate fimMes = mes.withDayOfMonth(mes.lengthOfMonth());

        String sql = """
            SELECT t.*, cat.nome AS categoria_nome, car.nome AS cartao_nome
            FROM tb_transacao t
            LEFT JOIN tb_categoria cat ON t.id_categoria = cat.id_categoria
            LEFT JOIN tb_cartao car ON t.id_cartao = car.id_cartao
            WHERE t.data_cobranca BETWEEN ? AND ?
            AND t.reembolsavel = 1
            ORDER BY t.data_cobranca ASC
            """;

        List<Transacao> lista = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setDate(1, Date.valueOf(inicioMes));
            pstmt.setDate(2, Date.valueOf(fimMes));

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    BigDecimal valor = rs.getBigDecimal("valor");
                    if (valor == null) valor = BigDecimal.ZERO;

                    Transacao t = Transacao.builder()
                            .idTransacao(rs.getInt("id_transacao"))
                            .descricao(rs.getString("descricao"))
                            .valor(valor)
                            .dataCobranca(rs.getDate("data_cobranca").toLocalDate())
                            .status(rs.getString("status"))
                            .categoria(Categoria.builder().nome(rs.getString("categoria_nome")).build())
                            .cartao(Cartao.builder().nome(rs.getString("cartao_nome")).build())
                            .build();
                    lista.add(t);
                }
            }
        }
        return lista;
    }

    public void atualizar(Transacao t) throws SQLException {
        String sql = """
            UPDATE tb_transacao 
            SET descricao = ?, valor = ?, data_cobranca = ?, tipo = ?, id_cartao = ?, id_categoria = ?, reembolsavel = ? 
            WHERE id_transacao = ?
            """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, t.getDescricao());
            pstmt.setBigDecimal(2, t.getValor());
            pstmt.setDate(3, Date.valueOf(t.getDataCobranca()));
            pstmt.setString(4, t.getTipo());
            pstmt.setInt(5, t.getCartao().getIdCartao());
            pstmt.setInt(6, t.getCategoria().getIdCategoria());
            pstmt.setInt(7, t.isReembolsavel() ? 1 : 0);
            pstmt.setInt(8, t.getIdTransacao());

            pstmt.executeUpdate();
        }
    }

    public void excluir(int idTransacao) throws SQLException {
        String sql = "DELETE FROM tb_transacao WHERE id_transacao = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idTransacao);
            pstmt.executeUpdate();
        }
    }
}