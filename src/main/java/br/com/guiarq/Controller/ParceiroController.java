package br.com.guiarq.Controller;

import br.com.guiarq.Model.Dao.ParceiroDAO;
import br.com.guiarq.Model.Entities.Parceiro;
import java.util.List;

public class ParceiroController {
    private final ParceiroDAO parceiroDAO = new ParceiroDAO();

    public void cadastrarParceiro(String nomeFantasia, String cnpj, String endereco, String descricao, String telefone) {
        Parceiro parceiro = new Parceiro();
        parceiro.setNomeFantasia(nomeFantasia);
        parceiro.setCnpj(cnpj);
        parceiro.setEndereco(endereco);
        parceiro.setDescricao(descricao);
        parceiro.setTelefone(telefone);
        parceiroDAO.inserir(parceiro);
    }

    public List<Parceiro> listarParceiros() {
        return parceiroDAO.listarTodos();
    }
}
