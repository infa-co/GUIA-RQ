/**package br.com.guiarq;

import br.com.guiarq.Model.Service.EmailService;
import br.com.guiarq.Model.Service.QrCodeService;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;

public class TesteEmail {

    public static void main(String[] args) throws Exception {

        ApplicationContext ctx = SpringApplication.run(GuiaRQApplication.class, args);

        EmailService emailService = ctx.getBean(EmailService.class);
        QrCodeService qrCodeService = ctx.getBean(QrCodeService.class);

        // ======= TESTE 1 — Email simples =======
        emailService.enviarVerificacaoEmail(
                "tkmarlon594@gmail.com",
                "TOKEN-DE-TESTE"
        );

        System.out.println("✔ EMAIL DE VERIFICAÇÃO ENVIADO");

        // ======= TESTE 2 — Email com QR Code =======
        byte[] qr = qrCodeService.generateQrCodeBytes("TESTE-QRCODE", 300, 300);

        emailService.sendTicketEmail(
                "tkmarlon594@gmail.com",
                "TESTE-CODIGO",
                qr
        );

        System.out.println("✔ EMAIL COM QR CODE ENVIADO");
    }
}
**/