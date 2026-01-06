package br.com.guiarq.controller;

import br.com.guiarq.Model.Entities.Ticket;
import br.com.guiarq.Model.Repository.TicketRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Optional;

@RestController
@RequestMapping("/api/ticket")
public class TicketCheckController {

    @Autowired
    TicketRepository ticketRepository;

    @PostMapping
    public ResponseEntity<String> verifyTicket(@RequestParam String token) {
        Optional<Ticket> ticketOpt = ticketRepository.findByQrToken(token);

        if(ticketOpt.isEmpty()){
            return ResponseEntity.status(404).body("Ticket não encontrado");
        }
        Ticket ticket = ticketOpt.get();
        if(ticket.isUsado()){
            return ResponseEntity.status(409).body("Ticket usado");
        }
        ticket.setUsado(true);
        ticket.setUsadoEm(LocalDateTime.now());
        ticketRepository.save(ticket);

        return ResponseEntity.status(200).body("Ticket verificado com    sucesso");
    }
}
