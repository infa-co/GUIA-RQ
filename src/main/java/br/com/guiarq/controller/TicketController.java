package br.com.guiarq.controller;

import br.com.guiarq.Model.Entities.Ticket;
import br.com.guiarq.Model.Service.TicketService;
import br.com.guiarq.Model.Repository.TicketRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    @Autowired
    private TicketService ticketService;

    @Autowired
    private TicketRepository ticketRepository;

    @PostMapping("/criar")
    public ResponseEntity<?> criar(@RequestBody Ticket ticket) {
        Ticket salvo = ticketService.salvar(ticket);
        return ResponseEntity.ok(salvo);
    }

    @GetMapping("/listar")
    public ResponseEntity<?> listar() {
        return ResponseEntity.ok(ticketService.listarTodos());
    }

    @GetMapping("/ver/{idPublico}")
    public ResponseEntity<?> verificar(@PathVariable String idPublico) {
        try {
            UUID id = UUID.fromString(idPublico);
            Ticket ticket = ticketService.verificar(id);
            return ResponseEntity.ok(ticket);
        } catch (Exception e) {
            return ResponseEntity.status(404).body("Ticket inválido");
        }
    }

    @PostMapping("/confirmar/{idPublico}")
    public ResponseEntity<?> confirmarUso(@PathVariable String idPublico) {
        try {
            Ticket ticket = ticketService.confirmarUso(idPublico);
            UUID id = UUID.fromString(idPublico);
            ticketService.confirmarUso(idPublico);
            Map<String, Object> resp = new HashMap<>();
            resp.put("nomeCliente", ticket.getNomeCliente());
            resp.put("nomeTicket", ticket.getNome());
            resp.put("usado", ticket.getUsado());

            return ResponseEntity.ok(resp);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body("Ticket já utilizado");
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body("Ticket inválido");
        }
    }
}
