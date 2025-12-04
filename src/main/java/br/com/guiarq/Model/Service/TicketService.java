package br.com.guiarq.Model.Service;

import br.com.guiarq.Model.Entities.Ticket;
import br.com.guiarq.Model.Repository.TicketRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
        try {
            String conteudo = URL_VALIDACAO + ticket.getQrToken();
            byte[] qrBytes = qrCodeService.generateQrCodeBytes(conteudo, 300, 300);

            emailService.sendTicketEmail(
                    ticket.getEmailCliente(),
                    ticket.getNomeCliente(),
                    ticket.getTelefoneCliente(),
                    ticket.getCpfCliente(),
                    ticket.getNome(),   // Nome do ticket agora vem correto
                    qrBytes
            );

            System.out.println("✔ COMPRA PROCESSADA (TICKET ÚNICO)");

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("❌ ERRO AO PROCESSAR COMPRA (TICKET ÚNICO)");
        }
    }
    public void processarPacote(List<Ticket> tickets) {
        try {
            if (tickets == null || tickets.isEmpty()) {
                throw new IllegalArgumentException("Lista de tickets vazia");
            }

            // O PRIMEIRO ticket contém os dados do comprador
            Ticket primeiro = tickets.get(0);

            String emailDestino = primeiro.getEmailCliente();
            String nomeCliente = primeiro.getNomeCliente();
            String telefone = primeiro.getTelefoneCliente();
            String cpf = primeiro.getCpfCliente();

            List<byte[]> qrBytesList = new ArrayList<>();

            for (Ticket ticket : tickets) {
                String conteudo = URL_VALIDACAO + ticket.getQrToken();
                byte[] qrBytes = qrCodeService.generateQrCodeBytes(conteudo, 300, 300);
                qrBytesList.add(qrBytes);
            }

            emailService.sendPacoteTicketsEmail(
                    emailDestino,
                    nomeCliente,
                    telefone,
                    cpf,
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
