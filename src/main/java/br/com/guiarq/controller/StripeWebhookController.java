package br.com.guiarq.controller;

import br.com.guiarq.Model.Entities.Ticket;
import br.com.guiarq.Model.Repository.TicketRepository;
import br.com.guiarq.Model.Service.TicketService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/stripe")
public class StripeWebhookController {

    private static final Logger logger = LoggerFactory.getLogger(StripeWebhookController.class);

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private TicketService ticketService;

    @Value("${STRIPE_WEBHOOK_SECRET}")
    private String endpointSecret;

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        Event event;

        try {
            event = Webhook.constructEvent(payload, sigHeader, endpointSecret);

        } catch (SignatureVerificationException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Webhook error");
        }

        logger.info("📩 Evento Stripe recebido: {}", event.getType());

        switch (event.getType()) {
            case "payment_intent.succeeded":
                processarPagamento(event);
                break;
        }

        return ResponseEntity.ok("Webhook OK");
    }

    private void processarPagamento(Event event) {

        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();

        deserializer.getObject().ifPresent(obj -> {

            PaymentIntent pi = (PaymentIntent) obj;
            Map<String, String> metadata = pi.getMetadata();

            Long ticketId = Long.parseLong(metadata.get("ticketId"));
            String email = metadata.get("email");
            String nome = metadata.get("nome");
            String telefone = metadata.get("telefone");
            String cpf = metadata.get("cpf");

            Ticket base = ticketRepository.findById(ticketId).orElse(null);

            if (base == null) {
                logger.error("Ticket base não encontrado.");
                return;
            }

            Ticket novo = new Ticket();
            novo.setNome(base.getNome());
            novo.setDescricao(base.getDescricao());
            novo.setPrecoOriginal(base.getPrecoOriginal());
            novo.setPrecoPromocional(base.getPrecoPromocional());
            novo.setExperiencia(base.getExperiencia());
            novo.setTipo(base.getTipo());
            novo.setParceiroId(base.getParceiroId());

            novo.setEmailCliente(email);
            novo.setNomeCliente(nome);
            novo.setTelefoneCliente(telefone); // NOVO
            novo.setCpfCliente(cpf); // NOVO
            novo.setDataCompra(LocalDateTime.now());

            novo.setIdPublico(UUID.randomUUID());
            novo.setQrToken(UUID.randomUUID());
            novo.setCompraId(UUID.randomUUID());
            novo.setStatus("PAGO");
            novo.setUsado(false);
            novo.setCriadoEm(LocalDateTime.now());
            novo.setValorPago(pi.getAmountReceived() / 100.0);

            ticketRepository.save(novo);

            logger.info("🎫 Ticket gerado: {}", novo.getIdPublico());

            // Agora enviamos todos os dados
            ticketService.processarCompra(
                    novo.getId(),
                    email,
                    nome,
                    telefone,
                    cpf,
                    novo.getNome()
            );
        });
    }

}
