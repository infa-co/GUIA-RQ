package br.com.guiarq.controller;

import br.com.guiarq.Model.Entities.Usuario;
import br.com.guiarq.Model.Repository.UsuarioRepository;
import br.com.guiarq.Model.Service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> body) {

        String email = body.get("email");
        String senha = body.get("senha");

        Usuario usuario = usuarioRepository.findByEmail(email);

        boolean success = usuario != null && passwordEncoder.matches(senha, usuario.getSenha());

        return Map.of("success", success);
    }

    @PostMapping("/enviar-verificacao")
    public ResponseEntity<?> enviarVerificacao(@RequestParam String email) {

        Usuario usuario = usuarioRepository.findByEmail(email);

        if (usuario == null) {
            return ResponseEntity.badRequest().body("Usuário não encontrado");
        }

        String token = UUID.randomUUID().toString();

        usuario.setTokenVerificacao(token);
        usuarioRepository.save(usuario);

        // ✔ MÉTODO CORRETO
        emailService.enviarVerificacaoEmail(email, token);

        return ResponseEntity.ok("E-mail de verificação enviado");
    }

    @GetMapping("/verify")
    public ResponseEntity<Void> verifyEmail(@RequestParam String token) {

        Usuario usuario = usuarioRepository.findByTokenVerificacao(token);

        if (usuario == null) {
            return ResponseEntity.status(302)
                    .location(URI.create("https://guiaranchoqueimado.com.br/pages/erro-email-confirmado.html"))
                    .build();
        }

        usuario.setVerificado(true);
        usuario.setTokenVerificacao(null);
        usuarioRepository.save(usuario);

        return ResponseEntity.status(302)
                .location(URI.create("https://guiaranchoqueimado.com.br/pages/email-confirmado.html"))
                .build();
    }
}
