package br.com.guiarq.controller;

import br.com.guiarq.Model.Service.EmailService;
import br.com.guiarq.Model.Service.QrCodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/test/email")
public class EmailTestController {

    @Autowired
    private EmailService emailService;

    @Autowired
    private QrCodeService qrCodeService;

    @GetMapping("/verificacao")
    public String testVerificacao(@RequestParam String email) {
        emailService.enviarVerificacaoEmail(email, "TOKEN-DE-TESTE");
        return "OK - Verificação enviada para " + email;
    }

    @GetMapping("/ticket")
    public String testTicket(@RequestParam String email) {
        try {
            byte[] qr = qrCodeService.generateQrCodeBytes("TESTE-QR", 300, 300);

            emailService.sendTicketEmail(
                    email,
                    "TESTE-CODIGO",
                    qr
            );

            return "OK - Ticket enviado para " + email;

        } catch (Exception e) {
            return "ERRO ao enviar ticket: " + e.getMessage();
        }
    }
}
