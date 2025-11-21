package br.com.guiarq.controller;

import br.com.guiarq.Model.Entities.Ticket;
import br.com.guiarq.Model.Repository.TicketRepository;
import br.com.guiarq.Model.Service.TicketService;
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

    @PostMapping("/criar")
    public ResponseEntity<?> criar(@RequestBody Ticket ticket) {
        ticketService.salvar(ticket);
        return ResponseEntity.ok("Ticket criado com sucesso.");
    }

    @GetMapping("/listar")
    public ResponseEntity<?> listar() {
        return ResponseEntity.ok(ticketService.listarTodos());
    }

    @GetMapping("/por-email")
    public ResponseEntity<?> listarPorEmail(@RequestParam String email) {
        List<Ticket> tickets = ticketRepository.findByEmailCliente(email);
        return ResponseEntity.ok(tickets);
    }

    @GetMapping("/ver/{idPublico}")
    public ResponseEntity<?> verTicket(@PathVariable String idPublico) {

        try {
            UUID uuid = UUID.fromString(idPublico);

            return ticketRepository.findByIdPublico(uuid)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("ID inválido");
        }
    }
}
