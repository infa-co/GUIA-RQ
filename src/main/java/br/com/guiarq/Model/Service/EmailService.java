package br.com.guiarq.Model.Service;

import br.com.guiarq.Model.Entities.Ticket;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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

    // ==========================================
    // PACOTE – ENVIO DE 10 QR CODES
    // ==========================================
    public void sendPacoteTicketsEmail(
            String emailDestino,
            String nomeCliente,
            String telefone,
            String cpf,
            List<Ticket> tickets,
            List<byte[]> qrBytesList
    ) {

        try {
            StringBuilder html = new StringBuilder();

            html.append("<h2>Seu Pacote Guia RQ está pronto 🎒</h2>");
            html.append("<p>Olá <strong>")
                    .append(nomeCliente)
                    .append("</strong>,</p>");

            html.append("""
    <p>Você adquiriu o <strong>Pacote Guia RQ</strong>, que dá acesso a <strong>10 experiências em Rancho Queimado</strong>.</p>

    <p style='background:#f0f7ff;padding:10px;border-left:4px solid #3b82f6;margin:15px 0;'>
        <strong>Importante:</strong><br>
        Este pacote utiliza <strong>APENAS 1 QR Code</strong>, válido para <strong>10 validações</strong>.<br>
        Cada validação corresponde ao uso de um ticket em um estabelecimento diferente.
    </p>

    <p><strong>Experiências incluídas:</strong></p>
    <ul>
        <li>🍕 Ticket Pizzaria Forno e Serra — Desconto de R$16</li>
        <li>🛵 Ticket RJ Off-Road — Desconto de R$25</li>
        <li>🏡 Ticket Chalé Encantado — Desconto de R$50</li>
        <li>🍰 Ticket Bergkaffee Café Colonial — Desconto de R$15</li>
        <li>🍽️ Ticket Da Roça — Desconto de R$10 a cada R$50 gasto</li>
        <li>🌿 Ticket Espaço Floresta — Desconto de R$50</li>
        <li>🍺 Ticket Bierhaus — 10% extra na compra</li>
        <li>📸 Ticket Mirante Boa Vista — Desconto de R$30</li>
        <li>🍷 Ticket Goyah Vinhos — Desconto de R$14</li>
        <li>🍖 Ticket Atafona — Desconto de R$10 (aos finais de semana)</li>
    </ul>

    <p style="margin-top:15px;">
        📎 O QR Code do pacote está anexado a este e-mail.<br>
        Você poderá utilizá-lo <strong>10 vezes</strong>, uma para cada experiência.
    </p>
""");

            List<Map<String, Object>> attachments = new ArrayList<>();

            for (int i = 0; i < tickets.size(); i++) {
                Ticket t = tickets.get(i);
                byte[] qrBytes = qrBytesList.get(i);
                String base64Qr = Base64.getEncoder().encodeToString(qrBytes);

                Map<String, Object> attachment = new HashMap<>();
                attachment.put("filename", "Ticket - " + t.getNome() + ".png");
                attachment.put("content", base64Qr);

                attachments.add(attachment);
            }

            Map<String, Object> body = new HashMap<>();
            body.put("from", "Guia Rancho Queimado <no-reply@guiaranchoqueimado.com.br>");
            body.put("to", new String[]{emailDestino});
            body.put("subject", "Seu Pacote Guia RQ – " + tickets.size() + " tickets");
            body.put("html", html.toString());
            body.put("attachments", attachments); // ← AQUI A CORREÇÃO

            String json = mapper.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.resend.com/emails"))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

        } catch (Exception e) {
            throw new RuntimeException("Erro ao enviar email de pacote: " + e.getMessage());
        }
    }

    // ==========================================
    // TICKET ÚNICO
    // ==========================================
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
                <p>Ticket: <strong>%s</strong></p>
                <p>Telefone: %s<br>CPF: %s</p>
                <p>Seu QR Code está anexado.</p>
                """.formatted(nomeCliente, nomeTicket, telefone, cpf);

            String qrBase64 = Base64.getEncoder().encodeToString(qrBytes);

            Map<String, Object> attachment = new HashMap<>();
            attachment.put("filename", "ticket.png");
            attachment.put("content", qrBase64);
            attachment.put("type", "image/png");

            Map<String, Object> body = new HashMap<>();
            body.put("from", "Guia Rancho Queimado <no-reply@guiaranchoqueimado.com.br>");
            body.put("to", new String[]{emailDestino});
            body.put("subject", "Seu Ticket – " + nomeTicket);
            body.put("html", html);
            body.put("attachments", List.of(attachment));

            String json = mapper.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.resend.com/emails"))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("📨 Email enviado com sucesso!");

        } catch (Exception e) {
            throw new RuntimeException("Erro ao enviar email: " + e.getMessage());
        }
    }


    // ==========================================
    // EMAIL DE VERIFICAÇÃO
    // ==========================================
    public void enviarVerificacaoEmail(String emailDestino, String token) {

        try {
            String link = baseUrl + "/api/auth/verify?token=" + token;

            String html = """
                <h2>Confirme seu e-mail</h2>
                <p>Clique no link abaixo:</p>
                <a href='%s'>Confirmar e-mail</a>
                """.formatted(link);

            Map<String, Object> body = new HashMap<>();
            body.put("from", "Guia Rancho Queimado <no-reply@guiaranchoqueimado.com.br>");
            body.put("to", new String[]{emailDestino});
            body.put("subject", "Confirme seu e-mail");
            body.put("html", html);

            String json = mapper.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.resend.com/emails"))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

        } catch (Exception e) {
            throw new RuntimeException("Erro ao enviar e-mail de verificação: " + e.getMessage());
        }
    }
}