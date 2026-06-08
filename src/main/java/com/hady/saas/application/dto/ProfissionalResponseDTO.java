package com.hady.saas.application.dto;

import com.hady.saas.domain.enums.Role;
import com.hady.saas.domain.profissional.Profissional;

import java.math.BigDecimal;

/**
 * DTO de resposta para listagem de profissionais no painel.
 * Expõe os dados do usuário vinculado (role, email) sem depender
 * do @JsonIgnore da entidade Profissional.
 */
public class ProfissionalResponseDTO {

    private Long id;
    private String nome;
    private String telefone;
    private String fotoPerfil;
    private BigDecimal comissaoPercent;
    private boolean ativo;

    // Dados do usuário vinculado
    private Long usuarioId;
    private String email;
    private Role role;

    public ProfissionalResponseDTO(Profissional p) {
        this.id             = p.getId();
        this.nome           = p.getNome();
        this.telefone       = p.getTelefone();
        this.fotoPerfil     = p.getFotoPerfil();
        this.comissaoPercent = p.getComissaoPercent();
        this.ativo          = p.isAtivo();

        if (p.getUsuario() != null) {
            this.usuarioId = p.getUsuario().getId();
            this.email     = p.getUsuario().getEmail();
            this.role      = p.getUsuario().getRole();
        }
    }

    public Long getId()                  { return id; }
    public String getNome()              { return nome; }
    public String getTelefone()          { return telefone; }
    public String getFotoPerfil()        { return fotoPerfil; }
    public BigDecimal getComissaoPercent() { return comissaoPercent; }
    public boolean isAtivo()             { return ativo; }
    public Long getUsuarioId()           { return usuarioId; }
    public String getEmail()             { return email; }
    public Role getRole()                { return role; }
}
