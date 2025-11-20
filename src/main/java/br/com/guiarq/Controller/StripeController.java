package br.com.guiarq.Controller;

import com.stripe.Stripe;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import br.com.guiarq.DTO.CheckoutRequest;

@RestController
@RequestMapping("/api/stripe")
@CrossOrigin(origins = "*")
public class StripeController {

    @Value("${STRIPE_SECRET_KEY}")
    private String stripeSecretKey;

    @PostMapping("/checkout")
    public ResponseEntity<?> criarCheckout(@RequestBody CheckoutRequest req) throws Exception {

        // Definir a chave da API Stripe
        Stripe.apiKey = stripeSecretKey;

        // Criar sessão de checkout
        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl("https://guia-rq.com.br/pages/sucesso.html")
                .setCancelUrl("https://guia-rq.com.br/pages/cancelado.html")

                // Linha única com valor total
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setQuantity(1L)
                                .setPriceData(
                                        SessionCreateParams.LineItem.PriceData.builder()
                                                .setCurrency("brl")
                                                .setUnitAmount((long) (req.getAmount() * 100)) // Stripe usa centavos
                                                .setProductData(
                                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                .setName(req.getDescription())
                                                                .build()
                                                )
                                                .build()
                                )
                                .build()
                )

                // Metadados importantes para o Webhook
                .putMetadata("ticketId", req.getTicketId() != null ? req.getTicketId().toString() : "0")
                .putMetadata("email", req.getEmail())
                .putMetadata("nome", req.getNome())

                .build();

        Session session = Session.create(params);

        return ResponseEntity.ok(session.getUrl());
    }
}
