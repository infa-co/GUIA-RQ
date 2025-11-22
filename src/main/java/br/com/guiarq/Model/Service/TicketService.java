package br.com.guiarq.Model.Service;

import br.com.guiarq.Model.Entities.Ticket;
import br.com.guiarq.Model.Repository.TicketRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TicketService {

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private QrCodeService qrCodeService;

    // ==========================================
    // 🔵 MÉTODO USADO PELO WEBHOOK (COMPRA)
    // ==========================================
    public void processarCompra(Long ticketId, String email, String nomeCliente, String nomeTicket) {
        try {

            // Conteúdo que vira o QR Code
            String conteudo = "https://guiaranchoqueimado.com.br/ticket/" + ticketId;

            // Gera o QR
            byte[] qrBytes = qrCodeService.generateQrCodeBytes(conteudo, 300, 300);

            // Envia o ticket por e-mail
            emailService.sendTicketEmail(
                    email,
                    nomeTicket,   // pode alterar para outro campo se quiser
                    qrBytes
            );

            System.out.println("✔ COMPRA PROCESSADA COM SUCESSO");

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("❌ ERRO AO PROCESSAR COMPRA: " + e.getMessage());
        }
    }

    // ==========================================
    // 🔵 RESTORE — MÉTODOS QUE O CONTROLLER USA
    // ==========================================

    public void salvar(Ticket t) {
        ticketRepository.save(t);
    }

    public List<Ticket> listarTodos() {
        return ticketRepository.findAll();
    }
}
