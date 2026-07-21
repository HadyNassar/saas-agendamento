package com.hady.saas.application.dto;

// Visão enxuta de um usuário, usada pelo admin master para listar quem
// pode ser impersonado dentro de um estabelecimento. Não inclui senha
// nem nenhum dado sensível — só o necessário para identificar a pessoa.
public class UsuarioAdminDTO {

    private Long id;
    private String nome;
    private String email;
    private String role;

    public UsuarioAdminDTO(Long id, String nome, String email, String role) {
        this.id = id;
        this.nome = nome;
        this.email = email;
        this.role = role;
    }

    public Long getId()       { return id; }
    public String getNome()   { return nome; }
    public String getEmail()  { return email; }
    public String getRole()   { return role; }
}
