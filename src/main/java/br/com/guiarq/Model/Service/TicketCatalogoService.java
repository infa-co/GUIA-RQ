package br.com.guiarq.Model.Service;

import br.com.guiarq.Model.Entities.TicketCatalogo;
import br.com.guiarq.Model.Repository.TicketCatalogoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TicketCatalogoService {

    private final TicketCatalogoRepository repo;

    public TicketCatalogoService(TicketCatalogoRepository repo) {
        this.repo = repo;
    }

    public List<TicketCatalogo> listarTodos() {
        return repo.findAll();
    }

    public TicketCatalogo buscarPorId(Long id) {
        return repo.findById(id).orElse(null);
    }
}
