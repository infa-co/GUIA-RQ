package br.com.guiarq.Model.Entities;

public class Ticket {
    private long id;    
    private String nome;
    private TipoTicket tipo;
    private String descricao;
    private double precoOrginal;
    private double precoPromocional;
    private Parceiro parceiro;

    public enum TipoTicket {
        GASTRONOMIA,
        AVENTURA,
        SERRA_EXPERIENCE,
        AVULSO
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
    public TipoTicket getTipo() {
        return tipo;
    }
    public void setTipo(TipoTicket tipo) {
        this.tipo = tipo;
    }
    public String getDescricao() {
        return descricao;
    }
    public void setDescricao(String descricao) {
        if (!descricao.isEmpty() && descricao != null) {
            this.descricao = descricao;
        }
    }
    public double getPrecoOrginal() {
        return precoOrginal;
    }
    public void setPrecoOrginal(double precoOrginal) {
        if(precoOrginal > 0){
            this.precoOrginal = precoOrginal;
        }
    }
    public double getPrecoPromocional() {
        return precoPromocional;
    }
    public void setPrecoPromocional(double precoPromocional) {
        if(precoPromocional > 0){
            this.precoPromocional = precoPromocional;
        }
    }
    public Parceiro getParceiro() {
        return parceiro;
    }
    public void setParceiro(Parceiro parceiro) {
        this.parceiro = parceiro;
    }
    public double calcularDesconto() {
        if (precoOrginal <= 0) {
            throw new IllegalArgumentException("Preço original deve ser maior que zero.");
        }
        return ((precoOrginal - precoPromocional) / precoOrginal) * 100;
    }
}
