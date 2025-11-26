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

    // IDs reais do catálogo
    private static final List<Long> IDS_TICKETS_PACOTE = Arrays.asList(
            1L, 2L, 3L, 4L, 5L,
            6L, 7L, 8L, 9L, 10L
    );

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

    private void processCheckout(JSONObject json) {

        JSONObject data = json.getJSONObject("data").getJSONObject("object");

        String sessionId = data.optString("id");
        if (sessionId == null || sessionId.isBlank()) {
            logger.error("❌ sessionId inválido");
            return;
        }

        // Idempotência
        if (ticketRepository.existsByStripeSessionId(sessionId)) {
            logger.warn("⚠️ Webhook duplicado ignorado para sessionId: {}", sessionId);
            return;
        }

        JSONObject metadata = data.optJSONObject("metadata");

        if (metadata == null) {
            logger.error("❌ Metadata vazio no checkout.session");
            return;
        }

        String email = metadata.optString("email");
        String nome = metadata.optString("nome");
        String telefone = metadata.optString("telefone");
        String cpf = metadata.optString("cpf");
        String ticketIdStr = metadata.optString("ticketId", null);

        boolean isPacote = "true".equalsIgnoreCase(metadata.optString("pacote", "false"));

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

        Long ticketCatalogoId = null;
        String nomeTicket = "Guia RQ";

        try {
            ticketCatalogoId = Long.parseLong(ticketIdStr);
            Optional<TicketCatalogo> cat = ticketCatalogoRepository.findById(ticketCatalogoId);

            if (cat.isPresent()) {
                nomeTicket = cat.get().getNome() + " - Guia RQ";
            }

        } catch (Exception e) {
            logger.error("Erro ao buscar ticket catálogo: {}", e.getMessage());
        }

        Ticket ticket = new Ticket();
        ticket.setStripeSessionId(sessionId);
        ticket.setTicketCatalogoId(ticketCatalogoId);
        ticket.setNome(nomeTicket);
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

        ticketRepository.save(ticket);

        logger.info("🎫 Ticket criado: {}", ticket.getNome());
        ticketService.processarCompra(ticket);

        logger.info("📨 Ticket avulso enviado!");
    }

    // ======================
    // PACOTE COMPLETO (10 tickets)
    // ======================
    private void processarPacote(String sessionId,
                                 JSONObject data,
                                 String email,
                                 String nome,
                                 String telefone,
                                 String cpf) {

        LocalDateTime agora = LocalDateTime.now();
        Double valorTotal = data.optDouble("amount_total") / 100.0;

        int qtd = IDS_TICKETS_PACOTE.size();
        Double valorPorTicket = valorTotal / qtd;

        UUID compraIdPacote = UUID.randomUUID();

        List<Ticket> ticketsGerados = new ArrayList<>();
        boolean primeiro = true;

        for (Long idCat : IDS_TICKETS_PACOTE) {

            TicketCatalogo cat = ticketCatalogoRepository.findById(idCat).orElse(null);

            if (cat == null) {
                logger.error("❌ Ticket catálogo ID {} não encontrado", idCat);
                continue;
            }

            Ticket t = new Ticket();

            if (primeiro) {
                t.setStripeSessionId(sessionId);
                primeiro = false;
            }

            t.setTicketCatalogoId(cat.getId());
            t.setNome(cat.getNome()); // ✔ agora nome certo
            t.setEmailCliente(email);
            t.setNomeCliente(nome);
            t.setTelefoneCliente(telefone);
            t.setCpfCliente(cpf);
            t.setStatus("PAGO");
            t.setUsado(false);
            t.setDataCompra(agora);
            t.setCriadoEm(agora);
            t.setIdPublico(UUID.randomUUID());
            t.setCompraId(compraIdPacote);
            t.setQrToken(UUID.randomUUID().toString());
            t.setValorPago(valorPorTicket);

            ticketRepository.save(t);
            ticketsGerados.add(t);

            logger.info("🎟 Ticket pacote criado: {}", t.getNome());
        }

        // 🚀 Envia os 10 tickets no e-mail final
        ticketService.processarPacote(email, nome, telefone, cpf, ticketsGerados);

        logger.info("📨 Pacote enviado com sucesso!");
    }
}
