package br.com.guiarq.Controller;

import br.com.guiarq.Model.Entities.Usuario;
import br.com.guiarq.Model.Repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @GetMapping("/listar")
    public List<Usuario> listar() {
        return usuarioRepository.findAll();
    }

    @PostMapping("/criar")
    public Usuario criar(@RequestBody Usuario usuario) {
        return usuarioRepository.save(usuario);
    }
}
