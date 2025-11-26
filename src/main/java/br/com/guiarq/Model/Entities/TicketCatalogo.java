package br.com.guiarq.Model.Entities;

import jakarta.persistence.*;

@Entity
@Table(name = "tickets_catalogo")
public class TicketCatalogo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;            // ex: Mirante Boa Vista
    private String descricao;       // texto explicativo
    private String tipo;            // ex: passeio, mirante, atividade
    private Double preco;           // preço base
    private Long parceiroId;        // qual estabelecimento é o responsável
    private String imagem;          // opcional, URL da foto

    public TicketCatalogo() {}

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public Double getPreco() {
        return preco;
    }

    public void setPreco(Double preco) {
        this.preco = preco;
    }

    public Long getParceiroId() {
        return parceiroId;
    }

    public void setParceiroId(Long parceiroId) {
        this.parceiroId = parceiroId;
    }

    public String getImagem() {
        return imagem;
    }

    public void setImagem(String imagem) {
        this.imagem = imagem;
    }
}
