package br.com.guiarq.Model.Entities;

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
        if (!nomeFantasia.isEmpty() && nomeFantasia != null) {
            this.nomeFantasia = nomeFantasia;
        }
    }
    public String getCnpj() {
        return cnpj;
    }
    public void setCnpj(String cnpj) {
        if(cnpj.length() == 14){
            this.cnpj = cnpj;
        } else {
            throw new IllegalArgumentException("CNPJ deve ter 14 caracteres.");
        }
    }
    public String getEndereco() {
        return endereco;
    }
    public void setEndereco(String endereco) {
        if (!endereco.isEmpty() && endereco != null) {
            this.endereco = endereco; 
        }else {
            throw new IllegalArgumentException("Endereço não pode ser vazio.");
        }
    }
    public String getDescricao() {
        return descricao;
    }
    public void setDescricao(String descricao) {
        if (!descricao.isEmpty() && descricao != null) {
            this.descricao = descricao;
        } else {
            throw new IllegalArgumentException("Descrição não pode ser vazia.");
        }
    }
    public String getTelefone() {
        return telefone;
    }
    public void setTelefone(String telefone) {
        if (telefone.length() >= 10 && telefone.length() <= 11) {
            this.telefone = telefone;
        } else {
            throw new IllegalArgumentException("Telefone deve ter entre 10 e 11 caracteres.");
        }
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
