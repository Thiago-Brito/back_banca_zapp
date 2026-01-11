package com.bancazapp.banca_zapp.service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.bancazapp.banca_zapp.entity.TipoVisita;
import com.bancazapp.banca_zapp.entity.Visita;
import com.bancazapp.banca_zapp.entity.VisitaItem;

import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final JavaMailSender mailSender;
    private final boolean enabled;
    private final String defaultRecipient;
    private final String from;

    public EmailService(JavaMailSender mailSender,
                        @Value("${app.mail.enabled:false}") boolean enabled,
                        @Value("${app.mail.default-recipient:}") String defaultRecipient,
                        @Value("${app.mail.from:no-reply@bancazapp.local}") String from) {
        this.mailSender = mailSender;
        this.enabled = enabled;
        this.defaultRecipient = defaultRecipient;
        this.from = from;
    }

    public void enviarVisitaCriada(Visita visita) {
        if (!enabled) {
            logger.debug("Envio de e-mail desativado.");
            return;
        }
        String destino = resolverDestino(visita);
        if (destino == null || destino.isBlank()) {
            logger.warn("E-mail nao enviado: destinatario nao configurado.");
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(destino);
            helper.setFrom(from);
            helper.setSubject(montarAssunto(visita.getTipo()));
            helper.setText(montarCorpoHtml(visita), true);
            mailSender.send(message);
        } catch (Exception ex) {
            logger.warn("Falha ao enviar e-mail da visita {}: {}", visita.getId(), ex.getMessage());
        }
    }

    private String resolverDestino(Visita visita) {
        if (defaultRecipient != null && !defaultRecipient.isBlank()) {
            return defaultRecipient;
        }
        if (visita.getCliente() != null) {
            return visita.getCliente().getEmail();
        }
        return null;
    }

    private String montarAssunto(TipoVisita tipoVisita) {
        if (tipoVisita == TipoVisita.VENDA) {
            return "Nova venda registrada";
        }
        return "Entrega / Devolucao registrada";
    }

    private String montarCorpoHtml(Visita visita) {
        StringBuilder html = new StringBuilder();
        html.append("<html><body>");
        html.append("<h2>").append(montarAssunto(visita.getTipo())).append("</h2>");
        html.append("<p><strong>Cliente:</strong> ").append(escape(visita.getCliente().getNome())).append("</p>");
        html.append("<p><strong>Data:</strong> ").append(visita.getDataVisita().format(DATE_FORMAT)).append("</p>");
        html.append("<p><strong>Tipo:</strong> ").append(visita.getTipo()).append("</p>");
        html.append("<p><strong>Observacoes:</strong> ").append(escape(Objects.toString(visita.getObservacoes(), ""))).append("</p>");

        html.append("<table border=\"1\" cellpadding=\"6\" cellspacing=\"0\">");
        html.append("<thead><tr>");
        html.append("<th>Produto</th>");
        if (visita.getTipo() == TipoVisita.VENDA) {
            html.append("<th>Vendido</th>");
        } else {
            html.append("<th>Entregue</th>");
            html.append("<th>Retirado</th>");
        }
        html.append("</tr></thead><tbody>");

        List<VisitaItem> itens = visita.getItens();
        int totalVendido = 0;
        int totalEntregue = 0;
        int totalRetirado = 0;
        for (VisitaItem item : itens) {
            html.append("<tr>");
            html.append("<td>").append(escape(item.getProduto().getNome())).append("</td>");
            if (visita.getTipo() == TipoVisita.VENDA) {
                html.append("<td>").append(item.getVendido()).append("</td>");
                totalVendido += item.getVendido();
            } else {
                html.append("<td>").append(item.getEntregue()).append("</td>");
                html.append("<td>").append(item.getRetirado()).append("</td>");
                totalEntregue += item.getEntregue();
                totalRetirado += item.getRetirado();
            }
            html.append("</tr>");
        }
        html.append("</tbody>");
        html.append("<tfoot><tr>");
        html.append("<td><strong>Total</strong></td>");
        if (visita.getTipo() == TipoVisita.VENDA) {
            html.append("<td><strong>").append(totalVendido).append("</strong></td>");
        } else {
            html.append("<td><strong>").append(totalEntregue).append("</strong></td>");
            html.append("<td><strong>").append(totalRetirado).append("</strong></td>");
        }
        html.append("</tr></tfoot>");
        html.append("</table>");

        html.append("</body></html>");
        return html.toString();
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
