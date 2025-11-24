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

    public void processarCompra(
            Long ticketId,
            String email,
            String nomeCliente,
            String telefone,
            String cpf,
            String nomeTicket
    ) {
        try {
            // 🔵 1. Conteúdo que vira o QR Code
            String conteudo = "https://guiaranchoqueimado.com.br/ticket/" + ticketId;

            // 🔵 2. Gera o QR Code
            byte[] qrBytes = qrCodeService.generateQrCodeBytes(conteudo, 300, 300);

            // 🔵 3. Envia por e-mail com todos os dados disponíveis
            emailService.sendTicketEmail(
                    email,
                    nomeCliente,
                    telefone,
                    cpf,
                    nomeTicket,
                    qrBytes
            );

            System.out.println("✔ COMPRA PROCESSADA COM SUCESSO");

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("❌ ERRO AO PROCESSAR COMPRA: " + e.getMessage());
        }
    }

    public void salvar(Ticket t) {
        ticketRepository.save(t);
    }

    public List<Ticket> listarTodos() {
        return ticketRepository.findAll();
    }
}
