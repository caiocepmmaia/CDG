package org.cdg.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import org.cdg.model.Meta;
import org.cdg.service.MetaService;
import org.cdg.util.AlertHelper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class MetaController {

    @FXML private TextField txtDescricaoMeta;
    @FXML private TextField txtValorAlvoMeta;
    @FXML private TextField txtDataLimiteMeta;

    @FXML private TableView<Meta> tabelaMetas;
    @FXML private TableColumn<Meta, String> colMetaDescricao;
    @FXML private TableColumn<Meta, String> colMetaAlvo;
    @FXML private TableColumn<Meta, String> colMetaProgresso;

    private Meta metaParaEditar = null;
    private final MetaService metaService = new MetaService();

    @FXML
    public void initialize() {
        configurarColunas();
        carregarDados();

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

    private void configurarColunas() {
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

    public void carregarDados() {
        tabelaMetas.setItems(FXCollections.observableArrayList(metaService.listarTodas()));
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

            metaService.salvar(metaParaEditar, desc, alvo, limite);

            limparFormulario();
            carregarDados();
            AlertHelper.showInformation("Sucesso", "Meta salva com sucesso!");
        } catch (IllegalArgumentException e) {
            AlertHelper.showWarning(e.getMessage());
        } catch (Exception e) {
            AlertHelper.showError("Erro ao salvar meta", e.getMessage());
        }
    }

    @FXML
    public void excluirMeta() {
        Meta selecionada = tabelaMetas.getSelectionModel().getSelectedItem();
        if (selecionada == null) {
            AlertHelper.showWarning("Selecione uma meta para excluir.");
            return;
        }

        if (AlertHelper.showConfirmation("Excluir Meta", "Deseja realmente excluir a meta '" + selecionada.getDescricao() + "'?")) {
            try {
                metaService.excluir(selecionada.getIdMeta());
                limparFormulario();
                carregarDados();
            } catch (Exception e) {
                AlertHelper.showError("Erro ao excluir meta", e.getMessage());
            }
        }
    }

    private void limparFormulario() {
        txtDescricaoMeta.clear();
        txtValorAlvoMeta.clear();
        txtDataLimiteMeta.clear();
        metaParaEditar = null;
    }
}