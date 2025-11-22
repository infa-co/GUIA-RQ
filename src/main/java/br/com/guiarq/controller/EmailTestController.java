package br.com.guiarq.controller;

import br.com.guiarq.Model.Service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/test/email")
public class EmailTestController {

    @Autowired
    private EmailService emailService;

    @PostMapping("/verificacao")
    public String testeVerificacao(@RequestParam String email) {
        emailService.enviarVerificacaoEmail(email, "TOKEN-DE-TESTE");
        return "E-mail de verificação enviado para: " + email;
    }

    @PostMapping("/ticket")
    public String testeTicket(@RequestParam String email) throws Exception {

        byte[] bytesFalsos = "qr-fake".getBytes();

        emailService.sendTicketEmail(email, "CODIGO-123456", bytesFalsos);
        return "E-mail de ticket enviado para: " + email;
    }
}
