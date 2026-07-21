package com.hady.saas.business.service;

import com.hady.saas.application.dto.ImpersonarResponseDTO;
import com.hady.saas.domain.usuario.Usuario;
import com.hady.saas.infrastructure.repository.UsuarioRepository;
import com.hady.saas.infrastructure.security.JwtUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ImpersonarService {

    private final UsuarioRepository usuarioRepository;
    private final JwtUtil jwtUtil;
    private final NotasAuditoriaService notasAuditoriaService;

    public ImpersonarService(UsuarioRepository usuarioRepository,
                             JwtUtil jwtUtil,
                             NotasAuditoriaService notasAuditoriaService) {
        this.usuarioRepository = usuarioRepository;
        this.jwtUtil = jwtUtil;
        this.notasAuditoriaService = notasAuditoriaService;
    }

    @Transactional
    public ImpersonarResponseDTO impersonar(Long estabelecimentoId, Long usuarioId, String adminEmail) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado."));

        // Garante que o usuário-alvo realmente pertence ao estabelecimento
        // informado na URL — evita impersonar um usuário de outro salão
        // passando o id errado por engano ou de propósito.
        if (usuario.getEstabelecimento() == null
                || !usuario.getEstabelecimento().getId().equals(estabelecimentoId)) {
            throw new RuntimeException("Usuário não pertence a este estabelecimento.");
        }

        String token = jwtUtil.gerarTokenImpersonacao(
                usuario.getEmail(),
                usuario.getId(),
                estabelecimentoId,
                adminEmail
        );

        notasAuditoriaService.registrar(
                adminEmail,
                "IMPERSONAR_CONTA",
                estabelecimentoId,
                "Impersonou o usuário " + usuario.getEmail() + " (role " + usuario.getRole() + ")"
        );

        return new ImpersonarResponseDTO(
                token,
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getRole().name(),
                usuario.getEstabelecimento().getNome()
        );
    }
}