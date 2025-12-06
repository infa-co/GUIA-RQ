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
            try {
                processCheckout(json);
            } catch (Exception e) {
                logger.error("❌ Erro ao processar checkout: {}", e.getMessage());
            }
        }

        return ResponseEntity.ok("OK");
    }

    private void processCheckout(JSONObject json) {

        JSONObject data = json.getJSONObject("data").getJSONObject("object");

        // ID da sessão
        String sessionId = data.optString("id", null);
        if (sessionId == null || sessionId.isBlank()) {
            logger.error("❌ sessionId inválido no webhook.");
            return;
        }

        // Evitar duplicação
        if (ticketRepository.existsByStripeSessionId(sessionId)) {
            logger.warn("⚠️ Webhook duplicado ignorado para sessionId {}", sessionId);
            return;
        }

        // Metadata
        JSONObject metadata = data.optJSONObject("metadata");
        if (metadata == null) {
            logger.error("❌ Metadata ausente.");
            return;
        }

        boolean isPacote = "true".equalsIgnoreCase(metadata.optString("pacote", "false"));

        String email = metadata.optString("email", "");
        String nome = metadata.optString("nome", "");
        String telefone = metadata.optString("telefone", "");
        String cpf = metadata.optString("cpf", "");

        if (email.isBlank() || nome.isBlank()) {
            logger.error("❌ Nome/email ausentes no metadata.");
            return;
        }

        // quantidade comprada
        int quantidade = metadata.optInt("quantidade", 1);

        if (isPacote) {
            processarPacote(sessionId, data, email, nome, telefone, cpf, quantidade);
        } else {
            String ticketIdStr = metadata.optString("ticketId", null);

            if (ticketIdStr == null || ticketIdStr.isBlank()) {
                logger.error("❌ ticketId ausente no avulso.");
                return;
            }

            processarTicketAvulso(sessionId, data, email, nome, telefone, cpf, ticketIdStr, quantidade);
        }
    }

    private void processarTicketAvulso(
            String sessionId,
            JSONObject data,
            String email,
            String nome,
            String telefone,
            String cpf,
            String ticketIdStr,
            int quantidade
    ) {

        TicketCatalogo catalogo;
        try {
            Long ticketCatalogoId = Long.parseLong(ticketIdStr);
            catalogo = ticketCatalogoRepository.findById(ticketCatalogoId).orElse(null);
        } catch (Exception e) {
            logger.error("❌ ticketId inválido: {}", ticketIdStr);
            return;
        }

        List<Ticket> ticketsGerados = new ArrayList<>();
        UUID compraId = UUID.randomUUID();
        LocalDateTime agora = LocalDateTime.now();

        double totalPaid = data.optDouble("amount_total") / 100.0;
        double valorUnitario = totalPaid / quantidade;

        for (int i = 0; i < quantidade; i++) {
            Ticket ticket = new Ticket();

            if (i == 0) {
                ticket.setStripeSessionId(sessionId);
            }

            ticket.setTicketCatalogoId(catalogo != null ? catalogo.getId() : null);
            ticket.setNome(catalogo != null ? catalogo.getNome() : "Ticket Guia RQ");
            ticket.setEmailCliente(email);
            ticket.setNomeCliente(nome);
            ticket.setTelefoneCliente(telefone);
            ticket.setCpfCliente(cpf);

            ticket.setStatus("PAGO");
            ticket.setUsado(false);
            ticket.setDataCompra(agora);
            ticket.setCriadoEm(agora);

            ticket.setIdPublico(UUID.randomUUID());
            ticket.setCompraId(compraId);
            ticket.setQrToken(UUID.randomUUID().toString());
            ticket.setValorPago(valorUnitario);

            ticketRepository.save(ticket);
            ticketsGerados.add(ticket);
        }

        logger.info("🎫 {} tickets avulsos gerados!", quantidade);

        // CORREÇÃO DEFINITIVA
        if (quantidade == 1) {
            ticketService.processarCompra(ticketsGerados.get(0)); // avulso único
        } else {
            ticketService.processarCompraAvulsaMultipla(ticketsGerados); // múltiplos avulsos
        }
    }

    private void processarPacote(
            String sessionId,
            JSONObject data,
            String email,
            String nome,
            String telefone,
            String cpf,
            int quantidade
    ) {

        List<Ticket> ticketsGerados = new ArrayList<>();
        UUID compraId = UUID.randomUUID();
        LocalDateTime agora = LocalDateTime.now();

        double totalPaid = data.optDouble("amount_total") / 100.0;
        double valorUnitario = totalPaid / quantidade;

        for (int i = 0; i < quantidade; i++) {

            Ticket ticket = new Ticket();

            if (i == 0) ticket.setStripeSessionId(sessionId);

            ticket.setTicketCatalogoId(null);
            ticket.setNome("Pacote Guia Rancho Queimado");
            ticket.setEmailCliente(email);
            ticket.setNomeCliente(nome);
            ticket.setTelefoneCliente(telefone);
            ticket.setCpfCliente(cpf);

            ticket.setStatus("PAGO");
            ticket.setUsado(false);
            ticket.setDataCompra(agora);
            ticket.setCriadoEm(agora);

            ticket.setIdPublico(UUID.randomUUID());
            ticket.setCompraId(compraId);
            ticket.setQrToken(UUID.randomUUID().toString());
            ticket.setValorPago(valorUnitario);

            ticketRepository.save(ticket);
            ticketsGerados.add(ticket);
        }

        logger.info("🎁 Pacote com {} tickets gerado!", quantidade);

        ticketService.processarPacote(ticketsGerados);
    }
}
