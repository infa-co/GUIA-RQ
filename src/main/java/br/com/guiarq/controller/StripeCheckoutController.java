    package br.com.guiarq.controller;

    import br.com.guiarq.DTO.CheckoutRequest;
    import br.com.guiarq.Model.Entities.Ticket;
    import br.com.guiarq.Model.Entities.TicketCatalogo;
    import br.com.guiarq.Model.Repository.TicketRepository;
    import br.com.guiarq.Model.Repository.TicketCatalogoRepository;
    import br.com.guiarq.Model.Service.TicketCatalogoService;
    import br.com.guiarq.Model.Service.TicketService;
    import com.fasterxml.jackson.databind.ObjectMapper;
    import com.stripe.Stripe;
    import com.stripe.model.checkout.Session;
    import com.stripe.param.checkout.SessionCreateParams;
    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.beans.factory.annotation.Value;
    import org.springframework.cglib.core.Local;
    import org.springframework.web.bind.annotation.*;

    import java.time.LocalDateTime;
    import java.util.*;

    @RestController
    @RequestMapping("/api/stripe")
    @CrossOrigin(origins = "*")
    public class StripeCheckoutController {
        @Autowired
        private TicketCatalogoService ticketCatalogoService;

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
            if(req.getCpf() != null){
                Ticket ultimoTicket = ticketRepository.findTop1ByCpfClienteOrderByDataCompraDesc(req.getCpf());
                if(ultimoTicket != null){
                    LocalDateTime ultimaCompra = ultimoTicket.getDataCompra();
                    LocalDateTime agoraCompra = LocalDateTime.now();
                    if(ultimaCompra.plusMonths(6).isAfter(agoraCompra)){
                        throw new IllegalStateException("Você só pode realizar uma nova compra 6 meses após a última.");
                    }
                }
            }

            Long quantidade = req.getQuantidade() != null ? req.getQuantidade() : 1L;

            SessionCreateParams.Builder builder = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl("https://guiaranchoqueimado.com.br/pages/sucesso.html")
                    .setCancelUrl("https://guiaranchoqueimado.com.br/pages/cancelado.html");
            Map<String, Integer> pedidos = req.getPedidos();
            pedidos.forEach((id, qtdTicket) -> {

                // Supondo que você tenha um método para buscar o produto pelo ID
                TicketCatalogo ticketCatalogo = ticketCatalogoService.buscarPorId(Long.valueOf(id));

                Long amountInCents = Math.round(ticketCatalogo.getPreco() * 100);

                builder.addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setQuantity(qtdTicket.longValue())
                                .setPriceData(
                                        SessionCreateParams.LineItem.PriceData.builder()
                                                .setCurrency("brl")
                                                .setUnitAmount(amountInCents)
                                                .setProductData(
                                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                .setName(ticketCatalogo.getNome())
                                                                .build()
                                                )
                                                .build()
                                )
                                .build()
                );
            });


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
            ObjectMapper mapper = new ObjectMapper();
            if (req.getPedidos() != null) {
                builder.putMetadata(
                        "pedidos",
                        mapper.writeValueAsString(req.getPedidos())
                );
            }

            System.out.println(req.getPedidos().toString() + " FLAG");

            SessionCreateParams params = builder.build();
            Session session = Session.create(params);

            Map<String, Object> response = new HashMap<>();
            response.put("url", session.getUrl());
            return response;

        }
    }