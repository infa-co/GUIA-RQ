package Model.Entities;

import java.util.List;

public class Usuario {
    private long id;
    private String nome;
    private String email;
    private String senha;
    private PerfilUsuario perfil;
    private List<TipoTicket> tickets; 

    public enum PerfilUsuario {
        ADMIN,
        CLIENTE,
        PARCEIRO
    }
    public enum TipoTicket {
        GASTRONOMIA,
        AVENTURA,
        SERRA_EXPERIENCE,
        AVULSO
    }
}
