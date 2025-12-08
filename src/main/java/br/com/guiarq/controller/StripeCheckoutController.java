package br.com.guiarq.controller;

import br.com.guiarq.DTO.CheckoutRequest;
import br.com.guiarq.Model.Service.VoucherService;
import com.stripe.Stripe;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/stripe")
@CrossOrigin(origins = "*")
public class StripeCheckoutController {

    @Value("${STRIPE_SECRET_KEY}")
    private String secretKey;

    private final VoucherService voucherService;

    public StripeCheckoutController(VoucherService voucherService) {
        this.voucherService = voucherService;
    }

    @PostMapping("/create-checkout")
    public Map<String, Object> createCheckout(@RequestBody CheckoutRequest req) throws Exception {

        if (req.getCpf() != null && voucherService.usuarioPossuiVoucherAtivo(req.getCpf())) {
            throw new IllegalStateException("CPF_JA_POSSUI_TICKET_ATIVO");
        }

        Stripe.apiKey = secretKey;

        if (req.getAmount() == null || req.getAmount() <= 0) {
            throw new IllegalArgumentException("Valor inválido.");
        }

        Long amountInCents = Math.round(req.getAmount() * 100);
        Long quantidade = req.getQuantidade() != null ? req.getQuantidade() : 1L;

        SessionCreateParams.Builder builder = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl("https://guiaranchoqueimado.com.br/pages/sucesso.html")
                .setCancelUrl("https://guiaranchoqueimado.com.br/pages/cancelado.html")
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setQuantity(quantidade)
                                .setPriceData(
                                        SessionCreateParams.LineItem.PriceData.builder()
                                                .setCurrency("brl")
                                                .setUnitAmount(amountInCents)
                                                .setProductData(
                                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                .setName(req.getDescription())
                                                                .build()
                                                )
                                                .build()
                                )
                                .build()
                );

        builder.putMetadata("quantidade", quantidade.toString());

        boolean isPacote = Boolean.TRUE.equals(req.getPacote());

        if (req.getTicketId() != null) {
            builder.putMetadata("ticketId", req.getTicketId().toString());
            builder.putMetadata("pacote", "false"); // garante que NUNCA vira pacote
        } else {
            // Se NÃO tem ticketId → pode ser pacote ou avulso múltiplo
            if (isPacote) {
                builder.putMetadata("pacote", "true");
            } else {
                builder.putMetadata("pacote", "false"); // avulso múltiplo
            }
        }


        if (req.getEmail() != null) builder.putMetadata("email", req.getEmail());
        if (req.getNome() != null) builder.putMetadata("nome", req.getNome());
        if (req.getTelefone() != null) builder.putMetadata("telefone", req.getTelefone());
        if (req.getCpf() != null) builder.putMetadata("cpf", req.getCpf());

        SessionCreateParams params = builder.build();
        Session session = Session.create(params);

        Map<String, Object> response = new HashMap<>();
        response.put("url", session.getUrl());
        return response;
    }
}
