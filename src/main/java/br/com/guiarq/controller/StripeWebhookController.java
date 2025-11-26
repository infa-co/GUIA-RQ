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

    // ⚠️ Ajuste estes IDs conforme os IDs reais em tickets_catalogo
    private static final List<Long> IDS_TICKETS_PACOTE = Arrays.asList(
            1L, // Ticket Forno e Serra
            2L, // Ticket RJ Off-Road
            3L, // Ticket Chalé Encantado
            4L, // Ticket Bergkafee
            5L, // Ticket Bierhaus RQ
            6L, // Ticket Mirante Boa Vista
            7L, // Ticket Goya Vinhos
            8L, // Ticket Restaurante Atafona
            9L, // Ticket Espaço Floresta
            10L // Ticket Da Roça Delicatessen
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
            logger.error("❌ sessionId inválido no webhook.");
            return;
        }

        // Idempotência: se já processamos esta sessão, ignorar
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

    // ✅ TICKET AVULSO
    private void processarTicketAvulso(String sessionId,
                                       JSONObject data,
                                       String email,
                                       String nome,
                                       String telefone,
                                       String cpf,
                                       String ticketIdStr) {

        Long ticketCatalogoId = null;
        String nomeTicket = "Ingresso - Guia RQ";

        try {
            ticketCatalogoId = Long.parseLong(ticketIdStr);
            Optional<TicketCatalogo> ticketCatOpt = ticketCatalogoRepository.findById(ticketCatalogoId);

            if (ticketCatOpt.isPresent()) {
                nomeTicket = ticketCatOpt.get().getNome();
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

        logger.info("🎫 Ticket criado ID público: {}", ticket.getIdPublico());

        ticketService.processarCompra(ticket);

        logger.info("📨 Email enviado com sucesso (ticket avulso)!");
    }

    // ✅ PACOTE COMPLETO
    private void processarPacote(String sessionId,
                                 JSONObject data,
                                 String email,
                                 String nome,
                                 String telefone,
                                 String cpf) {

        LocalDateTime agora = LocalDateTime.now();
        Double valorTotalPago = data.optDouble("amount_total") / 100.0;
        int qtd = IDS_TICKETS_PACOTE.size();
        Double valorPorTicket = valorTotalPago / qtd;

        UUID compraIdPacote = UUID.randomUUID();

        List<Ticket> ticketsGerados = new ArrayList<>();
        boolean primeiro = true; // apenas o primeiro recebe stripeSessionId (por causa do UNIQUE)

        for (Long idCatalogo : IDS_TICKETS_PACOTE) {

            Optional<TicketCatalogo> ticketCatOpt = ticketCatalogoRepository.findById(idCatalogo);
            if (ticketCatOpt.isEmpty()) {
                logger.error("❌ Ticket catálogo não encontrado para id={}", idCatalogo);
                continue;
            }

            TicketCatalogo cat = ticketCatOpt.get();

            Ticket ticket = new Ticket();

            if (primeiro) {
                ticket.setStripeSessionId(sessionId);
                primeiro = false;
            }

            ticket.setTicketCatalogoId(cat.getId());
            ticket.setNome(cat.getNome());
            ticket.setEmailCliente(email);
            ticket.setNomeCliente(nome);
            ticket.setTelefoneCliente(telefone);
            ticket.setCpfCliente(cpf);
            ticket.setStatus("PAGO");
            ticket.setUsado(false);
            ticket.setDataCompra(agora);
            ticket.setCriadoEm(agora);
            ticket.setIdPublico(UUID.randomUUID());
            ticket.setCompraId(compraIdPacote);
            ticket.setQrToken(UUID.randomUUID().toString());
            ticket.setValorPago(valorPorTicket);

            ticketRepository.save(ticket);
            ticketsGerados.add(ticket);

            logger.info("🎫 Ticket do pacote criado: {} / ID público {}", ticket.getNome(), ticket.getIdPublico());
        }

        ticketService.processarPacote(email, nome, telefone, cpf, ticketsGerados);

        logger.info("📨 Email enviado com sucesso (pacote)!");
    }
}
