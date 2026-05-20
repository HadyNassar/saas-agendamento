package com.hady.saas.infrastructure.repository;

import com.hady.saas.domain.enums.Role;
import com.hady.saas.domain.usuario.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    // Conta funcionários do estabelecimento excluindo o ADMIN
    // Usado para validar o limite do plano
    int countByEstabelecimentoIdAndRoleNot(Long estabelecimentoId, Role role);

    // Lista todos os usuários de um estabelecimento
    List<Usuario> findByEstabelecimentoId(Long estabelecimentoId);
}
