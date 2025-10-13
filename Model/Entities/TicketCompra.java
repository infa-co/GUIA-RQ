package Model.Entities;

import java.sql.Date;

public class TicketCompra {
    private long id;
    private Usuario usuario;
    private Ticket ticket;
    private Date dataCompra;
    private String codigoValidacao;
    private boolean usado;
}
