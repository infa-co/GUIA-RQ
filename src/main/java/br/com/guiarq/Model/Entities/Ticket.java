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

    public Ticket() {}

    public Long getId() { return id; }

    public UUID getIdPublico() { return idPublico; }
    public void setIdPublico(UUID idPublico) { this.idPublico = idPublico; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public BigDecimal getPrecoOriginal() { return precoOriginal; }
    public void setPrecoOriginal(BigDecimal precoOriginal) { this.precoOriginal = precoOriginal; }

    public BigDecimal getPrecoPromocional() { return precoPromocional; }
    public void setPrecoPromocional(BigDecimal precoPromocional) { this.precoPromocional = precoPromocional; }

    public Long getParceiroId() { return parceiroId; }
    public void setParceiroId(Long parceiroId) { this.parceiroId = parceiroId; }

    public LocalDateTime getDataCompra() { return dataCompra; }
    public void setDataCompra(LocalDateTime dataCompra) { this.dataCompra = dataCompra; }

    public String getEmailCliente() { return emailCliente; }
    public void setEmailCliente(String emailCliente) { this.emailCliente = emailCliente; }

    public String getExperiencia() { return experiencia; }
    public void setExperiencia(String experiencia) { this.experiencia = experiencia; }

    public String getNomeCliente() { return nomeCliente; }
    public void setNomeCliente(String nomeCliente) { this.nomeCliente = nomeCliente; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Double getValorPago() { return valorPago; }
    public void setValorPago(Double valorPago) { this.valorPago = valorPago; }

    public UUID getQrToken() { return qrToken; }
    public void setQrToken(UUID qrToken) { this.qrToken = qrToken; }

    public boolean isUsado() { return usado; }
    public void setUsado(boolean usado) { this.usado = usado; }

    public LocalDateTime getUsadoEm() { return usadoEm; }
    public void setUsadoEm(LocalDateTime usadoEm) { this.usadoEm = usadoEm; }

    public UUID getCompraId() { return compraId; }
    public void setCompraId(UUID compraId) { this.compraId = compraId; }

    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }
}
