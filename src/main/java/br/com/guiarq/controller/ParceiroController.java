package br.com.guiarq.controller;

import br.com.guiarq.Model.Entities.Parceiro;
import br.com.guiarq.Model.Repository.ParceiroRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/parceiros")
public class ParceiroController {

    @Autowired
    private ParceiroRepository parceiroRepository;

    @GetMapping("/listar")
    public List<Parceiro> listar() {
        return parceiroRepository.findAll();
    }

    @PostMapping("/criar")
    public Parceiro criar(@RequestBody Parceiro parceiro) {
        return parceiroRepository.save(parceiro);
    }
}
