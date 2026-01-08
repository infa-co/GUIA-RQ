package br.com.guiarq.Model.Service;

import br.com.guiarq.Model.Entities.Ticket;
import br.com.guiarq.Model.Repository.TicketRepository;
import br.com.guiarq.controller.StripeWebhookController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TicketService {

    private static final Logger logger = LoggerFactory.getLogger(TicketService.class);

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private QrCodeService qrCodeService;

    private static final String URL_VALIDACAO =
            "https://guiaranchoqueimado.com.br/pages/validar-ticket.html?qr=";

    public Ticket salvar(Ticket ticket) {
        return ticketRepository.save(ticket);
    }

    public List<Ticket> listarTodos() {
        return ticketRepository.findAll();
    }

    public void processarCompra(Ticket ticket) {
        if (ticket.getStripeSessionId() == null) {
            logger.warn("Ticket sem pagamento confirmado. Email NÃO enviado.");
            return;
        }
        try {
            // salva no banco ANTES de enviar e-mail
            ticket.setDataCompra(LocalDateTime.now());
            ticket.setCriadoEm(LocalDateTime.now());
            ticketRepository.save(ticket);

            String conteudo = URL_VALIDACAO + ticket.getQrToken();
            byte[] qrBytes = qrCodeService.generateQrCodeBytes(conteudo, 300, 300);

            emailService.sendTicketEmail(
                    ticket.getEmailCliente(),
                    ticket.getNomeCliente(),
                    ticket.getTelefoneCliente(),
                    ticket.getCpfCliente(),
                    ticket.getNome(),
                    qrBytes
            );

            logger.info("✔ COMPRA PROCESSADA (TICKET ÚNICO) id={}", ticket.getId());

        } catch (Exception e) {
            logger.error("ERRO AO PROCESSAR COMPRA (TICKET ÚNICO)", e);
        }
    }
    public void processarCompraAvulsaMultipla(List<Ticket> tickets) {
        if (tickets.stream().anyMatch(t -> t.getStripeSessionId() == null)) {
            logger.warn("Existem tickets sem pagamento confirmado. Email NÃO enviado.");
            return;
        }
        try {
            if (tickets == null || tickets.isEmpty()) {
                throw new IllegalArgumentException("Lista de tickets vazia");
            }

            // salva todos no banco
            tickets.forEach(t -> {
                t.setDataCompra(LocalDateTime.now());
                t.setCriadoEm(LocalDateTime.now());
            });
            ticketRepository.saveAll(tickets);

            // resto do código (gerar QR, enviar e-mail)...
        } catch (Exception e) {
            logger.error("ERRO AO PROCESSAR AVULSO MULTIPLO", e);
        }
    }

    public Ticket verificar(UUID idPublico) {
        return ticketRepository.findByIdPublico(idPublico)
                .orElseThrow(() -> new RuntimeException("Ticket não encontrado"));
    }

    public Ticket confirmarUso(String qrToken) {

        Ticket ticket = ticketRepository.findByQrToken(qrToken)
                .orElseThrow(() -> new RuntimeException("TICKET_NAO_ENCONTRADO"));

        if (ticket.isUsado()) {
            throw new IllegalStateException("TICKET_JA_USADO");
        }

        ticket.setUsado(true);
        ticket.setUsadoEm(LocalDateTime.now());

        return ticketRepository.save(ticket);
    }
}
