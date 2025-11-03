package br.com.guiarq.Model.Dao;

import connection.ConnectionFactory;
import br.com.guiarq.Model.Entities.Parceiro;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ParceiroDAO {
    public void inserir(Parceiro parceiro){
        String sql = "INSERT INTO parceiro (nome_fantasia, cnpj, endereco, descricao, telefone) VALUES (?, ?, ?, ?, ?)";

        try(Connection conn = ConnectionFactory.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, parceiro.getNomeFantasia());
            stmt.setString(2, parceiro.getCnpj());
            stmt.setString(3, parceiro.getEndereco());
            stmt.setString(4, parceiro.getDescricao());
            stmt.setString(5, parceiro.getTelefone());
            stmt.executeUpdate();

            System.out.println("Parceiro cadastrado com sucesso!");
        } catch (SQLException e) {
            System.out.println("Erro ao cadastrar parceiro: " + e.getMessage());
        }
    }
    public List<Parceiro> listarTodos() {
        List<Parceiro> parceiros = new ArrayList<>();
        String sql = "SELECT * FROM parceiro";

        try (Connection conn = ConnectionFactory.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Parceiro p = new Parceiro();
                p.setNomeFantasia(rs.getString("nome_fantasia"));
                p.setCnpj(rs.getString("cnpj"));
                p.setEndereco(rs.getString("endereco"));
                p.setDescricao(rs.getString("descricao"));
                p.setTelefone(rs.getString("telefone"));
                parceiros.add(p);
            }
        } catch (SQLException e) {
            System.err.println("Erro ao listar parceiros: " + e.getMessage());
        }
        return parceiros;
    }
}
