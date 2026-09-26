package org.cdg.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.geometry.Pos;
import org.cdg.model.Cartao;
import org.cdg.model.Categoria;
import org.cdg.model.Conta;
import org.cdg.model.Titular;
import org.cdg.model.Transacao;
import org.cdg.repository.CartaoRepository;
import org.cdg.repository.CategoriaRepository;
import org.cdg.repository.DatabaseConnection;
import org.cdg.repository.TransacaoRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DashboardController {

    // --- ELEMENTOS DA ABA DASHBOARD ---
    @FXML private Label lblSobra;
    @FXML private Label lblDeficit;
    @FXML private Label lblReembolso;
    @FXML private Label lblInvestido;
    @FXML private Label lblMesAno;
    @FXML private Button btnInvestirSobra;

    @FXML private HBox boxFaturasCartoes;
    @FXML private HBox boxGastosCategorias;

    @FXML private TableView<Transacao> tabelaTransacoes;
    @FXML private TableColumn<Transacao, String> colData;
    @FXML private TableColumn<Transacao, String> colDescricao;
    @FXML private TableColumn<Transacao, String> colCartao;
    @FXML private TableColumn<Transacao, String> colCategoria;
    @FXML private TableColumn<Transacao, String> colValor;
    @FXML private TableColumn<Transacao, String> colStatus;

    // --- ELEMENTOS DA ABA CARTÕES ---
    @FXML private TextField txtNomeCartao;
    @FXML private TextField txtDiaVencimento;
    @FXML private TableView<Cartao> tabelaCartoes;
    @FXML private TableColumn<Cartao, String> colCartaoNome;
    @FXML private TableColumn<Cartao, Integer> colCartaoVencimento;

    // --- ELEMENTOS DA ABA CATEGORIAS ---
    @FXML private TextField txtNomeCategoria;
    @FXML private TableView<Categoria> tabelaCategorias;
    @FXML private TableColumn<Categoria, String> colCategoriaNome;

    private LocalDate mesAtual = LocalDate.now();
    private double sobraDesteMes = 0;

    @FXML
    public void initialize() {
        configurarColunas();
        atualizarInterface();

        configurarColunasCartoes();
        carregarDadosCartoes();

        configurarColunasCategorias();
        carregarDadosCategorias();
    }

    // =========================================================================
    // LÓGICA DA ABA: DASHBOARD
    // =========================================================================

    @FXML
    public void mesAnterior() {
        mesAtual = mesAtual.minusMonths(1);
        atualizarInterface();
    }

    @FXML
    public void mesSeguinte() {
        mesAtual = mesAtual.plusMonths(1);
        atualizarInterface();
    }

    private void atualizarInterface() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM / yyyy");
        lblMesAno.setText(mesAtual.format(fmt));

        carregarDadosTabela();
        carregarInvestimentoGlobal();
        carregarFaturasCartoes();
        carregarGastosCategorias();
    }

    private void configurarColunas() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        colData.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDataCobranca().format(formatter)));
        colDescricao.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDescricao()));

        colCartao.setCellValueFactory(cellData -> {
            if (cellData.getValue().getCartao() != null) {
                return new SimpleStringProperty(cellData.getValue().getCartao().getNome());
            }
            return new SimpleStringProperty("-");
        });

        colCategoria.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getCategoria().getNome()));

        colValor.setCellValueFactory(cellData -> {
            Transacao t = cellData.getValue();
            String prefixo = t.getTipo().equals("RECEITA") ? "+ R$ " : "- R$ ";
            return new SimpleStringProperty(String.format(prefixo + "%.2f", t.getValor()));
        });

        colStatus.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getStatus()));
    }

    private void carregarDadosTabela() {
        ObservableList<Transacao> listaTransacoes = FXCollections.observableArrayList();
        LocalDate inicioMes = mesAtual.withDayOfMonth(1);
        LocalDate fimMes = mesAtual.withDayOfMonth(mesAtual.lengthOfMonth());

        String sql = """
            SELECT t.*, cat.nome AS categoria_nome, car.nome AS cartao_nome, car.dia_vencimento 
            FROM tb_transacao t 
            INNER JOIN tb_categoria cat ON t.id_categoria = cat.id_categoria 
            INNER JOIN tb_cartao car ON t.id_cartao = car.id_cartao 
            WHERE t.data_cobranca BETWEEN ? AND ?
            ORDER BY t.data_cobranca ASC
            """;

        double totalReceitas = 0;
        double totalDespesas = 0;
        double totalReembolsos = 0;
        double investimentosDesteMes = 0;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setDate(1, java.sql.Date.valueOf(inicioMes));
            pstmt.setDate(2, java.sql.Date.valueOf(fimMes));

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    double valor = rs.getDouble("valor");
                    String tipo = rs.getString("tipo");
                    boolean reembolsavel = rs.getInt("reembolsavel") == 1;
                    if (tipo == null) tipo = "DESPESA";

                    if (tipo.equals("RECEITA")) {
                        totalReceitas += valor;
                    } else if (tipo.equals("INVESTIMENTO")) {
                        investimentosDesteMes += valor;
                    } else {
                        if (reembolsavel) {
                            totalReembolsos += valor;
                        } else {
                            totalDespesas += valor;
                        }
                    }

                    Transacao t = Transacao.builder()
                            .idTransacao(rs.getInt("id_transacao"))
                            .descricao(rs.getString("descricao"))
                            .valor(valor)
                            .dataCobranca(rs.getDate("data_cobranca").toLocalDate())
                            .status(rs.getString("status"))
                            .tipo(tipo)
                            .reembolsavel(reembolsavel)
                            .categoria(org.cdg.model.Categoria.builder()
                                    .idCategoria(rs.getInt("id_categoria"))
                                    .nome(rs.getString("categoria_nome"))
                                    .build())
                            .cartao(org.cdg.model.Cartao.builder()
                                    .idCartao(rs.getInt("id_cartao"))
                                    .nome(rs.getString("cartao_nome"))
                                    .diaVencimento(rs.getInt("dia_vencimento"))
                                    .build())
                            .build();
                    listaTransacoes.add(t);
                }
            }
            tabelaTransacoes.setItems(listaTransacoes);
            sobraDesteMes = totalReceitas - totalDespesas - investimentosDesteMes;
            atualizarResumoMensal(totalDespesas, totalReembolsos);

        } catch (SQLException e) {
            System.err.println("Erro ao carregar tabela: " + e.getMessage());
        }
    }

    private void carregarFaturasCartoes() {
        boxFaturasCartoes.getChildren().clear();

        LocalDate inicioMes = mesAtual.withDayOfMonth(1);
        LocalDate fimMes = mesAtual.withDayOfMonth(mesAtual.lengthOfMonth());

        String sql = """
            SELECT c.nome, SUM(t.valor) as total
            FROM tb_transacao t
            INNER JOIN tb_cartao c ON t.id_cartao = c.id_cartao
            WHERE t.data_cobranca BETWEEN ? AND ?
            AND t.tipo = 'DESPESA'
            GROUP BY c.nome
            HAVING total > 0
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setDate(1, java.sql.Date.valueOf(inicioMes));
            pstmt.setDate(2, java.sql.Date.valueOf(fimMes));

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String nomeCartao = rs.getString("nome");
                    double total = rs.getDouble("total");

                    VBox boxCartao = new VBox();
                    boxCartao.getStyleClass().add("card-cartao");
                    boxCartao.setAlignment(Pos.CENTER);
                    boxCartao.setPrefWidth(180);

                    Label lblNome = new Label(nomeCartao);
                    lblNome.setStyle("-fx-font-size: 11px; -fx-text-fill: #858585;");

                    Label lblTotal = new Label(String.format("R$ %.2f", total));
                    lblTotal.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #ffb74d;");

                    boxCartao.getChildren().addAll(lblNome, lblTotal);
                    boxFaturasCartoes.getChildren().add(boxCartao);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro ao carregar faturas dos cartões: " + e.getMessage());
        }
    }

    private void carregarGastosCategorias() {
        boxGastosCategorias.getChildren().clear();

        LocalDate inicioMes = mesAtual.withDayOfMonth(1);
        LocalDate fimMes = mesAtual.withDayOfMonth(mesAtual.lengthOfMonth());

        String sql = """
            SELECT c.nome, SUM(t.valor) as total
            FROM tb_transacao t
            INNER JOIN tb_categoria c ON t.id_categoria = c.id_categoria
            WHERE t.data_cobranca BETWEEN ? AND ?
            AND t.tipo = 'DESPESA'
            GROUP BY c.nome
            HAVING total > 0
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setDate(1, java.sql.Date.valueOf(inicioMes));
            pstmt.setDate(2, java.sql.Date.valueOf(fimMes));

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String nomeCat = rs.getString("nome");
                    double total = rs.getDouble("total");

                    VBox boxCat = new VBox();
                    boxCat.getStyleClass().add("card-categoria");
                    boxCat.setAlignment(Pos.CENTER);
                    boxCat.setPrefWidth(180);

                    Label lblNome = new Label(nomeCat);
                    lblNome.setStyle("-fx-font-size: 11px; -fx-text-fill: #858585;");

                    Label lblTotal = new Label(String.format("R$ %.2f", total));
                    lblTotal.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #00c853;");

                    boxCat.getChildren().addAll(lblNome, lblTotal);
                    boxGastosCategorias.getChildren().add(boxCat);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro ao carregar gastos por categoria: " + e.getMessage());
        }
    }

    private void carregarInvestimentoGlobal() {
        String sql = "SELECT SUM(valor) FROM tb_transacao WHERE tipo = 'INVESTIMENTO'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            if (rs.next()) {
                double totalGlobal = rs.getDouble(1);
                lblInvestido.setText(String.format("R$ %.2f", totalGlobal));
            }
        } catch (SQLException e) {
            System.err.println("Erro ao buscar total investido: " + e.getMessage());
        }
    }

    private void atualizarResumoMensal(double despesas, double reembolsos) {
        lblDeficit.setText(String.format("R$ %.2f", despesas));
        lblReembolso.setText(String.format("R$ %.2f", reembolsos));
        lblSobra.setText(String.format("R$ %.2f", sobraDesteMes));

        if (sobraDesteMes > 0) {
            lblSobra.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #00c853;");
            btnInvestirSobra.setVisible(true);
        } else {
            lblSobra.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #ff5252;");
            btnInvestirSobra.setVisible(false);
        }
    }

    @FXML
    public void investirSobra() {
        if (sobraDesteMes <= 0) return;
        LocalDate ultimoDiaDoMes = mesAtual.withDayOfMonth(mesAtual.lengthOfMonth());
        Transacao aporte = Transacao.builder()
                .descricao("Aporte Fim de Mês (Automático)")
                .valor(sobraDesteMes)
                .dataRegisto(LocalDate.now())
                .dataCobranca(ultimoDiaDoMes)
                .parcelaAtual(1)
                .totalParcelas(1)
                .status("PAGO")
                .tipo("INVESTIMENTO")
                .cartao(Cartao.builder().idCartao(1).build())
                .categoria(Categoria.builder().idCategoria(1).build())
                .conta(Conta.builder().idConta(1).build())
                .titular(Titular.builder().idTitular(1).build())
                .build();

        TransacaoRepository repo = new TransacaoRepository();
        repo.salvar(aporte);
        atualizarInterface();
    }

    @FXML
    public void abrirFormulario() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/view/transacao_form.fxml"));
            javafx.scene.Parent root = loader.load();

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Nova Transação");

            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            scene.getStylesheets().add(getClass().getResource("/view/style.css").toExternalForm());

            stage.setScene(scene);
            stage.initOwner(tabelaTransacoes.getScene().getWindow());
            stage.initModality(javafx.stage.Modality.WINDOW_MODAL);
            stage.showAndWait();

            atualizarInterface();
        } catch (Exception e) {
            System.err.println("Erro ao abrir formulário: " + e.getMessage());
        }
    }

    // =========================================================================
    // LÓGICA DA ABA: CARTÕES
    // =========================================================================

    private void configurarColunasCartoes() {
        colCartaoNome.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getNome()));
        colCartaoVencimento.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getDiaVencimento()));
    }

    private void carregarDadosCartoes() {
        CartaoRepository repo = new CartaoRepository();
        ObservableList<Cartao> listaCartoes = FXCollections.observableArrayList(repo.listarTodos());
        tabelaCartoes.setItems(listaCartoes);
    }

    @FXML
    public void salvarCartao() {
        try {
            String nome = txtNomeCartao.getText();
            String diaTexto = txtDiaVencimento.getText();

            if (nome == null || nome.trim().isEmpty() || diaTexto == null || diaTexto.trim().isEmpty()) {
                throw new Exception("Preencha todos os campos.");
            }

            int dia = Integer.parseInt(diaTexto);
            if (dia < 1 || dia > 31) {
                throw new Exception("O dia deve estar entre 1 e 31.");
            }

            Cartao novo = Cartao.builder().nome(nome).diaVencimento(dia).build();
            CartaoRepository repo = new CartaoRepository();
            repo.salvar(novo);

            txtNomeCartao.clear();
            txtDiaVencimento.clear();
            carregarDadosCartoes();

        } catch (Exception e) {
            Alert alerta = new Alert(Alert.AlertType.ERROR);
            alerta.setTitle("Erro");
            alerta.setHeaderText("Falha ao gravar cartão");
            alerta.setContentText(e.getMessage());
            alerta.showAndWait();
        }
    }

    // =========================================================================
    // LÓGICA DA ABA: CATEGORIAS
    // =========================================================================

    private void configurarColunasCategorias() {
        colCategoriaNome.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getNome()));
    }

    private void carregarDadosCategorias() {
        CategoriaRepository repo = new CategoriaRepository();
        ObservableList<Categoria> listaCategorias = FXCollections.observableArrayList(repo.listarTodas());
        tabelaCategorias.setItems(listaCategorias);
    }

    @FXML
    public void salvarCategoria() {
        try {
            String nome = txtNomeCategoria.getText();

            if (nome == null || nome.trim().isEmpty()) {
                throw new Exception("O nome da categoria não pode estar vazio.");
            }

            Categoria nova = Categoria.builder().nome(nome).build();
            CategoriaRepository repo = new CategoriaRepository();
            repo.salvar(nova);

            txtNomeCategoria.clear();
            carregarDadosCategorias();

        } catch (Exception e) {
            Alert alerta = new Alert(Alert.AlertType.ERROR);
            alerta.setTitle("Erro");
            alerta.setHeaderText("Falha ao gravar categoria");
            alerta.setContentText(e.getMessage());
            alerta.showAndWait();
        }
    }
}