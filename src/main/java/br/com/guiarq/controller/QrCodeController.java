package br.com.guiarq.controller;

import br.com.guiarq.Model.Entities.Ticket;
import br.com.guiarq.Model.Repository.TicketRepository;
import br.com.guiarq.Model.Service.QrCodeService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/qr")
public class QrCodeController {

    private final QrCodeService qrCodeService;
    private final TicketRepository ticketRepository;

    public QrCodeController(QrCodeService qrCodeService, TicketRepository ticketRepository) {
        this.qrCodeService = qrCodeService;
        this.ticketRepository = ticketRepository;
    }

    @GetMapping(value = "/{idPublico}", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> gerarQr(@PathVariable String idPublico) throws Exception {

        UUID uuid;

        try {
            uuid = UUID.fromString(idPublico);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }

        Ticket ticket = ticketRepository.findByIdPublico(uuid).orElse(null);

        if (ticket == null)
            return ResponseEntity.notFound().build();

        String conteudo = "https://guiaranchoqueimado.com.br/pages/validar-ticket.html?qr=" + ticket.getQrToken();


        byte[] qr = qrCodeService.generateQrCodeBytes(conteudo, 400, 400);

        return ResponseEntity.ok(qr);
    }
}
