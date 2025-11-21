package br.com.guiarq.controller;

import br.com.guiarq.Model.Entities.Usuario;
import br.com.guiarq.Model.Service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/registrar")
    public Map<String, Object> registrar(@RequestBody Usuario usuario) {

        Usuario novo = authService.registrarUsuario(usuario);
        Map<String, Object> response = new HashMap<>();
        response.put("id", novo.getId());
        response.put("nome", novo.getNome());
        response.put("email", novo.getEmail());

        return response;
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String senha = body.get("senha");

        Usuario user = authService.login(email, senha);

        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getId());
        response.put("nome", user.getNome());
        response.put("email", user.getEmail());

        return response;
    }
}
