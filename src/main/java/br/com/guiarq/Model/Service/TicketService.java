package br.com.guiarq.Model.Service;

import br.com.guiarq.Model.Dao.TicketDAO;
import br.com.guiarq.utils.QrCodeGenerator;
import br.com.guiarq.utils.EmailSender;

import java.util.UUID;

public class TicketService {

    private final TicketDAO ticketDAO = new TicketDAO();
    private final QrCodeGenerator qrCodeGenerator = new QrCodeGenerator();
    private final EmailSender emailSender = new EmailSender();

    public UUID gerarCompra(int usuarioId, int ticketId, String emailCliente, String nomeTicket) {

        UUID qrToken = UUID.randomUUID();

        ticketDAO.registrarCompraComToken(usuarioId, ticketId, qrToken);

        String urlValidacao = "https://api.guiaranchoqueimado.com.br/api/validar/" + qrToken;

        byte[] qrCode = qrCodeGenerator.generateQrBytes(urlValidacao);

        emailSender.enviarQRCode(
                emailCliente,
                "Seu Ticket do Guia RQ - " + nomeTicket,
                "Obrigado pela sua compra! Apresente o QR Code no estabelecimento.",
                qrCode
        );

        return qrToken;
    }
}
