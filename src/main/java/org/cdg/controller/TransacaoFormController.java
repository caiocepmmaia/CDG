package org.cdg.controller;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.cdg.model.*;
import org.cdg.repository.CartaoRepository;
import org.cdg.repository.CategoriaRepository;
import org.cdg.service.CartaoService;
import org.cdg.service.CategoriaService;
import org.cdg.service.TransacaoService;
import org.cdg.util.AlertHelper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class TransacaoFormController {

    @FXML private ComboBox<String> cbTipo;
    @FXML private ComboBox<Cartao> cbCartao;
    @FXML private ComboBox<Categoria> cbCategoria;
    @FXML private TextField txtDescricao;
    @FXML private TextField txtValor;
    @FXML private TextField txtData;
    @FXML private TextField txtParcelas;
    @FXML private CheckBox chkReembolsavel;

    private Transacao transacaoParaEditar = null;
    private final TransacaoService transacaoService = new TransacaoService(); // <-- Instância do Service

    private final CartaoService cartaoService = new CartaoService();
    private final CategoriaService categoriaService = new CategoriaService();

    @FXML
    public void initialize() {
        cbTipo.getSelectionModel().select("DESPESA");

        try {
            // Agora usa os Services em vez de instanciar os Repositories diretamente
            cbCartao.getItems().addAll(cartaoService.listarTodos());
            if (!cbCartao.getItems().isEmpty()) cbCartao.getSelectionModel().selectFirst();

            cbCategoria.getItems().addAll(categoriaService.listarTodas());
            if (!cbCategoria.getItems().isEmpty()) cbCategoria.getSelectionModel().selectFirst();
        } catch (Exception e) {
            AlertHelper.showError("Falha ao carregar opções", "Não foi possível carregar cartões e categorias:\n" + e.getMessage());
        }

        txtData.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
    }

    @FXML
    public void salvarTransacao() {
        try {
            // 1. Leitura e conversão dos dados visuais (UI)
            String valorTexto = txtValor.getText().replace(",", ".").trim();
            BigDecimal valorTotal = new BigDecimal(valorTexto);

            String textoData = txtData.getText().trim();
            if (textoData.matches("\\d{2}/\\d{2}/\\d{2}")) {
                textoData = textoData.substring(0, 6) + "20" + textoData.substring(6);
            }
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            LocalDate dataInicial = LocalDate.parse(textoData, formatter);

            int totalParcelas = 1;
            if (transacaoParaEditar == null) {
                try {
                    totalParcelas = Integer.parseInt(txtParcelas.getText());
                } catch (NumberFormatException e) {
                    totalParcelas = 1;
                }
            }

            // 2. Passa tudo para o Service processar e guardar
            transacaoService.processarESalvar(
                    transacaoParaEditar,
                    cbTipo.getValue(),
                    cbCartao.getValue(),
                    cbCategoria.getValue(),
                    txtDescricao.getText(),
                    valorTotal,
                    dataInicial,
                    totalParcelas,
                    chkReembolsavel.isSelected()
            );

            // 3. Fecha a janela após o sucesso
            Stage stage = (Stage) txtDescricao.getScene().getWindow();
            stage.close();

        } catch (Exception e) {
            AlertHelper.showError("Falha ao salvar", "Verifique se preencheu os campos corretamente.\n" + e.getMessage());
        }
    }

    public void setTransacaoParaEditar(Transacao t) {
        this.transacaoParaEditar = t;
        if (t != null) {
            cbTipo.setValue(t.getTipo());
            cbCartao.setValue(t.getCartao());
            cbCategoria.setValue(t.getCategoria());
            txtDescricao.setText(t.getDescricao());
            txtValor.setText(t.getValor().toString());

            LocalDate dataExibir = t.getDataRegisto() != null ? t.getDataRegisto() : t.getDataCobranca();
            txtData.setText(dataExibir.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));

            txtParcelas.setText(String.valueOf(t.getTotalParcelas()));
            txtParcelas.setDisable(true);
            chkReembolsavel.setSelected(t.isReembolsavel());
        }
    }
}