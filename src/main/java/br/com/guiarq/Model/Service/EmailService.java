package br.com.guiarq.Model.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
public class EmailService {

    @Value("${RESEND_API_KEY}")
    private String apiKey;

    @Value("${APP_BASE_URL}")
    private String baseUrl;

    private final String RESEND_URL = "https://api.resend.com/emails";

    private RestTemplate rest = new RestTemplate();


    // -----------------------------
    // EMAIL DE VERIFICAÇÃO
    // -----------------------------
    public void enviarVerificacaoEmail(String emailDestino, String token) {

        String link = baseUrl + "/api/auth/verify?token=" + token;

        String html = "<h2>Confirme seu e-mail</h2>" +
                "<p>Clique no link abaixo para ativar sua conta:</p>" +
                "<a href='" + link + "'>Confirmar e-mail</a>";

        Map<String, Object> body = new HashMap<>();
        body.put("from", "no-reply@guiaranchoqueimado.com.br");
        body.put("to", emailDestino);
        body.put("subject", "Confirme seu e-mail");
        body.put("html", html);

        enviar(body);
    }


    // -----------------------------
    // ENVIO DO TICKET COM QR CODE
    // -----------------------------
    public void sendTicketEmail(String emailDestino, String codigo, byte[] qrBytes) {

        String html = "<h2>Seu Ticket – Guia Rancho Queimado</h2>" +
                "<p><strong>Código:</strong> " + codigo + "</p>" +
                "<p>O QR Code está anexado neste e-mail.</p>";

        Map<String, Object> attachment = new HashMap<>();
        attachment.put("filename", "ticket.png");
        attachment.put("content", Base64.getEncoder().encodeToString(qrBytes));

        Map<String, Object> body = new HashMap<>();
        body.put("from", "no-reply@guiaranchoqueimado.com.br");
        body.put("to", emailDestino);
        body.put("subject", "Seu Ticket – Guia Rancho Queimado");
        body.put("html", html);
        body.put("attachments", new Object[]{attachment});

        enviar(body);
    }
    private void enviar(Map<String, Object> body) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<String> response =
                rest.exchange(RESEND_URL, HttpMethod.POST, entity, String.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Erro ao enviar e-mail: " + response.getBody());
        }
    }
}
