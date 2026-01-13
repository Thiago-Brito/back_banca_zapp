package com.bancazapp.banca_zapp.service;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
import com.bancazapp.banca_zapp.entity.User;
import com.bancazapp.banca_zapp.entity.Visita;
import com.bancazapp.banca_zapp.entity.VisitaItem;
import com.bancazapp.banca_zapp.exception.ResourceNotFoundException;
import com.bancazapp.banca_zapp.repository.UserRepository;
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
    private static final BigDecimal CEM = BigDecimal.valueOf(100);

    private final VisitaRepository visitaRepository;
    private final UserRepository userRepository;
    private final NotaConferenciaProperties properties;

    public NotaConferenciaService(VisitaRepository visitaRepository,
                                  UserRepository userRepository,
                                  NotaConferenciaProperties properties) {
        this.visitaRepository = visitaRepository;
        this.userRepository = userRepository;
        this.properties = properties;
    }

    public byte[] gerarNotaConferencia(UUID visitaId) {
        Visita visita = visitaRepository.findWithItensById(visitaId)
                .orElseThrow(() -> new ResourceNotFoundException("Visita nao encontrada: " + visitaId));
        return gerarPdfA4(visita);
    }

    public byte[] gerarNotaConferencia(UUID visitaId, String formato) {
        Visita visita = visitaRepository.findWithItensById(visitaId)
                .orElseThrow(() -> new ResourceNotFoundException("Visita nao encontrada: " + visitaId));
        if ("80mm".equalsIgnoreCase(formato)) {
            return gerarPdf80mm(visita);
        }
        return gerarPdfA4(visita);
    }

    private byte[] gerarPdfA4(Visita visita) {
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
            NotaConferenciaInfo info = resolverEmitente();
            writer.writeAt(info.nomeFantasia().toUpperCase(Locale.ROOT), FONTE_NEGRITO, 18, headerTextX, headerTextY);
            writer.writeAt("CNPJ: " + info.cnpj(), FONTE_PADRAO, 10, headerTextX, headerTextY - 18);
            writer.writeAt("Telefone: " + info.telefone(), FONTE_PADRAO, 10, headerTextX, headerTextY - 32);
            writer.setY(headerBottom - ESPACAMENTO_SECAO);

            float dadosHeightBase = 88f;
            float dadosTop = writer.getY();
            float linha1Y = dadosTop - 30;
            float linha2Y = linha1Y - ESPACAMENTO_LINHA_FORM;
            float linha3Y = linha2Y - ESPACAMENTO_LINHA_FORM;
            float meioX = margin + (contentWidth / 2);

            float boxPadding = 6f;
            float obsBoxLeft = margin + boxPadding;
            float obsBoxRight = margin + contentWidth - boxPadding;
            float obsBoxWidth = obsBoxRight - obsBoxLeft;
            float obsBoxTop = linha3Y - 4;

            String textoObs = texto(visita.getObservacoes());
            List<String> obsLinhas = writer.wrapText(textoObs, obsBoxWidth - (boxPadding * 2), FONTE_PADRAO, 10);
            float alturaLinha = 12f;
            int linhasUsadas = Math.max(obsLinhas.size(), 1);
            float obsBoxHeight = (linhasUsadas * alturaLinha) + (boxPadding * 2);
            float obsBoxBottom = obsBoxTop - obsBoxHeight;

            float dadosBottomBase = dadosTop - dadosHeightBase;
            float dadosBottom = Math.min(dadosBottomBase, obsBoxBottom - boxPadding);
            float dadosHeight = dadosTop - dadosBottom;

            writer.drawRect(margin, dadosBottom, contentWidth, dadosHeight);
            writer.writeAt("Dados da visita", FONTE_NEGRITO, 11, margin + 6, dadosTop - 14);

            desenharLinhaFormulario(writer, "Cliente:", texto(visita.getCliente().getNome()),
                    margin + 6, linha1Y, margin + contentWidth - 6);
            desenharLinhaFormulario(writer, "Data da visita:", visita.getDataVisita().format(DATA_FORMATTER),
                    margin + 6, linha2Y, meioX - 6);
            desenharLinhaFormulario(writer, "Tipo da visita:", formatarTipo(visita.getTipo()),
                    meioX + 6, linha2Y, margin + contentWidth - 6);

            writer.writeAt("Observacoes:", FONTE_PADRAO, 10, margin + 6, linha3Y );
            writer.drawRect(obsBoxLeft, obsBoxBottom, obsBoxWidth, obsBoxHeight);
            float obsTextY = obsBoxTop - boxPadding - 2;
            for (int i = 0; i < linhasUsadas; i++) {
                writer.writeAt(obsLinhas.get(i), FONTE_PADRAO, 10, obsBoxLeft + boxPadding, obsTextY - (i * alturaLinha) -5);
            }
            writer.setY(obsBoxBottom - ESPACAMENTO_SECAO);

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
                BigDecimal valorBruto = totalValor;
                BigDecimal percentual = valorSeguro(visita.getCliente().getComissao());
                BigDecimal desconto = valorBruto.multiply(percentual)
                        .divide(CEM, 2, RoundingMode.HALF_UP);
                BigDecimal valorAPagar = valorBruto.subtract(desconto).setScale(2, RoundingMode.HALF_UP);

                String valorBrutoTexto = "Valor bruto: " + formatarMoeda(valorBruto);
                float valorBrutoX = writer.getPageWidth() - writer.getMargin() - writer.textWidth(valorBrutoTexto, FONTE_PADRAO, 11);
                writer.writeAt(valorBrutoTexto, FONTE_PADRAO, 11, valorBrutoX, writer.getY());
                writer.newLine();

                String descontoTexto = "Desconto (" + formatarPercentual(percentual) + "): " + formatarMoeda(desconto);
                float descontoX = writer.getPageWidth() - writer.getMargin() - writer.textWidth(descontoTexto, FONTE_PADRAO, 11);
                writer.writeAt(descontoTexto, FONTE_PADRAO, 11, descontoX, writer.getY());
                writer.newLine();

                float linhaY = writer.getY() + 4;
                writer.drawLine(writer.getMargin() + 250, linhaY, writer.getPageWidth() - writer.getMargin(), linhaY);
                writer.newLine();

                String valorAPagarTexto = "Valor a pagar: " + formatarMoeda(valorAPagar);
                float valorAPagarX = writer.getPageWidth() - writer.getMargin() - writer.textWidth(valorAPagarTexto, FONTE_NEGRITO, 12);
                writer.writeAt(valorAPagarTexto, FONTE_NEGRITO, 12, valorAPagarX, writer.getY());
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

    private byte[] gerarPdf80mm(Visita visita) {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            boolean venda = visita.getTipo() == TipoVisita.VENDA;
            float pageWidth = mmToPoints(80f);
            float margin = 10f;
            float contentWidth = pageWidth - (margin * 2);

            List<ReceiptElement> elementos = new ArrayList<>();
            NotaConferenciaInfo info = resolverEmitente();
            adicionarCentralizado(elementos, info.nomeFantasia().toUpperCase(Locale.ROOT), FONTE_NEGRITO, 11);
            adicionarCentralizado(elementos, "CNPJ: " + info.cnpj(), FONTE_PADRAO, 8);
            adicionarCentralizado(elementos, "Telefone: " + info.telefone(), FONTE_PADRAO, 8);
            elementos.add(ReceiptElement.separator());

            adicionarCentralizado(elementos, "NOTA DE CONFERÊNCIA", FONTE_NEGRITO, 10);
            for (String linha : wrapText(TEXTO_SEM_VALOR_FISCAL, contentWidth, FONTE_PADRAO, 8)) {
                adicionarCentralizado(elementos, linha, FONTE_PADRAO, 8);
            }
            elementos.add(ReceiptElement.blank(6));

            elementos.add(ReceiptElement.text("Cliente: " + texto(visita.getCliente().getNome()), FONTE_PADRAO, 8));
            elementos.add(ReceiptElement.text("Data: " + visita.getDataVisita().format(DATA_FORMATTER), FONTE_PADRAO, 8));
            elementos.add(ReceiptElement.text("Tipo: " + formatarTipo(visita.getTipo()), FONTE_PADRAO, 8));

            elementos.add(ReceiptElement.blank(4));
            elementos.add(ReceiptElement.text("Observacoes:", FONTE_NEGRITO, 8));
            List<String> obsLinhas = wrapText(texto(visita.getObservacoes()), contentWidth, FONTE_PADRAO, 8);
            for (String linha : obsLinhas) {
                elementos.add(ReceiptElement.text(linha, FONTE_PADRAO, 8));
            }
            elementos.add(ReceiptElement.separator());

            int totalItens = 0;
            BigDecimal totalValor = BigDecimal.ZERO;
            List<VisitaItem> itens = visita.getItens();
            for (int i = 0; i < itens.size(); i++) {
                VisitaItem item = itens.get(i);
                int quantidade = calcularQuantidade(item, venda);
                totalItens += quantidade;

                BigDecimal precoUnitario = item.getProduto().getPreco();
                BigDecimal totalItem = BigDecimal.ZERO;
                if (venda) {
                    totalItem = precoUnitario.multiply(BigDecimal.valueOf(item.getVendido()));
                    totalValor = totalValor.add(totalItem);
                }

                List<String> nomeProduto = wrapText(texto(item.getProduto().getNome()), contentWidth, FONTE_NEGRITO, 8);
                for (String linha : nomeProduto) {
                    elementos.add(ReceiptElement.text(linha, FONTE_NEGRITO, 8));
                }
                elementos.add(ReceiptElement.text("Qtd: " + quantidade, FONTE_PADRAO, 8));
                if (venda) {
                    elementos.add(ReceiptElement.text("Vlr unit: " + formatarMoeda(precoUnitario), FONTE_PADRAO, 8));
                    elementos.add(ReceiptElement.text("Subtotal: " + formatarMoeda(totalItem), FONTE_PADRAO, 8));
                }
                if (itens.size() > 1 && i < itens.size() - 1) {
                    elementos.add(ReceiptElement.separator());
                }
            }

            elementos.add(ReceiptElement.separator());
            elementos.add(ReceiptElement.text("Total de itens: " + totalItens, FONTE_PADRAO, 8));
            if (venda) {
                BigDecimal valorBruto = totalValor;
                BigDecimal percentual = valorSeguro(visita.getCliente().getComissao());
                BigDecimal desconto = valorBruto.multiply(percentual)
                        .divide(CEM, 2, RoundingMode.HALF_UP);
                BigDecimal valorAPagar = valorBruto.subtract(desconto).setScale(2, RoundingMode.HALF_UP);

                elementos.add(ReceiptElement.text("Valor bruto: " + formatarMoeda(valorBruto), FONTE_PADRAO, 8));
                elementos.add(ReceiptElement.text("Desconto (" + formatarPercentual(percentual) + "): " + formatarMoeda(desconto), FONTE_PADRAO, 8));
                elementos.add(ReceiptElement.separator());
                elementos.add(ReceiptElement.text("Valor a pagar: " + formatarMoeda(valorAPagar), FONTE_NEGRITO, 10));
            }

            elementos.add(ReceiptElement.blank(10));
            elementos.add(ReceiptElement.text("Assinatura: __________________________", FONTE_PADRAO, 8));
            elementos.add(ReceiptElement.blank(6));
            for (String linha : wrapText(TEXTO_SEM_VALOR_FISCAL, contentWidth, FONTE_NEGRITO, 9)) {
                adicionarCentralizado(elementos, linha, FONTE_NEGRITO, 9);
            }

            float alturaPagina = calcularAlturaPagina(elementos, margin);
            PDRectangle pageSize = new PDRectangle(pageWidth, alturaPagina);
            try (PdfWriter writer = new PdfWriter(document, pageSize, margin)) {
                escreverElementosCupom(writer, elementos, contentWidth);
            }
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

    private NotaConferenciaInfo resolverEmitente() {
        User user = userRepository.findFirstByOrderByIdAsc().orElse(null);
        String nomeFantasia = preferir(user != null ? user.getNomeFantasia() : null, properties.getNomeFantasia());
        String cnpj = preferir(user != null ? user.getCnpj() : null, properties.getCnpj());
        String telefone = preferir(user != null ? user.getTelefone() : null, properties.getTelefone());
        return new NotaConferenciaInfo(texto(nomeFantasia), texto(cnpj), texto(telefone));
    }

    private static String preferir(String valor, String fallback) {
        return (valor == null || valor.isBlank()) ? fallback : valor;
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

    private static String formatarPercentual(BigDecimal valor) {
        if (valor == null) {
            return "0%";
        }
        return valor.stripTrailingZeros().toPlainString() + "%";
    }

    private static BigDecimal valorSeguro(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }

    private static float mmToPoints(float mm) {
        return mm * 72f / 25.4f;
    }

    private static List<String> wrapText(String text, float maxWidth, PDFont font, float fontSize) throws IOException {
        if (text == null || text.isBlank()) {
            return List.of("-");
        }
        List<String> linhas = new ArrayList<>();
        String[] palavras = text.split("\\s+");
        StringBuilder linha = new StringBuilder();
        for (String palavra : palavras) {
            String teste = linha.length() == 0 ? palavra : linha + " " + palavra;
            if (font.getStringWidth(teste) / 1000f * fontSize > maxWidth) {
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

    private static void adicionarCentralizado(List<ReceiptElement> elementos, String texto, PDFont font, float size) {
        elementos.add(ReceiptElement.center(texto, font, size));
    }

    private static float calcularAlturaPagina(List<ReceiptElement> elementos, float margin) {
        float altura = margin;
        for (ReceiptElement elemento : elementos) {
            altura += elemento.height();
        }
        altura += margin;
        return Math.max(altura, 200f);
    }

    private static void escreverElementosCupom(PdfWriter writer, List<ReceiptElement> elementos, float contentWidth)
            throws IOException {
        for (ReceiptElement elemento : elementos) {
            if (elemento.type == ReceiptElementType.SEPARATOR) {
                writer.writeAt(elemento.text, elemento.font, elemento.size, writer.getMargin(), writer.getY());
                writer.moveDown(elemento.height());
                continue;
            }
            if (elemento.type == ReceiptElementType.BLANK) {
                writer.moveDown(elemento.height());
                continue;
            }
            float textY = writer.getY();
            float x = writer.getMargin();
            if (elemento.align == ReceiptAlign.CENTER) {
                float textWidth = writer.textWidth(elemento.text, elemento.font, elemento.size);
                x = writer.getMargin() + (contentWidth - textWidth) / 2;
            }
            writer.writeAt(elemento.text, elemento.font, elemento.size, x, textY);
            writer.moveDown(elemento.height());
        }
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

    private record NotaConferenciaInfo(String nomeFantasia, String cnpj, String telefone) {}

    private enum ReceiptElementType {
        TEXT,
        SEPARATOR,
        BLANK
    }

    private enum ReceiptAlign {
        LEFT,
        CENTER
    }

    private static class ReceiptElement {
        private final ReceiptElementType type;
        private final String text;
        private final PDFont font;
        private final float size;
        private final ReceiptAlign align;
        private final float height;

        private ReceiptElement(ReceiptElementType type, String text, PDFont font, float size, ReceiptAlign align, float height) {
            this.type = type;
            this.text = text;
            this.font = font;
            this.size = size;
            this.align = align;
            this.height = height;
        }

        private static ReceiptElement text(String text, PDFont font, float size) {
            return new ReceiptElement(ReceiptElementType.TEXT, text, font, size, ReceiptAlign.LEFT, size + 3);
        }

        private static ReceiptElement center(String text, PDFont font, float size) {
            return new ReceiptElement(ReceiptElementType.TEXT, text, font, size, ReceiptAlign.CENTER, size + 3);
        }

        private static ReceiptElement separator() {
            return new ReceiptElement(ReceiptElementType.SEPARATOR, "--------------------------------", FONTE_PADRAO, 10, ReceiptAlign.LEFT, 14f);
        }

        private static ReceiptElement blank(float height) {
            return new ReceiptElement(ReceiptElementType.BLANK, null, null, 0, ReceiptAlign.LEFT, height);
        }

        private float height() {
            return height;
        }
    }

    private static class PdfWriter implements Closeable {

        private final PDDocument document;
        private PDPage page;
        private PDPageContentStream content;
        private final PDRectangle pageSize;
        private final float margin;
        private final float leading = 14f;
        private float y;

        private PdfWriter(PDDocument document) throws IOException {
            this(document, PDRectangle.A4, 40f);
        }

        private PdfWriter(PDDocument document, PDRectangle pageSize, float margin) throws IOException {
            this.document = document;
            this.pageSize = pageSize;
            this.margin = margin;
            newPage();
        }

        private void newPage(PDRectangle pageSize) throws IOException {
            closeCurrentContent();
            page = new PDPage(pageSize);
            document.addPage(page);
            content = new PDPageContentStream(document, page);
            y = page.getMediaBox().getHeight() - margin;
        }

        private void newPage() throws IOException {
            newPage(pageSize);
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
