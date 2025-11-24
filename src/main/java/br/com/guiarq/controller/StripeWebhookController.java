package br.com.guiarq.controller;

import br.com.guiarq.Model.Entities.Ticket;
import br.com.guiarq.Model.Repository.TicketRepository;
import br.com.guiarq.Model.Service.TicketService;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.*;
import com.stripe.model.checkout.Session;
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

    @Value("${stripe.secret.key}")
    private String stripeKey;

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

        if ("checkout.session.completed".equals(event.getType())) {
            processarCheckout(event);
        }

        return ResponseEntity.ok("OK");
    }

    private void processarCheckout(Event event) {

        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();

        if (!deserializer.getObject().isPresent()) {
            logger.error("❌ Falha ao desserializar checkout.session");
            return;
        }

        Session session = (Session) deserializer.getObject().get();

        logger.info("🔎 Session ID: {}", session.getId());
        logger.info("🔎 Metadata: {}", session.getMetadata());

        Map<String, String> metadata = session.getMetadata();

        String email = metadata.get("email");
        String nome = metadata.get("nome");
        String telefone = metadata.get("telefone");
        String cpf = metadata.get("cpf");

        if (email == null || nome == null) {
            logger.error("❌ Metadata incompleto. Encerrando.");
            return;
        }

        // Recupera o PaymentIntent
        Stripe.apiKey = stripeKey;

        PaymentIntent pi;
        try {
            pi = PaymentIntent.retrieve(session.getPaymentIntent());
        } catch (Exception e) {
            logger.error("❌ Erro ao recuperar PaymentIntent: {}", e.getMessage());
            return;
        }

        double valorPago = pi.getAmountReceived() / 100.0;

        // Criar ticket novo
        Ticket novo = new Ticket();
        novo.setNome("Compra via Guia RQ");
        novo.setEmailCliente(email);
        novo.setNomeCliente(nome);
        novo.setTelefoneCliente(telefone);
        novo.setCpfCliente(cpf);
        novo.setValorPago(valorPago);
        novo.setStatus("PAGO");
        novo.setCriadoEm(LocalDateTime.now());
        novo.setDataCompra(LocalDateTime.now());
        novo.setUsado(false);
        novo.setIdPublico(UUID.randomUUID());
        novo.setQrToken(UUID.randomUUID());
        novo.setCompraId(UUID.randomUUID());

        ticketRepository.save(novo);

        logger.info("🎫 Ticket gerado: {}", novo.getIdPublico());

        ticketService.processarCompra(
                novo.getId(),
                email,
                nome,
                telefone,
                cpf,
                novo.getNome()
        );
    }
}
