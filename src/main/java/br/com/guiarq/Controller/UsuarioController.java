package br.com.guiarq.Controller;


import br.com.guiarq.Model.Dao.UsuarioDAO;
import br.com.guiarq.Model.Entities.Usuario;

import java.sql.SQLException;
import java.util.List;

public class UsuarioController {
    private final UsuarioDAO usuarioDAO = new UsuarioDAO();

    public void criarUsuario(String nome, String email, String senha) throws SQLException {
        Usuario usuario = new Usuario();
        usuario.setNome(nome);
        usuario.setEmail(email);
        usuario.setSenha(senha);
        usuarioDAO.inserir(usuario);
    }

    public List<Usuario> listarUsuarios() {
        return usuarioDAO.listarTodos();
    }
}
