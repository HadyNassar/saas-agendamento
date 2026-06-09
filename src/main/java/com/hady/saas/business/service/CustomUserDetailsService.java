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

        Usuario usuario = usuarioRepository.findByEmailComEstabelecimento(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado: " + username));

        return new CustomUserDetails(
                usuario.getEmail(),
                usuario.getSenha(),
                usuario.getId(),
                usuario.getEstabelecimento().getId(),
                usuario.getSenhaAtualizadaEm(),
                List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getRole().name()))
        );
    }
}
