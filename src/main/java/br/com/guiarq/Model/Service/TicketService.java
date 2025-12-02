package br.com.guiarq.Model.Service;

import br.com.guiarq.Model.Entities.Ticket;
import br.com.guiarq.Model.Entities.TicketCatalogo;
import br.com.guiarq.Model.Repository.TicketCatalogoRepository;
import br.com.guiarq.Model.Repository.TicketRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class TicketService {

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private TicketCatalogoRepository catalogoRepository;

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

    /**
     * PROCESSA COMPRA DE TICKET AVULSO
     * Gera o QR, obtém o NOME correto a partir do catálogo,
     * preenche no Ticket e envia o e-mail corretamente.
     */
    public void processarCompra(Ticket ticket) {
        try {
            // 1️⃣ Buscar o ticket no catálogo
            TicketCatalogo catalogo = catalogoRepository.findById(ticket.getTicketCatalogoId())
                    .orElseThrow(() -> new RuntimeException("Ticket catálogo não encontrado: ID " + ticket.getTicketCatalogoId()));

            // 2️⃣ Preencher nome dentro do ticket
            ticket.setNome(catalogo.getNome());

            // 3️⃣ Gerar QR Code
            String conteudo = "https://guiaranchoqueimado.com.br/pages/validar-ticket.html?qr=" + ticket.getQrToken();
            byte[] qrBytes = qrCodeService.generateQrCodeBytes(conteudo, 300, 300);

            // 4️⃣ Enviar email com o nome do ticket correto
            emailService.sendTicketEmail(
                    ticket.getEmailCliente(),
                    ticket.getNomeCliente(),
                    ticket.getTelefoneCliente(),
                    ticket.getCpfCliente(),
                    catalogo.getNome(), // <-- AQUI VAI O NOME DO CATÁLOGO
                    qrBytes
            );

            System.out.println("📩 Ticket avulso enviado: " + catalogo.getNome());

        } catch (Exception e) {
            throw new RuntimeException("Erro ao processar compra: " + e.getMessage());
        }
    }

    /**
     * PROCESSA COMPRA DE PACOTE
     * Cada ticket do pacote carrega o nome do catálogo.
     */
    public void processarPacote(List<Ticket> tickets) {
        try {
            // Lista de bytes do QR
            List<byte[]> qrCodes = new java.util.ArrayList<>();

            for (Ticket t : tickets) {

                // Buscar nome no catálogo
                TicketCatalogo catalogo = catalogoRepository.findById(t.getTicketCatalogoId())
                        .orElseThrow(() -> new RuntimeException("Ticket catálogo não encontrado: ID " + t.getTicketCatalogoId()));

                // Preencher nome no ticket
                t.setNome(catalogo.getNome());

                // Gerar QR Code
                String conteudo = "https://guiaranchoqueimado.com.br/pages/validar-ticket.html?qr=" + t.getQrToken();
                byte[] qr = qrCodeService.generateQrCodeBytes(conteudo, 300, 300);
                qrCodes.add(qr);

                // Salvar ticket individualmente (se ainda não salvo)
                ticketRepository.save(t);
            }

            // Enviar todos no mesmo email
            emailService.sendPacoteTicketsEmail(
                    tickets.get(0).getEmailCliente(),
                    tickets.get(0).getNomeCliente(),
                    tickets.get(0).getTelefoneCliente(),
                    tickets.get(0).getCpfCliente(),
                    tickets,
                    qrCodes
            );

            System.out.println("📦 Pacote enviado: " + tickets.size() + " tickets");

        } catch (Exception e) {
            throw new RuntimeException("Erro ao processar pacote: " + e.getMessage());
        }
    }
    public Ticket verificar(UUID idPublico) {
        return ticketRepository.findByIdPublico(idPublico)
                .orElseThrow(() -> new RuntimeException("Ticket não encontrado"));
    }

    public void confirmar(UUID idPublico) {
        Ticket ticket = ticketRepository.findByIdPublico(idPublico)
                .orElseThrow(() -> new RuntimeException("Ticket não encontrado"));

        if (ticket.isUsado()) {
            throw new RuntimeException("Ticket já foi utilizado");
        }

        ticket.setUsado(true);
        ticket.setUsadoEm(LocalDateTime.now());
        ticketRepository.save(ticket);
    }

}
