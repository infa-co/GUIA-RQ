package br.com.guiarq.Controller;


import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/webhook/stripe")
public class StripeWebhookController {
    @Value("${stripe.webhook.secret}")
    private String endpointSecret;

    @PostMapping
    public ResponseEntity<String> handleStripeEvent(@RequestBody String payload,
                                                    @RequestHeader("Stripe-Signature") String sigHeader) {
        Event event;

        try {
            // Verifica assinatura e decodifica o evento recebido
            event = Webhook.constructEvent(payload, sigHeader, endpointSecret);
        } catch (SignatureVerificationException e) {
            System.out.println("Erro na verificação da assinatura: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Assinatura inválida");
        }

        // Loga o tipo de evento recebido
        System.out.println("Evento recebido: " + event.getType());

        // Exemplo de tratamento: pagamento confirmado
        if ("checkout.session.completed".equals(event.getType())) {
            System.out.println("Pagamento confirmado!");
            //no futuro podemos atualizar o status do pedido/reserva
        }

        return ResponseEntity.ok("Evento processado com sucesso");
    }
}
