package br.com.guiarq.controller;

import br.com.guiarq.Model.Entities.Ticket;
import br.com.guiarq.Model.Repository.TicketRepository;
import br.com.guiarq.Model.Service.TicketService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
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

    // Render: STRIPE_WEBHOOK_SECRET=whsec_xxx
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
            logger.error("❌ Assinatura Stripe inválida", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");

        } catch (Exception e) {
            logger.error("❌ Erro genérico no webhook", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Webhook error");
        }

        logger.info("📩 Evento Stripe recebido: {}", event.getType());

        switch (event.getType()) {

            case "checkout.session.completed":
                processarCheckoutSession(event);
                break;

            case "payment_intent.succeeded":
                // Apenas log – não usamos esse evento para criar ticket
                logger.info("ℹ️ payment_intent.succeeded recebido (apenas log, lógica está em checkout.session.completed)");
                break;

            default:
                logger.info("ℹ️ Evento ignorado: {}", event.getType());
        }

        return ResponseEntity.ok("Webhook OK");
    }

    private void processarCheckoutSession(Event event) {

        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();

        deserializer.getObject().ifPresentOrElse(obj -> {

            if (!(obj instanceof Session)) {
                logger.error("❌ Objeto do evento não é uma Session. Tipo real: {}", obj.getClass().getName());
                return;
            }

            Session session = (Session) obj;

            Map<String, String> metadata = session.getMetadata();
            logger.info("🔎 METADATA RECEBIDO (SESSION): {}", metadata);

            try {
                String ticketIdStr = metadata.get("ticketId");
                String email = metadata.get("email");
                String nome = metadata.get("nome");
                String telefone = metadata.get("telefone");
                String cpf = metadata.get("cpf");

                logger.info("🔎 EMAIL: {}", email);
                logger.info("🔎 NOME: {}", nome);
                logger.info("🔎 TELEFONE: {}", telefone);
                logger.info("🔎 CPF: {}", cpf);

                if (ticketIdStr == null) {
                    logger.error("❌ ticketId ausente no metadata");
                    return;
                }

                Long ticketId = Long.parseLong(ticketIdStr);

                Ticket base = ticketRepository.findById(ticketId).orElse(null);

                if (base == null) {
                    logger.error("❌ Ticket base não encontrado para ID {}", ticketId);
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

                Long amountTotal = session.getAmountTotal(); // em centavos
                if (amountTotal != null) {
                    novo.setValorPago(amountTotal / 100.0);
                }

                ticketRepository.save(novo);
                logger.info("🎫 Ticket gerado e salvo. ID público: {}", novo.getIdPublico());

                // Envia o e-mail com QR code
                ticketService.processarCompra(
                        novo.getId(),
                        email,
                        nome,
                        telefone,
                        cpf,
                        novo.getNome()
                );

                logger.info("📧 processarCompra chamado com sucesso para {}", email);

            } catch (Exception e) {
                logger.error("❌ Erro ao processar checkout.session.completed", e);
            }

        }, () -> {
            logger.error("❌ Não foi possível desserializar o objeto do evento Stripe");
        });
    }
}
