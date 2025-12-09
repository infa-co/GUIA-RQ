package br.com.guiarq.controller;

import br.com.guiarq.Model.Entities.Ticket;
import br.com.guiarq.Model.Entities.TicketCatalogo;
import br.com.guiarq.Model.Repository.TicketCatalogoRepository;
import br.com.guiarq.Model.Repository.TicketRepository;
import br.com.guiarq.Model.Service.TicketService;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Webhook controller para processar eventos Stripe.
 * Detecta pacote usando metadata.description / data.description / line_items.
 */
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

    private static final Set<String> CPFS_LIBERADOS = Set.of(
            "11999143981",
            "13544956918"
    );

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(@RequestBody String payload) {
        logger.info("Webhook recebido");
        try {
            JSONObject json = new JSONObject(payload);
            String eventType = json.optString("type");
            if ("checkout.session.completed".equals(eventType)) {
                processCheckout(json);
            }
            return ResponseEntity.ok("OK");
        } catch (Exception e) {
            logger.error("Erro processando webhook", e);
            return ResponseEntity.status(500).body("ERROR");
        }
    }

    private String normalizeCpf(String cpf) {
        if (cpf == null) return null;
        String cleaned = cpf.replaceAll("\\D", "");
        return cleaned.isBlank() ? null : cleaned;
    }

    private boolean podeComprarNovamente(String cpf, LocalDate ultimaCompra) {
        if (cpf != null && CPFS_LIBERADOS.contains(cpf)) {
            logger.warn("CPF liberado pela lista: {}", cpf);
            return true;
        }
        if (ultimaCompra == null) return true;
        long dias = ChronoUnit.DAYS.between(ultimaCompra, LocalDate.now());
        if (dias >= 90) return true;
        logger.warn("Compra bloqueada: última compra há {} dias (CPF={})", dias, cpf);
        return false;
    }

    private boolean clientePodeComprar(String cpf) {
        if (cpf == null) return true;
        Ticket ultimo = ticketRepository.findTop1ByCpfClienteOrderByDataCompraDesc(cpf);
        LocalDate ultimaData = (ultimo != null && ultimo.getDataCompra() != null)
                ? ultimo.getDataCompra().toLocalDate()
                : null;
        return podeComprarNovamente(cpf, ultimaData);
    }

    private void processCheckout(JSONObject json) {
        JSONObject data = json.getJSONObject("data").getJSONObject("object");
        String sessionId = data.optString("id");
        if (sessionId == null || sessionId.isBlank()) {
            logger.error("sessionId inválido no webhook");
            return;
        }

        if (ticketRepository.existsByStripeSessionId(sessionId)) {
            logger.warn("Webhook já processado para sessionId={}", sessionId);
            return;
        }

        JSONObject metadata = data.optJSONObject("metadata");
        if (metadata == null) metadata = new JSONObject();

        // Verifica status do pagamento antes de processar
        String paymentStatus = data.optString("payment_status", "").trim();
        if (!"paid".equalsIgnoreCase(paymentStatus)) {
            logger.warn("Pagamento não confirmado (payment_status={}) para sessionId={}", paymentStatus, sessionId);
            return;
        }

        String description = extractDescription(data, metadata);
        if (description == null) description = "";

        String email = metadata.optString("email", "").trim();
        String nome = metadata.optString("nome", "").trim();
        String telefone = metadata.optString("telefone", "").trim();
        String cpfRaw = metadata.optString("cpf", "").trim();
        String ticketIdStr = metadata.optString("ticketId", null);
        String quantidadeStr = metadata.optString("quantidade", "1");

        if (email.isBlank() || nome.isBlank()) {
            logger.error("Metadata insuficiente: email='{}' nome='{}'", email, nome);
            return;
        }

        String cpf = normalizeCpf(cpfRaw);

        int quantidade = 1;
        try {
            quantidade = Integer.parseInt(quantidadeStr);
            if (quantidade <= 0) quantidade = 1;
        } catch (NumberFormatException e) {
            logger.warn("quantidade inválida no metadata ('{}'), usando 1", quantidadeStr);
            quantidade = 1;
        }

        Long ticketCatalogoId = null;
        if (ticketIdStr != null && !ticketIdStr.isBlank()) {
            try {
                ticketCatalogoId = Long.parseLong(ticketIdStr);
            } catch (NumberFormatException e) {
                logger.warn("ticketId inválido no metadata: {}", ticketIdStr);
                ticketCatalogoId = null;
            }
        }

        // Alinha com o frontend: chave enviada é "pacote"
        boolean isPacote = metadata.optBoolean("pacote", false);

        logger.info("Webhook recebido: sessionId={} payment_status={} | Decisão pacote? ticketCatalogoId={} -> isPacote={} | desc='{}' | quantidade={}",
                sessionId, paymentStatus, ticketCatalogoId, isPacote, description, quantidade);

        if (isPacote) {
            logger.info("Chamando processarPacote para sessionId={}", sessionId);
            processarPacote(sessionId, data, email, nome, telefone, cpf, ticketCatalogoId);
            return;
        }

        if (ticketCatalogoId != null && quantidade > 1) {
            logger.info("Chamando processarMultiplosTicketsAvulsos para sessionId={}", sessionId);
            processarMultiplosTicketsAvulsos(sessionId, data, email, nome, telefone, cpf, ticketCatalogoId, quantidade);
        } else {
            logger.info("Chamando processarTicketAvulso para sessionId={}", sessionId);
            processarTicketAvulso(sessionId, data, email, nome, telefone, cpf, ticketCatalogoId);
        }
    }

    private String extractDescription(JSONObject data, JSONObject metadata) {
        String desc = metadata.optString("description", "").trim();
        if (!desc.isBlank()) return desc;

        desc = data.optString("description", "").trim();
        if (!desc.isBlank()) return desc;

        try {
            if (data.has("line_items")) {
                Object liObj = data.get("line_items");
                if (liObj instanceof JSONArray) {
                    JSONArray lineItems = data.getJSONArray("line_items");
                    for (int i = 0; i < lineItems.length(); i++) {
                        JSONObject li = lineItems.getJSONObject(i);
                        if (li.has("price_data")) {
                            JSONObject priceData = li.optJSONObject("price_data");
                            if (priceData != null && priceData.has("product_data")) {
                                JSONObject productData = priceData.optJSONObject("product_data");
                                if (productData != null) {
                                    String name = productData.optString("name", "").trim();
                                    if (!name.isBlank()) return name;
                                }
                            }
                        }
                        String liDesc = li.optString("description", "").trim();
                        if (!liDesc.isBlank()) return liDesc;
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Não foi possível extrair description de line_items: {}", e.getMessage());
        }
        return null;
    }

    @Transactional
    private void processarTicketAvulso(String sessionId,
                                       JSONObject data,
                                       String email,
                                       String nome,
                                       String telefone,
                                       String cpf,
                                       Long ticketCatalogoId) {

        if (!clientePodeComprar(cpf)) {
            logger.warn("Compra bloqueada (ticket avulso) CPF={}", cpf);
            return;
        }

        String nomeTicket = "Guia Rancho Queimado - Ticket";
        try {
            if (ticketCatalogoId != null) {
                Optional<TicketCatalogo> cat = ticketCatalogoRepository.findById(ticketCatalogoId);
                if (cat.isPresent()) nomeTicket = cat.get().getNome();
            }
        } catch (Exception e) {
            logger.debug("Erro buscando catálogo: {}", e.getMessage());
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
        ticket.setPacote(false);
        ticket.setQuantidadeComprada(1);

        ticket.setDataCompra(LocalDateTime.now());
        ticket.setCriadoEm(LocalDateTime.now());

        ticket.setIdPublico(UUID.randomUUID());
        ticket.setQrToken(UUID.randomUUID().toString());

        ticketRepository.save(ticket);
        ticketService.processarCompra(ticket);
        logger.info("Ticket avulso criado para sessionId={}", sessionId);
    }

    @Transactional
    private void processarMultiplosTicketsAvulsos(String sessionId,
                                                  JSONObject data,
                                                  String email,
                                                  String nome,
                                                  String telefone,
                                                  String cpf,
                                                  Long ticketCatalogoId,
                                                  int quantidade) {

        if (!clientePodeComprar(cpf)) {
            logger.warn("Compra bloqueada (múltiplos avulsos) CPF={}", cpf);
            return;
        }

        String nomeTicket = "Guia Rancho Queimado - Ticket";
        try {
            if (ticketCatalogoId != null) {
                Optional<TicketCatalogo> cat = ticketCatalogoRepository.findById(ticketCatalogoId);
                if (cat.isPresent()) nomeTicket = cat.get().getNome();
            }
        } catch (Exception e) {
            logger.debug("Erro buscando catálogo: {}", e.getMessage());
        }

        LocalDateTime agora = LocalDateTime.now();
        UUID compraId = UUID.randomUUID();
        List<Ticket> tickets = new ArrayList<>();

        for (int i = 0; i < quantidade; i++) {
            Ticket ticket = new Ticket();
            ticket.setStripeSessionId(sessionId); //MUDEI AQUI AGORA tirei if(i==0)

            ticket.setTicketCatalogoId(ticketCatalogoId);
            ticket.setNome(nomeTicket);

            ticket.setEmailCliente(email);
            ticket.setNomeCliente(nome);
            ticket.setTelefoneCliente(telefone);
            ticket.setCpfCliente(cpf);

            ticket.setStatus("PAGO");
            ticket.setUsado(false);
            ticket.setPacote(false);
            ticket.setQuantidadeComprada(quantidade);
            if (quantidade > 1) {
                ticket.setPacote(false);
            }

            ticket.setDataCompra(agora);
            ticket.setCriadoEm(agora);

            ticket.setIdPublico(UUID.randomUUID());
            ticket.setQrToken(UUID.randomUUID().toString());

            ticket.setCompraId(compraId);

            ticketRepository.save(ticket);
            tickets.add(ticket);
        }

        ticketService.processarCompraAvulsaMultipla(tickets);
        logger.info("Criados {} tickets avulsos para sessionId={}", quantidade, sessionId);
    }

    @Transactional
    private void processarPacote(String sessionId,
                                 JSONObject data,
                                 String email,
                                 String nome,
                                 String telefone,
                                 String cpf,
                                 Long ticketCatalogoId) {

        if (!clientePodeComprar(cpf)) {
            logger.warn("Compra bloqueada (pacote) CPF={}", cpf);
            return;
        }

        LocalDateTime agora = LocalDateTime.now();
        Double valorTotal = data.optDouble("amount_total", 0.0) / 100.0;
        int quantidade = 10; // pacote fixo de 10
        if (valorTotal == 77.00) {

            UUID compraId = UUID.randomUUID();
            List<Ticket> tickets = new ArrayList<>();

            String nomeTicket = "Pacote Completo Guia RQ";
            try {
                if (ticketCatalogoId != null) {
                    Optional<TicketCatalogo> cat = ticketCatalogoRepository.findById(ticketCatalogoId);
                    if (cat.isPresent()) nomeTicket = cat.get().getNome();
                }
            } catch (Exception e) {
                logger.debug("Erro buscando catálogo: {}", e.getMessage());
            }

            for (int i = 0; i < quantidade; i++) {
                Ticket ticket = new Ticket();
                if (i == 0) ticket.setStripeSessionId(sessionId);

                ticket.setTicketCatalogoId(ticketCatalogoId != null ? ticketCatalogoId : 11);
                ticket.setNome(nomeTicket);

                ticket.setEmailCliente(email);
                ticket.setNomeCliente(nome);
                ticket.setTelefoneCliente(telefone);
                ticket.setCpfCliente(cpf);

                ticket.setStatus("PAGO");
                ticket.setUsado(false);
                ticket.setPacote(true);
                ticket.setQuantidadeComprada(quantidade);

                ticket.setDataCompra(agora);
                ticket.setCriadoEm(agora);

                ticket.setIdPublico(UUID.randomUUID());
                ticket.setQrToken(UUID.randomUUID().toString());

                ticket.setValorPago(77.00);
                ticket.setCompraId(compraId);

                ticketRepository.save(ticket);
                tickets.add(ticket);
            }

            ticketService.processarPacote(tickets);
            logger.info("Pacote processado ({} tickets) para sessionId={}", tickets.size(), sessionId);
        }
    }
}
