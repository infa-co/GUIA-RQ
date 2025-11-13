package br.com.guiarq.Model.Entities;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tickets")
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_publico")
    private UUID idPublico;

    private String nome;
    private String tipo;
    private String descricao;

    @Column(name = "preco_original")
    private BigDecimal precoOriginal;

    @Column(name = "preco_promocional")
    private BigDecimal precoPromocional;

    @Column(name = "parceiro_id")
    private Long parceiroId;

    @Column(name = "data_compra")
    private LocalDateTime dataCompra;

    @Column(name = "email_cliente")
    private String emailCliente;

    private String experiencia;

    @Column(name = "nome_cliente")
    private String nomeCliente;

    private String status;

    @Column(name = "valor_pago")
    private Double valorPago;

    @Column(name = "qr_token")
    private UUID qrToken;

    private boolean usado;

    @Column(name = "usado_em")
    private LocalDateTime usadoEm;

    @Column(name = "compra_id")
    private UUID compraId;

    @Column(name = "criado_em")
    private LocalDateTime criadoEm;

    public void setNome(String titulo) {
        if(this.nome != null) {
            this.nome = titulo;
        }else{
            System.out.println("Falha ao alterar o nome");
        }
    }
    public void setDescricao(String descricao) {
        if (this.descricao != null) {
            this.descricao = descricao;
        }
    }
    public void setPrecoOrginal(double preco) {
        if(preco > 0) {
            this.precoOriginal = BigDecimal.valueOf(preco);
        }
    }
    public String getNome() {
        return this.nome;
    }
    public String getDescricao() {
        return this.descricao;
    }
    public double getPrecoOrginal() {
        return this.precoOriginal.doubleValue();
    }
    public Long getId() {
        return this.id;
    }
}
