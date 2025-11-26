package br.com.guiarq.controller;

import br.com.guiarq.Model.Entities.Ticket;
import br.com.guiarq.Model.Repository.TicketRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/tickets")
public class TicketValidatorController {

    @Autowired
    private TicketRepository ticketRepository;

    // 🔍 Validar ticket por QR Token
    @GetMapping("/validar/{qr}")
    public ResponseEntity<?> validar(@PathVariable String qr) {

        return ticketRepository.findByQrToken(qr)
                .map(ticket -> ResponseEntity.ok(
                        java.util.Map.of(
                                "status", "VALIDO",
                                "cliente", ticket.getNomeCliente(),
                                "ticket", ticket.getNome(),
                                "usado", ticket.isUsado()
                        )
                ))
                .orElse(ResponseEntity.status(404).body(
                        java.util.Map.of(
                                "status", "INVALIDO"
                        )
                ));
    }

    // ✅ Confirmar ticket
    @PostMapping("/confirmar/{qr}")
    public ResponseEntity<?> confirmar(@PathVariable String qr) {

        return ticketRepository.findByQrToken(qr)
                .map(ticket -> {

                    if (ticket.isUsado()) {
                        return ResponseEntity.status(409).body("Ticket já utilizado");
                    }

                    ticket.setUsado(true);
                    ticket.setUsadoEm(LocalDateTime.now());
                    ticketRepository.save(ticket);

                    return ResponseEntity.ok("Ticket confirmado");
                })
                .orElse(ResponseEntity.status(404).body("Ticket não encontrado"));
    }
}
