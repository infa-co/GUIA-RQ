package br.com.guiarq.Model.DAO;

import br.com.guiarq.Config.ConnectionFactory;
import br.com.guiarq.Model.Entities.Ticket;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TicketDAO {

    // ============================
    // BUSCAR POR ID
    // ============================
    public Ticket buscarPorId(Long id) {
        String sql = "SELECT * FROM tickets_compra WHERE id = ?";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();

            if (!rs.next()) return null;
            return mapearTicket(rs);

        } catch (Exception e) {
            throw new RuntimeException("Erro ao buscar ticket por ID: " + e.getMessage());
        }
    }

    // ============================
    // BUSCAR POR QR TOKEN
    // ============================
    public Ticket buscarPorQrToken(String qrToken) {
        String sql = "SELECT * FROM tickets_compra WHERE qr_token = ?";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, qrToken);
            ResultSet rs = ps.executeQuery();

            if (!rs.next()) return null;
            return mapearTicket(rs);

        } catch (Exception e) {
            throw new RuntimeException("Erro ao buscar ticket por QR Token: " + e.getMessage());
        }
    }

    // ============================
    // MARCAR COMO USADO
    // ============================
    public boolean marcarComoUsado(String qrToken) {
        String sql = """
            UPDATE tickets_compra
            SET usado = true, usado_em = NOW()
            WHERE qr_token = ? AND usado = false
        """;

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, qrToken);
            int linhas = ps.executeUpdate();
            return linhas > 0;

        } catch (Exception e) {
            throw new RuntimeException("Erro ao marcar ticket como usado: " + e.getMessage());
        }
    }

    // ============================
    // INSERIR NOVA COMPRA
    // ============================
    public void registrarCompra(Ticket ticket) {
        String sql = """
            INSERT INTO tickets_compra (
                id_publico, nome, tipo, descricao, preco_original, preco_promocional,
                parceiro_id, data_compra, email_cliente, experiencia, nome_cliente,
                telefone_cliente, cpf_cliente, status, valor_pago, qr_token,
                usado, usado_em, compra_id, criado_em
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setObject(1, ticket.getIdPublico());
            ps.setString(2, ticket.getNome());
            ps.setString(3, ticket.getTipo());
            ps.setString(4, ticket.getDescricao());
            ps.setBigDecimal(5, ticket.getPrecoOriginal());
            ps.setBigDecimal(6, ticket.getPrecoPromocional());
            ps.setObject(7, ticket.getParceiroId());
            ps.setTimestamp(8, ticket.getDataCompra() != null ? Timestamp.valueOf(ticket.getDataCompra()) : null);
            ps.setString(9, ticket.getEmailCliente());
            ps.setString(10, ticket.getExperiencia());
            ps.setString(11, ticket.getNomeCliente());
            ps.setString(12, ticket.getTelefoneCliente());
            ps.setString(13, ticket.getCpfCliente());
            ps.setString(14, ticket.getStatus());
            ps.setObject(15, ticket.getValorPago());
            ps.setString(16, ticket.getQrToken());
            ps.setBoolean(17, ticket.isUsado());
            ps.setTimestamp(18, ticket.getUsadoEm() != null ? Timestamp.valueOf(ticket.getUsadoEm()) : null);
            ps.setObject(19, ticket.getCompraId());
            ps.setTimestamp(20, ticket.getCriadoEm() != null ? Timestamp.valueOf(ticket.getCriadoEm()) : Timestamp.valueOf(java.time.LocalDateTime.now()));

            ps.executeUpdate();

        } catch (Exception e) {
            throw new RuntimeException("Erro ao registrar compra: " + e.getMessage());
        }
    }

    // ============================
    // LISTAR TODOS OS TICKETS
    // ============================
    public List<Ticket> listarTodos() {
        String sql = "SELECT * FROM tickets_compra ORDER BY id DESC";
        List<Ticket> lista = new ArrayList<>();

        try (Connection conn = ConnectionFactory.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                lista.add(mapearTicket(rs));
            }

        } catch (Exception e) {
            throw new RuntimeException("Erro ao listar tickets: " + e.getMessage());
        }

        return lista;
    }

    // ============================
    // MÉTODO INTERNO DE MAPEAMENTO
    // ============================
    private Ticket mapearTicket(ResultSet rs) throws Exception {
        Ticket t = new Ticket();

        t.setIdPublico((UUID) rs.getObject("id_publico"));
        t.setNome(rs.getString("nome"));
        t.setTipo(rs.getString("tipo"));
        t.setDescricao(rs.getString("descricao"));
        t.setPrecoOriginal(rs.getBigDecimal("preco_original"));
        t.setPrecoPromocional(rs.getBigDecimal("preco_promocional"));
        t.setParceiroId(rs.getLong("parceiro_id"));

        Timestamp dataCompraTS = rs.getTimestamp("data_compra");
        if (dataCompraTS != null) t.setDataCompra(dataCompraTS.toLocalDateTime());

        t.setEmailCliente(rs.getString("email_cliente"));
        t.setExperiencia(rs.getString("experiencia"));
        t.setNomeCliente(rs.getString("nome_cliente"));
        t.setTelefoneCliente(rs.getString("telefone_cliente"));
        t.setCpfCliente(rs.getString("cpf_cliente"));
        t.setStatus(rs.getString("status"));
        t.setValorPago(rs.getDouble("valor_pago"));
        t.setQrToken(rs.getString("qr_token"));
        t.setUsado(rs.getBoolean("usado"));

        Timestamp usadoEmTS = rs.getTimestamp("usado_em");
        if (usadoEmTS != null) t.setUsadoEm(usadoEmTS.toLocalDateTime());

        t.setCompraId((UUID) rs.getObject("compra_id"));

        Timestamp criadoEmTS = rs.getTimestamp("criado_em");
        if (criadoEmTS != null) t.setCriadoEm(criadoEmTS.toLocalDateTime());

        return t;
    }
}
