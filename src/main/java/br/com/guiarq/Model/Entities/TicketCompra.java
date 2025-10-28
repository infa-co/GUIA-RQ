package src.main.java.br.com.guiarq.Model.Entities;

import java.sql.Date;

public class TicketCompra {
    private long id;
    private Usuario usuario;
    private Ticket ticket;
    private Date dataCompra;
    private String codigoValidacao;
    private boolean usado;

    public long getId() {
        return id;
    }
    public Usuario getUsuario() {
        return usuario;
    }
    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }
    public Ticket getTicket() {
        return ticket;
    }
    public void setTicket(Ticket ticket) {
        this.ticket = ticket;
    }
    public Date getDataCompra() {
        return dataCompra;
    }
    public void setDataCompra(Date dataCompra) {
        this.dataCompra = dataCompra;
    }
    public String getCodigoValidacao() {
        return codigoValidacao;
    }
    public boolean isUsado() {
        return usado;
    }
    public void marcarComoUsado() {
        this.usado = true;
    }
}
