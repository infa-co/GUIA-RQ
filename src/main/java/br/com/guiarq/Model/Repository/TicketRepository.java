package br.com.guiarq.Model.Repository;

import br.com.guiarq.Model.Entities.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    Optional<Ticket> findByQrToken(UUID qrToken);
}
