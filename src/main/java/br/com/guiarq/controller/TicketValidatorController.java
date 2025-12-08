package br.com.guiarq.controller;

import br.com.guiarq.Model.Entities.Ticket;
import br.com.guiarq.Model.Repository.TicketRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/tickets")
public class TicketValidatorController {

    @Autowired
    private TicketRepository ticketRepository;

    // ==========================================================
    // 1) VALIDAR (carrega informações do ticket)
    // ==========================================================
    @GetMapping("/validar-ticket/{qr}")
    public ResponseEntity<?> validar(@PathVariable String qr) {

        return ticketRepository.findByQrToken(qr)
                .map(ticket -> ResponseEntity.ok(
                        Map.of(
                                "status", "VALIDO",
                                "cliente", ticket.getNomeCliente(),
                                "ticket", ticket.getNome(),
                                "usado", ticket.isUsado(),
                                "usosRestantes", ticket.getUsosRestantes(),
                                "tipoPacote", ticket.getTipoPacote()
                        )
                ))
                .orElse(ResponseEntity.status(404).body(
                        Map.of("status", "INVALIDO")
                ));
    }

    // ==========================================================
    // 2) CONFIRMAR (grava no banco os dados do proprietário)
    // ==========================================================
    @PostMapping("/confirmar/{qr}")
    public ResponseEntity<?> confirmar(
            @PathVariable String qr,
            @RequestBody Map<String, String> dadosValidacao
    ) {

        return ticketRepository.findByQrToken(qr)
                .map(ticket -> {

                    boolean ePacote = ticket.getTipoPacote() != null && ticket.getTipoPacote();
                    Integer usosRestantes = ticket.getUsosRestantes() == null ? 0 : ticket.getUsosRestantes();

                    // ================================
                    // CASO 1 → Ticket INDIVIDUAL
                    // ================================
                    if (!ePacote) {

                        if (ticket.isUsado()) {
                            return ResponseEntity.status(409).body("Ticket já utilizado");
                        }

                        ticket.setUsado(true);
                        ticket.setUsadoEm(LocalDateTime.now());
                    }

                    // ================================
                    // CASO 2 → Ticket PACOTE (multi-uso)
                    // ================================
                    else {

                        if (usosRestantes <= 0) {
                            return ResponseEntity.status(409).body("Todos os usos já foram consumidos");
                        }

                        // reduz 1 uso
                        usosRestantes = usosRestantes - 1;
                        ticket.setUsosRestantes(usosRestantes);

                        // se zerou → marcar como usado
                        if (usosRestantes == 0) {
                            ticket.setUsado(true);
                            ticket.setUsadoEm(LocalDateTime.now());
                        }
                    }

                    // =======================================
                    // Salvando os dados do comércio
                    // =======================================
                    ticket.setClienteUso(dadosValidacao.get("clienteUso"));
                    ticket.setEstabelecimentoValidacao(dadosValidacao.get("estabelecimento"));
                    ticket.setValidadoPor(dadosValidacao.get("validadoPor"));

                    ticket.setDataCompra(LocalDateTime.now());
                    ticketRepository.save(ticket);

                    return ResponseEntity.ok(
                            ePacote
                                    ? "Uso registrado! Restam: " + ticket.getUsosRestantes()
                                    : "Ticket confirmado"
                    );

                })
                .orElse(ResponseEntity.status(404).body("Ticket não encontrado"));
    }
}