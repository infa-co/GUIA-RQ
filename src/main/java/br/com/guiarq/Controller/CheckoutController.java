package br.com.guiarq.Controller;

import br.com.guiarq.Model.Service.StripeService;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")

public class CheckoutController  {

    @Autowired
    private StripeService stripeService;

    @PostMapping("/checkout-session")
    public Map<String, Object> createCheckoutSession(@RequestParam Long amount) throws StripeException {
        // URLs de redirecionamento
        String successUrl = "https://guiaranchoqueimado.com.br/pages/sucesso.html";
        String cancelUrl = "https://guiaranchoqueimado.com.br/pages/cancelado.html";

        // Cria sessão no Stripe
        Session session = stripeService.createCheckoutSession(successUrl, cancelUrl, amount);

        // Retorna o ID da sessão para o front-end
        Map<String, Object> response = new HashMap<>();
        response.put("id", session.getId());
        return response;
    }
}
