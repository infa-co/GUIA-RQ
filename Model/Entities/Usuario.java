package Model.Entities;

import java.util.List;

import Model.Entities.Ticket.TipoTicket;

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
        this.nome = nome;
    }
    public String getEmail() {
        return email;
    }  
    public void setEmail(String email) {
        this.email = email;
    }
    public String getSenha() {
        return senha;
    }
    public void setSenha(String senha) {
        this.senha = senha;
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
