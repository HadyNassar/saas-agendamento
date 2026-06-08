package com.hady.saas.infrastructure.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Collection;

/**
 * Principal carregado uma única vez pelo JwtAuthenticationFilter e armazenado
 * no SecurityContext durante toda a requisição.
 *
 * Carrega usuarioId, estabelecimentoId e senhaAtualizadaEm do banco (via JOIN FETCH),
 * eliminando queries extras a cada requisição autenticada.
 */
public class CustomUserDetails implements UserDetails {

    private final String email;
    private final String senha;
    private final Long usuarioId;
    private final Long estabelecimentoId;
    private final Instant senhaAtualizadaEm;
    private final Collection<? extends GrantedAuthority> authorities;

    public CustomUserDetails(String email,
                             String senha,
                             Long usuarioId,
                             Long estabelecimentoId,
                             Instant senhaAtualizadaEm,
                             Collection<? extends GrantedAuthority> authorities) {
        this.email = email;
        this.senha = senha;
        this.usuarioId = usuarioId;
        this.estabelecimentoId = estabelecimentoId;
        this.senhaAtualizadaEm = senhaAtualizadaEm;
        this.authorities = authorities;
    }

    public Long getUsuarioId()             { return usuarioId; }
    public Long getEstabelecimentoId()     { return estabelecimentoId; }
    public Instant getSenhaAtualizadaEm()  { return senhaAtualizadaEm; }

    @Override public String getUsername()                                     { return email; }
    @Override public String getPassword()                                     { return senha; }
    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }
    @Override public boolean isAccountNonExpired()                            { return true; }
    @Override public boolean isAccountNonLocked()                             { return true; }
    @Override public boolean isCredentialsNonExpired()                        { return true; }
    @Override public boolean isEnabled()                                      { return true; }
}
