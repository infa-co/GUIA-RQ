package br.com.guiarq.controller;

import br.com.guiarq.Model.Entities.Ticket;
import br.com.guiarq.Model.Entities.TicketCatalogo;
import br.com.guiarq.Model.Repository.TicketCatalogoRepository;
import br.com.guiarq.Model.Repository.TicketRepository;
import br.com.guiarq.Model.Service.TicketService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    private static String lastSession = "";

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
            logger.error("❌ sessionId inválido");
            return;
        }

        if (ticketRepository.existsByStripeSessionId(sessionId)) {
            logger.warn("⚠️ Webhook já processado para sessionId={}", sessionId);
            return;
        }

        if (!"paid".equalsIgnoreCase(data.optString("payment_status"))) {
            logger.warn("⚠️ Pagamento não confirmado para sessionId={}", sessionId);
            return;
        }
        // Alterar dps, bug anotado no bloco de notas
        if (lastSession.equalsIgnoreCase(sessionId)) {
            logger.info("Sessão já processada");
            return;
        } else {
            lastSession = sessionId;
        }

        JSONObject metadata = Optional.ofNullable(data.optJSONObject("metadata"))
                .orElse(new JSONObject());

        String email = metadata.optString("email", "").trim();
        String nome = metadata.optString("nome", "").trim();
        String telefone = metadata.optString("telefone", "").trim();
        String cpf = normalizeCpf(metadata.optString("cpf", ""));

        boolean isPacote = metadata.optBoolean("pacote", false);
        String pedidosJson = metadata.optString("pedidos");

        ObjectMapper mapper = new ObjectMapper();
        Map<String, Integer> pedidos = new HashMap<>();
        try{
            pedidos = mapper.readValue(pedidosJson, new TypeReference<Map<String, Integer>>() {});
        }catch (Exception e){
            logger.error("Erro processando pedidos", e);
        }
        int quantidade = parseInt(metadata.optString("quantidade", "1"), 1);

        if (email.isBlank() || nome.isBlank()) {
            logger.error("Metadata incompleta: email ou nome ausente");
            return;
        }
        if (isPacote) {
            logger.info("Tipo de compra identificado: PACOTE");
            //processarPacote(sessionId, email, nome, telefone, cpf, catalogoIds);
        } else if (quantidade > 1) {
            logger.info("Tipo de compra identificado: TICKETS AVULSOS (múltiplos)");
            //processarMultiplosTicketsAvulsos(sessionId, email, nome, telefone, cpf, pedidos, quantidade);
        } else {
            logger.info("Tipo de compra identificado: TICKET AVULSO (único)");
            //processarTicketAvulso(sessionId, email, nome, telefone, cpf, ticketCatalogoId);
        }
        logger.info(pedidos.toString());
        processarMultiplosTicketsAvulsos(sessionId, email, nome, telefone, cpf, pedidos, quantidade);
    }

    @Transactional
    private void processarMultiplosTicketsAvulsos(String sessionId, String email, String nome,
                                                  String telefone, String cpf, Map<String, Integer> pedidos,
                                                  int quantidade) {
        if (!clientePodeComprar(cpf)) {
            logger.info("Cliente bloqueado por regra 90 dias cpf={}", cpf);
            return;
        }
        List<Ticket> tickets = new ArrayList<>();
        pedidos.forEach((id, qtdTicket) -> {
            for(int i = 0; i < qtdTicket; i++) {
                if (Long.valueOf(id) == 11) {
                    List<TicketCatalogo> todos_tickets = ticketCatalogoRepository.findAll();
                    for (TicketCatalogo ticket : todos_tickets) {
                        if (ticket.getId() != 11) {
                            tickets.add(criarTicketBase(sessionId, email, nome, telefone, cpf, ticket.getId()));
                        }
                    }
                    continue;
                }
                Ticket ticket = criarTicketBase(sessionId, email, nome, telefone, cpf, Long.valueOf(id));
                logger.info(ticket.toString()  +" - "+ ticket.getNome() +" - "+ ticket.getEmailCliente());
                tickets.add(ticket);
            }
        });
        logger.info(tickets.toString());
        ticketService.processarCompraAvulsaMultipla(tickets);
        logger.info("Tickets avulsos múltiplos criados sessionId={} | quantidade={} | ids={} | cliente={}",
                sessionId, quantidade, email);
    }
    private Ticket criarTicketBase(String sessionId, String email, String nome,
                                   String telefone, String cpf, Long catalogoId) {

        String nomeTicket = ticketCatalogoRepository.findById(catalogoId)
                .map(TicketCatalogo::getNome)
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
}