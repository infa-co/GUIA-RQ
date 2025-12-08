package br.com.guiarq.controller;

import br.com.guiarq.Model.Entities.Ticket;
import br.com.guiarq.Model.Entities.TicketCatalogo;
import br.com.guiarq.Model.Repository.TicketCatalogoRepository;
import br.com.guiarq.Model.Repository.TicketRepository;
import br.com.guiarq.Model.Service.TicketService;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/stripe")
public class StripeWebhookController {

    private static final Logger logger = LoggerFactory.getLogger(StripeWebhookController.class);

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private TicketCatalogoRepository ticketCatalogoRepository;

    @Autowired
    private TicketService ticketService;

    @Value("${STRIPE_WEBHOOK_SECRET}")
    private String endpointSecret;

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(@RequestBody String payload) {

        logger.info("📩 Payload recebido: {}", payload);

        JSONObject json = new JSONObject(payload);
        String eventType = json.optString("type");

        if ("checkout.session.completed".equals(eventType)) {
            processCheckout(json);
        }

        return ResponseEntity.ok("OK");
    }

    private boolean clientePodeComprar(String cpf) {

        Ticket ultimo = ticketRepository.findTopByCpfClienteOrderByDataCompraDesc(cpf);

        if (ultimo == null) return true; // nunca comprou → pode comprar

        // limite de 3 meses
        LocalDateTime limite = ultimo.getDataCompra().plusMonths(3);

        return LocalDateTime.now().isAfter(limite);
    }


    private void processCheckout(JSONObject json) {

        JSONObject data = json.getJSONObject("data").getJSONObject("object");

        String sessionId = data.optString("id");
        if (sessionId == null || sessionId.isBlank()) {
            logger.error("❌ sessionId inválido");
            return;
        }

        // Evita duplicação do webhook
        if (ticketRepository.existsByStripeSessionId(sessionId)) {
            logger.warn("⚠️ Webhook duplicado ignorado para sessionId: {}", sessionId);
            return;
        }

        JSONObject metadata = data.optJSONObject("metadata");

        if (metadata == null) {
            logger.error("❌ Metadata vazio no checkout.session");
            return;
        }

        // Dados principais do cliente
        String email = metadata.optString("email");
        String nome = metadata.optString("nome");
        String telefone = metadata.optString("telefone");
        String cpf = metadata.optString("cpf");

        // Aqui corrigimos a duplicação
        String ticketIdStr = metadata.optString("ticketId", null);

        // Aqui está a regra CORRETA:
        boolean isPacote = "11".equals(ticketIdStr);

        if (email == null || nome == null) {
            logger.error("❌ Metadata insuficiente.");
            return;
        }

        if (isPacote) {
            processarPacote(sessionId, data, email, nome, telefone, cpf);
        } else {
            processarTicketAvulso(sessionId, data, email, nome, telefone, cpf, ticketIdStr);
        }
    }

    private void processarTicketAvulso(String sessionId,
                                       JSONObject data,
                                       String email,
                                       String nome,
                                       String telefone,
                                       String cpf,
                                       String ticketIdStr) {

        // --- Regra C: trava de 3 meses ---
        if (!clientePodeComprar(cpf)) {
            logger.warn("❌ Cliente com CPF " + cpf + " ainda não pode comprar novamente.");
            return;
        }


        Long ticketCatalogoId = null;
        String nomeTicket = "Guia Rancho Queimado - Ticket";

        try {
            ticketCatalogoId = Long.parseLong(ticketIdStr);

            Optional<TicketCatalogo> cat = ticketCatalogoRepository.findById(ticketCatalogoId);

            if (cat.isPresent()) {
                nomeTicket = cat.get().getNome();
            }

        } catch (Exception e) {
            logger.error("Erro ao buscar ticket catálogo: {}", e.getMessage());
        }

        // Criar ticket único
        Ticket ticket = new Ticket();
        ticket.setStripeSessionId(sessionId);
        ticket.setTicketCatalogoId(ticketCatalogoId);
        ticket.setNome(nomeTicket);

        ticket.setEmailCliente(email);
        ticket.setNomeCliente(nome);
        ticket.setTelefoneCliente(telefone);
        ticket.setCpfCliente(cpf);

        // --- Regra D: registrar data da compra ---
        ticket.setDataCompra(LocalDateTime.now());

        // gerar token único
        ticket.setIdPublico(UUID.randomUUID());
        ticket.setQrToken(UUID.randomUUID().toString());

        // salvar no banco
        ticket = ticketRepository.save(ticket);

        // disparar email
        ticketService.processarCompra(ticket);

        logger.info("✔ Ticket avulso criado e processado com sucesso");
    }
    private void processarPacote(String sessionId,
                                 JSONObject data,
                                 String email,
                                 String nome,
                                 String telefone,
                                 String cpf) {

        // --- Regra C: trava de 3 meses ---
        if (!clientePodeComprar(cpf)) {
            logger.warn("❌ Cliente com CPF " + cpf + " ainda não pode comprar pacote novamente.");
            return;
        }

        // O catálogo do pacote é sempre o ID 11
        Long ticketCatalogoId = 11L;

        Optional<TicketCatalogo> cat = ticketCatalogoRepository.findById(ticketCatalogoId);
        String nomeTicket = cat.map(TicketCatalogo::getNome)
                .orElse("Guia Rancho Queimado - Pacote Completo");

        // Criar o ticket principal
        Ticket ticket = new Ticket();
        ticket.setStripeSessionId(sessionId);
        ticket.setTicketCatalogoId(ticketCatalogoId);
        ticket.setNome(nomeTicket);

        ticket.setEmailCliente(email);
        ticket.setNomeCliente(nome);
        ticket.setTelefoneCliente(telefone);
        ticket.setCpfCliente(cpf);

        // --- Regra D: registrar data da compra ---
        ticket.setDataCompra(LocalDateTime.now());

        // tokens únicos
        ticket.setIdPublico(UUID.randomUUID());
        ticket.setQrToken(UUID.randomUUID().toString());

        // salvar no banco
        ticket = ticketRepository.save(ticket);

        // disparar email
        ticketService.processarCompra(ticket);

        logger.info("✔ Pacote processado com sucesso para CPF {}", cpf);
    }
}
