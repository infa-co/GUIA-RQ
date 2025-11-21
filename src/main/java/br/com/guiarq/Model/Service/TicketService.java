package br.com.guiarq.Model.Service;

import br.com.guiarq.Model.Entities.Ticket;
import br.com.guiarq.Model.Repository.TicketRepository;
import br.com.guiarq.utils.EmailSender;
import br.com.guiarq.utils.QrCodeGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TicketService {

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private EmailSender emailSender;

    @Autowired
    private QrCodeGenerator qrCodeGenerator;

    // ==========================================
    // 🔵 MÉTODO USADO PELO WEBHOOK (COMPRA)
    // ==========================================
    public void processarCompra(Long ticketId, String email, String nomeCliente, String nomeTicket) {
        try {
            String conteudo = "https://guiaranchoqueimado.com.br/ticket/" + ticketId;
            byte[] qrBytes = qrCodeGenerator.generateQrCode(conteudo);
            emailSender.sendTicketEmail(
                    email,
                    nomeCliente,
                    qrBytes,
                    nomeTicket
            );

            System.out.println("✔ COMPRA PROCESSADA COM SUCESSO");

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("❌ ERRO AO PROCESSAR COMPRA");
        }
    }

    // ==========================================
    // 🔵 RESTORE — MÉTODOS QUE O CONTROLLER USA
    // ==========================================

    /** Salva um ticket manualmente */
    public void salvar(Ticket t) {
        ticketRepository.save(t);
    }

    /** Lista todos os tickets */
    public List<Ticket> listarTodos() {
        return ticketRepository.findAll();
    }
}
