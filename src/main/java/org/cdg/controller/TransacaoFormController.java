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
import org.cdg.util.AlertHelper;

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

        try {
            CartaoRepository cartaoRepo = new CartaoRepository();
            cbCartao.getItems().addAll(cartaoRepo.listarTodos());
            if (!cbCartao.getItems().isEmpty()) cbCartao.getSelectionModel().selectFirst();

            CategoriaRepository catRepo = new CategoriaRepository();
            cbCategoria.getItems().addAll(catRepo.listarTodas());
            if (!cbCategoria.getItems().isEmpty()) cbCategoria.getSelectionModel().selectFirst();
        } catch (Exception e) {
            AlertHelper.showError("Falha ao carregar opções", "Não foi possível carregar cartões e categorias:\n" + e.getMessage());
        }

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
                // MODO EDIÇÃO: Atualiza o registo existente recalculando a cobrança com base na nova data de compra
                LocalDate dataCobrancaEditada = dataInicial;
                if (cartaoSelecionado != null && cartaoSelecionado.getDiaFechamento() > 0) {
                    int diaCompra = dataInicial.getDayOfMonth();
                    int diaFechamento = cartaoSelecionado.getDiaFechamento();
                    int diaVencimento = cartaoSelecionado.getDiaVencimento();
                    LocalDate mesFatura = dataInicial;

                    if (diaCompra > diaFechamento) {
                        mesFatura = mesFatura.plusMonths(1);
                    }
                    if (diaVencimento < diaFechamento) {
                        mesFatura = mesFatura.plusMonths(1);
                    }
                    int diaAjustado = Math.min(diaVencimento, mesFatura.lengthOfMonth());
                    dataCobrancaEditada = mesFatura.withDayOfMonth(diaAjustado);
                }

                transacaoParaEditar.setDescricao(descricaoOriginal);
                transacaoParaEditar.setValor(valorTotal);
                transacaoParaEditar.setDataRegisto(dataInicial); // <--- Atualiza a data da compra
                transacaoParaEditar.setDataCobranca(dataCobrancaEditada); // <--- Atualiza a cobrança calculada
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

                // --- DIVISÃO FINANCEIRA EXATA ---
                BigDecimal valorParcelaBase = valorTotal.divide(BigDecimal.valueOf(totalParcelas), 2, RoundingMode.DOWN);
                BigDecimal somaBase = valorParcelaBase.multiply(BigDecimal.valueOf(totalParcelas));
                BigDecimal diferencaCentavos = valorTotal.subtract(somaBase);

                Conta contaBase = Conta.builder().idConta(1).build();
                Titular titularBase = Titular.builder().idTitular(1).build();

                for (int i = 1; i <= totalParcelas; i++) {
                    String descricaoFormatada = totalParcelas > 1 ? descricaoOriginal + " (" + i + "/" + totalParcelas + ")" : descricaoOriginal;

                    // Data da compra desta parcela específica (somando meses para parcelamentos)
                    LocalDate dataCompraParcela = dataInicial.plusMonths(i - 1);
                    LocalDate dataCobrancaFinal = dataCompraParcela;

                    // --- CÁLCULO AUTOMÁTICO DE FATURA COM BASE NO DIA DE FECHAMENTO E VENCIMENTO ---
                    if (cartaoSelecionado != null && cartaoSelecionado.getDiaFechamento() > 0) {
                        int diaCompra = dataCompraParcela.getDayOfMonth();
                        int diaFechamento = cartaoSelecionado.getDiaFechamento();
                        int diaVencimento = cartaoSelecionado.getDiaVencimento();

                        LocalDate mesFatura = dataCompraParcela;

                        // REGRA 1: Se a compra foi feita após o fechamento, passa para o ciclo seguinte
                        if (diaCompra > diaFechamento) {
                            mesFatura = mesFatura.plusMonths(1);
                        }

                        // REGRA 2: Se o vencimento é MENOR que o fechamento, o pagamento ocorre no mês seguinte
                        if (diaVencimento < diaFechamento) {
                            mesFatura = mesFatura.plusMonths(1);
                        }

                        int diaAjustado = Math.min(diaVencimento, mesFatura.lengthOfMonth());
                        dataCobrancaFinal = mesFatura.withDayOfMonth(diaAjustado);
                    }

                    BigDecimal valorEstaParcela = (i == 1) ? valorParcelaBase.add(diferencaCentavos) : valorParcelaBase;

                    Transacao parcela = Transacao.builder()
                            .descricao(descricaoFormatada)
                            .valor(valorEstaParcela)
                            .dataRegisto(dataCompraParcela) // <--- GRAVA A DATA DA COMPRA CORRETA
                            .dataCobranca(dataCobrancaFinal) // <--- GRAVA A DATA DE COBRANÇA CALCULADA
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

            // Exibe a Data da Compra (dataRegisto) no formulário ao editar, se disponível
            LocalDate dataExibir = t.getDataRegisto() != null ? t.getDataRegisto() : t.getDataCobranca();
            txtData.setText(dataExibir.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));

            txtParcelas.setText(String.valueOf(t.getTotalParcelas()));
            txtParcelas.setDisable(true);
            chkReembolsavel.setSelected(t.isReembolsavel());
        }
    }
}