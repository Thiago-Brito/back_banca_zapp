package com.bancazapp.banca_zapp.service;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Service;

import com.bancazapp.banca_zapp.config.NotaConferenciaProperties;
import com.bancazapp.banca_zapp.entity.TipoVisita;
import com.bancazapp.banca_zapp.entity.Visita;
import com.bancazapp.banca_zapp.entity.VisitaItem;
import com.bancazapp.banca_zapp.exception.ResourceNotFoundException;
import com.bancazapp.banca_zapp.repository.VisitaRepository;

@Service
public class NotaConferenciaService {

    private static final String TITULO = "Nota de Conferência";
    private static final String TEXTO_SEM_VALOR_FISCAL = "Documento sem valor fiscal. Apenas para conferência.";
    private static final DateTimeFormatter DATA_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DecimalFormat MOEDA_FORMATTER = criarFormatterMoeda();
    private static final PDFont FONTE_PADRAO = PDType1Font.HELVETICA;
    private static final PDFont FONTE_NEGRITO = PDType1Font.HELVETICA_BOLD;
    private static final float PADDING_TEXTO = 4f;
    private static final float ESPACAMENTO_SECAO = 10f;
    private static final float ESPACAMENTO_LINHA_FORM = 18f;

    private final VisitaRepository visitaRepository;
    private final NotaConferenciaProperties properties;

    public NotaConferenciaService(VisitaRepository visitaRepository,
                                  NotaConferenciaProperties properties) {
        this.visitaRepository = visitaRepository;
        this.properties = properties;
    }

    public byte[] gerarNotaConferencia(UUID visitaId) {
        Visita visita = visitaRepository.findWithItensById(visitaId)
                .orElseThrow(() -> new ResourceNotFoundException("Visita nao encontrada: " + visitaId));
        return gerarPdf(visita);
    }

    private byte[] gerarPdf(Visita visita) {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             PdfWriter writer = new PdfWriter(document)) {

            boolean venda = visita.getTipo() == TipoVisita.VENDA;

            float margin = writer.getMargin();
            float pageWidth = writer.getPageWidth();
            float contentWidth = pageWidth - (margin * 2);

            float headerHeight = 86f;
            float headerTop = writer.getY();
            float headerBottom = headerTop - headerHeight;
            float headerRightBoxWidth = 140f;
            float headerRightBoxLeft = margin + contentWidth - headerRightBoxWidth;
            writer.drawRect(margin, headerBottom, contentWidth, headerHeight);
            writer.drawLine(headerRightBoxLeft, headerTop, headerRightBoxLeft, headerBottom);
            writer.writeCentered(TITULO, FONTE_NEGRITO, 12, headerRightBoxLeft, headerBottom + (headerHeight / 2),
                    headerRightBoxWidth, headerHeight / 2);
            writer.writeCentered("Documento sem valor fiscal", FONTE_PADRAO, 8, headerRightBoxLeft, headerBottom,
                    headerRightBoxWidth, headerHeight / 2);

            float headerTextX = margin + 8;
            float headerTextY = headerTop - 18;
            writer.writeAt(texto(properties.getNomeFantasia()).toUpperCase(Locale.ROOT), FONTE_NEGRITO, 18, headerTextX, headerTextY);
            writer.writeAt("Razao social: " + texto(properties.getRazaoSocial()), FONTE_PADRAO, 10, headerTextX, headerTextY - 18);
            writer.writeAt("CNPJ: " + texto(properties.getCnpj()), FONTE_PADRAO, 10, headerTextX, headerTextY - 32);
            writer.writeAt("Telefone: " + texto(properties.getTelefone()), FONTE_PADRAO, 10, headerTextX, headerTextY - 46);
            writer.writeAt("Endereco: " + texto(properties.getEndereco()), FONTE_PADRAO, 10, headerTextX, headerTextY - 60);
            writer.setY(headerBottom - ESPACAMENTO_SECAO);

            float dadosHeight = 88f;
            float dadosTop = writer.getY();
            float dadosBottom = dadosTop - dadosHeight;
            writer.drawRect(margin, dadosBottom, contentWidth, dadosHeight);
            writer.writeAt("Dados da visita", FONTE_NEGRITO, 11, margin + 6, dadosTop - 14);
            float linha1Y = dadosTop - 30;
            float linha2Y = linha1Y - ESPACAMENTO_LINHA_FORM;
            float linha3Y = linha2Y - ESPACAMENTO_LINHA_FORM;
            float meioX = margin + (contentWidth / 2);
            writer.drawLine(meioX, dadosTop - 22, meioX, dadosBottom + 6);

            desenharLinhaFormulario(writer, "Cliente:", texto(visita.getCliente().getNome()),
                    margin + 6, linha1Y, margin + contentWidth - 6);
            desenharLinhaFormulario(writer, "Data da visita:", visita.getDataVisita().format(DATA_FORMATTER),
                    margin + 6, linha2Y, meioX - 6);
            desenharLinhaFormulario(writer, "Tipo da visita:", formatarTipo(visita.getTipo()),
                    meioX + 6, linha2Y, margin + contentWidth - 6);

            writer.writeAt("Observacoes:", FONTE_PADRAO, 10, margin + 6, linha3Y);
            float obsBoxTop = linha3Y + 6;
            float obsBoxBottom = dadosBottom + 6;
            float obsBoxLeft = margin + 90;
            float obsBoxWidth = contentWidth - 96;
            writer.drawRect(obsBoxLeft, obsBoxBottom, obsBoxWidth, obsBoxTop - obsBoxBottom);
            List<String> obsLinhas = writer.wrapText(texto(visita.getObservacoes()), obsBoxWidth - (PADDING_TEXTO * 2),
                    FONTE_PADRAO, 10);
            float obsTextY = obsBoxTop - 12;
            for (int i = 0; i < Math.min(obsLinhas.size(), 2); i++) {
                writer.writeAt(obsLinhas.get(i), FONTE_PADRAO, 10, obsBoxLeft + PADDING_TEXTO, obsTextY - (i * 12));
            }
            writer.setY(dadosBottom - ESPACAMENTO_SECAO);

            writer.moveDown(10);
            writer.writeLine("Produtos", FONTE_NEGRITO, 12);
            writer.moveDown(2);

            float tableLeft = margin;
            float tableWidth = contentWidth;
            float xProduto = tableLeft;
            float xQuantidade;
            float xUnitario = 0f;
            float xTotal = 0f;
            float xEntregue = 0f;
            float xRetirado = 0f;
            if (venda) {
                xQuantidade = tableLeft + 260f;
                xUnitario = xQuantidade + 70f;
                xTotal = xUnitario + 90f;
            } else {
                xEntregue = tableLeft + 320f;
                xRetirado = xEntregue + 90f;
                xQuantidade = xEntregue;
            }

            float headerRowHeight = 20f;
            float rowHeight = 18f;
            float footerReserve = 110f;
            float tableTop = writer.getY() + 6;
            float tableHeaderBottom = tableTop - headerRowHeight;
            desenharLinhaCabecalhoTabela(writer, venda, tableTop, tableHeaderBottom, tableLeft, tableWidth,
                    xQuantidade, xUnitario, xTotal, xEntregue, xRetirado);
            float headerLabelY = tableTop - 14;
            writer.writeAt("Produto", FONTE_NEGRITO, 10, xProduto + PADDING_TEXTO, headerLabelY);
            if (venda) {
                writer.writeAt("Quantidade", FONTE_NEGRITO, 10, xQuantidade + PADDING_TEXTO, headerLabelY);
                writer.writeAt("Valor Unitario", FONTE_NEGRITO, 10, xUnitario + PADDING_TEXTO, headerLabelY);
                writer.writeAt("Valor Total", FONTE_NEGRITO, 10, xTotal + PADDING_TEXTO, headerLabelY);
            } else {
                writer.writeAt("Entregue", FONTE_NEGRITO, 10, xEntregue + PADDING_TEXTO, headerLabelY);
                writer.writeAt("Retirado", FONTE_NEGRITO, 10, xRetirado + PADDING_TEXTO, headerLabelY);
            }
            writer.setY(tableHeaderBottom);

            int totalItens = 0;
            BigDecimal totalValor = BigDecimal.ZERO;
            for (VisitaItem item : visita.getItens()) {
                int quantidade = calcularQuantidade(item, venda);
                totalItens += quantidade;

                BigDecimal precoUnitario = item.getProduto().getPreco();
                BigDecimal totalItem = BigDecimal.ZERO;
                if (venda) {
                    totalItem = precoUnitario.multiply(BigDecimal.valueOf(item.getVendido()));
                    totalValor = totalValor.add(totalItem);
                }

                if (!writer.hasSpace(rowHeight + footerReserve)) {
                    writer.newPage();
                    writer.writeLine("Produtos (continua)", FONTE_NEGRITO, 12);
                    writer.moveDown(2);
                    tableTop = writer.getY() + 6;
                    tableHeaderBottom = tableTop - headerRowHeight;
                    desenharLinhaCabecalhoTabela(writer, venda, tableTop, tableHeaderBottom, tableLeft, tableWidth,
                            xQuantidade, xUnitario, xTotal, xEntregue, xRetirado);
                    headerLabelY = tableTop - 14;
                    writer.writeAt("Produto", FONTE_NEGRITO, 10, xProduto + PADDING_TEXTO, headerLabelY);
                    if (venda) {
                        writer.writeAt("Quantidade", FONTE_NEGRITO, 10, xQuantidade + PADDING_TEXTO, headerLabelY);
                        writer.writeAt("Valor Unitario", FONTE_NEGRITO, 10, xUnitario + PADDING_TEXTO, headerLabelY);
                        writer.writeAt("Valor Total", FONTE_NEGRITO, 10, xTotal + PADDING_TEXTO, headerLabelY);
                    } else {
                        writer.writeAt("Entregue", FONTE_NEGRITO, 10, xEntregue + PADDING_TEXTO, headerLabelY);
                        writer.writeAt("Retirado", FONTE_NEGRITO, 10, xRetirado + PADDING_TEXTO, headerLabelY);
                    }
                    writer.setY(tableHeaderBottom);
                }

                float rowTop = writer.getY();
                float rowBottom = rowTop - rowHeight;
                desenharLinhaTabela(writer, rowTop, rowBottom, tableLeft, tableWidth, xQuantidade, xUnitario, xTotal, xEntregue, xRetirado, venda);
                float textY = rowTop - 13;
                String produtoTexto = ajustarTexto(texto(item.getProduto().getNome()), xQuantidade - xProduto - (PADDING_TEXTO * 2),
                        FONTE_PADRAO, 10);
                writer.writeAt(produtoTexto, FONTE_PADRAO, 10, xProduto + PADDING_TEXTO, textY);
                if (venda) {
                    writer.writeAt(String.valueOf(quantidade), FONTE_PADRAO, 10, xQuantidade + PADDING_TEXTO, textY);
                    writer.writeAt(formatarMoeda(precoUnitario), FONTE_PADRAO, 10, xUnitario + PADDING_TEXTO, textY);
                    writer.writeAt(formatarMoeda(totalItem), FONTE_PADRAO, 10, xTotal + PADDING_TEXTO, textY);
                } else {
                    writer.writeAt(String.valueOf(item.getEntregue()), FONTE_PADRAO, 10, xEntregue + PADDING_TEXTO, textY);
                    writer.writeAt(String.valueOf(item.getRetirado()), FONTE_PADRAO, 10, xRetirado + PADDING_TEXTO, textY);
                }
                writer.setY(rowBottom);
            }

            int linhasExtras = 6;
            for (int i = 0; i < linhasExtras; i++) {
                if (!writer.hasSpace(rowHeight + footerReserve)) {
                    writer.newPage();
                    writer.writeLine("Produtos (continua)", FONTE_NEGRITO, 12);
                    writer.moveDown(2);
                    tableTop = writer.getY() + 6;
                    tableHeaderBottom = tableTop - headerRowHeight;
                    desenharLinhaCabecalhoTabela(writer, venda, tableTop, tableHeaderBottom, tableLeft, tableWidth,
                            xQuantidade, xUnitario, xTotal, xEntregue, xRetirado);
                    headerLabelY = tableTop - 14;
                    writer.writeAt("Produto", FONTE_NEGRITO, 10, xProduto + PADDING_TEXTO, headerLabelY);
                    if (venda) {
                        writer.writeAt("Quantidade", FONTE_NEGRITO, 10, xQuantidade + PADDING_TEXTO, headerLabelY);
                        writer.writeAt("Valor Unitario", FONTE_NEGRITO, 10, xUnitario + PADDING_TEXTO, headerLabelY);
                        writer.writeAt("Valor Total", FONTE_NEGRITO, 10, xTotal + PADDING_TEXTO, headerLabelY);
                    } else {
                        writer.writeAt("Entregue", FONTE_NEGRITO, 10, xEntregue + PADDING_TEXTO, headerLabelY);
                        writer.writeAt("Retirado", FONTE_NEGRITO, 10, xRetirado + PADDING_TEXTO, headerLabelY);
                    }
                    writer.setY(tableHeaderBottom);
                }
                float rowTop = writer.getY();
                float rowBottom = rowTop - rowHeight;
                desenharLinhaTabela(writer, rowTop, rowBottom, tableLeft, tableWidth, xQuantidade, xUnitario, xTotal,
                        xEntregue, xRetirado, venda);
                writer.setY(rowBottom);
            }

            writer.moveDown(ESPACAMENTO_SECAO + 7);
            writer.writeLine("Totais", FONTE_NEGRITO, 12);
            String totalItensTexto = "Total de itens: " + totalItens;
            float totalItensX = writer.getPageWidth() - writer.getMargin() - writer.textWidth(totalItensTexto, FONTE_PADRAO, 11);
            writer.writeAt(totalItensTexto, FONTE_PADRAO, 11, totalItensX, writer.getY());
            writer.newLine();
            if (venda) {
                String valorTotalTexto = "Valor total: " + formatarMoeda(totalValor);
                float valorTotalX = writer.getPageWidth() - writer.getMargin() - writer.textWidth(valorTotalTexto, FONTE_PADRAO, 11);
                writer.writeAt(valorTotalTexto, FONTE_PADRAO, 11, valorTotalX, writer.getY());
                writer.newLine();
            }
            writer.moveDown(12);
            float assinaturaY = writer.getY();
            writer.drawLine(writer.getMargin(), assinaturaY, writer.getMargin() + 250, assinaturaY);
            writer.writeAt("Assinatura do cliente", FONTE_PADRAO, 10, writer.getMargin(), assinaturaY - 14);
            writer.moveDown(24);
            writer.writeLine(TEXTO_SEM_VALOR_FISCAL, FONTE_NEGRITO, 11);

            writer.close();
            document.save(outputStream);
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Falha ao gerar Nota de Conferência.", ex);
        }
    }

    private static String formatarTipo(TipoVisita tipo) {
        return tipo == TipoVisita.VENDA ? "VENDA" : "ENTREGA / DEVOLUCAO";
    }

    private static int calcularQuantidade(VisitaItem item, boolean venda) {
        if (venda) {
            return item.getVendido();
        }
        return item.getEntregue() + item.getRetirado();
    }

    private static String texto(String valor) {
        return (valor == null || valor.isBlank()) ? "-" : valor;
    }

    private static DecimalFormat criarFormatterMoeda() {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("pt", "BR"));
        symbols.setDecimalSeparator(',');
        symbols.setGroupingSeparator('.');
        return new DecimalFormat("R$ #,##0.00", symbols);
    }

    private static String formatarMoeda(BigDecimal valor) {
        if (valor == null) {
            return "R$ 0,00";
        }
        return MOEDA_FORMATTER.format(valor);
    }

    private static String ajustarTexto(String texto, float maxWidth, PDFont font, float fontSize) throws IOException {
        if (texto == null) {
            return "-";
        }
        if (font.getStringWidth(texto) / 1000f * fontSize <= maxWidth) {
            return texto;
        }
        StringBuilder builder = new StringBuilder();
        for (char ch : texto.toCharArray()) {
            builder.append(ch);
            String candidate = builder + "...";
            if (font.getStringWidth(candidate) / 1000f * fontSize > maxWidth) {
                builder.setLength(Math.max(builder.length() - 1, 0));
                return builder + "...";
            }
        }
        return texto;
    }

    private static void desenharLinhaFormulario(PdfWriter writer,
                                                String rotulo,
                                                String valor,
                                                float xInicio,
                                                float y,
                                                float xFim) throws IOException {
        writer.writeAt(rotulo, FONTE_PADRAO, 10, xInicio, y);
        float rotuloWidth = writer.textWidth(rotulo, FONTE_PADRAO, 10);
        float linhaInicio = xInicio + rotuloWidth + PADDING_TEXTO;
        writer.drawLine(linhaInicio, y - 2, xFim, y - 2);
        writer.writeAt(valor, FONTE_PADRAO, 10, linhaInicio + 2, y);
    }

    private static void desenharLinhaCabecalhoTabela(PdfWriter writer,
                                                     boolean venda,
                                                     float topY,
                                                     float bottomY,
                                                     float tableLeft,
                                                     float tableWidth,
                                                     float xQuantidade,
                                                     float xUnitario,
                                                     float xTotal,
                                                     float xEntregue,
                                                     float xRetirado) throws IOException {
        writer.drawLine(tableLeft, topY, tableLeft + tableWidth, topY);
        writer.drawLine(tableLeft, bottomY, tableLeft + tableWidth, bottomY);
        writer.drawLine(tableLeft, topY, tableLeft, bottomY);
        if (venda) {
            writer.drawLine(xQuantidade, topY, xQuantidade, bottomY);
            writer.drawLine(xUnitario, topY, xUnitario, bottomY);
            writer.drawLine(xTotal, topY, xTotal, bottomY);
        } else {
            writer.drawLine(xEntregue, topY, xEntregue, bottomY);
            writer.drawLine(xRetirado, topY, xRetirado, bottomY);
        }
        writer.drawLine(tableLeft + tableWidth, topY, tableLeft + tableWidth, bottomY);
    }

    private static void desenharLinhaTabela(PdfWriter writer,
                                            float topY,
                                            float bottomY,
                                            float tableLeft,
                                            float tableWidth,
                                            float xQuantidade,
                                            float xUnitario,
                                            float xTotal,
                                            float xEntregue,
                                            float xRetirado,
                                            boolean venda) throws IOException {
        writer.drawLine(tableLeft, topY, tableLeft + tableWidth, topY);
        writer.drawLine(tableLeft, bottomY, tableLeft + tableWidth, bottomY);
        writer.drawLine(tableLeft, topY, tableLeft, bottomY);
        if (venda) {
            writer.drawLine(xQuantidade, topY, xQuantidade, bottomY);
            writer.drawLine(xUnitario, topY, xUnitario, bottomY);
            writer.drawLine(xTotal, topY, xTotal, bottomY);
        } else {
            writer.drawLine(xEntregue, topY, xEntregue, bottomY);
            writer.drawLine(xRetirado, topY, xRetirado, bottomY);
        }
        writer.drawLine(tableLeft + tableWidth, topY, tableLeft + tableWidth, bottomY);
    }

    private static class PdfWriter implements Closeable {

        private final PDDocument document;
        private PDPage page;
        private PDPageContentStream content;
        private final float margin = 40f;
        private final float leading = 14f;
        private float y;

        private PdfWriter(PDDocument document) throws IOException {
            this.document = document;
            newPage();
        }

        private void newPage() throws IOException {
            closeCurrentContent();
            page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            content = new PDPageContentStream(document, page);
            y = page.getMediaBox().getHeight() - margin;
        }

        private void closeCurrentContent() throws IOException {
                if (content != null) {
                    content.close();
                    content = null;
                }
        }

        private void writeLine(String text, PDFont font, float fontSize) throws IOException {
            ensureSpace(leading);
            writeLineAt(text, font, fontSize, margin);
            newLine();
        }

        private void writeWrapped(String text, PDFont font, float fontSize, float maxWidth) throws IOException {
            List<String> linhas = wrapText(text, maxWidth, font, fontSize);
            for (String linha : linhas) {
                writeLine(linha, font, fontSize);
            }
        }

        private void writeLineAt(String text, PDFont font, float fontSize, float x) throws IOException {
            content.beginText();
            content.setFont(font, fontSize);
            content.newLineAtOffset(x, y);
            content.showText(text);
            content.endText();
        }

        private void writeAt(String text, PDFont font, float fontSize, float x, float y) throws IOException {
            content.beginText();
            content.setFont(font, fontSize);
            content.newLineAtOffset(x, y);
            content.showText(text);
            content.endText();
        }

        private void writeCentered(String text, PDFont font, float fontSize, float x, float y,
                                   float width, float height) throws IOException {
            float textWidth = textWidth(text, font, fontSize);
            float textX = x + (width - textWidth) / 2;
            float textY = y + (height / 2) + (fontSize / 2) - 3;
            writeAt(text, font, fontSize, textX, textY);
        }

        private void drawLine(float x1, float y1, float x2, float y2) throws IOException {
            content.moveTo(x1, y1);
            content.lineTo(x2, y2);
            content.stroke();
        }

        private void drawRect(float x, float y, float width, float height) throws IOException {
            content.addRect(x, y, width, height);
            content.stroke();
        }

        private void moveDown(float amount) {
            y -= amount;
        }

        private void newLine() {
            y -= leading;
        }

        private void setY(float y) {
            this.y = y;
        }

        private float getY() {
            return y;
        }

        private boolean hasSpace(float height) {
            return y - height >= margin;
        }

        private void ensureSpace(float height) throws IOException {
            if (!hasSpace(height)) {
                newPage();
            }
        }

        private float getMargin() {
            return margin;
        }

        private float getPageWidth() {
            return page.getMediaBox().getWidth();
        }

        private float getMaxWidth() {
            return page.getMediaBox().getWidth() - (margin * 2);
        }

        private float getLeading() {
            return leading;
        }

        private float textWidth(String text, PDFont font, float fontSize) throws IOException {
            return larguraTexto(text, font, fontSize);
        }

        private List<String> wrapText(String text, float maxWidth, PDFont font, float fontSize) throws IOException {
            List<String> linhas = new ArrayList<>();
            if (text == null || text.isBlank()) {
                linhas.add("-");
                return linhas;
            }
            String[] palavras = text.split("\\s+");
            StringBuilder linha = new StringBuilder();
            for (String palavra : palavras) {
                String teste = linha.length() == 0 ? palavra : linha + " " + palavra;
                if (larguraTexto(teste, font, fontSize) > maxWidth) {
                    linhas.add(linha.toString());
                    linha = new StringBuilder(palavra);
                } else {
                    linha = new StringBuilder(teste);
                }
            }
            if (linha.length() > 0) {
                linhas.add(linha.toString());
            }
            return linhas;
        }

        private float larguraTexto(String text, PDFont font, float fontSize) throws IOException {
            return font.getStringWidth(text) / 1000f * fontSize;
        }

        @Override
        public void close() throws IOException {
            closeCurrentContent();
        }
    }
}
