package org.cdg.service;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.cdg.model.Transacao;
import org.cdg.repository.TransacaoRepository;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class RelatorioPdfService {

    public void gerarRelatorio(File file, LocalDate mesAtual, String textoMesAno) throws Exception {
        TransacaoRepository repo = new TransacaoRepository();
        List<Transacao> reembolsaveis = repo.listarReembolsaveisPorMes(mesAtual);

        if (reembolsaveis.isEmpty()) {
            // Lançamos uma exceção específica para o Controller saber que estava vazio
            throw new IllegalStateException("Nenhuma despesa reembolsável encontrada para o mês " + textoMesAno);
        }

        BigDecimal totalReembolso = BigDecimal.ZERO;
        for (Transacao t : reembolsaveis) {
            if (t.getValor() != null) {
                totalReembolso = totalReembolso.add(t.getValor());
            }
        }

        Document document = new Document();
        PdfWriter.getInstance(document, new FileOutputStream(file));

        document.open();

        Font fontTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
        Font fontSub = FontFactory.getFont(FontFactory.HELVETICA, 12);
        Font fontHeaderTab = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
        Font fontCorpo = FontFactory.getFont(FontFactory.HELVETICA, 10);

        Paragraph pTitulo = new Paragraph("Relatório de Despesas a Reembolsar", fontTitulo);
        pTitulo.setAlignment(Element.ALIGN_CENTER);
        document.add(pTitulo);

        Paragraph pData = new Paragraph("Período: " + textoMesAno + " | Gerado em: " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), fontSub);
        pData.setAlignment(Element.ALIGN_CENTER);
        pData.setSpacingAfter(20);
        document.add(pData);

        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{2f, 4f, 2.5f, 2.5f, 2.5f});

        String[] colunas = {"Data", "Descrição", "Cartão", "Categoria", "Valor (R$)"};
        for (String col : colunas) {
            PdfPCell cell = new PdfPCell(new Phrase(col, fontHeaderTab));
            cell.setBackgroundColor(Color.LIGHT_GRAY);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(6);
            table.addCell(cell);
        }

        DateTimeFormatter fmtData = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        for (Transacao t : reembolsaveis) {
            table.addCell(new Phrase(t.getDataCobranca().format(fmtData), fontCorpo));
            table.addCell(new Phrase(t.getDescricao(), fontCorpo));
            table.addCell(new Phrase(t.getCartao() != null ? t.getCartao().getNome() : "-", fontCorpo));
            table.addCell(new Phrase(t.getCategoria() != null ? t.getCategoria().getNome() : "-", fontCorpo));

            PdfPCell cVal = new PdfPCell(new Phrase(String.format("R$ %,.2f", t.getValor()), fontCorpo));
            cVal.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(cVal);
        }

        document.add(table);

        Paragraph pTotal = new Paragraph(String.format("Total a Reembolsar: R$ %,.2f", totalReembolso), fontTitulo);
        pTotal.setAlignment(Element.ALIGN_RIGHT);
        pTotal.setSpacingBefore(15);
        document.add(pTotal);

        document.close();
    }
}