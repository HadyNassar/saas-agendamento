package com.hady.saas.business.service;

import com.hady.saas.domain.usuario.Usuario;
import com.hady.saas.infrastructure.repository.UsuarioRepository;
import com.hady.saas.infrastructure.security.CustomUserDetails;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public CustomUserDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        // findByEmailComEstabelecimento usa JOIN FETCH (INNER JOIN) — não
        // retorna nada para usuários sem estabelecimento (ADMIN_MASTER).
        // Por isso primeiro tenta esse caminho (mais eficiente pro caso
        // comum: usuário de estabelecimento); se não achar, cai no
        // findByEmail simples, que cobre o ADMIN_MASTER também.
        Usuario usuario = usuarioRepository.findByEmailComEstabelecimento(username)
                .or(() -> usuarioRepository.findByEmail(username))
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado: " + username));

        // estabelecimentoId fica null para ADMIN_MASTER — CustomUserDetails
        // e o restante do sistema já tratam esse campo como opcional
        // (é o mesmo Long usado no claim "eid" do JWT, que também aceita null).
        Long estabelecimentoId = usuario.getEstabelecimento() != null
                ? usuario.getEstabelecimento().getId()
                : null;

        return new CustomUserDetails(
                usuario.getEmail(),
                usuario.getSenha(),
                usuario.getId(),
                estabelecimentoId,
                usuario.getSenhaAtualizadaEm(),
                List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getRole().name()))
        );
    }
}