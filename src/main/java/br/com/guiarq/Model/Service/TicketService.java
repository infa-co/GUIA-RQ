package br.com.guiarq.Model.Service;

import br.com.guiarq.Model.Entities.Ticket;
import br.com.guiarq.Model.Repository.TicketRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

    public void processarCompra(Ticket ticket) {
        try {
            // Gera o link que aparecerá ao escanear o QR Code
            String conteudo = "https://guiaranchoqueimado.com.br/pages/validar/?qr=" + ticket.getQrToken();

            // Gera o QR Code em imagem
            byte[] qrBytes = qrCodeService.generateQrCodeBytes(conteudo, 300, 300);

            // Envia o email com o QR Code
            emailService.sendTicketEmail(
                    ticket.getEmailCliente(),
                    ticket.getNomeCliente(),
                    ticket.getTelefoneCliente(),
                    ticket.getCpfCliente(),
                    ticket.getNome(),
                    qrBytes
            );

            System.out.println("✔ COMPRA PROCESSADA");

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("❌ ERRO AO PROCESSAR COMPRA");
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
