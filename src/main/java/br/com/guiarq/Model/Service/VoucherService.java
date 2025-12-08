package br.com.guiarq.Model.Service;

import br.com.guiarq.Model.Entities.Ticket;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class VoucherService {

    private final EmailService emailService;

    public VoucherService(EmailService emailService) {
        this.emailService = emailService;
    }

    public boolean usuarioPossuiVoucherAtivo(String cpf) {
        // TODO: Consultar no banco se há voucher ativo para este CPF
        return false;
    }

    // Ticket avulso
    public void gerarTicketAvulso(Long ticketId, String email, String nome, String telefone, String cpf) {
        Ticket ticket = buscarTicketPorId(ticketId);
        byte[] qrBytes = gerarQrCodeParaTicket(ticket, cpf);
        emailService.sendTicketEmail(email, nome, telefone, cpf, ticket.getNome(), qrBytes);
    }

    // Múltiplos tickets avulsos (agora com ticketId)
    public void gerarMultiplosTickets(Long ticketId, Long quantidade, String email, String nome, String telefone, String cpf) {
        if (quantidade == null || quantidade <= 0) {
            throw new IllegalArgumentException("Quantidade inválida para múltiplos tickets.");
        }

        Ticket ticketBase = buscarTicketPorId(ticketId);
        List<Ticket> tickets = new ArrayList<>();
        List<byte[]> qrBytesList = new ArrayList<>();

        for (int i = 0; i < quantidade; i++) {
            tickets.add(ticketBase);
            qrBytesList.add(gerarQrCodeParaTicket(ticketBase, cpf));
        }

        emailService.sendMultiplosTicketsAvulsos(email, nome, telefone, cpf, ticketBase.getNome(), tickets, qrBytesList);
    }
    // Pacote completo
    public void gerarPacoteCompleto(String email, String nome, String telefone, String cpf) {
        List<Ticket> ticketsDoPacote = buscarTicketsDoPacote(11);
        List<byte[]> qrCodes = new ArrayList<>();
        for (Ticket t : ticketsDoPacote) {
            qrCodes.add(gerarQrCodeParaTicket(t, cpf));
        }
        emailService.sendPacoteTicketsEmail(email, nome, telefone, cpf, ticketsDoPacote, qrCodes);
    }

    // ===== Auxiliares =====
    private Ticket buscarTicketPorId(Long ticketId) {
        Ticket t = new Ticket();
        t.setId(ticketId);
        t.setNome("Ticket " + ticketId);
        return t;
    }

    private List<Ticket> buscarTicketsDoPacote(int pacoteId) {
        List<Ticket> lista = new ArrayList<>();
        String[] nomes = {
                "Pizzaria Forno e Serra", "RJ Off-Road", "Chalé Encantado",
                "Bergkafee Café Colonial", "Da Roça", "Espaço Floresta",
                "Bierhaus", "Mirante Boa Vista", "Goyah Vinhos", "Atafona"
        };
        for (int i = 0; i < nomes.length; i++) {
            Ticket t = new Ticket();
            t.setId((long) (1000 + i));
            t.setNome(nomes[i]);
            lista.add(t);
        }
        return lista;
    }

    private byte[] gerarQrCodeParaTicket(Ticket ticket, String cpf) {
        return ("QR-" + ticket.getId() + "-" + cpf).getBytes();
    }
}

