package br.com.guiarq.Model.Service;

import br.com.guiarq.Model.Entities.Usuario;
import br.com.guiarq.Model.Repository.UsuarioRepository;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EmailService emailService;

    // === REGISTRO DE USUÁRIO COM TOKEN DE VERIFICAÇÃO ===
    public Usuario registrarUsuario(Usuario usuario) {

        // Verifica se email já existe
        if (usuarioRepository.findByEmail(usuario.getEmail()) != null) {
            throw new RuntimeException("Email já cadastrado.");
        }

        // Criptografa a senha
        String hash = BCrypt.hashpw(usuario.getSenha(), BCrypt.gensalt());
        usuario.setSenha(hash);

        // Gera token de verificação
        String token = UUID.randomUUID().toString();
        usuario.setTokenVerificacao(token);
        usuario.setExpiracaoToken(LocalDateTime.now().plusHours(24)); // expira em 24h
        usuario.setVerificado(false);

        // Salva usuário
        Usuario salvo = usuarioRepository.save(usuario);

        // Dispara o e-mail de verificação
        emailService.enviarVerificacaoEmail(salvo.getEmail(), token);

        return salvo;
    }

    // === LOGIN COM BLOQUEIO DE USUÁRIO NÃO VERIFICADO ===
    public Usuario login(String email, String senha) {

        Usuario usuario = usuarioRepository.findByEmail(email);

        if (usuario == null) {
            throw new RuntimeException("Usuário não encontrado.");
        }

        // Bloqueia login até verificar o e-mail
        if (!usuario.getVerificado()) {
            throw new RuntimeException("Confirme seu e-mail antes de fazer login.");
        }

        // Verifica a senha
        if (!BCrypt.checkpw(senha, usuario.getSenha())) {
            throw new RuntimeException("Senha incorreta.");
        }

        return usuario;
    }
}
