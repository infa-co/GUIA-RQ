package br.com.guiarq.controller;

import br.com.guiarq.Model.Entities.Ticket;
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
import java.util.UUID;

@RestController
@RequestMapping("/api/stripe")
public class StripeWebhookController {

    private static final Logger logger = LoggerFactory.getLogger(StripeWebhookController.class);

    @Autowired
    private TicketRepository ticketRepository;

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

    private void processCheckout(JSONObject json) {

        JSONObject data = json.getJSONObject("data").getJSONObject("object");

        String sessionId = data.optString("id");

        if (sessionId == null || sessionId.isBlank()) {
            logger.error("❌ sessionId inválido no webhook.");
            return;
        }

        if (ticketRepository.existsByStripeSessionId(sessionId)) {
            logger.warn("⚠️ Webhook duplicado ignorado para sessionId: {}", sessionId);
            return;
        }

        JSONObject metadata = data.optJSONObject("metadata");

        if (metadata == null) {
            logger.error("❌ Metadata vazio no checkout.session");
            return;
        }

        String email = metadata.optString("email", null);
        String nome = metadata.optString("nome", null);
        String telefone = metadata.optString("telefone", null);
        String cpf = metadata.optString("cpf", null);

        if (email == null || nome == null) {
            logger.error("❌ Metadata insuficiente.");
            return;
        }

        Ticket ticket = new Ticket();
        ticket.setNome("Compra Guia RQ");
        ticket.setEmailCliente(email);
        ticket.setNomeCliente(nome);
        ticket.setTelefoneCliente(telefone);
        ticket.setCpfCliente(cpf);
        ticket.setStatus("PAGO");
        ticket.setUsado(false);
        ticket.setDataCompra(LocalDateTime.now());
        ticket.setCriadoEm(LocalDateTime.now());
        ticket.setIdPublico(UUID.randomUUID());
        ticket.setCompraId(UUID.randomUUID());
        ticket.setQrToken(UUID.randomUUID().toString());
        ticket.setValorPago(data.optDouble("amount_total") / 100.0);

        ticket.setStripeSessionId(sessionId);

        ticketRepository.save(ticket);

        logger.info("🎫 Ticket criado ID público: {}", ticket.getIdPublico());

        ticketService.processarCompra(ticket);

        logger.info("📨 Email enviado com sucesso!");
    }
}
