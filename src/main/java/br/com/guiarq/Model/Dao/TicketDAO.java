package br.com.guiarq.Model.Dao;

import connection.ConnectionFactory;
import br.com.guiarq.Model.Entities.Ticket;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TicketDAO {
    public void inserir(Ticket ticket) {
        String sql = "INSERT INTO ticket (titulo, descricao, preco, parceiro_id) VALUES (?, ?, ?, ?)";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, ticket.getNome());
            stmt.setString(2, ticket.getDescricao());
            stmt.setDouble(3, ticket.getPrecoOriginal());
            stmt.setLong(4, ticket.getId());
            stmt.executeUpdate();

            System.out.println("Ticket cadastrado com sucesso!");
        } catch (SQLException e) {
            System.err.println("Erro ao cadastrar ticket: " + e.getMessage());
        }
    }

    public List<Ticket> listarTodos() {
        List<Ticket> tickets = new ArrayList<>();
        String sql = "SELECT * FROM ticket";

        try (Connection conn = ConnectionFactory.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Ticket t = new Ticket();
                t.setNome(rs.getString("titulo"));
                t.setDescricao(rs.getString("descricao"));
                t.setPrecoOrginal(rs.getDouble("preco"));
                tickets.add(t);
            }
        } catch (SQLException e) {
            System.err.println("Erro ao listar tickets: " + e.getMessage());
        }
        return tickets;
    }
    /**
     * Registra a compra do ticket, gerando um código de validação único.
     */
    public void registrarCompra(int usuarioId, int ticketId, String codigoValidacao) {
        String sql = """
            INSERT INTO tickets_compra (usuario_id, ticket_id, data_compra, codigo_validacao, usado)
            VALUES (?, ?, NOW(), ?, false)
        """;

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, usuarioId);
            stmt.setInt(2, ticketId);
            stmt.setString(3, codigoValidacao);
            stmt.executeUpdate();

            System.out.println("Compra registrada com sucesso!");
        } catch (SQLException e) {
            System.err.println("Erro ao registrar compra: " + e.getMessage());
        }
    }
    public void registrarTransacaoStripe(String stripeId, double amount, String currency,
                                         String status, String metadata, int ticketCompraId) {
        String sql = """
            INSERT INTO transacoes_stripe (stripe_transaction_id, amount, currency, status, metadata, ticket_compra_id, criado_em, atualizado_em)
            VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW())
        """;

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, stripeId);
            stmt.setDouble(2, amount);
            stmt.setString(3, currency);
            stmt.setString(4, status);
            stmt.setString(5, metadata);
            stmt.setInt(6, ticketCompraId);
            stmt.executeUpdate();

            System.out.println("Transação Stripe registrada com sucesso!");
        } catch (SQLException e) {
            System.err.println("Erro ao registrar transação Stripe: " + e.getMessage());
        }
    }

    public void registrarCompraComToken(int usuarioId, int ticketId, UUID qrToken) {

    }
    public Ticket buscarPorId(Long id) {
        String sql = "SELECT * FROM tickets WHERE id = ?";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                Ticket ticket = new Ticket();
                ticket.setId(rs.getLong("id"));
                ticket.setNome(rs.getString("nome"));
                ticket.setDescricao(rs.getString("descricao"));
                ticket.setPrecoOriginal(rs.getBigDecimal("preco_original"));
                return ticket;
            }

        } catch (Exception e) {
            System.out.println("Erro ao buscar ticket: " + e.getMessage());
        }

        return null;
    }

}
