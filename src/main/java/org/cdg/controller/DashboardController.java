package org.cdg.controller;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.cdg.model.*;
import org.cdg.repository.*;
import org.cdg.service.InvestimentoService;
import org.cdg.service.RelatorioPdfService;
import org.cdg.util.AlertHelper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class DashboardController {

    private Cartao cartaoParaEditar = null;
    private Categoria categoriaParaEditar = null;
    private Meta metaParaEditar = null;

    // --- ELEMENTOS DA DASHBOARD PRINCIPAL ---
    @FXML private Label lblSobra;
    @FXML private Label lblDeficit;
    @FXML private Label lblReembolso;
    @FXML private Label lblMesAno;
    @FXML private Button btnInvestirSobra;

    @FXML private HBox boxCardsMetas;
    @FXML private HBox boxFaturasCartoes;
    @FXML private HBox boxGastosCategorias;

    @FXML private TableView<Transacao> tabelaTransacoes;
    @FXML private TableColumn<Transacao, String> colDataCompra;
    @FXML private TableColumn<Transacao, String> colData;
    @FXML private TableColumn<Transacao, String> colDescricao;
    @FXML private TableColumn<Transacao, String> colCartao;
    @FXML private TableColumn<Transacao, String> colCategoria;
    @FXML private TableColumn<Transacao, String> colValor;
    @FXML private TableColumn<Transacao, String> colStatus;


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



        configurarColunasCategorias();
        carregarDadosCategorias();

        configurarColunasMetas();
        carregarDadosMetas();


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

    @FXML
    public void criarNovaCategoriaRapida() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Nova Categoria");
        dialog.setHeaderText("Cadastrar Categoria Rapidamente");
        dialog.setContentText("Nome da Categoria:");

        dialog.showAndWait().ifPresent(nome -> {
            String nomeFormatado = nome.trim();
            if (!nomeFormatado.isEmpty()) {
                try {
                    Categoria nova = Categoria.builder().nome(nomeFormatado).build();
                    new CategoriaRepository().salvar(nova);
                    carregarDadosCategorias();
                    atualizarInterface();
                } catch (Exception e) {
                    AlertHelper.showError("Erro ao cadastrar categoria rápida", e.getMessage());
                }
            }
        });
    }



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
        carregarFaturasCartoes();
        carregarGastosCategorias();
        carregarCardsMetasDashboard();
    }

    private void configurarColunas() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        colDescricao.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDescricao()));

        colDataCompra.setCellValueFactory(cellData -> {
            LocalDate d = cellData.getValue().getDataRegisto();
            return new SimpleStringProperty(d != null ? d.format(formatter) : "-");
        });
        colData.setCellValueFactory(cellData -> {
            LocalDate d = cellData.getValue().getDataCobranca();
            return new SimpleStringProperty(d != null ? d.format(formatter) : "-");
        });

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
        try {
            TransacaoRepository repo = new TransacaoRepository();
            ResumoMensalDTO resumo = repo.obterResumoMensal(mesAtual);

            tabelaTransacoes.setItems(FXCollections.observableArrayList(resumo.getTransacoes()));
            sobraDesteMes = resumo.getSobra();
            atualizarResumoMensal(resumo.getTotalDespesas(), resumo.getTotalReembolsos());

        } catch (SQLException e) {
            AlertHelper.showError("Erro ao carregar transações", e.getMessage());
        }
    }

    private void carregarCardsMetasDashboard() {
        boxCardsMetas.getChildren().clear();
        MetaRepository repo = new MetaRepository();

        for (Meta m : repo.listarTodas()) {
            BigDecimal alvo = m.getValorAlvo() != null ? m.getValorAlvo() : BigDecimal.ZERO;
            BigDecimal acumulado = m.getValorAtual() != null ? m.getValorAtual() : BigDecimal.ZERO;

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
            itemMeta.setMinWidth(220);
            itemMeta.setPrefWidth(220);
            itemMeta.setStyle("-fx-background-color: #1a1b1e; -fx-padding: 10 14; -fx-background-radius: 8; -fx-border-color: #2d3035; -fx-border-radius: 8;");

            ProgressIndicator ring = new ProgressIndicator(Math.min(fracao, 1.0));
            ring.setPrefSize(42, 42);
            ring.setMinSize(42, 42);
            ring.setStyle(fracao >= 1.0 ? "-fx-progress-color: #00c853;" : "-fx-progress-color: #3574f0;");

            VBox info = new VBox(2);

            HBox linhaNome = new HBox(6);
            linhaNome.setAlignment(Pos.CENTER_LEFT);

            Label lblTitulo = new Label(m.getDescricao());
            lblTitulo.setStyle("-fx-font-weight: bold; -fx-text-fill: #ffffff; -fx-font-size: 13px;");

            Label lblPct = new Label("(" + pct + "%)");
            lblPct.setStyle("-fx-font-weight: bold; -fx-text-fill: #3574f0; -fx-font-size: 11px;");

            linhaNome.getChildren().addAll(lblTitulo, lblPct);

            Label lblAlvo = new Label("Meta: " + formatarMoeda(alvo));
            lblAlvo.setStyle("-fx-font-size: 10px; -fx-text-fill: #9da5b4;");

            Label lblGuardado = new Label("Guardado: " + formatarMoeda(acumulado));
            lblGuardado.setStyle("-fx-font-size: 10px; -fx-text-fill: #00c853;");

            Label lblFalta = new Label("Falta: " + formatarMoeda(falta));
            lblFalta.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #ffb74d;");

            info.getChildren().addAll(linhaNome, lblAlvo, lblGuardado, lblFalta);
            itemMeta.getChildren().addAll(ring, info);
            boxCardsMetas.getChildren().add(itemMeta);
        }
    }

    private VBox criarCardVisual(String titulo, BigDecimal valor, String corHex) {
        VBox card = new VBox(4);
        card.setStyle("-fx-background-color: #212225; -fx-padding: 10 16; -fx-background-radius: 6; -fx-border-color: #37393e; -fx-border-radius: 6;");
        card.setAlignment(Pos.CENTER);
        card.setMinWidth(160);

        Label lblNome = new Label(titulo);
        lblNome.setStyle("-fx-font-size: 11px; -fx-text-fill: #9da5b4; -fx-font-weight: bold;");

        Label lblTotal = new Label(formatarMoeda(valor));
        lblTotal.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: " + corHex + ";");

        card.getChildren().addAll(lblNome, lblTotal);
        return card;
    }

    private void carregarGastosCategorias() {
        boxGastosCategorias.getChildren().clear();
        try {
            List<GastoAgrupadoDTO> gastos = new CategoriaRepository().obterGastosPorMes(mesAtual);
            for (GastoAgrupadoDTO item : gastos) {
                VBox card = criarCardVisual(item.getNome(), item.getTotal(), "#00c853");
                boxGastosCategorias.getChildren().add(card);
            }
        } catch (SQLException e) {
            AlertHelper.showError("Erro ao carregar gastos por categoria", e.getMessage());
        }
    }

    private void carregarFaturasCartoes() {
        boxFaturasCartoes.getChildren().clear();
        try {
            List<GastoAgrupadoDTO> faturas = new CartaoRepository().obterFaturasPorMes(mesAtual);
            for (GastoAgrupadoDTO item : faturas) {
                VBox card = criarCardVisual(item.getNome(), item.getTotal(), "#ffb74d");
                boxFaturasCartoes.getChildren().add(card);
            }
        } catch (SQLException e) {
            AlertHelper.showError("Erro ao carregar faturas dos cartões", e.getMessage());
        }
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

        InvestimentoService invService = new InvestimentoService();
        List<Meta> metas = invService.listarMetas();

        if (metas.isEmpty()) {
            AlertHelper.showWarning("Cadastre pelo menos 1 Meta na aba 'Metas' antes de investir a sobra.");
            return;
        }

        ChoiceDialog<Meta> dialog = new ChoiceDialog<>(metas.get(0), metas);
        dialog.setTitle("Investir Sobra do Mês");
        dialog.setHeaderText("Escolha a Meta de Destino");
        dialog.setContentText("Destinar " + formatarMoeda(sobraDesteMes) + " para:");

        dialog.showAndWait().ifPresent(metaEscolhida -> {
            try {
                invService.realizarInvestimento(metaEscolhida, sobraDesteMes, mesAtual);

                carregarDadosMetas();
                atualizarInterface();
                AlertHelper.showInformation("Sucesso", "Aporte de " + formatarMoeda(sobraDesteMes) + " realizado com sucesso!");

            } catch (IllegalStateException e) {
                AlertHelper.showWarning(e.getMessage());
            } catch (Exception e) {
                AlertHelper.showError("Erro ao realizar investimento", e.getMessage());
            }
        });
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
            AlertHelper.showError("Erro ao abrir formulário", e.getMessage());
        }
    }

    @FXML
    public void editarTransacao() {
        Transacao selecionada = tabelaTransacoes.getSelectionModel().getSelectedItem();

        if (selecionada == null) {
            AlertHelper.showWarning("Selecione uma transação na tabela para editar.");
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
            AlertHelper.showError("Erro ao editar transação", e.getMessage());
        }
    }

    @FXML
    public void excluirTransacao() {
        Transacao selecionada = tabelaTransacoes.getSelectionModel().getSelectedItem();

        if (selecionada == null) {
            AlertHelper.showWarning("Selecione uma transação na tabela para excluir.");
            return;
        }

        if (AlertHelper.showConfirmation("Excluir", "Excluir transação: " + selecionada.getDescricao() + "?")) {
            try {
                TransacaoRepository repo = new TransacaoRepository();
                repo.excluir(selecionada.getIdTransacao());
                atualizarInterface();
            } catch (Exception e) {
                AlertHelper.showError("Não foi possível excluir a transação", e.getMessage());
            }
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
            AlertHelper.showError("Erro ao salvar categoria", e.getMessage());
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
            AlertHelper.showError("Erro ao excluir categoria", e.getMessage());
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
                    BigDecimal acumulado = meta.getValorAtual() != null ? meta.getValorAtual() : BigDecimal.ZERO;

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
            AlertHelper.showError("Erro ao salvar meta", e.getMessage());
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
            AlertHelper.showError("Erro ao excluir meta", e.getMessage());
        }
    }

    @FXML
    public void gerarRelatorioReembolsos() {
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Salvar Relatório de Reembolsos");
        fileChooser.setInitialFileName("Relatorio_Reembolsos_" + mesAtual.format(DateTimeFormatter.ofPattern("MM_yyyy")) + ".pdf");
        fileChooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("Documento PDF", "*.pdf"));

        java.io.File file = fileChooser.showSaveDialog(lblSobra.getScene().getWindow());

        if (file != null) {
            try {
                RelatorioPdfService pdfService = new RelatorioPdfService();
                pdfService.gerarRelatorio(file, mesAtual, lblMesAno.getText());

                AlertHelper.showInformation("Sucesso", "Relatório PDF gerado com sucesso em:\n" + file.getAbsolutePath());
            } catch (IllegalStateException e) {
                AlertHelper.showInformation("Relatório Vazio", e.getMessage());
            } catch (Exception e) {
                AlertHelper.showError("Erro ao gerar arquivo PDF", e.getMessage());
            }
        }
    }

    private static final java.text.NumberFormat FMT_MOEDA = java.text.NumberFormat.getCurrencyInstance(new java.util.Locale("pt", "BR"));

    private String formatarMoeda(BigDecimal valor) {
        if (valor == null) valor = BigDecimal.ZERO;
        return FMT_MOEDA.format(valor);
    }
}