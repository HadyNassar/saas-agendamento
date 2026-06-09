package com.hady.saas.infrastructure.repository;

import com.hady.saas.domain.enums.Role;
import com.hady.saas.domain.usuario.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    // Conta usuários de um role específico — usado para validar o limite de 2 ADMINs no PLATINUM
    int countByEstabelecimentoIdAndRole(Long estabelecimentoId, Role role);

    // Lista todos os usuários de um estabelecimento
    List<Usuario> findByEstabelecimentoId(Long estabelecimentoId);

    // Busca o usuário já com o estabelecimento carregado (evita LazyInitializationException no PlanoAcessoFilter)
    @Query("SELECT u FROM Usuario u JOIN FETCH u.estabelecimento WHERE u.email = :email")
    Optional<Usuario> findByEmailComEstabelecimento(@Param("email") String email);
}