package br.com.guiarq.Controller;

import br.com.guiarq.Model.Dao.TicketDAO;
import br.com.guiarq.Model.Entities.Ticket;
import java.util.List;
import java.util.UUID;

public class TicketController {
    private final TicketDAO ticketDAO = new TicketDAO();

    public void criarTicket(String titulo, String descricao, double preco) {
        Ticket ticket = new Ticket();
        ticket.setNome(titulo);
        ticket.setDescricao(descricao);
        ticket.setPrecoOrginal(preco);
        ticketDAO.inserir(ticket);
    }

    public List<Ticket> listarTickets() {
        return ticketDAO.listarTodos();
    }
    /**
     * Registra uma compra gerando código único de validação.
     */
    public void registrarCompra(int usuarioId, int ticketId) {
        String codigoValidacao = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        ticketDAO.registrarCompra(usuarioId, ticketId, codigoValidacao);
    }

    /**
     * Registra a transação Stripe (recebida da API).
     */
    public void registrarTransacaoStripe(String stripeId, double amount, String currency,
                                         String status, String metadata, int ticketCompraId) {
        ticketDAO.registrarTransacaoStripe(stripeId, amount, currency, status, metadata, ticketCompraId);
    }
}
