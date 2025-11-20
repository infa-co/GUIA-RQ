package br.com.guiarq.utils;

import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
public class EmailSender {

    @Autowired
    private JavaMailSender mailSender;

    public void sendTicketEmail(String to, String nome, byte[] qrImage, String qrToken) throws Exception {

        MimeMessage msg = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(msg, true);

        helper.setTo(to);
        helper.setSubject("Seu Ticket - Guia RQ");
        helper.setText("Olá " + nome + ",\n\nAqui está seu ticket.\n\nApresente este QR Code na entrada.");

        helper.addAttachment("ticket.png", () -> new java.io.ByteArrayInputStream(qrImage));

        mailSender.send(msg);

        System.out.println("📨 EMAIL ENVIADO PARA " + to);
    }
}
