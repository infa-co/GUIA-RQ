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
            html.append("<p>Você recebeu <strong>")
                    .append(tickets.size())
                    .append(" tickets individuais</strong>. Cada um pode ser utilizado separadamente nos estabelecimentos abaixo:</p>");
            html.append("<ul>");
            for (Ticket t : tickets) {
                html.append("<li>")
                        .append(t.getNome())
                        .append("</li>");
            }
            html.append("</ul>");
            html.append("<p>Os QR Codes de cada ticket estão anexados a este e-mail.</p>");

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
            body.put("attachments", attachments.toArray());

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
                <p>Seu ticket: <strong>%s</strong></p>
                <p>Telefone: %s<br>CPF: %s</p>
                <p>Seu QR Code está anexado a este e-mail.</p>
                """.formatted(nomeCliente, nomeTicket, telefone, cpf);

            String qrBase64 = Base64.getEncoder().encodeToString(qrBytes);

            Map<String, Object> attachment = new HashMap<>();
            attachment.put("filename", "ticket.png");
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

            HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("📨 Email enviado com sucesso!");

        } catch (Exception e) {
            throw new RuntimeException("Erro ao enviar email: " + e.getMessage());
        }
    }
    public void enviarVerificacaoEmail(String emailDestino, String token) {

        try {
            String link = baseUrl + "/api/auth/verify?token=" + token;

            String html = """
                <h2>Confirme seu e-mail</h2>
                <p>Clique abaixo para ativar sua conta:</p>
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

            System.out.println("📨 E-mail de verificação enviado!");

        } catch (Exception e) {
            throw new RuntimeException("Erro ao enviar email de verificação: " + e.getMessage());
        }
    }

}
