package br.com.guiarq.Model.Service;

import br.com.guiarq.Model.Dao.TicketDAO;
import java.util.UUID;

public class TicketService {
    private final TicketDAO ticketDAO = new TicketDAO();

    public String gerarCompra(int usuarioId, int ticketId) {
        // Gerar código único de validação
        String codigoValidacao = UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // Registrar compra no banco
        ticketDAO.registrarCompra(usuarioId, ticketId, codigoValidacao);

        return codigoValidacao;
    }
}
