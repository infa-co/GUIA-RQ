package br.com.guiarq.controller;

import br.com.guiarq.Model.Repository.TicketRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/tickets")
public class TicketValidatorController {

    @Autowired
    private TicketRepository ticketRepository;

    @GetMapping("/validar/{qr}")
    public ResponseEntity<?> validar(@PathVariable String qr) {

        return ticketRepository.findByQrToken(qr)
                .map(ticket -> ResponseEntity.ok(Map.of(
                        "status", "VALIDO",
                        "cliente", ticket.getNomeCliente(),
                        "ticket", ticket.getNome(),
                        "usado", ticket.isUsado()
                )))
                .orElse(ResponseEntity.status(404).body(Map.of(
                        "status", "INVALIDO"
                )));
    }
}
