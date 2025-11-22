package br.com.guiarq.Model.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
public class EmailService {

    @Value("${RESEND_API_KEY}")
    private String apiKey;

    @Value("${APP_BASE_URL}")
    private String baseUrl;

    // -----------------------------
    // ENVIO DE EMAIL DE VERIFICAÇÃO
    // -----------------------------
    public void enviarVerificacaoEmail(String emailDestino, String token) {

        try {
            String link = baseUrl + "/api/auth/verify?token=" + token;

            String json = """
                    {
                      "from": "no-reply@guiaranchoqueimado.com.br",
                      "to": ["%s"],
                      "subject": "Confirme seu e-mail",
                      "html": "<h2>Confirme seu e-mail</h2><p>Clique no link abaixo:</p><a href='%s'>Confirmar e-mail</a>"
                    }
                    """.formatted(emailDestino, link);

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


    // -----------------------------
    // ENVIO DO TICKET COM QR CODE
    // -----------------------------
    public void sendTicketEmail(String emailDestino, String codigo, byte[] qrBytes) {
        try {
            String base64Qr = java.util.Base64.getEncoder().encodeToString(qrBytes);

            String json = """
                    {
                      "from": "no-reply@guiaranchoqueimado.com.br",
                      "to": ["%s"],
                      "subject": "Seu Ticket – Guia Rancho Queimado",
                      "html": "<h2>Seu Ticket</h2><p>Código: %s</p>",
                      "attachments": [
                        {
                          "filename": "ticket-qrcode.png",
                          "content": "%s"
                        }
                      ]
                    }
                    """.formatted(emailDestino, codigo, base64Qr);

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
