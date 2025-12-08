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

    // ✅ CPFs LIBERADOS - NÃO ENTRAM NA REGRA DOS 3 MESES
    private static final Set<String> CPFS_LIBERADOS = Set.of(
            "11999143981",
            "13544956918"
    );

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

    public boolean podeComprarNovamente(String cpf, LocalDate ultimaCompra) {

        // ✅ BYPASS PARA CPFs LIBERADOS
        if (CPFS_LIBERADOS.contains(cpf)) {
            logger.warn("⚠ CPF liberado (bypass regra 3 meses): " + cpf);
            return true;
        }

        if (ultimaCompra == null) {
            return true;
        }

        long dias = ChronoUnit.DAYS.between(ultimaCompra, LocalDate.now());

        if (dias >= 90) {
            return true;
        }

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

        String email = metadata.optString("email");
        String nome = metadata.optString("nome");
        String telefone = metadata.optString("telefone");
        String cpf = metadata.optString("cpf");
        String ticketIdStr = metadata.optString("ticketId", null);
        String pacoteStr = metadata.optString("pacote", "false");
        String quantidadeStr = metadata.optString("quantidade", "1");

        if (email == null || nome == null) {
            logger.error("❌ Metadata insuficiente.");
            return;
        }

        // ✅ CORREÇÃO: REGRA DEFINITIVA PARA DIFERENCIAR PACOTE vs AVULSO MÚLTIPLO
        boolean isPacote = "true".equalsIgnoreCase(pacoteStr);
        int quantidade = Integer.parseInt(quantidadeStr);

        logger.info("🔍 Decisão: isPacote={}, quantidade={}, ticketId={}", isPacote, quantidade, ticketIdStr);

        if (isPacote) {
            // É PACOTE (10 tickets do id=11)
            processarPacote(sessionId, data, email, nome, telefone, cpf);
        } else if (ticketIdStr != null && quantidade > 1) {
            // ✅ MÚLTIPLOS TICKETS AVULSOS DO MESMO TIPO
            processarMultiplosTicketsAvulsos(sessionId, data, email, nome, telefone, cpf, ticketIdStr, quantidade);
        } else {
            // TICKET AVULSO ÚNICO
            processarTicketAvulso(sessionId, data, email, nome, telefone, cpf, ticketIdStr);
        }
    }

    private boolean clientePodeComprar(String cpf) {

        Ticket ultimo = ticketRepository.findTop1ByCpfClienteOrderByDataCompraDesc(cpf);

        LocalDate ultimaData = (ultimo != null && ultimo.getDataCompra() != null)
                ? ultimo.getDataCompra().toLocalDate()
                : null;

        return podeComprarNovamente(cpf, ultimaData);
    }

    private void processarTicketAvulso(String sessionId,
                                       JSONObject data,
                                       String email,
                                       String nome,
                                       String telefone,
                                       String cpf,
                                       String ticketIdStr) {

        if (!clientePodeComprar(cpf)) {
            logger.warn("❌ Compra bloqueada (ticket avulso) CPF: {}", cpf);
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

    // ✅ NOVO MÉTODO: PROCESSA MÚLTIPLOS TICKETS AVULSOS (NÃO É PACOTE!)
    private void processarMultiplosTicketsAvulsos(String sessionId,
                                                  JSONObject data,
                                                  String email,
                                                  String nome,
                                                  String telefone,
                                                  String cpf,
                                                  String ticketIdStr,
                                                  int quantidade) {

        if (!clientePodeComprar(cpf)) {
            logger.warn("❌ Compra bloqueada (múltiplos avulsos) CPF: {}", cpf);
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
            logger.error("Erro ao buscar catálogo: {}", e.getMessage());
        }

        LocalDateTime agora = LocalDateTime.now();
        UUID compraId = UUID.randomUUID();

        List<Ticket> tickets = new ArrayList<>();

        for (int i = 0; i < quantidade; i++) {

            Ticket ticket = new Ticket();

            // Apenas o PRIMEIRO ticket leva o sessionId
            if (i == 0) {
                ticket.setStripeSessionId(sessionId);
            }

            ticket.setTicketCatalogoId(ticketCatalogoId);
            ticket.setNome(nomeTicket);

            ticket.setEmailCliente(email);
            ticket.setNomeCliente(nome);
            ticket.setTelefoneCliente(telefone);
            ticket.setCpfCliente(cpf);

            ticket.setStatus("PAGO");
            ticket.setUsado(false);
            ticket.setPacote(false); // ✅ NÃO É PACOTE!
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

        logger.info("✔ {} tickets avulsos MÚLTIPLOS criados com sucesso!", quantidade);
    }

    private void processarPacote(String sessionId,
                                 JSONObject data,
                                 String email,
                                 String nome,
                                 String telefone,
                                 String cpf) {

        if (!clientePodeComprar(cpf)) {
            logger.warn("❌ Compra bloqueada (pacote) CPF: {}", cpf);
            return;
        }

        LocalDateTime agora = LocalDateTime.now();

        Double valorTotal = data.optDouble("amount_total") / 100.0;
        int quantidade = 10;

        double valorUnitario = valorTotal / quantidade;

        UUID compraId = UUID.randomUUID();

        List<Ticket> tickets = new ArrayList<>();

        for (int i = 0; i < quantidade; i++) {

            Ticket ticket = new Ticket();

            if (i == 0) {
                ticket.setStripeSessionId(sessionId);
            }

            ticket.setTicketCatalogoId(11L);
            ticket.setNome("Pacote 10 Tickets - Guia Rancho Queimado");

            ticket.setEmailCliente(email);
            ticket.setNomeCliente(nome);
            ticket.setTelefoneCliente(telefone);
            ticket.setCpfCliente(cpf);

            ticket.setStatus("PAGO");
            ticket.setUsado(false);
            ticket.setPacote(true); // ✅ É PACOTE!
            ticket.setQuantidadeComprada(10);

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