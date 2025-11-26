package br.com.guiarq.controller;

import br.com.guiarq.Model.Repository.TicketRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
                        "status", ticket.isUsado() ? "USADO" : "VALIDO",
                        "cliente", ticket.getNomeCliente(),
                        "ticket", ticket.getNome(),
                        "usado", ticket.isUsado()
                )))
                .orElse(ResponseEntity.status(404).body(Map.of(
                        "status", "INVALIDO"
                )));
    }

    @PostMapping("/confirmar/{qr}")
    public ResponseEntity<?> confirmar(@PathVariable String qr) {
        return ticketRepository.findByQrToken(qr)
                .map(ticket -> {

                    if (ticket.isUsado()) {
                        return ResponseEntity.status(409)
                                .body(Map.of("status", "USADO"));
                    }

                    ticket.setUsado(true);
                    ticket.setUsadoEm(java.time.LocalDateTime.now());
                    ticketRepository.save(ticket);

                    return ResponseEntity.ok(Map.of("status", "OK"));
                })
                .orElse(ResponseEntity.status(404).body(Map.of(
                        "status", "INVALIDO"
                )));
    }
}
