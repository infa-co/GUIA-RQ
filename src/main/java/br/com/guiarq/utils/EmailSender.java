/**package br.com.guiarq.utils;

import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;

@Component
public class EmailSender {

    @Autowired
    private JavaMailSender mailSender;

    public void sendTicketEmail(String to, String nome, byte[] qrImage, String qrToken) throws Exception {

        MimeMessage msg = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(msg, true);

        helper.setTo(to);
        helper.setSubject("Seu Ticket - Guia Rancho Queimado");
        helper.setText(
                "Olá " + nome + ",\n\n" +
                        "Aqui está seu ticket " + qrToken + ".\n\n" +
                        "Apresente este QR Code na entrada.\n\n" +
                        "Equipe Guia RQ",
                false
        );

        helper.addAttachment("ticket.png", () -> new ByteArrayInputStream(qrImage));

        mailSender.send(msg);

        System.out.println("📨 EMAIL ENVIADO PARA " + to);
    }
}
**/