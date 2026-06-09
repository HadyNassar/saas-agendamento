package com.hady.saas.application.dto;

import com.hady.saas.domain.enums.Role;

public class UsuarioMeDTO {

    private Long id;
    private String nome;
    private String email;
    private Role role;

    public UsuarioMeDTO(Long id, String nome, String email, Role role) {
        this.id = id;
        this.nome = nome;
        this.email = email;
        this.role = role;
    }

    public Long getId()      { return id; }
    public String getNome()  { return nome; }
    public String getEmail() { return email; }
    public Role getRole()    { return role; }
}
