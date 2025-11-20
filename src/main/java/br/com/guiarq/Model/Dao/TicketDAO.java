package br.com.guiarq.Model.Dao;

import java.sql.*;
import connection.ConnectionFactory;

public class TicketDAO {

    private Connection getConnection() throws SQLException {
        return ConnectionFactory.getConnection();
    }

    public Long registrarCompraStripe(Long ticketId, String email, String nome, String paymentId) {

        String sql = """
            INSERT INTO tickets_compra (ticket_id, email_cliente, nome_cliente, stripe_payment_id, usado)
            VALUES (?, ?, ?, ?, false)
            RETURNING id;
        """;

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, ticketId);
            stmt.setString(2, email);
            stmt.setString(3, nome);
            stmt.setString(4, paymentId);

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getLong("id");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public void salvarQrCode(Long compraId, String qrToken, byte[] qrImage) {

        String sql = """
            UPDATE tickets_compra 
            SET qr_token = ?, qr_image = ?
            WHERE id = ?;
        """;

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, qrToken);
            stmt.setBytes(2, qrImage);
            stmt.setLong(3, compraId);

            stmt.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
