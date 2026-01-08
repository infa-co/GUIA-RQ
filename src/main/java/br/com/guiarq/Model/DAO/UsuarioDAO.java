package br.com.guiarq.Model.DAO;

import br.com.guiarq.Config.ConnectionFactory;
import br.com.guiarq.Model.Entities.Usuario;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class UsuarioDAO {

    public Usuario buscarPorEmail(String email) {
        String sql = "SELECT * FROM usuarios WHERE email = ? LIMIT 1";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return mapearUsuario(rs);
            }

            return null;

        } catch (Exception e) {
            throw new RuntimeException("Erro ao buscar usuário por email: " + e.getMessage());
        }
    }

    public Usuario buscarPorId(Long id) {
        String sql = "SELECT * FROM usuarios WHERE id = ?";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return mapearUsuario(rs);
            }

            return null;

        } catch (Exception e) {
            throw new RuntimeException("Erro ao buscar usuário por ID: " + e.getMessage());
        }
    }

    public Usuario buscarPorToken(String token) {
        String sql = "SELECT * FROM usuarios WHERE token_verificacao = ? LIMIT 1";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, token);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return mapearUsuario(rs);
            }

            return null;

        } catch (Exception e) {
            throw new RuntimeException("Erro ao buscar token: " + e.getMessage());
        }
    }

    public void salvar(Usuario u) {

        if (u.getId() == null) {
            inserir(u);
        } else {
            atualizar(u);
        }
    }

    private void inserir(Usuario u) {
        String sql = """
            INSERT INTO usuarios (nome, email, senha, perfil, verificado, token_verificacao, expiracao_token)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, u.getNome());
            ps.setString(2, u.getEmail());
            ps.setString(3, u.getSenha());
            ps.setString(4, "USER"); // perfil padrão
            ps.setBoolean(5, u.getVerificado() != null ? u.getVerificado() : false);
            ps.setString(6, u.getTokenVerificacao());
            ps.setTimestamp(7, u.getExpiracaoToken() != null ? Timestamp.valueOf(u.getExpiracaoToken()) : null);

            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
                u.setId(keys.getLong(1));
            }

        } catch (Exception e) {
            throw new RuntimeException("Erro ao inserir usuário: " + e.getMessage());
        }
    }

    private void atualizar(Usuario u) {
        String sql = """
            UPDATE usuarios SET
                nome = ?, email = ?, senha = ?, perfil = ?, 
                verificado = ?, token_verificacao = ?, expiracao_token = ?
            WHERE id = ?
        """;

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, u.getNome());
            ps.setString(2, u.getEmail());
            ps.setString(3, u.getSenha());
            ps.setString(4, "USER");
            ps.setBoolean(5, u.getVerificado());
            ps.setString(6, u.getTokenVerificacao());
            ps.setTimestamp(7, u.getExpiracaoToken() != null ? Timestamp.valueOf(u.getExpiracaoToken()) : null);
            ps.setLong(8, u.getId());

            ps.executeUpdate();

        } catch (Exception e) {
            throw new RuntimeException("Erro ao atualizar usuário: " + e.getMessage());
        }
    }

    public void atualizarSenha(Long id, String novaSenha) {
        String sql = "UPDATE usuarios SET senha = ? WHERE id = ?";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, novaSenha);
            ps.setLong(2, id);
            ps.executeUpdate();

        } catch (Exception e) {
            throw new RuntimeException("Erro ao atualizar senha: " + e.getMessage());
        }
    }

    public void marcarVerificado(Long id) {
        String sql = "UPDATE usuarios SET verificado = true, token_verificacao = NULL WHERE id = ?";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            ps.executeUpdate();

        } catch (Exception e) {
            throw new RuntimeException("Erro ao verificar usuário: " + e.getMessage());
        }
    }


    public void setarTokenVerificacao(Long id, String token, LocalDateTime expiracao) {
        String sql = "UPDATE usuarios SET token_verificacao = ?, expiracao_token = ? WHERE id = ?";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, token);
            ps.setTimestamp(2, Timestamp.valueOf(expiracao));
            ps.setLong(3, id);
            ps.executeUpdate();

        } catch (Exception e) {
            throw new RuntimeException("Erro ao definir token de verificação: " + e.getMessage());
        }
    }

    private Usuario mapearUsuario(ResultSet rs) throws Exception {
        Usuario u = new Usuario();

        u.setId(rs.getLong("id"));
        u.setNome(rs.getString("nome"));
        u.setEmail(rs.getString("email"));
        u.setSenha(rs.getString("senha"));

        u.setVerificado(rs.getBoolean("verificado"));
        u.setTokenVerificacao(rs.getString("token_verificacao"));

        Timestamp exp = rs.getTimestamp("expiracao_token");
        if (exp != null) {
            u.setExpiracaoToken(exp.toLocalDateTime());
        }

        return u;
    }
}
