package br.com.guiarq.Model.Service;

import com.stripe.Stripe;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;

public class StripeService {
    public StripeService() {
        // ⚠️ Troque pela sua chave secreta do Stripe
        Stripe.apiKey = "rqguiagrupo1@";
    }

    public String criarSessaoCheckout(String nomeTicket, double preco, String successUrl, String cancelUrl) throws Exception {
        SessionCreateParams params =
                SessionCreateParams.builder()
                        .setMode(SessionCreateParams.Mode.PAYMENT)
                        .setSuccessUrl(successUrl)
                        .setCancelUrl(cancelUrl)
                        .addLineItem(
                                SessionCreateParams.LineItem.builder()
                                        .setQuantity(1L)
                                        .setPriceData(
                                                SessionCreateParams.LineItem.PriceData.builder()
                                                        .setCurrency("brl")
                                                        .setUnitAmount((long) (preco * 100))
                                                        .setProductData(
                                                                SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                        .setName(nomeTicket)
                                                                        .build()
                                                        )
                                                        .build()
                                        )
                                        .build()
                        )
                        .build();

        Session session = Session.create(params);
        return session.getUrl(); // URL do checkout
    }
}
