package br.com.guiarq.controller;

import br.com.guiarq.Model.Entities.Ticket;
import br.com.guiarq.Model.Repository.TicketRepository;
import br.com.guiarq.Model.Service.TicketService;
import com.stripe.Stripe;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
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

    @Value("${STRIPE_SECRET_KEY}")
    private String stripeSecretKey;

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        logger.info("📩 Webhook recebido");

        try {
            Webhook.constructEvent(payload, sigHeader, endpointSecret);
        } catch (Exception e) {
            logger.error("❌ ERRO DE ASSINATURA: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
        }

        JSONObject json = new JSONObject(payload);
        String eventType = json.getString("type");

        logger.info("📌 Evento recebido: {}", eventType);

        if ("checkout.session.completed".equals(eventType)) {
            processCheckout(json);
        }

        return ResponseEntity.ok("OK");
    }

    private void processCheckout(JSONObject json) {

        JSONObject data = json.getJSONObject("data").getJSONObject("object");

        // ⁃ Metadados
        JSONObject metadata = data.optJSONObject("metadata");

        if (metadata == null) {
            logger.error("❌ Metadata vazio");
            return;
        }

        String email = metadata.optString("email", null);
        String nome = metadata.optString("nome", null);
        String telefone = metadata.optString("telefone", null);
        String cpf = metadata.optString("cpf", null);

        logger.info("📬 Email: {}", email);
        logger.info("👤 Nome: {}", nome);
        logger.info("📱 Telefone: {}", telefone);
        logger.info("🧾 CPF: {}", cpf);

        // ⁃ PaymentIntent
        String paymentIntentId = data.getString("payment_intent");

        Stripe.apiKey = stripeSecretKey;

        double valorPago;
        try {
            PaymentIntent pi = PaymentIntent.retrieve(paymentIntentId);
            valorPago = pi.getAmountReceived() / 100.0;
        } catch (Exception e) {
            logger.error("❌ Falha ao recuperar PaymentIntent: {}", e.getMessage());
            return;
        }

        // ⁃ Criar ticket
        Ticket ticket = new Ticket();
        ticket.setNome("Ticket Guia Rancho Queimado");
        ticket.setEmailCliente(email);
        ticket.setNomeCliente(nome);
        ticket.setTelefoneCliente(telefone);
        ticket.setCpfCliente(cpf);
        ticket.setValorPago(valorPago);
        ticket.setStatus("PAGO");
        ticket.setCriadoEm(LocalDateTime.now());
        ticket.setDataCompra(LocalDateTime.now());
        ticket.setUsado(false);

        ticket.setIdPublico(UUID.randomUUID());
        ticket.setQrToken(UUID.randomUUID());
        ticket.setCompraId(UUID.randomUUID());

        ticketRepository.save(ticket);

        logger.info("🎫 Ticket criado: {}", ticket.getIdPublico());

        // ⁃ Enviar email com QR Code
        ticketService.processarCompra(
                ticket.getId(),
                email,
                nome,
                telefone,
                cpf,
                ticket.getNome()
        );

        logger.info("📧 Email enviado com sucesso!");
    }
}
