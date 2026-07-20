package com.hady.saas.application.dto;

public class ImpersonarResponseDTO {

    private String token;
    private String nomeUsuario;
    private String emailUsuario;
    private String role;
    private String nomeEstabelecimento;

    public ImpersonarResponseDTO(String token, String nomeUsuario, String emailUsuario,
                                 String role, String nomeEstabelecimento) {
        this.token = token;
        this.nomeUsuario = nomeUsuario;
        this.emailUsuario = emailUsuario;
        this.role = role;
        this.nomeEstabelecimento = nomeEstabelecimento;
    }

    public String getToken()               { return token; }
    public String getNomeUsuario()         { return nomeUsuario; }
    public String getEmailUsuario()        { return emailUsuario; }
    public String getRole()                { return role; }
    public String getNomeEstabelecimento() { return nomeEstabelecimento; }
}