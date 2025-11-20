package br.com.guiarq.Model.Service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Service
public class StripeService {

    @Value("${stripe.secret-key}")
    private String secretKey;

    public Session createCheckoutSession(String successUrl, String cancelUrl, Long amount) throws StripeException {

        Stripe.apiKey = secretKey;

        Map<String, Object> params = new HashMap<>();
        params.put("mode", "payment");
        params.put("success_url", successUrl);
        params.put("cancel_url", cancelUrl);

        Map<String, Object> lineItem = new HashMap<>();
        lineItem.put("quantity", 1);

        Map<String, Object> priceData = new HashMap<>();
        priceData.put("currency", "brl");
        priceData.put("unit_amount", amount);

        priceData.put("product_data", Map.of("name", "Guia Rancho Queimado - Ticket"));

        lineItem.put("price_data", priceData);

        params.put("line_items", Arrays.asList(lineItem));

        return Session.create(params);
    }
}
