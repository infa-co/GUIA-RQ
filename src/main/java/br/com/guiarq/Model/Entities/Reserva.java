package br.com.guiarq.Model.Entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tickets")
public class Reserva {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nomeCliente;

    @Column(nullable = false)
    private String emailCliente;

    @Column(nullable = false)
    private String experiencia; // nome do passeio/atração

    @Column(nullable = false)
    private Double valorPago;

    @Column(nullable = false)
    private LocalDateTime dataCompra;

    @Column(nullable = false)
    private String status; // PENDENTE, CONFIRMADO, CANCELADO

    public Reserva() {}
    public Reserva(String nomeCliente, String emailCliente, String experiencia, Double valorPago, LocalDateTime dataCompra, String status) {
        this.nomeCliente = nomeCliente;
        this.emailCliente = emailCliente;
        this.experiencia = experiencia;
        this.valorPago = valorPago;
        this.dataCompra = dataCompra;
        this.status = status;
    }
    public Long getId() {
        return id;
    }
    public String getNomeCliente() {
        return nomeCliente;
    }
    public void setNomeCliente(String nomeCliente) {
        this.nomeCliente = nomeCliente;
    }
    public String getEmailCliente() {
        return emailCliente;
    }
    public void setEmailCliente(String emailCliente) {
        if (emailCliente != null && !emailCliente.trim().isEmpty()) {
            this.emailCliente = emailCliente.trim();
        } else {
            System.out.println("Erro: O endereço de e-mail é inválido ou vazio.");
            // Opcional: lançar uma exceção ou definir um valor padrão
            // throw new IllegalArgumentException("Email não pode ser vazio.");
        }
    }
    public String getExperiencia() {
        return experiencia;
    }
    public void setExperiencia(String experiencia) {
        this.experiencia = experiencia;
    }
    public Double getValorPago() {
        return valorPago;
    }
    public void setValorPago(Double valorPago) {
        if(valorPago > 0) {
            this.valorPago = valorPago;
        }
    }
    public LocalDateTime getDataCompra() {
        return dataCompra;
    }
    public void setDataCompra(LocalDateTime dataCompra) {
        this.dataCompra = dataCompra;
    }
    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }
}
