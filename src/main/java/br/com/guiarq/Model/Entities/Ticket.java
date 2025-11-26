package br.com.guiarq.Model.Entities;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tickets_compra")
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_publico")
    private UUID idPublico;

    // Referência ao ticket original do catálogo
    @Column(name = "ticket_catalogo_id")
    private Long ticketCatalogoId;

    // Dados do ticket comprado
    private String nome;          // nome do ticket (ex: “Mirante Boa Vista”)
    private String descricao;
    private String tipo;

    @Column(name = "data_compra")
    private LocalDateTime dataCompra;

    @Column(name = "email_cliente")
    private String emailCliente;

    @Column(name = "nome_cliente")
    private String nomeCliente;

    @Column(name = "telefone_cliente")
    private String telefoneCliente;

    @Column(name = "cpf_cliente")
    private String cpfCliente;

    private String status;

    @Column(name = "valor_pago")
    private Double valorPago;

    @Column(name = "qr_token")
    private String qrToken;

    private boolean usado;

    @Column(name = "usado_em")
    private LocalDateTime usadoEm;

    @Column(name = "compra_id")
    private UUID compraId;

    @Column(name = "criado_em")
    private LocalDateTime criadoEm;

    @Column(name = "stripe_session_id", unique = true)
    private String stripeSessionId;

    public Ticket() {}

    public Long getId() {
        return id;
    }

    public UUID getIdPublico() {
        return idPublico;
    }

    public void setIdPublico(UUID idPublico) {
        this.idPublico = idPublico;
    }

    public Long getTicketCatalogoId() {
        return ticketCatalogoId;
    }

    public void setTicketCatalogoId(Long ticketCatalogoId) {
        this.ticketCatalogoId = ticketCatalogoId;
    }

    public String getStripeSessionId() {
        return stripeSessionId;
    }

    public void setStripeSessionId(String stripeSessionId) {
        this.stripeSessionId = stripeSessionId;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public LocalDateTime getDataCompra() {
        return dataCompra;
    }

    public void setDataCompra(LocalDateTime dataCompra) {
        this.dataCompra = dataCompra;
    }

    public String getEmailCliente() {
        return emailCliente;
    }

    public void setEmailCliente(String emailCliente) {
        this.emailCliente = emailCliente;
    }

    public String getNomeCliente() {
        return nomeCliente;
    }

    public void setNomeCliente(String nomeCliente) {
        this.nomeCliente = nomeCliente;
    }

    public String getTelefoneCliente() {
        return telefoneCliente;
    }

    public void setTelefoneCliente(String telefoneCliente) {
        this.telefoneCliente = telefoneCliente;
    }

    public String getCpfCliente() {
        return cpfCliente;
    }

    public void setCpfCliente(String cpfCliente) {
        this.cpfCliente = cpfCliente;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Double getValorPago() {
        return valorPago;
    }

    public void setValorPago(Double valorPago) {
        this.valorPago = valorPago;
    }

    public String getQrToken() {
        return qrToken;
    }

    public void setQrToken(String qrToken) {
        this.qrToken = qrToken;
    }

    public boolean isUsado() {
        return usado;
    }

    public void setUsado(boolean usado) {
        this.usado = usado;
    }

    public LocalDateTime getUsadoEm() {
        return usadoEm;
    }

    public void setUsadoEm(LocalDateTime usadoEm) {
        this.usadoEm = usadoEm;
    }

    public UUID getCompraId() {
        return compraId;
    }

    public void setCompraId(UUID compraId) {
        this.compraId = compraId;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(LocalDateTime criadoEm) {
        this.criadoEm = criadoEm;
    }
}
