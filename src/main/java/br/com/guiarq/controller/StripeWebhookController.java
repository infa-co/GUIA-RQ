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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
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

    private static final Set<String> CPFS_LIBERADOS = Set.of(
            "11999143981",
            "13544956918"
    );
    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(@RequestBody String payload) {
        logger.info("📩 Payload recebido: {}", payload);
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

    public boolean podeComprarNovamente(String cpf, LocalDate ultimaCompra) {
        if (cpf != null && CPFS_LIBERADOS.contains(cpf)) {
            logger.warn("⚠ CPF liberado (bypass regra 3 meses): {}", cpf);
            return true;
        }
        if (ultimaCompra == null) return true;
        long dias = ChronoUnit.DAYS.between(ultimaCompra, LocalDate.now());
        if (dias >= 90) return true;
        logger.warn("❌ CPF bloqueado, última compra há {} dias: {}", dias, cpf);
        return false;
    }

    private void processCheckout(JSONObject json) {
        JSONObject data = json.getJSONObject("data").getJSONObject("object");
        String sessionId = data.optString("id");
        if (sessionId == null || sessionId.isBlank()) {
            logger.error("❌ sessionId inválido");
            return;
        }

        if (ticketRepository.existsByStripeSessionId(sessionId)) {
            logger.warn("⚠️ Webhook duplicado ignorado: {}", sessionId);
            return;
        }
        JSONObject metadata = data.optJSONObject("metadata");
        if (metadata == null) {
            logger.error("❌ Metadata vazio");
            return;
        }

        String email = metadata.optString("email", "").trim();
        String nome = metadata.optString("nome", "").trim();
        String telefone = metadata.optString("telefone", "").trim();
        String cpfRaw = metadata.optString("cpf", "").trim();
        String ticketIdStr = metadata.optString("ticketId", null);
        String pacoteStr = metadata.optString("pacote", "false");
        String quantidadeStr = metadata.optString("quantidade", "1");

        if (email.isBlank() || nome.isBlank()) {
            logger.error("❌ Metadata insuficiente: email ou nome ausentes. email='{}' nome='{}'", email, nome);
            return;
        }

        String cpf = normalizeCpf(cpfRaw);

        int quantidade = 1;
        try {
            quantidade = Integer.parseInt(quantidadeStr);
            if (quantidade <= 0) quantidade = 1;
        } catch (NumberFormatException e) {
            logger.warn("quantidade inválida ('{}'), usando 1", quantidadeStr);
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

        boolean isPacoteMetadata = "true".equalsIgnoreCase(pacoteStr);
        boolean isCatalogoPacote = (ticketCatalogoId != null && ticketCatalogoId == 11L);

        // **Regra principal**: se o catálogo for 11, tratar sempre como pacote
        boolean isPacote = isPacoteMetadata || isCatalogoPacote;
        logger.info("🔍 Decisão: isPacoteMetadata={}, isCatalogoPacote={}, isPacote={}, quantidade={}, ticketId={}",
                isPacoteMetadata, isCatalogoPacote, isPacote, quantidade, ticketCatalogoId);

        if (isPacote) {
            // Mesmo que quantidade == 1, se o catálogo for 11 tratamos como pacote
            processarPacote(sessionId, data, email, nome, telefone, cpf, ticketCatalogoId);
        } else if (ticketCatalogoId != null && quantidade > 1) {
            processarMultiplosTicketsAvulsos(sessionId, data, email, nome, telefone, cpf, ticketCatalogoId, quantidade);
        } else {
            processarTicketAvulso(sessionId, data, email, nome, telefone, cpf, ticketCatalogoId);
        }
    }

    private boolean clientePodeComprar(String cpf) {
        if (cpf == null) return true;
        Ticket ultimo = ticketRepository.findTop1ByCpfClienteOrderByDataCompraDesc(cpf);
        LocalDate ultimaData = (ultimo != null && ultimo.getDataCompra() != null)
                ? ultimo.getDataCompra().toLocalDate()
                : null;
        return podeComprarNovamente(cpf, ultimaData);
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
            logger.warn("❌ Compra bloqueada (ticket avulso) CPF: {}", cpf);
            return;
        }

        // Se o catálogo for 11, redireciona para pacote (proteção extra)
        if (ticketCatalogoId != null && ticketCatalogoId == 11L) {
            logger.info("Ticket avulso com ticketCatalogoId==11 detectado — tratando como pacote");
            processarPacote(sessionId, data, email, nome, telefone, cpf, ticketCatalogoId);
            return;
        }
        String nomeTicket = "Guia Rancho Queimado - Ticket";
        try {
            if (ticketCatalogoId != null) {
                Optional<TicketCatalogo> cat = ticketCatalogoRepository.findById(ticketCatalogoId);
                if (cat.isPresent()) nomeTicket = cat.get().getNome();
            }
        } catch (Exception e) {
            logger.error("Erro ao buscar catálogo: {}", e.getMessage());
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

        logger.info("✔ Ticket avulso ÚNICO criado com sucesso");
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
            logger.warn("❌ Compra bloqueada (múltiplos avulsos) CPF: {}", cpf);
            return;
        }

        String nomeTicket = "Guia Rancho Queimado - Ticket";
        try {
            if (ticketCatalogoId != null) {
                Optional<TicketCatalogo> cat = ticketCatalogoRepository.findById(ticketCatalogoId);
                if (cat.isPresent()) nomeTicket = cat.get().getNome();
            }
        } catch (Exception e) {
            logger.error("Erro ao buscar catálogo: {}", e.getMessage());
        }

        LocalDateTime agora = LocalDateTime.now();
        UUID compraId = UUID.randomUUID();
        List<Ticket> tickets = new ArrayList<>();

        boolean isPacote = (ticketCatalogoId != null && ticketCatalogoId == 11L);

        for (int i = 0; i < quantidade; i++) {
            Ticket ticket = new Ticket();
            if (i == 0) ticket.setStripeSessionId(sessionId);

            ticket.setTicketCatalogoId(ticketCatalogoId);
            ticket.setNome(nomeTicket);

            ticket.setEmailCliente(email);
            ticket.setNomeCliente(nome);
            ticket.setTelefoneCliente(telefone);
            ticket.setCpfCliente(cpf);

            ticket.setStatus("PAGO");
            ticket.setUsado(false);
            ticket.setPacote(isPacote);
            ticket.setQuantidadeComprada(quantidade);
            ticket.setDataCompra(agora);
            ticket.setCriadoEm(agora);

            ticket.setIdPublico(UUID.randomUUID());
            ticket.setQrToken(UUID.randomUUID().toString());

            ticket.setCompraId(compraId);

            ticketRepository.save(ticket);
            tickets.add(ticket);
        }

        ticketService.processarCompraAvulsaMultipla(tickets);
        logger.info("✔ {} tickets avulsos MÚLTIPLOS criados com sucesso! (isPacote={})", quantidade, isPacote);
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
            logger.warn("❌ Compra bloqueada (pacote) CPF: {}", cpf);
            return;
        }

        LocalDateTime agora = LocalDateTime.now();

        Double valorTotal = data.optDouble("amount_total") / 100.0;
        int quantidade = 10;
        double valorUnitario = quantidade > 0 ? (valorTotal / quantidade) : 0.0;

        UUID compraId = UUID.randomUUID();
        List<Ticket> tickets = new ArrayList<>();

        String nomeTicket = "Pacote 10 Tickets - Guia Rancho Queimado";
        try {
            if (ticketCatalogoId != null) {
                Optional<TicketCatalogo> cat = ticketCatalogoRepository.findById(ticketCatalogoId);
                if (cat.isPresent()) nomeTicket = cat.get().getNome();
            }
        } catch (Exception e) {
            logger.error("Erro ao buscar catálogo: {}", e.getMessage());
        }

        for (int i = 0; i < quantidade; i++) {
            Ticket ticket = new Ticket();
            if (i == 0) ticket.setStripeSessionId(sessionId);

            ticket.setTicketCatalogoId(ticketCatalogoId != null ? ticketCatalogoId : 11L);
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

            ticket.setValorPago(valorUnitario);
            ticket.setCompraId(compraId);

            ticketRepository.save(ticket);
            tickets.add(ticket);
        }

        ticketService.processarPacote(tickets);
        logger.info("🎁 Pacote criado com sucesso!");
    }
}
