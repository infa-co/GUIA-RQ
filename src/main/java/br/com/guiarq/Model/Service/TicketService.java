package br.com.guiarq.Model.Service;

import br.com.guiarq.Model.Entities.Ticket;
import br.com.guiarq.Model.Repository.TicketRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TicketService {

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
            System.out.println("Ticket sem pagamento confirmado. Email NÃO enviado.");
            return;
        }
        try {
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

            System.out.println("✔ COMPRA PROCESSADA (TICKET ÚNICO)");

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("ERRO AO PROCESSAR COMPRA (TICKET ÚNICO)");
        }
    }
    public void processarCompraAvulsaMultipla(List<Ticket> tickets) {
        if (tickets.stream().anyMatch(t -> t.getStripeSessionId() == null)) {
            System.out.println("Existem tickets sem pagamento confirmado. Email NÃO enviado.");
            return;
        }
        try {
            if (tickets == null || tickets.isEmpty()) {
                throw new IllegalArgumentException("Lista de tickets vazia");
            }

            if (tickets.size() == 1) {
                processarCompra(tickets.get(0));
                return;
            }

            Ticket primeiro = tickets.get(0);

            List<byte[]> qrBytesList = new ArrayList<>();
            for (Ticket t : tickets) {
                String conteudo = URL_VALIDACAO + t.getQrToken();
                byte[] qrBytes = qrCodeService.generateQrCodeBytes(conteudo, 300, 300);
                if (qrBytes == null) throw new RuntimeException("Falha ao gerar QR do ticket: " + t.getIdPublico());
                qrBytesList.add(qrBytes);
            }

            String nomesTickets = tickets.stream()
                    .map(Ticket::getNome)
                    .collect(Collectors.joining(", "));

            emailService.sendMultiplosTicketsAvulsos(
                    primeiro.getEmailCliente(),
                    primeiro.getNomeCliente(),
                    primeiro.getTelefoneCliente(),
                    primeiro.getCpfCliente(),
                    nomesTickets, // lista de todos os nomes
                    tickets,
                    qrBytesList
            );

            System.out.println("TICKETS AVULSOS MULTIPLOS ENVIADOS");

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("ERRO AO PROCESSAR AVULSO MULTIPLO: " + e.getMessage());
        }
    }

    public void processarPacote(List<Ticket> tickets) {
        if (tickets.stream().anyMatch(t -> t.getStripeSessionId() == null)) {
            System.out.println("⚠️ Existem tickets sem pagamento confirmado. Email NÃO enviado.");
            return;
        }
        try {
            if (tickets == null || tickets.isEmpty()) {
                throw new IllegalArgumentException("Lista de tickets vazia");
            }

            if (tickets.size() == 1) {
                processarCompra(tickets.get(0));
                return;
            }

            Ticket primeiro = tickets.get(0);

            List<byte[]> qrBytesList = new ArrayList<>();
            for (Ticket t : tickets) {
                String conteudo = URL_VALIDACAO + t.getQrToken();
                qrBytesList.add(qrCodeService.generateQrCodeBytes(conteudo, 300, 300));
            }

            emailService.sendPacoteTicketsEmail(
                    primeiro.getEmailCliente(),
                    primeiro.getNomeCliente(),
                    primeiro.getTelefoneCliente(),
                    primeiro.getCpfCliente(),
                    tickets,
                    qrBytesList
            );

            System.out.println("✔ PACOTE PROCESSADO");

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("❌ ERRO AO PROCESSAR PACOTE: " + e.getMessage());
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
