package br.com.guiarq.Model.Service;

import com.resend.Resend;
import com.resend.services.emails.Emails;
import com.resend.services.emails.model.CreateEmailOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Value("${RESEND_API_KEY}")
    private String apiKey;

    @Value("${APP_BASE_URL}")
    private String baseUrl;

    public void enviarVerificacaoEmail(String emailDestino, String token) {

        try {
            Resend resend = new Resend(apiKey);

            String link = baseUrl + "/api/auth/verify?token=" + token;

            String conteudoHtml =
                    "<h2>Confirme seu e-mail</h2>" +
                            "<p>Clique no link abaixo para ativar sua conta:</p>" +
                            "<a href=\"" + link + "\">Confirmar e-mail</a>" +
                            "<br><br>" +
                            "<p>Se você não criou uma conta, ignore esta mensagem.</p>";

            CreateEmailOptions params = CreateEmailOptions.builder()
                    .from("gropoguiarq@gmail.com")  //ALTERAR DPS para .to("gropoguiarq@gmail.com")
                    .to("gropoguiarq@gmail.com")
                    .subject("Confirme seu e-mail")
                    .html(conteudoHtml)
                    .build();

            resend.emails().send(params);

        } catch (Exception e) {
            throw new RuntimeException("Erro ao enviar e-mail: " + e.getMessage());
        }
    }
    public void sendTicketEmail(String emailDestino, String nome, byte[] pdfBytes) {
        System.out.println("sendTicketEmail chamado, mas ainda não implementado com Resend.");
    }

}
