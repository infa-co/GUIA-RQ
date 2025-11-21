package br.com.guiarq.Model.Service;

import br.com.guiarq.Model.Entities.Ticket;
import br.com.guiarq.Model.Repository.TicketRepository;
import br.com.guiarq.utils.EmailSender;
import br.com.guiarq.utils.QrCodeGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TicketService {

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private EmailSender emailSender;

    @Autowired
    private QrCodeGenerator qrCodeGenerator;

    // -----------------------------
    // USADO PELO WEBHOOK
    // -----------------------------
    public void processarCompra(Ticket ticket) {
        enviarTicketSimples(ticket);
    }

    // -----------------------------
    // ENVIO DE TICKET INDIVIDUAL
    // -----------------------------
    public void enviarTicketSimples(Ticket ticket) {
        try {
            String conteudo = "https://guiaranchoqueimado.com.br/ticket/" + ticket.getIdPublico();

            byte[] qrBytes = qrCodeGenerator.generateQrCode(conteudo);

            emailSender.sendTicketEmail(
                    ticket.getEmailCliente(),
                    ticket.getNomeCliente(),
                    qrBytes,
                    ticket.getQrToken().toString()
            );

            System.out.println("✔ Ticket enviado para " + ticket.getEmailCliente());

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("❌ ERRO AO ENVIAR TICKET POR EMAIL");
        }
    }
}
