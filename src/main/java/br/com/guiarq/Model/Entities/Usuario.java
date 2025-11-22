package br.com.guiarq.Model.Entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;
    private String email;
    private String senha;

    // Novos campos para verificação
    private Boolean verificado = false;
    private String tokenVerificacao;
    private LocalDateTime expiracaoToken;

    public Usuario() {}

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }

    public Boolean getVerificado() { return verificado; }
    public void setVerificado(Boolean verificado) { this.verificado = verificado; }

    public String getTokenVerificacao() { return tokenVerificacao; }
    public void setTokenVerificacao(String tokenVerificacao) { this.tokenVerificacao = tokenVerificacao; }

    public LocalDateTime getExpiracaoToken() { return expiracaoToken; }
    public void setExpiracaoToken(LocalDateTime expiracaoToken) { this.expiracaoToken = expiracaoToken; }
}
