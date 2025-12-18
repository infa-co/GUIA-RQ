package br.com.guiarq.Model.Repository;

import br.com.guiarq.Model.Entities.Ticket;
import br.com.guiarq.Model.Entities.TicketCatalogo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TicketCatalogoRepository extends JpaRepository<TicketCatalogo, Long> {

    List<TicketCatalogo> findByIdIn(List<Long> ticketCatalogoId);

    @Override
    TicketCatalogo getReferenceById(Long ticketId);
}