package org.cdg.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.cdg.model.*;
import org.cdg.service.DashboardService;
import org.cdg.service.InvestimentoService;
import org.cdg.service.RelatorioPdfService;
import org.cdg.service.TransacaoService;
import org.cdg.util.AlertHelper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class DashboardController {

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

    private LocalDate mesAtual = LocalDate.now();
    private BigDecimal sobraDesteMes = BigDecimal.ZERO;

    // Injeção limpa dos serviços
    private final DashboardService dashboardService = new DashboardService();
    private final TransacaoService transacaoService = new TransacaoService();

    @FXML
    public void initialize() {
        btnInvestirSobra.managedProperty().bind(btnInvestirSobra.visibleProperty());
        configurarColunas();
        atualizarInterface();
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

        try {
            // O Controller apenas pede o ViewModel pronto ao Service
            DashboardViewModel vm = dashboardService.carregarDadosDashboard(mesAtual);

            sobraDesteMes = vm.getSobra();
            tabelaTransacoes.setItems(FXCollections.observableArrayList(vm.getTransacoes()));

            atualizarResumoVisual(vm);
            renderizarMetas(vm.getMetas());
            renderizarCardsAgrupados(boxFaturasCartoes, vm.getFaturasCartoes(), "#ffb74d");
            renderizarCardsAgrupados(boxGastosCategorias, vm.getGastosCategorias(), "#00c853");

        } catch (Exception e) {
            AlertHelper.showError("Erro ao carregar dashboard", e.getMessage());
        }
    }

    private void atualizarResumoVisual(DashboardViewModel vm) {
        lblDeficit.setText(formatarMoeda(vm.getTotalDespesas()));
        lblReembolso.setText(formatarMoeda(vm.getTotalReembolsos()));
        lblSobra.setText(formatarMoeda(vm.getSobra()));

        if (vm.getSobra().compareTo(BigDecimal.ZERO) < 0) {
            lblSobra.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #f75454;");
            btnInvestirSobra.setVisible(false);
        } else {
            lblSobra.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #00c853;");
            btnInvestirSobra.setVisible(vm.isTemSobraPositiva());
        }
    }

    private void renderizarMetas(List<Meta> metas) {
        boxCardsMetas.getChildren().clear();
        for (Meta m : metas) {
            BigDecimal alvo = m.getValorAlvo() != null ? m.getValorAlvo() : BigDecimal.ZERO;
            BigDecimal acumulado = m.getValorAtual() != null ? m.getValorAtual() : BigDecimal.ZERO;

            double fracao = alvo.compareTo(BigDecimal.ZERO) > 0 ?
                    acumulado.divide(alvo, 4, RoundingMode.HALF_UP).doubleValue() : 0.0;

            BigDecimal falta = alvo.subtract(acumulado);
            if (falta.compareTo(BigDecimal.ZERO) < 0) falta = BigDecimal.ZERO;

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

    private void renderizarCardsAgrupados(HBox containerBox, List<GastoAgrupadoDTO> itens, String corHex) {
        containerBox.getChildren().clear();
        for (GastoAgrupadoDTO item : itens) {
            VBox card = new VBox(4);
            card.setStyle("-fx-background-color: #212225; -fx-padding: 10 16; -fx-background-radius: 6; -fx-border-color: #37393e; -fx-border-radius: 6;");
            card.setAlignment(Pos.CENTER);
            card.setMinWidth(160);

            Label lblNome = new Label(item.getNome());
            lblNome.setStyle("-fx-font-size: 11px; -fx-text-fill: #9da5b4; -fx-font-weight: bold;");

            Label lblTotal = new Label(formatarMoeda(item.getTotal()));
            lblTotal.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: " + corHex + ";");

            card.getChildren().addAll(lblNome, lblTotal);
            containerBox.getChildren().add(card);
        }
    }

    private void configurarColunas() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        colDescricao.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getDescricao()));
        colDataCompra.setCellValueFactory(cell -> {
            LocalDate d = cell.getValue().getDataRegisto();
            return new SimpleStringProperty(d != null ? d.format(formatter) : "-");
        });
        colData.setCellValueFactory(cell -> {
            LocalDate d = cell.getValue().getDataCobranca();
            return new SimpleStringProperty(d != null ? d.format(formatter) : "-");
        });
        colCartao.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getCartao() != null ? cell.getValue().getCartao().getNome() : "-"));
        colCategoria.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getCategoria() != null ? cell.getValue().getCategoria().getNome() : "-"));
        colValor.setCellValueFactory(cell -> {
            Transacao t = cell.getValue();
            boolean isReceita = "RECEITA".equalsIgnoreCase(t.getTipo());
            return new SimpleStringProperty((isReceita ? "+ " : "- ") + formatarMoeda(t.getValor()));
        });
        colStatus.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getStatus()));
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
                atualizarInterface();
                AlertHelper.showInformation("Sucesso", "Aporte de " + formatarMoeda(sobraDesteMes) + " realizado com sucesso!");
            } catch (Exception e) {
                AlertHelper.showError("Erro ao realizar investimento", e.getMessage());
            }
        });
    }

    @FXML
    public void abrirFormulario() {
        abrirJanelaModal("/view/transacao_form.fxml", "Nova Transação", null);
    }

    @FXML
    public void editarTransacao() {
        Transacao selecionada = tabelaTransacoes.getSelectionModel().getSelectedItem();
        if (selecionada == null) {
            AlertHelper.showWarning("Selecione uma transação na tabela para editar.");
            return;
        }
        abrirJanelaModal("/view/transacao_form.fxml", "Editar Transação", selecionada);
    }

    private void abrirJanelaModal(String fxmlPath, String titulo, Transacao transacaoParaEditar) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource(fxmlPath));
            javafx.scene.Parent root = loader.load();

            if (transacaoParaEditar != null && loader.getController() instanceof TransacaoFormController) {
                ((TransacaoFormController) loader.getController()).setTransacaoParaEditar(transacaoParaEditar);
            }

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle(titulo);
            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            scene.getStylesheets().add(getClass().getResource("/view/style.css").toExternalForm());
            stage.setScene(scene);
            stage.initOwner(tabelaTransacoes.getScene().getWindow());
            stage.initModality(javafx.stage.Modality.WINDOW_MODAL);
            stage.showAndWait();

            atualizarInterface();
        } catch (Exception e) {
            AlertHelper.showError("Erro", e.getMessage());
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
                transacaoService.excluir(selecionada.getIdTransacao());
                atualizarInterface();
            } catch (Exception e) {
                AlertHelper.showError("Não foi possível excluir", e.getMessage());
            }
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
                new RelatorioPdfService().gerarRelatorio(file, mesAtual, lblMesAno.getText());
                AlertHelper.showInformation("Sucesso", "Relatório PDF gerado com sucesso em:\n" + file.getAbsolutePath());
            } catch (Exception e) {
                AlertHelper.showError("Erro ao gerar PDF", e.getMessage());
            }
        }
    }

    private static final java.text.NumberFormat FMT_MOEDA = java.text.NumberFormat.getCurrencyInstance(new java.util.Locale("pt", "BR"));
    private String formatarMoeda(BigDecimal valor) {
        if (valor == null) valor = BigDecimal.ZERO;
        return FMT_MOEDA.format(valor);
    }
}