package br.com.guiarq.Controller;

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

    @Value("${stripe.webhook.secret}")
    private String endpointSecret;

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        Event event;

        try {
            event = Webhook.constructEvent(payload, sigHeader, endpointSecret);
        } catch (SignatureVerificationException e) {
            logger.error("❌ Assinatura inválida do Webhook Stripe", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
        } catch (Exception e) {
            logger.error("❌ Erro ao processar Webhook", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Webhook error");
        }

        logger.info("📩 Evento Stripe recebido: {}", event.getType());

        switch (event.getType()) {
            case "payment_intent.succeeded":
                processarPagamento(event);
                break;

            case "payment_intent.payment_failed":
                logger.warn("⚠ Pagamento falhou.");
                break;

            default:
                logger.warn("⚠ Evento não tratado: {}", event.getType());
        }

        return ResponseEntity.ok("Webhook OK");
    }

    private void processarPagamento(Event event) {

        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();

        deserializer.getObject().ifPresentOrElse(obj -> {

            PaymentIntent pi = (PaymentIntent) obj;

            Map<String, String> metadata = pi.getMetadata();

            Long ticketId = Long.parseLong(metadata.get("ticketId"));
            String email = metadata.get("email");
            String nome = metadata.get("nome");

            logger.info("💰 PAGAMENTO APROVADO PARA {}", email);

            // Buscar ticket original
            Ticket ticketBase = ticketRepository.findById(ticketId)
                    .orElse(null);

            if (ticketBase == null) {
                logger.error("❌ Ticket base não encontrado para ID {}", ticketId);
                return;
            }

            // Criar novo ticket gerado pela compra
            Ticket novo = new Ticket();
            novo.setNome(ticketBase.getNome());
            novo.setDescricao(ticketBase.getDescricao());
            novo.setPrecoOriginal(ticketBase.getPrecoOriginal());
            novo.setPrecoPromocional(ticketBase.getPrecoPromocional());
            novo.setExperiencia(ticketBase.getExperiencia());
            novo.setTipo(ticketBase.getTipo());
            novo.setParceiroId(ticketBase.getParceiroId());

            novo.setEmailCliente(email);
            novo.setNomeCliente(nome);
            novo.setDataCompra(LocalDateTime.now());

            novo.setIdPublico(UUID.randomUUID());
            UUID qr = UUID.randomUUID();
            novo.setQrToken(qr);

            novo.setCompraId(UUID.randomUUID());
            novo.setStatus("PAGO");
            novo.setUsado(false);
            novo.setCriadoEm(LocalDateTime.now());

            // Valor pago registrado
            novo.setValorPago(pi.getAmountReceived() / 100.0);

            ticketRepository.save(novo);

            logger.info("🎫 Ticket gerado: {}", novo.getIdPublico());

            // Enviar email com QR
            ticketService.processarCompra(
                    novo.getId(),
                    email,
                    nome,
                    novo.getNome()
            );

        }, () -> logger.error("❌ Falhou ao desserializar PaymentIntent"));
    }
}
