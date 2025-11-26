package br.com.guiarq.Model.Repository;

import br.com.guiarq.Model.Entities.TicketCatalogo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TicketCatalogoRepository extends JpaRepository<TicketCatalogo, Long> {

}
