package br.com.guiarq.Model.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
public class EmailService {

    @Value("${RESEND_API_KEY}")
    private String apiKey;

    @Value("${API_URL}")
    private String baseUrl;

    private final ObjectMapper mapper = new ObjectMapper();

    // ----------------------------------------------------
    // ENVIO EMAIL DE VERIFICAÇÃO
    // ----------------------------------------------------
    public void enviarVerificacaoEmail(String emailDestino, String token) {

        try {
            String link = baseUrl + "/api/auth/verify?token=" + token;

            String html = """
                    <h2>Confirme seu e-mail</h2>
                    <p>Clique no botão abaixo para ativar sua conta:</p>
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
            throw new RuntimeException("Erro ao enviar email: " + e.getMessage());
        }
    }

    // ----------------------------------------------------
    // ENVIO EMAIL DO TICKET + QR CODE
    // ----------------------------------------------------
    public void sendTicketEmail(
            String emailDestino,
            String nomeCliente,
            String telefone,
            String cpf,
            String nomeTicket,
            byte[] qrBytes
    ) {

        try {
            String base64Qr = Base64.getEncoder().encodeToString(qrBytes);

            String html = """
                    <h2>Seu Ticket Está Pronto 🎟️</h2>
                    <p>Olá <strong>%s</strong>,</p>
                    <p>Seu ticket: <strong>%s</strong></p>
                    <p>Telefone: %s</p>
                    <p>CPF: %s</p>
                    <p>Seu QR Code está anexado a este e-mail.</p>
                    """.formatted(nomeCliente, nomeTicket, telefone, cpf);

            Map<String, Object> attachment = new HashMap<>();
            attachment.put("filename", "ticket-qrcode.png");
            attachment.put("content", base64Qr);

            Map<String, Object> body = new HashMap<>();
            body.put("from", "Guia Rancho Queimado <no-reply@guiaranchoqueimado.com.br>");
            body.put("to", new String[]{emailDestino});
            body.put("subject", "Seu Ticket – Guia Rancho Queimado");
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

        } catch (Exception e) {
            throw new RuntimeException("Erro ao enviar ticket: " + e.getMessage());
        }
    }
}
