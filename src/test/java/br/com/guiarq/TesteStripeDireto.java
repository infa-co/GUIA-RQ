package br.com.guiarq;

import com.stripe.Stripe;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;

public class TesteStripeDireto {
    public static void main(String[] args) throws Exception {
        String key = System.getenv("STRIPE_SECRET_KEY");
        if (key == null || key.isBlank()) {
            System.out.println("❌ STRIPE_SECRET_KEY não encontrada!");
            return;
        }
        Stripe.apiKey = key;
        System.out.println("✅ Conectando com a API Stripe...");

        try {
            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl("https://example.com/success")
                    .setCancelUrl("https://example.com/cancel")
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setQuantity(1L)
                                    .setPriceData(
                                            SessionCreateParams.LineItem.PriceData.builder()
                                                    .setCurrency("brl")
                                                    .setUnitAmount(1000L)
                                                    .setProductData(
                                                            SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                    .setName("Teste Stripe SDK")
                                                                    .build()
                                                    )
                                                    .build()
                                    )
                                    .build()
                    )
                    .build();

            Session session = Session.create(params);
            System.out.println("✅ Sessão criada com sucesso!");
            System.out.println("Session ID: " + session.getId());
            System.out.println("URL do Checkout: " + session.getUrl());
        } catch (Exception e) {
            System.out.println("❌ Erro ao conectar com o Stripe:");
            e.printStackTrace();
        }
    }
}

