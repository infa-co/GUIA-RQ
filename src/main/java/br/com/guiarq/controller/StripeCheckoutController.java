package br.com.guiarq.controller;

import br.com.guiarq.DTO.CheckoutRequest;
import br.com.guiarq.Model.Service.StripeService;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
public class StripeCheckoutController {

    @Autowired
    private StripeService stripeService;

    @PostMapping("/api/stripe/checkout")
    public ResponseEntity<Map<String, Object>> createCheckout(@RequestBody CheckoutRequest req) throws StripeException {

        if (req.getAmount() == null || req.getAmount() <= 0) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Valor inválido."));
        }

        long amountInCents = Math.round(req.getAmount());

        String successUrl = "https://guiaranchoqueimado.com.br/pages/sucesso.html";
        String cancelUrl  = "https://guiaranchoqueimado.com.br/pages/cancelado.html";

        Session session = stripeService.createCheckoutSession(
                successUrl,
                cancelUrl,
                amountInCents
        );

        Map<String, Object> response = new HashMap<>();
        response.put("url", session.getUrl());

        return ResponseEntity.ok(response);
    }
}
