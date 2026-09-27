package org.cdg.controller;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.cdg.model.*;
import org.cdg.repository.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class DashboardController {

    private Cartao cartaoParaEditar = null;
    private Categoria categoriaParaEditar = null;
    private Meta metaParaEditar = null;

    // --- ELEMENTOS DA DASHBOARD PRINCIPAL ---
    @FXML private Label lblSobra;
    @FXML private Label lblDeficit;
    @FXML private Label lblReembolso;
    @FXML private Label lblInvestido;
    @FXML private Label lblMesAno;
    @FXML private Button btnInvestirSobra;

    @FXML private HBox boxCardsMetas;
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

    // --- ELEMENTOS DA ABA METAS ---
    @FXML private TextField txtDescricaoMeta;
    @FXML private TextField txtValorAlvoMeta;
    @FXML private TextField txtDataLimiteMeta;
    @FXML private TableView<Meta> tabelaMetas;
    @FXML private TableColumn<Meta, String> colMetaDescricao;
    @FXML private TableColumn<Meta, String> colMetaAlvo;
    @FXML private TableColumn<Meta, String> colMetaProgresso;

    private LocalDate mesAtual = LocalDate.now();
    private BigDecimal sobraDesteMes = BigDecimal.ZERO;

    @FXML
    public void initialize() {
        btnInvestirSobra.managedProperty().bind(btnInvestirSobra.visibleProperty());

        configurarColunas();
        atualizarInterface();

        configurarColunasCartoes();
        carregarDadosCartoes();

        configurarColunasCategorias();
        carregarDadosCategorias();

        configurarColunasMetas();
        carregarDadosMetas();

        tabelaCartoes.getSelectionModel().selectedItemProperty().addListener((obs, antigo, novo) -> {
            if (novo != null) {
                cartaoParaEditar = novo;
                txtNomeCartao.setText(novo.getNome());
                txtDiaVencimento.setText(String.valueOf(novo.getDiaVencimento()));
            }
        });

        tabelaCategorias.getSelectionModel().selectedItemProperty().addListener((obs, antigo, novo) -> {
            if (novo != null) {
                categoriaParaEditar = novo;
                txtNomeCategoria.setText(novo.getNome());
            }
        });

        tabelaMetas.getSelectionModel().selectedItemProperty().addListener((obs, antigo, novo) -> {
            if (novo != null) {
                metaParaEditar = novo;
                txtDescricaoMeta.setText(novo.getDescricao());
                txtValorAlvoMeta.setText(novo.getValorAlvo() != null ? novo.getValorAlvo().toString() : "");
                if (novo.getDataLimite() != null) {
                    txtDataLimiteMeta.setText(novo.getDataLimite().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                }
            }
        });
    }

    // =========================================================================
    // CADASTRO RÁPIDO VIA DIÁLOGO NATIVO
    // =========================================================================

    @FXML
    public void criarNovaCategoriaRapida() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Nova Categoria");
        dialog.setHeaderText("Cadastrar Categoria Rapidamente");
        dialog.setContentText("Nome da Categoria:");

        dialog.showAndWait().ifPresent(nome -> {
            String nomeFormatado = nome.trim();
            if (!nomeFormatado.isEmpty()) {
                Categoria nova = Categoria.builder().nome(nomeFormatado).build();
                new CategoriaRepository().salvar(nova);
                carregarDadosCategorias();
                atualizarInterface();
            }
        });
    }

    @FXML
    public void criarNovoCartaoRapido() {
        Dialog<Cartao> dialog = new Dialog<>();
        dialog.setTitle("Novo Cartão");
        dialog.setHeaderText("Cadastrar Novo Cartão");

        ButtonType btnSalvar = new ButtonType("Salvar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnSalvar, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        TextField txtNome = new TextField();
        txtNome.setPromptText("Ex: Nubank, Itaú");
        TextField txtDia = new TextField();
        txtDia.setPromptText("Ex: 10");

        grid.add(new Label("Nome do Cartão:"), 0, 0);
        grid.add(txtNome, 1, 0);
        grid.add(new Label("Dia Vencimento (1-31):"), 0, 1);
        grid.add(txtDia, 1, 1);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == btnSalvar) {
                try {
                    String nome = txtNome.getText().trim();
                    int dia = Integer.parseInt(txtDia.getText().trim());
                    if (!nome.isEmpty() && dia >= 1 && dia <= 31) {
                        return Cartao.builder().nome(nome).diaVencimento(dia).build();
                    }
                } catch (Exception ignored) {}
            }
            return null;
        });

        dialog.showAndWait().ifPresent(cartao -> {
            new CartaoRepository().salvar(cartao);
            carregarDadosCartoes();
            atualizarInterface();
        });
    }

    // =========================================================================
    // LÓGICA E RENDERIZAÇÃO DA DASHBOARD
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
        carregarCardsMetasDashboard();
    }

    private void configurarColunas() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        colData.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDataCobranca().format(formatter)));
        colDescricao.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDescricao()));

        colCartao.setCellValueFactory(cellData -> {
            if (cellData.getValue().getCartao() != null && cellData.getValue().getCartao().getNome() != null) {
                return new SimpleStringProperty(cellData.getValue().getCartao().getNome());
            }
            return new SimpleStringProperty("-");
        });

        colCategoria.setCellValueFactory(cellData -> {
            if (cellData.getValue().getCategoria() != null && cellData.getValue().getCategoria().getNome() != null) {
                return new SimpleStringProperty(cellData.getValue().getCategoria().getNome());
            }
            return new SimpleStringProperty("-");
        });

        colValor.setCellValueFactory(cellData -> {
            Transacao t = cellData.getValue();
            boolean isReceita = "RECEITA".equalsIgnoreCase(t.getTipo());
            String sinal = isReceita ? "+ " : "- ";
            return new SimpleStringProperty(sinal + formatarMoeda(t.getValor()));
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
            LEFT JOIN tb_categoria cat ON t.id_categoria = cat.id_categoria 
            LEFT JOIN tb_cartao car ON t.id_cartao = car.id_cartao 
            WHERE t.data_cobranca BETWEEN ? AND ?
            ORDER BY t.data_cobranca ASC
            """;

        BigDecimal totalReceitas = BigDecimal.ZERO;
        BigDecimal totalDespesas = BigDecimal.ZERO;
        BigDecimal totalReembolsos = BigDecimal.ZERO;
        BigDecimal investimentosDesteMes = BigDecimal.ZERO;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setDate(1, java.sql.Date.valueOf(inicioMes));
            pstmt.setDate(2, java.sql.Date.valueOf(fimMes));

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
                        investimentosDesteMes = investimentosDesteMes.add(valor);
                    } else {
                        if (reembolsavel) {
                            totalReembolsos = totalReembolsos.add(valor);
                        } else {
                            totalDespesas = totalDespesas.add(valor);
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
                    listaTransacoes.add(t);
                }
            }
            tabelaTransacoes.setItems(listaTransacoes);

            // CÁLCULO DA SOBRA: Receitas - Despesas - Investimentos
            sobraDesteMes = totalReceitas.subtract(totalDespesas).subtract(investimentosDesteMes);
            atualizarResumoMensal(totalDespesas, totalReembolsos);

        } catch (SQLException e) {
            System.err.println("Erro ao carregar tabela: " + e.getMessage());
        }
    }

    private BigDecimal obterTotalInvestidoGlobal() {
        BigDecimal totalInvestidoGlobal = BigDecimal.ZERO;
        String sqlInvestido = "SELECT SUM(valor) FROM tb_transacao WHERE tipo = 'INVESTIMENTO'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sqlInvestido);
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                totalInvestidoGlobal = rs.getBigDecimal(1);
                if (totalInvestidoGlobal == null) totalInvestidoGlobal = BigDecimal.ZERO;
            }
        } catch (SQLException e) {
            System.err.println("Erro ao buscar total investido: " + e.getMessage());
        }
        return totalInvestidoGlobal;
    }

    private void carregarCardsMetasDashboard() {
        boxCardsMetas.getChildren().clear();
        MetaRepository repo = new MetaRepository();
        BigDecimal totalInvestidoGlobal = obterTotalInvestidoGlobal();

        for (Meta m : repo.listarTodas()) {
            BigDecimal alvo = m.getValorAlvo() != null ? m.getValorAlvo() : BigDecimal.ZERO;
            BigDecimal acumulado = totalInvestidoGlobal;

            double fracao = alvo.compareTo(BigDecimal.ZERO) > 0 ?
                    acumulado.divide(alvo, 4, RoundingMode.HALF_UP).doubleValue() : 0.0;

            BigDecimal falta = alvo.subtract(acumulado);
            if (falta.compareTo(BigDecimal.ZERO) < 0) {
                falta = BigDecimal.ZERO;
            }

            BigDecimal pct = alvo.compareTo(BigDecimal.ZERO) > 0 ?
                    acumulado.multiply(new BigDecimal("100")).divide(alvo, 0, RoundingMode.HALF_UP) : BigDecimal.ZERO;

            HBox itemMeta = new HBox(12);
            itemMeta.setAlignment(Pos.CENTER_LEFT);
            itemMeta.setStyle("-fx-background-color: #212225; -fx-padding: 8 12; -fx-background-radius: 6; -fx-border-color: #37393e; -fx-border-radius: 6;");

            ProgressIndicator ring = new ProgressIndicator(Math.min(fracao, 1.0));
            ring.setPrefSize(42, 42);
            ring.setStyle(fracao >= 1.0 ? "-fx-progress-color: #00c853;" : "-fx-progress-color: #3574f0;");

            VBox info = new VBox(2);

            Label lblTitulo = new Label(m.getDescricao());
            lblTitulo.setStyle("-fx-font-weight: bold; -fx-text-fill: #ffffff; -fx-font-size: 12px;");

            Label lblAlvo = new Label(String.format("Alvo: R$ %,.2f (%s%%)", alvo, pct.toString()));
            lblAlvo.setStyle("-fx-font-size: 10px; -fx-text-fill: #9da5b4;");

            Label lblFalta = new Label(String.format("Falta: R$ %,.2f", falta));
            lblFalta.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #ffb74d;");

            info.getChildren().addAll(lblTitulo, lblAlvo, lblFalta);
            itemMeta.getChildren().addAll(ring, info);
            boxCardsMetas.getChildren().add(itemMeta);
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
            ORDER BY total DESC
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setDate(1, java.sql.Date.valueOf(inicioMes));
            pstmt.setDate(2, java.sql.Date.valueOf(fimMes));

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String nomeCat = rs.getString("nome");
                    BigDecimal total = rs.getBigDecimal("total");

                    VBox card = new VBox(4);
                    card.setStyle("-fx-background-color: #212225; -fx-padding: 10 16; -fx-background-radius: 6; -fx-border-color: #37393e; -fx-border-radius: 6;");
                    card.setAlignment(Pos.CENTER);
                    card.setMinWidth(160);

                    Label lblNome = new Label(nomeCat);
                    lblNome.setStyle("-fx-font-size: 11px; -fx-text-fill: #9da5b4; -fx-font-weight: bold;");

                    Label lblTotal = new Label(formatarMoeda(total));
                    lblTotal.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #00c853;");

                    card.getChildren().addAll(lblNome, lblTotal);
                    boxGastosCategorias.getChildren().add(card);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro ao carregar gastos por categoria: " + e.getMessage());
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
            ORDER BY total DESC
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setDate(1, java.sql.Date.valueOf(inicioMes));
            pstmt.setDate(2, java.sql.Date.valueOf(fimMes));

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String nomeCartao = rs.getString("nome");
                    BigDecimal total = rs.getBigDecimal("total");

                    VBox card = new VBox(4);
                    card.setStyle("-fx-background-color: #212225; -fx-padding: 10 16; -fx-background-radius: 6; -fx-border-color: #37393e; -fx-border-radius: 6;");
                    card.setAlignment(Pos.CENTER);
                    card.setMinWidth(160);

                    Label lblNome = new Label(nomeCartao);
                    lblNome.setStyle("-fx-font-size: 11px; -fx-text-fill: #9da5b4; -fx-font-weight: bold;");

                    Label lblTotal = new Label(formatarMoeda(total));
                    lblTotal.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #ffb74d;");

                    card.getChildren().addAll(lblNome, lblTotal);
                    boxFaturasCartoes.getChildren().add(card);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro ao carregar faturas dos cartões: " + e.getMessage());
        }
    }

    private void carregarInvestimentoGlobal() {
        BigDecimal totalGlobal = obterTotalInvestidoGlobal();
        lblInvestido.setText(formatarMoeda(totalGlobal));
    }

    private void atualizarResumoMensal(BigDecimal despesas, BigDecimal reembolsos) {
        lblDeficit.setText(formatarMoeda(despesas));
        lblReembolso.setText(formatarMoeda(reembolsos));

        if (sobraDesteMes.compareTo(BigDecimal.ZERO) < 0) {
            lblSobra.setText(formatarMoeda(sobraDesteMes));
            lblSobra.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #f75454;");
            btnInvestirSobra.setVisible(false);
        } else {
            lblSobra.setText(formatarMoeda(sobraDesteMes));
            lblSobra.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #00c853;");
            btnInvestirSobra.setVisible(sobraDesteMes.compareTo(BigDecimal.ZERO) > 0);
        }
    }

    @FXML
    public void investirSobra() {
        if (sobraDesteMes == null || sobraDesteMes.compareTo(BigDecimal.ZERO) <= 0) return;

        CartaoRepository cartaoRepo = new CartaoRepository();
        CategoriaRepository catRepo = new CategoriaRepository();

        List<Cartao> cartoes = cartaoRepo.listarTodos();
        List<Categoria> categorias = catRepo.listarTodas();

        if (cartoes.isEmpty() || categorias.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Cadastre pelo menos 1 cartão e 1 categoria antes de investir.").showAndWait();
            return;
        }

        LocalDate ultimoDiaDoMes = mesAtual.withDayOfMonth(mesAtual.lengthOfMonth());

        // Instancia os objetos base de Conta e Titular idênticos aos usados no TransacaoFormController
        Conta contaBase = Conta.builder().idConta(1).build();
        Titular titularBase = Titular.builder().idTitular(1).build();

        Transacao aporte = Transacao.builder()
                .descricao("Aporte Fim de Mês (Automático)")
                .valor(sobraDesteMes)
                .dataRegisto(LocalDate.now())
                .dataCobranca(ultimoDiaDoMes)
                .parcelaAtual(1)
                .totalParcelas(1)
                .status("PAGO")
                .tipo("INVESTIMENTO")
                .cartao(cartoes.get(0))
                .categoria(categorias.get(0))
                .conta(contaBase)
                .titular(titularBase)
                .build();

        TransacaoRepository repo = new TransacaoRepository();
        repo.salvar(aporte);

        atualizarInterface();
    }

    // =========================================================================
    // AÇÕES DA TABELA DE TRANSAÇÕES
    // =========================================================================

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

    @FXML
    public void editarTransacao() {
        Transacao selecionada = tabelaTransacoes.getSelectionModel().getSelectedItem();

        if (selecionada == null) {
            Alert alerta = new Alert(Alert.AlertType.WARNING, "Selecione uma transação na tabela para editar.");
            alerta.showAndWait();
            return;
        }

        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/view/transacao_form.fxml"));
            javafx.scene.Parent root = loader.load();

            TransacaoFormController formController = loader.getController();
            formController.setTransacaoParaEditar(selecionada);

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Editar Transação");

            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            scene.getStylesheets().add(getClass().getResource("/view/style.css").toExternalForm());

            stage.setScene(scene);
            stage.initOwner(tabelaTransacoes.getScene().getWindow());
            stage.initModality(javafx.stage.Modality.WINDOW_MODAL);
            stage.showAndWait();

            atualizarInterface();
        } catch (Exception e) {
            System.err.println("Erro ao abrir formulário de edição: " + e.getMessage());
        }
    }

    @FXML
    public void excluirTransacao() {
        Transacao selecionada = tabelaTransacoes.getSelectionModel().getSelectedItem();

        if (selecionada == null) {
            Alert alerta = new Alert(Alert.AlertType.WARNING, "Selecione uma transação na tabela para excluir.");
            alerta.showAndWait();
            return;
        }

        Alert confirmacao = new Alert(Alert.AlertType.CONFIRMATION, "Excluir transação: " + selecionada.getDescricao() + "?");
        if (confirmacao.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                TransacaoRepository repo = new TransacaoRepository();
                repo.excluir(selecionada.getIdTransacao());
                atualizarInterface();
            } catch (Exception e) {
                Alert erro = new Alert(Alert.AlertType.ERROR, "Não foi possível excluir: " + e.getMessage());
                erro.showAndWait();
            }
        }
    }

    // =========================================================================
    // LÓGICA DAS ABAS SECUNDÁRIAS (CARTÕES, CATEGORIAS, METAS)
    // =========================================================================

    private void configurarColunasCartoes() {
        colCartaoNome.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getNome()));
        colCartaoVencimento.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getDiaVencimento()));
    }

    private void carregarDadosCartoes() {
        CartaoRepository repo = new CartaoRepository();
        tabelaCartoes.setItems(FXCollections.observableArrayList(repo.listarTodos()));
    }

    @FXML
    public void salvarCartao() {
        try {
            String nome = txtNomeCartao.getText();
            String diaTexto = txtDiaVencimento.getText();

            if (nome == null || nome.trim().isEmpty() || diaTexto == null || diaTexto.trim().isEmpty()) {
                throw new Exception("Preencha todos os campos do cartão.");
            }

            int dia = Integer.parseInt(diaTexto.trim());
            CartaoRepository repo = new CartaoRepository();

            if (cartaoParaEditar != null) {
                cartaoParaEditar.setNome(nome);
                cartaoParaEditar.setDiaVencimento(dia);
                repo.atualizar(cartaoParaEditar);
                cartaoParaEditar = null;
            } else {
                repo.salvar(Cartao.builder().nome(nome).diaVencimento(dia).build());
            }

            txtNomeCartao.clear();
            txtDiaVencimento.clear();
            carregarDadosCartoes();
            atualizarInterface();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Erro ao salvar cartão: " + e.getMessage()).showAndWait();
        }
    }

    @FXML
    public void excluirCartao() {
        Cartao selecionado = tabelaCartoes.getSelectionModel().getSelectedItem();
        if (selecionado == null) return;

        try {
            new CartaoRepository().excluir(selecionado.getIdCartao());
            txtNomeCartao.clear();
            txtDiaVencimento.clear();
            cartaoParaEditar = null;
            carregarDadosCartoes();
            atualizarInterface();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Erro ao excluir cartão: " + e.getMessage()).showAndWait();
        }
    }

    private void configurarColunasCategorias() {
        colCategoriaNome.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getNome()));
    }

    private void carregarDadosCategorias() {
        CategoriaRepository repo = new CategoriaRepository();
        tabelaCategorias.setItems(FXCollections.observableArrayList(repo.listarTodas()));
    }

    @FXML
    public void salvarCategoria() {
        try {
            String nome = txtNomeCategoria.getText();
            if (nome == null || nome.trim().isEmpty()) {
                throw new Exception("Preencha o nome da categoria.");
            }

            CategoriaRepository repo = new CategoriaRepository();

            if (categoriaParaEditar != null) {
                categoriaParaEditar.setNome(nome);
                repo.atualizar(categoriaParaEditar);
                categoriaParaEditar = null;
            } else {
                repo.salvar(Categoria.builder().nome(nome).build());
            }

            txtNomeCategoria.clear();
            carregarDadosCategorias();
            atualizarInterface();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Erro ao salvar categoria: " + e.getMessage()).showAndWait();
        }
    }

    @FXML
    public void excluirCategoria() {
        Categoria selecionada = tabelaCategorias.getSelectionModel().getSelectedItem();
        if (selecionada == null) return;

        try {
            new CategoriaRepository().excluir(selecionada.getIdCategoria());
            txtNomeCategoria.clear();
            categoriaParaEditar = null;
            carregarDadosCategorias();
            atualizarInterface();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Erro ao excluir categoria: " + e.getMessage()).showAndWait();
        }
    }

    private void configurarColunasMetas() {
        colMetaDescricao.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getDescricao()));
        colMetaAlvo.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getValorAlvo() != null ? String.format("R$ %,.2f", cell.getValue().getValorAlvo()) : "R$ 0,00"));

        colMetaProgresso.setCellFactory(column -> new TableCell<Meta, String>() {
            private final ProgressBar progressBar = new ProgressBar(0);
            private final Label lblStatusMeta = new Label("0.0%");
            private final HBox container = new HBox(8, progressBar, lblStatusMeta);

            {
                container.setAlignment(Pos.CENTER_LEFT);
                progressBar.setPrefWidth(100);
                progressBar.setStyle("-fx-accent: #3574f0;");
                lblStatusMeta.setStyle("-fx-text-fill: #bcbec4; -fx-font-size: 11px;");
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    Meta meta = getTableRow().getItem();
                    BigDecimal alvo = meta.getValorAlvo() != null ? meta.getValorAlvo() : BigDecimal.ZERO;
                    BigDecimal acumulado = obterTotalInvestidoGlobal();

                    if (alvo.compareTo(BigDecimal.ZERO) > 0) {
                        double fracao = acumulado.divide(alvo, 4, RoundingMode.HALF_UP).doubleValue();
                        progressBar.setProgress(Math.min(fracao, 1.0));

                        BigDecimal percentual = acumulado.multiply(new BigDecimal("100")).divide(alvo, 1, RoundingMode.HALF_UP);
                        BigDecimal falta = alvo.subtract(acumulado);
                        if (falta.compareTo(BigDecimal.ZERO) < 0) falta = BigDecimal.ZERO;

                        lblStatusMeta.setText(String.format("%.1f%% (Falta R$ %,.2f)", percentual.doubleValue(), falta));
                    } else {
                        progressBar.setProgress(0);
                        lblStatusMeta.setText("0.0%");
                    }
                    setGraphic(container);
                }
            }
        });
    }

    private void carregarDadosMetas() {
        MetaRepository repo = new MetaRepository();
        tabelaMetas.setItems(FXCollections.observableArrayList(repo.listarTodas()));
    }

    @FXML
    public void salvarMeta() {
        try {
            String desc = txtDescricaoMeta.getText();
            BigDecimal alvo = new BigDecimal(txtValorAlvoMeta.getText().replace(",", ".").trim());

            String textoData = txtDataLimiteMeta.getText().trim();
            if (textoData.matches("\\d{2}/\\d{2}/\\d{2}")) {
                textoData = textoData.substring(0, 6) + "20" + textoData.substring(6);
            }

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            LocalDate limite = LocalDate.parse(textoData, fmt);

            MetaRepository repo = new MetaRepository();

            if (metaParaEditar != null) {
                metaParaEditar.setDescricao(desc);
                metaParaEditar.setValorAlvo(alvo);
                metaParaEditar.setDataLimite(limite);
                repo.atualizar(metaParaEditar);
                metaParaEditar = null;
            } else {
                repo.salvar(Meta.builder()
                        .descricao(desc)
                        .valorAlvo(alvo)
                        .valorAtual(BigDecimal.ZERO)
                        .dataLimite(limite)
                        .build());
            }

            txtDescricaoMeta.clear();
            txtValorAlvoMeta.clear();
            txtDataLimiteMeta.clear();

            carregarDadosMetas();
            atualizarInterface();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Erro ao salvar meta: " + e.getMessage()).showAndWait();
        }
    }

    @FXML
    public void excluirMeta() {
        Meta selecionada = tabelaMetas.getSelectionModel().getSelectedItem();
        if (selecionada == null) return;

        try {
            new MetaRepository().excluir(selecionada.getIdMeta());
            txtDescricaoMeta.clear();
            txtValorAlvoMeta.clear();
            txtDataLimiteMeta.clear();
            metaParaEditar = null;
            carregarDadosMetas();
            atualizarInterface();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Erro ao excluir meta: " + e.getMessage()).showAndWait();
        }
    }

    @FXML
    public void gerarRelatorioReembolsos() {
        LocalDate inicioMes = mesAtual.withDayOfMonth(1);
        LocalDate fimMes = mesAtual.withDayOfMonth(mesAtual.lengthOfMonth());

        String sql = """
            SELECT t.*, cat.nome AS categoria_nome, car.nome AS cartao_nome
            FROM tb_transacao t
            LEFT JOIN tb_categoria cat ON t.id_categoria = cat.id_categoria
            LEFT JOIN tb_cartao car ON t.id_cartao = car.id_cartao
            WHERE t.data_cobranca BETWEEN ? AND ?
            AND t.reembolsavel = 1
            ORDER BY t.data_cobranca ASC
            """;

        List<Transacao> reembolsaveis = new ArrayList<>();
        BigDecimal totalReembolso = BigDecimal.ZERO;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setDate(1, java.sql.Date.valueOf(inicioMes));
            pstmt.setDate(2, java.sql.Date.valueOf(fimMes));

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    BigDecimal valor = rs.getBigDecimal("valor");
                    if (valor == null) valor = BigDecimal.ZERO;
                    totalReembolso = totalReembolso.add(valor);

                    Transacao t = Transacao.builder()
                            .idTransacao(rs.getInt("id_transacao"))
                            .descricao(rs.getString("descricao"))
                            .valor(valor)
                            .dataCobranca(rs.getDate("data_cobranca").toLocalDate())
                            .status(rs.getString("status"))
                            .categoria(Categoria.builder().nome(rs.getString("categoria_nome")).build())
                            .cartao(Cartao.builder().nome(rs.getString("cartao_nome")).build())
                            .build();
                    reembolsaveis.add(t);
                }
            }
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, "Erro ao procurar reembolsáveis: " + e.getMessage()).showAndWait();
            return;
        }

        if (reembolsaveis.isEmpty()) {
            new Alert(Alert.AlertType.INFORMATION, "Nenhuma despesa reembolsável encontrada para o mês " + lblMesAno.getText()).showAndWait();
            return;
        }

        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Salvar Relatório de Reembolsos");
        fileChooser.setInitialFileName("Relatorio_Reembolsos_" + mesAtual.format(DateTimeFormatter.ofPattern("MM_yyyy")) + ".pdf");
        fileChooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("Documento PDF", "*.pdf"));

        java.io.File file = fileChooser.showSaveDialog(lblSobra.getScene().getWindow());

        if (file != null) {
            try {
                com.lowagie.text.Document document = new com.lowagie.text.Document();
                com.lowagie.text.pdf.PdfWriter.getInstance(document, new java.io.FileOutputStream(file));

                document.open();

                com.lowagie.text.Font fontTitulo = com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 18);
                com.lowagie.text.Font fontSub = com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA, 12);
                com.lowagie.text.Font fontHeaderTab = com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 10);
                com.lowagie.text.Font fontCorpo = com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA, 10);

                com.lowagie.text.Paragraph pTitulo = new com.lowagie.text.Paragraph("Relatório de Despesas a Reembolsar", fontTitulo);
                pTitulo.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
                document.add(pTitulo);

                com.lowagie.text.Paragraph pData = new com.lowagie.text.Paragraph("Período: " + lblMesAno.getText() + " | Gerado em: " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), fontSub);
                pData.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
                pData.setSpacingAfter(20);
                document.add(pData);

                com.lowagie.text.pdf.PdfPTable table = new com.lowagie.text.pdf.PdfPTable(5);
                table.setWidthPercentage(100);
                table.setWidths(new float[]{2f, 4f, 2.5f, 2.5f, 2.5f});

                String[] colunas = {"Data", "Descrição", "Cartão", "Categoria", "Valor (R$)"};
                for (String col : colunas) {
                    com.lowagie.text.pdf.PdfPCell cell = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase(col, fontHeaderTab));
                    cell.setBackgroundColor(java.awt.Color.LIGHT_GRAY);
                    cell.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_CENTER);
                    cell.setPadding(6);
                    table.addCell(cell);
                }

                DateTimeFormatter fmtData = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                for (Transacao t : reembolsaveis) {
                    table.addCell(new com.lowagie.text.Phrase(t.getDataCobranca().format(fmtData), fontCorpo));
                    table.addCell(new com.lowagie.text.Phrase(t.getDescricao(), fontCorpo));
                    table.addCell(new com.lowagie.text.Phrase(t.getCartao() != null ? t.getCartao().getNome() : "-", fontCorpo));
                    table.addCell(new com.lowagie.text.Phrase(t.getCategoria() != null ? t.getCategoria().getNome() : "-", fontCorpo));

                    com.lowagie.text.pdf.PdfPCell cVal = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase(String.format("R$ %,.2f", t.getValor()), fontCorpo));
                    cVal.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_RIGHT);
                    table.addCell(cVal);
                }

                document.add(table);

                com.lowagie.text.Paragraph pTotal = new com.lowagie.text.Paragraph(String.format("Total a Reembolsar: R$ %,.2f", totalReembolso), fontTitulo);
                pTotal.setAlignment(com.lowagie.text.Element.ALIGN_RIGHT);
                pTotal.setSpacingBefore(15);
                document.add(pTotal);

                document.close();

                new Alert(Alert.AlertType.INFORMATION, "Relatório PDF gerado com sucesso em:\n" + file.getAbsolutePath()).showAndWait();

            } catch (Exception e) {
                new Alert(Alert.AlertType.ERROR, "Erro ao gerar PDF: " + e.getMessage()).showAndWait();
            }
        }
    }

    private static final java.text.NumberFormat FMT_MOEDA = java.text.NumberFormat.getCurrencyInstance(new java.util.Locale("pt", "BR"));

    private String formatarMoeda(BigDecimal valor) {
        if (valor == null) valor = BigDecimal.ZERO;
        return FMT_MOEDA.format(valor);
    }
}