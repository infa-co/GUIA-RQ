package br.com.guiarq.Model.Service;

import com.resend.Resend;
import com.resend.services.emails.Emails;
import com.resend.services.emails.model.Attachment;
import com.resend.services.emails.model.CreateEmailOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.List;

@Service
public class EmailService {

    @Value("${RESEND_API_KEY}")
    private String resendApiKey;

    @Value("${EMAIL_USERNAME}")
    private String emailFrom;


    public void sendTicketEmail(String to, String codigoValidacao, byte[] qrCodeBytes) {

        Resend resend = new Resend(resendApiKey);

        String base64Qr = Base64.getEncoder().encodeToString(qrCodeBytes);

        Attachment attachment = Attachment.builder()
                .fileName("ticket.png")
                .content(base64Qr)
                .build();

        CreateEmailOptions params = CreateEmailOptions.builder()
                .from(emailFrom)
                .to(to)
                .subject("Seu Ticket – Guia RQ")
                .html(
                        "<h2>Seu ticket está pronto! 🎉</h2>" +
                                "<p><b>Código de validação:</b> " + codigoValidacao + "</p>" +
                                "<p>Use o QR Code em anexo para validar sua experiência.</p>"
                )
                .attachments(List.of(attachment))
                .build();

        try {
            resend.emails().send(params);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Erro ao enviar email via Resend", e);
        }
    }
}
