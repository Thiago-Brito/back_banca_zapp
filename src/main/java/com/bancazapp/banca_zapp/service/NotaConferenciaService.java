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

            float headerTop = writer.getY();
            writer.writeLine(TITULO, FONTE_NEGRITO, 18);
            writer.moveDown(2);
            writer.writeLine("Razao social: " + texto(properties.getRazaoSocial()), FONTE_PADRAO, 11);
            writer.writeLine("CNPJ: " + texto(properties.getCnpj()), FONTE_PADRAO, 11);
            writer.writeLine("Telefone: " + texto(properties.getTelefone()), FONTE_PADRAO, 11);
            writer.writeLine("Endereco: " + texto(properties.getEndereco()), FONTE_PADRAO, 11);
            writer.moveDown(2);
            writer.writeLine("Nome fantasia: " + texto(properties.getNomeFantasia()), FONTE_NEGRITO, 13);
            float headerBottom = writer.getY();
            float headerWidth = writer.getPageWidth() - (writer.getMargin() * 2);
            writer.drawRect(writer.getMargin(), headerBottom - 6, headerWidth, (headerTop - headerBottom) + 12);
            writer.drawLine(writer.getMargin(), headerBottom - 10, writer.getMargin() + headerWidth, headerBottom - 10);
            writer.setY(headerBottom - ESPACAMENTO_SECAO);

            float dadosTop = writer.getY();
            writer.writeLine("Dados da visita", FONTE_NEGRITO, 12);
            writer.writeLine("Cliente: " + texto(visita.getCliente().getNome()), FONTE_PADRAO, 11);
            writer.writeLine("Data da visita: " + visita.getDataVisita().format(DATA_FORMATTER), FONTE_PADRAO, 11);
            writer.writeLine("Tipo da visita: " + formatarTipo(visita.getTipo()), FONTE_PADRAO, 11);
            writer.writeWrapped("Observacoes: " + texto(visita.getObservacoes()), FONTE_PADRAO, 11, writer.getMaxWidth() - 10);
            float dadosBottom = writer.getY();
            writer.drawRect(writer.getMargin(), dadosBottom - 6, headerWidth, (dadosTop - dadosBottom) + 12);
            writer.setY(dadosBottom - ESPACAMENTO_SECAO);

            writer.writeLine("Produtos", FONTE_NEGRITO, 12);
            writer.moveDown(4);

            float tableLeft = writer.getMargin();
            float tableWidth = 480f;
            float colunaProdutoLargura;
            float xProduto = tableLeft;
            float xQuantidade;
            float xUnitario = 0f;
            float xTotal = 0f;
            float xEntregue = 0f;
            float xRetirado = 0f;

            if (venda) {
                colunaProdutoLargura = 240f;
                xQuantidade = tableLeft + colunaProdutoLargura;
                xUnitario = xQuantidade + 60f;
                xTotal = xUnitario + 90f;
            } else {
                colunaProdutoLargura = 300f;
                xQuantidade = tableLeft + colunaProdutoLargura;
                xEntregue = xQuantidade;
                xRetirado = xEntregue + 90f;
            }

            float headerRowHeight = 20f;
            float headerTopY = writer.getY() + 6;
            float headerBottomY = headerTopY - headerRowHeight;
            desenharLinhaCabecalhoTabela(writer, venda, headerTopY, headerBottomY, tableLeft, tableWidth, xQuantidade, xUnitario, xTotal, xEntregue, xRetirado);
            float headerTextY = headerTopY - 14;
            writer.writeAt("Produto", FONTE_NEGRITO, 10, xProduto + PADDING_TEXTO, headerTextY);
            if (venda) {
                writer.writeAt("Qtd.", FONTE_NEGRITO, 10, xQuantidade + PADDING_TEXTO, headerTextY);
                writer.writeAt("Valor Unit.", FONTE_NEGRITO, 10, xUnitario + PADDING_TEXTO, headerTextY);
                writer.writeAt("Valor Total", FONTE_NEGRITO, 10, xTotal + PADDING_TEXTO, headerTextY);
            } else {
                writer.writeAt("Entregue", FONTE_NEGRITO, 10, xEntregue + PADDING_TEXTO, headerTextY);
                writer.writeAt("Retirado", FONTE_NEGRITO, 10, xRetirado + PADDING_TEXTO, headerTextY);
            }
            writer.setY(headerBottomY);

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

                List<String> produtoLinhas = writer.wrapText(texto(item.getProduto().getNome()), colunaProdutoLargura - 2 * PADDING_TEXTO,
                        FONTE_PADRAO, 10);
                float rowHeight = Math.max(writer.getLeading() * produtoLinhas.size(), writer.getLeading()) + 8;
                if (!writer.hasSpace(rowHeight + headerRowHeight)) {
                    writer.newPage();
                    headerTopY = writer.getY() + 6;
                    headerBottomY = headerTopY - headerRowHeight;
                    desenharLinhaCabecalhoTabela(writer, venda, headerTopY, headerBottomY, tableLeft, tableWidth, xQuantidade, xUnitario, xTotal, xEntregue, xRetirado);
                    headerTextY = headerTopY - 14;
                    writer.writeAt("Produto", FONTE_NEGRITO, 10, xProduto + PADDING_TEXTO, headerTextY);
                    if (venda) {
                        writer.writeAt("Qtd.", FONTE_NEGRITO, 10, xQuantidade + PADDING_TEXTO, headerTextY);
                        writer.writeAt("Valor Unit.", FONTE_NEGRITO, 10, xUnitario + PADDING_TEXTO, headerTextY);
                        writer.writeAt("Valor Total", FONTE_NEGRITO, 10, xTotal + PADDING_TEXTO, headerTextY);
                    } else {
                        writer.writeAt("Entregue", FONTE_NEGRITO, 10, xEntregue + PADDING_TEXTO, headerTextY);
                        writer.writeAt("Retirado", FONTE_NEGRITO, 10, xRetirado + PADDING_TEXTO, headerTextY);
                    }
                    writer.setY(headerBottomY);
                }

                float rowTop = writer.getY();
                float rowBottom = rowTop - rowHeight;
                desenharLinhaTabela(writer, rowTop, rowBottom, tableLeft, tableWidth, xQuantidade, xUnitario, xTotal, xEntregue, xRetirado, venda);
                float textY = rowTop - 14;
                for (int i = 0; i < produtoLinhas.size(); i++) {
                    writer.writeAt(produtoLinhas.get(i), FONTE_PADRAO, 10, xProduto + PADDING_TEXTO, textY - (i * writer.getLeading()));
                }
                writer.writeAt(String.valueOf(quantidade), FONTE_PADRAO, 10, xQuantidade + PADDING_TEXTO, textY);
                if (venda) {
                    writer.writeAt(formatarMoeda(precoUnitario), FONTE_PADRAO, 10, xUnitario + PADDING_TEXTO, textY);
                    writer.writeAt(formatarMoeda(totalItem), FONTE_PADRAO, 10, xTotal + PADDING_TEXTO, textY);
                } else {
                    writer.writeAt(String.valueOf(item.getEntregue()), FONTE_PADRAO, 10, xEntregue + PADDING_TEXTO, textY);
                    writer.writeAt(String.valueOf(item.getRetirado()), FONTE_PADRAO, 10, xRetirado + PADDING_TEXTO, textY);
                }
                writer.setY(rowBottom);
            }

            writer.moveDown(ESPACAMENTO_SECAO);
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
        private final float margin = 50f;
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
