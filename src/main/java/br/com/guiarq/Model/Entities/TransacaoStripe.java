package src.main.java.br.com.guiarq.Model.Entities;

import java.math.BigDecimal;
import java.security.Timestamp;

public class TransacaoStripe {

    private long id;
    private String stripeTransactionId;
    private BigDecimal amount;
    private String currency;
    private String status;
    private Timestamp criadoEm;
    private Timestamp atualizadoEm;
    private String metadata;
    private Long ticketCompraId; // Assuming this is a foreign key reference to TicketCompra

    public long getId() {
        return id;
    }   
    public String getStripeTransactionId() {
        return stripeTransactionId;
    }
    public void setStripeTransactionId(String stripeTransactionId) {
        this.stripeTransactionId = stripeTransactionId;
    }
    public BigDecimal getAmount() {
        return amount;
    }
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
    public String getCurrency() {
        return currency;
    }
    public void setCurrency(String currency) {
        this.currency = currency;
    }
    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }
    public Timestamp getCriadoEm() {
        return criadoEm;
    }
    public void setCriadoEm(Timestamp criadoEm) {
        this.criadoEm = criadoEm;
    }
    public Timestamp getAtualizadoEm() {
        return atualizadoEm;
    }
    public void setAtualizadoEm(Timestamp atualizadoEm) {
        this.atualizadoEm = atualizadoEm;
    }
    public String getMetadata() {
        return metadata;
    }
    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }
    public Long getTicketCompraId() {
        return ticketCompraId;
    }
    public void setTicketCompraId(Long ticketCompraId) {
        this.ticketCompraId = ticketCompraId;
    }
    public boolean isSuccessful() {
        return "succeeded".equalsIgnoreCase(this.status);
    }
}
