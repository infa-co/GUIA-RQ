package br.com.guiarq.Model.Repository;

import br.com.guiarq.Model.Entities.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    List<Ticket> findByEmailCliente(String emailCliente);

    Optional<Ticket> findByIdPublico(UUID idPublico);

    Optional<Ticket> findByQrToken(UUID qrToken);
}
