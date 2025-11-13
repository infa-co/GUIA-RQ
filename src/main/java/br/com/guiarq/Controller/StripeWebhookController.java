package br.com.guiarq.Controller;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stripe")
public class StripeWebhookController {

    private static final Logger logger = LoggerFactory.getLogger(StripeWebhookController.class);

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
            logger.error("Assinatura inválida do webhook Stripe", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
        } catch (Exception e) {
            logger.error("Erro ao processar webhook", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Webhook error");
        }

        logger.info("Evento recebido: {}", event.getType());

        // Tratamento dos eventos recebidos
        switch (event.getType()) {
            case "payment_intent.succeeded":
                handlePaymentSucceeded(event);
                break;

            case "payment_intent.payment_failed":
                handlePaymentFailed(event);
                break;

            case "charge.refunded":
                handleChargeRefunded(event);
                break;

            default:
                logger.warn("⚠Evento não tratado: {}", event.getType());
        }

        return ResponseEntity.ok("Webhook processed successfully");
    }

    private void handlePaymentSucceeded(Event event) {
        EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
        dataObjectDeserializer.getObject().ifPresentOrElse(
                obj -> {
                    PaymentIntent paymentIntent = (PaymentIntent) obj;
                    logger.info("Pagamento aprovado. ID: {}, Valor: {}, Status: {}",
                            paymentIntent.getId(),
                            paymentIntent.getAmountReceived() / 100.0,
                            paymentIntent.getStatus());

                },
                () -> logger.error("Falha ao desserializar PaymentIntent"));
    }

    private void handlePaymentFailed(Event event) {
        EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
        dataObjectDeserializer.getObject().ifPresentOrElse(
                obj -> {
                    PaymentIntent paymentIntent = (PaymentIntent) obj;
                    logger.warn("Pagamento falhou. ID: {}, Motivo: {}",
                            paymentIntent.getId(),
                            paymentIntent.getLastPaymentError() != null
                                    ? paymentIntent.getLastPaymentError().getMessage()
                                               : "Desconhecido");

                },
                () -> logger.error("Falha ao desserializar PaymentIntent"));
    }

    private void handleChargeRefunded(Event event) {
        EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
        dataObjectDeserializer.getObject().ifPresentOrElse(
                obj -> logger.info("Reembolso detectado: {}", obj),
                () -> logger.error("Falha ao desserializar Charge"));
    }
}
