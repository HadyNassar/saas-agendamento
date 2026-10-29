package com.hady.saas.business.service;

import com.hady.saas.application.dto.EditarEstabelecimentoAdminDTO;
import com.hady.saas.application.dto.EstabelecimentoAdminDTO;
import com.hady.saas.application.dto.EstabelecimentoListaAdminDTO;
import com.hady.saas.domain.estabelecimento.Estabelecimento;
import com.hady.saas.domain.enums.TipoEstabelecimento;
import com.hady.saas.infrastructure.repository.EstabelecimentoRepository;
import com.hady.saas.infrastructure.repository.UsuarioRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class GestaoUsuariosService {

    private final EstabelecimentoRepository estabelecimentoRepository;
    private final UsuarioRepository usuarioRepository;
    private final GeocodingService geocodingService;
    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final NotasAuditoriaService notasAuditoriaService;

    public GestaoUsuariosService(EstabelecimentoRepository estabelecimentoRepository,
                                 UsuarioRepository usuarioRepository,
                                 GeocodingService geocodingService,
                                 JdbcTemplate jdbcTemplate,
                                 PasswordEncoder passwordEncoder,
                                 NotasAuditoriaService notasAuditoriaService) {
        this.estabelecimentoRepository = estabelecimentoRepository;
        this.usuarioRepository = usuarioRepository;
        this.geocodingService = geocodingService;
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
        this.notasAuditoriaService = notasAuditoriaService;
    }

    // ── Listagem com filtros ──────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<EstabelecimentoListaAdminDTO> listar(String busca, String plano, String cidade, String status) {
        List<Estabelecimento> resultado = estabelecimentoRepository.buscarComFiltrosAdmin(busca, plano, cidade, status);

        return resultado.stream()
                .map(e -> new EstabelecimentoListaAdminDTO(
                        e.getId(),
                        e.getNome(),
                        e.getSlug(),
                        e.getTipo() != null ? e.getTipo().name() : null,
                        e.getCidade(),
                        e.getEstado(),
                        e.getPlano() != null ? e.getPlano().name() : null,
                        calcularStatus(e),
                        e.getCriadoEm(),
                        e.getPlanoValidoAte(),
                        e.getTrialFim(),
                        e.isAtivo()
                ))
                .toList();
    }

    // ── Detalhe completo de um estabelecimento ──────────────────────────
    @Transactional(readOnly = true)
    public EstabelecimentoAdminDTO buscarDetalhe(Long id) {
        Estabelecimento e = estabelecimentoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Estabelecimento não encontrado"));

        EstabelecimentoAdminDTO dto = new EstabelecimentoAdminDTO();
        dto.setId(e.getId());
        dto.setNome(e.getNome());
        dto.setSlug(e.getSlug());
        dto.setTipo(e.getTipo() != null ? e.getTipo().name() : null);
        dto.setCriadoEm(e.getCriadoEm());
        dto.setAtivo(e.isAtivo());

        dto.setTelefone(e.getTelefone());
        dto.setWhatsappContato(e.getWhatsappContato());
        dto.setWhatsappConectado(e.isWhatsappConectado());
        dto.setInstagram(e.getInstagram());
        dto.setFacebook(e.getFacebook());
        dto.setFotoPerfil(e.getFotoPerfil());

        dto.setCep(e.getCep());
        dto.setLogradouro(e.getLogradouro());
        dto.setNumero(e.getNumero());
        dto.setComplemento(e.getComplemento());
        dto.setBairro(e.getBairro());
        dto.setCidade(e.getCidade());
        dto.setEstado(e.getEstado());
        dto.setEnderecoCompleto(e.getEnderecoCompleto());
        dto.setLatitude(e.getLatitude());
        dto.setLongitude(e.getLongitude());

        dto.setPlano(e.getPlano() != null ? e.getPlano().name() : null);
        dto.setPlanoAtivo(e.isPlanoAtivo());
        dto.setPlanoValidoAte(e.getPlanoValidoAte());
        dto.setMpSubscriptionId(e.getMpSubscriptionId());
        dto.setMpConectado(e.isMpConectado());
        dto.setAceitaPagamento(e.isAceitaPagamento());
        dto.setExigirSinal(e.isExigirSinal());
        dto.setTipoSinal(e.getTipoSinal());
        dto.setValorSinal(e.getValorSinal());

        dto.setTrialAtivo(e.isTrialAtivo());
        dto.setTrialInicio(e.getTrialInicio());
        dto.setTrialFim(e.getTrialFim());
        dto.setTrialUsado(e.isTrialUsado());

        dto.setMesesAntecipacao(e.getMesesAntecipacao());
        dto.setComodidades(new ArrayList<>(e.getComodidades()));
        dto.setFormasPagamento(new ArrayList<>(e.getFormasPagamento()));

        preencherContadores(dto, id);

        return dto;
    }

    // ── Edição administrativa — qualquer campo, tudo opcional ──────────
    @Transactional
    public void editar(Long id, EditarEstabelecimentoAdminDTO dto) {
        Estabelecimento e = estabelecimentoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Estabelecimento não encontrado"));

        if (dto.getNome() != null && !dto.getNome().isBlank())     e.setNome(dto.getNome().trim());
        if (dto.getTipo() != null) {
            try {
                e.setTipo(TipoEstabelecimento.valueOf(dto.getTipo()));
            } catch (IllegalArgumentException ex) {
                throw new RuntimeException("Tipo de estabelecimento inválido: " + dto.getTipo());
            }
        }
        if (dto.getTelefone() != null)          e.setTelefone(dto.getTelefone());
        if (dto.getWhatsappContato() != null)   e.setWhatsappContato(dto.getWhatsappContato());
        if (dto.getInstagram() != null)         e.setInstagram(dto.getInstagram());
        if (dto.getFacebook() != null)          e.setFacebook(dto.getFacebook());
        if (dto.getFotoPerfil() != null)        e.setFotoPerfil(dto.getFotoPerfil());
        if (dto.getMesesAntecipacao() != null)  e.setMesesAntecipacao(dto.getMesesAntecipacao());
        if (dto.getComodidades() != null)       e.setComodidades(dto.getComodidades());
        if (dto.getFormasPagamento() != null)   e.setFormasPagamento(dto.getFormasPagamento());
        if (dto.getAtivo() != null)             e.setAtivo(dto.getAtivo());
        if (dto.getExigirSinal() != null)       e.setExigirSinal(dto.getExigirSinal());
        if (dto.getTipoSinal() != null)         e.setTipoSinal(dto.getTipoSinal());
        if (dto.getValorSinal() != null)        e.setValorSinal(dto.getValorSinal());

        // Endereço estruturado — se CEP veio preenchido, atualiza tudo e regeocodifica
        boolean enderecoMudou = dto.getCep() != null && !dto.getCep().isBlank();
        if (enderecoMudou) {
            e.setCep(dto.getCep());
            if (dto.getLogradouro() != null)  e.setLogradouro(dto.getLogradouro());
            if (dto.getNumero() != null)      e.setNumero(dto.getNumero());
            if (dto.getComplemento() != null) e.setComplemento(dto.getComplemento());
            if (dto.getBairro() != null)      e.setBairro(dto.getBairro());
            if (dto.getCidade() != null)      e.setCidade(dto.getCidade());
            if (dto.getEstado() != null)      e.setEstado(dto.getEstado());

            e.setEndereco(e.getEnderecoCompleto());
            geocodificarSePossivel(e);
        }

        // Nome mudou → regenera slug único (reaproveita lógica do EstabelecimentoService
        // via query direta simples, pra não criar dependência circular entre services)
        if (dto.getNome() != null && !dto.getNome().isBlank()) {
            e.setSlug(gerarSlugUnico(dto.getNome().trim(), e.getId()));
        }

        estabelecimentoRepository.save(e);
    }

    // ── Usuários do estabelecimento (para o seletor de impersonar) ────
    @Transactional(readOnly = true)
    public List<com.hady.saas.application.dto.UsuarioAdminDTO> listarUsuarios(Long estabelecimentoId) {
        // Garante que o estabelecimento existe antes de listar, para
        // devolver um erro claro em vez de uma lista vazia silenciosa.
        if (!estabelecimentoRepository.existsById(estabelecimentoId)) {
            throw new RuntimeException("Estabelecimento não encontrado");
        }

        return usuarioRepository.findByEstabelecimentoId(estabelecimentoId).stream()
                .map(u -> new com.hady.saas.application.dto.UsuarioAdminDTO(
                        u.getId(),
                        u.getNome(),
                        u.getEmail(),
                        u.getRole() != null ? u.getRole().name() : null
                ))
                .toList();
    }

    // ── Redefinição de senha pelo admin master ───────────────────────
    // Usado quando o cliente esqueceu a senha e não consegue recuperar por e-mail.
    @Transactional
    public void redefinirSenhaUsuario(Long estabelecimentoId, Long usuarioId,
                                      String novaSenha, String adminEmail) {
        if (novaSenha == null || novaSenha.length() < 8) {
            throw new RuntimeException("A senha deve ter no mínimo 8 caracteres.");
        }

        var usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado."));

        // Garante que o usuário pertence ao estabelecimento da URL
        if (usuario.getEstabelecimento() == null
                || !usuario.getEstabelecimento().getId().equals(estabelecimentoId)) {
            throw new RuntimeException("Usuário não pertence a este estabelecimento.");
        }

        usuario.setSenha(passwordEncoder.encode(novaSenha));
        // Invalida todas as sessões/JWTs emitidos antes deste instante
        usuario.setSenhaAtualizadaEm(java.time.Instant.now());
        usuarioRepository.save(usuario);

        // Auditoria — nunca registra a senha em si
        notasAuditoriaService.registrar(
                adminEmail,
                "REDEFINIR_SENHA_USUARIO",
                estabelecimentoId,
                "Redefiniu a senha do usuário " + usuario.getEmail() + " (role " + usuario.getRole() + ")"
        );
    }

    // ── Ações administrativas rápidas ────────────────────────────────

    @Transactional
    public void ativarDesativar(Long id, boolean ativo) {
        Estabelecimento e = estabelecimentoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Estabelecimento não encontrado"));
        e.setAtivo(ativo);
        estabelecimentoRepository.save(e);
    }

    @Transactional
    public void trocarPlano(Long id, String novoPlano, LocalDate validoAte) {
        Estabelecimento e = estabelecimentoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Estabelecimento não encontrado"));
        try {
            e.setPlano(com.hady.saas.domain.enums.Plano.valueOf(novoPlano));
        } catch (IllegalArgumentException ex) {
            throw new RuntimeException("Plano inválido: " + novoPlano);
        }
        e.setPlanoAtivo(true);
        e.setPlanoValidoAte(validoAte);
        estabelecimentoRepository.save(e);
    }

    @Transactional
    public void adicionarDiasTrial(Long id, int dias) {
        Estabelecimento e = estabelecimentoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Estabelecimento não encontrado"));

        LocalDate baseAtual = (e.getTrialFim() != null && e.getTrialFim().isAfter(LocalDate.now()))
                ? e.getTrialFim()
                : LocalDate.now();

        e.setTrialAtivo(true);
        e.setTrialFim(baseAtual.plusDays(dias));
        if (e.getTrialInicio() == null) e.setTrialInicio(LocalDate.now());

        estabelecimentoRepository.save(e);
    }

    // ── Privados ──────────────────────────────────────────────────────

    private String calcularStatus(Estabelecimento e) {
        boolean ativo = e.isPlanoAtivo() && e.getPlanoValidoAte() != null
                && !LocalDate.now().isAfter(e.getPlanoValidoAte());
        if (ativo) return "ATIVO";

        boolean trial = e.isTrialAtivo() && e.getTrialFim() != null
                && !LocalDate.now().isAfter(e.getTrialFim());
        if (trial) return "TRIAL";

        return "INATIVO";
    }

    private void preencherContadores(EstabelecimentoAdminDTO dto, Long estabelecimentoId) {
        dto.setTotalUsuarios(contar(
                "SELECT COUNT(*) FROM usuarios WHERE estabelecimento_id = ?", estabelecimentoId));
        dto.setTotalClientes(contar(
                "SELECT COUNT(*) FROM clientes WHERE estabelecimento_id = ?", estabelecimentoId));
        dto.setTotalAgendamentos(contar(
                "SELECT COUNT(*) FROM agendamentos WHERE estabelecimento_id = ?", estabelecimentoId));
        dto.setVisualizacoesMapaTotal(contar(
                "SELECT COUNT(*) FROM mapa_visualizacoes WHERE salao_id = ?", estabelecimentoId));
    }

    private long contar(String sql, Long id) {
        try {
            Long resultado = jdbcTemplate.queryForObject(sql, Long.class, id);
            return resultado != null ? resultado : 0L;
        } catch (Exception e) {
            // Tabela pode não existir ainda em ambientes mais antigos — não derruba a tela
            return 0L;
        }
    }

    private void geocodificarSePossivel(Estabelecimento e) {
        String endereco = e.getEnderecoCompleto();
        if (endereco == null || endereco.isBlank()) return;
        try {
            double[] coords = geocodingService.geocodificarEndereco(endereco);
            if (coords != null) {
                e.setLatitude(BigDecimal.valueOf(coords[0]));
                e.setLongitude(BigDecimal.valueOf(coords[1]));
            }
        } catch (Exception ex) {
            System.err.println("Geocodificação falhou silenciosamente: " + ex.getMessage());
        }
    }

    private String gerarSlugUnico(String nome, Long idAtual) {
        String base = java.text.Normalizer.normalize(nome, java.text.Normalizer.Form.NFD)
                .replaceAll("[^\\p{ASCII}]", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
        if (base.isBlank()) base = "estabelecimento";

        String slugFinal = base;
        int contador = 1;
        while (estabelecimentoRepository.existsBySlugAndIdNot(slugFinal, idAtual)) {
            slugFinal = base + "-" + contador++;
        }
        return slugFinal;
    }
}