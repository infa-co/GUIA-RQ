package br.com.guiarq.controller;

import br.com.guiarq.Model.Service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/email")
@CrossOrigin("*")
public class EmailTestController {

    @Autowired
    private EmailService emailService;

    @GetMapping("/teste")
    public String enviarTeste(@RequestParam String to) {
        emailService.enviarEmailTeste(to);
        return "Enviado para " + to;
    }
}
