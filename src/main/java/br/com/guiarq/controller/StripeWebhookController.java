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

        JSONObject metadata = Optional.ofNullable(data.optJSONObject("metadata"))
                .orElse(new JSONObject());

        String email = metadata.optString("email", "").trim();
        String nome = metadata.optString("nome", "").trim();
        String telefone = metadata.optString("telefone", "").trim();
        String cpf = normalizeCpf(metadata.optString("cpf", ""));

        boolean isPacote = metadata.optBoolean("pacote", false);
        Map<Integer, Integer> pedidos = new HashMap<>();
        JSONObject pedidosJson = metadata.optJSONObject("pedidos");

        if (pedidosJson != null) {
            for (String key : pedidosJson.keySet()) {
                Integer chave = Integer.valueOf(key);

                Number valor = (Number) pedidosJson.get(key);
                pedidos.put(chave, valor.intValue());
            }
        }
        pedidos.forEach((id, quantidade) -> {
            System.out.println("ticketID: " + id  + "  ->  quantidade: " + quantidade);
        });

        //String idsString = metadata.optString("ids", null);


        /*List<Long> catalogoIds = new ArrayList<>();
        if (idsString != null && !idsString.isBlank()) {
            catalogoIds = Arrays.stream(idsString.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(this::parseLong)
                    .filter(Objects::nonNull)
                    .toList();
        } else if (ticketCatalogoId != null) {
            catalogoIds = Collections.singletonList(ticketCatalogoId);
        }*/

        //if (catalogoIds.contains(11L)) {
          //  isPacote = true;
        //}

        int quantidade = parseInt(metadata.optString("quantidade", "1"), 1);

        if (email.isBlank() || nome.isBlank()) {
            logger.error("Metadata incompleta: email ou nome ausente");
            return;
        }

        // Log principal do checkout
        //logger.info("Checkout confirmado sessionId={} | pacote={} | quantidade={} | ticketIds={} | cliente={} ({})",
          //      sessionId, isPacote, quantidade, catalogoIds, nome, email);

        if (isPacote) {
            logger.info("Tipo de compra identificado: PACOTE");
            //processarPacote(sessionId, email, nome, telefone, cpf, catalogoIds);
        } else if (quantidade > 1) {
            logger.info("Tipo de compra identificado: TICKETS AVULSOS (múltiplos)");
            //processarMultiplosTicketsAvulsos(sessionId, email, nome, telefone, cpf, catalogoIds, quantidade);
        } else {
            logger.info("Tipo de compra identificado: TICKET AVULSO (único)");
            //processarTicketAvulso(sessionId, email, nome, telefone, cpf, ticketCatalogoId);
        }
    }

    @Transactional
    private void processarTicketAvulso(String sessionId, String email, String nome,
                                       String telefone, String cpf, Long ticketCatalogoId) {
        if (!clientePodeComprar(cpf)) {
            logger.warn("Cliente bloqueado por regra 90 dias cpf={}", cpf);
            return;
        }

        UUID compraId = UUID.randomUUID();
        List<Ticket> tickets = criarTickets(sessionId, email, nome, telefone, cpf,
                Collections.singletonList(ticketCatalogoId), 1, false, compraId);

        ticketService.processarCompra(tickets.get(0));
        logger.info("Ticket avulso criado sessionId={} | ticketId={} | cliente={}", sessionId, ticketCatalogoId, email);
    }

    @Transactional
    private void processarMultiplosTicketsAvulsos(String sessionId, String email, String nome,
                                                  String telefone, String cpf, List<Long> ticketCatalogoIds,
                                                  int quantidade) {
        if (!clientePodeComprar(cpf)) {
            logger.warn("Cliente bloqueado por regra 90 dias cpf={}", cpf);
            return;
        }

        UUID compraId = UUID.randomUUID();
        List<Ticket> tickets = criarTickets(sessionId, email, nome, telefone, cpf,
                ticketCatalogoIds, quantidade, false, compraId);

        ticketService.processarCompraAvulsaMultipla(tickets);
        logger.info("Tickets avulsos múltiplos criados sessionId={} | quantidade={} | ids={} | cliente={}",
                sessionId, quantidade, ticketCatalogoIds, email);
    }

    @Transactional
    private void processarPacote(String sessionId, String email, String nome,
                                 String telefone, String cpf, List<Long> ticketCatalogoIds) {
        logger.info("INICIANDO PROCESSAMENTO DE PACOTE sessionId={}", sessionId);

        if (!clientePodeComprar(cpf)) {
            logger.warn("Cliente bloqueado por regra 90 dias cpf={}", cpf);
            return;
        }

        int quantidade = 10;
        UUID compraId = UUID.randomUUID();
        List<Ticket> tickets = criarTickets(sessionId, email, nome, telefone, cpf,
                ticketCatalogoIds, quantidade, true, compraId);

        logger.info("{} tickets criados para PACOTE compraId={} | cliente={}", quantidade, compraId, email);
        ticketService.processarPacote(tickets);
        logger.info("Pacote processado com sucesso sessionId={} | cliente={}", sessionId, email);
    }

    private List<Ticket> criarTickets(String sessionId, String email, String nome,
                                      String telefone, String cpf, List<Long> catalogoIds, int quantidade,
                                      boolean isPacote, UUID compraId) {
        if (catalogoIds == null || catalogoIds.isEmpty()) {
            throw new IllegalArgumentException("Nenhum ID de catálogo recebido para criação de tickets.");
        }

        List<Ticket> tickets = new ArrayList<>();
        List<TicketCatalogo> catalogos = ticketCatalogoRepository.findByIdIn(catalogoIds);

        if (catalogos.isEmpty()) {
            throw new IllegalStateException("Nenhum catálogo encontrado para os IDs fornecidos: " + catalogoIds);
        }

        for (TicketCatalogo catalogo : catalogos) {
            Long catalogoId = catalogo.getId();
            for (int i = 0; i < quantidade; i++) {
                Ticket t = criarTicketBase(sessionId, email, nome, telefone, cpf, catalogoId);
                t.setPacote(isPacote);
                t.setQuantidadeComprada(quantidade);
                t.setCompraId(compraId);

                ticketRepository.save(t);
                tickets.add(t);

                System.out.println("Ticket criado: nome=" + t.getNome() + " | catálogoId=" + catalogoId + " | cliente=" + email);
            }
        }

        System.out.println("Total de tickets criados: " + tickets.size());
        return tickets;
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

    private Long parseLong(String v) {
        try { return v == null ? null : Long.parseLong(v); } catch (Exception e) {
            return null; }
    }
}