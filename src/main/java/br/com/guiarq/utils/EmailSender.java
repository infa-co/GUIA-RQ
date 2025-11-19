package br.com.guiarq.utils;

import com.resend.Resend;
import com.resend.services.emails.model.Attachment;
import com.resend.services.emails.model.CreateEmailOptions;
import java.util.Base64;

import java.util.Arrays;

public class EmailSender {

    private final Resend resend = new Resend("SUA_API_KEY");

    public void enviarQRCode(String emailDestino, String assunto, String mensagem, byte[] qrCodeBytes) {

        try {
            // cria o anexo do QR em PNG
            Attachment qrAttachment = Attachment.builder()
                    .fileName("ticket.png")
                    .content(Base64.getEncoder().encodeToString(qrCodeBytes))
                    .build();

            String html = """
                    <div style="font-family: Arial, sans-serif; padding: 20px;">
                        <h2>Seu Ticket está Pronto!</h2>
                        <p>%s</p>
                        
                        <p>Apresente este QR Code na entrada do evento/passeio:</p>
                        <img src="cid:ticket.png" style="width: 220px; height: 220px;" />
                        
                        <br><br>
                        <p>Obrigado pela sua compra!</p>
                        <p><strong>Guia Rancho Queimado</strong></p>
                    </div>
                """.formatted(mensagem);

            CreateEmailOptions params = CreateEmailOptions.builder()
                    .from("Ingressos Guia RQ <noreply@SEU_DOMINIO.com>")
                    .to(emailDestino)
                    .subject(assunto)
                    .html(html)
                    .addAttachment(qrAttachment)
                    .build();

            resend.emails().send(params);

        } catch (Exception e) {
            throw new RuntimeException("Erro ao enviar email com QR Code", e);
        }
    }
}
