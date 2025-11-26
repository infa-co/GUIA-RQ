package br.com.guiarq.Model.Service;

import br.com.guiarq.Model.Entities.Ticket;
import br.com.guiarq.Model.Repository.TicketRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

    public Ticket salvar(Ticket ticket) {
        return ticketRepository.save(ticket);
    }

    public List<Ticket> listarTodos() {
        return ticketRepository.findAll();
    }

    // TICKET AVULSO (já existia)
    public void processarCompra(Ticket ticket) {
        try {
            String conteudo = "https://guiaranchoqueimado.com.br/validar-ticket.html/?qr=" + ticket.getQrToken();
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
            System.out.println("❌ ERRO AO PROCESSAR COMPRA (TICKET ÚNICO)");
        }
    }

    // ✅ NOVO: PROCESSAR PACOTE
    public void processarPacote(String emailDestino,
                                String nomeCliente,
                                String telefone,
                                String cpf,
                                List<Ticket> tickets) {
        try {
            List<byte[]> qrBytesList = new ArrayList<>();

            for (Ticket ticket : tickets) {
                String conteudo = "https://guiaranchoqueimado.com.br/validar/?qr=" + ticket.getQrToken();
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
            System.out.println("❌ ERRO AO PROCESSAR PACOTE");
        }
    }

    public Ticket verificar(UUID idPublico) {
        return ticketRepository.findByIdPublico(idPublico)
                .orElseThrow(() -> new RuntimeException("Ticket não encontrado"));
    }

    public Ticket confirmar(UUID idPublico) {
        Ticket t = verificar(idPublico);

        if (t.isUsado()) {
            throw new RuntimeException("Ticket já utilizado!");
        }

        t.setUsado(true);
        t.setUsadoEm(java.time.LocalDateTime.now());

        return ticketRepository.save(t);
    }
}
