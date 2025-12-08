package br.com.guiarq.Model.Repository;

import br.com.guiarq.Model.Entities.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VoucherRepository extends JpaRepository<Ticket, Long> {

    // verifica se existe ticket ATIVO pelo CPF (não usado)
    boolean existsByCpfClienteAndUsadoFalse(String cpfCliente);
}
