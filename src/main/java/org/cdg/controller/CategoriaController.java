package org.cdg.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import org.cdg.model.Categoria;
import org.cdg.service.CategoriaService;
import org.cdg.util.AlertHelper;

public class CategoriaController {

    @FXML private TextField txtNomeCategoria;
    @FXML private TableView<Categoria> tabelaCategorias;
    @FXML private TableColumn<Categoria, String> colCategoriaNome;

    private Categoria categoriaParaEditar = null;
    private final CategoriaService categoriaService = new CategoriaService();

    @FXML
    public void initialize() {
        configurarColunas();
        carregarDados();

        tabelaCategorias.getSelectionModel().selectedItemProperty().addListener((obs, antigo, novo) -> {
            if (novo != null) {
                categoriaParaEditar = novo;
                txtNomeCategoria.setText(novo.getNome());
            }
        });
    }

    private void configurarColunas() {
        colCategoriaNome.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getNome()));
    }

    public void carregarDados() {
        tabelaCategorias.setItems(FXCollections.observableArrayList(categoriaService.listarTodas()));
    }

    @FXML
    public void salvarCategoria() {
        try {
            categoriaService.salvar(categoriaParaEditar, txtNomeCategoria.getText());

            limparFormulario();
            carregarDados();
            AlertHelper.showInformation("Sucesso", "Categoria salva com sucesso!");
        } catch (IllegalArgumentException e) {
            AlertHelper.showWarning(e.getMessage());
        } catch (Exception e) {
            AlertHelper.showError("Erro ao salvar categoria", e.getMessage());
        }
    }

    @FXML
    public void excluirCategoria() {
        Categoria selecionada = tabelaCategorias.getSelectionModel().getSelectedItem();
        if (selecionada == null) {
            AlertHelper.showWarning("Selecione uma categoria para excluir.");
            return;
        }

        if (AlertHelper.showConfirmation("Excluir Categoria", "Deseja realmente excluir a categoria '" + selecionada.getNome() + "'?")) {
            try {
                categoriaService.excluir(selecionada.getIdCategoria());
                limparFormulario();
                carregarDados();
            } catch (Exception e) {
                AlertHelper.showError("Erro ao excluir categoria", e.getMessage());
            }
        }
    }

    private void limparFormulario() {
        txtNomeCategoria.clear();
        categoriaParaEditar = null;
    }
}