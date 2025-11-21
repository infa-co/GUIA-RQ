package br.com.guiarq.Model.Service;

import br.com.guiarq.Model.Entities.Usuario;
import br.com.guiarq.Model.Repository.UsuarioRepository;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    public Usuario registrarUsuario(Usuario usuario) {
        if(usuarioRepository.findByEmail(usuario.getEmail())!=null){
            throw new RuntimeException("Email já cadastrado.");
        }

        String hash = BCrypt.hashpw(usuario.getSenha(), BCrypt.gensalt());
        usuario.setSenha(hash);

        return usuarioRepository.save(usuario);
    }
    public Usuario login(String email, String senha) {
        Usuario usuario = usuarioRepository.findByEmail(email);

        if (usuario == null) {
            throw new RuntimeException("Usuário não encontrado.");
        }
        if (BCrypt.checkpw(senha, usuario.getSenha())) {
            throw new RuntimeException("Senha incorreta.");
        }
        return usuario;
    }
}
