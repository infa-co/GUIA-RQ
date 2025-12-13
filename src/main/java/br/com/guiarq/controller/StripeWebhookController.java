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
        try {
            JSONObject json = new JSONObject(payload);
            if ("checkout.session.completed".equals(json.optString("type"))) {
                processCheckout(json);
            }
            return ResponseEntity.ok("OK");
        } catch (Exception e) {
            logger.error("Erro processando webhook", e);
            return ResponseEntity.status(500).body("ERROR");
        }
    }

    private void processCheckout(JSONObject json) {

        JSONObject data = json.getJSONObject("data").getJSONObject("object");
        String sessionId = data.optString("id");

        if (sessionId == null || sessionId.isBlank()) {
            logger.error("sessionId inválido");
            return;
        }

        if (ticketRepository.existsByStripeSessionId(sessionId)) {
            logger.warn("Webhook já processado para sessionId={}", sessionId);
            return;
        }

        if (!"paid".equalsIgnoreCase(data.optString("payment_status"))) {
            logger.warn("Pagamento não confirmado para sessionId={}", sessionId);
            return;
        }

        JSONObject metadata = Optional.ofNullable(data.optJSONObject("metadata"))
                .orElse(new JSONObject());

        String email = metadata.optString("email", "").trim();
        String nome = metadata.optString("nome", "").trim();
        String telefone = metadata.optString("telefone", "").trim();
        String cpf = normalizeCpf(metadata.optString("cpf", ""));

        // CORREÇÃO: verifica tanto pelo campo "pacote" quanto pelo ticketId
        boolean isPacote = metadata.optBoolean("pacote", false);
        // recuperar ticket IDs
        // long[] ticketCatalogoIDs
        Long ticketCatalogoId = parseLong(metadata.optString("ticketId", null));

        // se o ID for 11, É PACOTE COM CERTEZA
        if (ticketCatalogoId != null && ticketCatalogoId == 11) {
            isPacote = true;
        }

        int quantidade = parseInt(metadata.optString("quantidade", "1"), 1);

        if (email.isBlank() || nome.isBlank()) {
            logger.error("Metadata incompleta");
            return;
        }

        logger.info("Checkout confirmado sessionId={} pacote={} quantidade={} ticketId={}",
                sessionId, isPacote, quantidade, ticketCatalogoId);

        if (isPacote) {
            processarPacote(sessionId, email, nome, telefone, cpf, ticketCatalogoId);
            return;
        }

        if (quantidade > 1) {
            processarMultiplosTicketsAvulsos(sessionId, email, nome, telefone, cpf, ticketCatalogoId, quantidade);
        } else {
            processarTicketAvulso(sessionId, email, nome, telefone, cpf, ticketCatalogoId);
        }
    }

    @Transactional
    private void processarTicketAvulso(String sessionId, String email, String nome,
                                       String telefone, String cpf, Long ticketCatalogoId) {

        if (!clientePodeComprar(cpf)) return;

        Ticket ticket = criarTicketBase(sessionId, email, nome, telefone, cpf, ticketCatalogoId);
        ticket.setPacote(false);
        ticket.setQuantidadeComprada(1);

        ticketRepository.save(ticket);
        ticketService.processarCompra(ticket);

        logger.info("Ticket avulso criado sessionId={}", sessionId);
    }

    @Transactional
    private void processarMultiplosTicketsAvulsos(String sessionId, String email, String nome,
                                                  String telefone, String cpf, Long catalogoId,
                                                  int quantidade) {

        if (!clientePodeComprar(cpf)) return;

        UUID compraId = UUID.randomUUID();
        List<Ticket> tickets = new ArrayList<>();

        logger.warn("CatalogoId{} não encontrado, ignorando...");

        for (int i = 0; i < quantidade; i++) {
            Ticket t = criarTicketBase(sessionId, email, nome, telefone, cpf, catalogoId);
            t.setPacote(false);
            t.setQuantidadeComprada(quantidade);
            t.setCompraId(compraId);
            ticketRepository.save(t);
            tickets.add(t);
        }
        ticketService.processarCompraAvulsaMultipla(tickets);
        logger.info("Tickets avulsos múltiplos criados sessionId={} quantidade={}", sessionId, quantidade);
    }

    @Transactional
    private void processarPacote(String sessionId, String email, String nome,
                                 String telefone, String cpf, Long ticketCatalogoId) {

        logger.info("INICIANDO PROCESSAMENTO DE PACOTE sessionId={}", sessionId);

        if (!clientePodeComprar(cpf)) {
            logger.warn("Cliente bloqueado por regra 90 dias cpf={}", cpf);
            return;
        }

        int quantidade = 10;
        UUID compraId = UUID.randomUUID();
        List<Ticket> tickets = new ArrayList<>();

        logger.info("Criando {} tickets para o pacote compraId={}", quantidade, compraId);

        for (int i = 0; i < quantidade; i++) {
            Ticket t = criarTicketBase(sessionId, email, nome, telefone, cpf,
                    ticketCatalogoId != null ? ticketCatalogoId : 11);
            t.setPacote(true);
            t.setQuantidadeComprada(quantidade);
            t.setCompraId(compraId);
            ticketRepository.save(t);
            tickets.add(t);
            logger.debug("✔️ Ticket {}/{} criado id={}", i+1, quantidade, t.getIdPublico());
        }

        logger.info("Enviando email de pacote para {}", email);
        ticketService.processarPacote(tickets);
        logger.info("Pacote processado com sucesso sessionId={}", sessionId);
    }

    private Ticket criarTicketBase(String sessionId, String email, String nome,
                                   String telefone, String cpf, Long catalogoId) {

        String nomeTicket = ticketCatalogoRepository.findById(catalogoId)
                .map(TicketCatalogo::getNome)
                //REVER
                .orElse("Guia Rancho Queimado - Ticket");

        Ticket t = new Ticket();
        t.setStripeSessionId(sessionId);
        t.setTicketCatalogoId(catalogoId);
        t.setNome(nomeTicket);
        t.setEmailCliente(email);
        t.setNomeCliente(nome);
        t.setTelefoneCliente(telefone);
        t.setCpfCliente(cpf);
        t.setStatus("PAGO");
        t.setUsado(false);
        t.setDataCompra(LocalDateTime.now());
        t.setCriadoEm(LocalDateTime.now());
        t.setIdPublico(UUID.randomUUID());
        t.setQrToken(UUID.randomUUID().toString());
        return t;
    }

    private String normalizeCpf(String cpf) {
        return cpf == null ? null : cpf.replaceAll("\\D", "");
    }

    private boolean clientePodeComprar(String cpf) {
        if (cpf != null && CPFS_LIBERADOS.contains(cpf)) return true;
        Ticket ultimo = ticketRepository.findTop1ByCpfClienteOrderByDataCompraDesc(cpf);
        if (ultimo == null || ultimo.getDataCompra() == null) return true;
        return ChronoUnit.DAYS.between(
                ultimo.getDataCompra().toLocalDate(), LocalDate.now()) >= 90;
    }

    private int parseInt(String v, int def) {
        try { return Integer.parseInt(v); } catch (Exception e) { return def; }
    }

    private Long parseLong(String v) {
        try { return v == null ? null : Long.parseLong(v); } catch (Exception e) { return null; }
    }
}