package br.com.guiarq.Controller;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/stripe")
public class PaymentController {

    @PostMapping("/create-checkout-session")
    public ResponseEntity<Map<String, Object>> createCheckoutSession(@RequestBody Map<String, Object> body)
            throws StripeException {

        // 1) Lê a secret via variável de ambiente
        String secret = System.getenv("STRIPE_SECRET_KEY");
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("STRIPE_SECRET_KEY não configurada no ambiente.");
        }
        Stripe.apiKey = secret;

        // 2) Extrai total do body (fallback p/ 22,00)
        BigDecimal total = new BigDecimal("22.00");
        try {
            Map<String, Object> resumo = (Map<String, Object>) body.get("resumo");
            if (resumo != null && resumo.get("totalFinal") != null) {
                total = new BigDecimal(String.valueOf(resumo.get("totalFinal")));
            }
        } catch (Exception ignored) {
        }

        // 3) Converte para centavos (Stripe usa inteiro)
        long amountInCents = total
                .setScale(2, RoundingMode.HALF_UP)
                .multiply(new BigDecimal(100))
                .longValueExact();

        // 4) URLs de retorno
        String origin = "http://localhost:8080"; // se publicar, troque pelo domínio
        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(origin + "/pages/sucesso.html")
                .setCancelUrl(origin + "/pages/cancelado.html")
                .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                .addPaymentMethodType(SessionCreateParams.PaymentMethodType.BLIK) // opcional
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setQuantity(1L)
                                .setPriceData(
                                        SessionCreateParams.LineItem.PriceData.builder()
                                                .setCurrency("brl")
                                                .setUnitAmount(amountInCents)
                                                .setProductData(
                                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                .setName("Guia RQ – Pedido")
                                                                .build()
                                                ).build()
                                ).build()
                ).build();

        Session session = Session.create(params);

        Map<String, Object> resp = new HashMap<>();
        resp.put("id", session.getId());
        return ResponseEntity.ok(resp);
    }
}
