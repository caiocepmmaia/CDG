package org.cdg.controller;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import org.cdg.model.Cartao;
import org.cdg.service.CartaoService;
import org.cdg.util.AlertHelper;

public class CartaoController {

    @FXML private TextField txtNomeCartao;
    @FXML private TextField txtDiaVencimento;
    @FXML private TextField txtDiaFechamento;

    @FXML private TableView<Cartao> tabelaCartoes;
    @FXML private TableColumn<Cartao, String> colCartaoNome;
    @FXML private TableColumn<Cartao, Integer> colCartaoVencimento;
    @FXML private TableColumn<Cartao, Integer> colCartaoFechamento;

    private Cartao cartaoParaEditar = null;
    private final CartaoService cartaoService = new CartaoService();

    @FXML
    public void initialize() {
        configurarColunas();
        carregarDados();

        tabelaCartoes.getSelectionModel().selectedItemProperty().addListener((obs, antigo, novo) -> {
            if (novo != null) {
                cartaoParaEditar = novo;
                txtNomeCartao.setText(novo.getNome());
                txtDiaVencimento.setText(String.valueOf(novo.getDiaVencimento()));
                txtDiaFechamento.setText(String.valueOf(novo.getDiaFechamento()));
            }
        });
    }

    private void configurarColunas() {
        colCartaoNome.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getNome()));
        colCartaoVencimento.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getDiaVencimento()));
        colCartaoFechamento.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getDiaFechamento()));
    }

    public void carregarDados() {
        tabelaCartoes.setItems(FXCollections.observableArrayList(cartaoService.listarTodos()));
    }

    @FXML
    public void salvarCartao() {
        try {
            // Delega a validação e a gravação para o Service
            cartaoService.salvar(
                    cartaoParaEditar,
                    txtNomeCartao.getText(),
                    txtDiaVencimento.getText(),
                    txtDiaFechamento.getText()
            );

            limparFormulario();
            carregarDados();
            AlertHelper.showInformation("Sucesso", "Cartão salvo com sucesso!");
        } catch (IllegalArgumentException e) {
            // Erros de validação de regra de negócio (ex: dias inválidos)
            AlertHelper.showWarning(e.getMessage());
        } catch (Exception e) {
            // Erros inesperados de base de dados
            AlertHelper.showError("Erro ao salvar cartão", e.getMessage());
        }
    }

    @FXML
    public void excluirCartao() {
        Cartao selecionado = tabelaCartoes.getSelectionModel().getSelectedItem();
        if (selecionado == null) {
            AlertHelper.showWarning("Selecione um cartão para excluir.");
            return;
        }

        if (AlertHelper.showConfirmation("Excluir Cartão", "Deseja realmente excluir o cartão " + selecionado.getNome() + "?")) {
            try {
                cartaoService.excluir(selecionado.getIdCartao());
                limparFormulario();
                carregarDados();
            } catch (Exception e) {
                AlertHelper.showError("Erro ao excluir cartão", e.getMessage());
            }
        }
    }

    private void limparFormulario() {
        txtNomeCartao.clear();
        txtDiaVencimento.clear();
        txtDiaFechamento.clear();
        cartaoParaEditar = null;
    }
}