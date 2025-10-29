package br.com.guiarq.Model.Entities;

import java.util.List;

import br.com.guiarq.Model.Entities.Ticket.TipoTicket;

public class Usuario {
    private long id;
    private String nome;
    private String email;
    private String senha;
    private PerfilUsuario perfil;
    private List<TipoTicket> tickets; 

    public enum PerfilUsuario {
        ADMIN,
        CLIENTE,
        PARCEIRO
    }
    public long getId() {
        return id;
    }  
    public String getNome() {
        return nome;
    }
    public void setNome(String nome) {
        if (!nome.isEmpty() && nome != null) {
            this.nome = nome;
        }
    }
    public String getEmail() {
        return email;
    }  
    public void setEmail(String email) {
        if (email.contains("@") && email.contains(".")) {
            this.email = email;
        } else {
            throw new IllegalArgumentException("Email inválido.");
        }
    }
    public String getSenha() {
        return senha;
    }
    public void setSenha(String senha) {
        if (senha.length() >= 6) {
            this.senha = senha;
        } else {
            throw new IllegalArgumentException("Senha deve ter pelo menos 6 caracteres.");
        }
    }
    public PerfilUsuario getPerfil() {
        return perfil;
    }
    public void setPerfil(PerfilUsuario perfil) {
        this.perfil = perfil;
    }
    public List<TipoTicket> getTickets() {
        for (TipoTicket ticket : tickets) {
            return tickets;
        }
        return null;
    }
}
