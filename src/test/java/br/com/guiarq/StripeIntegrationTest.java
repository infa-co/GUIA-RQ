package br.com.guiarq;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StripeIntegrationTest {

    private static String pk = "sk_test_51SP9kuRJDmp6Dshr7tuQZUWr2ZAabHjVJH01981luuhDNsN7ndMI9MT2QpjUjPJ7tXriHetPZZJYAkTEbn71Phob00C0GeeMif"; // (opcional, apenas se quiser verificar formatação)
    private static String sk = "sk_test_51SP9kuRJDmp6Dshr7tuQZUWr2ZAabHjVJH01981luuhDNsN7ndMI9MT2QpjUjPJ7tXriHetPZZJYAkTEbn71Phob00C0GeeMif";

    @BeforeAll
    static void setup() {
        sk = System.getenv("SECRET_KEY");
        pk = System.getenv("PUBLICATE_KEY"); // se não tiver, ignore essa var

        // pula o teste se não houver chave
        Assumptions.assumeTrue(sk != null && !sk.isBlank(), "STRIPE_SECRET_KEY ausente nos envs (modo teste).");

        // configura o SDK
        Stripe.apiKey = sk;
    }

    @Test
    @DisplayName("Stripe | Cria sessão de Checkout (teste) e retorna sessionId")
    void deveCriarCheckoutSession() throws StripeException {
        // R$ 77,00 => 7700 centavos
        long amount = 7700L;

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
                                                .setUnitAmount(amount)
                                                .setProductData(
                                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                .setName("Ticket Guia RQ (teste)")
                                                                .build()
                                                )
                                                .build()
                                )
                                .build()
                )
                .build();

        Session session = Session.create(params);

        assertNotNull(session, "Sessão não pode ser nula");
        assertNotNull(session.getId(), "sessionId não pode ser nulo");
        assertEquals("payment", session.getMode(), "Modo deve ser PAYMENT");
    }
}
