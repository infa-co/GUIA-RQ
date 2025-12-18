package br.com.guiarq.DTO;

import org.json.JSONObject;

import java.util.List;
import java.util.Map;

public class CheckoutRequest {

    private Double amount;
    private String description;
    private Long ticketId;
    private Integer quantidade;

    private String email;
    private String nome;
    private String telefone;
    private String cpf;
    private Map<String, Integer> pedidos;

    private Boolean pacote;      // indica se é compra de pacote

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getTicketId() {
        return ticketId;
    }

    public void setTicketId(Long ticketId) {
        this.ticketId = ticketId;
    }

    public Integer getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(Integer quantidade) {
        this.quantidade = quantidade;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public Boolean getPacote() {
        return pacote;
    }

    public void setPacote(Boolean pacote) {
        this.pacote = pacote;
    }

    public Map<String, Integer> getPedidos() {
        return pedidos;
    }
    public void setPedidos(Map<String, Integer> pedidos) {
        this.pedidos = pedidos;
    }
}
