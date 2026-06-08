package com.hady.saas.business.service;

import com.hady.saas.application.dto.FichaClienteDTO;
import com.hady.saas.application.dto.FotoClienteDTO;
import com.hady.saas.application.dto.FotoProfissionalDTO;
import com.hady.saas.domain.fotocliente.FotoCliente;
import com.hady.saas.domain.fotoprofissional.FotoProfissional;
import com.hady.saas.domain.cliente.Cliente;
import com.hady.saas.domain.profissional.Profissional;
import com.hady.saas.infrastructure.repository.*;
import jakarta.annotation.PostConstruct;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FotoService {

    private final FotoClienteRepository fotoClienteRepository;
    private final FotoProfissionalRepository fotoProfissionalRepository;
    private final ClienteRepository clienteRepository;
    private final ProfissionalRepository profissionalRepository;
    private final AgendamentoRepository agendamentoRepository;
    private final RespostaAnamneseRepository respostaAnamneseRepository;

    @Value("${upload.dir:uploads}")
    private String uploadDir;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    private static final int MAX_FOTOS = 10;
    private static final long MAX_TAMANHO_BYTES = 2L * 1024 * 1024; // 2 MB
    private static final Set<String> TIPOS_ACEITOS = Set.of("image/jpeg", "image/png", "image/webp");

    public FotoService(FotoClienteRepository fotoClienteRepository,
                       FotoProfissionalRepository fotoProfissionalRepository,
                       ClienteRepository clienteRepository,
                       ProfissionalRepository profissionalRepository,
                       AgendamentoRepository agendamentoRepository,
                       RespostaAnamneseRepository respostaAnamneseRepository) {
        this.fotoClienteRepository = fotoClienteRepository;
        this.fotoProfissionalRepository = fotoProfissionalRepository;
        this.clienteRepository = clienteRepository;
        this.profissionalRepository = profissionalRepository;
        this.agendamentoRepository = agendamentoRepository;
        this.respostaAnamneseRepository = respostaAnamneseRepository;
    }

    @PostConstruct
    public void criarDiretorios() throws IOException {
        Files.createDirectories(Paths.get(uploadDir, "clientes"));
        Files.createDirectories(Paths.get(uploadDir, "profissionais"));
    }

    // ─── FICHA COMPLETA DO CLIENTE (painel autenticado) ───────────────────────
    // Acesso exclusivo pelo painel — requer JWT com estabelecimentoId validado.
    // A ficha contém dados sensíveis: histórico, anamnese, fotos antes/depois.

    public FichaClienteDTO montarFichaCliente(Long clienteId, Long estabelecimentoId) {
        Cliente cliente = clienteRepository.findByIdAndEstabelecimentoId(clienteId, estabelecimentoId)
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));

        FichaClienteDTO ficha = new FichaClienteDTO();
        ficha.setId(cliente.getId());
        ficha.setNome(cliente.getNome());
        ficha.setTelefone(cliente.getTelefone());
        ficha.setEmail(cliente.getEmail());
        ficha.setAlergias(cliente.getAlergias());
        ficha.setPreferencias(cliente.getPreferencias());
        ficha.setRestricoes(cliente.getRestricoes());
        ficha.setObservacoes(cliente.getObservacoes());
        ficha.setBloqueado(cliente.isBloqueado());
        ficha.setTotalFaltas(cliente.getTotalFaltas());
        ficha.setCriadoEm(cliente.getCriadoEm());

        // Histórico — últimos 20 agendamentos
        ficha.setHistorico(
                agendamentoRepository
                        .findTop20ByClienteIdOrderByDataHoraInicioDesc(clienteId, estabelecimentoId)
                        .stream()
                        .map(a -> {
                            FichaClienteDTO.AgendamentoResumoDTO r = new FichaClienteDTO.AgendamentoResumoDTO();
                            r.setId(a.getId());
                            r.setDataHoraInicio(a.getDataHoraInicio());
                            r.setStatus(a.getStatus().name());
                            r.setProfissionalNome(a.getProfissional().getNome());
                            r.setServicoNome(a.getServico().getNome());
                            r.setValor(a.getServico().getPreco() != null
                                    ? a.getServico().getPreco().doubleValue() : null);
                            return r;
                        })
                        .collect(Collectors.toList())
        );

        // Respostas de anamnese
        ficha.setAnamnese(
                respostaAnamneseRepository
                        .findByClienteIdAndEstabelecimentoId(clienteId, estabelecimentoId)
                        .stream()
                        .map(r -> {
                            FichaClienteDTO.RespostaAnamneseDTO dto = new FichaClienteDTO.RespostaAnamneseDTO();
                            dto.setPerguntaId(r.getPergunta().getId());
                            dto.setPergunta(r.getPergunta().getTexto());
                            dto.setResposta(r.getResposta());
                            dto.setDetalhe(r.getDetalhe());
                            return dto;
                        })
                        .collect(Collectors.toList())
        );

        // Fotos antes/depois
        ficha.setFotos(
                fotoClienteRepository
                        .findByClienteIdAndClienteEstabelecimentoIdOrderByCriadoEmDesc(clienteId, estabelecimentoId)
                        .stream()
                        .map(f -> FotoClienteDTO.from(f, baseUrl))
                        .collect(Collectors.toList())
        );

        return ficha;
    }

    // ─── FOTOS DO CLIENTE ──────────────────────────────────────────────────────

    public List<FotoClienteDTO> listarFotosCliente(Long clienteId, Long estabelecimentoId) {
        clienteRepository.findByIdAndEstabelecimentoId(clienteId, estabelecimentoId)
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));

        return fotoClienteRepository
                .findByClienteIdOrderByCriadoEmDesc(clienteId)
                .stream()
                .map(f -> FotoClienteDTO.from(f, baseUrl))
                .collect(Collectors.toList());
    }

    public FotoClienteDTO adicionarFotoCliente(Long clienteId, Long estabelecimentoId,
                                               MultipartFile arquivo, String tipo) throws IOException {
        validarArquivo(arquivo);

        Cliente cliente = clienteRepository.findByIdAndEstabelecimentoId(clienteId, estabelecimentoId)
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));

        long total = fotoClienteRepository.countByClienteId(clienteId);
        if (total >= MAX_FOTOS) {
            throw new RuntimeException("Limite de " + MAX_FOTOS + " fotos atingido para este cliente");
        }

        FotoCliente.TipoFoto tipoFoto;
        try {
            tipoFoto = FotoCliente.TipoFoto.valueOf(tipo.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Tipo inválido. Use ANTES ou DEPOIS");
        }

        String nomeArquivo = salvarArquivoComprimido(arquivo, "clientes/" + clienteId);

        FotoCliente foto = new FotoCliente();
        foto.setCliente(cliente);
        foto.setTipo(tipoFoto);
        foto.setCaminho(uploadDir + "/clientes/" + clienteId + "/" + nomeArquivo);
        foto.setNomeOriginal(arquivo.getOriginalFilename());
        foto.setCriadoEm(LocalDateTime.now());

        fotoClienteRepository.save(foto);
        return FotoClienteDTO.from(foto, baseUrl);
    }

    // ─── SERVE ARQUIVO AUTENTICADO ────────────────────────────────────────────
    // Chamado pelo endpoint GET /painel/fotos/clientes/{fotoId}/serve.
    // Valida que a foto pertence ao estabelecimento do usuário logado antes de retornar o arquivo.
    public Resource serveArquivoCliente(Long fotoId, Long estabelecimentoId) throws IOException {
        FotoCliente foto = fotoClienteRepository.findById(fotoId)
                .orElseThrow(() -> new RuntimeException("Foto não encontrada"));

        if (!foto.getCliente().getEstabelecimento().getId().equals(estabelecimentoId)) {
            throw new RuntimeException("Acesso negado");
        }

        Path caminho = Paths.get(foto.getCaminho()).toAbsolutePath().normalize();

        // FIX: verifica que o caminho resolvido ainda está dentro do diretório de uploads.
        // Sem essa checagem, um valor corrompido no banco (ex: "../../etc/passwd") poderia
        // ser servido pelo endpoint autenticado — path traversal via banco de dados.
        Path base = Paths.get(uploadDir).toAbsolutePath().normalize();
        if (!caminho.startsWith(base)) {
            throw new RuntimeException("Acesso negado");
        }

        Resource resource = new UrlResource(caminho.toUri());

        if (!resource.exists() || !resource.isReadable()) {
            throw new RuntimeException("Arquivo não encontrado no disco");
        }

        return resource;
    }

    public void removerFotoCliente(Long fotoId, Long estabelecimentoId) throws IOException {
        FotoCliente foto = fotoClienteRepository.findById(fotoId)
                .orElseThrow(() -> new RuntimeException("Foto não encontrada"));

        if (!foto.getCliente().getEstabelecimento().getId().equals(estabelecimentoId)) {
            throw new RuntimeException("Acesso negado");
        }

        Files.deleteIfExists(Paths.get(foto.getCaminho()));
        fotoClienteRepository.delete(foto);
    }

    // ─── FOTOS DO PROFISSIONAL ─────────────────────────────────────────────────
    // Valida isolamento por estabelecimentoId em todos os casos (painel e portfólio público).
    // O portfólio público (FotoPublicoController) também passa estabelecimentoId resolvido via slug,
    // garantindo que um profissional de outro tenant não seja acessível.

    public List<FotoProfissionalDTO> listarFotosProfissional(Long profissionalId, Long estabelecimentoId) {
        profissionalRepository.findByIdAndEstabelecimentoId(profissionalId, estabelecimentoId)
                .orElseThrow(() -> new RuntimeException("Profissional não encontrado"));

        return fotoProfissionalRepository
                .findByProfissionalIdOrderByCriadoEmDesc(profissionalId)
                .stream()
                .map(f -> FotoProfissionalDTO.from(f, baseUrl))
                .collect(Collectors.toList());
    }

    public FotoProfissionalDTO adicionarFotoProfissional(Long profissionalId, Long estabelecimentoId,
                                                         MultipartFile arquivo) throws IOException {
        validarArquivo(arquivo);

        Profissional profissional = profissionalRepository
                .findByIdAndEstabelecimentoId(profissionalId, estabelecimentoId)
                .orElseThrow(() -> new RuntimeException("Profissional não encontrado"));

        long total = fotoProfissionalRepository.countByProfissionalId(profissionalId);
        if (total >= MAX_FOTOS) {
            throw new RuntimeException("Limite de " + MAX_FOTOS + " fotos atingido para este profissional");
        }

        String nomeArquivo = salvarArquivoComprimido(arquivo, "profissionais/" + profissionalId);

        FotoProfissional foto = new FotoProfissional();
        foto.setProfissional(profissional);
        foto.setCaminho(uploadDir + "/profissionais/" + profissionalId + "/" + nomeArquivo);
        foto.setNomeOriginal(arquivo.getOriginalFilename());
        foto.setCriadoEm(LocalDateTime.now());

        fotoProfissionalRepository.save(foto);
        return FotoProfissionalDTO.from(foto, baseUrl);
    }

    public void removerFotoProfissional(Long fotoId, Long estabelecimentoId) throws IOException {
        FotoProfissional foto = fotoProfissionalRepository.findById(fotoId)
                .orElseThrow(() -> new RuntimeException("Foto não encontrada"));

        if (!foto.getProfissional().getEstabelecimento().getId().equals(estabelecimentoId)) {
            throw new RuntimeException("Acesso negado");
        }

        Files.deleteIfExists(Paths.get(foto.getCaminho()));
        fotoProfissionalRepository.delete(foto);
    }

    // ─── HELPERS ──────────────────────────────────────────────────────────────

    private void validarArquivo(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new RuntimeException("Arquivo vazio");
        }
        String contentType = arquivo.getContentType();
        if (contentType == null || !TIPOS_ACEITOS.contains(contentType)) {
            throw new RuntimeException("Formato inválido. Use JPG, PNG ou WEBP");
        }
        if (arquivo.getSize() > MAX_TAMANHO_BYTES) {
            throw new RuntimeException("Arquivo muito grande. Máximo: 2MB");
        }
    }

    /**
     * Comprime a imagem com Thumbnailator:
     * - máximo 1200px de largura
     * - qualidade 80%
     * - salvo como JPG (extensão .jpg)
     *
     * Adicionar ao pom.xml:
     * <dependency>
     *   <groupId>net.coobird</groupId>
     *   <artifactId>thumbnailator</artifactId>
     *   <version>0.4.20</version>
     * </dependency>
     */
    private String salvarArquivoComprimido(MultipartFile arquivo, String subpasta) throws IOException {
        Path dir = Paths.get(uploadDir, subpasta);
        Files.createDirectories(dir);

        String nomeArquivo = UUID.randomUUID() + ".jpg";
        Path destino = dir.resolve(nomeArquivo);

        Thumbnails.of(arquivo.getInputStream())
                .width(1200)
                .keepAspectRatio(true)
                .outputQuality(0.80)
                .outputFormat("jpg")
                .toFile(destino.toFile());

        return nomeArquivo;
    }
}
