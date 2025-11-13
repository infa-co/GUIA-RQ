package br.com.guiarq.Controller;

import com.stripe.Stripe;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/stripe")
public class PaymentController {

    public PaymentController() {
        String stripeKey = System.getenv("STRIPE_SECRET_KEY");

        if (stripeKey == null || stripeKey.isBlank()) {
            System.err.println("⚠️ Variável STRIPE_SK não definida nas variáveis de ambiente!");
            throw new RuntimeException("Chave STRIPE_SK ausente. Defina-a antes de iniciar o servidor.");
        }
        Stripe.apiKey = stripeKey;
    }

    @PostMapping("/create-checkout-session")
    public ResponseEntity<Map<String, Object>> createCheckoutSession(@RequestBody Map<String, Object> payload) {
        try {
            List<Map<String, Object>> carrinho = (List<Map<String, Object>>) payload.get("carrinho");
            Map<String, Object> resumo = (Map<String, Object>) payload.get("resumo");

            if (carrinho == null || carrinho.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Carrinho vazio."));
            }

            double total = resumo != null && resumo.get("totalFinal") != null
                    ? Double.parseDouble(resumo.get("totalFinal").toString())
                    : carrinho.stream()
                    .mapToDouble(item -> Double.parseDouble(item.get("preco").toString()))
                    .sum();

            long totalEmCentavos = Math.round(total * 100);

            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl("http://localhost:8080/pages/sucesso.html")
                    .setCancelUrl("http://localhost:8080/pages/cancelado.html")
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setQuantity(1L)
                                    .setPriceData(
                                            SessionCreateParams.LineItem.PriceData.builder()
                                                    .setCurrency("brl")
                                                    .setUnitAmount(totalEmCentavos)
                                                    .setProductData(
                                                            SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                    .setName("Pagamento Guia RQ")
                                                                    .build()
                                                    )
                                                    .build()
                                    )
                                    .build()
                    )
                    .build();

            Session session = Session.create(params);

            return ResponseEntity.ok(Map.of("id", session.getId(), "url", session.getUrl()));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}
