package org.cdg.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.cdg.model.*;
import org.cdg.repository.CartaoRepository;
import org.cdg.repository.CategoriaRepository;
import org.cdg.repository.TransacaoRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

    @FXML
    public void initialize() {
        cbTipo.getSelectionModel().select("DESPESA");

        CartaoRepository cartaoRepo = new CartaoRepository();
        cbCartao.getItems().addAll(cartaoRepo.listarTodos());
        if (!cbCartao.getItems().isEmpty()) cbCartao.getSelectionModel().selectFirst();

        CategoriaRepository catRepo = new CategoriaRepository();
        cbCategoria.getItems().addAll(catRepo.listarTodas());
        if (!cbCategoria.getItems().isEmpty()) cbCategoria.getSelectionModel().selectFirst();

        txtData.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
    }

    @FXML
    public void salvarTransacao() {
        try {
            String tipoSelecionado = cbTipo.getValue();
            Cartao cartaoSelecionado = cbCartao.getValue();
            Categoria categoriaSelecionada = cbCategoria.getValue();

            if (cartaoSelecionado == null) throw new Exception("Selecione um cartão.");
            if (categoriaSelecionada == null) throw new Exception("Selecione uma categoria.");

            String descricaoOriginal = txtDescricao.getText();

            // Leitura exata do valor com BigDecimal
            String valorTexto = txtValor.getText().replace(",", ".").trim();
            BigDecimal valorTotal = new BigDecimal(valorTexto);

            LocalDate dataInicial;
            try {
                String textoData = txtData.getText().trim();

                if (textoData.matches("\\d{2}/\\d{2}/\\d{2}")) {
                    textoData = textoData.substring(0, 6) + "20" + textoData.substring(6);
                }

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                dataInicial = LocalDate.parse(textoData, formatter);
            } catch (Exception e) {
                throw new Exception("Formato de data inválido. Use DD/MM/AAAA (ex: 26/09/2026 ou 26/09/26).");
            }

            TransacaoRepository repo = new TransacaoRepository();

            if (transacaoParaEditar != null) {
                // MODO EDIÇÃO: Atualiza o registo existente sem criar novas parcelas
                transacaoParaEditar.setDescricao(descricaoOriginal);
                transacaoParaEditar.setValor(valorTotal);
                transacaoParaEditar.setDataCobranca(dataInicial);
                transacaoParaEditar.setTipo(tipoSelecionado);
                transacaoParaEditar.setCartao(cartaoSelecionado);
                transacaoParaEditar.setCategoria(categoriaSelecionada);
                transacaoParaEditar.setReembolsavel(chkReembolsavel.isSelected());

                repo.atualizar(transacaoParaEditar);
            } else {
                // MODO CRIAÇÃO: Gera as parcelas normalmente
                int totalParcelas = 1;
                try {
                    totalParcelas = Integer.parseInt(txtParcelas.getText());
                    if (totalParcelas < 1) totalParcelas = 1;
                } catch (NumberFormatException e) {
                    totalParcelas = 1;
                }

                // Divisão monetária exata com BigDecimal e arredondamento HALF_UP (2 casas decimais)
                BigDecimal valorParcela = valorTotal.divide(BigDecimal.valueOf(totalParcelas), 2, RoundingMode.HALF_UP);

                Conta contaBase = Conta.builder().idConta(1).build();
                Titular titularBase = Titular.builder().idTitular(1).build();

                for (int i = 1; i <= totalParcelas; i++) {
                    String descricaoFormatada = totalParcelas > 1 ? descricaoOriginal + " (" + i + "/" + totalParcelas + ")" : descricaoOriginal;
                    LocalDate dataParcela = dataInicial.plusMonths(i - 1);

                    Transacao parcela = Transacao.builder()
                            .descricao(descricaoFormatada)
                            .valor(valorParcela)
                            .dataRegisto(LocalDate.now())
                            .dataCobranca(dataParcela)
                            .parcelaAtual(i)
                            .totalParcelas(totalParcelas)
                            .status("PENDENTE")
                            .tipo(tipoSelecionado)
                            .cartao(cartaoSelecionado)
                            .categoria(categoriaSelecionada)
                            .reembolsavel(chkReembolsavel.isSelected())
                            .conta(contaBase)
                            .titular(titularBase)
                            .build();

                    repo.salvar(parcela);
                }
            }

            Stage stage = (Stage) txtDescricao.getScene().getWindow();
            stage.close();

        } catch (Exception e) {
            Alert alerta = new Alert(Alert.AlertType.ERROR);
            alerta.setTitle("Erro");
            alerta.setHeaderText("Falha ao salvar");
            alerta.setContentText("Verifique se preencheu os campos corretamente.\n" + e.getMessage());
            alerta.showAndWait();
        }
    }

    public void setTransacaoParaEditar(Transacao t) {
        this.transacaoParaEditar = t;
        if (t != null) {
            cbTipo.setValue(t.getTipo());
            cbCartao.setValue(t.getCartao());
            cbCategoria.setValue(t.getCategoria());
            txtDescricao.setText(t.getDescricao());
            txtValor.setText(t.getValor().toString()); // Converte BigDecimal para String no campo
            txtData.setText(t.getDataCobranca().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            txtParcelas.setText(String.valueOf(t.getTotalParcelas()));
            txtParcelas.setDisable(true); // Desativa o campo de parcelas na edição
            chkReembolsavel.setSelected(t.isReembolsavel());
        }
    }
}