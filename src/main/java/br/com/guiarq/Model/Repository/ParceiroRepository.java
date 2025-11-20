package br.com.guiarq.Model.Repository;

import br.com.guiarq.Model.Entities.Parceiro;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParceiroRepository extends JpaRepository<Parceiro, Long> {}
