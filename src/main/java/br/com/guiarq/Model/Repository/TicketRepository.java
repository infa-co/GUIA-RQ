    package br.com.guiarq.Model.Repository;

    import br.com.guiarq.Model.Entities.Ticket;
    import org.springframework.data.jpa.repository.JpaRepository;
    import org.springframework.stereotype.Repository;

    import java.util.Optional;
    import java.util.UUID;

    @Repository
    public interface TicketRepository extends JpaRepository<Ticket, Long> {

        Optional<Ticket> findByIdPublico(UUID idPublico);

        Optional<Ticket> findByQrToken(String qrToken);

        boolean existsByStripeSessionId(String stripeSessionId);

        Ticket findTop1ByCpfClienteOrderByDataCompraDesc(String cpfCliente);

    }

