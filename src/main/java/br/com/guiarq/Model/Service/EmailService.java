package br.com.guiarq.Model.Service;

import br.com.guiarq.Model.Entities.Ticket;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

@Service
public class EmailService {

    @Value("${RESEND_API_KEY}")
    private String apiKey;

    @Value("${API_URL}")
    private String baseUrl;

    private final ObjectMapper mapper = new ObjectMapper();

    public void sendTicketEmail(
            String emailDestino,
            String nomeCliente,
            String telefone,
            String cpf,
            String nomeTicket,
            byte[] qrBytes
    ) {
        try {
            String html = """
                    <h2>Seu Ticket Está Pronto 🎟️</h2>
                    <p>Olá <strong>%s</strong>,</p>
                    <p><strong>Ticket:</strong> %s</p>
                    <p><strong>Telefone:</strong> %s<br><strong>CPF:</strong> %s</p>
                    <p>O QR Code está anexado a este e-mail.</p>
                    <p>Apresente no estabelecimento participante DO MEU PAU GRANDAO.</p>
                    """.formatted(nomeCliente, nomeTicket, telefone, cpf);

            String qrBase64 = Base64.getEncoder().encodeToString(qrBytes);

            Map<String, Object> attachment = new HashMap<>();
            attachment.put("filename", nomeTicket + ".png");
            attachment.put("content", qrBase64);

            Map<String, Object> body = new HashMap<>();
            body.put("from", "Guia Rancho Queimado <no-reply@guiaranchoqueimado.com.br>");
            body.put("to", new String[]{emailDestino});
            body.put("subject", "Seu Ticket – " + nomeTicket);
            body.put("html", html);
            body.put("attachments", new Object[]{attachment});

            String json = mapper.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.resend.com/emails"))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("📨 Email de ticket individual enviado!");

        } catch (Exception e) {
            throw new RuntimeException("Erro ao enviar email de ticket: " + e.getMessage());
        }
    }

    public void sendMultiplosTicketsAvulsos(
            String emailDestino,
            String nomeCliente,
            String telefone,
            String cpf,
            String nomeTicket,
            List<Ticket> tickets,
            List<byte[]> qrBytesList
    ) {
        try {
            StringBuilder html = new StringBuilder();
            html.append("<h2>Seus Tickets Estão Prontos 🎟️</h2>");
            html.append("<p>Olá <strong>").append(nomeCliente).append("</strong>,</p>");
            html.append("<p>Você comprou <strong>")
                    .append(tickets.size())
                    .append(" tickets</strong> do estabelecimento <strong>")
                    .append(nomeTicket)
                    .append("</strong>.</p>");
            html.append("<p>Os QR Codes estão anexados a este e-mail.</p>");

            List<Map<String, Object>> attachments = new ArrayList<>();
            for (int i = 0; i < tickets.size(); i++) {
                Ticket t = tickets.get(i);
                byte[] qrBytes = qrBytesList.get(i);
                String base64Qr = Base64.getEncoder().encodeToString(qrBytes);

                Map<String, Object> attachment = new HashMap<>();
                attachment.put("filename", t.getNome() + " - Ticket " + (i + 1) + ".png");
                attachment.put("content", base64Qr);

                attachments.add(attachment);
            }
            Map<String, Object> body = new HashMap<>();
            body.put("from", "Guia Rancho Queimado <no-reply@guiaranchoqueimado.com.br>");
            body.put("to", new String[]{emailDestino});
            body.put("subject", "Seus " + tickets.size() + " Tickets – " + nomeTicket);
            body.put("html", html.toString());
            body.put("attachments", attachments.toArray(new Map[0]));

            String json = mapper.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.resend.com/emails"))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("📨 Email de múltiplos tickets avulsos enviado!");

        } catch (Exception e) {
            throw new RuntimeException("Erro ao enviar múltiplos tickets avulsos: " + e.getMessage());
        }
    }

    public void sendPacoteTicketsEmail(
            String emailDestino,
            String nomeCliente,
            String telefone,
            String cpf,
            List<Ticket> tickets,
            List<byte[]> qrBytesList
    ) {
        try {
            if (tickets == null || tickets.isEmpty()) {
                throw new IllegalArgumentException("Lista de tickets vazia (pacote).");
            }
            if (qrBytesList == null) {
                throw new IllegalArgumentException("qrBytesList é nulo. Não há QRs para anexar.");
            }
            if (qrBytesList.size() < tickets.size()) {
                throw new IllegalArgumentException(String.format(
                        "Quantidade de QRs (%d) é menor que quantidade de tickets (%d).",
                        qrBytesList.size(), tickets.size()));
            }

            StringBuilder html = new StringBuilder();
            html.append("<h2>Seu Pacote Está Pronto 🎟️</h2>");
            html.append("<p>Olá <strong>").append(nomeCliente).append("</strong>,</p>");
            html.append("<p>Aqui estão seus <strong>").append(tickets.size()).append(" tickets</strong>.</p>");
            html.append("<p>Os QR Codes estão anexados a este e-mail.</p>");

            // 🔽 Adiciona a lista de locais
            html.append("<h3>O que você vai ter acesso:</h3>");
            html.append("<ul>");
            html.append("<li>🍕 Ticket Pizzaria Forno e Serra • Desconto de R$16</li>");
            html.append("<li>🏍️ Ticket RJ Off-Road • Desconto de R$25</li>");
            html.append("<li>🏡 Ticket Chalé Encantado • Desconto de R$50</li>");
            html.append("<li>☕ Ticket Bergkafee Café Colonial • Desconto de R$15</li>");
            html.append("<li>🥗 Ticket Da Roça • Desconto de R$10 a cada R$50 gasto</li>");
            html.append("<li>🌿 Ticket Espaço Floresta • Desconto de R$50</li>");
            html.append("<li>🍺 Ticket Bierhaus • 10% extra na compra</li>");
            html.append("<li>🌄 Ticket Mirante Boa Vista • Desconto de R$30</li>");
            html.append("<li>🍷 Ticket Goyah Vinhos • Desconto de R$14</li>");
            html.append("<li>🪵 Ticket Atafona (Aos finais de semana) • Desconto de R$10</li>");
            html.append("</ul>");

            List<Map<String, Object>> attachments = new ArrayList<>();
            for (int i = 0; i < tickets.size(); i++) {
                Ticket t = tickets.get(i);
                byte[] qrBytes = qrBytesList.get(i);
                String base64Qr = Base64.getEncoder().encodeToString(qrBytes);

                Map<String, Object> attachment = new HashMap<>();
                String filename = String.format("Pacote - %02d - %s.png", i + 1,
                        t.getNome() != null ? t.getNome().replaceAll("[^a-zA-Z0-9\\- ]", "") : "ticket");
                attachment.put("filename", filename);
                attachment.put("content", base64Qr);

                attachments.add(attachment);
            }

            Map<String, Object> body = new HashMap<>();
            body.put("from", "Guia Rancho Queimado <no-reply@guiaranchoqueimado.com.br>");
            body.put("to", new String[]{emailDestino});
            body.put("subject", "Seu Pacote de Tickets (" + tickets.size() + " unidades)");
            body.put("html", html.toString());
            body.put("attachments", attachments.toArray(new Map[0]));

            String json = mapper.writeValueAsString(body);
            System.out.println("DEBUG - JSON de envio (sendPacoteTicketsEmail): " + json);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.resend.com/emails"))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("📨 Email de pacote enviado!");

        } catch (Exception e) {
            throw new RuntimeException("Erro ao enviar email do pacote: " + e.getMessage(), e);
        }
    }

    public void enviarVerificacaoEmail(String emailDestino, String token) {
        try {
            String linkVerificacao = "https://guiaranchoqueimado.com.br/verificar?token=" + token;

            String html = """
                    <h2>Verifique sua conta 🔐</h2>
                    <p>Para ativar sua conta, clique no botão abaixo:</p>
                    <p>
                        <a href="%s" 
                           style="display:inline-block;padding:12px 20px;background:#4CAF50;color:white;
                                  text-decoration:none;border-radius:6px;font-weight:bold;">
                           Verificar Conta
                           </a>
                    </p>
                    <p>Se você não solicitou esta criação de conta, ignore este e-mail.</p>
                    """.formatted(linkVerificacao);

            Map<String, Object> body = new HashMap<>();
            body.put("from", "Guia Rancho Queimado <no-reply@guiaranchoqueimado.com.br>");
            body.put("to", new String[]{emailDestino});
            body.put("subject", "Confirmação de Conta – Guia RQ");
            body.put("html", html);

            String json = mapper.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.resend.com/emails"))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("📨 Email de verificação enviado!");
        } catch (Exception e) {
            throw new RuntimeException("Erro ao enviar email de verificação: " + e.getMessage(), e);
        }
    }
}