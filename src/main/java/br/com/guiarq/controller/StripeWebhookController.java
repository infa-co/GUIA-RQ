package br.com.guiarq.controller;

import br.com.guiarq.Model.Entities.Ticket;
import br.com.guiarq.Model.Repository.TicketRepository;
import br.com.guiarq.Model.Service.TicketService;
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

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        Event event;

        try {
            event = Webhook.constructEvent(payload, sigHeader, endpointSecret);

        } catch (SignatureVerificationException e) {
            logger.error("❌ Assinatura inválida no webhook");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");

        } catch (Exception e) {
            logger.error("❌ Erro ao processar webhook", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Webhook error");
        }

        logger.info("📩 Evento Stripe recebido: {}", event.getType());

        if ("checkout.session.completed".equals(event.getType())) {
            processarCheckout(event);
        }

        return ResponseEntity.ok("Webhook OK");
    }

    private void processarCheckout(Event event) {

        Session session = (Session) event.getDataObjectDeserializer()
                .getObject()
                .orElse(null);

        if (session == null) {
            logger.error("❌ Não foi possível desserializar Session");
            return;
        }

        logger.info("🟦 Sessão desserializada com sucesso: {}", session.getId());

        String paymentIntentId = session.getPaymentIntent();

        if (paymentIntentId == null) {
            logger.error("❌ session.getPaymentIntent() veio nulo!");
            return;
        }

        logger.info("🟨 PaymentIntent ID recebido: {}", paymentIntentId);

        PaymentIntent pi;

        try {
            pi = PaymentIntent.retrieve(paymentIntentId);

        } catch (Exception e) {
            logger.error("❌ Erro ao consultar PaymentIntent no Stripe", e);
            return;
        }

        Map<String, String> metadata = pi.getMetadata();

        logger.info("🔎 METADATA RECEBIDO DO PAYMENT INTENT: {}", metadata);

        Long ticketId = Long.parseLong(metadata.get("ticketId"));
        String email = metadata.get("email");
        String nome = metadata.get("nome");
        String telefone = metadata.get("telefone");
        String cpf = metadata.get("cpf");

        Ticket base = ticketRepository.findById(ticketId).orElse(null);

        if (base == null) {
            logger.error("❌ Ticket base não encontrado no banco");
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
        novo.setTelefoneCliente(telefone);
        novo.setCpfCliente(cpf);
        novo.setDataCompra(LocalDateTime.now());

        novo.setIdPublico(UUID.randomUUID());
        novo.setQrToken(UUID.randomUUID());
        novo.setCompraId(UUID.randomUUID());
        novo.setStatus("PAGO");
        novo.setUsado(false);
        novo.setCriadoEm(LocalDateTime.now());
        novo.setValorPago(pi.getAmountReceived() / 100.0);

        ticketRepository.save(novo);

        logger.info("🎫 Ticket GERADO: {}", novo.getIdPublico());

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
