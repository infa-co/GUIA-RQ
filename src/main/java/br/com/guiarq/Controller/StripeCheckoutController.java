package br.com.guiarq.Controller;

import br.com.guiarq.DTO.CheckoutRequest;
import br.com.guiarq.Model.Service.StripeService;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/stripe")
@CrossOrigin(origins = "*")
public class StripeCheckoutController {

    @Autowired
    private StripeService stripeService;

    @PostMapping("/checkout")
    public Map<String, Object> createCheckout(@RequestBody CheckoutRequest req) throws StripeException {

        // Validação básica
        if (req.getAmount() == null || req.getAmount() <= 0) {
            throw new IllegalArgumentException("Valor inválido.");
        }

        Long amountInCents = Math.round(req.getAmount());

        String successUrl = "https://guiaranchoqueimado.com.br/pages/sucesso.html";
        String cancelUrl = "https://guiaranchoqueimado.com.br/pages/cancelado.html";

        Session session = stripeService.createCheckoutSession(
                successUrl,
                cancelUrl,
                amountInCents
        );

        Map<String, Object> response = new HashMap<>();
        response.put("url", session.getUrl()); // link direto para o Stripe Checkout
        return response;
    }
}
