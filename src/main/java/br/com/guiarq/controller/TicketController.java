package br.com.guiarq.controller;

import br.com.guiarq.Model.Entities.Ticket;
import br.com.guiarq.Model.Service.TicketService;
import br.com.guiarq.Model.Repository.TicketRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    @Autowired
    private TicketService ticketService;

    @Autowired
    private TicketRepository ticketRepository;

    // ============================
    // CRIAR
    // ============================
    @PostMapping("/criar")
    public ResponseEntity<?> criar(@RequestBody Ticket ticket) {
        ticketService.salvar(ticket);
        return ResponseEntity.ok("Ticket criado com sucesso.");
    }

    // ============================
    // LISTAR
    // ============================
    @GetMapping("/listar")
    public ResponseEntity<?> listar() {
        return ResponseEntity.ok(ticketService.listarTodos());
    }

    // ============================
    // VERIFICAR TICKET POR UUID
    // ============================
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

    // ============================
    // CONFIRMAR VALIDAÇÃO
    // ============================
    @PostMapping("/confirmar/{idPublico}")
    public ResponseEntity<?> confirmar(@PathVariable String idPublico) {
        try {
            UUID id = UUID.fromString(idPublico);
            ticketService.confirmar(id);
            return ResponseEntity.ok("Ticket validado com sucesso");

        } catch (Exception e) {
            return ResponseEntity.status(404).body("Ticket inválido ou já usado");
        }
    }
    public Ticket salvar(Ticket ticket) {

        // Gerar ID público se não existir
        if (ticket.getIdPublico() == null) {
            ticket.setIdPublico(UUID.randomUUID());
        }

        // Gerar QR token se não existir
        if (ticket.getQrToken() == null) {
            ticket.setQrToken(UUID.randomUUID().toString());
        }

        // Criar timestamp se não existir
        if (ticket.getCriadoEm() == null) {
            ticket.setCriadoEm(java.time.LocalDateTime.now());
        }

        return ticketRepository.save(ticket);
    }
}
