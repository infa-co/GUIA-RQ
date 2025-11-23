package br.com.guiarq.controller;

import br.com.guiarq.Model.Entities.Usuario;
import br.com.guiarq.Model.Repository.UsuarioRepository;
import br.com.guiarq.Model.Service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin("*")
public class AuthController {

    @Autowired
    private UsuarioRepository.UsuarioRepositoryInterface usuarioRepository;

    @GetMapping("/verify")
    public ResponseEntity<Void> verifyEmail(@RequestParam String token) {

        try {
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

        } catch (Exception e) {
            return ResponseEntity.status(302)
                    .location(URI.create("https://guiaranchoqueimado.com.br/pages/erro-email-confirmado.html"))
                    .build();
        }
    }
}
