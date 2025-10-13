package Model.Entities;

import java.util.List;

public class Parceiro {
    private long id;
    private String nomeFantasia;
    private String cnpj;
    private String endereco;
    private String descricao;
    private String telefone;
    private List<Ticket> ticketsOferecidos;
}
