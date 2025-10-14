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

    public long getId() {
        return id;
    }
    public String getNomeFantasia() {
        return nomeFantasia;
    }
    public void setNomeFantasia(String nomeFantasia) {
        this.nomeFantasia = nomeFantasia;
    }
    public String getCnpj() {
        return cnpj;
    }
    public void setCnpj(String cnpj) {
        this.cnpj = cnpj;
    }
    public String getEndereco() {
        return endereco;
    }
    public void setEndereco(String endereco) {
        this.endereco = endereco;
    }
    public String getDescricao() {
        return descricao;
    }
    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }
    public String getTelefone() {
        return telefone;
    }
    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }
    public List<Ticket> getTicketsOferecidos() {
        for ( Ticket ticket : ticketsOferecidos) {
            return ticketsOferecidos;
        }
        return null;
    }
    public void adicionarTicket(Ticket ticket) {
        this.ticketsOferecidos.add(ticket);
    }   
}
