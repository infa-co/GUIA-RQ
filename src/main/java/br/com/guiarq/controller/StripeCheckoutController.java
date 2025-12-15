package br.com.guiarq.controller;

import br.com.guiarq.DTO.CheckoutRequest;
import br.com.guiarq.Model.Entities.Ticket;
import br.com.guiarq.Model.Repository.TicketRepository;
import br.com.guiarq.Model.Repository.TicketCatalogoRepository;
import br.com.guiarq.Model.Service.TicketService;
import com.stripe.Stripe;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/stripe")
@CrossOrigin(origins = "*")
public class StripeCheckoutController {

    @Value("${STRIPE_SECRET_KEY}")
    private String secretKey;

    private final TicketRepository ticketRepository;
    private final TicketService ticketService;

    public StripeCheckoutController(TicketRepository ticketRepository,
                                    TicketService ticketService) {
        this.ticketRepository = ticketRepository;
        this.ticketService = ticketService;
    }

    @PostMapping("/create-checkout")
    public Map<String, Object> createCheckout(@RequestBody CheckoutRequest req) throws Exception {

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

        // if explicit package chosen by front-end
        if (Boolean.TRUE.equals(req.getPacote())) {
            // marque pacote explicitamente
            builder.putMetadata("pacote", "true");
            // preferencialmente também envie ticketId = 11 para rastreio
            builder.putMetadata("ticketId", "11");
        }

        if (req.getTicketId() != null) {
            builder.putMetadata("ticketId", req.getTicketId().toString());
        }
        if (req.getEmail() != null) builder.putMetadata("email", req.getEmail());
        if (req.getNome() != null) builder.putMetadata("nome", req.getNome());
        if (req.getTelefone() != null) builder.putMetadata("telefone", req.getTelefone());
        if (req.getCpf() != null) builder.putMetadata("cpf", req.getCpf());
        builder.putMetadata("pedidos", req.getPedidos().toString());

        SessionCreateParams params = builder.build();
        Session session = Session.create(params);

        Map<String, Object> response = new HashMap<>();
        response.put("url", session.getUrl());
        return response;
    }
}