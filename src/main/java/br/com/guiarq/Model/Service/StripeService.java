package br.com.guiarq.Model.Service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Service
public class StripeService {
    @Autowired
    private EmailService emailService;

    @Autowired
    private QrCodeService qrCodeService;


    @Value("${stripe.secret-key}")
    private String secretKey;

    public Session createCheckoutSession(
            String successUrl,
            String cancelUrl,
            Long amountInCents,
            String customerEmail,
            String description
    ) throws StripeException {

        Stripe.apiKey = secretKey;

        // Configurações básicas da sessão
        Map<String, Object> params = new HashMap<>();
        params.put("mode", "payment");
        params.put("success_url", successUrl);
        params.put("cancel_url", cancelUrl);

        // Email do comprador
        if (customerEmail != null && !customerEmail.isEmpty()) {
            params.put("customer_email", customerEmail);
        }

        // Item da compra (preço dinâmico)
        Map<String, Object> lineItem = new HashMap<>();
        lineItem.put("quantity", 1);

        Map<String, Object> priceData = new HashMap<>();
        priceData.put("currency", "brl");
        priceData.put("unit_amount", amountInCents);

        // Nome que vai aparecer no Stripe
        Map<String, Object> productData = new HashMap<>();
        productData.put("name", description != null ? description : "Guia Rancho Queimado - Ticket");

        priceData.put("product_data", productData);

        lineItem.put("price_data", priceData);

        params.put("line_items", Arrays.asList(lineItem));

        return Session.create(params);
    }
    public void processarCompra(String email, String codigoValidacao) {
        try {
            // gerar QR
            byte[] qrBytes = qrCodeService.generateQrCodeBytes(codigoValidacao, 300, 300);

            // enviar email
            emailService.sendTicketEmail(
                    email,
                    codigoValidacao,
                    qrBytes
            );

            System.out.println("Email de ticket enviado com sucesso para " + email);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Erro ao processar compra e enviar ticket", e);
        }
    }

}