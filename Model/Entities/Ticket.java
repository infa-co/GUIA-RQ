package Model.Entities;

import Model.Entities.Usuario.TipoTicket;

public class Ticket {
    private long id;    
    private String nome;
    private TipoTicket tipo;
    private String descricao;
    private double precoOrginal;
    private double precoPromocional;
    private Parceiro parceiro;
}
