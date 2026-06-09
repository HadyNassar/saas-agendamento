package com.hady.saas.application.dto;

public class LoginResponseDTO {

    private String token;
    private Long usuarioId;
    private String nome;
    private String email;
    private String role;
    private Long estabelecimentoId;
    private String nomeEstabelecimento;
    private String slug;
    private String plano;
    private boolean temAcesso;
    private boolean trialAtivo;
    private String trialFim;

    public LoginResponseDTO(String token,
                            Long usuarioId,
                            String nome,
                            String email,
                            String role,
                            Long estabelecimentoId,
                            String nomeEstabelecimento,
                            String slug,
                            String plano,
                            boolean temAcesso,
                            boolean trialAtivo,
                            String trialFim) {
        this.token = token;
        this.usuarioId = usuarioId;
        this.nome = nome;
        this.email = email;
        this.role = role;
        this.estabelecimentoId = estabelecimentoId;
        this.nomeEstabelecimento = nomeEstabelecimento;
        this.slug = slug;
        this.plano = plano;
        this.temAcesso = temAcesso;
        this.trialAtivo = trialAtivo;
        this.trialFim = trialFim;
    }

    public String getToken()                  { return token; }
    public Long getUsuarioId()                { return usuarioId; }
    public String getNome()                   { return nome; }
    public String getEmail()                  { return email; }
    public String getRole()                   { return role; }
    public Long getEstabelecimentoId()        { return estabelecimentoId; }
    public String getNomeEstabelecimento()    { return nomeEstabelecimento; }
    public String getSlug()                   { return slug; }
    public String getPlano()                  { return plano; }
    public boolean isTemAcesso()              { return temAcesso; }
    public boolean isTrialAtivo()             { return trialAtivo; }
    public String getTrialFim()               { return trialFim; }
}