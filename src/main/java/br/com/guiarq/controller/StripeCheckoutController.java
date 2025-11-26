package br.com.guiarq.controller;

import br.com.guiarq.DTO.CheckoutRequest;
import com.stripe.Stripe;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/stripe")
@CrossOrigin(origins = "*")
public class StripeCheckoutController {

    @Value("${STRIPE_SECRET_KEY}")
    private String secretKey;

    @PostMapping("/create-checkout")
    public Map<String, Object> createCheckout(@RequestBody CheckoutRequest req) throws Exception {

        Stripe.apiKey = secretKey;

        if (req.getAmount() == null || req.getAmount() <= 0) {
            throw new IllegalArgumentException("Valor inválido.");
        }

        Long amountInCents = Math.round(req.getAmount() * 100); // valor em centavos

        SessionCreateParams.Builder builder = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl("https://guiaranchoqueimado.com.br/pages/sucesso.html")
                .setCancelUrl("https://guiaranchoqueimado.com.br/pages/cancelado.html")
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setQuantity(1L)
                                .setPriceData(
                                        SessionCreateParams.LineItem.PriceData.builder()
                                                .setCurrency("brl")
                                                .setUnitAmount(amountInCents)
                                                .setProductData(
                                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                .setName(req.getDescription())
                                                                .build()
                                                )
                                                .build()
                                )
                                .build()
                );

        // ticketId (para avulsos)
        if (req.getTicketId() != null) {
            builder.putMetadata("ticketId", req.getTicketId().toString());
        }

        // flag de pacote
        if (Boolean.TRUE.equals(req.getPacote())) {
            builder.putMetadata("pacote", "true");
        }

        if (req.getEmail() != null) {
            builder.putMetadata("email", req.getEmail());
        }
        if (req.getNome() != null) {
            builder.putMetadata("nome", req.getNome());
        }
        if (req.getTelefone() != null) {
            builder.putMetadata("telefone", req.getTelefone());
        }
        if (req.getCpf() != null) {
            builder.putMetadata("cpf", req.getCpf());
        }

        SessionCreateParams params = builder.build();
        Session session = Session.create(params);

        Map<String, Object> response = new HashMap<>();
        response.put("url", session.getUrl());
        return response;
    }
}
