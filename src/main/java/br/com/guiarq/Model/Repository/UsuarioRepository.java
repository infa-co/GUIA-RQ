package br.com.guiarq.Model.Repository;

import br.com.guiarq.Model.Entities.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Usuario findByEmail(String email);

    Usuario findByTokenVerificacao(String token);

    @Repository
    public interface UsuarioRepositoryInterface extends JpaRepository<Usuario, Long> {
        Usuario findByTokenVerificacao(String token);
    }

}
