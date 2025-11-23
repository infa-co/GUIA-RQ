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

            String html = """
                    <table width='100%%' cellspacing='0' cellpadding='0' style='background-color:#f5f5f5; padding:40px 0; font-family:Arial, Helvetica, sans-serif;'>
                        <tr>
                            <td align='center'>
                                <table width='480' cellspacing='0' cellpadding='0'
                                       style='background-color:#ffffff; border-radius:12px; overflow:hidden; box-shadow:0 4px 14px rgba(0,0,0,0.08);'>

                                    <tr>
                                        <td style='background-color:#0d47a1; padding:24px; text-align:center;'>
                                            <img src='https://guiaranchoqueimado.com.br/assets/images/guia-rancho-queimado-logo-sem-fundo.png'
                                                 alt='Logo' width='140' style='display:block; margin:auto;'>
                                            <h2 style='color:#ffffff; margin-top:16px; margin-bottom:0; font-size:22px;'>Confirme seu e-mail</h2>
                                        </td>
                                    </tr>

                                    <tr>
                                        <td style='padding:28px; color:#333; font-size:15px;'>
                                            <p>Obrigado por criar sua conta no <strong>Guia Rancho Queimado</strong>.</p>
                                            <p>Clique no botão abaixo para ativar seu e-mail:</p>

                                            <p style='text-align:center; margin:32px 0;'>
                                                <a href='%s' style='background-color:#0d47a1; color:white; padding:14px 28px; border-radius:6px; text-decoration:none; font-size:16px;'>
                                                    Confirmar e-mail
                                                </a>
                                            </p>

                                            <p style='font-size:13px; color:#777;'>Se não foi você quem criou a conta, ignore este e-mail.</p>
                                        </td>
                                    </tr>

                                    <tr>
                                        <td style='background-color:#fafafa; text-align:center; padding:18px; font-size:12px; color:#999;'>
                                            © 2025 Guia Rancho Queimado. Todos os direitos reservados.
                                        </td>
                                    </tr>

                                </table>
                            </td>
                        </tr>
                    </table>
                    """.formatted(link);

            String json = """
                    {
                      "from": "no-reply@guiaranchoqueimado.com.br",
                      "to": ["%s"],
                      "subject": "Confirme seu e-mail",
                      "html": "%s"
                    }
                    """.formatted(emailDestino, html.replace("\"", "\\\""));

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

            String html = """
                    <table width='100%%' cellspacing='0' cellpadding='0' style='background-color:#f5f5f5; padding:40px 0; font-family:Arial, Helvetica, sans-serif;'>
                        <tr>
                            <td align='center'>
                                <table width='480' cellspacing='0' cellpadding='0'
                                       style='background-color:#ffffff; border-radius:12px; overflow:hidden; box-shadow:0 4px 14px rgba(0,0,0,0.08);'>

                                    <tr>
                                        <td style='background-color:#0d47a1; padding:24px; text-align:center;'>
                                            <img src='https://guiaranchoqueimado.com.br/assets/images/guia-rancho-queimado-logo-sem-fundo.png'
                                                 alt='Logo' width='140' style='display:block; margin:auto;'>
                                            <h2 style='color:#ffffff; margin-top:16px; margin-bottom:0; font-size:22px;'>Seu Ticket Está Pronto 🎟️</h2>
                                        </td>
                                    </tr>

                                    <tr>
                                        <td style='padding:28px; color:#333; font-size:15px;'>

                                            <p>Aqui está seu código de validação:</p>

                                            <p style='font-size:20px; font-weight:bold; color:#0d47a1; text-align:center; margin:20px 0;'>
                                                %s
                                            </p>

                                            <p>Seu QR Code está anexado a este e-mail.</p>

                                        </td>
                                    </tr>

                                    <tr>
                                        <td style='background-color:#fafafa; text-align:center; padding:18px; font-size:12px; color:#999;'>
                                            © 2025 Guia Rancho Queimado. Todos os direitos reservados.
                                        </td>
                                    </tr>

                                </table>
                            </td>
                        </tr>
                    </table>
                    """.formatted(codigo);

            String json = """
                    {
                      "from": "no-reply@guiaranchoqueimado.com.br",
                      "to": ["%s"],
                      "subject": "Seu Ticket – Guia Rancho Queimado",
                      "html": "%s",
                      "attachments": [
                        {
                          "filename": "ticket-qrcode.png",
                          "content": "%s"
                        }
                      ]
                    }
                    """
                    .formatted(emailDestino, html.replace("\"", "\\\""), base64Qr);

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
